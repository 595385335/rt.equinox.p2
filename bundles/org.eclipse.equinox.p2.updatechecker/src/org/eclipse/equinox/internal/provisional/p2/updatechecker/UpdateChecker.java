/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.updatechecker.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * Default implementation of {@link IUpdateChecker}.
 * <p>
 * This implementation is not optimized.  It doesn't optimize for multiple
 * polls on the same profile, nor does it cache any info about a profile from
 * poll to poll.
 */
public class UpdateChecker implements IUpdateChecker {
	public static boolean DEBUG = false;
	public static boolean TRACE = false;
	/**
	 * Map of IUpdateListener->UpdateCheckThread.
	 */
	private HashMap checkers = new HashMap();

	IProfileRegistry profileRegistry;
	IPlanner planner;

	private class UpdateCheckThread extends Thread {
		boolean done = false;
		long poll, delay;
		IUpdateListener listener;
		String profileId;
		Query query;

		UpdateCheckThread(String profileId, Query query, long delay, long poll, IUpdateListener listener) {
			this.poll = poll;
			this.delay = delay;
			this.profileId = profileId;
			this.query = query;
			this.listener = listener;
		}

		public void run() {
			try {
				if (delay != ONE_TIME_CHECK && delay > 0) {
					Thread.sleep(delay);
				}
				while (!done) {

					trace("Checking for updates for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
					IInstallableUnit[] iusWithUpdates = checkForUpdates(profileId, query);
					if (iusWithUpdates.length > 0) {
						trace("Notifying listener of available updates"); //$NON-NLS-1$
						UpdateEvent event = new UpdateEvent(profileId, iusWithUpdates);
						if (!done)
							listener.updatesAvailable(event);
					} else {
						trace("No updates were available"); //$NON-NLS-1$
					}
					if (delay == ONE_TIME_CHECK || delay <= 0) {
						done = true;
					} else {
						Thread.sleep(poll);
					}
				}
			} catch (InterruptedException e) {
				// nothing
			} catch (Exception e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Exception in update check thread", e)); //$NON-NLS-1$
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker#addUpdateCheck(java.lang.String, long, long, org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener)
	 */
	public void addUpdateCheck(String profileId, Query query, long delay, long poll, IUpdateListener listener) {
		if (checkers.containsKey(listener))
			return;
		trace("Adding update checker for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
		UpdateCheckThread thread = new UpdateCheckThread(profileId, query, delay, poll, listener);
		checkers.put(listener, thread);
		thread.start();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker#removeUpdateCheck(org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener)
	 */
	public void removeUpdateCheck(IUpdateListener listener) {
		checkers.remove(listener);
	}

	/*
	 * Return the array of ius in the profile that have updates
	 * available.
	 */
	IInstallableUnit[] checkForUpdates(String profileId, Query query) {
		IProfile profile = getProfileRegistry().getProfile(profileId);
		ArrayList iusWithUpdates = new ArrayList();
		if (profile == null)
			return new IInstallableUnit[0];
		ProvisioningContext context = new ProvisioningContext(getAvailableRepositories());
		if (query == null)
			query = InstallableUnitQuery.ANY;
		Iterator iter = profile.query(query, new Collector(), null).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			IInstallableUnit[] replacements = getPlanner().updatesFor(iu, context, null);
			if (replacements.length > 0)
				iusWithUpdates.add(iu);
		}
		return (IInstallableUnit[]) iusWithUpdates.toArray(new IInstallableUnit[iusWithUpdates.size()]);
	}

	/**
	 * Returns the list of metadata repositories that are currently available.
	 */
	private URI[] getAvailableRepositories() {
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		URI[] repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		ArrayList available = new ArrayList();
		for (int i = 0; i < repositories.length; i++) {
			try {
				repoMgr.loadRepository(repositories[i], null);
				available.add(repositories[i]);
			} catch (ProvisionException e) {
				//ignore unavailable repository
			}
		}
		return (URI[]) available.toArray(new URI[available.size()]);
	}

	void trace(String message) {
		if (Tracing.DEBUG_UPDATE_CHECK)
			Tracing.debug(message);
	}

	String getTimeStamp() {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("[MM/dd/yy;HH:mm:ss:SSS]"); //$NON-NLS-1$
		return df.format(d);
	}

	IPlanner getPlanner() {
		if (planner == null) {
			planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
			if (planner == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return planner;
	}

	IProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
			if (profileRegistry == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return profileRegistry;
	}

}

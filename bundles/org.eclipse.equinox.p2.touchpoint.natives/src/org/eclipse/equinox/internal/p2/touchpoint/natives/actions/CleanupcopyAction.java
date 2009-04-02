/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class CleanupcopyAction extends ProvisioningAction {

	public static final String ACTION_CLEANUPCOPY = "cleanupcopy"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		return cleanupcopy(parameters, true);
	}

	public IStatus undo(Map parameters) {
		return CopyAction.copy(parameters, false);
	}

	/**
	 * Perform a cleanup of a previously made copy action.
	 * @param parameters action parameters
	 * @param restoreable flag indicating if the operation should be backed up
	 * @return status
	 */
	public static IStatus cleanupcopy(Map parameters, boolean restoreable) {
		String source = (String) parameters.get(ActionConstants.PARM_SOURCE);
		if (source == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_SOURCE, ACTION_CLEANUPCOPY));
		String target = (String) parameters.get(ActionConstants.PARM_TARGET);
		if (target == null)
			return Util.createError(NLS.bind(Messages.param_not_set, ActionConstants.PARM_TARGET, ACTION_CLEANUPCOPY));
		IBackupStore backupStore = (IBackupStore) parameters.get(NativeTouchpoint.PARM_BACKUP);

		IInstallableUnit iu = (IInstallableUnit) parameters.get(ActionConstants.PARM_IU);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);

		String copied = profile.getInstallableUnitProperty(iu, "copied" + ActionConstants.PIPE + source + ActionConstants.PIPE + target); //$NON-NLS-1$

		if (copied == null)
			return Status.OK_STATUS;

		StringTokenizer tokenizer = new StringTokenizer(copied, ActionConstants.PIPE);
		List directories = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists())
				continue;

			if (file.isDirectory())
				directories.add(file);
			else {
				if (restoreable)
					try {
						backupStore.backup(file);
					} catch (IOException e) {
						return Util.createError(NLS.bind(Messages.backup_file_failed, file));
					}
				else
					file.delete();
			}
		}

		for (Iterator it = directories.iterator(); it.hasNext();) {
			File directory = (File) it.next();
			File[] children = directory.listFiles();
			if (children == null)
				return Util.createError(NLS.bind(Messages.Error_list_children_0, directory));

			if (children.length == 0) {
				if (restoreable)
					try {
						backupStore.backup(directory);
					} catch (IOException e) {
						return Util.createError(NLS.bind(Messages.backup_file_failed, directory));
					}
				else
					directory.delete();
			}
		}

		return Status.OK_STATUS;
	}

}
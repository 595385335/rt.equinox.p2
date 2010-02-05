/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;

/**
 * A metadata repository that resides in the local file system.  If the repository
 * location is a directory, this implementation will traverse the directory structure
 * and combine any metadata repository files that are found.
 */
public class LocalMetadataRepository extends AbstractMetadataRepository {

	static final private String CONTENT_FILENAME = "content"; //$NON-NLS-1$
	static final private String REPOSITORY_TYPE = LocalMetadataRepository.class.getName();
	static final private Integer REPOSITORY_VERSION = new Integer(1);
	static final private String JAR_EXTENSION = ".jar"; //$NON-NLS-1$
	static final private String XML_EXTENSION = ".xml"; //$NON-NLS-1$

	protected IUMap units = new IUMap();
	protected HashSet<RepositoryReference> repositories = new HashSet<RepositoryReference>();

	private static File getActualLocation(URI location, String extension) {
		File spec = URIUtil.toFile(location);
		String path = spec.getAbsolutePath();
		if (path.endsWith(CONTENT_FILENAME + extension)) {
			//todo this is the old code that doesn't look right
			//			return new File(spec + extension);
			return spec;
		}
		if (path.endsWith("/")) //$NON-NLS-1$
			path += CONTENT_FILENAME;
		else
			path += "/" + CONTENT_FILENAME; //$NON-NLS-1$
		return new File(path + extension);
	}

	public static File getActualLocation(URI location) {
		return getActualLocation(location, XML_EXTENSION);
	}

	/**
	 * This no argument constructor is called when restoring an existing repository.
	 */
	public LocalMetadataRepository(IProvisioningAgent agent) {
		super(agent);
	}

	/**
	 * This constructor is used when creating a new local repository.
	 * @param location The location of the repository
	 * @param name The name of the repository
	 */
	public LocalMetadataRepository(IProvisioningAgent agent, URI location, String name, Map<String, String> properties) {
		super(agent, name == null ? (location != null ? location.toString() : "") : name, REPOSITORY_TYPE, REPOSITORY_VERSION.toString(), location, null, null, properties); //$NON-NLS-1$
		if (!location.getScheme().equals("file")) //$NON-NLS-1$
			throw new IllegalArgumentException("Invalid local repository location: " + location); //$NON-NLS-1$
		//when creating a repository, we must ensure it exists on disk so a subsequent load will succeed
		save();
	}

	public synchronized void addInstallableUnits(IInstallableUnit[] installableUnits) {
		if (installableUnits == null || installableUnits.length == 0)
			return;
		units.addAll(installableUnits);
		save();
	}

	public synchronized void addReference(URI repositoryLocation, String nickname, int repositoryType, int options) {
		assertModifiable();
		repositories.add(new RepositoryReference(repositoryLocation, nickname, repositoryType, options));
	}

	public void initialize(RepositoryState state) {
		synchronized (this) {
			this.name = state.Name;
			this.type = state.Type;
			this.version = state.Version.toString();
			this.provider = state.Provider;
			this.description = state.Description;
			this.location = state.Location;
			this.properties = state.Properties;
			this.units.addAll(state.Units);
			this.repositories.addAll(Arrays.asList(state.Repositories));
		}
		publishRepositoryReferences();
	}

	/**
	 * Broadcast discovery events for all repositories referenced by this repository.
	 */
	public void publishRepositoryReferences() {
		IProvisioningEventBus bus = (IProvisioningEventBus) agent.getService(IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;

		List<RepositoryReference> repositoriesSnapshot = createRepositoriesSnapshot();
		for (RepositoryReference reference : repositoriesSnapshot) {
			boolean isEnabled = (reference.Options & IRepository.ENABLED) != 0;
			bus.publishEvent(new RepositoryEvent(reference.Location, reference.Type, RepositoryEvent.DISCOVERED, isEnabled));
		}
	}

	private synchronized List<RepositoryReference> createRepositoriesSnapshot() {
		if (repositories.isEmpty())
			return CollectionUtils.emptyList();
		return new ArrayList<RepositoryReference>(repositories);
	}

	// use this method to setup any transient fields etc after the object has been restored from a stream
	public synchronized void initializeAfterLoad(URI aLocation) {
		this.location = aLocation;
	}

	public boolean isModifiable() {
		return true;
	}

	public synchronized IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (query instanceof InstallableUnitQuery)
			return units.query((InstallableUnitQuery) query);
		return query.perform(units.iterator());
	}

	public synchronized void removeAll() {
		units.clear();
		save();
	}

	public synchronized boolean removeInstallableUnits(IInstallableUnit[] installableUnits, IProgressMonitor monitor) {
		boolean changed = false;
		if (installableUnits != null && installableUnits.length > 0) {
			changed = true;
			units.removeAll(Arrays.asList(installableUnits));
		}
		if (changed)
			save();
		return changed;
	}

	// caller should be synchronized
	private void save() {
		File file = getActualLocation(location);
		File jarFile = getActualLocation(location, JAR_EXTENSION);
		boolean compress = "true".equalsIgnoreCase(properties.get(PROP_COMPRESSED)); //$NON-NLS-1$
		try {
			OutputStream output = null;
			if (!compress) {
				if (jarFile.exists()) {
					jarFile.delete();
				}
				if (!file.exists()) {
					if (!file.getParentFile().exists())
						file.getParentFile().mkdirs();
					file.createNewFile();
				}
				output = new FileOutputStream(file);
			} else {
				if (file.exists()) {
					file.delete();
				}
				if (!jarFile.exists()) {
					if (!jarFile.getParentFile().exists())
						jarFile.getParentFile().mkdirs();
					jarFile.createNewFile();
				}
				JarEntry jarEntry = new JarEntry(file.getName());
				JarOutputStream jOutput = new JarOutputStream(new FileOutputStream(jarFile));
				jOutput.putNextEntry(jarEntry);
				output = jOutput;
			}
			super.setProperty(IRepository.PROP_TIMESTAMP, Long.toString(System.currentTimeMillis()));
			new MetadataRepositoryIO(agent).write(this, output);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_WRITE, "Error saving metadata repository: " + location, e)); //$NON-NLS-1$
		}
	}

	public String setProperty(String key, String newValue) {
		String oldValue = null;
		synchronized (this) {
			oldValue = super.setProperty(key, newValue);
			if (oldValue == newValue || (oldValue != null && oldValue.equals(newValue)))
				return oldValue;
			save();
		}
		//force repository manager to reload this repository because it caches properties
		MetadataRepositoryManager manager = (MetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager.removeRepository(getLocation()))
			manager.addRepository(this);
		return oldValue;
	}
}

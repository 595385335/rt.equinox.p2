/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import org.eclipse.equinox.p2.metadata.Version;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import junit.framework.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;

/**
 * A simple metadata repository used for testing purposes.  All metadata
 * is kept in memory.
 */
public class TestMetadataRepository extends AbstractMetadataRepository {

	private static final String DESCRIPTION = "A Test Metadata Repository"; //$NON-NLS-1$
	private static final String NAME = "ATestMetadataRepository"; //$NON-NLS-1$
	private static final String PROVIDER = "org.eclipse"; //$NON-NLS-1$
	private static final String TYPE = "testmetadatarepo"; //$NON-NLS-1$
	private static final String VERSION = "1"; //$NON-NLS-1$
	private final List units = new ArrayList();
	protected HashSet repositories = new HashSet();

	private static URI createLocation() {
		try {
			//Just need a unique URL - we don't need to read/write this location
			return new URI("http://TestMetadataRepository.com/" + Long.toString(System.currentTimeMillis()));
		} catch (URISyntaxException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	public TestMetadataRepository(IInstallableUnit[] ius) {
		super(NAME, TYPE, VERSION, createLocation(), DESCRIPTION, PROVIDER, null);
		units.addAll(Arrays.asList(ius));
	}

	public void addInstallableUnits(IInstallableUnit[] toAdd) {
		units.addAll(Arrays.asList(toAdd));
	}

	public IInstallableUnit find(String id, String versionString) {
		Iterator result = query(new InstallableUnitQuery(id, Version.create(versionString)), null).iterator();
		return (IInstallableUnit) (result.hasNext() ? result.next() : null);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == TestMetadataRepository.class || adapter == IMetadataRepository.class || adapter == IRepository.class) {
			return this;
		}
		return null;
	}

	public IQueryResult query(IQuery query, IProgressMonitor monitor) {
		return query.perform(units.iterator());
	}

	public void removeAll() {
		units.clear();
	}

	public boolean removeInstallableUnits(IInstallableUnit[] toRemove) {
		boolean modified = false;
		for (int i = 0; i < toRemove.length; i++) {
			modified |= units.remove(toRemove[i]);
		}
		return modified;
	}

	public void initialize(RepositoryState state) {
		this.name = state.Name;
		this.type = state.Type;
		this.version = state.Version.toString();
		this.provider = state.Provider;
		this.description = state.Description;
		this.location = state.Location;
		this.properties = state.Properties;
		this.units.addAll(Arrays.asList(state.Units));
		this.repositories.addAll(Arrays.asList(state.Repositories));
	}

	public synchronized void addReference(URI repositoryLocation, String nickname, int repositoryType, int options) {
		assertModifiable();
		repositories.add(new RepositoryReference(repositoryLocation, nickname, repositoryType, options));
	}

	/**
	 * Returns a collection of {@link RepositoryReference}.
	 */
	public Collection getReferences() {
		return repositories;
	}

	/**
	 * Asserts that this repository is modifiable, throwing a runtime exception if
	 * it is not. This is suitable for use by subclasses when an attempt is made
	 * to write to a repository.
	 */
	protected void assertModifiable() {
	}
}

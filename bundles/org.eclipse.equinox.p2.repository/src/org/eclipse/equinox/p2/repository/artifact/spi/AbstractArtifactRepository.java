/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact.spi;

import org.eclipse.equinox.p2.repository.spi.AbstractRepository;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.*;

/**
 * The common base class for all artifact repository implementations. Clients must
 * subclass this class to create their own repository implementations.
 * <p>
 * This base class provides default implementations of all methods that modify the repository.
 * These default methods throw an exception if {@link #isModifiable()} returns <code>false</code>.
 * Therefore a client can implement a read-only repository by overriding only the abstract methods.
 * @since 2.0
 */
public abstract class AbstractArtifactRepository extends AbstractRepository<IArtifactKey> implements IArtifactRepository {

	protected AbstractArtifactRepository(String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		super(name, type, version, location, description, provider, properties);
	}

	public abstract boolean contains(IArtifactDescriptor descriptor);

	public abstract boolean contains(IArtifactKey key);

	public abstract IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	public abstract IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	public abstract IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	public abstract OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException;

	public void addDescriptor(IArtifactDescriptor descriptor) {
		assertModifiable();
	}

	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		assertModifiable();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		assertModifiable();
	}

	public void removeDescriptor(IArtifactKey key) {
		assertModifiable();
	}

	public void removeAll() {
		assertModifiable();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AbstractArtifactRepository)) {
			return false;
		}
		if (URIUtil.sameURI(getLocation(), ((AbstractArtifactRepository) o).getLocation()))
			return true;
		return false;
	}

	public int hashCode() {
		return (this.getLocation().toString().hashCode()) * 87;
	}

	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return new ArtifactDescriptor(key);
	}
}

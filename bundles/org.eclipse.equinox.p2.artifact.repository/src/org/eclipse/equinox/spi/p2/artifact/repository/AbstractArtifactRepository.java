/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.spi.p2.artifact.repository;

import java.io.OutputStream;
import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.Utils;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;

public abstract class AbstractArtifactRepository extends AbstractRepository implements IArtifactRepository {

	protected AbstractArtifactRepository(String name, String type, String version, URL location, String description, String provider) {
		super(name, type, version, location, description, provider);
	}

	public abstract boolean contains(IArtifactDescriptor descriptor);

	public abstract boolean contains(IArtifactKey key);

	public abstract IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	public abstract IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	public abstract IArtifactKey[] getArtifactKeys();

	public abstract IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	public OutputStream getOutputStream(IArtifactDescriptor descriptor, IArtifactRequest request) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
		return null;
	}

	public void addDescriptor(IArtifactDescriptor descriptor) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeDescriptor(IArtifactKey key) {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public void removeAll() {
		if (!isModifiable())
			throw new UnsupportedOperationException("Repository not modifiable");
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AbstractArtifactRepository)) {
			return false;
		}
		if (Utils.sameURL(getLocation(), ((AbstractArtifactRepository) o).getLocation()))
			return true;
		return false;
	}

	public int hashCode() {
		return (this.getLocation().toString().hashCode()) * 87;
	}

}

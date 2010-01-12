/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *     EclipseSource - ongoing development
 *     Thomas Hallgren - Fix for bug 268659
 *     IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

import org.eclipse.equinox.internal.p2.core.helpers.StringHelper;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * An object representing a (id,version) pair. 
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class VersionedId implements IVersionedId {
	private final String id;
	private final Version version;

	/**
	 * Creates and returns a new {@link VersionedId} from the given string specification.  
	 * The specification must be of the form "id/version", or just "id" if the version is absent
	 * <p>
	 * This factory method can be used to reconstruct a {@link VersionedId}
	 * instance from the string representation produced by a previous invocation of 
	 * {@link #toString()}.
	 * 
	 * @param spec the specification for the versioned id to create
	 * @return the parsed versioned id
	 * @throws IllegalArgumentException If <code>spec</code> is improperly
	 *         formatted.
	 */
	public static IVersionedId parse(String spec) {
		String[] segments = StringHelper.getArrayFromString(spec, '/');
		return new VersionedId(segments[0], segments.length == 1 ? null : segments[1]);
	}

	/**
	 * Creates a new versioned id with the given id and version.
	 * 
	 * @param id The identifier
	 * @param version The version
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public VersionedId(String id, String version) {
		this.id = id;
		this.version = Version.parseVersion(version);
	}

	/**
	 * Creates a new versioned id with the given id and version.
	 * 
	 * @param id The identifier
	 * @param version The version
	 */
	public VersionedId(String id, Version version) {
		this.id = id;
		this.version = (version == null) ? Version.emptyVersion : version;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof VersionedId))
			return false;

		VersionedId vname = (VersionedId) obj;
		return id.equals(vname.id) && version.equals(vname.version);
	}

	public int hashCode() {
		return id.hashCode() * 31 + version.hashCode();
	}

	public String getId() {
		return id;
	}

	public Version getVersion() {
		return version;
	}

	/**
	 * Returns a string representation of this versioned id.
	 * The result can be used to later construct an equal {@link VersionedId}
	 * instance using {{@link #parse(String)}.
	 * @return A string representation of this versioned id
	 */
	public String toString() {
		return Version.emptyVersion.equals(version) ? id : id + '/' + version.toString();
	}
}

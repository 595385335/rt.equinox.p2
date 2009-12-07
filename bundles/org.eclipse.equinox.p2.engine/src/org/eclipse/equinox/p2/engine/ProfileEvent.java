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
package org.eclipse.equinox.p2.engine;

import java.util.EventObject;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ProfileEvent extends EventObject {
	private static final long serialVersionUID = 3082402920617281765L;

	public static final byte ADDED = 0;
	public static final byte REMOVED = 1;
	public static final byte CHANGED = 2;

	private byte reason;

	public ProfileEvent(String profileId, byte reason) {
		super(profileId);
		this.reason = reason;
	}

	public byte getReason() {
		return reason;
	}

	public String getProfileId() {
		return (String) getSource();
	}
}
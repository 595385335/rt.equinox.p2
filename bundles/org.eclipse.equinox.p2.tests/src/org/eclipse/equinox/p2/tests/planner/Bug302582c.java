/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * @since 1.0
 */
public class Bug302582c extends Bug302582 {
	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.planner.Bug302582#getProfileId()
	 */
	@Override
	protected String getProfileId() {
		return "PlatformProfile";
	}

	/* (non-Javadoc)
		 * @see org.eclipse.equinox.p2.tests.planner.Bug302582#getTestDataPath()
		 */
	@Override
	protected String getTestDataPath() {
		return "testData/bug302582c";
	}

	public void testInstall() {
		IPlanner planner = createPlanner();

		// create the actual plan - install everything in the repo as optional (mimic the dropins folder)
		IQueryResult allIUs = repo.query(InstallableUnitQuery.ANY, new NullProgressMonitor());
		//		IQueryResult allIUs = repo.query(new InstallableUnitQuery("aaa", new VersionRange("[1.0.2, 1.0.3]")), new NullProgressMonitor());

		IProfileChangeRequest actualChangeRequest = createProfileChangeRequest(allIUs.toSet(), null, null);
		IProvisioningPlan actualPlan = planner.getProvisioningPlan(actualChangeRequest, null, new NullProgressMonitor());

		// this is the plan that we expect - highest version only
		IQueryResult queryResult = repo.query(new InstallableUnitQuery("aaa", Version.createOSGi(1, 0, 3)), new NullProgressMonitor());
		IProfileChangeRequest expectedChangeRequest = createProfileChangeRequest(queryResult.toSet(), null, null);
		IProvisioningPlan expectedPlan = planner.getProvisioningPlan(expectedChangeRequest, null, new NullProgressMonitor());

		assertContains("1.0", expectedPlan, actualPlan);
	}
}

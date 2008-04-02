/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class MissingOptionalWithDependencies extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit d;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", new Version("1.0.0"), true);

		//B's dependency is missing
		RequiredCapability[] reqB = new RequiredCapability[2];
		reqB[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		reqB[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true);
		b1 = createIU("B", new Version("1.0.0"), reqB);

		RequiredCapability[] req = new RequiredCapability[2];
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true);
		req[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		d = createIU("D", req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, d});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//Ensure that D's installation does not fail because of C's absence
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {d});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertInstallOperand(plan, d);
	}
}

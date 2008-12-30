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
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleOptionalTest2 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit b2;
	private IInstallableUnit b3;
	private IInstallableUnit c1;
	private IInstallableUnit c2;
	private IInstallableUnit d1;
	private IInstallableUnit d2;
	private IInstallableUnit x1;
	private IInstallableUnit y1;
	private IInstallableUnit z1;

	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		b1 = createIU("B", new Version("1.0.0"), true);
		b2 = createIU("B", new Version("2.0.0"), true);
		b3 = createIU("B", new Version("3.0.0"), true);

		c1 = createIU("C", new Version("1.0.0"), true);
		c2 = createIU("C", new Version("2.0.0"), true);

		d1 = createIU("D", new Version("1.0.0"), true);
		d2 = createIU("D", new Version("2.0.0"), true);

		y1 = createIU("Y", new Version("1.0.0"), true);

		z1 = createIU("Z", new Version("1.0.0"), true);

		//B's dependency is missing
		IRequiredCapability[] reqA = new IRequiredCapability[3];
		reqA[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		reqA[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		reqA[2] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true);
		a1 = createIU("A", new Version("1.0.0"), reqA);

		IRequiredCapability[] req = new IRequiredCapability[3];
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		req[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, false, false, true);
		req[2] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "Z", VersionRange.emptyRange, null, true, false, true);
		x1 = createIU("X", req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, c1, c2, d1, d2, x1, z1, y1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//Ensure that D's installation does not fail because of C's absence
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, x1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, c2);
		assertInstallOperand(plan, d1);
		assertInstallOperand(plan, y1);
		assertInstallOperand(plan, z1);
		assertInstallOperand(plan, x1);
	}
}

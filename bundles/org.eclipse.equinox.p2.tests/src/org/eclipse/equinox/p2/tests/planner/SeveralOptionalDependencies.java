/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
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

public class SeveralOptionalDependencies extends AbstractProvisioningTest {
	private IInstallableUnit x1;
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		IRequiredCapability reqA = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, true, false, true);
		IRequiredCapability reqB = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		IRequiredCapability reqC = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[0.0.0, 1.0.0)"), null, true, false, true); //will not match
		IRequiredCapability reqD = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true); //will not match
		x1 = createIU("X", new Version("1.0.0"), new IRequiredCapability[] {reqA, reqB, reqC, reqD});
		a1 = createIU("A", new Version("1.0.0"), true);
		b1 = createIU("B", new Version("1.0.0"), true);
		c1 = createIU("C", new Version("2.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {x1, a1, b1, c1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {x1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, x1);
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertNoOperand(plan, c1);
	}
}
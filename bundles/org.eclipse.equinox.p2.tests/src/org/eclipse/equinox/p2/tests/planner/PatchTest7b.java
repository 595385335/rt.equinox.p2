/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest7b extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit x1;
	IInstallableUnit y1;
	IInstallableUnit y2;
	IInstallableUnit f1;

	IInstallableUnitPatch p1;
	IInstallableUnitPatch pp1;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		x1 = createIU("X", new Version(1, 2, 0), true);
		y1 = createIU("Y", new Version(1, 0, 0), true);
		y2 = createIU("Y", new Version(1, 2, 0), true);
		a1 = createIU("A", new Version("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		b1 = createIU("B", new Version("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		c1 = createIU("C", new Version("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "Y", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});

		IRequiredCapability[] req = new IRequiredCapability[3];
		req[2] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 1.1.0)"), null, false, true);
		f1 = createIU("F", new Version(1, 0, 0), req);

		IRequirementChange changeX = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "X", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability[][] scope = new IRequiredCapability[0][0]; //new RequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, false)}};
		p1 = createIUPatch("P", new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeX}, scope, null, new IRequiredCapability[0]);

		IRequirementChange changeY = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "Y", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability[][] scopePP = new IRequiredCapability[0][0]; //new RequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, false)}};
		pp1 = createIUPatch("PP", new Version("1.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, new IRequirementChange[] {changeY}, scopePP, null, new IRequiredCapability[0]);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, x1, y1, y2, f1, p1, pp1});
		//		createTestMetdataRepository(new IInstallableUnit[] {c1, y1, y2, f1, pp1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testGeneralScope() {
		//Confirm that f1 can't be installed
		//		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		//		req1.addInstallableUnits(new IInstallableUnit[] {f1});
		//		ProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		//		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());

		//Verify that the installation of f1 and p1 succeed
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {f1, p1});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, f1);
		assertInstallOperand(plan2, a1);
		assertInstallOperand(plan2, b1);
		assertInstallOperand(plan2, c1);
		assertInstallOperand(plan2, x1);
		assertInstallOperand(plan2, y1);
		assertInstallOperand(plan2, p1);

	}
}

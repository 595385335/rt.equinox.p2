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

import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTest2 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnitPatch p1;
	IInstallableUnitPatch p2;
	IInstallableUnitPatch p3;
	IInstallableUnitPatch p4;
	IInstallableUnitPatch p5;
	IInstallableUnitPatch p6;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		IProvidedCapability[] cap = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("foo", "bar", new Version(1, 0, 0))};
		a1 = createIU("A", new Version("1.0.0"), null, new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, false)}, cap, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, false, null, null);
		b1 = createIU("B", new Version(1, 2, 0), true);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		p1 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, false)}}, null);
		p2 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.3.0, 1.5.0]"), null, false, false, false)}}, null);
		p3 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] { {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.1.0]"), null, false, false, false)}, {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.3.0, 1.5.0]"), null, false, false, false)}}, null);
		p4 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] { {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.6.0, 1.7.0]"), null, false, false, false)}, {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.3.0, 1.5.0]"), null, false, false, false)}}, null);
		p5 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability("foo", "bar", new VersionRange("[1.0.0, 2.0.0)"), null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.5.0]"), null, false, false, false)}}, null);
		p6 = createIUPatch("P", new Version("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {}, null);
		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, p1, p2, p3, p4, p5, p6});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testPatchScope() {
		//p6 applies to all IUs therefore A's installation succeed
		ProfileChangeRequest req8 = new ProfileChangeRequest(profile1);
		req8.addInstallableUnits(new IInstallableUnit[] {a1, p6});
		ProvisioningPlan plan8 = planner.getProvisioningPlan(req8, null, null);
		assertEquals(IStatus.OK, plan8.getStatus().getSeverity());

		//p5 does not causes a1 to resolve therefore the application fails
		ProfileChangeRequest req6 = new ProfileChangeRequest(profile1);
		req6.addInstallableUnits(new IInstallableUnit[] {a1, p5});
		ProvisioningPlan plan6 = planner.getProvisioningPlan(req6, null, null);
		assertEquals(IStatus.OK, plan6.getStatus().getSeverity());

		//Ensure that p1 causes a1 to resolve
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());

		//p2 does not causes a1 to resolve therefore the application fails
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p2});
		ProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.ERROR, plan3.getStatus().getSeverity());

		//Ensure that p3 causes a1 to resolve since it has two scopes where one is applicable
		ProfileChangeRequest req4 = new ProfileChangeRequest(profile1);
		req4.addInstallableUnits(new IInstallableUnit[] {a1, p3});
		ProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertEquals(IStatus.OK, plan4.getStatus().getSeverity());

		//p4 does not causes a1 to resolve therefore the application fails
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile1);
		req5.addInstallableUnits(new IInstallableUnit[] {a1, p4});
		ProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.ERROR, plan5.getStatus().getSeverity());

	}

	public void testExplanation3() {
		//p2 does not cause a1 to resolve therefore the application fails
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a1, p2});
		ProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.ERROR, plan3.getStatus().getSeverity());
		assertEquals(Explanation.MISSING_REQUIREMENT, plan3.getRequestStatus().getShortExplanation());
		// System.out.println(plan3.getRequestStatus().getExplanations());
		Set conflictingRoot = plan3.getRequestStatus().getConflictsWithInstalledRoots();
		assertTrue(conflictingRoot.contains(a1));
		assertEquals(1, conflictingRoot.size());
	}

	public void testExplanation5() {
		//p4 does not cause a1 to resolve therefore the application fails
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile1);
		req5.addInstallableUnits(new IInstallableUnit[] {a1, p4});
		ProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.ERROR, plan5.getStatus().getSeverity());
		assertEquals(Explanation.MISSING_REQUIREMENT, plan5.getRequestStatus().getShortExplanation());
		// System.out.println(plan5.getRequestStatus().getExplanations());
		Set conflictingRoot = plan5.getRequestStatus().getConflictsWithInstalledRoots();
		assertTrue(conflictingRoot.contains(a1));
		assertEquals(1, conflictingRoot.size());
	}
}

/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MultipleSingleton extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit a3;
	IInstallableUnit a4;
	IInstallableUnit u, v, w, x, y, z;

	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", new Version("1.0.0"), true);

		a2 = createIU("A", new Version("2.0.0"), true);

		a3 = createIU("A", new Version("3.0.0"), false);

		a4 = createIU("A", new Version("4.0.0"), false);

		x = createIU("X", new Version(2, 0, 0), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 4.1.0)"), null));

		IRequiredCapability c1 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequiredCapability c2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]"), null, false, false);
		y = createIU("Y", new Version(2, 0, 0), new IRequiredCapability[] {c1, c2});

		IRequiredCapability c3 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 3.0.0]"), null, false, false);
		IRequiredCapability c4 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[4.0.0, 4.0.0]"), null, false, false);
		z = createIU("Z", new Version(2, 0, 0), new IRequiredCapability[] {c3, c4});

		IRequiredCapability c5 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequiredCapability c6 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 3.0.0]"), null, false, false);
		w = createIU("W", new Version(2, 0, 0), new IRequiredCapability[] {c5, c6});

		IRequiredCapability c7 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0]"), null, false, false);
		IRequiredCapability c8 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 4.0.0]"), null, false, false);
		v = createIU("V", new Version(2, 0, 0), new IRequiredCapability[] {c7});
		u = createIU("U", new Version(2, 0, 0), new IRequiredCapability[] {c8});

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, a3, a4, w, x, y, z});

		profile = createProfile("TestProfile.MultipleSingleton" + getName());
		planner = createPlanner();

	}

	public void test1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {x});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test2() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation2() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		Set explanation = plan.getRequestStatus().getExplanations();
		System.out.println(explanation);
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, plan.getRequestStatus().getShortExplanation());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(y));

	}

	public void test3() {
		//Test that we can install A3 and A4 together
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {z});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test4() {
		//Ensure that A1 and A3 can't be installed together since one is singleton and the other not
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {w});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation4() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {w});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		Set explanation = plan.getRequestStatus().getExplanations();
		System.out.println(explanation);
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, plan.getRequestStatus().getShortExplanation());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(w));

	}

	public void test5b() {
		//Validate the setup
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test5c() {
		//Validate the setup
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {v});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test5() {
		//Ensure
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u, v});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation5() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u, v});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		Set explanation = plan.getRequestStatus().getExplanations();
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, plan.getRequestStatus().getShortExplanation());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(u));
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(v));
		System.out.println(explanation);

	}
}

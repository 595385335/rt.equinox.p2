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

import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTestOptional2 extends AbstractProvisioningTest {
	private static final String PP1 = "PatchForIUP1";
	private static final String P2 = "P2";
	private static final String P1 = "P1";
	private static final String P2_FEATURE = "p2.feature";
	private IInstallableUnit p2Feature;
	private IInstallableUnit p1;
	private IInstallableUnit p2;
	private IInstallableUnitPatch pp1;
	private IInstallableUnit p2b;
	private IProfile profile1;
	private IPlanner planner;
	private IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		p2Feature = createIU(P2_FEATURE, new Version(1, 0, 0), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P2, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)});
		p1 = createIU(P1, new Version(1, 0, 0), true);
		p2 = createIU(P2, new Version(1, 0, 0), true);
		p2b = createIU(P2, new Version(1, 1, 1), true);

		IRequirementChange changepp1 = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P1, VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.1.1, 1.1.1]"), null, true, false, true));
		IRequiredCapability lifeCyclepp1 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequiredCapability[][] scopepp1 = new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0,1.0.0]"), null, false, false)}};
		pp1 = createIUPatch(PP1, new Version("3.0.0"), true, new IRequirementChange[] {changepp1}, scopepp1, lifeCyclepp1);

		createTestMetdataRepository(new IInstallableUnit[] {p2Feature, p1, p2, p2b, pp1});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		if (!install(profile1, new IInstallableUnit[] {p2Feature}, true, planner, engine).isOK())
			fail("Setup failed");
	}

	public void testInstallPatchSettingAMissingOptionalDependency() {
		//The patch changes the requirement from p2Feature to P1 1.1.1. to be optional, but P1 1.1.1 is missing  
		install(profile1, new IInstallableUnit[] {pp1}, true, planner, engine);
		assertProfileContainsAll("Profile setup incorrectly", profile1, new IInstallableUnit[] {p2Feature, pp1, p2});
	}
}

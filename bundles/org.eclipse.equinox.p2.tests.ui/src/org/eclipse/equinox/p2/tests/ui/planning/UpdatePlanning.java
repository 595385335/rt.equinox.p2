/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.planning;

import org.eclipse.equinox.p2.core.ProvisionException;

import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

public class UpdatePlanning extends AbstractProvisioningUITest {
	IInstallableUnit a1;
	IInstallableUnit a120WithDifferentId;
	IInstallableUnit a130;
	IInstallableUnit a140WithDifferentId;
	IInstallableUnitPatch firstPatchForA1, secondPatchForA1, thirdPatchForA1, patchFora2;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"));
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("A", new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		a120WithDifferentId = createIU("UpdateA", Version.createOSGi(1, 2, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a130 = createIU("A", Version.createOSGi(1, 3, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		a140WithDifferentId = createIU("UpdateForA", Version.createOSGi(1, 4, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, false, update, NO_REQUIRES);
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		firstPatchForA1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);
		secondPatchForA1 = createIUPatch("P", Version.create("2.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);
		thirdPatchForA1 = createIUPatch("P2", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle);

		IRequirementChange change2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequiredCapability lifeCycle2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 3.2.0]"), null, false, false);
		patchFora2 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change2}, new IRequiredCapability[][] {{MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, lifeCycle2);

		// Ensure that all versions, not just the latest, are considered by the UI
		getPolicy().setShowLatestVersionsOnly(false);
	}

	public void testChooseUpdateOverPatch() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		assertTrue("1.0", request.getAddedInstallableUnits().length == 1);
		assertTrue("1.1", request.getAddedInstallableUnits()[0].equals(a130));
		assertTrue("1.2", request.getRemovedInstallableUnits().length == 1);
		assertTrue("1.3", request.getRemovedInstallableUnits()[0].equals(a1));
	}

	public void testForcePatchOverUpdate() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		Update[] updates = op.getPossibleUpdates();
		Update firstPatch = null;
		for (int i = 0; i < updates.length; i++) {
			if (updates[i].replacement.equals(firstPatchForA1)) {
				firstPatch = updates[i];
				break;
			}
		}
		assertNotNull(".99", firstPatch);
		op.setSelectedUpdates(new Update[] {firstPatch});
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		assertTrue("1.0", request.getAddedInstallableUnits().length == 1);
		assertTrue("1.1", request.getAddedInstallableUnits()[0].equals(firstPatchForA1));
		assertTrue("1.2", request.getRemovedInstallableUnits().length == 0);
	}

	public void testRecognizePatchIsInstalled() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		install(firstPatchForA1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		// update was favored, that would happen even if patch was not installed
		assertTrue("1.0", request.getAddedInstallableUnits().length == 1);
		assertTrue("1.1", request.getAddedInstallableUnits()[0].equals(a130));
		// the patch is not being shown to the user because we figured out it was already installed
		// The elements showing are a130 and a120WithDifferentId
		assertEquals("1.2", 2, op.getPossibleUpdates().length);
	}

	public void testChooseNotTheNewest() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, a120WithDifferentId, a130, firstPatchForA1, patchFora2});
		install(a1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		Update[] updates = op.getPossibleUpdates();
		Update notNewest = null;
		for (int i = 0; i < updates.length; i++) {
			if (updates[i].replacement.equals(a120WithDifferentId)) {
				notNewest = updates[i];
				break;
			}
		}
		assertNotNull(".99", notNewest);
		op.setSelectedUpdates(new Update[] {notNewest});
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		// selected was favored
		assertTrue("1.0", request.getAddedInstallableUnits().length == 1);
		assertTrue("1.1", request.getAddedInstallableUnits()[0].equals(a120WithDifferentId));
		// The two updates and the patch were recognized
		assertEquals("1.2", 3, op.getPossibleUpdates().length);
	}

	public void testChooseLatestPatches() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, firstPatchForA1, secondPatchForA1, thirdPatchForA1});
		install(a1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		// the latest two patches were selected
		HashSet chosen = new HashSet();
		assertTrue("1.0", request.getAddedInstallableUnits().length == 2);
		chosen.addAll(Arrays.asList(request.getAddedInstallableUnits()));
		assertTrue("1.1", chosen.contains(secondPatchForA1));
		assertTrue("1.2", chosen.contains(thirdPatchForA1));

		assertEquals("1.2", 3, op.getPossibleUpdates().length);
	}

	public void testLatestHasDifferentId() throws ProvisionException {
		createTestMetdataRepository(new IInstallableUnit[] {a1, firstPatchForA1, secondPatchForA1, thirdPatchForA1, a120WithDifferentId, a130, a140WithDifferentId});
		install(a1, true, false);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(new IInstallableUnit[] {a1}, null);
		op.resolveModal(getMonitor());
		ProfileChangeRequest request = op.getProfileChangeRequest();
		// update 140 was recognized as the latest even though it had a different id
		assertTrue("1.0", request.getAddedInstallableUnits().length == 1);
		assertTrue("1.1", request.getAddedInstallableUnits()[0].equals(a140WithDifferentId));
		// All three patches and all three updates can be chosen
		assertEquals("1.2", 6, op.getPossibleUpdates().length);
	}
}

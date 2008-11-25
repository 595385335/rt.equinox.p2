/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.MarkStartedAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class MarkStartedActionTest extends AbstractProvisioningTest {

	public MarkStartedActionTest(String name) {
		super(name);
	}

	public MarkStartedActionTest() {
		super("");
	}

	public void testExecuteUndo() throws IOException {
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profileProperties.setProperty(IProfile.PROP_CACHE, installFolder.toString());
		IProfile profile = createProfile("test", null, profileProperties);

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(profile);
		File osgiSource = getTestData("1.0", "/testData/eclipseTouchpoint/bundles/org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		File targetPlugins = new File(installFolder, "plugins");
		assertTrue(targetPlugins.mkdir());
		File osgiTarget = new File(targetPlugins, "org.eclipse.osgi_3.4.2.R34x_v20080826-1230.jar");
		copy("2.0", osgiSource, osgiTarget);

		BundleDescription bundleDescription = BundlesAction.createBundleDescription(osgiTarget);
		IArtifactKey key = BundlesAction.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, osgiTarget);
		IInstallableUnit iu = createBundleIU(bundleDescription, osgiTarget.isDirectory(), key);
		bundlePool.addDescriptor(descriptor);

		Map parameters = new HashMap();
		parameters.put(ActionConstants.PARM_PROFILE, profile);
		parameters.put(InstallableUnitPhase.PARM_ARTIFACT_REQUESTS, new ArrayList());
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		touchpoint.initializePhase(null, profile, "test", parameters);
		InstallableUnitOperand operand = new InstallableUnitOperand(null, iu);
		parameters.put("iu", operand.second());
		touchpoint.initializeOperand(profile, operand, parameters);

		parameters.put(ActionConstants.PARM_STARTED, Boolean.TRUE.toString());
		parameters = Collections.unmodifiableMap(parameters);

		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);

		BundleInfo bundleInfo = Util.createBundleInfo(osgiTarget, Util.getManifest(iu.getTouchpointData()));
		manipulator.getConfigData().addBundle(bundleInfo);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));

		MarkStartedAction action = new MarkStartedAction();
		action.execute(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, true));
		action.undo(parameters);
		assertTrue(isMarkedStarted(manipulator, osgiTarget, false));
	}

	private boolean isMarkedStarted(Manipulator manipulator, File osgiTarget, boolean started) throws IOException {
		String location = osgiTarget.toURL().toExternalForm();
		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (location.equals(bundles[i].getLocation()) && (started == bundles[i].isMarkedAsStarted()))
				return true;
		}
		return false;
	}
}
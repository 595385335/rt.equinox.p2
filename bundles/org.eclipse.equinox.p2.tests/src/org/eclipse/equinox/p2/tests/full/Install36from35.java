/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

//Install 3.6 using 3.5
public class Install36from35 extends AbstractReconcilerTest {
	public Install36from35(String string) {
		super(string);
	}

	public int runDirectorToInstall(String message, File installFolder, String sourceRepo, String iuToInstall) {
		File root = new File(TestActivator.getContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-repository", sourceRepo, "-installIU", iuToInstall,

		"-destination", installFolder.getAbsolutePath(), "-profile", "PlatformProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", installFolder.getAbsolutePath(), "-p2.os", Platform.getOS(), "-p2.ws", Platform.getWS(), "-p2.arch", Platform.getOSArch(), "-roaming", "-vmArgs", "-Dosgi.checkConfiguration=true"};

		// command-line if you want to run and allow a remote debugger to connect
		// String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-installIU", iuToInstall, "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"};
		return run(message, command);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.35.platform.archive");
		suite.addTest(new Install36from35("install36From35"));
		return suite;
	}

	public void install36From35() {
		//Create a new installation of 3.6 using 3.5
		File installFolder = getTempFolder();
		System.out.println(installFolder);
		assertEquals(0, runDirectorToInstall("Installing 3.6 from 3.5", new File(installFolder, "eclipse"), "http://download.eclipse.org/eclipse/updates/3.6-I-builds", "org.eclipse.platform.ide"));
		assertEquals(0, installAndRunVerifierBundle35(installFolder));
	}
}

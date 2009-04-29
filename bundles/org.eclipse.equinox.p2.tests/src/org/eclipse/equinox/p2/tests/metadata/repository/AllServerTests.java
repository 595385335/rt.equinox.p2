/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.cert.Certificate;
import junit.framework.*;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Performs all automated metadata repository tests that requires a local test server running
 * on port 8080.
 */
public class AllServerTests extends TestCase {

	private static final String BUNDLE_TESTSERVER = "org.eclipse.equinox.p2.testserver";
	private static final String BUNDLE_EQUINOX_HTTP = "org.eclipse.equinox.http";
	public static final String PROP_TESTSERVER_PORT = "org.osgi.service.http.port";

	static IServiceUI hookedAuthDialog;
	private static ServiceRegistration certificateUIRegistration;
	private static int setUpCounter = 0;
	private static ServiceReference packageAdminRef;

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite("AllServerBasedTestSuite");
		addToSuite(suite);
		return suite;
	}

	public static void addToSuite(TestSuite suite) {
		suite.addTest(new AllServerTests("startServer"));
		// AuthTest *should* run twice to make sure that second attempt produces the same result.
		suite.addTestSuite(AuthTest.class);
		suite.addTestSuite(AuthTest.class);
		suite.addTest(new AllServerTests("stopServer"));
	}

	public void startServer() throws Exception {
		oneTimeSetUp();
	}

	public void stopServer() throws Exception {
		oneTimeTearDown();
	}

	public AllServerTests(String testName) {
		super(testName);
	}

	private static Bundle getBundle(PackageAdmin packageAdmin, String symbolicName) {
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				return bundle;
		}
		return null;
	}

	private static boolean startTransient(PackageAdmin packageAdmin, String bundleName) throws BundleException {
		Bundle bundle = getBundle(packageAdmin, bundleName);
		if (bundle == null)
			return false;
		bundle.start(Bundle.START_TRANSIENT);
		return true;
	}

	private static void stopTransient(PackageAdmin packageAdmin, String bundleName) throws BundleException {
		Bundle bundle = getBundle(packageAdmin, bundleName);
		if (bundle != null)
			bundle.stop(Bundle.STOP_TRANSIENT);
	}

	private static int obtainFreePort() throws IOException {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} finally {
			if (socket != null)
				socket.close();
		}
	}

	public static void oneTimeSetUp() throws Exception {
		BundleContext context = TestActivator.getContext();
		packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin pkgAdmin = (PackageAdmin) context.getService(packageAdminRef);

		// Make sure these are not running
		stopTransient(pkgAdmin, BUNDLE_EQUINOX_HTTP);
		stopTransient(pkgAdmin, BUNDLE_TESTSERVER);

		// Get an available port and assign it the "org.osgi.service.http.port" property. The
		// server will listen to this port and all tests use it to connect.
		System.setProperty(PROP_TESTSERVER_PORT, Integer.toString(obtainFreePort()));

		// Now start them again (with our property settings)
		if (!startTransient(pkgAdmin, BUNDLE_EQUINOX_HTTP))
			throw new IllegalStateException("Unable to start bundle " + BUNDLE_EQUINOX_HTTP);
		if (!startTransient(pkgAdmin, BUNDLE_TESTSERVER))
			throw new IllegalStateException("Unable to start bundle " + BUNDLE_TESTSERVER);

		certificateUIRegistration = context.registerService(IServiceUI.class.getName(), new DelegatingAuthService(), null);
		setUpCounter = 1;
	}

	public static void oneTimeTearDown() throws Exception {
		BundleContext context = TestActivator.getContext();
		certificateUIRegistration.unregister();
		PackageAdmin pkgAdmin = (PackageAdmin) context.getService(packageAdminRef);
		stopTransient(pkgAdmin, BUNDLE_TESTSERVER);
		stopTransient(pkgAdmin, BUNDLE_EQUINOX_HTTP);
		context.ungetService(packageAdminRef);
		setUpCounter = 0;
	}

	/**
	 * Used by tests in the suite to enable that they run individually
	 * @throws Exception
	 */
	public static synchronized void checkSetUp() throws Exception {
		if (setUpCounter == 0) {
			oneTimeSetUp();
			return;
		}
		setUpCounter++;
	}

	/**
	 * Used by tests in the suite to enable that they run individually
	 * @throws Exception
	 */
	public static synchronized void checkTearDown() throws Exception {
		setUpCounter--;
		if (setUpCounter < 0)
			throw new IllegalStateException("Unbalanced setup/teardown");

		if (setUpCounter == 0)
			oneTimeTearDown();
		return;
	}

	static public void setServiceUI(IServiceUI hook) {
		hookedAuthDialog = hook;
	}

	public static class DelegatingAuthService implements IServiceUI {

		public AuthenticationInfo getUsernamePassword(String location) {
			if (hookedAuthDialog != null)
				return hookedAuthDialog.getUsernamePassword(location);
			return null;
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			if (hookedAuthDialog != null)
				return hookedAuthDialog.getUsernamePassword(location, previousInfo);
			return null;
		}

		/**
		 * No need to implement
		 */
		public Certificate[] showCertificates(Certificate[][] certificates) {
			return null;
		}

	}
}
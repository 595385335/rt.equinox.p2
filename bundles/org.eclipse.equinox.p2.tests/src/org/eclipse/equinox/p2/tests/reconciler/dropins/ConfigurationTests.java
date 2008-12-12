/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.util.Iterator;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.update.*;

/*
 * Tests related to the platform configuration before and after reconciliation.
 * 
 * Tests to add and regression tests to add:
 * - ensure there is a platform:base: entry
 * - 222505 - IUs in the dropins only rely on each other and not on things already in the install
 * - ...
 */
public class ConfigurationTests extends AbstractReconcilerTest {

	/*
	 * Constructor for the class.
	 */
	public ConfigurationTests(String name) {
		super(name);
	}

	/*
	 * The list of tests for this class. Order is important since some of them rely
	 * on the state from the previous test run.
	 */
	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.addTest(new ConfigurationTests("testDiscoverOne"));
		suite.addTest(new ConfigurationTests("test_247095"));
		suite.addTest(new ConfigurationTests("test_247095b"));
		suite.addTest(new ConfigurationTests("test_249607"));
		suite.addTest(new ConfigurationTests("test_249898"));
		suite.addTest(new ConfigurationTests("test_232094"));
		suite.addTest(new ConfigurationTests("testSiteEnabled"));
		return suite;
	}

	public void testDiscoverOne() {
		// copy feature and bundle to dropins and reconcile
		assertInitialized();
		File featureFile = getTestData("2.0", "testData/reconciler/features/myFeature_1.0.0");
		add("2.2", "dropins/features", featureFile);
		File bundleFile = getTestData("2.3", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("2.4", "dropins/plugins", bundleFile);
		assertDoesNotExistInBundlesInfo("2.5", "myBundle");
		assertFalse("2.6", isInstalled("myBundle", "1.0.0"));
		reconcile("2.7");

		// make sure the feature is listed in a site in the configuration
		Configuration config = getConfiguration();
		assertFeatureExists("3.0", config, "myFeature", "1.0.0");
		assertTrue("3.1", isInstalled("myBundle", "1.0.0"));
		assertExistsInBundlesInfo("3.2", "myBundle", "1.0.0");

		// cleanup
		remove("99.0", "dropins/plugins", bundleFile.getName());
		remove("99.1", "dropins/features", featureFile.getName());
		reconcile("99.2");
		config = getConfiguration();
		assertFalse("99.4", isInstalled("myFeature.feature", "1.0.0"));
		assertDoesNotExistInBundlesInfo("99.5", "myBundle");
		assertFalse("99.6", isInstalled("myBundle", "1.0.0"));
	}

	/*
	 * Test discovering a site in a platform.xml file and installing the bundles from it.
	 * Then change the site to be disabled and then re-reconcile.
	 */
	public void testSiteEnabled() {
		assertInitialized();
		File temp = getTempFolder();
		toRemove.add(temp);
		Configuration configuration = getConfiguration();
		String siteLocation = new File(temp, "eclipse").toURI().toString();

		File source = getTestData("2.0", "testData/reconciler/ext.jar");
		copy("2.1", source, temp);

		/* this is the entry to add to the site.xml file
		<site enabled="true" policy="USER-EXCLUDE" updateable="false" url="file:C:/share/1/">
			<feature id="bbb.feature" version="1.0.0" />
		</site>
		*/
		assertDoesNotExistInBundlesInfo("3.01", "bbb");
		assertDoesNotExistInBundlesInfo("3.02", "ccc");
		assertFalse("3.11", isInstalled("bbb", "1.0.0"));
		assertFalse("3.12", isInstalled("ccc", "1.0.0"));
		Site site = createSite(Site.POLICY_USER_EXCLUDE, true, false, siteLocation, null);
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("3.2", configuration);
		reconcile("3.3");
		assertExistsInBundlesInfo("3.41", "bbb");
		assertExistsInBundlesInfo("3.42", "ccc");
		assertTrue("3.51", isInstalled("bbb", "1.0.0"));
		assertTrue("3.52", isInstalled("ccc", "1.0.0"));
		// make sure the feature is listed in a site in the configuration
		configuration = getConfiguration();
		assertFeatureExists("3.6", configuration, "bbb.feature", "1.0.0");

		// change the configuration so the site is disabled
		assertTrue("4.0", removeSite(configuration, siteLocation));
		site = createSite(Site.POLICY_USER_EXCLUDE, false, false, siteLocation, null);
		feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("4.1", configuration);
		reconcile("4.2");

		// verify
		assertDoesNotExistInBundlesInfo("5.01", "bbb");
		assertDoesNotExistInBundlesInfo("5.02", "ccc");
		assertFalse("5.11", isInstalled("bbb", "1.0.0"));
		assertFalse("5.12", isInstalled("ccc", "1.0.0"));
	}

	/*
	 * We have a user-include site which lists some plug-ins and has a feature as
	 * a sub-element of the site. When the feature and its plug-ins are removed
	 * from the site we need to ensure the plug-ins are removed from the install.
	 */
	public void test_247095() {
		assertInitialized();
		Configuration configuration = getConfiguration();
		File temp = getTempFolder();
		toRemove.add(temp);
		String siteLocation = null;
		siteLocation = new File(temp, "eclipse").toURI().toString();

		// copy the data to the temp folder
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		/* this is the entry to add to the site.xml file
			<site enabled="true" policy="USER-INCLUDE" updateable="false"
					url="file:C:/share/1/" list="plugins/hello_1.0.0.jar" >
				<feature id="hello_feature" version="1.0.0" />
			</site>
		 */
		Site site = createSite(Site.POLICY_USER_INCLUDE, true, false, siteLocation, new String[] {"plugins/bbb_1.0.0.jar,plugins/ccc_1.0.0.jar"});
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("5.0", configuration);
		reconcile("6.0");
		assertExistsInBundlesInfo("7.0", "bbb", "1.0.0");
		assertTrue("7.1", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("7.2", "ccc", "1.0.0");
		assertTrue("7.3", isInstalled("ccc", "1.0.0"));
		configuration = getConfiguration();
		assertFeatureExists("8.0", configuration, "bbb.feature", "1.0.0");

		// remove the feature and its bundle from the platform.xml but leave the second bundle
		configuration = getConfiguration();
		assertTrue("9.0", removeSite(configuration, siteLocation));
		site = createSite(Site.POLICY_USER_INCLUDE, true, false, siteLocation, new String[] {"plugins/ccc_1.0.0.jar"});
		configuration.add(site);
		save("9.1", configuration);
		reconcile("10.0");
		assertDoesNotExistInBundlesInfo("10.1", "bbb", "1.0.0");
		assertFalse("10.2", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("10.3", "ccc", "1.0.0");
		assertTrue("10.4", isInstalled("ccc", "1.0.0"));

		// cleanup
		configuration = getConfiguration();
		removeSite(configuration, siteLocation);
		save("99.2", configuration);
		reconcile("99.3");
		assertDoesNotExistInBundlesInfo("99.4", "ccc", "1.0.0");
		assertFalse("99.5", isInstalled("ccc", "1.0.0"));
	}

	/*
	 * Same but delete the files from disk. (other test cases doesn't delete the files... simulates
	 * the use of a shared bundle pool)
	 */
	public void test_247095b() {
		assertInitialized();
		Configuration configuration = getConfiguration();
		File temp = getTempFolder();
		toRemove.add(temp);
		String siteLocation = null;
		siteLocation = new File(temp, "eclipse").toURI().toString();

		// copy the data to the temp folder
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		/* this is the entry to add to the site.xml file
			<site enabled="true" policy="USER-INCLUDE" updateable="false"
					url="file:C:/share/1/" list="plugins/hello_1.0.0.jar" >
				<feature id="hello_feature" version="1.0.0" />
			</site>
		 */
		Site site = createSite(Site.POLICY_USER_INCLUDE, true, false, siteLocation, new String[] {"plugins/bbb_1.0.0.jar,plugins/ccc_1.0.0.jar"});
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("5.0", configuration);
		reconcile("6.0");
		assertExistsInBundlesInfo("7.0", "bbb", "1.0.0");
		assertTrue("7.1", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("7.2", "ccc", "1.0.0");
		assertTrue("7.3", isInstalled("ccc", "1.0.0"));
		configuration = getConfiguration();
		assertFeatureExists("8.0", configuration, "bbb.feature", "1.0.0");

		// remove the feature and its bundle from the platform.xml but leave the second bundle
		configuration = getConfiguration();
		assertTrue("9.0", removeSite(configuration, siteLocation));
		site = createSite(Site.POLICY_USER_INCLUDE, true, false, siteLocation, new String[] {"plugins/ccc_1.0.0.jar"});
		configuration.add(site);
		save("9.1", configuration);
		File parent = new File(temp, "eclipse");
		assertTrue("9.2", delete(new File(parent, "plugins/bbb_1.0.0.jar")));
		assertTrue("9.3", delete(new File(parent, "features/bbb.feature_1.0.0")));
		reconcile("10.0");
		assertDoesNotExistInBundlesInfo("10.1", "bbb", "1.0.0");
		assertFalse("10.2", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("10.3", "ccc", "1.0.0");
		assertTrue("10.4", isInstalled("ccc", "1.0.0"));

		// cleanup
		configuration = getConfiguration();
		removeSite(configuration, siteLocation);
		save("99.2", configuration);
		reconcile("99.3");
		assertDoesNotExistInBundlesInfo("99.4", "ccc", "1.0.0");
		assertFalse("99.5", isInstalled("ccc", "1.0.0"));
	}

	/*
	 * There was a problem if we had a user-exclude site policy and a list of
	 * features, we were always adding the features to the excludes list and
	 * therefore they were never installed.
	 */
	public void test_249607() {
		assertInitialized();
		Configuration configuration = getConfiguration();
		File temp = getTempFolder();
		toRemove.add(temp);
		String siteLocation = null;
		siteLocation = new File(temp, "eclipse").toURI().toString();

		// copy the data to the temp folder
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		Site site = createSite(Site.POLICY_USER_EXCLUDE, true, false, siteLocation, new String[] {"plugins/ccc_1.0.0.jar"});
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("2.0", configuration);
		reconcile("2.1");
		assertExistsInBundlesInfo("2.2", "bbb", "1.0.0");
		assertTrue("2.3", isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("2.4", "ccc");
		assertFalse("2.4", isInstalled("ccc", "1.0.0"));
		configuration = getConfiguration();
		assertFeatureExists("3.0", configuration, "bbb.feature", "1.0.0");
		assertTrue("3.1", isInstalled("bbb.feature.feature.group", "1.0.0"));

		// cleanup
		configuration = getConfiguration();
		removeSite(configuration, siteLocation);
		save("99.2", configuration);
		reconcile("99.3");
		assertDoesNotExistInBundlesInfo("99.4", "bbb", "1.0.0");
		assertFalse("99.5", isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("99.6", "ccc", "1.0.0");
		assertFalse("99.7", isInstalled("ccc", "1.0.0"));
	}

	/*
	 * Add a site to the platform.xml, reconcile, ensure its contents are installed, remove the site,
	 * reconcile, ensure the contents are uninstalled.
	 */
	public void test_249898() {
		assertInitialized();
		Configuration configuration = getConfiguration();
		File temp = getTempFolder();
		toRemove.add(temp);
		String siteLocation = new File(temp, "eclipse").toURI().toString();

		// copy the data to the temp folder
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		Site site = createSite(Site.POLICY_USER_INCLUDE, true, false, siteLocation, new String[] {"plugins/bbb_1.0.0.jar,plugins/ccc_1.0.0.jar"});
		Feature feature = createFeature(site, "bbb.feature", "1.0.0", "features/bbb.feature_1.0.0/");
		site.addFeature(feature);
		configuration.add(site);
		save("5.0", configuration);
		reconcile("6.0");
		assertExistsInBundlesInfo("7.0", "bbb", "1.0.0");
		assertTrue("7.1", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("7.2", "ccc", "1.0.0");
		assertTrue("7.3", isInstalled("ccc", "1.0.0"));
		configuration = getConfiguration();
		assertFeatureExists("8.0", configuration, "bbb.feature", "1.0.0");

		// remove the site from the platform.xml
		configuration = getConfiguration();
		assertTrue("9.0", removeSite(configuration, siteLocation));
		save("9.1", configuration);
		reconcile("10.0");
		assertDoesNotExistInBundlesInfo("10.1", "bbb", "1.0.0");
		assertFalse("10.2", isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("10.3", "ccc", "1.0.0");
		assertFalse("10.4", isInstalled("ccc", "1.0.0"));
	}

	/*
	 * Test the case where we have a new site in the platform.xml file which was added
	 * by the user putting a .link file in the links/ folder. Then they delete the link file
	 * and the features and plug-ins should be uninstalled.
	 */
	public void test_232094() {
		assertInitialized();
		File temp = getTempFolder();
		toRemove.add(temp);

		// copy the data to an extension location
		File source = getTestData("1.0", "testData/reconciler/247095");
		copy("1.1", source, temp);

		// create the file in the links/ folder
		createLinkFile("2.0", "myLink", temp.getAbsolutePath());

		// reconcile
		reconcile("3.0");

		// ensure everything was added ok
		assertExistsInBundlesInfo("4.0", "bbb");
		assertTrue("4.1", isInstalled("bbb", "1.0.0"));
		assertExistsInBundlesInfo("4.2", "ccc");
		assertTrue("4.3", isInstalled("ccc", "1.0.0"));
		assertTrue("4.4", isInstalled("bbb.feature.feature.group", "1.0.0"));
		assertFeatureExists("4.5", getConfiguration(), "bbb.feature", "1.0.0");

		// delete the link file from the links/ folder
		removeLinkFile("5.0", "myLink");

		// reconcile
		reconcile("6.0");

		// ensure things were uninstalled
		assertDoesNotExistInBundlesInfo("7.0", "bbb");
		assertFalse("7.1", isInstalled("bbb", "1.0.0"));
		assertDoesNotExistInBundlesInfo("7.2", "ccc");
		assertFalse("7.3", isInstalled("ccc", "1.0.0"));
		assertFalse("7.4", isInstalled("bbb.feature.feature", "1.0.0"));
		boolean found = false;
		for (Iterator iter = getConfiguration().getSites().iterator(); iter.hasNext();) {
			Site site = (Site) iter.next();
			String link = site.getLinkFile();
			if (link != null && link.contains("myLink"))
				found = true;
		}
		assertFalse("7.5", found);

		// cleanup
	}
}

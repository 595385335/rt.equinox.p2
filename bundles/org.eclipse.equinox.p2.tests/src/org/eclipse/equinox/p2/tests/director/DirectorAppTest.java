/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.director.app.Application;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Bundle;

/**
 * Various automated tests of the {@link IDirector} API.
 */
public class DirectorAppTest extends AbstractProvisioningTest {

	/**
	 * runs default director app.
	 */
	private void runDirectorApp(String message, final String[] args) throws Exception {
		Application application = new Application();
		application.start(new IApplicationContext() {

			public void applicationRunning() {
			}

			public Map getArguments() {
				Map arguments = new HashMap();

				arguments.put(IApplicationContext.APPLICATION_ARGS, args);

				return arguments;
			}

			public String getBrandingApplication() {
				return null;
			}

			public Bundle getBrandingBundle() {
				return null;
			}

			public String getBrandingDescription() {
				return null;
			}

			public String getBrandingId() {
				return null;
			}

			public String getBrandingName() {
				return null;
			}

			public String getBrandingProperty(String key) {
				return null;
			}
		});
	}

	/**
	 * creates the director app arguments based on the arguments submitted with bug 248045
	 */
	private String[] getSingleRepoArgs(String message, File metadataRepo, File artifactRepo, File destinationRepo, String installIU) {
		String[] args = new String[0];
		try {
			args = new String[] {"-metadataRepository", metadataRepo.toURL().toExternalForm(), "-artifactRepository", artifactRepo.toURL().toExternalForm(), "-installIU", installIU, "-destination", destinationRepo.toURL().toExternalForm(), "-profile", "PlatformSDKProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", destinationRepo.toURL().toExternalForm(), "-roaming", "-vmargs", "-Declipse.p2.data.area=", destinationRepo.toURL().toExternalForm()};
		} catch (MalformedURLException e) {
			fail(message, e);
		}
		return args;
	}

	/**
	 * creates the director app arguments based on the arguments submitted with bug 248045 but with multiple repositories for both  metadata and artifacts
	 */
	private String[] getMultipleRepoArgs(String message, File metadataRepo1, File metadataRepo2, File artifactRepo1, File artifactRepo2, File destinationRepo, String installIU) {
		String[] args = new String[0];
		try {
			args = new String[] {"-metadataRepository", metadataRepo1.toURL().toExternalForm() + "," + metadataRepo2.toURL().toExternalForm(), "-artifactRepository", artifactRepo1.toURL().toExternalForm() + "," + artifactRepo2.toURL().toExternalForm(), "-installIU", installIU, "-destination", destinationRepo.toURL().toExternalForm(), "-profile", "PlatformSDKProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", destinationRepo.toURL().toExternalForm(), "-roaming", "-vmargs", "-Declipse.p2.data.area=", destinationRepo.toURL().toExternalForm()};
		} catch (MalformedURLException e) {
			fail(message, e);
		}
		return args;
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where both are invalid
	 */
	public void testSingleRepoCreationBothInvalid() {
		//Setup: Create the folders
		File metadataRepo = new File(getTempFolder(), "DirectorApp Metadata");
		File artifactRepo = new File(getTempFolder(), "DirectorApp Artifact");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo);
		delete(artifactRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("1.0", metadataRepo, artifactRepo, destinationRepo, installIU);

		try {
			runDirectorApp("1.1", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("1.2", e);
		}

		//ensures that repositories have not been mistakenly created
		assertFalse("1.3", metadataRepo.exists());
		assertFalse("1.4", artifactRepo.exists());
		assertFalse("1.5", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo);
		delete(artifactRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where the metadata repo is invalid
	 */
	public void testSingleRepoCreationMetadataInvalid() {
		//Setup: create repos
		File metadataRepo = new File(getTempFolder(), "DirectorApp Metadata");
		//Valid repository
		File artifactRepo = getTestData("2.0", "/testData/mirror/mirrorSourceRepo1");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("2.1", metadataRepo, artifactRepo, destinationRepo, installIU);

		try {
			runDirectorApp("2.2", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("2.3", e);
		}

		//ensures that repository has not been mistakenly created
		assertFalse("2.4", metadataRepo.exists());
		assertTrue("2.5", artifactRepo.exists());
		assertFalse("2.6", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where the artifact repo is invalid
	 */
	public void testSingleRepoCreationArtifactInvalid() {
		//Setup: create repos
		//Valid repository
		File metadataRepo = getTestData("3.0", "/testData/mirror/mirrorSourceRepo1");
		File artifactRepo = new File(getTempFolder(), "DirectorApp Artifact");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("3.1", metadataRepo, artifactRepo, destinationRepo, installIU);

		try {
			runDirectorApp("3.2", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("3.3", e);
		}

		//ensures that repository has not been mistakenly created
		assertFalse("3.4", artifactRepo.exists());
		assertTrue("3.5", metadataRepo.exists());
		assertFalse("3.6", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where all are invalid
	 */
	public void testMultipleRepoCreationAllInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		File metadataRepo2 = new File(getTempFolder(), "DirectorApp Metadata2");
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File artifactRepo2 = new File(getTempFolder(), "DirectorApp Artifact2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("4.0", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("4.1", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("4.3", e);
		}

		//ensures that repositories have not been mistakenly created
		assertFalse("4.4", metadataRepo1.exists());
		assertFalse("4.5", metadataRepo2.exists());
		assertFalse("4.6", artifactRepo1.exists());
		assertFalse("4.6", artifactRepo2.exists());
		assertFalse("4.7", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where both metadata repos are invalid
	 */
	public void testMultipleRepoCreationAllMetadataInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		File metadataRepo2 = new File(getTempFolder(), "DirectorApp Metadata2");
		//Valid repositories
		File artifactRepo1 = getTestData("5.0", "/testData/mirror/mirrorSourceRepo1");
		File artifactRepo2 = getTestData("5.1", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("5.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("5.3", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("5.5", e);
		}

		//ensures that repositories have not been mistakenly created
		assertFalse("5.6", metadataRepo1.exists());
		assertFalse("5.7", metadataRepo2.exists());
		assertTrue("5.8", artifactRepo1.exists());
		assertTrue("5.9", artifactRepo2.exists());
		assertFalse("5.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one metadata repo is invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneMetadataInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		//Valid repositories
		File metadataRepo2 = getTestData("6.0", "/testData/mirror/mirrorSourceRepo1");
		File artifactRepo1 = getTestData("6.1", "/testData/mirror/mirrorSourceRepo1");
		File artifactRepo2 = getTestData("6.2", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("6.3", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("6.4", args);
		} catch (ProvisionException e) {
			fail("6.5", e);
		} catch (Exception e) {
			fail("6.6", e);
		}

		//ensures that repositories have not been mistakenly created
		assertFalse("6.7", metadataRepo1.exists());
		assertTrue("6.8", metadataRepo2.exists());
		assertTrue("6.9", artifactRepo1.exists());
		assertTrue("6.10", artifactRepo2.exists());
		assertFalse("6.11", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where both artifact repos are invalid
	 */
	public void testMultipleRepoCreationAllArtifactInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File artifactRepo2 = new File(getTempFolder(), "DirectorApp Artifact2");
		//Valid repositories
		File metadataRepo1 = getTestData("7.0", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo2 = getTestData("7.1", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("7.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("7.3", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("7.5", e);
		}

		//ensures that repositories have not been mistakenly created
		assertTrue("7.6", metadataRepo1.exists());
		assertTrue("7.7", metadataRepo2.exists());
		assertFalse("7.8", artifactRepo1.exists());
		assertFalse("7.9", artifactRepo2.exists());
		assertFalse("7.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one artifact repo is invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneArtifactInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		//Valid repositories
		File artifactRepo2 = getTestData("8.0", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo1 = getTestData("8.1", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo2 = getTestData("8.2", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("8.3", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("8.4", args);
		} catch (ProvisionException e) {
			fail("8.5", e);
		} catch (Exception e) {
			fail("8.6", e);
		}

		//ensures that repositories have not been mistakenly created
		assertTrue("8.7", metadataRepo1.exists());
		assertTrue("8.8", metadataRepo2.exists());
		assertFalse("8.9", artifactRepo1.exists());
		assertTrue("8.10", artifactRepo2.exists());
		assertFalse("8.11", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one artifact repo and only one metadata repo are invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneArtifactOneMetadataInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		//Valid repositories
		File artifactRepo2 = getTestData("9.0", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo2 = getTestData("9.1", "/testData/mirror/mirrorSourceRepo1");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("9.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("9.3", args);
		} catch (ProvisionException e) {
			fail("9.4", e);
		} catch (Exception e) {
			fail("9.5", e);
		}

		//ensures that repositories have not been mistakenly created
		assertFalse("9.6", metadataRepo1.exists());
		assertTrue("9.7", metadataRepo2.exists());
		assertFalse("9.8", artifactRepo1.exists());
		assertTrue("9.9", artifactRepo2.exists());
		assertFalse("9.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(metadataRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and a single artifact repository where all are valid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testSingleRepoCreationNoneInvalid() {
		//Setup: get repositories
		File artifactRepo = getTestData("10.0", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo = getTestData("10.1", "/testData/mirror/mirrorSourceRepo1");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getSingleRepoArgs("10.2", metadataRepo, artifactRepo, destinationRepo, installIU);

		try {
			runDirectorApp("10.3", args);
		} catch (ProvisionException e) {
			fail("10.4", e);
		} catch (Exception e) {
			fail("10.5", e);
		}

		//ensures that repositories have not been mistakenly created
		assertTrue("10.6", metadataRepo.exists());
		assertTrue("10.7", artifactRepo.exists());
		assertFalse("10.8", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where all repos are valid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationNoneInvalid() {
		//Setup: Create the folders
		//Valid repositories
		File artifactRepo1 = getTestData("11.0", "/testData/mirror/mirrorSourceRepo1");
		File metadataRepo1 = getTestData("11.1", "/testData/mirror/mirrorSourceRepo1");
		File artifactRepo2 = getTestData("11.2", "/testData/mirror/mirrorSourceRepo2");
		File metadataRepo2 = getTestData("11.3", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("11.4", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("11.5", args);
		} catch (ProvisionException e) {
			fail("11.6", e);
		} catch (Exception e) {
			fail("11.7", e);
		}

		//ensures that repositories have not been mistakenly created
		assertTrue("11.8", metadataRepo1.exists());
		assertTrue("11.9", metadataRepo2.exists());
		assertTrue("11.10", artifactRepo1.exists());
		assertTrue("11.11", artifactRepo2.exists());
		assertFalse("11.12", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(destinationRepo);
	}
}

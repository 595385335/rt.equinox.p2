/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PermissiveSlicerTest extends AbstractProvisioningTest {
	private IMetadataRepository repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File repoFile = getTestData("Repo for permissive slicer test", "testData/permissiveSlicer");
		repo = getMetadataRepositoryManager().loadRepository(repoFile.toURI(), new NullProgressMonitor());
	}

	public void testSliceRCPOut() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), true, false, true, false, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(66, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		assertEquals(1, queryResultSize(result.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	//Test with and without optional pieces
	public void testSliceRCPWithOptionalPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), false, false, true, false, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(64, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithIgnoringGreed() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), false, true, true, false, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(64, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testSliceRCPWithFilter() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, true, false, false, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(0, queryResultSize(result.query(new InstallableUnitQuery("org.eclipse.swt.motif.linux.x86"), new NullProgressMonitor())));
		assertEquals(34, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testStrictDependency() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, true, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(0, queryResultSize(result.query(new InstallableUnitQuery("org.eclipse.ecf"), new NullProgressMonitor())));
		assertEquals(29, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testExtractPlatformIndependentPieces() {
		PermissiveSlicer slicer = new PermissiveSlicer(repo, new Properties(), true, false, false, false, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryable result = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor());
		assertNotNull(result);
		queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor()));
		assertEquals(32, queryResultSize(result.query(InstallableUnitQuery.ANY, new NullProgressMonitor())));
		assertEquals(1, queryResultSize(result.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor())));
		//		assertOK("1.0", slicer.getStatus());
	}

	public void testMetaRequirements() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		PermissiveSlicer slicer = new PermissiveSlicer(createTestMetdataRepository(new IInstallableUnit[] {a, act1}), new Properties(), true, false, false, false, false);
		IQueryable result = slicer.slice(new IInstallableUnit[] {a}, new NullProgressMonitor());
		assertEquals(1, queryResultSize(result.query(new InstallableUnitQuery("Action1"), null)));
	}

	public void testValidateIU() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, true, false);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.swt.cocoa.macosx"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		assertNull(slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor()));
		assertNotOK(slicer.getStatus());
	}

	public void testMissingNecessaryPiece() {
		IRequiredCapability[] req = createRequiredCapabilities("B", "B", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit iuA = createIU("A", DEFAULT_VERSION, null, req, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true);
		PermissiveSlicer slicer = new PermissiveSlicer(createTestMetdataRepository(new IInstallableUnit[] {iuA}), new Properties(), true, false, false, false, false);
		IQueryable result = slicer.slice(new IInstallableUnit[] {iuA}, new NullProgressMonitor());
		assertNotNull(result);
		assertNotOK(slicer.getStatus());
	}

	public void testExtractOnlyPlatformSpecificForOnePlatform() {
		Properties p = new Properties();
		p.setProperty("osgi.os", "win32");
		p.setProperty("osgi.ws", "win32");
		p.setProperty("osgi.arch", "x86");
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, false, false, true);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryResult resultCollector = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor()).query(InstallableUnitQuery.ANY, new NullProgressMonitor());
		assertEquals(3, queryResultSize(resultCollector));
	}

	public void testExtractOnlyPlatformSpecific() {
		Properties p = new Properties();
		PermissiveSlicer slicer = new PermissiveSlicer(repo, p, true, false, true, false, true);
		IQueryResult c = repo.query(new InstallableUnitQuery("org.eclipse.rcp.feature.group"), new NullProgressMonitor());
		IInstallableUnit iu = (IInstallableUnit) c.iterator().next();
		IQueryResult resultCollector = slicer.slice(new IInstallableUnit[] {iu}, new NullProgressMonitor()).query(InstallableUnitQuery.ANY, new NullProgressMonitor());
		assertEquals(35, queryResultSize(resultCollector));
	}

}

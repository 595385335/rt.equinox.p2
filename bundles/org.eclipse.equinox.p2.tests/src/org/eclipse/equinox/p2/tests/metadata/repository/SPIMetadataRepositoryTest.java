/******************************************************************************* 
* Copyright (c) 2008, 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.MatchQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test API of the metadata interfaces with an SPI implementation.
 */
public class SPIMetadataRepositoryTest extends AbstractProvisioningTest {
	protected File repoLocation;

	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		repoLocation = new File(tempDir, "SPILocalMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
		repoLocation.mkdir();
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		delete(repoLocation);
		super.tearDown();
	}

	class SPIRequiredCapability extends MatchQuery implements IRequiredCapability {
		LDAPQuery filter;
		String name;
		String namespace;
		VersionRange versionRange;
		boolean isGreedy;
		int min;
		int max;

		public SPIRequiredCapability(String namespace, String name, VersionRange versionRange, String filter, boolean isGreedy, boolean isMultiple, boolean isOptional) {
			this.namespace = namespace;
			this.name = name;
			this.versionRange = versionRange;
			setFilter(filter);
			this.isGreedy = isGreedy;
			this.min = isOptional ? 0 : 1;
			this.max = 1;
		}

		public SPIRequiredCapability(String namespace, String name, VersionRange versionRange) {
			this.namespace = namespace;
			this.name = name;
			this.versionRange = versionRange;
		}

		public IQuery getFilter() {
			return this.filter;
		}

		public String getName() {
			return this.name;
		}

		public String getNamespace() {
			return this.namespace;
		}

		public VersionRange getRange() {
			return this.versionRange;
		}

		public boolean isGreedy() {
			return isGreedy;
		}

		public void setFilter(String filter) {
			if (filter != null) {
				this.filter = new LDAPQuery(filter);
			}
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof IRequiredCapability))
				return false;
			final IRequiredCapability other = (IRequiredCapability) obj;
			if (filter == null) {
				if (other.getFilter() != null)
					return false;
			} else if (!filter.equals(other.getFilter()))
				return false;
			if (!name.equals(other.getName()))
				return false;
			if (!namespace.equals(other.getNamespace()))
				return false;
			if (other.getMin() != this.getMin())
				return false;
			if (other.getMax() != this.getMax())
				return false;
			if (!versionRange.equals(other.getRange()))
				return false;
			return true;
		}

		public boolean isNegation() {
			return false;
		}

		public boolean satisfiedBy(IProvidedCapability cap) {
			if (getName() == null || !getName().equals(cap.getName()))
				return false;
			if (getNamespace() == null || !getNamespace().equals(cap.getNamespace()))
				return false;
			return getRange().isIncluded(cap.getVersion());
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}

		public IQuery getMatches() {
			return this;
		}

		public boolean isMatch(Object object) {
			if (!(object instanceof IInstallableUnit))
				return false;
			IInstallableUnit candidate = (IInstallableUnit) object;
			if (!candidate.satisfies((IRequirement) this))
				return false;
			return true;
		}
	}

	class SPIProvidedCapability implements IProvidedCapability {

		String namespace = null;
		String name = null;
		Version version = null;

		public SPIProvidedCapability(String namespace, String name, Version version) {
			this.namespace = namespace;
			this.name = name;
			this.version = version;
		}

		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (!(other instanceof IProvidedCapability))
				return false;
			IProvidedCapability otherCapability = (IProvidedCapability) other;
			if (!(namespace.equals(otherCapability.getNamespace())))
				return false;
			if (!(name.equals(otherCapability.getName())))
				return false;
			return true;
		}

		public String getName() {
			return this.name;
		}

		public String getNamespace() {
			return this.namespace;
		}

		public Version getVersion() {
			return this.version;
		}

		public boolean satisfies(IRequirement candidate) {
			return false;
		}

	}

	class SPIInstallableUnit implements IInstallableUnit {

		List artifacts = new ArrayList();
		List fragments = new ArrayList();
		List requiredCapabilities = new ArrayList();
		List providedCapabilities = new ArrayList();
		List touchpointData = new ArrayList();
		ICopyright copyright = null;
		LDAPQuery filter = null;
		String id = null;
		ILicense[] license = null;
		Map properties = new HashMap();
		ITouchpointType touchpointType = null;
		IUpdateDescriptor updateDescriptor = null;
		Version version = null;
		boolean isFragment;
		boolean isResolved;
		boolean isSingleton;

		public SPIInstallableUnit(String id, Version version) {
			this.id = id;
			this.version = version;
		}

		public void addProvidedCapability(IProvidedCapability providedCapability) {
			this.providedCapabilities.add(providedCapability);
		}

		public IArtifactKey[] getArtifacts() {
			return (IArtifactKey[]) this.artifacts.toArray(new IArtifactKey[artifacts.size()]);
		}

		public ICopyright getCopyright() {
			return this.copyright;
		}

		public IQuery getFilter() {
			return this.filter;
		}

		public IInstallableUnitFragment[] getFragments() {
			return (IInstallableUnitFragment[]) this.fragments.toArray(new IInstallableUnitFragment[fragments.size()]);
		}

		public String getId() {
			return this.id;
		}

		public ILicense[] getLicenses() {
			return this.license;
		}

		public Map getProperties() {
			return this.properties;
		}

		public String getProperty(String key) {
			return (String) this.properties.get(key);
		}

		public IProvidedCapability[] getProvidedCapabilities() {
			return (IProvidedCapability[]) this.providedCapabilities.toArray(new IProvidedCapability[profilesToRemove.size()]);
		}

		public IRequiredCapability[] getRequiredCapabilities() {
			return (IRequiredCapability[]) this.requiredCapabilities.toArray(new IRequiredCapability[requiredCapabilities.size()]);
		}

		public ITouchpointData[] getTouchpointData() {
			return (ITouchpointData[]) this.touchpointData.toArray(new ITouchpointData[touchpointData.size()]);
		}

		public ITouchpointType getTouchpointType() {
			if (this.touchpointType == null)
				return ITouchpointType.NONE;
			return this.touchpointType;
		}

		public IUpdateDescriptor getUpdateDescriptor() {
			return this.updateDescriptor;
		}

		public Version getVersion() {
			return this.version;
		}

		public boolean isFragment() {
			return this.isFragment;
		}

		public boolean isResolved() {
			return this.isResolved;
		}

		public boolean isSingleton() {
			return this.isSingleton;
		}

		public boolean satisfies(IRequirement candidate) {
			IProvidedCapability[] provides = getProvidedCapabilities();
			for (int i = 0; i < provides.length; i++)
				if (provides[i].satisfies(candidate))
					return true;
			return false;
		}

		public IInstallableUnit unresolved() {
			return this;
		}

		public int compareTo(Object toCompareTo) {
			if (!(toCompareTo instanceof IInstallableUnit)) {
				return -1;
			}
			IInstallableUnit other = (IInstallableUnit) toCompareTo;
			if (getId().compareTo(other.getId()) == 0)
				return (getVersion().compareTo(other.getVersion()));
			return getId().compareTo(other.getId());
		}

		public IRequiredCapability[] getMetaRequiredCapabilities() {
			return null;
		}

	}

	class SPITouchpointData implements ITouchpointData {

		Map instructions = new HashMap();

		public ITouchpointInstruction getInstruction(String instructionKey) {
			return (ITouchpointInstruction) instructions.get(instructionKey);
		}

		public void addInstruction(String instructionKey, ITouchpointInstruction instruction) {
			this.instructions.put(instructionKey, instruction);
		}

		public Map getInstructions() {
			return this.instructions;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ITouchpointData))
				return false;
			final ITouchpointData other = (ITouchpointData) obj;
			if (instructions == null) {
				if (other.getInstructions() != null)
					return false;
			} else if (!instructions.equals(other.getInstructions()))
				return false;
			return true;
		}
	}

	class SPITouchpointInstruction implements ITouchpointInstruction {

		private String body;
		private String importAttribute;

		public SPITouchpointInstruction(String body, String importAttribute) {
			this.body = body;
			this.importAttribute = importAttribute;
		}

		public String getBody() {
			return this.body;
		}

		public String getImportAttribute() {
			return this.importAttribute;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ITouchpointInstruction))
				return false;
			ITouchpointInstruction other = (ITouchpointInstruction) obj;
			if (body == null) {
				if (other.getBody() != null)
					return false;
			} else if (!body.equals(other.getBody()))
				return false;
			if (importAttribute == null) {
				if (other.getImportAttribute() != null)
					return false;
			} else if (!importAttribute.equals(other.getImportAttribute()))
				return false;
			return true;
		}
	}

	class SPITouchpointType implements ITouchpointType {

		private String id;
		private Version version;

		public SPITouchpointType(String id, Version version) {
			this.id = id;
			this.version = version;
		}

		public String getId() {
			return this.id;
		}

		public Version getVersion() {
			return this.version;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (super.equals(obj))
				return true;
			if (obj == null || !(obj instanceof ITouchpointType))
				return false;
			ITouchpointType other = (ITouchpointType) obj;
			return id.equals(other.getId()) && version.equals(other.getVersion());
		}
	}

	class SPIRequirementChange implements IRequirementChange {

		private IRequiredCapability applyOn;
		private IRequiredCapability newValue;

		public SPIRequirementChange(IRequiredCapability applyOn2, IRequiredCapability newValue2) {
			if (applyOn2 == null && newValue2 == null)
				throw new IllegalArgumentException();
			this.applyOn = applyOn2;
			this.newValue = newValue2;
		}

		public IRequiredCapability applyOn() {
			return applyOn;
		}

		public IRequiredCapability newValue() {
			return newValue;
		}

		public boolean matches(IRequiredCapability toMatch) {
			if (!toMatch.getNamespace().equals(applyOn.getNamespace()))
				return false;
			if (!toMatch.getName().equals(applyOn.getName()))
				return false;
			if (toMatch.getRange().equals(applyOn.getRange()))
				return true;

			return intersect(toMatch.getRange(), applyOn.getRange()) == null ? false : true;
		}

		private VersionRange intersect(VersionRange r1, VersionRange r2) {
			Version resultMin = null;
			boolean resultMinIncluded = false;
			Version resultMax = null;
			boolean resultMaxIncluded = false;

			int minCompare = r1.getMinimum().compareTo(r2.getMinimum());
			if (minCompare < 0) {
				resultMin = r2.getMinimum();
				resultMinIncluded = r2.getIncludeMinimum();
			} else if (minCompare > 0) {
				resultMin = r1.getMinimum();
				resultMinIncluded = r1.getIncludeMinimum();
			} else if (minCompare == 0) {
				resultMin = r1.getMinimum();
				resultMinIncluded = r1.getIncludeMinimum() && r2.getIncludeMinimum();
			}

			int maxCompare = r1.getMaximum().compareTo(r2.getMaximum());
			if (maxCompare > 0) {
				resultMax = r2.getMaximum();
				resultMaxIncluded = r2.getIncludeMaximum();
			} else if (maxCompare < 0) {
				resultMax = r1.getMaximum();
				resultMaxIncluded = r1.getIncludeMaximum();
			} else if (maxCompare == 0) {
				resultMax = r1.getMaximum();
				resultMaxIncluded = r1.getIncludeMaximum() && r2.getIncludeMaximum();
			}

			int resultRangeComparison = resultMin.compareTo(resultMax);
			if (resultRangeComparison < 0)
				return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
			else if (resultRangeComparison == 0 && resultMinIncluded == resultMaxIncluded)
				return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);
			else
				return null;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((applyOn == null) ? 0 : applyOn.hashCode());
			result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof IRequirementChange))
				return false;
			final IRequirementChange other = (IRequirementChange) obj;
			if (applyOn == null) {
				if (other.applyOn() != null)
					return false;
			} else if (!applyOn.equals(other.applyOn()))
				return false;
			if (newValue == null) {
				if (other.newValue() != null)
					return false;
			} else if (!newValue.equals(other.newValue()))
				return false;
			return true;
		}

		public String toString() {
			return applyOn + " --> " + newValue; //$NON-NLS-1$
		}

	}

	class SPILicense implements ILicense {

		private String body;
		private URI location;
		private String uuid;

		public SPILicense(String body, URI location) {
			this.body = body;
			this.location = location;
		}

		public String getBody() {
			return this.body;
		}

		public String getUUID() {
			if (uuid == null)
				uuid = this.calculateLicenseDigest().toString(16);
			return uuid;
		}

		public URI getLocation() {
			return this.location;
		}

		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null)
				return false;
			if (obj instanceof ILicense) {
				ILicense other = (ILicense) obj;
				if (other.getUUID().equals(getUUID()))
					return true;
			}
			return false;
		}

		private BigInteger calculateLicenseDigest() {
			String message = normalize(getBody());
			try {
				MessageDigest algorithm = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
				algorithm.reset();
				algorithm.update(message.getBytes("UTF-8")); //$NON-NLS-1$
				byte[] digestBytes = algorithm.digest();
				return new BigInteger(1, digestBytes);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		private String normalize(String license) {
			String text = license.trim();
			StringBuffer result = new StringBuffer();
			int length = text.length();
			for (int i = 0; i < length; i++) {
				char c = text.charAt(i);
				boolean foundWhitespace = false;
				while (Character.isWhitespace(c) && i < length) {
					foundWhitespace = true;
					c = text.charAt(++i);
				}
				if (foundWhitespace)
					result.append(' ');
				if (i < length)
					result.append(c);
			}
			return result.toString();
		}

	}

	class AllAcceptingQuery extends MatchQuery {
		public boolean isMatch(Object candidate) {
			return true;
		}
	}

	/**
	 * This test cases creates an SPI implementation of an IU and writes it to a repository.  
	 * If the repository is Cached, it reads back the SPI implementation. If the repository is
	 * not cached, it reads back the default (InstallableUnit) implementation.
	 * 
	 * @throws ProvisionException
	 */
	public void testSPIMetadataIU() throws ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IMetadataRepository repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		repo.addInstallableUnits(new IInstallableUnit[] {new SPIInstallableUnit("foo", Version.createOSGi(1, 1, 1))});

		Collector collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		Collection collection = collector.toCollection();
		assertEquals(1, collection.size());

		assertTrue("Repo contains SPI IU)", collection.iterator().next() instanceof SPIInstallableUnit);

		repo = manager.refreshRepository(repoLocation.toURI(), null);
		collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		collection = collector.toCollection();
		assertEquals(1, collection.size());

		assertTrue("Refreshed repo contains default IU", collection.iterator().next() instanceof InstallableUnit);
	}

	/**
	 * This test cases creates an SPI IU and adds a default provided capability. It ensures that 
	 * you can write this type of repository and read it back again.  If you read it back, and it is cached,
	 * you get the SPI IU, otherwise you get the default (InstallableUnit) IU.
	 * 
	 * @throws ProvisionException
	 */
	public void testProvidedCapabilitywithSPI_IU() throws ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");

		IMetadataRepository repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability("foo", "bar", Version.createOSGi(1, 0, 0));

		SPIInstallableUnit spiInstallableUnit = new SPIInstallableUnit("foo", Version.createOSGi(1, 1, 1));
		spiInstallableUnit.addProvidedCapability(providedCapability);
		repo.addInstallableUnits(new IInstallableUnit[] {spiInstallableUnit});

		Collector collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		Collection collection = collector.toCollection();
		assertEquals(1, collection.size());

		IInstallableUnit spiUnit = (IInstallableUnit) collection.iterator().next();
		assertTrue("Repo contains SPI IU)", spiUnit instanceof SPIInstallableUnit);
		assertEquals(spiUnit.getProvidedCapabilities().length, 1);
		assertTrue(spiUnit.getProvidedCapabilities()[0] instanceof ProvidedCapability);

		repo = manager.refreshRepository(repoLocation.toURI(), null);
		collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		collection = collector.toCollection();
		assertEquals(1, collection.size());

		IInstallableUnit defaultUnit = (IInstallableUnit) collection.iterator().next();
		assertTrue("Repo contains SPI IU)", defaultUnit instanceof InstallableUnit);
		assertEquals(spiUnit.getProvidedCapabilities().length, 1);
		assertTrue(spiUnit.getProvidedCapabilities()[0] instanceof ProvidedCapability);
	}

	/**
	 * This test cases creates an IU and adds an SPI  required capability. It ensures that 
	 * you can write this type of repository and read it back again.  If you read it back, and it is cached,
	 * you get the SPI Required Capability, otherwise you get the default RequiredCapability.
	 * 
	 * @throws ProvisionException
	 */
	public void testSPIRequiredCapability() throws ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");

		IMetadataRepository repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("foo");
		iuDescription.setVersion(Version.createOSGi(1, 1, 1));
		IRequiredCapability spiRequiredCapability = new SPIRequiredCapability("com.example", "bar", new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), true));
		Collection list = new ArrayList();
		list.add(spiRequiredCapability);
		iuDescription.addRequiredCapabilities(list);

		repo.addInstallableUnits(new IInstallableUnit[] {MetadataFactory.createInstallableUnit(iuDescription)});

		Collector collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		Collection collection = collector.toCollection();
		assertEquals(1, collection.size());

		IInstallableUnit unit = (IInstallableUnit) collection.iterator().next();
		assertEquals(unit.getRequiredCapabilities().length, 1);
		assertTrue(unit.getRequiredCapabilities()[0] instanceof SPIRequiredCapability);

		repo = manager.refreshRepository(repoLocation.toURI(), null);
		collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		collection = collector.toCollection();
		assertEquals(1, collection.size());

		unit = (IInstallableUnit) collection.iterator().next();
		assertEquals(unit.getRequiredCapabilities().length, 1);
		assertTrue(unit.getRequiredCapabilities()[0] instanceof RequiredCapability);
		assertTrue(((IRequiredCapability) unit.getRequiredCapabilities()[0]).getName().equals("bar"));
	}

	/**
	 * This tests the .equals method in many of the metadata classes.  This test
	 * case ensures that an SPI implementation .equals() the default one.
	 * @throws ProvisionException
	 */
	public void testSPIEquals() throws ProvisionException, URISyntaxException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");

		IMetadataRepository repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		InstallableUnitPatchDescription iuPatchDescription = new InstallableUnitPatchDescription();
		iuDescription.setId("foo");
		iuDescription.setVersion(Version.createOSGi(1, 1, 1));

		SPIRequiredCapability spiRequiredCapability1 = new SPIRequiredCapability("com.example", "bar", new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), true), "bar", true, true, true);
		IRequiredCapability requiredCapability1 = MetadataFactory.createRequiredCapability("com.example2", "foo", new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), true), "bar", false, false, false);

		SPIRequirementChange spiRequirementChange = new SPIRequirementChange(spiRequiredCapability1, requiredCapability1);
		iuPatchDescription.setRequirementChanges(new IRequirementChange[] {spiRequirementChange});

		IRequiredCapability spiRequiredCapability = new SPIRequiredCapability("com.example", "bar", new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), true), "bar", true, true, true);
		IProvidedCapability spiProvidedCapability = new SPIProvidedCapability("bar", "foo", Version.createOSGi(1, 1, 1));

		ITouchpointData spiTouchpointData = new SPITouchpointData();
		ITouchpointInstruction spiTouchpointInstruction = new SPITouchpointInstruction("the body", "the import attribute");
		((SPITouchpointData) spiTouchpointData).addInstruction("foo", spiTouchpointInstruction);
		iuDescription.addTouchpointData(spiTouchpointData);

		SPILicense spiLicense = new SPILicense("body", new URI("http://example.com"));
		iuDescription.setLicenses(new ILicense[] {spiLicense});

		SPITouchpointType spiTouchpointType = new SPITouchpointType("foo", Version.createOSGi(3, 3, 3));
		iuDescription.setTouchpointType(spiTouchpointType);

		Collection requiredCapabilityList = new ArrayList();
		requiredCapabilityList.add(spiRequiredCapability);
		iuDescription.addRequiredCapabilities(requiredCapabilityList);

		Collection providedCapabilityList = new ArrayList();
		providedCapabilityList.add(spiProvidedCapability);
		iuDescription.addProvidedCapabilities(providedCapabilityList);

		repo.addInstallableUnits(new IInstallableUnit[] {MetadataFactory.createInstallableUnit(iuDescription), MetadataFactory.createInstallableUnitPatch(iuPatchDescription)});

		repo = manager.refreshRepository(repoLocation.toURI(), null);
		Collector collector = repo.query(new AllAcceptingQuery(), new NullProgressMonitor());

		Collection collection = collector.toCollection();
		assertEquals(2, collection.size());
		Iterator iterator = collection.iterator();

		IInstallableUnit unit = null;
		IInstallableUnitPatch patchUnit = null;
		while (iterator.hasNext()) {
			Object o = iterator.next();
			if (o instanceof IInstallableUnitPatch) {
				patchUnit = (IInstallableUnitPatch) o;
			} else if (o instanceof IInstallableUnit) {
				unit = (IInstallableUnit) o;
			}
		}
		assertFalse(unit == null);
		assertFalse(patchUnit == null);

		assertEquals(unit.getRequiredCapabilities().length, 1);
		assertEquals(unit.getProvidedCapabilities().length, 1);
		assertEquals(unit.getTouchpointData().length, 1);
		assertEquals(((IRequiredCapability) unit.getRequiredCapabilities()[0]).getNamespace(), spiRequiredCapability.getNamespace());
		assertEquals(((IRequiredCapability) unit.getRequiredCapabilities()[0]).getName(), spiRequiredCapability.getName());
		assertEquals(((IRequiredCapability) unit.getRequiredCapabilities()[0]).getMin(), spiRequiredCapability.getMin());
		assertEquals(((IRequiredCapability) unit.getRequiredCapabilities()[0]).getMax(), spiRequiredCapability.getMax());
		assertEquals(unit.getProvidedCapabilities()[0], spiProvidedCapability);
		assertEquals(unit.getTouchpointData()[0], spiTouchpointData);
		assertEquals(unit.getTouchpointType(), spiTouchpointType);
		assertEquals(unit.getLicenses()[0], spiLicense);
		assertEquals(spiProvidedCapability, unit.getProvidedCapabilities()[0]);
		assertEquals(spiTouchpointData, unit.getTouchpointData()[0]);
		assertEquals(spiTouchpointType, unit.getTouchpointType());
		assertEquals(spiLicense, unit.getLicenses()[0]);

		assertEquals(patchUnit.getRequirementsChange().length, 1);
		assertEquals(patchUnit.getRequirementsChange()[0], spiRequirementChange);
		assertEquals(spiRequirementChange, patchUnit.getRequirementsChange()[0]);

		// Check to make sure the actual objects are not equal.  This is because the repo has 
		// been refreshed, and re-parsed, thus using the default implementations.
		assertFalse(spiTouchpointData == unit.getTouchpointData()[0]);
		assertFalse(spiRequiredCapability == unit.getRequiredCapabilities()[0]);
		assertFalse(spiProvidedCapability == unit.getProvidedCapabilities()[0]);
		assertFalse(spiTouchpointType == unit.getTouchpointType());
		assertFalse(spiLicense == unit.getLicenses()[0]);
	}
}

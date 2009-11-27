/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Prashant Deva - Bug 194674 [prov] Provide write access to metadata repository
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.RepositoryReference;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.AbstractMetadataRepository.RepositoryState;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.xml.sax.*;

/**
 * This class reads and writes provisioning metadata.
 */
public class MetadataRepositoryIO {

	/**
	 * Reads metadata from the given stream, and returns the contained array
	 * of abstract metadata repositories.
	 * This method performs buffering, and closes the stream when finished.
	 */
	public IMetadataRepository read(URL location, InputStream input, IProgressMonitor monitor) throws ProvisionException {
		BufferedInputStream bufferedInput = null;
		try {
			try {
				bufferedInput = new BufferedInputStream(input);

				Parser repositoryParser = new Parser(Activator.getContext(), Activator.ID);
				repositoryParser.parse(input, monitor);
				IStatus result = repositoryParser.getStatus();
				switch (result.getSeverity()) {
					case IStatus.CANCEL :
						throw new OperationCanceledException();
					case IStatus.ERROR :
						throw new ProvisionException(result);
					case IStatus.WARNING :
					case IStatus.INFO :
						LogHelper.log(result);
				}
				return repositoryParser.getRepository();
			} finally {
				if (bufferedInput != null)
					bufferedInput.close();
			}
		} catch (IOException ioe) {
			String msg = NLS.bind(Messages.io_failedRead, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, ioe));
		}
	}

	/**
	 *
	 */
	public void write(IMetadataRepository repository, OutputStream output) throws IOException {
		OutputStream bufferedOutput = null;
		try {
			bufferedOutput = new BufferedOutputStream(output);
			Writer repositoryWriter = new Writer(bufferedOutput, repository.getClass());
			repositoryWriter.write(repository);
		} finally {
			if (bufferedOutput != null) {
				bufferedOutput.close();
			}
		}
	}

	private interface XMLConstants extends org.eclipse.equinox.internal.p2.metadata.repository.io.XMLConstants {

		// Constants defining the structure of the XML for a MetadataRepository

		// A format version number for metadata repository XML.
		public static final Version COMPATIBLE_VERSION = Version.createOSGi(1, 0, 0);
		public static final Version CURRENT_VERSION = Version.createOSGi(1, 1, 0);
		public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true, Version.createOSGi(2, 0, 0), false);

		// Constants for processing Instructions
		public static final String PI_REPOSITORY_TARGET = "metadataRepository"; //$NON-NLS-1$

		// Constants for metadata repository elements
		public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$

	}

	protected XMLWriter.ProcessingInstruction[] createPI(Class repositoryClass) {
		//TODO We should remove this processing instruction, but currently old clients rely on this. See bug 210450.
		return new XMLWriter.ProcessingInstruction[] {XMLWriter.ProcessingInstruction.makeTargetVersionInstruction(XMLConstants.PI_REPOSITORY_TARGET, XMLConstants.CURRENT_VERSION)};
	}

	// XML writer for a IMetadataRepository
	protected class Writer extends MetadataWriter implements XMLConstants {

		public Writer(OutputStream output, Class repositoryClass) throws IOException {
			super(output, createPI(repositoryClass));
		}

		/**
		 * Write the given metadata repository to the output stream.
		 */
		public void write(IMetadataRepository repository) {
			start(REPOSITORY_ELEMENT);
			attribute(NAME_ATTRIBUTE, repository.getName());
			attribute(TYPE_ATTRIBUTE, repository.getType());
			attribute(VERSION_ATTRIBUTE, repository.getVersion());
			attributeOptional(PROVIDER_ATTRIBUTE, repository.getProvider());
			attributeOptional(DESCRIPTION_ATTRIBUTE, repository.getDescription()); // TODO: could be cdata?

			writeProperties(repository.getProperties());
			Collector units = repository.query(InstallableUnitQuery.ANY, new Collector(), null);
			if (repository instanceof LocalMetadataRepository) {
				Set references = ((LocalMetadataRepository) repository).repositories;
				writeRepositoryReferences(references.iterator(), references.size());
			}
			writeInstallableUnits(units.iterator(), units.size());

			end(REPOSITORY_ELEMENT);
			flush();
		}

		/**
		 * Writes a list of {@link RepositoryReference}.
		 * @param references An Iterator of {@link RepositoryReference}.
		 * @param size The number of references  to write
		 */
		protected void writeRepositoryReferences(Iterator references, int size) {
			if (size == 0)
				return;
			start(REPOSITORY_REFERENCES_ELEMENT);
			attribute(COLLECTION_SIZE_ATTRIBUTE, size);
			while (references.hasNext())
				writeRepositoryReference((RepositoryReference) references.next());
			end(REPOSITORY_REFERENCES_ELEMENT);
		}

		private void writeRepositoryReference(RepositoryReference reference) {
			start(REPOSITORY_REFERENCE_ELEMENT);
			attribute(URI_ATTRIBUTE, reference.Location.toString());

			try {
				// we write the URL attribute for backwards compatibility with 3.4.x
				// this attribute should be removed if we make a breaking format change.
				attribute(URL_ATTRIBUTE, URIUtil.toURL(reference.Location).toExternalForm());
			} catch (MalformedURLException e) {
				attribute(URL_ATTRIBUTE, reference.Location.toString());
			}

			attribute(TYPE_ATTRIBUTE, Integer.toString(reference.Type));
			attribute(OPTIONS_ATTRIBUTE, Integer.toString(reference.Options));
			end(REPOSITORY_REFERENCE_ELEMENT);
		}
	}

	/*
	 * 	Parser for the contents of a metadata repository,
	 * 	as written by the Writer class.
	 */
	private class Parser extends MetadataParser implements XMLConstants {

		private IMetadataRepository theRepository = null;

		public Parser(BundleContext context, String bundleId) {
			super(context, bundleId);
		}

		public synchronized void parse(InputStream stream, IProgressMonitor monitor) throws IOException {
			this.status = null;
			setProgressMonitor(monitor);
			monitor.beginTask(Messages.repo_loading, IProgressMonitor.UNKNOWN);
			try {
				// TODO: currently not caching the parser since we make no assumptions
				//		 or restrictions on concurrent parsing
				getParser();
				RepositoryHandler repositoryHandler = new RepositoryHandler();
				xmlReader.setContentHandler(new RepositoryDocHandler(REPOSITORY_ELEMENT, repositoryHandler));
				xmlReader.parse(new InputSource(stream));
				if (isValidXML()) {
					theRepository = repositoryHandler.getRepository();
				}
			} catch (SAXException e) {
				if (!(e.getException() instanceof OperationCanceledException))
					throw new IOException(e.getMessage());
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} finally {
				monitor.done();
				stream.close();
			}
		}

		public IMetadataRepository getRepository() {
			return theRepository;
		}

		protected Object getRootObject() {
			return theRepository;
		}

		private final class RepositoryDocHandler extends DocHandler {

			public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
				super(rootName, rootHandler);
			}

			public void processingInstruction(String target, String data) throws SAXException {
				if (PI_REPOSITORY_TARGET.equals(target)) {
					Version repositoryVersion = extractPIVersion(target, data);
					if (!MetadataRepositoryIO.XMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
						throw new SAXException(NLS.bind(Messages.io_IncompatibleVersion, repositoryVersion, MetadataRepositoryIO.XMLConstants.XML_TOLERANCE));
					}
				}
			}

		}

		private final class RepositoryHandler extends RootHandler {

			private final String[] required = new String[] {NAME_ATTRIBUTE, TYPE_ATTRIBUTE, VERSION_ATTRIBUTE};
			private final String[] optional = new String[] {DESCRIPTION_ATTRIBUTE, PROVIDER_ATTRIBUTE};

			private InstallableUnitsHandler unitsHandler = null;
			private PropertiesHandler propertiesHandler = null;
			private RepositoryReferencesHandler repositoryReferencesHandler = null;

			private AbstractMetadataRepository repository = null;

			private RepositoryState state = new RepositoryState();

			public RepositoryHandler() {
				super();
			}

			public IMetadataRepository getRepository() {
				return repository;
			}

			protected void handleRootAttributes(Attributes attributes) {
				String[] values = parseAttributes(attributes, required, optional);
				Version version = checkVersion(this.elementHandled, VERSION_ATTRIBUTE, values[2]);
				state.Name = values[0];
				state.Type = values[1];
				state.Version = version;
				state.Description = values[3];
				state.Provider = values[4];
				state.Location = null;
			}

			public void startElement(String name, Attributes attributes) {
				checkCancel();
				if (PROPERTIES_ELEMENT.equals(name)) {
					if (propertiesHandler == null) {
						propertiesHandler = new PropertiesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
					if (unitsHandler == null) {
						unitsHandler = new InstallableUnitsHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else if (REPOSITORY_REFERENCES_ELEMENT.equals(name)) {
					if (repositoryReferencesHandler == null) {
						repositoryReferencesHandler = new RepositoryReferencesHandler(this, attributes);
					} else {
						duplicateElement(this, name, attributes);
					}
				} else {
					invalidElement(name, attributes);
				}
			}

			protected void finished() {
				if (isValidXML()) {
					state.Properties = (propertiesHandler == null ? new OrderedProperties(0) //
							: propertiesHandler.getProperties());
					state.Units = (unitsHandler == null ? new IInstallableUnit[0] //
							: unitsHandler.getUnits());
					state.Repositories = repositoryReferencesHandler == null ? new RepositoryReference[0] : repositoryReferencesHandler.getReferences();
					try {
						//can't create repository if missing type - this is already logged when parsing attributes
						if (state.Type == null)
							return;
						Class clazz = Class.forName(state.Type);
						Object repositoryObject = clazz.newInstance();
						if (repositoryObject instanceof AbstractMetadataRepository) {
							repository = (AbstractMetadataRepository) repositoryObject;
							repository.initialize(state);
						}
					} catch (InstantiationException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO: Throw a SAXException
						e.printStackTrace();
					}
				}
			}
		}

		protected String getErrorMessage() {
			return Messages.io_parseError;
		}

		public String toString() {
			// TODO:
			return null;
		}
	}
}

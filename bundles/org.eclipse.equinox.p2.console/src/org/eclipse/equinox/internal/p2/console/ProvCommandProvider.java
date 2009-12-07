/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 * 	Band XI - add more commands
 *  Composent, Inc. - command additions
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.console;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.metadata.query.GroupQuery;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * An OSGi console command provider that adds various commands for interacting
 * with the provisioning system.
 */
public class ProvCommandProvider implements CommandProvider {
	private static final String WILDCARD_ANY = "*"; //$NON-NLS-1$
	public static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	//	private Profile profile;

	public ProvCommandProvider(String profileId, IProfileRegistry registry) {
		// look up the profile we are currently running and use it as the
		// default.
		// TODO define a way to spec the default profile to manage
		//		if (profileId != null) {
		//			profile = registry.getProfile(profileId);
		//			if (profile != null)
		//				return;
		//		}
		//		// A default was not defined so manage the first profile we can find
		//		Profile[] profiles = registry.getProfiles();
		//		if (profiles.length > 0)
		//			profile = profiles[0];
	}

	/**
	 * Adds both a metadata repository and artifact repository
	 */
	public void _provaddrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		// add metadata repo
		if (ProvisioningHelper.addMetadataRepository(repoURI) == null) {
			interpreter.println("Unable to add metadata repository: " + repoURI);
		} else // add artifact repo at same URL
		if (ProvisioningHelper.addArtifactRepository(repoURI) == null) {
			interpreter.println("Unable to add artifact repository: " + repoURI);
		}
	}

	public void _provdelrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeMetadataRepository(repoURI);
		ProvisioningHelper.removeArtifactRepository(repoURI);
	}

	/**
	 * Adds a metadata repository.
	 */
	public void _provaddmetadatarepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		if (ProvisioningHelper.addMetadataRepository(repoURI) == null)
			interpreter.println("Unable to add repository: " + repoURI);
	}

	public void _provdelmetadatarepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeMetadataRepository(repoURI);
	}

	public void _provaddartifactrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		if (ProvisioningHelper.addArtifactRepository(repoURI) == null)
			interpreter.println("Unable to add repository " + repoURI);
	}

	public void _provdelartifactrepo(CommandInterpreter interpreter) {
		String urlString = interpreter.nextArgument();
		if (urlString == null) {
			interpreter.println("Repository location must be provided");
			return;
		}
		URI repoURI = toURI(interpreter, urlString);
		if (repoURI == null)
			return;
		ProvisioningHelper.removeArtifactRepository(repoURI);
	}

	/**
	 * Install a given IU to a given profile location.
	 */
	public void _provinstall(CommandInterpreter interpreter) {
		String iu = interpreter.nextArgument();
		String version = interpreter.nextArgument();
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this")) //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		if (iu == null || version == null || profileId == null) {
			interpreter.println("Installable unit id, version, and profileid must be provided");
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.install(iu, version, ProvisioningHelper.getProfile(profileId), new NullProgressMonitor());
		} catch (ProvisionException e) {
			interpreter.println("Installation failed with ProvisionException for " + iu + " " + version);
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("Installation complete for " + iu + " " + version);
		else {
			interpreter.println("Installation failed for " + iu + " " + version);
			printErrorStatus(interpreter, s);
		}
	}

	/**
	 * Creates a profile given an id, location, and flavor
	 */
	public void _provaddprofile(CommandInterpreter interpreter) {
		String profileId = interpreter.nextArgument();
		String location = interpreter.nextArgument();
		String flavor = interpreter.nextArgument();
		if (profileId == null || location == null || flavor == null) {
			interpreter.println("Id, location, and flavor must be provided");
			return;
		}
		String environments = interpreter.nextArgument();
		Properties props = new Properties();
		props.setProperty(IProfile.PROP_INSTALL_FOLDER, location);
		if (environments != null)
			props.setProperty(IProfile.PROP_ENVIRONMENTS, environments);

		try {
			ProvisioningHelper.addProfile(profileId, props);
		} catch (ProvisionException e) {
			interpreter.println("Add profile failed.  " + e.getMessage());
			interpreter.printStackTrace(e);
		}
	}

	/**
	 * Deletes a profile given an id, location, and flavor
	 */
	public void _provdelprofile(CommandInterpreter interpreter) {
		String profileId = interpreter.nextArgument();
		if (profileId == null) {
			interpreter.println("profileid must be provided");
			return;
		}
		ProvisioningHelper.removeProfile(profileId);
	}

	/**
	 * Lists the installable units that match the given URL, id, and/or version.
	 * 
	 * @param interpreter
	 */
	public void _provliu(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		URI repoURL = null;
		if (urlString != null && !urlString.equals(WILDCARD_ANY))
			repoURL = toURI(interpreter, urlString);
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(repoURL, new InstallableUnitQuery(id, new VersionRange(version)), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the known metadata repositories, or the contents of a given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlr(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String version = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			URI[] repositories = ProvisioningHelper.getMetadataRepositories();
			if (repositories != null)
				for (int i = 0; i < repositories.length; i++)
					interpreter.println(repositories[i]);
			return;
		}
		URI repoLocation = toURI(interpreter, urlString);
		if (repoLocation == null)
			return;
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(repoLocation, new InstallableUnitQuery(id, new VersionRange(version)), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the group IUs in all known metadata repositories, or in the given
	 * metadata repository.
	 * 
	 * @param interpreter
	 */
	public void _provlg(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		IQueryable queryable = null;
		if (urlString == null) {
			queryable = (IQueryable) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
			if (queryable == null)
				return;
		} else {
			URI repoURL = toURI(interpreter, urlString);
			if (repoURL == null)
				return;
			queryable = ProvisioningHelper.getMetadataRepository(repoURL);
			if (queryable == null)
				return;
		}
		IInstallableUnit[] units = sort(queryable.query(new GroupQuery(), new Collector(), null));
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);
	}

	/**
	 * Lists the known artifact repositories, or the contents of a given
	 * artifact repository.
	 * 
	 * @param interpreter
	 */
	public void _provlar(CommandInterpreter interpreter) {
		String urlString = processArgument(interpreter.nextArgument());
		if (urlString == null) {
			URI[] repositories = ProvisioningHelper.getArtifactRepositories();
			if (repositories == null)
				return;
			for (int i = 0; i < repositories.length; i++)
				interpreter.println(repositories[i]);
			return;
		}
		URI repoURL = toURI(interpreter, urlString);
		if (repoURL == null)
			return;
		IArtifactRepository repo = ProvisioningHelper.getArtifactRepository(repoURL);
		Collector keys = null;
		try {
			keys = (repo != null) ? repo.query(ArtifactKeyQuery.ALL_KEYS, new Collector(), null) : null;
		} catch (UnsupportedOperationException e) {
			interpreter.println("Repository does not support queries.");
			return;
		}
		if (keys == null || keys.isEmpty()) {
			interpreter.println("Repository has no artifacts");
			return;
		}
		IFileArtifactRepository fileRepo = (IFileArtifactRepository) repo.getAdapter(IFileArtifactRepository.class);
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = (IArtifactKey) iterator.next();
			IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(key);
			for (int j = 0; j < descriptors.length; j++) {
				IArtifactDescriptor descriptor = descriptors[j];
				File location = null;
				if (fileRepo != null)
					location = fileRepo.getArtifactFile(descriptor);
				println(interpreter, key, location);
			}

		}
	}

	/**
	 * Returns the given string as an URL, or <code>null</code> if the string
	 * could not be interpreted as an URL.
	 */
	private URI toURI(CommandInterpreter interpreter, String urlString) {
		try {
			return new URI(urlString);
		} catch (URISyntaxException e) {
			interpreter.print(e.getMessage());
			interpreter.println();
			return null;
		}
	}

	private String processArgument(String arg) {
		if (arg == null || arg.equals(WILDCARD_ANY))
			return null;
		return arg;
	}

	/**
	 * Lists the known profiles, or the contents of a given profile.
	 * 
	 * @param interpreter
	 */
	public void _provlp(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		String id = processArgument(interpreter.nextArgument());
		String range = processArgument(interpreter.nextArgument());
		if (profileId == null) {
			IProfile[] profiles = ProvisioningHelper.getProfiles();
			for (int i = 0; i < profiles.length; i++)
				interpreter.println(profiles[i].getProfileId());
			return;
		}
		// determine which profile is to be listed
		IProfile target = null;
		if (profileId.equals("this")) //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		target = ProvisioningHelper.getProfile(profileId);
		if (target == null)
			return;

		// list the profile contents
		IInstallableUnit[] result = sort(target.query(new InstallableUnitQuery(id, new VersionRange(range)), new Collector(), null));
		for (int i = 0; i < result.length; i++)
			interpreter.println(result[i]);
	}

	/**
	 * Lists the profile timestamps for a given profile id, if no profile id, the default profile
	 * is used.
	 * 
	 * @param interpreter
	 */
	public void _provlpts(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		if (profileId == null || profileId.equals("this")) { //$NON-NLS-1$
			profileId = IProfileRegistry.SELF;
		}
		long[] profileTimestamps = ProvisioningHelper.getProfileTimestamps(profileId);
		// if no profile timestamps for given id, print that out and done
		if (profileTimestamps == null || profileTimestamps.length == 0) {
			interpreter.print("No timestamps found for profile ");
			interpreter.println(profileId);
			return;
		}
		// else if there are some timestamps then print them out on separate line
		interpreter.print("Timestamps for profile ");
		interpreter.println(profileId);
		for (int i = 0; i < profileTimestamps.length; i++) {
			interpreter.print("\t"); //$NON-NLS-1$
			interpreter.println(new Long(profileTimestamps[i]));
		}
	}

	/**
	 * Revert a profile to a given timestamp
	 */
	public void _provrevert(CommandInterpreter interpreter) {
		String timestamp = interpreter.nextArgument();
		if (timestamp == null) {
			interpreter.println("Valid timestamp must be provided.  Timestamps can be retrieved via 'provlpts' command.");
			return;
		}
		Long ts = null;
		try {
			ts = new Long(timestamp);
		} catch (NumberFormatException e) {
			interpreter.println("Timestamp " + timestamp + " not valid.  Timestamps can be retrieved via 'provlpts' command.");
			return;
		}
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this"))
			profileId = IProfileRegistry.SELF;

		IProfile profile = ProvisioningHelper.getProfile(profileId);
		if (profile == null) {
			interpreter.println("Profile " + profileId + " not found");
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.revertToPreviousState(profile, ts.longValue());
		} catch (ProvisionException e) {
			interpreter.println("revert failed ");
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("revert completed");
		else {
			interpreter.println("revert failed ");
			printErrorStatus(interpreter, s);
		}
	}

	private IInstallableUnit[] sort(Collector collector) {
		IInstallableUnit[] units = (IInstallableUnit[]) collector.toArray(IInstallableUnit.class);
		Arrays.sort(units, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return arg0.toString().compareTo(arg1.toString());
			}
		});
		return units;
	}

	public void _provlgp(CommandInterpreter interpreter) {
		String profileId = processArgument(interpreter.nextArgument());
		if (profileId == null || profileId.equals("this")) {
			profileId = IProfileRegistry.SELF;
		}
		IProfile profile = ProvisioningHelper.getProfile(profileId);
		if (profile == null) {
			interpreter.println("Profile " + profileId + " not found");
			return;
		}
		IInstallableUnit[] units = sort(ProvisioningHelper.getInstallableUnits(profile, new GroupQuery(), new NullProgressMonitor()));
		// Now print out results
		for (int i = 0; i < units.length; i++)
			println(interpreter, units[i]);

	}

	public void _provremove(CommandInterpreter interpreter) {
		String iu = interpreter.nextArgument();
		String version = interpreter.nextArgument();
		String profileId = interpreter.nextArgument();
		if (profileId == null || profileId.equals("this"))
			profileId = IProfileRegistry.SELF;
		if (version == null) {
			version = Version.emptyVersion.toString();
		}
		if (iu == null) {
			interpreter.println("Installable unit id must be provided");
			return;
		}
		IStatus s = null;
		try {
			s = ProvisioningHelper.uninstall(iu, version, ProvisioningHelper.getProfile(profileId), new NullProgressMonitor());
		} catch (ProvisionException e) {
			interpreter.println("Remove failed with ProvisionException for " + iu + " " + version);
			interpreter.printStackTrace(e);
			return;
		}
		if (s.isOK())
			interpreter.println("Remove complete for " + iu + " " + version);
		else {
			interpreter.println("Remove failed for " + iu + " " + version);
			printErrorStatus(interpreter, s);
		}
	}

	private void printErrorStatus(CommandInterpreter interpreter, IStatus status) {
		interpreter.print("--Error status ");
		interpreter.print("message=" + status.getMessage());
		interpreter.print(",code=" + status.getCode());
		String severityString = null;
		switch (status.getSeverity()) {
			case IStatus.INFO :
				severityString = "INFO";
				break;
			case IStatus.CANCEL :
				severityString = "CANCEL";
				break;
			case IStatus.WARNING :
				severityString = "WARNING";
				break;
			case IStatus.ERROR :
				severityString = "ERROR";
				break;
		}
		interpreter.print(",severity=" + severityString);
		interpreter.print(",bundle=" + status.getPlugin());
		interpreter.println("--");
		Throwable t = status.getException();
		if (t != null)
			interpreter.printStackTrace(t);
		IStatus[] children = status.getChildren();
		if (children != null && children.length > 0) {
			interpreter.println("Error status children:");
			for (int i = 0; i < children.length; i++) {
				printErrorStatus(interpreter, children[i]);
			}
		}
		interpreter.println("--End Error Status--");
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append(NEW_LINE);
		help.append("---");
		help.append("P2 Provisioning Commands");
		help.append("---");
		help.append(NEW_LINE);

		help.append("---");
		help.append("Repository Commands");
		help.append("---");
		help.append(NEW_LINE);
		help.append("\tprovaddrepo <repository URI> - Adds a both a metadata and artifact repository at URI");
		help.append(NEW_LINE);
		help.append("\tprovdelrepo <repository URI> - Deletes a metadata and artifact repository at URI");
		help.append(NEW_LINE);
		help.append("\tprovaddmetadatarepo <repository URI> - Adds a metadata repository at URI");
		help.append(NEW_LINE);
		help.append("\tprovdelmetadatarepo <repository URI> - Deletes a metadata repository at URI");
		help.append(NEW_LINE);
		help.append("\tprovaddartifactrepo <repository URI> - Adds an artifact repository at URI");
		help.append(NEW_LINE);
		help.append("\tprovdelartifactrepo <repository URI> - Deletes an artifact repository URI");
		help.append(NEW_LINE);
		help.append("\tprovlg [<repository URI> <iu id | *> <version range | *>] - Lists all IUs with group capabilities in the given repo or in all repos if URI is omitted");
		help.append(NEW_LINE);
		help.append("\tprovlr [<repository URI> <iu id | *> <version range | *>]   - Lists all metadata repositories, or the contents of a given metadata repository");
		help.append(NEW_LINE);
		help.append("\tprovlar [<repository URI>] - Lists all artifact repositories, or the contents of a given artifact repository");
		help.append(NEW_LINE);
		help.append("\tprovliu [<repository URI | *> <iu id | *> <version range | *>] - Lists the IUs that match the pattern in the given repo.  * matches all");
		help.append(NEW_LINE);

		help.append("---");
		help.append("Profile Registry Commands");
		help.append("---");
		help.append(NEW_LINE);

		help.append("\tprovaddprofile <profileid> <location> <flavor> - Adds a profile with the given profileid, location and flavor");
		help.append(NEW_LINE);
		help.append("\tprovdelprofile <profileid> - Deletes a profile with the given profileid");
		help.append(NEW_LINE);
		help.append("\tprovlp [<profileid | *>] - Lists all profiles, or the contents of the profile at the given profile");
		help.append(NEW_LINE);
		help.append("\tprovlgp [<profileid>] - Lists all IUs with group capabilities in the given profile, or current profile if profileid is omitted");
		help.append(NEW_LINE);
		help.append("\tprovlpts [<profileid>] - Lists timestamps for given profile, or if no profileid given then the default profile timestamps are reported");
		help.append(NEW_LINE);

		help.append("---");
		help.append("Install Commands");
		help.append("---");
		help.append(NEW_LINE);

		help.append("\tprovinstall <InstallableUnit> <version> <profileid> - installs an IU to the profileid.  If no profileid is given, installs into default profile.");
		help.append(NEW_LINE);
		help.append("\tprovremove <InstallableUnit> <version> <profileid> - Removes an IU from the profileid.  If no profileid is given, installs into default profile.");
		help.append(NEW_LINE);
		help.append("\tprovrevert <profileTimestamp> <profileid>] - Reverts to a given profileTimestamp for an optional profileId");
		help.append(NEW_LINE);

		return help.toString();
	}

	/**
	 * Prints a string representation of an {@link IInstallableUnit} to the
	 * iterpreter's output stream.
	 */
	public void print(CommandInterpreter interpreter, IInstallableUnit unit) {
		interpreter.print(unit.getId() + ' ' + unit.getVersion());
	}

	/**
	 * Prints a string representation of an {@link IInstallableUnit} to the
	 * iterpreter's output stream, following by a line terminator
	 */
	public void println(CommandInterpreter interpreter, IInstallableUnit unit) {
		print(interpreter, unit);
		interpreter.println();
	}

	private void println(CommandInterpreter interpreter, IArtifactKey artifactKey, File location) {
		interpreter.print(artifactKey.toString() + ' ' + location);
		interpreter.println();
	}
}
/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.equinox.frameworkadmin.LauncherData;
import org.eclipse.equinox.frameworkadmin.equinox.internal.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.service.log.LogService;

public class EclipseLauncherParser {

	private String[] getConfigFileLines(LauncherData launcherData, File outputFile, boolean relative) {
		List lines = new LinkedList();

		boolean startUpFlag = false;
		final String[] programArgs = launcherData.getProgramArgs();
		if (programArgs != null && programArgs.length != 0)
			for (int i = 0; i < programArgs.length; i++) {
				if (programArgs[i].equals(EquinoxConstants.OPTION_STARTUP) && (programArgs[i + 1] != null || programArgs[i + 1].length() != 0)) {
					lines.add(programArgs[i]);
					lines.add(programArgs[++i]);
					startUpFlag = true;
				} else
					lines.add(programArgs[i]);
			}
		if (launcherData.isClean())
			lines.add(EquinoxConstants.OPTION_CLEAN);
		File fwPersistentDataLocation = launcherData.getFwPersistentDataLocation();
		File fwConfigLocation = launcherData.getFwConfigLocation();
		if (fwPersistentDataLocation != null) {
			if (fwConfigLocation != null) {
				if (!fwPersistentDataLocation.equals(fwConfigLocation))
					throw new IllegalStateException();
			}
			launcherData.setFwConfigLocation(fwPersistentDataLocation);
		} else if (fwConfigLocation != null)
			launcherData.setFwPersistentDataLocation(fwConfigLocation, launcherData.isClean());

		if (launcherData.getFwConfigLocation() != null) {
			lines.add(EquinoxConstants.OPTION_CONFIGURATION);
			String path = "";
			if (relative)
				path = Utils.getRelativePath(launcherData.getFwConfigLocation(), outputFile.getParentFile());
			else
				path = launcherData.getFwConfigLocation().getAbsolutePath();
			lines.add(path);
		}

		if (!startUpFlag)
			if (launcherData.getFwJar() != null) {
				lines.add(EquinoxConstants.OPTION_FW);
				String path = "";
				//if (relative)
				//	path = Utils.getRelativePath(launcherData.getFwJar(), outputFile.getParentFile());
				//else
				path = launcherData.getFwJar().getAbsolutePath();
				lines.add(path);
			}

		if (launcherData.getJvm() != null) {
			lines.add(EquinoxConstants.OPTION_VM);
			lines.add(launcherData.getJvm().getAbsolutePath());
		}
		final String[] jvmArgs = launcherData.getJvmArgs();
		if (jvmArgs != null && jvmArgs.length != 0) {
			lines.add(EquinoxConstants.OPTION_VMARGS);
			for (int i = 0; i < jvmArgs.length; i++)
				lines.add(jvmArgs[i]);
		}
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private void parseCmdLine(LauncherData launcherData, String[] lines) {
		//Log.log(LogService.LOG_DEBUG, "inputFile=" + inputFile.getAbsolutePath());
		//		final File launcherFile = launcherData.getLauncher();
		final File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);

		boolean clean = launcherData.isClean();
		boolean needToUpdate = false;
		File fwPersistentDataLoc = launcherData.getFwPersistentDataLocation();
		File fwConfigLocation = launcherData.getFwConfigLocation();
		if (fwPersistentDataLoc == null) {
			if (fwConfigLocation == null) {
				fwPersistentDataLoc = new File(launcherConfigFile.getParent(), EquinoxConstants.DEFAULT_CONFIGURATION);
				fwConfigLocation = fwPersistentDataLoc;
				needToUpdate = true;
			} else {
				fwPersistentDataLoc = fwConfigLocation;
				needToUpdate = true;
			}
		} else {
			if (fwConfigLocation == null) {
				fwConfigLocation = fwPersistentDataLoc;
				needToUpdate = true;
			}
		}

		File fwJar = launcherData.getFwJar();
		if (fwJar == null) {
			String location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_JAR_PLUGIN_NAME, new File(launcherConfigFile.getParent(), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				try {
					fwJar = new File(new URL(location).getFile());
					launcherData.setFwJar(fwJar);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		// launcherData.initialize(); // reset except launcherFile.

		//		launcherData.setLauncher(launcherFile);
		boolean vmArgsFlag = false;

		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i].trim();
			StringTokenizer tokenizer = new StringTokenizer(line, " ");
			if (line.startsWith("#"))
				continue;
			if (line.length() == 0)
				continue;
			if (tokenizer.countTokens() != 1) {
				Log.log(LogService.LOG_WARNING, this, "parseCmdLine(String[] lines, File inputFile)", "Illegal Format:line=" + line + "tokenizer.countTokens()=" + tokenizer.countTokens());
				//throw new IOException("Illegal Format:line=" + line + "tokenizer.countTokens()=" + tokenizer.countTokens());
			}
			if (vmArgsFlag) {
				launcherData.addJvmArg(line);
				continue;
			}
			if (line.startsWith("-vmargs")) {
				vmArgsFlag = true;
				continue;
			}
			if (line.startsWith(EquinoxConstants.OPTION_CONFIGURATION)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute())
					file = new File(launcherConfigFile.getParent() + File.separator + nextLine);
				fwPersistentDataLoc = file;
				needToUpdate = true;
				continue;
			} else if (line.startsWith(EquinoxConstants.OPTION_CLEAN)) {
				clean = true;
				needToUpdate = true;
				continue;
			} else if (line.startsWith(EquinoxConstants.OPTION_VM)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute()) {
					file = new File(launcherConfigFile.getAbsolutePath() + File.separator + nextLine);
				}
				launcherData.setJvm(file);
				continue;
			} else if (line.startsWith(EquinoxConstants.OPTION_FW)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute()) {
					file = new File(launcherConfigFile.getAbsolutePath() + File.separator + nextLine);
				}
				launcherData.setFwJar(file);
				continue;
			} else {
				launcherData.addProgramArg(lines[i]);
				//				Log.log(LogService.LOG_WARNING, this, "parseCmdLine(String[] lines, File inputFile)", "Unsupported by current impl:line=" + line);
			}
		}
		if (needToUpdate) {
			launcherData.setFwPersistentDataLocation(fwPersistentDataLoc, clean);
			launcherData.setFwConfigLocation(fwPersistentDataLoc);
		}
	}

	public void read(LauncherData launcherData) throws IOException {
		final File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);
		if (launcherConfigFile == null || !launcherConfigFile.exists())
			throw new IllegalStateException("launcherData.getLauncherConfigFile() should be set in advance");

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(launcherConfigFile));

			String line;
			List list = new LinkedList();
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			String[] lines = new String[list.size()];
			list.toArray(lines);
			this.parseCmdLine(launcherData, lines);
		} finally {
			if (br != null)
				br.close();
		}
		Log.log(LogService.LOG_INFO, "Launcher Config file(" + launcherConfigFile.getAbsolutePath() + ") is read successfully.");

	}

	public void save(LauncherData launcherData, boolean relative, boolean backup) throws IOException {

		File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);

		if (launcherConfigFile == null)
			throw new IllegalStateException("launcherConfigFile cannot be set. launcher file should be set in advance.");
		Utils.createParentDir(launcherConfigFile);
		// backup file if exists.		
		if (backup)
			if (launcherConfigFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(launcherConfigFile);
				if (!launcherConfigFile.renameTo(dest))
					throw new IOException("Fail to rename from (" + launcherConfigFile + ") to (" + dest + ")");
				Log.log(LogService.LOG_INFO, this, "saveConfigs()", "Succeed to rename from (" + launcherConfigFile + ") to (" + dest + ")");
			}

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(launcherConfigFile));

			String[] lines = this.getConfigFileLines(launcherData, launcherConfigFile, relative);
			for (int i = 0; i < lines.length; i++) {
				bw.write(lines[i]);
				bw.newLine();
			}
			bw.flush();
			Log.log(LogService.LOG_INFO, "Launcher Config file is saved successfully into:" + launcherConfigFile);
		} finally {
			if (bw != null)
				bw.close();
		}
	}

}

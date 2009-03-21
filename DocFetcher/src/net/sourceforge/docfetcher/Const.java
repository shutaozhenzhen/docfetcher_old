/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;

import net.sourceforge.docfetcher.util.UtilFile;

/**
 * A collection of system-wide constants.
 * 
 * @author Tran Nam Quang
 * @author Tonio Rush
 */
public class Const {
	
	private Const() {
		// Static use only.
	}
	
	/**
	 * System property: os.name
	 */
	private static final String OS_NAME = System.getProperty("os.name"); //$NON-NLS-1$
	
	/**
	 * Whether the system is a Windows machine.
	 */
	public static final boolean IS_WINDOWS = OS_NAME.toUpperCase().contains("WINDOWS"); //$NON-NLS-1$
	
	/**
	 * Whether the system is a Linux machine.
	 */
	public static final boolean IS_LINUX = OS_NAME.toUpperCase().contains("LINUX"); //$NON-NLS-1$
	
	/**
	 * Minimum width of buttons.
	 */
	public static final int MIN_BT_WIDTH = 75;
	
	/**
	 * Default dialog shell style 
	 */
	public static final int DIALOG_STYLE = SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE;
	
	/**
	 * Default margin for Group widgets.
	 */
	public static final int GROUP_MARGIN = 5;
	
	/**
	 * The current working directory.
	 */
	public static final String USER_DIR = System.getProperty("user.dir"); //$NON-NLS-1$
	
	/**
	 * Each time DocFetcher starts, a new file is used to write the stacktrace
	 * to. The files are differentiated by this date string in their filenames.
	 * The date string has minute resolution, so multiple stacktraces occurring
	 * within the same minute will be written to the same file.
	 */
	public static final String ERROR_FILENAME = "stacktrace_" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + ".txt"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
	/**
	 * File separator character.
	 */
	public static final String FS = System.getProperty("file.separator"); //$NON-NLS-1$
	
	/**
	 * Path separator character.
	 */
	public static final String PS = System.getProperty("path.separator"); //$NON-NLS-1$
	
	/**
	 * Line separator character.
	 */
	public static final String LS = System.getProperty("line.separator"); //$NON-NLS-1$
	
	/**
	 * Whether this instance of DocFetcher was installed on the system or not.
	 */
	public static final Boolean IS_PORTABLE;
	
	/**
	 * The file where the preferences are stored.
	 */
	public static final File USER_PROPERTIES_FILE;
	
	/**
	 * Name of the file where the preferences are stored.
	 */
	public static final String USER_PROPERTIES_FILENAME = "user.properties"; //$NON-NLS-1$
	
	/**
	 * The folder where the ScopeRegistry.ser file and the index folders are
	 * stored. If the folder lies inside the program folder, it will have a
	 * relative path, if not, then an absolute path.
	 */
	public static final File INDEX_PARENT_FILE;

	/**
	 * The file for writing the daemon file
	 */
	public static final File INDEX_DAEMON_FILE;

	/**
	 * The file let opened to let know DocFetcher is running 
	 */
	private static FileOutputStream DAEMON_LOCK;

	/**
	 * The current working directory.
	 */
	public static final File PROGRAM_FOLDER = new File("").getAbsoluteFile(); //$NON-NLS-1$
	
	/**
	 * Path to where the manual is stored.
	 */
	public static final String MANUAL_PATH;
	
	/**
	 * Sets up the path to the user.properties file and the index parent folder.
	 * If a user.properties file is found inside the program folder, both the
	 * user.properties and the index parent folder will be placed inside it. If
	 * not, a ".docfetcher" folder inside the home directory of the user will be
	 * used to store the application data.
	 */
	static {
		File propFile = new File(USER_PROPERTIES_FILENAME);
		// Portable version
		if (propFile.exists() && propFile.isFile()) {
			USER_PROPERTIES_FILE = propFile;
			INDEX_PARENT_FILE = new File("indexes"); //$NON-NLS-1$
			MANUAL_PATH = UtilFile.join(USER_DIR, "help"); //$NON-NLS-1$
			INDEX_DAEMON_FILE = new File("indexes" + FS + "indexes.txt"); //$NON-NLS-1$ //$NON-NLS-2$
			IS_PORTABLE = true;
		}
		// Installed version
		else {
			String appDataPath = ""; //$NON-NLS-1$
			if (IS_WINDOWS)
				appDataPath = System.getenv("APPDATA") + FS + "DocFetcher"; //$NON-NLS-1$ //$NON-NLS-2$
			else if (IS_LINUX)
				appDataPath = System.getProperty("user.home") + FS + ".docfetcher"; //$NON-NLS-1$ //$NON-NLS-2$
			USER_PROPERTIES_FILE = new File(appDataPath, USER_PROPERTIES_FILENAME);
			INDEX_PARENT_FILE = new File(appDataPath);
			MANUAL_PATH = IS_WINDOWS ? UtilFile.join(USER_DIR, "help") : "/usr/share/doc/docfetcher"; //$NON-NLS-1$ //$NON-NLS-2$
			INDEX_DAEMON_FILE = new File(appDataPath + FS + "indexes.txt"); //$NON-NLS-1$
			new File(appDataPath).mkdirs();
			IS_PORTABLE = false;
		}

		// the lock file 
		String daemon_lock_file_path;
		daemon_lock_file_path = INDEX_DAEMON_FILE.getAbsolutePath() + ".lock"; //$NON-NLS-1$ 

		// Open the lock file for writing, all DocFetcher's life
		try {
			DAEMON_LOCK = new FileOutputStream(daemon_lock_file_path);
		} catch (FileNotFoundException e) {
			// This can occure if to instances or running, or someone is using the file
			e.printStackTrace();
		}
	}
	
	/**
	 * Name of the directory containing icons for this application.
	 */
	public static final String ICON_DIRNAME = "icons"; //$NON-NLS-1$
	
	/**
	 * Absolute path to the application's help file.
	 */
	public static final String HELP_FILE = UtilFile.join(MANUAL_PATH, "DocFetcher_Manual.html"); //$NON-NLS-1$
	
	/**
	 * Absolute path to the preferences section of the application's help file.
	 */
	public static final String HELP_FILE_PREF = UtilFile.join(MANUAL_PATH, "DocFetcher_Manual_files/Preferences.html"); //$NON-NLS-1$
	
	/**
	 * Absolute path to the indexing preferences section of the application's help file.
	 */
	public static final String HELP_FILE_INDEXING = UtilFile.join(MANUAL_PATH, "DocFetcher_Manual_files/Indexing_Options.html"); //$NON-NLS-1$
	
	/**
	 * Base name of the resource files containing translations.
	 */
	public static final String RESOURCE_BUNDLE = "Resource"; //$NON-NLS-1$
	
	/**
	 * Possible suffixes of HTML folders. 
	 */
	public static final String[] HTML_FOLDER_SUFFIXES = new String[] {
		"_archivos", //$NON-NLS-1$
		"_arquivos", //$NON-NLS-1$
		"_bestanden", //$NON-NLS-1$
		"_bylos", //$NON-NLS-1$
		"-Dateien", //$NON-NLS-1$
		"_datoteke", //$NON-NLS-1$
		"_dosyalar", //$NON-NLS-1$
		"_elemei", //$NON-NLS-1$
		"_failid", //$NON-NLS-1$
		"_fails", //$NON-NLS-1$
		"_fajlovi", //$NON-NLS-1$
		"_ficheiros", //$NON-NLS-1$
		"_fichiers", //$NON-NLS-1$
		"-filer", //$NON-NLS-1$
		".files", //$NON-NLS-1$
		"_files", //$NON-NLS-1$
		"_file", //$NON-NLS-1$
		"_fitxers", //$NON-NLS-1$
		"_fitxategiak", //$NON-NLS-1$
		"_pliki", //$NON-NLS-1$
		"_soubory", //$NON-NLS-1$
		"_tiedostot" //$NON-NLS-1$
	};

}

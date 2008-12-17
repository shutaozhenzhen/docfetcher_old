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

package net.sourceforge.docfetcher.enumeration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

/**
 * Represents generic program preferences and allows type safe access to them
 * using the enumerations nested herein. The default values of the preferences
 * are hardcoded so as to avoid program corruption caused by manipulation
 * of the properties file.
 * 
 * @author Tran Nam Quang
 */
public class Pref {

	private Pref () {
	}

	public static enum Bool {
		ShellMaximized (false),
		ShowWelcomePage (true),
		WatchFS (true), 
		FirstLaunch (true),
		ShowFilterPanel (true),
		ShowPreview (true),
		PreviewBottom (true),
		PreviewHTML (true),
		HideOnOpen (true),
		HighlightSearchTerms (true),
		;

		public final Event<Boolean> evtChanged = new Event<Boolean> ();
		public final boolean defaultValue;
		private boolean value;
		
		public boolean getValue() {
			return value;
		}
		
		public void setValue(boolean value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fireUpdate(value);
		}

		Bool(boolean defaultValue) {
			this.defaultValue = defaultValue;
		}
	}

	public static enum Int {
		/*
		 * If ShellX and/or ShellY is < 0, the shell will be centered on screen,
		 * ignoring both parameters.
		 */
		ShellX (-1),
		ShellY (-1),
		
		ShellWidth (800),
		ShellHeight (600),
		IndexingBoxWidth (400),
		IndexingBoxHeight (430),
		PrefPageWidth (500),
		PrefPageHeight (400),
		MaxResults (50),
		OpenLimit (10),
		PreviewFontHeight (10),
		PreviewFontHeightMono (10),
		PreviewLimit (500000),
		SearchHistorySize (15),
		SearchBoxMaxWidth (200),
		ErrorTypeColWidth (200),
		ErrorPathColWidth (500),
		;

		public final Event<Integer> evtChanged = new Event<Integer> ();
		public final int defaultValue;
		private int value;
		
		public int value() {
			return value;
		}
		
		public void setValue(int value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fireUpdate(value);
		}

		Int(int defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		public String valueToString() {
			return Integer.toString(value);
		}
	}

	public static enum Str {
		AppName (""), //$NON-NLS-1$
		DateFormat (""), //$NON-NLS-1$
		ExclusionFilter (""), //$NON-NLS-1$
		PreviewFontWin ("Verdana"), //$NON-NLS-1$
		PreviewFontMonoWin ("Courier New"), //$NON-NLS-1$
		PreviewFontLinux ("Sans"), //$NON-NLS-1$
		PreviewFontMonoLinux ("Monospace"), //$NON-NLS-1$
		;

		public final Event<String> evtChanged = new Event<String> ();
		public final String defaultValue;
		private String value;
		
		public String value() {
			return value;
		}
		
		public void setValue(String value) {
			if (this.value == value) return;
			this.value = value;
			evtChanged.fireUpdate(value);
		}

		Str(String defaultValue) {
			this.defaultValue = defaultValue;
			
		}
	}

	public static enum IntArray {
		SashHorizontalWeights (1, 3),
		SashLeftWeights (1, 1),
		SashRightVerticalWeights (1, 1),
		SashRightHorizontalWeights (1, 1),
		SashProgressPanelWeights (2, 1),
		ResultColumnOrder (new int[0]),
		ResultColumnWidths (new int[0]),
		HotKeyToFront (SWT.CTRL, SWT.F8),
		;

		public final Event<int[]> evtChanged = new Event<int[]> ();
		public final int[] defaultValue;
		private int[] value;
		
		public int[] value() {
			return value;
		}
		
		public void setValue(int... value) {
			if (Arrays.equals(this.value, value)) return;
			this.value = value;
			evtChanged.fireUpdate(value);
		}

		IntArray(int... defaultValue) {
			this.defaultValue = defaultValue;
		}
	}

	public static enum StrArray {
		HTMLExtensions ("html", "htm", "shtml", "shtm", "php", "asp", "jsp"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		TextExtensions ("txt", "nfo"), //$NON-NLS-1$ //$NON-NLS-2$
		;

		public final Event<String[]> evtChanged = new Event<String[]> ();
		public final String[] defaultValue;
		private String[] value;
		
		public String[] value() {
			return value;
		}
		
		public void setValue(String... value) {
			if (Arrays.equals(this.value, value)) return;
			this.value = value;
			evtChanged.fireUpdate(value);
		}

		StrArray(String... defaultValue) {
			this.defaultValue = defaultValue;
		}
	}

	/**
	 * The prefix to append to the classname when storing the check state of a
	 * FileParser object. For example, if the class is named "HTMLParser" and
	 * this prefix is "Checked", then the line "CheckedHTMLParser=someValue"
	 * will be stored in the preferences.
	 */
	private static final String checkPrefix = "Checked"; //$NON-NLS-1$

	private static Map<Class<? extends Object>, Boolean> checkedClasses = new HashMap<Class<? extends Object>, Boolean> ();

	/**
	 * Loads ands returns the check state of the given class object from the
	 * preferences.
	 */
	public static boolean isChecked(Class<?> clazz) {
		Boolean ret = checkedClasses.get(clazz);
		if (ret == null)
			throw new IllegalArgumentException("Unregistered class."); //$NON-NLS-1$
		return ret;
	}

	/**
	 * Stores a check state of the given class object in the preferences.
	 */
	public static void setChecked(Class<?> clazz, boolean checked) {
		checkedClasses.put(clazz, checked);
	}

	/**
	 * Loads the preferences from the preferences file.
	 */
	public static void load() {
		File propFile = Const.USER_PROPERTIES_FILE;
		try {
			propFile.createNewFile();
			FileInputStream inStream = new FileInputStream(propFile);
			Properties prop = new Properties();
			prop.load(inStream);
			inStream.close();
			for (Bool bool : Pref.Bool.values())
				bool.value = Boolean.parseBoolean(
						prop.getProperty(
								bool.name(),
								Boolean.toString(bool.defaultValue)
						)
				);
			for (Int intpref : Pref.Int.values())
				intpref.value = Integer.parseInt(
						prop.getProperty(
								intpref.name(),
								Integer.toString(intpref.defaultValue)
						)
				);
			for (Str str : Pref.Str.values())
				str.value = prop.getProperty(str.name(), str.defaultValue);
			for (StrArray strArray : Pref.StrArray.values())
				strArray.value = prop.getProperty(
						strArray.name(),
						UtilList.toString(", ", strArray.defaultValue) //$NON-NLS-1$
				).split("[^\\p{Alnum}]+"); //$NON-NLS-1$
			for (IntArray intArray : Pref.IntArray.values()) {
				String rawValue = prop.getProperty(
						intArray.name(),
						UtilList.toString(", ", intArray.defaultValue) //$NON-NLS-1$
				);
				String[] rawValues = rawValue.trim().equals("") ? new String[0] : rawValue.split("[^\\p{Alnum}]+"); //$NON-NLS-1$ //$NON-NLS-2$
				intArray.value = new int[rawValues.length];
				for (int i = 0; i < rawValues.length; i++)
					intArray.value[i] = Integer.parseInt(rawValues[i]);
			}
			for (Parser parser : ParserRegistry.getParsers())
				checkedClasses.put(
						parser.getClass(),
						Boolean.parseBoolean(
								prop.getProperty(
										checkPrefix + parser.getClass().getSimpleName(),
										"True" //$NON-NLS-1$
								)
						)
				);
		}
		catch (IOException e) {
			UtilGUI.showErrorMsgOnStart(Msg.read_error.format(propFile.getAbsolutePath()));
			System.exit(0);
		}
	}

	/**
	 * Saves the preferences to the preferences file.
	 */
	public static void save() throws IOException {
		SortedMap<String, String> props = new TreeMap<String, String> ();
		for (Bool bool : Pref.Bool.values())
			props.put(bool.name(), Boolean.toString(bool.value));
		for (Int intpref : Pref.Int.values())
			props.put(intpref.name(), Integer.toString(intpref.value));
		for (Str str : Pref.Str.values())
			props.put(str.name(), str.value);
		for (StrArray strArray : Pref.StrArray.values())
			props.put(strArray.name(), UtilList.toString(", ", strArray.value)); //$NON-NLS-1$
		for (IntArray intArray : Pref.IntArray.values())
			props.put(intArray.name(), UtilList.toString(", ", intArray.value)); //$NON-NLS-1$
		for (Entry<Class<? extends Object>, Boolean> checkEntry : checkedClasses.entrySet())
			props.put(
					checkPrefix + checkEntry.getKey().getSimpleName(),
					checkEntry.getValue().toString()
			);
		FileWriter writer = new FileWriter(Const.USER_PROPERTIES_FILE, false);
		writer.write("#" + DocFetcher.appName + " preferences" + Const.LS); //$NON-NLS-1$ //$NON-NLS-2$
		writer.write("#" + "Only modify this if you know what you're doing" + Const.LS); //$NON-NLS-1$ //$NON-NLS-2$
		writer.write("#" + new Date().toString() + Const.LS); //$NON-NLS-1$
		String lastKey = props.lastKey();
		for (Entry<String, String> entry : props.entrySet()) {
			writer.write(entry.getKey() + "=" + entry.getValue()); //$NON-NLS-1$
			if (lastKey != entry.getKey())
				writer.write(Const.LS);
		}
		writer.close();
	}

}
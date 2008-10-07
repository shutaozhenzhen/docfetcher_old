/*******************************************************************************
 * Copyright (c) 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.enumeration;

import net.sourceforge.docfetcher.Const;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * An enumeration of fonts used in the application.
 * 
 * @author Tran Nam Quang
 */
public enum Font {
	
	SYSTEM_BOLD ("sysbold"), //$NON-NLS-1$
	PREVIEW ("preview"), //$NON-NLS-1$
	PREVIEW_MONO ("preview_mono"), //$NON-NLS-1$
	;
	
	/** An ID used to store the font in the FontRegistry. */
	private final String ref;
	
	Font (String ref) {
		this.ref = ref;
	}
	
	private static FontRegistry fontRegistry = new FontRegistry();
	
	static {
		fontRegistry.put(PREVIEW.ref, new FontData[] {
				new FontData(
						Const.IS_WINDOWS ? Pref.Str.PreviewFontWin.value : Pref.Str.PreviewFontLinux.value,
						Pref.Int.PreviewFontHeight.value,
						SWT.NORMAL
				)
		});
		fontRegistry.put(PREVIEW_MONO.ref, new FontData[] {
				new FontData(
						Const.IS_WINDOWS ? Pref.Str.PreviewFontMonoWin.value : Pref.Str.PreviewFontMonoLinux.value,
						Pref.Int.PreviewFontHeightMono.value,
						SWT.NORMAL
				)
		});
	}
	
	/**
	 * Initializes some fonts. This method must be called on startup, after the
	 * display has been created.
	 */
	public static void initialize() {
		/*
		 * We have to wait for the display to be created because we want to use
		 * a modified version of the SWT system font, which is only accessible
		 * after the creation of the display.
		 */
		FontData sysFD = Display.getDefault().getSystemFont().getFontData()[0];
		String sysFDName = sysFD.getName();
		int sysFDHeight = sysFD.getHeight();
		fontRegistry.put(SYSTEM_BOLD.ref, new FontData[] {new FontData(sysFDName, sysFDHeight, SWT.BOLD)});
	}
	
	/**
	 * Returns the <tt>Font</tt> object corresponding to this enumeration
	 * entity. It does not need to be disposed.
	 */
	public org.eclipse.swt.graphics.Font getFont() {
		return fontRegistry.get(ref);
	}

}

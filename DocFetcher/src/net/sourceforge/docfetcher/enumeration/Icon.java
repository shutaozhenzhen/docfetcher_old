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
import java.net.MalformedURLException;

import net.sourceforge.docfetcher.Const;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * An enumeration for type-safe access to image files.
 * 
 * @author Tran Nam Quang
 */
public enum Icon {
	
	DOCFETCHER16 ("docfetcher16.png"), //$NON-NLS-1$
	DOCFETCHER32 ("docfetcher32.png"), //$NON-NLS-1$
	DOCFETCHER48 ("docfetcher48.png"), //$NON-NLS-1$
	
	FILE ("file.gif"), //$NON-NLS-1$
	BROWSER ("browser.gif"), //$NON-NLS-1$

	WARNING ("warning.gif"), //$NON-NLS-1$
	WARNING_BIG ("warning_big.gif"), //$NON-NLS-1$
	INFO ("info.gif"), //$NON-NLS-1$

	ARROW_LEFT ("nav_backward.gif"), //$NON-NLS-1$
	ARROW_RIGHT ("nav_forward.gif"), //$NON-NLS-1$
	STOP ("stop.gif"), //$NON-NLS-1$
	REFRESH ("refresh.gif"), //$NON-NLS-1$
	PROGRAM ("program.gif"), //$NON-NLS-1$
	
	ARROW_UP ("arrow_up.gif"), //$NON-NLS-1$
	ARROW_DOWN ("arrow_down.gif"), //$NON-NLS-1$
	HELP ("help.gif"), //$NON-NLS-1$
	PREFERENCES ("preferences.gif"), //$NON-NLS-1$
	LETTERS ("letters.gif"), //$NON-NLS-1$
	
	LAYOUT ("layout.gif"), //$NON-NLS-1$
	ATTACH_RIGHT ("attach_right.gif"), //$NON-NLS-1$
	ATTACH_BOTTOM ("attach_bottom.gif"), //$NON-NLS-1$
	
	PREVIEW ("preview.gif"), //$NON-NLS-1$
	INDEX_MANAGEMENT ("index_management.gif"), //$NON-NLS-1$
	ADD ("add.gif"), //$NON-NLS-1$
	WALK_TREE ("walk_tree.gif"), //$NON-NLS-1$
	CHECK ("check.gif"), //$NON-NLS-1$
	;
	
	private static ImageRegistry imageRegistry = new ImageRegistry(); // ImageRegistry is a Singleton
	
	// Put all icons from the icon folder into the ImageRegistry
	static {
		File[] iconFiles = new File(Const.ICON_DIRNAME).listFiles();
		for (int i = 0; i < iconFiles.length; i++) {
			String name = iconFiles[i].getName();
			ImageDescriptor descriptor = null;
			try {
				descriptor = ImageDescriptor.createFromURL(iconFiles[i].toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			imageRegistry.put(name, descriptor);
		}
	}
	
	private final String filename;
	
	Icon(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Returns the <tt>Image</tt> object corresponding to this enumeration
	 * entity. It does not need to be disposed.
	 */
	public Image getImage() {
		Image img = imageRegistry.get(filename);
		if (img == null)
			throw new IllegalStateException("Image registry was not loaded properly."); //$NON-NLS-1$
		return img;
	}

}
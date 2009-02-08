/*******************************************************************************
 * Copyright (c) 2009 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.docfetcher.enumeration.Msg;

import au.id.jericho.lib.html.Source;

/**
 * @author Tran Nam Quang
 */
public class SVGParser extends Parser {

	private String[] extensions = new String[] {"svg"}; //$NON-NLS-1$
	
	public String[] getExtensions() {
		return extensions;
	}

	public String getFileType() {
		return Msg.filetype_svg.value();
	}

	public String renderText(File file) throws ParseException {
		try {
			InputStream in = new FileInputStream(file);
			Source source = new Source(in);
			in.close();
			source.setLogger(null);
			return source.getTextExtractor().toString();
		}
		catch (FileNotFoundException e) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
	}

}

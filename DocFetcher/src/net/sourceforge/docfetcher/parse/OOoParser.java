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

package net.sourceforge.docfetcher.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.catcode.odf.OpenDocumentTextInputStream;

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;
import au.id.jericho.lib.html.CharacterReference;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.Source;
import au.id.jericho.lib.html.StartTag;

/**
 * @author Tran Nam Quang
 */
public abstract class OOoParser extends Parser {
	
	public Document parse(File file) throws ParseException {
		ZipFile zipFile = null;
		try {
			// Get zip entries
			zipFile = new ZipFile(file);
			ZipEntry manifZipEntry = zipFile.getEntry("META-INF/manifest.xml"); //$NON-NLS-1$
			ZipEntry metaZipEntry = zipFile.getEntry("meta.xml"); //$NON-NLS-1$
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			if (manifZipEntry == null || metaZipEntry == null || contentZipEntry == null)
				throw new ParseException(file, Msg.file_corrupted.value());

			// Find out if file is password protected
			InputStream manifInputStream = zipFile.getInputStream(manifZipEntry);
			Source manifSource = new Source(manifInputStream);
			manifInputStream.close();
			manifSource.setLogger(null);
			StartTag encryptTag = manifSource.findNextStartTag(0, "manifest:encryption-data"); //$NON-NLS-1$
			if (encryptTag != null)
				throw new ParseException(file, Msg.doc_pw_protected.value());
			
			// Get tags from meta.xml file
			InputStream metaInputStream = zipFile.getInputStream(metaZipEntry);
			Source metaSource = new Source(metaInputStream);
			metaInputStream.close();
			metaSource.setLogger(null);
			String[] metaData = new String[] {
					getElementContent(metaSource, "dc:title"), //$NON-NLS-1$
					getElementContent(metaSource, "dc:creator"), //$NON-NLS-1$
					getElementContent(metaSource, "dc:description"), //$NON-NLS-1$
					getElementContent(metaSource, "dc:subject"), //$NON-NLS-1$
					getElementContent(metaSource, "meta:keyword") //$NON-NLS-1$
			};

			// Get contents from content.xml file
			InputStream contentInputStream = zipFile.getInputStream(contentZipEntry);
			Source contentSource = new Source(contentInputStream);
			contentInputStream.close();
			contentSource.setLogger(null);
			Element contentElement = contentSource.findNextElement(0, "office:body"); //$NON-NLS-1$
			String contents = contentElement.getContent().getTextExtractor().toString();
			StringBuffer sb = new StringBuffer(contents);
			for (String field : metaData)
				if (field != null)
					sb.append(" ").append(field); //$NON-NLS-1$

			return new Document(file, metaData[0], sb).addAuthor(metaData[1]);
		} catch (IOException e) {
			throw new ParseException(file, Msg.file_not_readable.value());
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Returns the textual content inside the given HTML element from the given
	 * HTML source. Returns null if the HTML element is not found.
	 */
	private String getElementContent(Source source, String elementName) {
		Element el = source.findNextElement(0, elementName);
		return el == null ? null : CharacterReference.decode(el.getContent());
	}
	
	public String renderText(File file) throws ParseException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			ZipEntry contentZipEntry = zipFile.getEntry("content.xml"); //$NON-NLS-1$
			if (contentZipEntry == null)
				throw new ParseException(file, Msg.file_corrupted.value());

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
							new OpenDocumentTextInputStream(
									zipFile.getInputStream(contentZipEntry),
									null, null
							),
							"utf8" // OpenDocument files are always encoded in UTF-8 //$NON-NLS-1$
					)
			);

			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
				sb.append(line).append("\n"); //$NON-NLS-1$
			return sb.toString();
		}
		catch (FileNotFoundException e1) {
			throw new ParseException(file, Msg.file_not_found.value());
		}
		catch (IOException ioe) {
			throw new ParseException(file, Msg.file_not_readable.value());
		}
		finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

}

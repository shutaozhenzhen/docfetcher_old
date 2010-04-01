/*******************************************************************************
 * Copyright (c) 2010 Andreas Kalender
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Kalender - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.webinterface.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.model.ScopeRegistry;

/**
 * Simple and very first testservlet It provides just some very basic behaviour
 * and no style and layout elements and was just created for simple first tests
 * 
 * @author Andreas Kalender
 */
public class TestServlet extends ServletBase {

	private static final String PARAM_SEARCHSTRING = "searchString"; //$NON-NLS-1$

	/**
	 * auto-generated
	 */
	private static final long serialVersionUID = -8733293133189913043L;

	/**
	 * @see net.sourceforge.docfetcher.webinterface.servlets.ServletBase#handleRequest(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void handleRequest(	final HttpServletRequest request,
									final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html"); //$NON-NLS-1$

		final StringBuilder sb = new StringBuilder();
		this.insertHeader(sb);

		final String searchString = request.getParameter(TestServlet.PARAM_SEARCHSTRING);
		this.insertForm(sb);

		if (searchString != null) {
			this.insertSearchResults(	sb,
										searchString);
		}

		this.insertFooter(sb);
		response.getWriter()
				.println(sb.toString());
	}

	/**
	 * Creates the HTML file footer and appends it to the given
	 * <code>StringBuilder</code>
	 * 
	 * @param sb
	 *            The <code>StringBuilder</code> the created footer will be
	 *            appended to
	 */
	protected void insertFooter(final StringBuilder sb) {
		sb.append("\t</BODY>\n"); //$NON-NLS-1$
		sb.append("</HTML>\n"); //$NON-NLS-1$	
	}

	/**
	 * Creates the HTML formular data and appends it to the given
	 * <code>StringBuilder</code> The form offers a single search field as well
	 * as a button. The user could enter any search string and send the request
	 * by pressing the button.
	 * 
	 * @param sb
	 *            The <code>StringBuilder</code> the formular data will be
	 *            appended to
	 */
	protected void insertForm(final StringBuilder sb) {
		sb.append("\t\t<FORM action=\"/docfetcher\" method=\"GET\">\n"); //$NON-NLS-1$
		sb.append("\t\t\t<INPUT type=\"text\" id=\"" + //$NON-NLS-1$
					TestServlet.PARAM_SEARCHSTRING
					+ "\" NAME=\"" + //$NON-NLS-1$
					TestServlet.PARAM_SEARCHSTRING
					+ "\">\n"); //$NON-NLS-1$
		sb.append("\t\t\t<INPUT TYPE=submit>\n"); //$NON-NLS-1$
		sb.append("\t\t</FORM>\n"); //$NON-NLS-1$
	}

	/**
	 * Creates the HTML file header and appends it to the given
	 * <code>StringBuilder</code>
	 * 
	 * @param sb
	 *            The <code>StringBuilder</code> the formular data will be
	 *            appended to
	 */
	protected void insertHeader(final StringBuilder sb) {
		sb.append("<HTML>\n"); //$NON-NLS-1$
		sb.append("\t<HEAD>\n\t\t<TITLE>Test Page</TITLE>\n\t</HEAD>\n"); //$NON-NLS-1$
		sb.append("\t<BODY>\n"); //$NON-NLS-1$
	}

	/**
	 * Creates the section that contains the search results and appends it to
	 * the given <code>StringBuilder</code> Therefor the a search request will
	 * be transmitted to the <code>ScopeRegistry</code> and the corresponding
	 * results will be formatted as HTML and added to the response
	 * 
	 * @param sb
	 *            The <code>StringBuilder</code> the formular data will be
	 *            appended to
	 * @param searchString
	 *            The string that is lookedup
	 */
	protected void insertSearchResults(	final StringBuilder sb,
										final String searchString) {
		try {
			final ResultDocument[] results = ScopeRegistry	.getInstance()
															.search(searchString);

			final StringBuilder searchResults = new StringBuilder();

			searchResults.append("\t\t<table>\n\t\t\t<tr>\n\t\t\t\t<th>FileName</th>\t\t\t</tr>\n"); //$NON-NLS-1$

			for (final ResultDocument resultDocument : results) {
				searchResults.append("\t\t\t<tr>\n\t\t\t\t<td>" + //$NON-NLS-1$
										resultDocument	.getFile()
														.getAbsolutePath()
										+ "</td>\n\t\t\t</tr>\n"); //$NON-NLS-1$
			}

			searchResults.append("\t\t</table>"); //$NON-NLS-1$
			sb.append(searchResults);
		}
		catch (final Exception e) {
			// TODO Externalize
			sb.append("An unxpected error occured, please try again");
		}
	}
}

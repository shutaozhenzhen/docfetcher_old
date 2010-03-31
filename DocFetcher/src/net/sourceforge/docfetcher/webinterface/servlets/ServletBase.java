/*******************************************************************************
 * Copyright (c) ${year} Andreas Kalender
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class of HTTP servlets Handles any request independent of the used
 * request method (GET / POST)
 * 
 * @author Andreas Kalender
 */
public abstract class ServletBase extends HttpServlet {

	/**
	 * auto-generated
	 */
	private static final long serialVersionUID = 308824910393284633L;

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(	final HttpServletRequest req,
							final HttpServletResponse resp)
			throws ServletException, IOException {
		this.handleRequest(	req,
							resp);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(	final HttpServletRequest req,
							final HttpServletResponse resp)
			throws ServletException, IOException {
		this.handleRequest(	req,
							resp);
	}

	/**
	 * Method that handles the request This method is called if either a GET or
	 * a POST request was triggered
	 * 
	 * @param request
	 *            The request that is to be handled
	 * @param response
	 *            The corresponding response
	 * @throws ServletException
	 *             If an ServletException occurs
	 * @throws IOException
	 *             If an IO Exception occurs
	 */
	protected abstract void handleRequest(	HttpServletRequest request,
											HttpServletResponse response)
			throws ServletException, IOException;

}

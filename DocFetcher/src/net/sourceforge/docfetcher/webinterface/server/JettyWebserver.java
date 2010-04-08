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

package net.sourceforge.docfetcher.webinterface.server;

import net.sourceforge.docfetcher.webinterface.servlets.TestServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Implementation of the <code>IWebserver</code> Interface This implementation
 * is based upon the usage of Jetty as the application and webserver
 * 
 * @author Andreas Kalender
 */
public class JettyWebserver implements IWebserver {

	private final int port;
	private Server server;

	/**
	 * Constructor
	 * 
	 * @param port
	 *            The port the server should listen on
	 */
	public JettyWebserver(final int port) {
		super();

		this.port = port;
		this.server = new Server(port);
	}

	/**
	 * @see net.sourceforge.docfetcher.webinterface.server.IWebserver#getPort()
	 */
	@Override
	public int getPort() {
		return this.port;
	}

	/**
	 * @see net.sourceforge.docfetcher.webinterface.server.IWebserver#startServer()
	 */
	@Override
	public void startServer() throws WebserverException {
		new Thread(new Runnable() {

			/**
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
				context.setContextPath("/"); //$NON-NLS-1$
				JettyWebserver.this.server.setHandler(context);

				context.addServlet(	new ServletHolder(new TestServlet()),
									"/*"); //$NON-NLS-1$
				context.addServlet(	new ServletHolder(new TestServlet()),
									"/docfetcher"); //$NON-NLS-1$
				try {
					JettyWebserver.this.server.start();
					JettyWebserver.this.server.join();
				}
				catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * @see net.sourceforge.docfetcher.webinterface.server.IWebserver#stopServer()
	 */
	@Override
	public void stopServer() {
		if (this.server != null) {
			try {
				this.server.stop();
			}
			catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.server = null;
		}
	}

}
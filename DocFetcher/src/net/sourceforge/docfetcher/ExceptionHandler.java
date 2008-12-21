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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import net.sourceforge.docfetcher.view.ErrorDialog;

import org.eclipse.swt.widgets.Display;

/**
 * This class makes the system error output show up in an error window.
 * 
 * @author Tran Nam Quang
 */
public class ExceptionHandler {

	/**
	 * A convenience variable to turn this class off.
	 */
	private static boolean forceDisabled;

	/**
	 * The SWT textbox the error output is send to.
	 */
	private static ErrorDialog errorDialog;

	/**
	 * The default error printstream provided by the system. This needs to be
	 * saved so we can both output to the default printstream and the textbox.
	 */
	private static PrintStream defaultErr = System.err;

	private static PrintStream customErr;

	public static void setEnabled(boolean enabled) {
		if (forceDisabled) return;
		if (enabled) {
			if (customErr == null)
				customErr = new PrintStream(new ErrToScreen());
			System.setErr(customErr);
		}
		else {
			System.setErr(defaultErr);
		}
	}

	/**
	 * Returns the textbox to write the error output to, or creates a new one if
	 * none has been created yet.
	 */
	private static ErrorDialog getErrorDialog() {
		if (errorDialog == null)
			errorDialog = new ErrorDialog();
		return errorDialog;
	}

	/**
	 * A custom implementation of a printstream that writes into a textbox.
	 */
	static class ErrToScreen extends OutputStream {

		public void write(int b) throws IOException {
			defaultErr.write(b);

			/*
			 * Check if we're running in the GUI thread. If not, we cannot
			 * directly write into the textbox, but instead have to
			 * instantiate a Runnable for each method call, which is
			 * much slower.
			 */
			if (Display.getCurrent() != null) {
				getErrorDialog().append(String.valueOf((char) b));
			}
			else {
				Display display = Display.getDefault();
				if (display == null || display.isDisposed()) return;
				final int fb = b;
				display.syncExec(new Runnable() {
					public void run() {
						getErrorDialog().append(String.valueOf((char) fb));
					}
				});
			}
		}

		public void write(byte[] b, int off, int len) throws IOException {
			defaultErr.write(b, off, len);

			// Check input
			if (b == null)
				throw new NullPointerException("No byte array to write to."); //$NON-NLS-1$
			else if ((off < 0) || (off > b.length) || (len < 0) ||
					((off + len) > b.length) || ((off + len) < 0))
				throw new IndexOutOfBoundsException();
			else if (len == 0)
				return;

			// Convert to StringBuffer
			final StringBuffer buf = new StringBuffer(len);
			for (int i = 0; i < len; i++)
				buf.append((char) b[off + i]);

			// Display message
			/*
			 * Check if we're running in the GUI thread. If not, we cannot
			 * directly write into the textbox, but instead have to
			 * instantiate a Runnable for each method call, which is
			 * much slower.
			 */
			if (Display.getCurrent() != null)
				getErrorDialog().append(buf.toString());
			else {
				Display display = Display.getDefault();
				if (display == null || display.isDisposed()) return;
				display.syncExec(new Runnable() {
					public void run() {
						getErrorDialog().append(buf.toString());
					}
				});
			}
		}

	}

}

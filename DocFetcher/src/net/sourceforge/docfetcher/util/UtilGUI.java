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

package net.sourceforge.docfetcher.util;


import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Msg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * GUI-related utility methods.
 * 
 * @author Tran Nam Quang
 */
public class UtilGUI {

	/**
	 * Static use only.
	 */
	private UtilGUI() {
	}

	/**
	 * Convenience method for retrieving system colors. Enter SWT stylebits as
	 * parameter (e.g. <code>SWT.COLOR_WHITE</code>).
	 */
	public static Color getColor(int styleBit) {		
		return Display.getDefault().getSystemColor(styleBit);		
	}
	
	/**
	 * Places <tt>shell</tt> at the center of the shell <tt>parent</tt>, or
	 * in the middle of the screen if <tt>parent</tt> is null. Note that the
	 * shell size must have already been set in order to get a correct result.
	 */
	public static void centerShell(Shell parent, Shell shell) {
		Rectangle parentBounds = null;
		if (parent == null)
			parentBounds = shell.getMonitor().getBounds();
		else
			parentBounds = parent.getBounds();
		int parentWidth = parentBounds.width;
		int parentHeight = parentBounds.height;
		int shellWidth = shell.getSize().x;
		int shellHeight = shell.getSize().y;
		int shellPosX = (parentWidth - shellWidth) / 2;
		int shellPosY = (parentHeight - shellHeight) / 2;
		if (parent != null) {
			shellPosX += parentBounds.x;
			shellPosY += parentBounds.y;
		}
		shell.setLocation(shellPosX, shellPosY);
	}

	public static Shell getActiveShell() {
		return Display.getDefault().getActiveShell();
	}

	/**
	 * Shows a message box with an error icon. It is expected that the main
	 * application shell has already been created.
	 * 
	 * @param text
	 *            The message box title. If set to null, a generic "System
	 *            Error" string will be displayed.
	 * @param message
	 *            The error message to display.
	 */
	public static void showErrorMsg(final String text, final String message) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					showErrorMsg(text, message);
				}
			});
			return;
		}
		MessageBox msgBox = new MessageBox(getActiveShell(), SWT.ICON_ERROR | SWT.OK | SWT.PRIMARY_MODAL);
		msgBox.setText(text == null ? Msg.system_error.value() : text);
		msgBox.setMessage(message);
		msgBox.open();
	}
	
	/**
	 * Displays an error message. This method can be used before any GUI
	 * components are created, because it creates its own display and shell.
	 */
	public static void showErrorMsgOnStart(String message) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setSize(1, 1);
		UtilGUI.centerShell(null, shell);
		shell.open();
		shell.setVisible(false);
		MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK | SWT.PRIMARY_MODAL);
		msgBox.setText(Msg.system_error.value());
		msgBox.setMessage(message);
		msgBox.open();
	}

	/**
	 * Shows a message box with a question icon. It is expected that the main
	 * application shell has already been created.
	 * 
	 * @param text
	 *            The message box title. If set to null, a generic "Confirm
	 *            Operation" will be displayed.
	 * @param message
	 *            The message to display
	 * @return The return code of the message box, either SWT.OK or SWT.CANCEL
	 */
	public static int showConfirmMsg(final String text, final String message) {
		if (Display.getCurrent() == null) {
			class MyRunnable implements Runnable {
				public int answer = -1;
				public void run() {
					answer = showConfirmMsg(text, message);
				}
			}
			MyRunnable myRunnable = new MyRunnable();
			Display.getDefault().syncExec(myRunnable);
			return myRunnable.answer;
		}
		MessageBox msgBox = new MessageBox(getActiveShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL | SWT.PRIMARY_MODAL);
		msgBox.setText(text == null ? Msg.confirm_operation.value() : text);
		msgBox.setMessage(message);
		return msgBox.open();
	}

	/**
	 * Shows a message box with an information icon. It is expected that the
	 * main application shell has already been created.
	 * 
	 * @param text
	 *            The message box title. If set to null, no title is shown.
	 * @param message
	 *            The message to display
	 */
	public static void showInfoMsg(final String text, final String message) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					showInfoMsg(text, message);
				}
			});
			return;
		}
		MessageBox msgBox = new MessageBox(getActiveShell(), SWT.ICON_INFORMATION | SWT.OK | SWT.PRIMARY_MODAL);
		msgBox.setText(text == null ? "" : text); //$NON-NLS-1$
		msgBox.setMessage(message);
		msgBox.open();
	}

	/**
	 * Shows a message box with a warning icon. It is expected that the main
	 * application shell has already been created.
	 * 
	 * @param text
	 *            The message box title. If set to null, a generic "Invalid
	 *            Operation" message will be displayed.
	 * @param message
	 *            The message to display
	 */
	public static void showWarningMsg(final String text, final String message) {
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					showWarningMsg(text, message);
				}
			});
			return;
		}
		MessageBox msgBox = new MessageBox(DocFetcher.getInst().getShell(), SWT.ICON_WARNING | SWT.OK | SWT.PRIMARY_MODAL);
		msgBox.setText(text == null ? Msg.invalid_operation.value() : text);
		msgBox.setMessage(message);
		msgBox.open();
	}

	/**
	 * Shortens the given string if its length exceeds a fixed limit.
	 */
	public static String truncate(String str) {
		if (str.length() > 32)
			return str.substring(0, 32) + "..."; //$NON-NLS-1$
		return str;
	}

	/**
	 * Converts the given period of time in milliseconds into something more
	 * human-friendly (e.g. "1 h 24 min 3 s").
	 */
	public static String simpleDuration(long millis) {
		int secs = (int) (millis / 1000);
		int hrs = secs / 3600;
		secs -= hrs * 3600;
		int mins = secs / 60;
		secs -= mins * 60;
		String ret = ""; //$NON-NLS-1$
		if (hrs != 0) ret += hrs + " h"; //$NON-NLS-1$
		if (mins != 0) ret += (hrs == 0 ? "" : " ") + mins + " min"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (secs != 0) ret += (hrs == 0 && mins == 0 ? "" : " ") + secs + " s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (ret.equals("")) //$NON-NLS-1$
			return "0 s"; //$NON-NLS-1$
		return ret;
	}
	
	/**
	 * Converts the given raw regex string into an array of valid regular
	 * expressions. The raw regex string is expected to consist of one or more
	 * regular expressions separated by whitespace and/or a '$' character.
	 */
	public static String[] parseExclusionString(String str) {
		int first = 0;
		int last = str.length() - 1;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == '$')
				first += 1;
			else break;
		}
		for (int i = last; i >= 0; i--) {
			if (Character.isWhitespace(str.charAt(i)) || str.charAt(i) == '$')
				last -= 1;
			else break;
		}
		if (first == str.length() || last == -1)
			return new String[0];
		return str.substring(first, last + 1).split("\\s*\\$+\\s*"); //$NON-NLS-1$
	}
	
	public static Point minimum(Point target, Point min) {
		Point out = new Point(target.x, target.y);
		if (out.x < min.x) out.x = min.x;
		if (out.y < min.y) out.y = min.y;
		return out;
	}
	
	private static class SelectAllOnFocus extends MouseAdapter implements FocusListener {
		private boolean selectAllTextAllowed = false;
		private Control text;
		SelectAllOnFocus(Control text) {
			this.text = text;
			if (! (text instanceof Combo) && ! (text instanceof Text))
				throw new IllegalArgumentException();
			text.addFocusListener(this);
			text.addMouseListener(this);
		}
		public void focusGained(FocusEvent e) {
			Point sel = null;
			int textLength = -1;
			if (text instanceof Combo) {
				sel = ((Combo) text).getSelection();
				textLength = ((Combo) text).getText().length();
			}
			else if (text instanceof Text) {
				sel = ((Text) text).getSelection();
				textLength = ((Text) text).getText().length();
			}
			
			selectAllTextAllowed = sel.x != 0 || sel.y != textLength;
		}
		public void focusLost(FocusEvent e) {
		}
		public void mouseDown(MouseEvent e) {
			if (selectAllTextAllowed) {
				if (text instanceof Combo) {
					((Combo) text).setSelection(new Point(0, ((Combo) text).getText().length()));
				}
				else if (text instanceof Text) {
					((Text) text).setSelection(new Point(0, ((Text) text).getText().length()));
				}
			}
			selectAllTextAllowed = false;
		}
	}
	
	/**
	 * Applying this method on the given widget will cause the all in it to be
	 * selected if the user clicks on it after leaving another part of the GUI
	 * or another program. The widget must be a Combo or a Text.
	 */
	public static void selectAllOnFocus(Control text) {
		new SelectAllOnFocus(text);
	}
	
}

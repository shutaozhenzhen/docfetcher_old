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

package net.sourceforge.docfetcher.util;

/**
 * @author Tran Nam Quang
 */
public class Timer {
	
	private static long lastPrintTime = -1;
	
	private Timer() {}
	
	public static void reset() {
		lastPrintTime = -1;
	}
	
	public static void print(String msg) {
		if (lastPrintTime == -1)
			System.out.println("0:\t" + msg); //$NON-NLS-1$
		else
			System.out.println((System.currentTimeMillis() - lastPrintTime) + ":\t" + msg); //$NON-NLS-1$
		lastPrintTime = System.currentTimeMillis();
	}

}

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

package net.sourceforge.docfetcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

/**
 * An event class that provides a much simpler alternative to the Observer
 * pattern: Instead of adding methods like addListener, removeListener, etc. to
 * each class one wants to listen to, this class can simply be added as a field.
 * Moreover, the entire event system can be temporarily disabled used the hold()
 * and flush() methods, which is useful for avoiding mass notification when a
 * lot of changes are made to an observed object.
 * 
 * @author Tran Nam Quang
 */
public class Event {
	
	public interface IObserver {
		public void update();
	}
	
	private static Set<IObserver> cachedObservers = new HashSet<IObserver> ();
	
	private static boolean hold = false;
	
	private Set<IObserver> observers = new HashSet<IObserver> ();
	
	public void add(IObserver observer) {
		observers.add(observer);
	}
	
	public void remove(IObserver observer) {
		observers.remove(observer);
	}
	
	public void fireUpdate() {
		if (! hold)
			delayNotification(observers);
		else
			cachedObservers.addAll(observers);
	}
	
	public static void hold() {
		hold = true;
	}
	
	public static void flush() {
		hold = false;
		delayNotification(cachedObservers);
		cachedObservers.clear();
	}
	
	private static void delayNotification(final Set<IObserver> observers) {
		if (Display.getCurrent() != null) {
			for (IObserver observer : observers)
				observer.update();
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					for (IObserver observer : observers)
						observer.update();
				}
			});
		}
	}

}

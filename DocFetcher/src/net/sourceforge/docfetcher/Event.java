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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

/**
 * An event class that provides a much simpler alternative to the Observer
 * pattern: Instead of adding methods like addListener, removeListener, etc. to
 * each class one wants to listen to, this class can simply be added as a field.
 * <p>
 * Moreover, the entire event system can be temporarily disabled using the
 * static hold() and flush() methods, which is useful for avoiding mass
 * notification when a lot of changes are made to an observed object.
 * <p>
 * The additional type parameter T of this class specifies the type of the event
 * data object that is transmitted on notifications.
 * 
 * @author Tran Nam Quang
 */
public class Event<T> {
	
	public interface Listener<T> {
		public void update(T eventData);
	}
	
	private boolean enabled = true;
	private Set<Listener<T>> observers = new HashSet<Listener<T>> ();
	private List<T> eventCache = new ArrayList<T> ();
	
	public void add(Listener<T> observer) {
		observers.add(observer);
	}
	
	public void addAll(Collection<Listener<T>> observers) {
		this.observers.addAll(observers);
	}
	
	
	public void remove(Listener<T> observer) {
		observers.remove(observer);
	}
	
	public void removeAllObservers() {
		observers.clear();
	}
	
	public Set<Listener<T>> getObservers() {
		return observers;
	}
	
	public void fireUpdate(final T eventData) {
		if (! (enabled && globalEnabled)) return;
		if (hold == 0) {
			if (Display.getCurrent() != null || Display.getDefault() == null) {
				for (Listener<T> observer : new HashSet<Listener<T>> (observers))
					observer.update(eventData);
			}
			else {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						for (Listener<T> observer : new HashSet<Listener<T>> (observers))
							observer.update(eventData);
					}
				});
			}
		}
		else {
			eventCache.add(eventData);
			needsFlushing.add(this);
		}
	}
	
	private void flushCache() {
		if (enabled && globalEnabled)
			for (T eventData : new ArrayList<T> (eventCache))
				for (Listener<T> observer : new HashSet<Listener<T>> (observers))
					observer.update(eventData);
		eventCache.clear();
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Enables or disables this event.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	private static int hold = 0;
	private static boolean globalEnabled = true;
	private static Set<Event<?>> needsFlushing = new HashSet<Event<?>> ();
	
	/**
	 * Temporarily puts the entire event system into a 'caching mode', meaning
	 * that subsequent notification requests caused by changes on the observed
	 * objects will be delayed until <tt>flush</tt> is called. Each
	 * <tt>hold</tt> must be followed by a <tt>flush</tt> some time later.
	 * <p>
	 * Calls to <tt>hold</tt> and <tt>flush</tt> can be nested, so you could,
	 * for example, call <tt>hold</tt> three times, and then <tt>flush</tt>
	 * three times.
	 */
	public static void hold() {
		hold++;
	}
	
	/**
	 * @see #hold()
	 */
	public static void flush() {
		hold = Math.max(0, hold - 1);
		if (hold > 0) return;
		if (Display.getCurrent() != null || Display.getDefault() == null) {
			for (Event<?> event : new HashSet<Event<?>> (needsFlushing))
				event.flushCache();
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					for (Event<?> event : new HashSet<Event<?>> (needsFlushing))
						event.flushCache();
				}
			});
		}
		needsFlushing.clear();
	}
	
	public static boolean isGlobalEnabled() {
		return globalEnabled;
	}
	
	/**
	 * Enables or disables the entire event system.
	 */
	public static void setGlobalEnabled(boolean enabled) {
		globalEnabled = enabled;
	}

}

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

package net.sourceforge.docfetcher.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.ExceptionHandler;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilList;
import net.sourceforge.docfetcher.view.IndexingBox;

import org.eclipse.swt.widgets.Display;

/**
 * A class that handles indexing on file system events.
 * 
 * @author Tran Nam Quang
 */
public class FSEventHandler {
	
	private static FSEventHandler instance;
	
	/**
	 * A map associating all registered <tt>RootScope</tt>s with their
	 * JNotify's watch IDs.
	 */
	private Map<RootScope, Integer> watchIdMap = new HashMap<RootScope, Integer> ();
	
	/**
	 * A cache for incoming indexing requests. This cache is necessary because
	 * the request rate can sometimes be so high that the indexing won't be
	 * handled correctly without this cache.
	 */
	private List<RootScope> eventCache = new ArrayList<RootScope> ();
	
	/**
	 * A thread checking for and executing indexing requests in the request
	 * queue.
	 */
	private Thread eventCacheChecker = null;
	
	/**
	 * Time stamp of the last incoming indexing request.
	 */
	private long lastEventTime = -1;
	
	private FSEventHandler() {
	}
	
	/**
	 * Returns the <tt>FSEventHandler</tt> instance.
	 */
	public static FSEventHandler getInst() {
		if (instance == null)
			instance = new FSEventHandler();
		return instance;
	}
	
	// Updates the internal RootScope-WatchID-map whenever the scope registry changes
	Event.Listener<ScopeRegistry> regChangeHandler = new Event.Listener<ScopeRegistry> () {
		public void update(ScopeRegistry scopeReg) {
			List<RootScope> regEntries = scopeReg.getEntriesList();
			Set<RootScope> mapEntries = watchIdMap.keySet();
			List<RootScope> addedEntries = UtilList.subtract(regEntries, mapEntries);
			List<RootScope> removedEntries = UtilList.subtract(mapEntries, regEntries);
			for (RootScope entry : addedEntries)
				addWatch(entry);
			for (RootScope entry : removedEntries)
				removeWatch(entry);
		}
	};
	
	/**
	 * The file system event listener. It marks the appropriate RootScopes as
	 * 'dirty' and starts the update thread.
	 */
	JNotifyListener fsListener = new JNotifyListener() {
		public void fileCreated(int arg0, String arg1, String arg2) {
			handleEvent(arg1, arg2);
		}
		public void fileDeleted(int arg0, String arg1, String arg2) {
			handleEvent(arg1, arg2);
		}
		public void fileModified(int arg0, String arg1, String arg2) {
			handleEvent(arg1, arg2);
		}
		public void fileRenamed(int arg0, String arg1, String arg2, String arg3) {
			handleEvent(arg1, arg2);
		}
		private void handleEvent(String rootPath, String filePath) {
			// Get the containing RootScope
			if (filePath == null) filePath = ""; //$NON-NLS-1$
			File targetFile = new File(rootPath, filePath);
			RootScope modifiedScope = null;
			for (RootScope candidate : ScopeRegistry.load().getEntries()) {
				if (candidate.getFile().equals(targetFile) || candidate.contains(targetFile)) {
					modifiedScope = candidate;
					break;
				}
			}
			if (modifiedScope == null) return; // Index may have been deleted
			
			// Ignore unparsable files
			if (targetFile.isFile() &&
					! ParserRegistry.canParseIn(modifiedScope, targetFile)) return;
			
			// Check if file was REALLY modified (JNotify tends to fire even when files have only been accessed)
			if (! modifiedScope.getFile().equals(targetFile)) {
				FileWrapper fileWrapper = modifiedScope.getFileWrapperDeep(targetFile);
				if (fileWrapper != null && ! fileWrapper.isModified()) return;
			}
			
			// Put modified scope into the local queue
			if (! eventCache.contains(modifiedScope)) {
				eventCache.add(modifiedScope);
				lastEventTime = System.currentTimeMillis();
				processNextEvent();
			}
		}
	};
	
	// Start queue processing thread if it isn't running yet
	private void processNextEvent() {
		if (eventCacheChecker != null) return;
		eventCacheChecker = new Thread() {
			public void run() {
				if (eventCache.isEmpty()) {
					eventCacheChecker = null;
					return;
				}
				
				// Wait till at least 1000 ms have passed since the last file system event
				while (System.currentTimeMillis() - lastEventTime < 1000) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						break;
					}
				}
				
				Display display = Display.getDefault();
				if (display == null || display.isDisposed()) return;
				display.syncExec(new Runnable() {
					public void run() {
						IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
						if (eventCache.isEmpty()) return;
						indexingBox.addJob(new Job(eventCache.get(0), false, false));
						
						/*
						 * FIXME Opening of the indexing box on file system
						 * events has been disabled because sometimes JNotify
						 * sends notifications even when we've just read a file
						 * without modifying it at all. That becomes a problem
						 * when we open a file for preview: Whenever the preview
						 * file changes, the user will be annoyed by the sight
						 * the indexing box, even though it lasts only for a
						 * split second.
						 */
						//indexingBox.open();
					}
				});
				if (eventCache.isEmpty()) return;
				eventCache.remove(0);
				eventCacheChecker = null;
				processNextEvent();
			}
		};
		eventCacheChecker.start();
	}
	
	/**
	 * Enables or disables the file system event listening feature. Runs in a
	 * thread to avoid slowing down the application.
	 */
	public void setThreadWatchEnabled(final boolean enabled) {
		// Doing this inside a thread is better if lots of watches have to be added or removed
		new Thread() {
			public void run() {
				setWatchEnabled(enabled);
			}
		}.start();
	}
	
	/**
	 * Enables or disables the file system event listening feature.
	 */
	private void setWatchEnabled(boolean enabled) {
		if (enabled) {
			ScopeRegistry.load().getEvtRegistryRootChanged().add(regChangeHandler);
			for (RootScope rootScope : ScopeRegistry.load().getEntries())
				addWatch(rootScope);
		}
		else {
			ScopeRegistry.load().getEvtRegistryRootChanged().remove(regChangeHandler);
			if (watchIdMap.isEmpty()) return;
			for (RootScope rootScope : ScopeRegistry.load().getEntries())
				removeWatch(rootScope);
		}
	}
	
	/**
	 * Enables or disables the file system event listening for the given
	 * <tt>RootScope</tt>s. This method can only be used for temporarily
	 * disabling the watching for certain folders. The effect of this method
	 * will be undone if entries are added to or removed from the
	 * <tt>ScopeRegistry</tt>.
	 */
	public void setWatchEnabled(boolean enabled, Collection<RootScope> targets) {
		if (enabled) {
			for (RootScope rootScope : targets)
				addWatch(rootScope);
		}
		else {
			if (watchIdMap.isEmpty()) return;
			for (RootScope rootScope : targets)
				removeWatch(rootScope);
		}
	}
	
	/**
	 * Enables or disables the file system event listening for the given
	 * <tt>RootScope</tt>s. This method can only be used for temporarily
	 * disabling the watching for certain folders. The effect of this method
	 * will be undone if entries are added to or removed from the
	 * <tt>ScopeRegistry</tt>.
	 */
	public void setWatchEnabled(boolean enabled, RootScope... targets) {
		if (enabled) {
			for (RootScope rootScope : targets)
				addWatch(rootScope);
		}
		else {
			if (watchIdMap.isEmpty()) return;
			for (RootScope rootScope : targets)
				removeWatch(rootScope);
		}
	}
	
	/**
	 * Adds the given RootScope to the watched RootScopes. Ignores non-existent
	 * folders.
	 */
	private void addWatch(RootScope rootScope) {
		if (watchIdMap.containsKey(rootScope)) return;
		File file = rootScope.getFile();
		if (! file.exists()) return;
		try {
			/*
			 * FIXME JNotify 0.91 can somehow throw JNotifyExceptions that
			 * bypass this try-catch-clause. WTF?
			 */
			ExceptionHandler.setEnabled(false);
			int id = JNotify.addWatch(
					file.getAbsolutePath(),
					JNotify.FILE_ANY, true,
					fsListener
			);
			ExceptionHandler.setEnabled(true);
			
			watchIdMap.put(rootScope, id);
		} catch (JNotifyException e) {
			// Ignore
		}
	}
	
	/**
	 * Removes the given RootScope from the list of watched RootScopes.
	 */
	private void removeWatch(RootScope rootScope) {
		if (! watchIdMap.containsKey(rootScope)) return;
		if (! rootScope.getFile().exists()) {
			watchIdMap.remove(rootScope);
			return;
		}
		try {
			/*
			 * FIXME JNotify 0.91 can somehow throw JNotifyExceptions that
			 * bypass this try-catch-clause. WTF?
			 */
			ExceptionHandler.setEnabled(false);
			JNotify.removeWatch(watchIdMap.get(rootScope));
			ExceptionHandler.setEnabled(true);
			
			watchIdMap.remove(rootScope);
		} catch (JNotifyException e) {
			// Ignore
		}
	}

}
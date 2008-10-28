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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.util.UtilGUI;

/**
 * A registry for managing registered scopes.
 * 
 * @author Tran Nam Quang
 */
public class ScopeRegistry implements Serializable {
	
	static final long serialVersionUID = 1;
	
	/**
	 * Singleton instance
	 */
	private static ScopeRegistry instance;
	
	/**
	 * This registry's entries
	 */
	private Set<RootScope> rootScopes = new TreeSet<RootScope> ();
	
	/**
	 * The indexing queue.
	 */
	private transient List<Job> indexingJobs = new ArrayList<Job> ();
	
	/**
	 * The currently processed indexing job.
	 */
	private transient Job currentJob;
	
	/**
	 * The thread that carries out the indexing.
	 */
	private transient Thread indexingThread;
	
	/**
	 * Event: Changes in the indexing queue.
	 */
	private transient Event evtQueueChanged = new Event();
	
	/**
	 * Event: The list of registered <tt>RootScope</tt>'s has changed.
	 */
	private transient Event evtRegistryRootChanged = new Event();
	
	/**
	 * Event: Changes somewhere down the registry.
	 */
	private transient Event evtRegistryChanged = new Event();
	
	/**
	 * Singleton constructor
	 */
	private ScopeRegistry() {}
	
	/**
	 * Event: Changes in the indexing queue.
	 */
	public Event getEvtQueueChanged() {
		return evtQueueChanged;
	}
	
	/**
	 * Event: The list of registered <tt>RootScope</tt>s has changed.
	 */
	public Event getEvtRegistryRootChanged() {
		return evtRegistryRootChanged;
	}
	
	/**
	 * Event: Changes somewhere down the registry.
	 */
	public Event getEvtRegistryChanged() {
		return evtRegistryChanged;
	}
	
	/**
	 * Returns the entries of this registry.
	 */
	public RootScope[] getEntries() {
		return rootScopes.toArray(new RootScope[rootScopes.size()]);
	}
	
	/**
	 * Returns the entries of this registry.
	 */
	public List<RootScope> getEntriesList() {
		return new ArrayList<RootScope> (rootScopes);
	}
	
	/**
	 * Returns the checked entries of this registry.
	 */
	public RootScope[] getCheckedEntries() {
		List<RootScope> checkedEntries = new ArrayList<RootScope> (rootScopes.size());
		for (RootScope entry : rootScopes) {
			if (entry.isChecked())
				checkedEntries.add(entry);
		}
		return checkedEntries.toArray(new RootScope[checkedEntries.size()]);
	}
	
	/**
	 * Checks whether the given <tt>RootScope</tt> intersect with registered
	 * entries or entries in the indexing queue. If so, a string containing a
	 * warning message is returned, which can be used for display in a message
	 * box. Otherwise, null is returned.
	 */
	public String checkIntersection(RootScope... newScopes) {
		for (RootScope newScope : newScopes) {
			if (intersectsEntry(newScope))
				return Msg.inters_indexes.value();
			if (intersectsQueue(newScope))
				return Msg.inters_queue.value();
		}
		return null;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with registered
	 * entries.
	 */
	public boolean intersectsEntry(RootScope newScope) {
		for (RootScope oldScope : rootScopes)
			if (oldScope.equals(newScope) ||
				oldScope.contains(newScope) ||
				newScope.contains(oldScope))
				return true;
		return false;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with entries in
	 * the indexing queue.
	 */
	public boolean intersectsQueue(RootScope newScope) {
		for (Job job : getJobs()) {
			Scope js = job.getScope();
			Scope ns = newScope;
			if (ns.equals(js) || ns.contains(js) || js.contains(ns))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> intersects with entries in
	 * the indexing queue, excluding the currently processed entry.
	 */
	public boolean intersectsInactiveQueue(RootScope newScope) {
		for (Job job : getJobs()) {
			if (job == currentJob) // Don't use equals(..) here, this must be identity
				continue;
			Scope js = job.getScope();
			Scope ns = newScope;
			if (ns.equals(js) || ns.contains(js) || js.contains(ns))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given directory is already registered herein.
	 */
	public boolean containsEntry(File directory) {
		for (RootScope rootScope : rootScopes) {
			if (rootScope.file.equals(directory))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns whether the given <tt>RootScope</tt> is already registered
	 * herein.
	 */
	public boolean containsEntry(RootScope rootScope) {
		return rootScopes.contains(rootScope);
	}
	
	/**
	 * Returns whether the given directory is an index directory of one of the
	 * registered <tt>RootScope</tt> entries. An index directory of a
	 * <tt>RootScope</tt> is the place where its Lucene index files are stored.
	 */
	public boolean containsIndexDir(File indexDir) {
		for (RootScope rootScope : rootScopes) {
			if (rootScope.getIndexDir().equals(indexDir))
				return true;
		}
		return false;
	}

	/**
	 * Removes the given <tt>RootScope</tt>s from the registry and deletes
	 * their corresponding index files.
	 */
	public void remove(RootScope... scopes) {
		for (RootScope rootScope : scopes)
			if (rootScopes.remove(rootScope))
				rootScope.deleteIndex();
		evtRegistryRootChanged.fireUpdate();
		evtRegistryChanged.fireUpdate();
	}
	
	/**
	 * Adds a new indexing job to the queue. It does not check for intersection
	 * with registered entries or entries in the queue. If the indexing box is
	 * visible, the appropriate method in the <tt>IndexingBox</tt> class
	 * should be used istead.
	 */
	public void addJob(Job newJob) {
		indexingJobs.add(newJob);
		newJob.evtReadyStateChanged.add(new Event.IObserver() {
			public void update() {
				evtQueueChanged.fireUpdate();
				startNextJob();
			}
		});
		evtQueueChanged.fireUpdate();
		startNextJob();
	}
	
	/**
	 * Returns the entries of the indexing queue.
	 */
	public Job[] getJobs() {
		return indexingJobs.toArray(new Job[indexingJobs.size()]);
	}
	
	/**
	 * Returns the currently processed entry in the indexing queue. This is not
	 * necessarily the first item in the queue.
	 */
	public Job getCurrentJob() {
		return currentJob;
	}
	
	/**
	 * Returns the entries of the indexing queue that are 'ready for indexing',
	 * i.e. the entries for which the user has given indexing permission by
	 * pressing the 'submit' button on the corresponding indexing tab.
	 */
	public Job[] getSubmittedJobs() {
		List<Job> sj = new ArrayList<Job> ();
		for (Job candidate : indexingJobs)
			if (candidate.isReadyForIndexing())
				sj.add(candidate);
		return sj.toArray(new Job[sj.size()]);
	}
	
	/**
	 * Process the next entry in the indexing queue. After each processed entry,
	 * the method will move on to the next allowed entry until none is left.
	 */
	private void startNextJob() {
		if (indexingThread != null) return;
		
		// Get the next entry that is ready for indexing
		currentJob = null;
		for (Job candidate : indexingJobs) {
			if (candidate.isReadyForIndexing()) {
				currentJob = candidate;
				break;
			}
		}
		
		// Stop if there's no ready entry left
		if (currentJob == null) {
			try {
				save(); // Save registry after queue is emptied, just in case the user successfully kills the app...
			} catch (IOException e) {
				UtilGUI.showErrorMsg(null, Msg.write_error.value());
			}
			return;
		}
		
		evtQueueChanged.fireUpdate();
		indexingThread = new Thread() {
			public void run() {
				try {
					// Caching variables (necessary, because later currentJob may be null)
					boolean addToReg = currentJob.isAddToRegistry();
					boolean doRebuild = currentJob.isDoRebuild();
					RootScope currentScope = currentJob.getScope();
					
					// Indexing
					if (doRebuild && ! addToReg)
						currentScope.reindex();
					else
						currentScope.updateIndex();
					
					// Postprocessing
					boolean interrupted = Thread.currentThread().isInterrupted();
					if (doRebuild && interrupted) {
						if (addToReg)
							currentScope.deleteIndex();
						else
							remove(currentScope);
					}
					else if (addToReg && ! intersectsEntry(currentScope) && ! interrupted) {
						rootScopes.add(currentScope);
						evtRegistryRootChanged.fireUpdate();
					}
					evtRegistryChanged.fireUpdate();
				} catch (FileNotFoundException e) {
					// Nothing; appropriate measures will be taken by aspects
				} finally {
					indexingThread = null;
					indexingJobs.remove(currentJob);
					currentJob = null;
					evtQueueChanged.fireUpdate();
					startNextJob();
				}
			}
		};
		indexingThread.start();
	}

	/**
	 * Removes the given job from the queue (based on an equality check). If the
	 * given job is the currently processed job, the processing will be
	 * terminated and processing of the next entry will start.
	 * <p>
	 * If the indexing box is open, the appropriate method in the
	 * <tt>IndexingBox</tt> should be used instead.
	 */
	public void removeFromQueue(Job job) {
		// Remove entries from queue
		List<Job> removals = new ArrayList<Job> ();
		for (Job candidate : indexingJobs)
			if (candidate.equals(job))
				removals.add(candidate);
		indexingJobs.removeAll(removals);
		
		// Send interrupt signal to the indexing thread if it's processing the given job
		if (job.equals(currentJob)) {
			/*
			 * This assignment here is important, because the next interrupt
			 * call won't be fast enough in doing the very same thing, causing
			 * the program to freeze under some circumstances. (And I have no
			 * idea why...)
			 */
			currentJob = null;
			
			indexingThread.interrupt(); // thread will continue with the next entry
		}
		evtQueueChanged.fireUpdate();
	}
	
	/**
	 * Removes all entries from the indexing queue. If the indexing box is open
	 * the appropriate method in the <tt>IndexingBox</tt> should be used
	 * instead.
	 */
	public void clearQueue() {
		if (indexingThread != null)
			indexingThread.interrupt();
		indexingJobs.clear();
		currentJob = null;
		indexingThread = null;
		evtQueueChanged.fireUpdate();
	}
	
	/**
	 * Loads and returns the singleton instance of this class. Returns the
	 * instance without loading if it has already been loaded.
	 */
	public static ScopeRegistry load() {
		if (instance != null) return instance;
		try {
			File indexParentFile = new File(Const.INDEX_PARENT_PATH);
			instance = (ScopeRegistry) Serializer.load(ScopeRegistry.class, indexParentFile);
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		if (instance == null) {
			instance = new ScopeRegistry();
		}
		else {
			instance.indexingJobs = new ArrayList<Job> ();
			instance.evtQueueChanged = new Event();
			instance.evtRegistryRootChanged = new Event();
			instance.evtRegistryChanged = new Event();
		}
		return instance;
	}
	
	/**
	 * Saves this registry to disk.
	 * 
	 * @throws IOException
	 *             if the write process failed.
	 */
	public void save() throws IOException {
		File indexParentFile = new File(Const.INDEX_PARENT_PATH);
		Serializer.save(this, indexParentFile);
	}
	
}

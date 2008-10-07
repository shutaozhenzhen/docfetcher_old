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

package net.sourceforge.docfetcher.aspect;

import java.io.File;
import java.io.FileNotFoundException;

import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.FileWrapper;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.parse.HTMLParser;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.IndexingBox;

/**
 * Hooks onto various method calls during indexing processes and provides the
 * user with feedback about them by adding text messages to the indexing box.
 * 
 * @author Tran Nam Quang
 */
public aspect IndexingFeedback {
	
	/**
	 * A file counter for the current indexing process.
	 */
	private static int fileCount = 1;

	/**
	 * A file counter over all indexes processes in the queue. Counting starts
	 * when the indexing box opens and stops when the indexing box is closed.
	 */
	private static int multipleFileCount = 0;
	
	/**
	 * Whenever an index modification operation occurs
	 */
	pointcut indexing(RootScope rootScope): call(* RootScope.updateIndex()) && target(rootScope);
	
	/**
	 * Parsing a file, excluding parse processes on files inside folders that
	 * are associated with HTML files, and development related parse processes.
	 */
	pointcut parsing(File file):
		call(* *.parse(File, ..)) && args(file, ..) && withincode (* FileWrapper+.parse(..));
	
	/**
	 * Add the name of the file currently indexed to the info box.
	 */
	before(final File file): parsing(file) {
		Singletons.docFetcher.getIndexingBox().appendInfo(fileCount + "\t" + file.getName()); //$NON-NLS-1$
	}
	
	/**
	 * Increase file counter after one file has been successfully parsed.
	 */
	after(File file) returning(Object output): parsing(file) {
		fileCount++;
	}
	
	/**
	 * Keep track of parse errors, excluding those on files in directories that
	 * are associated with HTML files.
	 */
	before(final ParseException e, RootScope rootScope):
		handler(ParseException) && args(e) &&
		cflow(indexing(rootScope)) && !withincode(* HTMLParser.merge(..)) {
		IndexingBox indexingBox = Singletons.docFetcher.getIndexingBox();
		indexingBox.appendInfo("### " + Msg.file_skipped.format(e.getMessage())); //$NON-NLS-1$
		indexingBox.addError(e);
	}
	
	/**
	 * Feedback on excluded files.
	 */
	after(final File file) returning(boolean excluded): execution(* RootScope.isExcluded(..)) && args(file) {
		if (excluded) {
			IndexingBox indexingBox = Singletons.docFetcher.getIndexingBox();
			indexingBox.appendInfo(Msg.file_skipped.format(file.getName()));
		}
	}
	
	/**
	 * Handle the termination of indexing caused by missing index files.
	 */
	after(RootScope rootScope) throwing(FileNotFoundException e): indexing(rootScope) {
		IndexingBox indexingBox = Singletons.docFetcher.getIndexingBox();
		indexingBox.appendInfo(Msg.target_folder_deleted.value());
		fileCount = 1; // Reset file counter
	}
	
	/**
	 * Displays an appropriate status message at the end of an indexing process if it returns normally.
	 */
	after(final RootScope rootScope) returning(): indexing(rootScope) {
		IndexingBox indexingBox = Singletons.docFetcher.getIndexingBox();
		
		// Set message: Finished without or with errors
		if (indexingBox.getErrors().length == 0)
			indexingBox.appendInfo(Msg.finished.value());
		else
			indexingBox.appendInfo(Msg.finished_with_errors.value());
		
		// Set mesage: total elapsed time
		indexingBox.appendInfo(Msg.total_elapsed_time.format(UtilGUI.simpleDuration(rootScope.getParseTime())));

		// File counters
		if (! Thread.currentThread().isInterrupted()) // Don't increase counter if indexing process was terminated by user
			multipleFileCount += fileCount - 1;
		fileCount = 1; // Reset file counter
	}
	
	/**
	 * Some things to do after the indexing box is closed.
	 */
	after(IndexingBox indexingBox): call(* IndexingBox.close()) && target(indexingBox) {
		String msg = Msg.num_documents_added.format(multipleFileCount);
		Singletons.docFetcher.setStatus(msg);
		multipleFileCount = 0;
	}
	
}

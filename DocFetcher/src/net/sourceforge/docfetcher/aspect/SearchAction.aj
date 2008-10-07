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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.docfetcher.enumeration.Filesize;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.FilesizeGroup;
import net.sourceforge.docfetcher.view.SearchPanel;

/**
 * Allows or disallows searches based on certain conditions and carries out the
 * search.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect SearchAction {
	
	// Setting this avoids errors when searching for very generic terms like "*?".
	static {
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
	}
	
	/**
	 * Finds out if this Action should be enabled/disabled and sets the Enabled
	 * state if necessary.
	 */
	public String checkSearchDisabled() {
		SearchPanel searchPanel = Singletons.searchPanel;
		FilesizeGroup filesizeGroup = Singletons.filesizeGroup;
		if (searchPanel == null || filesizeGroup == null)
			return null;
		
		// Disallow empty search strings
		if (searchPanel.searchBox.getText().trim().equals("")) //$NON-NLS-1$
			return Msg.enter_nonempty_string.value();
		
		// Disallow search when there are no indexes to search in
		if (ScopeRegistry.load().getEntries().length == 0)
			return Msg.search_scope_empty.value();
		/*
		 * Make sure the minimum and maximum filesize values are correct.
		 */
		try {
			String numString1 = filesizeGroup.minField.getText();
			String numString2 = filesizeGroup.maxField.getText();
			int selIndex1 = filesizeGroup.minCombo.getSelectionIndex();
			int selIndex2 = filesizeGroup.maxCombo.getSelectionIndex();
			boolean parsable1 = ! numString1.equals("") && selIndex1 != -1; //$NON-NLS-1$
			boolean parsable2 = ! numString2.equals("") && selIndex2 != -1; //$NON-NLS-1$
			long num1 = parsable1 ? checkRange(numString1, selIndex1) : 0;
			long num2 = parsable2 ? checkRange(numString2, selIndex2) : 0;
			if (parsable1 && parsable2) {
				num1 *= Math.pow(1024, selIndex1);
				num2 *= Math.pow(1024, selIndex2);
				if (num1 > num2)
					return Msg.minsize_not_greater_maxsize.value();
			}
		} catch (NumberFormatException ex) {
			return Msg.filesize_out_of_range.value();
		}
		
		/*
		 * At least one item in the filetype table must be checked.
		 */
		if (! ParserRegistry.hasCheckedParsers())
			return Msg.no_filetypes_selected.value();
		
		return null;
	}
	
	/**
	 * Checks if the provided filesize is smaller than Long.MAX_VALUE bytes and
	 * throws a NumberFormatException if not.
	 * 
	 * @param numString
	 *            The numeric value of the filesize
	 * @param power
	 *            The power corresponding to the unit of the filesize. For byte,
	 *            this is 0, for KB it's 1, for MB 2 and for GB 3.
	 * @return The given numerical string as a long number.
	 */
	private long checkRange(String numString, int power) throws NumberFormatException {
		long num = Long.parseLong(numString);
		long max = Long.MAX_VALUE;
		switch (power) {
		case 0: break;
		case 1:
			long maxKB = Filesize.KB.convert(max, Filesize.Byte);
			if (num <= maxKB) break;
		case 2:
			long maxMB = Filesize.MB.convert(max, Filesize.Byte);
			if (num <= maxMB) break;
		case 3:
			long maxGB = Filesize.GB.convert(max, Filesize.Byte);
			if (num <= maxGB) break;
		default:
			throw new NumberFormatException();
		}
		return num;
	}
	
	/**
	 * When the user presses Enter in the search textbox, check if the search is
	 * allowed. If so, then run the search.
	 */
	after(final Combo searchBox): set(Combo searchBox) && within(SearchPanel) && args(searchBox) {
		searchBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode != SWT.CR) return;
				
				// Generic checks
				String errorMsg = checkSearchDisabled();
				if (errorMsg != null) {
					UtilGUI.showWarningMsg(
							Msg.invalid_operation.value(),
							errorMsg
					);
					return;
				}
				
				/*
				 * Get query string and check if it starts with '*' or '?'. If
				 * so, abort with a MessageBox.
				 */
				String searchString = Singletons.searchPanel.searchBox.getText();
				if (searchString.startsWith("*") || searchString.startsWith("?")) { //$NON-NLS-1$ //$NON-NLS-2$
					UtilGUI.showWarningMsg(
							Msg.invalid_query_syntax.value(),
							Msg.wildcard_first_char.value()
					);
					return;
				}
				
				searchBox.setEnabled(false);
				new SearchThread(searchString).start();
			}
		});
	}
	
	/**
	 * The thread that carries out the search.
	 */
	private class SearchThread extends Thread {
		
		private String searchString;
		
		public SearchThread(String searchString) {
			this.searchString = searchString;
		}
		
		public void run() {
			MultiSearcher multiSearcher = null;
			try {
				// Build a lucene query object
				Query query = new QueryParser(
						Document.contents,
						RootScope.analyzer
				).parse(searchString);

				// Check that all indexes still exist
				RootScope[] rootScopes = ScopeRegistry.load().getEntries();
				for (final RootScope rootScope : rootScopes) {
					if (! rootScope.getIndexDir().exists()) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								UtilGUI.showWarningMsg(
										Msg.folders_not_found_title.value,
										Msg.folders_not_found.value() + "\n" + //$NON-NLS-1$
										rootScope.getIndexDir().getAbsolutePath()
								);
							}
						});
						return;
					}
				}
				
				// Perform search
				Searchable[] searchables = new Searchable[rootScopes.length];
				for (int i = 0; i < searchables.length; i++)
					searchables[i] = new IndexSearcher(rootScopes[i].getIndexDir().getAbsolutePath());
				multiSearcher = new MultiSearcher(searchables);
				Hits hits = multiSearcher.search(query);
				
				// Process results 
				final ResultDocument[] results = new ResultDocument[hits.length()];
				for (int i = 0; i < results.length; i++)
					results[i] = new ResultDocument(hits.doc(i), hits.score(i));
				
				// Get search terms (for term highlighting in the preview panel)
				Set termsSet = new HashSet();
				query = multiSearcher.rewrite(query);
				query.extractTerms(termsSet);
				final String[] terms = new String[termsSet.size()];
				int i = 0;
				for (Object term : termsSet) {
					terms[i] = ((Term) term).text();
					i++;
				}
				
				// Display results
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						Singletons.resultPanel.setResults(results);
						Singletons.previewPanel.setTerms(terms);
						Singletons.resultPanel.setFocus();
						Singletons.searchPanel.searchBox.setEnabled(true);
						Singletons.searchPanel.addToSearchHistory(searchString);
					}
				});
			} catch (final ParseException e) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						UtilGUI.showWarningMsg(null, Msg.invalid_query.value() + "\n" + e.getLocalizedMessage()); //$NON-NLS-1$
					}
				});
			} catch (final IOException e) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						UtilGUI.showWarningMsg(null, e.getLocalizedMessage());
					}
				});
			} finally {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						Singletons.searchPanel.searchBox.setEnabled(true);
						Singletons.searchPanel.setFocus();
					}
				});
				if (multiSearcher != null) {
					try {
						multiSearcher.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}

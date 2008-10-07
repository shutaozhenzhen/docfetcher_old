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

package net.sourceforge.docfetcher.aspect;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolItem;

import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.view.PreviewPanel;
import net.sourceforge.docfetcher.view.ResultPanel;
import net.sourceforge.docfetcher.view.SearchPanel;

/**
 * Handles actions triggered from the search bar buttons and from the result
 * panel.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect SearchPanelAction {
	
	/**
	 * Update result panel and status line after selection changes in the
	 * result panel.
	 */
	after(TableViewer viewer): set(TableViewer ResultPanel.viewer) && args(viewer) {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				onResultPanelSelectionChanged();
			}
		});
	}
	
	/**
	 * Event handler for selection changes in the result panel. This is
	 * made public so that the <tt>KeyHandler</tt> can fake selection change
	 * events.
	 */
	public void onResultPanelSelectionChanged() {
		PreviewPanel preview = Singletons.previewPanel;
		if (preview == null) return;
		ResultDocument doc = (ResultDocument) Singletons.resultPanel.getSelection().getFirstElement();
		if (doc == null) return;
		preview.setFile(doc.getFile(), doc.getParsedBy());
		Singletons.docFetcher.showResultStatus();
	}
	
	/**
	 * Open up preferences dialog when the preferences button is clicked.
	 */
	after(ToolItem prefBt): set(ToolItem SearchPanel.prefBt) && args(prefBt) {
		prefBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Singletons.docFetcher.openPrefPage();
			}
		});
	}
	
	/**
	 * Navigate to previous/next page on the result panel using the
	 * previous/next buttons, enable/disable them accordingly.
	 */
	after(final SearchPanel searchPanel): execution(SearchPanel+.new(..)) && target(searchPanel) {
		final ResultPanel resultPanel = searchPanel.resultPanel;
		
		// Previous page button
		searchPanel.leftBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				resultPanel.previousPage();
				if (resultPanel.getPageIndex() <= 0)
					searchPanel.leftBt.setEnabled(false);
				if (resultPanel.getPageIndex() == resultPanel.getPageCount() - 2)
					searchPanel.rightBt.setEnabled(true);
			}
		});
		
		// Next page button
		searchPanel.rightBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				resultPanel.nextPage();
				if (resultPanel.getPageIndex() == 1)
					searchPanel.leftBt.setEnabled(true);
				if (resultPanel.getPageIndex() >= resultPanel.getPageCount() - 1)
					searchPanel.rightBt.setEnabled(false);
			}
		});
	}
	
	/**
	 * Update status line when the displayed result panel page has changed.
	 */
	after(): execution(* ResultPanel+.previousPage()) || execution(* ResultPanel+.nextPage()) {
		Singletons.docFetcher.showResultStatus();
	}
	
	/**
	 * Update navigation buttons and status line after the ResultPanel has
	 * received new input.
	 */
	after(ResultPanel resultPanel): execution(* ResultPanel.setResults(..)) && target(resultPanel) {
		Singletons.searchPanel.leftBt.setEnabled(resultPanel.getPageIndex() > 0);
		Singletons.searchPanel.rightBt.setEnabled(resultPanel.getPageIndex() < resultPanel.getPageCount() - 1);
		Singletons.docFetcher.showResultStatus();
	}

}

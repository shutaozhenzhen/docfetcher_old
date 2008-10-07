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

import java.io.IOException;

import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.enumeration.Pref.Bool;
import net.sourceforge.docfetcher.enumeration.Pref.Int;
import net.sourceforge.docfetcher.enumeration.Pref.IntArray;
import net.sourceforge.docfetcher.model.FSEventHandler;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.view.IndexingTab;

/**
 * Handles actions triggered by changes in the preferences.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect PreferenceAction {
	
	after(Bool target): set(* Bool.value) && target(target) && !within(Pref) {
		switch (target) {
		case ShowPreview:
			if (Singletons.mainPanel != null) // This is null on program launch
				Singletons.mainPanel.setPreviewVisible(target.value);
			break;
		case ShowFilterPanel: Singletons.docFetcher.setFilterPanelVisible(target.value); break;
		case PreviewBottom: Singletons.mainPanel.setPreviewBottom(target.value); break;
		case WatchFS:
			/*
			 * FIXME Save preferences and scope registry before we remove the watches;
			 * on Windows that may cause a crash.
			 */
			if (! target.value)
				try {
					Pref.save();
					ScopeRegistry.load().save();
				} catch (IOException e) {
				}
			FSEventHandler.getInst().setThreadWatchEnabled(target.value); break;
		case HighlightSearchTerms: Singletons.docFetcher.getPreviewPanel().setHighlighting(target.value); break;
		}
	}
	
	after(Int target): set(* Int.value) && target(target) && !within(Pref) {
		switch (target) {
		case MaxResults: Singletons.resultPanel.refresh(); break;
			
		/*
		 * Synchronize error type column width across indexing tabs.
		 */
		case ErrorTypeColWidth:
			for (IndexingTab tab : Singletons.docFetcher.getIndexingBox().getIndexingTabs()) {
				if (! tab.getJob().isReadyForIndexing()) continue;
				if (tab.progressPanel.errorTypeCol.getWidth() == target.value)
					continue;
				tab.progressPanel.errorTypeCol.setWidth(target.value);
			}
			break;
		
		/*
		 * Synchronize path column width across indexing tabs.
		 */
		case ErrorPathColWidth:
			for (IndexingTab tab : Singletons.docFetcher.getIndexingBox().getIndexingTabs()) {
				if (! tab.getJob().isReadyForIndexing()) continue;
				if (tab.progressPanel.pathCol.getWidth() == target.value)
					continue;
				tab.progressPanel.pathCol.setWidth(target.value);
			}
			break;
		}
	}
	
	after(IntArray target): set(* IntArray.value) && target(target) && !within(Pref) {
		switch (target) {
		
		/*
		 * When sash weight of one indexing tab changes, synchronize sash
		 * weights of other indexing tabs with it.
		 */
		case SashProgressPanelWeights:
			for (IndexingTab tab : Singletons.docFetcher.getIndexingBox().getIndexingTabs()) {
				if (! tab.getJob().isReadyForIndexing()) continue;
				int[] w = tab.progressPanel.sash.getWeights();
				int[] pref_w = target.value;
				if (w[0] == pref_w[0] && w[1] == pref_w[1])
					continue;
				tab.progressPanel.sash.setWeights(pref_w);
			}
			break;
		}
	}

}

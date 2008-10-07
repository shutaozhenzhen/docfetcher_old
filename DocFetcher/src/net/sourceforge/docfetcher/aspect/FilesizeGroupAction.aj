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

import net.sourceforge.docfetcher.enumeration.Filesize;
import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.view.FilesizeGroup;
import net.sourceforge.docfetcher.view.ResultPanel;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;

/**
 * Handles events in the filesize group widget.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect FilesizeGroupAction {
	
	/**
	 * Filters result tabs according to the state of the filesize group widget.
	 */
	private static class FilesizeFilter
	implements ResultPanel.ResultFilter, ModifyListener, SelectionListener {
		
		private static FilesizeFilter instance = new FilesizeFilter();
		
		/**
		 * Cache of the current minimum filesize value.
		 */
		private long minBytes = 0;
		
		/**
		 * Cache of the current minimum filesize value.
		 */
		private long maxBytes = -1;
		
		private FilesizeFilter() {
			// Singleton
		}
		
		/**
		 * Determines which viewer to filter.
		 */
		public static void applyTo(ResultPanel resultTab) {
			resultTab.addFilter(instance);
		}
		
		/**
		 * Adds the given widget to the list of objects this filter will listen
		 * to.
		 */
		public static void listenTo(Text text) {
			text.addModifyListener(instance);
		}
		
		/**
		 * Adds the given widget to the list of objects this filter will listen
		 * to.
		 */
		public static void listenTo(Combo combo) {
			combo.addSelectionListener(instance);
			combo.addModifyListener(instance);
		}
		
		public boolean select(ResultDocument doc) {
			long targetSize = doc.getFile().length();
			boolean minPassed = minBytes <= targetSize;
			boolean maxPassed = maxBytes == -1 ? true : targetSize <= maxBytes;
			return minPassed && maxPassed;
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {updateCache();}
		public void widgetSelected(SelectionEvent e) {updateCache();}
		public void modifyText(ModifyEvent e) {updateCache();}
		
		/**
		 * Updates the cached minimum and maximum filesize values and refreshes all
		 * opened result tabs.
		 */
		private void updateCache() {
			// Set min/max defaults
			long minSize = minBytes = 0;
			long maxSize = maxBytes = -1;
			Filesize minUnit = Filesize.Byte;
			Filesize maxUnit = Filesize.Byte;
			
			// Try to change the defaults
			FilesizeGroup group = Singletons.filesizeGroup;
			try {
				minSize = Long.parseLong(group.minField.getText());
				minUnit = Filesize.valueOf(group.minCombo.getText());
			} catch (Exception e) {
				minSize = 0;
				minUnit = Filesize.Byte;
			}
			try {
				maxSize = Long.parseLong(group.maxField.getText());
				maxUnit = Filesize.valueOf(group.maxCombo.getText());
			} catch (Exception e) {
				maxSize = -1;
				maxUnit = Filesize.Byte;
			}
			
			// Convert to bytes and save to cache
			if (minSize > 0)
				minBytes = Filesize.Byte.convert(minSize, minUnit);
			if (maxSize != -1)
				maxBytes = Filesize.Byte.convert(maxSize, maxUnit);
			
			// Update viewers
			if (Singletons.resultPanel != null)
				Singletons.resultPanel.refresh();
		}
		
	}
	
	/**
	 * Apply filter to newly created result panel.
	 */
	after() returning(ResultPanel resultTab): call(ResultPanel+.new(..)) {
		FilesizeFilter.applyTo(resultTab);
	}
	
	/**
	 * Add the filter object as a listener to the filesize group widget.
	 */
	after() returning(FilesizeGroup group): call(FilesizeGroup+.new(..)) {
		/*
		 * It's important to do the following _after_ the constructor call,
		 * because halfway through the widgets will be modified, which would
		 * trigger an undesirable event chain, resulting in a status line
		 * message "Results: 0" immediately after start up.
		 */
		FilesizeFilter.listenTo(group.minField);
		FilesizeFilter.listenTo(group.maxField);
		FilesizeFilter.listenTo(group.minCombo);
		FilesizeFilter.listenTo(group.maxCombo);
	}

}

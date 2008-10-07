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

package net.sourceforge.docfetcher.view;

import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A panel consisting of the search bar and the result panel.
 * 
 * @author Tran Nam Quang
 */
public class SearchPanel extends Composite {
	
	private Composite searchBar;
	private Combo searchBox;
	private ToolItem leftBt;
	private ToolItem rightBt;
	private ToolItem filterBt;
	private ToolItem previewBt;
	private ToolItem prefBt;
	private ResultPanel resultPanel;
	
	public SearchPanel(Composite parent) {
		super(parent, SWT.NONE);
		searchBar = new Composite(this, SWT.BORDER);
		searchBar.setLayout(new FormLayout());
		
		searchBox = new Combo(searchBar, SWT.BORDER);
		searchBox.setVisibleItemCount(Pref.Int.SearchHistorySize.value);
		UtilGUI.selectAllOnFocus(searchBox);
		
		final Composite toolBarContainer = new Composite(searchBar, SWT.NONE);
		toolBarContainer.setLayout(new FormLayout());
		ToolBar toolBar = new ToolBar(toolBarContainer, SWT.FLAT);
		
		leftBt = new ToolItem(toolBar, SWT.FLAT);
		leftBt.setImage(Icon.ARROW_LEFT.getImage());
		leftBt.setToolTipText(Msg.prev_page.value());
		leftBt.setEnabled(false);
		
		rightBt = new ToolItem(toolBar, SWT.FLAT);
		rightBt.setImage(Icon.ARROW_RIGHT.getImage());
		rightBt.setToolTipText(Msg.next_page.value());
		rightBt.setEnabled(false);
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		filterBt = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		filterBt.setImage(Icon.LAYOUT.getImage());
		filterBt.setToolTipText(Msg.show_filterpanel.value() + " (" + Key.HideFilterPanel.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		filterBt.setSelection(Pref.Bool.ShowFilterPanel.value);
		filterBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.ShowFilterPanel.value = filterBt.getSelection();
			}
		});
		
		previewBt = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		previewBt.setImage(Icon.PREVIEW.getImage());
		previewBt.setToolTipText(Msg.show_preview.value() + " (" + Key.HidePreviewPanel.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		previewBt.setSelection(Pref.Bool.ShowPreview.value);
		previewBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.ShowPreview.value = previewBt.getSelection();
			}
		});
		
		prefBt = new ToolItem(toolBar, SWT.FLAT);
		prefBt.setImage(Icon.PREFERENCES.getImage());
		prefBt.setToolTipText(Msg.preferences.value());
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().right().applyTo(toolBar);
		fdf.reset().setMargin(0).top().bottom().right().applyTo(toolBarContainer);
		fdf.left().right(toolBarContainer).applyTo(searchBox);
		
		// Make the search box smaller when there's not enough space left
		searchBar.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int spaceLeft = searchBar.getSize().x - toolBarContainer.getSize().x - searchBar.getBorderWidth() * 2;
				if (spaceLeft < Pref.Int.SearchBoxMaxWidth.value)
					FormDataFactory.getInstance().setMargin(0)
					.top().bottom()
					.left().right(toolBarContainer)
					.applyTo(searchBox);
			}
		});
		
		// Limit the width of the search box
		searchBox.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int maxWidth = Pref.Int.SearchBoxMaxWidth.value;
				if (searchBox.getSize().x > maxWidth) {
					searchBox.setSize(maxWidth, SWT.DEFAULT); // necessary for avoiding a layout bug
					FormDataFactory.getInstance().setMargin(0)
					.top().bottom()
					.left().width(maxWidth)
					.applyTo(searchBox);
				}
			}
		});
		
		resultPanel = new ResultPanel(this);
		
		setLayout(new FormLayout());
		fdf.reset().setMargin(0).top().left().right().applyTo(searchBar);
		fdf.top(searchBar).bottom().applyTo(resultPanel);
		
		/*
		 * Without this call, the position of the search box will be slightly
		 * off by a few pixels to the top.
		 */
		layout();
	}
	
	/**
	 * Adds the given term to the search history (i.e. the drop down list) of
	 * the search box.
	 */
	public void addToSearchHistory(String term) {
		String[] oldHistory = searchBox.getItems();
		
		// Get length of new search history
		int newHistoryLength = oldHistory.length;
		if (! UtilList.containsEquality(oldHistory, term))
			newHistoryLength += 1;
		newHistoryLength = Math.min(newHistoryLength, Pref.Int.SearchHistorySize.value);
		if (newHistoryLength <= 0) // search history size was set to a value <= 0
			return;
		String[] newHistory = new String[newHistoryLength];

		// Fill new search history
		newHistory[0] = term;
		int j = 1;
		for (int i = 0; i < oldHistory.length && j < newHistory.length; i++) {
			if (! oldHistory[i].equals(term)) {
				newHistory[j] = oldHistory[i];
				j += 1;
			}
		}
		
		searchBox.setItems(newHistory);
		searchBox.setText(term);
	}
	
	public ResultPanel getResultPanel() {
		return resultPanel;
	}
	
	public boolean setFocus() {
		return searchBox.setFocus();
	}
	
	public boolean isFocusControl() {
		return searchBox.isFocusControl();
	}
	
	public void setFilterButtonChecked(boolean checked) {
		filterBt.setSelection(checked);
	}
	
	public void setPreviewButtonChecked(boolean checked) {
		previewBt.setSelection(checked);
	}

}

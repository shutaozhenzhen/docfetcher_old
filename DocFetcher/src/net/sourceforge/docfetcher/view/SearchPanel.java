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

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * A panel consisting of the search bar and the result panel.
 * 
 * @author Tran Nam Quang
 */
public class SearchPanel extends Composite {
	
	public final Event<String> evtSearchRequest = new Event<String> ();
	public final Event<Widget> evtLeftBtClicked = new Event<Widget> ();
	public final Event<Widget> evtRightBtClicked = new Event<Widget> ();
	public final Event<Widget> evtPrefBtClicked = new Event<Widget> ();
	
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
		
		/*
		 * On Windows, we draw our own border because 'style = SWT.BORDER' looks
		 * awful on the classic theme (which some people still seem to use...).
		 */
		searchBar = new Composite(this, Const.IS_WINDOWS ? SWT.NONE : SWT.BORDER);
		if (Const.IS_WINDOWS)
			searchBar.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Point size = searchBar.getSize();
					e.gc.setForeground(UtilGUI.getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
					e.gc.drawRectangle(0, 0, size.x - 1, size.y - 1);
					e.gc.setForeground(UtilGUI.getColor(SWT.COLOR_WHITE));
					e.gc.drawRectangle(1, 1, size.x - 3, size.y - 3);
				}
			});
		FormLayout formLayout = new FormLayout();
		if (Const.IS_WINDOWS)
			formLayout.marginWidth = formLayout.marginHeight = 2;
		searchBar.setLayout(formLayout);
		
		searchBox = new Combo(searchBar, SWT.BORDER);
		searchBox.setVisibleItemCount(Pref.Int.SearchHistorySize.value());
		UtilGUI.selectAllOnFocus(searchBox);
		
		searchBox.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (UtilGUI.isCRKey(e)) 
					evtSearchRequest.fireUpdate(searchBox.getText());
			}
		});
		
		final Composite toolBarContainer = new Composite(searchBar, SWT.NONE);
		toolBarContainer.setLayout(new FormLayout());
		ToolBar toolBar = new ToolBar(toolBarContainer, SWT.FLAT);
		
		leftBt = new ToolItem(toolBar, SWT.FLAT);
		leftBt.setImage(Icon.ARROW_LEFT.getImage());
		leftBt.setToolTipText(Msg.prev_page.value());
		leftBt.setEnabled(false);
		leftBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtLeftBtClicked.fireUpdate(leftBt);
			}
		});
		
		rightBt = new ToolItem(toolBar, SWT.FLAT);
		rightBt.setImage(Icon.ARROW_RIGHT.getImage());
		rightBt.setToolTipText(Msg.next_page.value());
		rightBt.setEnabled(false);
		rightBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evtRightBtClicked.fireUpdate(rightBt);
			}
		});
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		filterBt = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		filterBt.setImage(Icon.LAYOUT.getImage());
		filterBt.setToolTipText(Msg.show_filterpanel.value() + " (" + Key.HideFilterPanel.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		filterBt.setSelection(Pref.Bool.ShowFilterPanel.getValue());
		filterBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.ShowFilterPanel.setValue(filterBt.getSelection());
			}
		});
		
		previewBt = new ToolItem(toolBar, SWT.FLAT | SWT.CHECK);
		previewBt.setImage(Icon.PREVIEW.getImage());
		previewBt.setToolTipText(Msg.show_preview.value() + " (" + Key.HidePreviewPanel.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		previewBt.setSelection(Pref.Bool.ShowPreview.getValue());
		previewBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.ShowPreview.setValue(previewBt.getSelection());
			}
		});
		
		prefBt = new ToolItem(toolBar, SWT.FLAT);
		prefBt.setImage(Icon.PREFERENCES.getImage());
		prefBt.setToolTipText(Msg.preferences.value());
		prefBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Pref.Bool.ShowFilterPanel.setValue(filterBt.getSelection());
				evtPrefBtClicked.fireUpdate(prefBt);
			}
		});
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.setMargin(0).top().bottom().right().applyTo(toolBar);
		fdf.reset().setMargin(0).top().bottom().right().applyTo(toolBarContainer);
		fdf.left().right(toolBarContainer).applyTo(searchBox);
		
		// Make the search box smaller when there's not enough space left
		searchBar.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int spaceLeft = searchBar.getSize().x - toolBarContainer.getSize().x - searchBar.getBorderWidth() * 2;
				if (spaceLeft < Pref.Int.SearchBoxMaxWidth.value())
					FormDataFactory.getInstance().setMargin(0)
					.top().bottom()
					.left().right(toolBarContainer)
					.applyTo(searchBox);
			}
		});
		
		// Limit the width of the search box
		searchBox.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int maxWidth = Pref.Int.SearchBoxMaxWidth.value();
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
		 * Without this call the position of the search box will be slightly
		 * off by a few pixels to the top.
		 */
		layout();
	}
	
	/**
	 * Returns an error message if the current text in the search box does not
	 * allow performing a search, otherwise returns null.
	 */
	public String checkSearchDisabled() {
		if (searchBox.getText().trim().equals("")) //$NON-NLS-1$
			return Msg.enter_nonempty_string.value();
		return null;
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
		newHistoryLength = Math.min(newHistoryLength, Pref.Int.SearchHistorySize.value());
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
	
	public void setSearchBoxEnabled(boolean enabled) {
		searchBox.setEnabled(enabled);
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
	
	public void setLeftBtEnabled(boolean enabled) {
		leftBt.setEnabled(enabled);
	}
	
	public void setRightBtEnabled(boolean enabled) {
		rightBt.setEnabled(enabled);
	}

}

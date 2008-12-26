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

import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.enumeration.Pref;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * The main panel of the application, containing the search bar, the result
 * panel and the preview panel.
 * 
 * @author Tran Nam Quang
 */
public class MainPanel extends Composite {
	
	private SashForm sash;
	private SearchPanel searchPanel;
	private PreviewPanel previewPanel;

	public MainPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		sash = new SashForm(this, Pref.Bool.PreviewBottom.getValue() ? SWT.VERTICAL : SWT.HORIZONTAL);
		searchPanel = new SearchPanel(sash);
		previewPanel = new PreviewPanel(sash);
		if (Pref.Bool.ShowPreview.getValue()) {
			loadSashWeights();
			previewPanel.setActive(true);
		}
		else 
			sash.setMaximizedControl(searchPanel);
		
		Pref.Bool.ShowPreview.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				setPreviewVisible(eventData);
			}
		});
		
		Pref.Bool.PreviewBottom.evtChanged.add(new Event.Listener<Boolean> () {
			public void update(Boolean eventData) {
				setPreviewBottom(eventData);
			}
		});
	}
	
	/**
	 * Sets the sash weights using the values in the preferences.
	 */
	private void loadSashWeights() {
		sash.setWeights(sash.getOrientation() == SWT.VERTICAL ?
				Pref.IntArray.SashRightVerticalWeights.getValue() :
				Pref.IntArray.SashRightHorizontalWeights.getValue()
		);
	}
	
	/**
	 * Returns whether the preview panel is visible.
	 */
	public boolean isPreviewVisible() {
		return sash.getMaximizedControl() == null;
	}
	
	/**
	 * Sets whether the preview panel is visible.
	 */
	private void setPreviewVisible(boolean show) {
		if (show) {
			sash.setMaximizedControl(null);
			previewPanel.setActive(true);
			loadSashWeights();
		}
		else {
			/**
			 * If we hide the preview panel when it's focused, it will keep its
			 * focus even after becoming invisible, which would disable global
			 * key events.
			 */
			if (isPreviewFocused())
				searchPanel.setFocus();
			
			saveWeights();
			previewPanel.setActive(false);
			sash.setMaximizedControl(searchPanel);
		}
		if (show != Pref.Bool.ShowPreview.getValue())
			Pref.Bool.ShowPreview.setValue(show);		searchPanel.setPreviewButtonChecked(show);
	}
	
	/**
	 * Returns whether the preview is shown below the result panel (instead of
	 * on the right).
	 */
	public boolean isPreviewBottom() {
		return sash.getOrientation() == SWT.VERTICAL;
	}
	
	/**
	 * Sets whether the preview is shown below the result panel (instead of
	 * on the right).
	 */
	private void setPreviewBottom(boolean bottom) {
		if (bottom == (sash.getOrientation() == SWT.VERTICAL)) return;
		sash.setOrientation(bottom ? SWT.VERTICAL : SWT.HORIZONTAL);
		if (isPreviewVisible()) {
			if (bottom) {
				Pref.IntArray.SashRightHorizontalWeights.setValue(sash.getWeights());				sash.setWeights(Pref.IntArray.SashRightVerticalWeights.getValue());
			}
			else {
				Pref.IntArray.SashRightVerticalWeights.setValue(sash.getWeights());				sash.setWeights(Pref.IntArray.SashRightHorizontalWeights.getValue());
			}
		}
		if (bottom != Pref.Bool.PreviewBottom.getValue())
			Pref.Bool.PreviewBottom.setValue(bottom);	}
	
	/**
	 * Save the current sash weights to the preferences.
	 */
	public void saveWeights() {
		if (sash.getOrientation() == SWT.VERTICAL)
			Pref.IntArray.SashRightVerticalWeights.setValue(sash.getWeights());		else
			Pref.IntArray.SashRightHorizontalWeights.setValue(sash.getWeights());	}
	
	/**
	 * Bring the focus to the search box.
	 */
	public void focusSearchBox() {
		searchPanel.setFocus();
	}
	
	/**
	 * Returns the search panel.
	 */
	public SearchPanel getSearchPanel() {
		return searchPanel;
	}
	
	/**
	 * Returns the result panel.
	 */
	public ResultPanel getResultPanel() {
		return searchPanel.getResultPanel();
	}
	
	/**
	 * Returns the preview panel.
	 */
	public PreviewPanel getPreviewPanel() {
		return previewPanel;
	}
	
	/**
	 * If the internal HTML viewer is available, this method displays the help
	 * page in it, opens the preview panel if necessary and return true. If not,
	 * it opens the help page in the external HTML browser and returns false.
	 */
	public boolean showHelpPage() {
		if (previewPanel.showHelpPage()) {
			setPreviewVisible(true);
			return true;
		}
		return false;
	}
	
	/**
	 * Sets the check state of the hide/show filter button.
	 */
	public void setFilterButtonChecked(boolean checked) {
		searchPanel.setFilterButtonChecked(checked);
	}
	
	/**
	 * Returns whether the preview panel or one of its child controls have the
	 * user-interface focus.
	 */
	public boolean isPreviewFocused() {
		return hasFocus(previewPanel);
	}
	
	/**
	 * Recursively determines whether the given composite or any of its children
	 * have the user-interface focus.
	 */
	private boolean hasFocus(Composite comp) {
		if (comp.isFocusControl())
			return true;
		for (Control child : comp.getChildren()) {
			if (child instanceof Composite) {
				if (hasFocus((Composite) child))
					return true;
			}
			else if (child.isFocusControl())
				return true;
		}
		return false;
	}
	
}

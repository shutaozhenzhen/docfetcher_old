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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.dev.ExceptionHandler;
import net.sourceforge.docfetcher.dev.ParserTestboxInjector;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.view.IndexingTab;
import net.sourceforge.docfetcher.view.PreviewPanel;
import net.sourceforge.docfetcher.view.SearchPanel;
import net.sourceforge.docfetcher.view.ResultPanel;

/**
 * Provides the application with keyboard shortcuts
 * 
 * @author Tran Nam Quang
 */
public privileged aspect KeyHandler {
	
	/**
	 * Global keys (i.e. accessible from anywhere)
	 */
	after(): execution(* DocFetcher+.createContents(Composite)) {
		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				/*
				 * FIXME This line fixes a bug in SWT 3.2.2 / Windows: If the
				 * current tab displays an SWT browser widget (e.g. this app's
				 * Welcome Page) and the user hits a key, two key events are
				 * triggered, one from the Browser class and one from a class
				 * named Website, This means, for example, that if the key for
				 * hiding/showing the search bar is pressed, its visibility
				 * state will not change after all, because it will be
				 * hidden/shown right after it was shown/hidden. Thus we're
				 * intercepting key events from the Website class here. This is
				 * done using a name check instead of 'instanceof' since this
				 * class is not visible.
				 */
				if (event.widget.getClass().getSimpleName().equals("WebSite")) return; //$NON-NLS-1$
				
				Key key = Key.getKey(event.stateMask, event.keyCode);
				if (key == null) return;
				
				// Disable global keys when the main shell is inactive
				if (Display.getCurrent().getActiveShell() != DocFetcher.getInst().getShell()) return;
				event.doit = false;
				
				switch (key) {
				case Help:
					Singletons.docFetcher.mainPanel.showHelpPage();
					break;
				case FocusSearchBox:
				case FocusSearchBox2:
					Singletons.searchPanel.setFocus();
					break;
				case FocusFilesizeGroup:
					Singletons.filesizeGroup.minField.setFocus();
					break;
				case FocusParserGroup:
					Singletons.parserGroup.viewer.getControl().setFocus();
					break;
				case FocusScopeGroup:
					Singletons.scopeGroup.viewer.getControl().setFocus();
					break;
				case FocusResults:
					if (Singletons.resultPanel != null)
						Singletons.resultPanel.setFocus();
					break;
				case HideFilterPanel:
					Pref.Bool.ShowFilterPanel.value = ! Pref.Bool.ShowFilterPanel.value;
					break;
				case HidePreviewPanel:
					Pref.Bool.ShowPreview.value = ! Pref.Bool.ShowPreview.value;
					break;
				default:
					event.doit = true;
				}
			}
		});
	}
	
	/**
	 * Handler for result panel related keys.
	 */
	static class TabNavigator extends KeyAdapter {
		
		public void keyPressed(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			
			Table table = (Table) e.widget;
			int rowIndex = table.getSelectionIndex();
			
			ResultPanel resultPanel = Singletons.resultPanel;
			
			switch (key) {
			case Up:
				rowIndex = Math.max(0, rowIndex - 1);
				// This won't cause a selection event, therefore we have to call the event handler manually
				table.setSelection(rowIndex);
				SearchPanelAction.aspectOf().onResultPanelSelectionChanged();
				break;
			case Down:
				rowIndex = Math.min(table.getItemCount() - 1, rowIndex + 1);
				// This won't cause a selection event, therefore we have to call the event handler manually
				table.setSelection(rowIndex);
				SearchPanelAction.aspectOf().onResultPanelSelectionChanged();
				break;
			case Arrow_Left:
				e.doit = false; // fall through
			case Left:
				resultPanel.previousPage();
				break;
			case Arrow_Right:
				e.doit = false; // fall through
			case Right:
				resultPanel.nextPage();
				break;
			case Copy:
				resultPanel.copySelectionToClipboard();
				break;
			case Delete:
				resultPanel.deleteSelection();
				break;
			}
		}		
		
	}
	
	/**
	 * This class handles sorting of results by a specific column
	 * using keyboard shortcuts.
	 */
	static class SortSelector extends KeyAdapter {
		
		public void keyPressed(KeyEvent e) {
			if (e.stateMask != SWT.ALT) return;
			int key = 0;
			try {
				key = Integer.valueOf(String.valueOf((char) e.keyCode));
			} catch (NumberFormatException e1) {
				return;
			}
			ResultPanel resultPanel = Singletons.resultPanel;
			if (resultPanel == null) return;
			int columnCount = resultPanel.viewer.getTable().getColumnCount();
			if (key == 0) key = 10;
			if (key > columnCount || key < 1 || key > 10) return;
			ResultPanel.ResultProperty property = ResultPanel.ResultProperty.get(key - 1);
			resultPanel.sortItems(property);
		}
		
	}
	
	after(TableViewer viewer): set(* ResultPanel+.viewer) && args(viewer) {
		viewer.getTable().addKeyListener(new TabNavigator());
		viewer.getTable().addKeyListener(new SortSelector());
	}
	
	/**
	 * A KeyListener that provides the text widgets it is added to (either Text
	 * or StyledText) with a Select-All key.
	 */
	static KeyAdapter stdTextKeyProvider = new KeyAdapter() {
		
		public void keyPressed(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == Key.SelectAll) {
				if (e.widget instanceof Text)
					((Text) e.widget).selectAll();
				else if (e.widget instanceof StyledText)
					((StyledText) e.widget).selectAll();
			}
		}
		
	};
	
	after(Text textWidget): args(textWidget) && (
	set(Text ExceptionHandler.text) ||
	set(Text ParserTestboxInjector.pathField) ||
	set(Text ParserTestboxInjector.infoField)) {
		textWidget.addKeyListener(stdTextKeyProvider);
	}
	
	after() returning(Text textWidget): call(Text.new(..)) && within(IndexingTab) {
		textWidget.addKeyListener(stdTextKeyProvider);
	}
	
	after(StyledText textWidget): args(textWidget) && (
	set(StyledText SearchPanel.searchBox) ||
	set(StyledText ParserTestboxInjector.contentBox) ||
	set(StyledText PreviewPanel.textViewer)) {
		textWidget.addKeyListener(stdTextKeyProvider);
	}

}

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

package net.sourceforge.docfetcher;

import java.io.File;
import java.io.IOException;

import net.sourceforge.docfetcher.Event.IObserver;
import net.sourceforge.docfetcher.dev.ExceptionHandler;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.FSEventHandler;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.FilesizeGroup;
import net.sourceforge.docfetcher.view.FillLayoutFactory;
import net.sourceforge.docfetcher.view.FormDataFactory;
import net.sourceforge.docfetcher.view.IndexingBox;
import net.sourceforge.docfetcher.view.MainPanel;
import net.sourceforge.docfetcher.view.ParserGroup;
import net.sourceforge.docfetcher.view.PrefPage;
import net.sourceforge.docfetcher.view.PreviewPanel;
import net.sourceforge.docfetcher.view.ResultPanel;
import net.sourceforge.docfetcher.view.SashWeightHandler;
import net.sourceforge.docfetcher.view.ScopeGroup;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

/**
 * The main application window.
 * 
 * @author Tran Nam Quang
 */
public class DocFetcher extends ApplicationWindow {
	
	private static DocFetcher docFetcher;
	public static String appName;
	private Composite filterPanel;
	private SashForm sashHorizontal;
	private SashForm sashLeft;
	private MainPanel mainPanel;
	private IndexingBox indexingBox;
	private TrayItem trayItem;
	private Clipboard clipboard; // must be disposed
	
	private ScopeRegistry scopeReg;
	private FSEventHandler fsEventHandler;

	public static void main(String[] args) {
		docFetcher = new DocFetcher();
		docFetcher.setBlockOnOpen(true);
		docFetcher.open();
		Display.getCurrent().dispose();
	}
	
	private DocFetcher() {
		super(null);
		addStatusLine();
		
		// Load preferences and scope registry
		Pref.load();
		appName = Pref.Str.AppName.value;
		if (appName.trim().equals("")) //$NON-NLS-1$
			appName = "DocFetcher"; //$NON-NLS-1$
		
		Display.setAppName(DocFetcher.appName);
		scopeReg = ScopeRegistry.load();
		
		// Remove scope registry entries whose index folders don't exist anymore
		RootScope[] rootScopes = scopeReg.getEntries();
		for (RootScope rootScope : rootScopes)
			if (! rootScope.getIndexDir().exists())
				scopeReg.remove(rootScope);
		
		/*
		 * Wipe out unregistered index folders (possibly from older
		 * installations or program crashes).
		 */
		File[] indexDirs = UtilFile.getSubDirs(Const.INDEX_PARENT_FILE);
		for (File indexDir : indexDirs)
			if (! scopeReg.containsIndexDir(indexDir) && indexDir.getName().matches(".*_[0-9]+")) //$NON-NLS-1$
				UtilFile.delete(indexDir, true);
		
		// Hook onto scope registry and set app name according to number of jobs
		scopeReg.getEvtQueueChanged().add(new IObserver() {
			public void update() {
				Shell shell = getShell();
				if (shell == null) return;
				int cnt = scopeReg.getSubmittedJobs().length;
				String prefix = Msg.jobs.format(cnt) + " - "; //$NON-NLS-1$
				shell.setText((cnt == 0 ? "" : prefix) + DocFetcher.appName); //$NON-NLS-1$
			}
		});
		
		fsEventHandler = FSEventHandler.getInst();
		fsEventHandler.setThreadWatchEnabled(Pref.Bool.WatchFS.value);
	}
	
	public static DocFetcher getInst() {
		return docFetcher;
	}
	
	protected void initializeBounds() {
		// Set shell size
		final Shell shell = getShell();
		int shellWidth = Pref.Int.ShellWidth.value;
		int shellHeight = Pref.Int.ShellHeight.value;
		shell.setSize(shellWidth, shellHeight);
		
		/*
		 * Set shell location. Must be done after setting the shell size,
		 * because the Util.centerShell(..) method depends on a correct shell
		 * size.
		 */
		int shellX = Pref.Int.ShellX.value;
		int shellY = Pref.Int.ShellY.value;
		if (shellX < 0 || shellY < 0)
			UtilGUI.centerShell(null, shell);
		else
			shell.setLocation(shellX, shellY);
		
		shell.setMaximized(Pref.Bool.ShellMaximized.value);

		// Set sash weights
		// Note: This must be done AFTER setting the maximization state of the main shell!
		sashHorizontal.setWeights(Pref.IntArray.SashHorizontalWeights.value);
		sashLeft.setWeights(Pref.IntArray.SashLeftWeights.value);
		
		/*
		 * FIXME On GTK Linux (GNOME 2.22.3), when the user changes the shell
		 * size and closes the program, on the next launch the shell size will
		 * be slightly smaller. This has not been observed on Windows, so this
		 * must be an external bug.
		 */
		// Couple preferences with shell state
		shell.addControlListener(new ControlAdapter() {
			public void controlMoved(ControlEvent e) {
				if (shell.getMaximized() || ! shell.isVisible())
					return;  // Don't store shell position when it's maximized or invisible
				Point pos = shell.getLocation();
				Pref.Int.ShellX.value = pos.x;
				Pref.Int.ShellY.value = pos.y;
			}
			public void controlResized(ControlEvent e) {
				if (shell.getMaximized() || ! shell.isVisible())
					return; // Don't store shell size when it's maximized or invisible
				Point size = shell.getSize();
				Pref.Int.ShellWidth.value = size.x;
				Pref.Int.ShellHeight.value = size.y;
			}
		});
	}
	
	protected void configureShell(final Shell shell) {
		shell.setText(DocFetcher.appName);
		shell.setImages(new Image[] {
				Icon.DOCFETCHER16.getImage(),
				Icon.DOCFETCHER32.getImage(),
				Icon.DOCFETCHER48.getImage(),
		});
		
		super.configureShell(shell);
		
		// Replace shell minimization with sending it to the system tray
		shell.addShellListener(new ShellAdapter() {
			public void shellIconified(ShellEvent e) {
				toSystemTray();
			}
		});
	}
	
	protected Control createContents(Composite parent) {
		clipboard = new Clipboard(getShell().getDisplay());
		
		// Create widgets
		Composite topContainer = new Composite(parent, SWT.NONE);
		topContainer.setLayout(FillLayoutFactory.getInst().margin(5).create());
		sashHorizontal = new SashForm(topContainer, SWT.HORIZONTAL);
		filterPanel = new Composite(sashHorizontal, SWT.NONE);
		mainPanel = new MainPanel(sashHorizontal);
		FilesizeGroup filesizeGroup = new FilesizeGroup(filterPanel);
		sashLeft = new SashForm(filterPanel, SWT.VERTICAL | SWT.SMOOTH);
		ParserGroup parserGroup = new ParserGroup(sashLeft);
		ScopeGroup scopeGroup = new ScopeGroup(sashLeft);
		
		// Layout
		filterPanel.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.top(0, 0).left(0, 0).right(100, -5).applyTo(filesizeGroup);
		fdf.top(filesizeGroup).bottom(100, 0).applyTo(sashLeft);
		
		// Load settings
		filterPanel.setVisible(Pref.Bool.ShowFilterPanel.value);
		parserGroup.setParsers(ParserRegistry.getParsers());
		scopeGroup.setScopes(true, scopeReg.getEntries());
		
		/*
		 * Enabling the sash weight handler must be delayed using a Thread;
		 * otherwise we would get a nasty layout bug that would shrink the width
		 * of the filter panel after each subsequent program launch, provided
		 * the program is terminated in maximized state.
		 */
		new Thread() {
			public void run() {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						new SashWeightHandler(getShell(), sashHorizontal);
					}
				});
			}
		}.start();
		
		// Create indexing box
		indexingBox = new IndexingBox(getShell());
		
		// Try to show help page
		boolean internalBrowserAvailable = false;
		if (Pref.Bool.ShowWelcomePage.value && Pref.Bool.ShowPreview.value)
			internalBrowserAvailable = mainPanel.showHelpPage();
		else
			DocFetcher.getInst().setStatus(Msg.press_help_button.format(Key.Help.toString()));
		Pref.Bool.ShowWelcomePage.value = internalBrowserAvailable;
		
		// Move text cursor to search box
		mainPanel.focusSearchBox();
		
		/*
		 * Do this at the end so developers can see a stacktrace in the Eclipse
		 * console if they haven't set up the run configuration appropriately.
		 */
		ExceptionHandler.setEnabled(true);
		
		return topContainer;
	}
	
	/**
	 * Shows or hides the search bar.
	 */
	public void setFilterPanelVisible(boolean show) {
		filterPanel.setVisible(show);
		sashHorizontal.layout();
		mainPanel.setFilterButtonChecked(show);
	}
	
	/**
	 * Returns whether the search bar is visible.
	 */
	public boolean isFilterPanelVisible() {
		return filterPanel.getVisible();
	}
	
	/**
	 * Returns the application's clipboard.
	 */
	public Clipboard getClipboard() {
		return clipboard;
	}

	public boolean close() {
		if (clipboard != null && ! clipboard.isDisposed())
			clipboard.dispose();
		
		Pref.Bool.FirstLaunch.value = false;
		Pref.Bool.ShellMaximized.value = getShell().getMaximized();
		
		// Store sash weights
		Pref.IntArray.SashHorizontalWeights.value = sashHorizontal.getWeights();
		Pref.IntArray.SashLeftWeights.value = sashLeft.getWeights();
		mainPanel.saveWeights();
		
		// Save preferences and registries
		try {
			Pref.save();
			scopeReg.save();
		} catch (IOException e) {
			UtilGUI.showErrorMsg(null, Msg.write_error.value());
		}
		
		// On Windows, this may cause a crash, so we do this last
		FSEventHandler.getInst().setThreadWatchEnabled(false);
		return super.close();
	}
	
	/**
	 * Sets an error message on the status line.
	 */
	public void setErrorStatus(String msg) {
		if (msg == null || msg.equals("")) //$NON-NLS-1$
			getStatusLineManager().setErrorMessage(null);
		else
			getStatusLineManager().setErrorMessage(Icon.WARNING.getImage(), msg);
	}
	
	public void setStatus(String msg) {
		if (msg == null || msg.equals("")) //$NON-NLS-1$
			getStatusLineManager().setMessage(null, null);
		else
			getStatusLineManager().setMessage(Icon.INFO.getImage(), msg);
	}
	
	/**
	 * Displays a status message about the results in the currently active
	 * result page.
	 */
	public void showResultStatus() {
		// Get active result tab, clear status line if no active result tab
		ResultPanel resultPanel = mainPanel.getResultPanel();
		if (resultPanel == null) {
			setStatus(null);
			return;
		}
		
		// Get total number of visible result items
		int visibleResultCount = resultPanel.getVisibleResultCount();
		
		// Get number of selected results
		IStructuredSelection sel = resultPanel.getSelection();
		int selSize = sel.size();
		
		// Create status message
		String msg = null;
		String space = "     "; //$NON-NLS-1$
		
		// Simple message: "Results: 123"
		if (resultPanel.getPageCount() <= 1)
			msg = Msg.num_results.format(visibleResultCount);
		
		// More complicated message: "Results: 101-200 of 320	Page 2/4"
		else {
			int maxSize = Pref.Int.MaxResults.value;
			int pageIndex = resultPanel.getPageIndex();
			int pageCount = resultPanel.getPageCount();
			int a = pageIndex * maxSize + 1;
			int b = pageIndex + 1 == pageCount ? visibleResultCount : (pageIndex + 1) * maxSize;
			msg = Msg.num_results_detail.format(new Object[] {a, b, visibleResultCount});
			msg += space + Msg.page_m_n.format(new Object[] {pageIndex + 1, pageCount});
		}
		
		// Append selection info if more than 1 item selected
		if (selSize > 1)
			msg += space + Msg.num_sel_results.format(selSize);
		
		setStatus(msg);
	}
	
	/**
	 * Removes the focus from any widget in the application.
	 */
	public void unfocus() {
		Control statusLine = getStatusLineManager().getControl();
		if (statusLine == null || statusLine.isDisposed()) return;
		statusLine.setFocus();
	}
	
	/**
	 * Sends the application to the system tray.
	 */
	public void toSystemTray() {
		// Get shell; return if we're already in the tray
		Shell shell = getShell();
		if (! shell.isVisible()) return;
		
		// Get tray; abort and display error message if it's not available
		Tray tray = shell.getDisplay().getSystemTray();
		if (tray == null) {
			UtilGUI.showErrorMsg(null, Msg.systray_not_available.value());
			return;
		}
		
		/*
		 * If DocFetcher is sent to the system tray while being maximized and
		 * showing a big file on the preview panel, one would experience an
		 * annoying delay once the program returns from the system tray. The
		 * workaround is to deactivate the preview panel before going to the
		 * system tray and reactivate it when we come back.
		 */
		mainPanel.getPreview().setActive(false);
		
		/*
		 * For some reason the shell will have the wrong position without this
		 * when brought back from system tray.
		 */
		Point shellPos = shell.getLocation();
		Pref.Int.ShellX.value = shellPos.x;
		Pref.Int.ShellY.value = shellPos.y;
		
		// Create and configure tray item
		trayItem = new TrayItem (tray, SWT.NONE);
		trayItem.setToolTipText(DocFetcher.appName);
		
		// Set system tray icon
		if (Const.IS_LINUX)
			// On Linux, the default 16x16px image would be too small and lack transparency
			trayItem.setImage(Icon.DOCFETCHER_SYSTRAY_LINUX.getImage());
		else
			trayItem.setImage(Icon.DOCFETCHER16.getImage());
		
		final Menu trayMenu = new Menu(shell, SWT.POP_UP);
		MenuItem restoreItem = new MenuItem(trayMenu, SWT.PUSH);
		MenuItem closeItem = new MenuItem(trayMenu, SWT.PUSH);
		restoreItem.setText(Msg.restore_app.value());
		closeItem.setText(Msg.exit.value());
		trayMenu.setDefaultItem(restoreItem);
		
		shell.setVisible(false);
		
		/*
		 * Event handling
		 */
		// Open system tray menu when the user clicks on it
		trayItem.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				trayMenu.setVisible(true);
			}
		});
		
		// Shut down application when user clicks on the 'close' tray item
		closeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DocFetcher.this.close();
			}
		});
		
		// Restore application when user clicks on the 'restore' item or doubleclicks on the tray icon.
		Listener appRestorer = new Listener() {
			public void handleEvent(Event event) {
				restoreFromSystemTray();
			}
		};
		trayItem.addListener(SWT.Selection, appRestorer);
		restoreItem.addListener(SWT.Selection, appRestorer);
	}
	
	/**
	 * Returns whether the application is hidden in the system tray.
	 */
	public boolean isInSystemTray() {
		return trayItem != null;
	}
	
	/**
	 * Restores application from the system tray.
	 */
	public void restoreFromSystemTray() {
		Shell shell = getShell();
		shell.setVisible(true);
		shell.forceActive();
		shell.setMinimized(false);
		mainPanel.getPreview().setActive(true);
		if (trayItem != null) {
			trayItem.dispose();
			trayItem = null;
		}
		shell.setLocation(Pref.Int.ShellX.value, Pref.Int.ShellY.value);
		mainPanel.focusSearchBox();
	}
	
	/**
	 * Opens up the preferences dialog.
	 */
	public void openPrefPage() {
		new PrefPage(getShell());
	}
	
	/**
	 * Returns the indexing box. Will not return null.
	 */
	public IndexingBox getIndexingBox() {
		return indexingBox;
	}
	
	public ResultPanel getResultPanel() {
		return mainPanel.getResultPanel();
	}
	
	public PreviewPanel getPreviewPanel() {
		return mainPanel.getPreview();
	}

	public void focusSearchBox() {
		mainPanel.focusSearchBox();
	}
	
}

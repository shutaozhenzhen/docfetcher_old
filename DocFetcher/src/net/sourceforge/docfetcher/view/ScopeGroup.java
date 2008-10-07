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

package net.sourceforge.docfetcher.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.Event;
import net.sourceforge.docfetcher.Event.IObserver;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.FSEventHandler;
import net.sourceforge.docfetcher.model.Indexable;
import net.sourceforge.docfetcher.model.Job;
import net.sourceforge.docfetcher.model.ResultDocument;
import net.sourceforge.docfetcher.model.RootScope;
import net.sourceforge.docfetcher.model.Scope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

/**
 * @author Tran Nam Quang
 */
public class ScopeGroup extends GroupWrapper {
	
	private CheckboxTreeViewer viewer;
	private MenuManager contextMenu;
	private Action removeIndexAction;
	private Action updateIndexAction;
	private Action rebuildIndexAction;
	private Action checkFlatAction;
	private Action uncheckFlatAction;
	private Action openDirectoryAction;
	private Action listDocumentsAction;
	private Action createSubfolderAction;
	private Action renameFolderAction;
	private Action deleteFolderAction;
	private Action pasteIntoFolderAction;
	private ISelectionChangedListener selChangedListener;

	public ScopeGroup(Composite parent) {
		super(parent);
		group.setText(Msg.search_scope.value());
		group.setLayout(FillLayoutFactory.getInst().margin(Const.GROUP_MARGIN).create());
		viewer = new CheckboxTreeViewer(group, SWT.BORDER | SWT.MULTI);
		initViewer();
		initDragAndDrop();
		contextMenu = new MenuManager();
		viewer.getTree().setMenu(contextMenu.createContextMenu(viewer.getTree()));
		initContextMenu();

		// Update self on add/remove in scope registry
		ScopeRegistry.load().getEvtRegistryChanged().add(new IObserver() {
			public void update() {
				Object[] expandedElements = viewer.getExpandedElements();
				setScopes(ScopeRegistry.load().getEntries());
				viewer.setExpandedElements(expandedElements);
				updateVisibleCheckStates(new Object[0]);
			}
		});
	}
	
	private void initViewer() {
		// Remove selection when viewer loses focus
		viewer.getTree().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((Tree) e.widget).deselectAll();
				/*
				 * Manual deselection won't fire a selection changed event, so
				 * we have to it ourselves.
				 */
				selChangedListener.selectionChanged(
						new SelectionChangedEvent(
								viewer,
								viewer.getSelection()
						)
				);
			}
		});
		
		viewer.getTree().addKeyListener(new ScopeGroupKeys());
		
		viewer.setContentProvider(new ITreeContentProvider() {
			public Object[] getElements(Object inputElement) {
				return (RootScope[]) inputElement;
			}
			public Object[] getChildren(Object parentElement) {
				return ((Scope) parentElement).getChildren();
			}
			public boolean hasChildren(Object element) {
				return getChildren(element).length > 0;
			}
			public Object getParent(Object element) {
				return ((Indexable) element).getParent();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			public void dispose() {}
		});
		
		viewer.setLabelProvider(new org.eclipse.jface.viewers.LabelProvider() {
			public String getText(Object element) {
				Indexable scope = (Indexable) element;
				File file = scope.getFile();
				String label = file.getName();
				if (label.equals("")) // applies to root directories, i.e. "C:\\" //$NON-NLS-1$
					label = file.getAbsolutePath();
				else if(scope instanceof RootScope)
					label += " --- " + file.getAbsolutePath(); //$NON-NLS-1$
				return label;
			}
		});
		
		viewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((Indexable) e1).compareTo((Indexable) e2);
			}
		});
		
		/*
		 * Because the check states of the viewer items are maintained manually,
		 * they have to be kept in sync with tree expansion events.
		 */
		viewer.addTreeListener(new ITreeViewerListener() {
			public void treeExpanded(TreeExpansionEvent event) {
				Indexable[] newScopes = ((Scope) event.getElement()).getChildren();
				updateVisibleCheckStates(newScopes);
			}
			public void treeCollapsed(TreeExpansionEvent event) {
				// Do nothing
			}
		});
		
		/*
		 * Check/uncheck children of checked/unchecked item and propagate
		 * checkstate changes in viewer back to the model.
		 */
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Event.hold();
				checkDeep((Scope) event.getElement(), event.getChecked());
				Event.flush();
				updateVisibleCheckStates(new Object[] {event.getElement()});
			}
			/**
			 * Recursively checks or unchecks all Scopes under the given Scope,
			 * including the latter.
			 */
			private void checkDeep(Scope scope, boolean checked) {
				scope.setChecked(checked);
				for (Scope child : scope.getChildren())
					checkDeep(child, checked);
			}
		});
	}
	
	/**
	 * Provides drag & drop for this viewer.
	 */
	private void initDragAndDrop() {
		int operations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT | DND.DROP_LINK;
		Transfer[] types = new Transfer[] {FileTransfer.getInstance(), TextTransfer.getInstance()};
		
		viewer.addDropSupport(operations, types, new DropTargetAdapter() {
			
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
			}
			
			public void dragOperationChanged(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
			}
			
			public void drop(DropTargetEvent event) {
				// Abort if shells other than the main shell and the indexing box are open
				Shell[] shells = Display.getDefault().getShells();
				Shell mainShell = DocFetcher.getInst().getShell();
				Shell indexingBoxShell = DocFetcher.getInst().getIndexingBox().getShell();
				for (Shell shell : shells)
					if (! shell.equals(mainShell) && ! shell.equals(indexingBoxShell))
						return;
				
				// Get path strings
				String[] paths = null;
				if (event.data instanceof String)
					paths = new String[] {(String) event.data};
				else if (event.data instanceof String[])
					paths = (String[]) event.data;
				else {
					UtilGUI.showWarningMsg(null, Msg.invalid_dnd_source.value());
					return;
				}
				
				// Abort if drop objects include files or if they're all contained in the ScopeRegistry
				List<RootScope> newScopes = new ArrayList<RootScope> ();
				for (String path : paths) {
					File file = new File(path);
					if (! file.isDirectory()) {
						UtilGUI.showWarningMsg(null, Msg.contains_file.value());
						return;
					}
					newScopes.add(new RootScope(file));
				}
				String msg = ScopeRegistry.load().checkIntersection(newScopes.toArray(new RootScope[newScopes.size()]));
				if (msg != null) {
					UtilGUI.showWarningMsg(Msg.invalid_operation.value(), msg);
					return;
				}
				
				// Run indexing
				IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
				for (RootScope newScope : newScopes)
					indexingBox.addJob(new Job(newScope, true, true));
				indexingBox.open();
			}
			
		});
	}
	
	/**
	 * Fills the context menu of the ScopeGroup.
	 */
	private void initContextMenu() {
		// This listener sets the enabled state of the context menu entries according to the current selection
		viewer.addSelectionChangedListener(selChangedListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				List<RootScope> candidates = new ArrayList<RootScope> (sel.size());
				Iterator it = sel.iterator();
				while (it.hasNext()) {
					Object item = it.next();
					if (item instanceof RootScope) // Only enable action for RootScopes (not for other Scopes)
						candidates.add((RootScope) item);
				}
				setRootEnabled(candidates.size() > 0);
				setNonEmptySelectionEnabled(! sel.isEmpty());
				setOneElementSelectedEnabled(sel.size() == 1);
			}
			private void setRootEnabled(boolean enabled) {
				updateIndexAction.setEnabled(enabled);
				rebuildIndexAction.setEnabled(enabled);
				removeIndexAction.setEnabled(enabled);
				listDocumentsAction.setEnabled(enabled);
				
			}
			private void setNonEmptySelectionEnabled(boolean enabled) {
				checkFlatAction.setEnabled(enabled);
				uncheckFlatAction.setEnabled(enabled);
				openDirectoryAction.setEnabled(enabled);
				deleteFolderAction.setEnabled(enabled);
			}
			private void setOneElementSelectedEnabled(boolean enabled) {
				createSubfolderAction.setEnabled(enabled);
				pasteIntoFolderAction.setEnabled(enabled);
				renameFolderAction.setEnabled(enabled);
			}
		});
		
		// Create context menu entries
		contextMenu.add(new CreateIndexAction());
		contextMenu.add(new Separator());
		contextMenu.add(updateIndexAction = new UpdateIndexAction());
		contextMenu.add(rebuildIndexAction = new RebuildIndexAction());
		contextMenu.add(new Separator());
		contextMenu.add(removeIndexAction = new RemoveIndexAction());
		contextMenu.add(new Separator());
		contextMenu.add(checkFlatAction = new CheckFlatAction(true));
		contextMenu.add(uncheckFlatAction = new CheckFlatAction(false));
		contextMenu.add(new Separator());
		contextMenu.add(openDirectoryAction = new OpenDirectoryAction());
		contextMenu.add(listDocumentsAction = new ListDocumentsAction());
		contextMenu.add(new Separator());
		contextMenu.add(createSubfolderAction = new CreateSubfolderAction());
		contextMenu.add(renameFolderAction = new RenameFolderAction());
		contextMenu.add(deleteFolderAction = new DeleteFolderAction());
		contextMenu.add(pasteIntoFolderAction = new PasteIntoFolderAction());
		
		// Disable actions on startup
		Action[] actions = new Action[] {
				updateIndexAction, rebuildIndexAction, removeIndexAction,
				checkFlatAction, uncheckFlatAction,
				openDirectoryAction, listDocumentsAction, createSubfolderAction,
				renameFolderAction, deleteFolderAction, pasteIntoFolderAction};
		for (Action action : actions)
			action.setEnabled(false);
	}
	
	/**
	 * Sets the RootScopes to be displayed in this widget. If you want all
	 * viewer elements to have the same check state, use
	 * <tt>setScopes(boolean, RootScopes[])</tt> instead, because the latter
	 * is faster.
	 */
	public void setScopes(RootScope... scopes) {
		viewer.setInput(scopes);
		updateVisibleCheckStates(new Object[0]);
	}
	
	/**
	 * Sets the RootScopes to be displayed in this widget, which all have the
	 * check state given by <tt>checkState</tt>. 
	 */
	public void setScopes(boolean checkState, RootScope... scopes) {
		viewer.setInput(scopes);
		for (Scope scope : scopes)
			viewer.setSubtreeChecked(scope, checkState);
	}
	
	/**
	 * Updates the check states of the visible viewer elements and the elements
	 * corresponding to the Scopes given in and under <tt>includedScopes</tt>
	 * according to the check states in the model. All other elements are left
	 * unchecked.
	 * <p>
	 * The <tt>includedScopes</tt> parameter's purpose is to include Scopes
	 * which aren't expanded yet, but will be expanded some time after the call
	 * of this method. In particular, TreeEvents (expand or collapse) indicate
	 * which elements are <i>going</i> to be expanded.
	 */
	private void updateVisibleCheckStates(Object[] includedScopes) {
		List<Scope> checkedItems = new ArrayList<Scope> ();
		for (Object includedItem : includedScopes) {
			Scope scope = (Scope) includedItem;
			if (scope.isChecked())
				checkedItems.add(scope);
			addVisibleCheckedChildren(scope, checkedItems);
		}
		for (RootScope rootScope : ScopeRegistry.load().getEntries()) {
			if (rootScope.isChecked())
				checkedItems.add(rootScope);
		}
		Object[] expandedItems = viewer.getVisibleExpandedElements();
		for (Object expandedItem : expandedItems) {
			for (Scope subScope : ((Scope) expandedItem).getChildren()) {
				if (subScope.isChecked())
					checkedItems.add(subScope);
			}
		}
		viewer.setCheckedElements(checkedItems.toArray());
	}
	
	/**
	 * Adds all Scopes under the given Scope which are visible and checked
	 * according to the model to the given list of Scopes.
	 */
	private void addVisibleCheckedChildren(Scope scope, List<Scope> output) {
		if (! viewer.getExpandedState(scope)) return;
		for (Scope subScope : scope.getChildren()) {
			if (subScope.isChecked())
				output.add(subScope);
			addVisibleCheckedChildren(subScope, output);
		}
	}
	
	/**
	 * Returns the currently selected <tt>RootScope</tt>s.
	 */
	public RootScope[] getRootSelection() {
		StructuredSelection sel = (StructuredSelection) viewer.getSelection();
		List<RootScope> selRootScopes = new ArrayList<RootScope> (sel.size());
		Iterator it = sel.iterator();
		while (it.hasNext()) {
			Object item = it.next();
			if (item instanceof RootScope) // Only enable action for RootScopes (not for other Scopes)
				selRootScopes.add((RootScope) item);
		}
		return selRootScopes.toArray(new RootScope[selRootScopes.size()]);
	}
	
	/**
	 * Returns the currently selected <tt>Scope</tt>s (including the
	 * <tt>RootScope</tt>s.)
	 */
	public Scope[] getSelection() {
		Object[] objects = ((StructuredSelection) viewer.getSelection()).toArray();
		Scope[] scopes = new Scope[objects.length];
		System.arraycopy(objects, 0, scopes, 0, objects.length);
		return scopes;
	}
	
	/**
	 * Performs an index update for all selected <tt>RootScope</tt>s. It
	 * checks whether the index folders still exist. If not, an error message is
	 * displayed.
	 */
	private void updateSelectedIndexes(boolean doRebuild) {
		Scope[] checkedScopes = getExistingSelection(getRootSelection());
		if (checkedScopes.length == 0) return;
		IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
		for (Scope scope : checkedScopes)
			indexingBox.addJob(new Job((RootScope) scope, false, doRebuild));
		indexingBox.open();
	}
	
	/**
	 * Activates context menu entries through keyboard shortcuts.
	 */
	private class ScopeGroupKeys extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			switch (key) {
			case Insert:
				IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
				if (! indexingBox.addJobFromDialog())
					indexingBox.close();
				else
					indexingBox.open();
				break;
			case Update:
				if (updateIndexAction.isEnabled())
					updateIndexAction.run();
				break;
			case Delete: 
				if (removeIndexAction.isEnabled())
					removeIndexAction.run();
				break;
			case Enter:
				if (openDirectoryAction.isEnabled())
					openDirectoryAction.run();
				break;
			case ShiftInsert:
				if (createSubfolderAction.isEnabled())
					createSubfolderAction.run();
			case Rename:
				if (renameFolderAction.isEnabled())
					renameFolderAction.run();
				break;
			case ShiftDelete:
				if (deleteFolderAction.isEnabled())
					deleteFolderAction.run();
				break;
			case Paste:
				if (pasteIntoFolderAction.isEnabled())
					pasteIntoFolderAction.run();
				break;
			default: break;
			}
		}
	}
	
	/**
	 * Returns all <tt>Scope</tt>s in the given array of <tt>Scope</tt>s whose
	 * corresponding directories still exist. Shows a warning message if some of
	 * them don't exist anymore.
	 */
	private Scope[] getExistingSelection(Scope[] scopes) {
		if (scopes.length == 0) return new Scope[0];
		List<Scope> existing = new ArrayList<Scope> (scopes.length);
		List<Scope> missing = new ArrayList<Scope> (scopes.length);
		for (Scope scope : scopes) {
			if (scope.getFile().exists())
				existing.add(scope);
			else
				missing.add(scope);
		}
		if (! missing.isEmpty()) {
			String items = "\n" + UtilList.toString(missing, "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			UtilGUI.showWarningMsg(Msg.folders_not_found_title.value(), Msg.folders_not_found.value() + items);
		}
		return existing.toArray(new Scope[existing.size()]);
	}
		
	
	/**
	 * Action to add scopes to the search scope.
	 */
	class CreateIndexAction extends Action {
		public CreateIndexAction() {
			setText(Msg.create_index.value());
			setAccelerator(Key.Insert.keyCode);
		}
		public void run() {
			IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
			if (! indexingBox.addJobFromDialog())
				indexingBox.close();
			else
				indexingBox.open();
		}
	}

	/**
	 * Action to remove currently selected scopes.
	 */
	class RemoveIndexAction extends Action {
		public RemoveIndexAction() {
			setText(Msg.remove_index.value());
			setAccelerator(Key.Delete.keyCode);
		}
		public void run() {
			RootScope[] scopes = getRootSelection();
			/*
			 * This check is needed because we don't want the message box to pop up
			 * when the user hits DELETE without selecting anything.
			 */
			if (scopes.length == 0) return;
			int ans = UtilGUI.showConfirmMsg(null, Msg.remove_sel_indexes.value());
			if (ans == SWT.OK)
				ScopeRegistry.load().remove(scopes);
		}
	}
	
	/**
	 * Action to find and index new files in the selected scopes.
	 */
	class UpdateIndexAction extends Action {
		public UpdateIndexAction() {
			setText(Msg.update_index.value());
			setAccelerator(Key.Update.keyCode);
		}
		public void run() {
			updateSelectedIndexes(false);
		}
	}
	
	/**
	 * Action to fully reindex the selected scopes.
	 */
	class RebuildIndexAction extends Action {
		public RebuildIndexAction() {
			setText(Msg.rebuild_index.value());
		}
		public void run() {
			updateSelectedIndexes(true);
		}
		
	}
	
	/**
	 * Action to check/uncheck the selected scopes only. This class
	 * can turn into a Check All or Uncheck All action, depending on the Boolean
	 * it is initialized with.
	 */
	class CheckFlatAction extends Action {
		
		/**
		 * Whether this Action checks or unchecks selected Scopes.
		 */
		private boolean checked = true;
		
		/**
		 * @param select true: CheckFlatAction; false: UncheckFlatAction
		 */
		public CheckFlatAction(boolean checked) {
			setText(checked ? Msg.check_toplevel_only.value() : Msg.uncheck_toplevel_only.value());
			this.checked = checked;
		}
		
		public void run() {
			IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
			Iterator it = sel.iterator();
			Event.hold();
			while (it.hasNext())
				((Scope) it.next()).setChecked(checked);
			Event.flush();
			updateVisibleCheckStates(sel.toArray());
		}
		
	}
	
	/**
	 * An action class that opens the directories corresponding to the Scopes
	 * selected in the Scope group.
	 */
	class OpenDirectoryAction extends Action {
		public OpenDirectoryAction() {
			setText(Msg.open_folder.value());
			setAccelerator(Key.Enter.getAccelerator());
		}
		public void run() {
			Scope[] scopes = getExistingSelection(getSelection());
			if (scopes.length == 0) return;
			int openLimit = Pref.Int.OpenLimit.value;
			if (scopes.length > openLimit) {
				UtilGUI.showInfoMsg(null, Msg.open_limit.format(openLimit));
				return;
			}
			for (Scope scope : scopes)
				Program.launch(scope.getFile().getAbsolutePath());
		}
	}
	
	/**
	 * An action class that lists all documents under the selected
	 * <tt>RootScope</tt>s.
	 */
	class ListDocumentsAction extends Action {
		public ListDocumentsAction() {
			setText(Msg.list_docs.value());
		}
		public void run() {
			final Scope[] scopes = getExistingSelection(getRootSelection());
			if (scopes.length == 0) return;
			new Thread() {
				public void run() {
					try {
						IndexReader[] readers = new IndexReader[scopes.length];
						for (int i = 0; i < scopes.length; i++)
							readers[i] = IndexReader.open(((RootScope) scopes[i]).getIndexDir());
						MultiReader multiReader = new MultiReader(readers);
						ResultDocument[] docs = new ResultDocument[multiReader.numDocs()];
						for (int i = 0; i < multiReader.numDocs(); i++)
							docs[i] = new ResultDocument(multiReader.document(i), 0);
						multiReader.close();
						final ResultDocument[] finalDocs = docs;
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								DocFetcher.getInst().getResultPanel().setResults(finalDocs);
								DocFetcher.getInst().getPreviewPanel().setTerms(new String[0]);
							}
						});
					} catch (CorruptIndexException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	/**
	 * Action class for creating a subfolder in the currently selected folder.
	 */
	class CreateSubfolderAction extends Action {
		public CreateSubfolderAction() {
			setText(Msg.create_subfolder.value());
			setAccelerator(Key.ShiftInsert.getAccelerator());
		}
		public void run() {
			Scope[] scopes = getExistingSelection(getSelection());
			if (scopes.length == 0) return;
			RootScope rootScope = scopes[0].getRootScope();
			File parentFolder = scopes[0].getFile();
			
			String suggestedName = UtilFile.suggestNewSubfolderName(parentFolder);
			InputDialog inputDialog = new InputDialog(getShell(),
					Msg.enter_folder_name.value(),
					Msg.enter_folder_name_new.value(),
					suggestedName);
			String newFolderName = inputDialog.open();
			if (newFolderName == null) return;
			File newFolder = new File(parentFolder.getAbsolutePath(), newFolderName);
			if (newFolder.exists()) {
				UtilGUI.showInfoMsg(null, Msg.folder_already_exists.value());
				return;
			}
			
			// Create folder
			FSEventHandler.getInst().setWatchEnabled(false, rootScope);
			boolean success = newFolder.mkdir();
			FSEventHandler.getInst().setWatchEnabled(Pref.Bool.WatchFS.value, rootScope);
			
			if (! success) {
				UtilGUI.showErrorMsg(null, Msg.create_subfolder_failed.value());
			}
			else {
				// Update indexes, but silently
				IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
				indexingBox.addJob(new Job(rootScope, false, false));
			}
		}
	}
	
	/**
	 * Action class for renaming the currently selected folder.
	 */
	class RenameFolderAction extends Action {
		public RenameFolderAction() {
			setText(Msg.rename_folder.value());
			setAccelerator(Key.Rename.keyCode);
		}
		public void run() {
			Scope[] scopes = getExistingSelection(getSelection());
			if (scopes.length == 0) return;
			RootScope rootScope = scopes[0].getRootScope();
			File targetFolder = scopes[0].getFile();
			IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
			
			// Show confirmation message for root folders
			if (scopes[0] instanceof RootScope) {
				int ans = UtilGUI.showConfirmMsg(null, Msg.rename_requires_full_rebuild.value());
				if (ans != SWT.OK) return;
			}
			
			// Get new name from input dialog
			InputDialog inputDialog = new InputDialog(getShell(),
					Msg.enter_folder_name.value(), Msg.enter_new_foldername.value(), targetFolder.getName());
			String input = inputDialog.open();
			if (input == null) return;
			if (targetFolder.getName().equals(input)) return;
			
			// Try to rename file
			File newFile = new File(targetFolder.getParentFile().getAbsolutePath(), input);
			FSEventHandler.getInst().setWatchEnabled(false, rootScope);
			boolean success = targetFolder.renameTo(newFile);
			FSEventHandler.getInst().setWatchEnabled(Pref.Bool.WatchFS.value, rootScope);
			if (! success) {
				UtilGUI.showErrorMsg(null, Msg.cant_rename_folder.value());
				return;
			}
			
			// Update or rebuild index
			if (scopes[0] instanceof RootScope) {
				// Rebuild index if a root folder was renamed
				ScopeRegistry.load().remove(rootScope);
				RootScope newRootScope = new RootScope(newFile);
				Job job = new Job(newRootScope, true, true);
				job.setReadyForIndexing(true);
				indexingBox.addJob(job);
				indexingBox.open();
			}
			else {
				// Just update the index for non-root folders
				indexingBox.addJob(new Job(rootScope, false, false));
				indexingBox.open();
			}
		}
	}

	/**
	 * Action class for removing the currently selected folder.
	 */
	class DeleteFolderAction extends Action {
		public DeleteFolderAction() {
			setText(Msg.delete_folder.value());
			setAccelerator(Key.ShiftDelete.getAccelerator());
		}
		public void run() {
			Scope[] scopes = getExistingSelection(getSelection());
			if (scopes.length == 0) return;
			final List<Scope> scopeList = UtilList.toList(scopes);
			
			// Remove entries from deletion list that are inside other to be deleted items
			final UtilList.Equality<Scope, Scope> parentChildMapper
			= new UtilList.Equality<Scope, Scope>() {
				public boolean equals(Scope obj1, Scope obj2) {
					return UtilFile.contains(obj1.getFile(), obj2.getFile());
				}
			};
			UtilList.remove(scopeList, new UtilList.Selector<Scope> () {
				public boolean select(Scope obj) {
					return UtilList.containsEquality(scopeList, obj, parentChildMapper);
				}
			});
			
			// Ask user to confirm operation
			String msg = Msg.delete_folder_q.value() + Const.LS + UtilList.toString(scopeList, Const.LS);
			int ans = UtilGUI.showConfirmMsg(null, msg);
			if (ans != SWT.OK) return;
			
			// Do it
			Set<Job> jobs = new HashSet<Job> (scopeList.size());
			for (Scope scope : scopeList) {
				RootScope rootScope = scope.getRootScope();
				File targetFolder = scope.getFile();
				FSEventHandler.getInst().setWatchEnabled(false, rootScope);
				if (scope instanceof RootScope) {
					ScopeRegistry.load().remove(rootScope);
					UtilFile.delete(targetFolder, true);
				}
				else {
					UtilFile.delete(targetFolder, true);
					FSEventHandler.getInst().setWatchEnabled(Pref.Bool.WatchFS.value, rootScope);
					jobs.add(new Job(rootScope, false, false));
				}
			}
			IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
			for (Job job : jobs)
				indexingBox.addJob(job);
		}
	}
	
	/**
	 * An Action class that moves all files from the clipboard into the selected
	 * folder.
	 */
	class PasteIntoFolderAction extends Action {
		public PasteIntoFolderAction() {
			setText(Msg.paste_into_folder.value());
			setAccelerator('V' | Key.Paste.stateMask);
		}
		public void run() {
			// Get new parent directory to move the files to
			Scope[] scopes = getExistingSelection(getSelection());
			if (scopes.length == 0) return;
			File newParent = scopes[0].getFile();
			RootScope rootScopeToUpdate = scopes[0].getRootScope();
			
			// Get files from clipboard
			Clipboard cb = DocFetcher.getInst().getClipboard();
			File[] files = null;
			if (Const.IS_WINDOWS) {
				TransferData[] types = cb.getAvailableTypes();
				boolean enabled = false;
				for (TransferData type : types) {
					if (FileTransfer.getInstance().isSupportedType(type)) {
						enabled = true;
						break;
					}
				}
				Object data = cb.getContents(FileTransfer.getInstance());
				enabled |= data == null;
				enabled |= ! (data instanceof String[]);
				if (! enabled) {
					UtilGUI.showWarningMsg(null, Msg.no_files_in_cb.value());
					return;
				}
				String[] filepaths = (String[]) data;
				files = UtilFile.toFiles(filepaths);
			}
			/*
			 * FIXME On Linux, the FileTransfer doesn't work for some unknown
			 * reason.
			 */
			else if (Const.IS_LINUX) {
				Object data = cb.getContents(TextTransfer.getInstance());
				if (data == null) {
					UtilGUI.showWarningMsg(null, Msg.no_files_in_cb.value());
					return;
				}
				String[] filePaths = ((String) data).trim().split(Const.LS);
				files = UtilFile.toFiles(filePaths);
				for (File file : files)
					if (! file.exists()) {
						UtilGUI.showWarningMsg(null, Msg.no_files_in_cb.value());
						return;
					}	
			}
			
			FSEventHandler.getInst().setWatchEnabled(false, rootScopeToUpdate);
			
			// Copy files
			FileTransferBox transferBox = new FileTransferBox(getShell(), Msg.file_transfer.value());
			transferBox.open();
			transferBox.transferFiles(files, newParent);
			
			FSEventHandler.getInst().setWatchEnabled(Pref.Bool.WatchFS.value, rootScopeToUpdate);
			
			// Update indexes, but silently
			IndexingBox indexingBox = DocFetcher.getInst().getIndexingBox();
			indexingBox.addJob(new Job(rootScopeToUpdate, false, false));
		}
	}
	
}
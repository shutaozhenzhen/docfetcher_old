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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.enumeration.Icon;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.enumeration.Pref.Bool;
import net.sourceforge.docfetcher.enumeration.Pref.Int;
import net.sourceforge.docfetcher.enumeration.Pref.Str;
import net.sourceforge.docfetcher.enumeration.Pref.StrArray;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.util.UtilList;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The preferences shell.
 * 
 * @author Tran Nam Quang
 */
public class PrefPage {
	
	private Shell shell;
	private static int shellX = -1;
	private static int shellY = -1;
	
	/*
	 * Mappings between preference items and GUI widgets. Makes it easier to
	 * save new preference values.
	 */
	private Map<Button, Bool> checkBtMap = new HashMap<Button, Bool> ();
	private Map<Text, Str> textBoxStrMap = new HashMap<Text, Str> ();
	private Map<Text, StrArray> textBoxStrArrayMap = new HashMap<Text, StrArray> ();
	private Map<Text, Int> textBoxIntMap = new HashMap<Text, Int> ();
	private Text exclFilterBox;
	private Text maxResultsBox;
	
	public PrefPage (Shell parentShell) {
		shell = new Shell(parentShell, Const.DIALOG_STYLE);
		shell.setImage(Icon.PREFERENCES.getImage());
		shell.setText(Msg.preferences.value());
		shell.setLayout(new FormLayout());
		shell.setSize(
				Pref.Int.PrefPageWidth.value(),
				Pref.Int.PrefPageHeight.value()
		);
		if (shellX == -1 || shellY == -1)
			UtilGUI.centerShell(parentShell, shell);
		else
			shell.setLocation(shellX, shellY);
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Point pos = shell.getLocation();
				shellX = pos.x;
				shellY = pos.y;
				Point size = shell.getSize();
				Pref.Int.PrefPageWidth.setValue(size.x);
		});
		
		Composite container = new Composite(shell, SWT.NONE);
		container.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		
		createCheckButton(container,
				Msg.pref_manual_on_startup.value(),
				Pref.Bool.ShowWelcomePage
		);
		createCheckButton(container,
				Msg.pref_watch_fs.value(),
				Pref.Bool.WatchFS
		);
		createCheckButton(container,
				Msg.pref_hide_in_systray.value(),
				Pref.Bool.HideOnOpen
		);
		createCheckButton(container,
				Msg.pref_highlight.value(),
				Pref.Bool.HighlightSearchTerms
		);
		createTextBox(container,
				Msg.pref_text_ext.value(),
				Pref.StrArray.TextExtensions
		);
		createTextBox(container,
				Msg.pref_html_ext.value(),
				Pref.StrArray.HTMLExtensions
		);
		exclFilterBox = createTextBox(container,
				Msg.pref_skip_regex.value(),
				Pref.Str.ExclusionFilter
		);
		maxResultsBox = createTextBox(container,
				Msg.pref_max_results.value(),
				Pref.Int.MaxResults
		);
		
		/*
		 * Lower panel with buttons starts here
		 */
		Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		
		Button okButton = new Button(shell, SWT.PUSH);
		Button cancelButton = new Button(shell, SWT.PUSH);
		Button resetButton = new Button(shell, SWT.PUSH);
		Button helpButton = new Button(shell, SWT.PUSH);
		okButton.setText(Msg.ok.value());
		cancelButton.setText(Msg.cancel.value());
		resetButton.setText(Msg.restore_defaults.value());
		helpButton.setText(Msg.help.value());
		
		FormDataFactory fdf = FormDataFactory.getInstance();
		fdf.bottom().right().minWidth(Const.MIN_BT_WIDTH).applyTo(cancelButton);
		fdf.right(cancelButton).applyTo(okButton);
		fdf.reset().bottom().left().minWidth(Const.MIN_BT_WIDTH).applyTo(helpButton);
		fdf.left(helpButton).applyTo(resetButton);
		fdf.reset().bottom(okButton).left().right().minWidth(0).applyTo(separator);
		fdf.top().bottom(separator).applyTo(container);
		
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onOKButton();
			}
		});
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onResetButton();
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		helpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Program.launch(Const.HELP_FILE_PREF);
			}
		});
		
		shell.open();
	}
	
	private Button createCheckButton(Composite parent, String label, Bool boolPref) {
		final Button checkBt = new Button(parent, SWT.CHECK);
		checkBt.setText(label);
		checkBt.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false, 2, 1));
		checkBt.setSelection(boolPref.getValue());
		checkBtMap.put(checkBt, boolPref);
		return checkBt;
	}
	
	private Text createTextBox(Composite parent, String label) {
		Label labelWidget = new Label(parent, SWT.NONE);
		labelWidget.setText(label);
		Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return text;
	}
	
	private Text createTextBox(Composite parent, String label, Str strPref) {
		Text text = createTextBox(parent, label);
		text.setText(strPref.value());
		textBoxStrMap.put(text, strPref);
		return text;
	}
	
	private Text createTextBox(Composite parent, String label, StrArray strArrayPref) {
		Text text = createTextBox(parent, label);
		text.setText(UtilList.toString(" ", strArrayPref.value())); //$NON-NLS-1$
		textBoxStrArrayMap.put(text, strArrayPref);
		return text;
	}
	
	private Text createTextBox(Composite parent, String label, Int intPref) {
		Text text = createTextBox(parent, label);
		text.setText(Integer.toString(intPref.value()));
		textBoxIntMap.put(text, intPref);
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				e.doit = e.text.matches("[0-9]*"); //$NON-NLS-1$
			}
		});
		return text;
	}
	
	private void onOKButton() {
		// Make sure the max results value is not "" or out of range
		try {
			Integer i = Integer.parseInt(maxResultsBox.getText());
			if (i == 0)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			UtilGUI.showWarningMsg(null, Msg.pref_max_results_range.format(Integer.MAX_VALUE));
			maxResultsBox.setText(Integer.toString(Math.max(1, Pref.Int.MaxResults.value())));
			return;
		}
		
		boolean changed = false;
		
		/*
		 * Only modify a preference value if the new value does not equal the
		 * old one, otherwise an unnecessary chain of events would be triggered.
		 */
		for (Entry<Button, Bool> entry : checkBtMap.entrySet()) {
			boolean newValue = entry.getKey().getSelection();
			if (entry.getValue().getValue() != newValue) {
				entry.getValue().setValue(newValue);
				changed = true;
			}
		}
		for (Entry<Text, Int> entry : textBoxIntMap.entrySet()) {
			int newValue = Integer.parseInt(entry.getKey().getText());
			if (entry.getValue().value() != newValue) {
				entry.getValue().setValue(newValue);
				changed = true;
			}
		}
		for (Entry<Text, Str> entry : textBoxStrMap.entrySet()) {
			String newValue = null;
			if (entry.getKey() == exclFilterBox)
				newValue = UtilList.toString(
						" $ ", //$NON-NLS-1$
						UtilGUI.parseExclusionString(exclFilterBox.getText())
				);
			else
				newValue = entry.getKey().getText();
			if (! entry.getValue().value().equals(newValue)) {
				entry.getValue().setValue(newValue);
				changed = true;
			}
		}
		for (Entry<Text, StrArray> entry : textBoxStrArrayMap.entrySet()) {
			String[] newValue = entry.getKey().getText().split("[^\\p{Alnum}]+"); //$NON-NLS-1$
			if (! Arrays.equals(entry.getValue().value(), newValue)) {
				entry.getValue().setValue(newValue);
				changed = true;
			}
		}
		
		if (changed) {
			try {
				Pref.save();
			} catch (IOException e) {
				UtilGUI.showErrorMsg(null, Msg.write_error.value());
			}
		}
		shell.close();
	}
	
	private void onResetButton() {
		for (Entry<Button, Bool> entry : checkBtMap.entrySet())
			entry.getKey().setSelection(entry.getValue().defaultValue);
		for (Entry<Text, Int> entry : textBoxIntMap.entrySet())
			entry.getKey().setText(Integer.toString(entry.getValue().defaultValue));
		for (Entry<Text, Str> entry : textBoxStrMap.entrySet())
			entry.getKey().setText(entry.getValue().defaultValue);
		for (Entry<Text, StrArray> entry : textBoxStrArrayMap.entrySet())
			entry.getKey().setText(UtilList.toString(" ", entry.getValue().defaultValue)); //$NON-NLS-1$
	}

}
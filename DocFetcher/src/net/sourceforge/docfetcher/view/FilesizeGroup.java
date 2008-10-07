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

import net.sourceforge.docfetcher.enumeration.Filesize;
import net.sourceforge.docfetcher.enumeration.Key;
import net.sourceforge.docfetcher.enumeration.Msg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author Tran Nam Quang
 */
public class FilesizeGroup extends GroupWrapper {
	
	private Text minField;
	private Text maxField;
	private Combo minCombo;
	private Combo maxCombo;

	public FilesizeGroup(Composite parent) {
		/*
		 * Creating widgets. They must be instantiated in this order to
		 * allow reasonable widget navigation using the tab key.
		 */
		super(parent);
		minField = new Text(group, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
		minCombo = new Combo(group, SWT.DROP_DOWN);
		maxField = new Text(group, SWT.RIGHT | SWT.SINGLE | SWT.BORDER);
		maxCombo = new Combo(group, SWT.DROP_DOWN);
		
		// Configure widgets
		group.setText(Msg.filesize_group_label.value());
		String[] comboItems = Filesize.valuesAsStrings();
		minCombo.setItems(comboItems);
		maxCombo.setItems(comboItems);
		minCombo.select(Filesize.KB.ordinal());
		maxCombo.select(Filesize.KB.ordinal());
		
		// Layout
		group.setLayout(new FormLayout());
		FormDataFactory fdf = FormDataFactory.getInstance();
		int m = FormDataFactory.DEFAULT_MARGIN;
		fdf.top().bottom().right(50, -m).applyTo(minCombo);
		fdf.right(minCombo, -m/2).left().applyTo(minField);
		fdf.reset().top().bottom().right().applyTo(maxCombo);
		fdf.right(maxCombo, -m/2).left(50, m).applyTo(maxField);
		
		/*
		 * FIXME On GNOME 2.18.1, all combos have a way too large default width,
		 * breaking the layout. Thus we're manually setting the width if its
		 * intial value seems to large.
		 */
		if (minCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT).x > 150) {
			((FormData) minCombo.getLayoutData()).width = 60;
			((FormData) maxCombo.getLayoutData()).width = 60;
		}
		
		// Ensure the user can only enter non-negative integers into the textboxes
		VerifyListener numbersOnlyListener = new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				e.doit = e.text.matches("[0-9]*"); //$NON-NLS-1$
			}
		};
		minField.addVerifyListener(numbersOnlyListener);
		maxField.addVerifyListener(numbersOnlyListener);
		
		// Prevent the user from typing anything into the combos
		KeyListener noTypingListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				e.doit = false;
			}
		};
		minCombo.addKeyListener(noTypingListener);
		maxCombo.addKeyListener(noTypingListener);
		
		new FilesizeGroupNavigator(new Control[] {
				minField, minCombo, maxField, maxCombo
		});
	}
	
	/**
	 * Navigation in the filesize group
	 */
	private class FilesizeGroupNavigator extends KeyAdapter {
		
		private Control[] controls;
		
		FilesizeGroupNavigator(Control[] controls) {
			this.controls = controls;
			for (Control control : controls)
				control.addKeyListener(this);
		}
		
		public void keyPressed(KeyEvent e) {
			Key key = Key.getKey(e.stateMask, e.keyCode);
			if (key == null) return;
			
			// Get index of the control that triggered the event
			int index = -1;
			for (int i = 0; i < controls.length; i++)
				if (controls[i] == e.widget)
					index = i;
			if (index == -1) return;
			
			switch (key) {
			case Left:
				index = Math.max(0, index - 1);
				controls[index].setFocus();
				break;
			case Right:
				index = Math.min(controls.length - 1, index + 1);
				controls[index].setFocus();
				break;
			}
			
			if (e.widget instanceof Text) {
				Text text = (Text) e.widget;
				text.setSelection(0, text.getCharCount());
			}
			else if (e.widget instanceof Combo) {
				Combo combo = (Combo) e.widget;
				int selIndex = combo.getSelectionIndex();
				switch (key) {
				case Up:
					selIndex = Math.max(0, selIndex - 1);
					combo.select(selIndex);
					break;
				case Down:
					selIndex = Math.min(combo.getItemCount() - 1, selIndex + 1);
					combo.select(selIndex);
					break;
				}
			}
		}
		
	}

}

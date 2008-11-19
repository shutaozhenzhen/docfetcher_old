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

package net.sourceforge.docfetcher.dev;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.model.Document;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParseException;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.UtilGUI;
import net.sourceforge.docfetcher.view.FormDataFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Provides for the parser testbox.
 * 
 * @author Tran Nam Quang
 */
public aspect ParserTestboxInjector {
	
	/*
	 * Widgets
	 */
	private Shell testBox;
	private Rectangle testBoxBounds;
	private String content;
	private Text pathField;
	private Button chooseBt;
	private StyledText contentBox;
	private Button originalBt;
	private Text infoField;

	/**
	 * Create testbox
	 */
	after(): execution(* DocFetcher+.createContents(Composite)) {
		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode != SWT.F11) return;
				event.type = SWT.None;
				if (testBox != null && ! testBox.isDisposed()) {
					testBoxBounds = testBox.getBounds();
					testBox.dispose();
					return;
				}
				
				// Create new testbox
				testBox = new Shell(Display.getDefault(), SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.RESIZE);
				if (testBoxBounds != null)
					testBox.setBounds(testBoxBounds);
				else {
					testBox.setSize(500, 500);
					UtilGUI.centerShell(null, testBox);
				}
				testBox.setText("Parser Testbox"); //$NON-NLS-1$
				testBox.setLayout(new FormLayout());

				// Populate testbox
				chooseBt = new Button(testBox, SWT.PUSH);
				chooseBt.setText("Choose File..."); //$NON-NLS-1$
				pathField = new Text(testBox, SWT.BORDER | SWT.SINGLE);
				pathField.setText("Enter a path here and press Enter or click on the button to the right."); //$NON-NLS-1$
				contentBox = new StyledText(testBox, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
				originalBt = new Button(testBox, SWT.CHECK);
				originalBt.setText("Original parser output"); //$NON-NLS-1$
				infoField = new Text(testBox, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
				infoField.setText("Please note:\n* HTML pairing is disabled." + //$NON-NLS-1$
						"\n* The parse time shown here will decrease significantly if you parse the same " + //$NON-NLS-1$
						"document several times over."); //$NON-NLS-1$

				// Layout
				FormDataFactory fdf = FormDataFactory.getInstance();
				fdf.top().right().applyTo(chooseBt);
				fdf.right(chooseBt).left().bottom(contentBox).applyTo(pathField);
				fdf.reset().left().bottom().right(originalBt).top(100, -50).applyTo(infoField);
				fdf.reset().top(chooseBt).bottom(infoField).left().right().applyTo(contentBox);
				fdf.reset().top(contentBox).bottom().right().applyTo(originalBt);

				// Show file dialog when choose button is pressed and parse the selected file
				chooseBt.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						FileDialog dg = new FileDialog(testBox, SWT.OPEN | SWT.PRIMARY_MODAL);
						showResults(dg.open());
					}
				});

				// Switch between original and beautified parser output
				originalBt.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (originalBt.getSelection())
							contentBox.setText(content);
						else
							contentBox.setText(content.replaceAll("\\s+", " ").trim()); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});

				// Parse file denoted by the path entered in the path field when ENTER is pressed
				pathField.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent e) {
						if (UtilGUI.isCRKey(e)) {
							String path = pathField.getText();
							File file = new File(path);
							if (file.exists())
								showResults(file.getAbsolutePath());
							else {
								try {
									file = new File(new URL(path).toURI());
									if (file.exists())
										showResults(file.getAbsolutePath());
								} catch (MalformedURLException e1) {
									contentBox.setText("File does not exist, or path string is unsupported or malformed."); //$NON-NLS-1$
								} catch (URISyntaxException e2) {
									contentBox.setText("File does not exist, or path string is unsupported or malformed.");  //$NON-NLS-1$
								}
							}
						}
					}
				});

				testBox.open();
			}
		});
	}
	
	/**
	 * Tries to parse the file denoted by the given path and show the results in
	 * the testbox.
	 */
	private void showResults(String path) {
		new ResultsThread(path).start();
	}
	
	public class ResultsThread extends Thread {
		
		private String path;
		
		public ResultsThread(String path) {
			this.path = path;
		}
		
		public void run() {
			try {
				if (path == null) return;
				File file = new File(path);
				final Parser parser = ParserRegistry.getParser(file);
				
				// Abort if no parser is available
				if (parser == null) {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							contentBox.setText("Unknown document format."); //$NON-NLS-1$
						}
					});
					return;
				}
				
				// Display some text before parsing
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						pathField.setText(path);
						contentBox.setText("Parsing..."); //$NON-NLS-1$
					}
				});
				
				// Do the parsing
				final long t_start = System.currentTimeMillis();
				Document _doc = parser.parse(file);
				final Document doc = _doc;
				final long t_end = System.currentTimeMillis();
				
				// Display the results
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (doc == null) {
							contentBox.setText("Parser not supported: " + //$NON-NLS-1$
									parser.getClass().getSimpleName());
							return;
						}
						content = doc.getContents();
						if (originalBt.getSelection())
							contentBox.setText(content);
						else
							contentBox.setText(content.replaceAll("\\s+", " ").trim()); //$NON-NLS-1$ //$NON-NLS-2$
						String infos = ""; //$NON-NLS-1$
						infos += "Parsed by " + parser.getClass().getSimpleName(); //$NON-NLS-1$
						infos += " in " + (t_end - t_start) + " ms"; //$NON-NLS-1$ //$NON-NLS-2$
						infos += Const.LS + "Title: \"" + doc.getTitle() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
						infos += Const.LS + "Author: \"" + doc.getAuthor() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
						infoField.setText(infos);
					}
				});
			} catch (final ParseException e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText("Parse Exception: " + e.getMessage()); //$NON-NLS-1$
					}
				});
			} catch (Exception e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText("Something went really bad..."); //$NON-NLS-1$
					}
				});
			} catch (OutOfMemoryError e) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						contentBox.setText("Not enough memory!"); //$NON-NLS-1$
					}
				});
			}
		}
		
	}
	
}

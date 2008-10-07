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

import org.eclipse.swt.widgets.Display;

import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.view.FilesizeGroup;
import net.sourceforge.docfetcher.view.MainPanel;
import net.sourceforge.docfetcher.view.ParserGroup;
import net.sourceforge.docfetcher.view.PreviewPanel;
import net.sourceforge.docfetcher.view.ResultPanel;
import net.sourceforge.docfetcher.view.ScopeGroup;
import net.sourceforge.docfetcher.view.SearchPanel;

/**
 * This aspect injects the singleton property into other parts of the program
 * and provides central access to the singleton instances.
 * <p>
 * Note: This is not necessarily a complete collection of all singletons
 * occuring in the application. This aspect only takes care about "Singletoness"
 * that is required by the aspect overlay.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect Singletons {
	
	/*
	 * Global singletons
	 */
	static DocFetcher docFetcher;
	static MainPanel mainPanel;
	static SearchPanel searchPanel;
	static FilesizeGroup filesizeGroup;
	static ParserGroup parserGroup;
	static ScopeGroup scopeGroup;
	static ResultPanel resultPanel;
	static PreviewPanel previewPanel;
	
	/**
	 * Sets a status message using a Runnable. Convenience method for calling
	 * from outside the UI thread.
	 */
	public static void setStatus(final String msg) {
		if (Display.getCurrent() != null)
			Singletons.docFetcher.setStatus(msg);
		else Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Singletons.docFetcher.setStatus(msg);
			}
		});
	}
	
	DocFetcher around(): call(DocFetcher+.new(..)) {
		if (docFetcher == null)
			docFetcher = proceed();
		return docFetcher;
	}
	
	MainPanel around(): call(MainPanel+.new(..)) {
		if (mainPanel == null)
			mainPanel = proceed();
		return mainPanel;
	}
	
	SearchPanel around(): call(SearchPanel+.new(..)) {
		if (searchPanel == null)
			searchPanel = proceed();
		return searchPanel;
	}
	
	FilesizeGroup around(): call(FilesizeGroup+.new(..)) {
		if (filesizeGroup == null)
			filesizeGroup = proceed();
		return filesizeGroup;
	}
	
	ParserGroup around(): call(ParserGroup+.new(..)) {
		if (parserGroup == null)
			parserGroup = proceed();
		return parserGroup;
	}
	
	PreviewPanel around(): call(PreviewPanel+.new(..)) {
		if (previewPanel == null)
			previewPanel = proceed();
		return previewPanel;
	}
	
	ScopeGroup around(): call(ScopeGroup+.new(..)) {
		if (scopeGroup == null)
			scopeGroup = proceed();
		return scopeGroup;
	}
	
	ResultPanel around(): call(ResultPanel+.new(..)) {
		if (resultPanel == null)
			resultPanel = proceed();
		return resultPanel;
	}

}

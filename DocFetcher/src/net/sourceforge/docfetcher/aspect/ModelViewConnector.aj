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

import net.sourceforge.docfetcher.Event.IObserver;
import net.sourceforge.docfetcher.model.Scope;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.Parser;

import org.eclipse.swt.widgets.Display;

/**
 * Updates views according to changes in the model.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect ModelViewConnector {
	
	private static IObserver resultPanelUpdater = new IObserver() {
		public void update() {
			if (Singletons.resultPanel != null)
				Singletons.resultPanel.refresh();
		}
	};
	
	/**
	 * Update result panel when parsers are checked/unchecked.
	 */
	after() returning(Parser parser): call(Parser+.new(..)) {
		parser.evtCheckStateChanged.add(resultPanelUpdater);
	}
	
	/**
	 * Update result panel when scopes are checked/unchecked.
	 */
	static {
		Scope.checkStateChanged.add(resultPanelUpdater);
	}
	
	/**
	 * Update result panel after removal of a RootScope.
	 */
	after(): execution(* ScopeRegistry.remove(..)) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if (Singletons.resultPanel != null)
					Singletons.resultPanel.refresh();
			}
		});
	}

}

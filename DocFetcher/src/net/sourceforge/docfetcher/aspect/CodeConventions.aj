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
import java.io.PrintStream;

import net.sourceforge.docfetcher.ExceptionHandler;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.ScopeRegistry;
import net.sourceforge.docfetcher.parse.Parser;
import net.sourceforge.docfetcher.parse.ParserRegistry;
import net.sourceforge.docfetcher.util.Timer;
import net.sourceforge.docfetcher.util.UtilFile;
import net.sourceforge.docfetcher.view.IndexingDialog;

/**
 * Some constraints on the code.
 * 
 * @author Tran Nam Quang
 */
public aspect CodeConventions {
	
	declare warning: call(* PrintStream.print*(..)) && !within(Timer):
		"Don't forget to remove System.out.print*() calls after usage."; //$NON-NLS-1$
	
	declare warning: call(* Timer.print*(..)):
		"Don't forget to remove Timer.print*() calls after usage."; //$NON-NLS-1$
	
	declare warning: set(* ExceptionHandler.forceDisabled):
		"Don't forget to re-enable the exception handler after usage."; //$NON-NLS-1$
	
	declare warning: call(* ScopeRegistry.addJob(..)) && !within(IndexingDialog):
		"Don't add jobs via the ScopeRegistry; use the IndexingDialog interface instead."; //$NON-NLS-1$
	
	declare warning: call(Parser+.new(..)) && !within(ParserRegistry):
		"Don't instantiate parsers outside the parser registry."; //$NON-NLS-1$
	
	declare warning: execution(boolean get*(..)) && !within(Pref.Bool):
		"Boolean getter methods should start with 'is'."; //$NON-NLS-1$
	
	declare warning: (call(* File.list(..)) || call(* File.listFiles(..)))
	&& !withincode(* UtilFile.listAll(..))
	&& !withincode(* UtilFile.listFiles(..))
	&& !withincode(* UtilFile.listFolders(..)):
		"Don't use the JDK File.list* methods! Use the list method from the UtilFile class instead!"; //$NON-NLS-1$

}

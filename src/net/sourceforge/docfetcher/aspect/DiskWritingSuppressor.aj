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

package net.sourceforge.docfetcher.aspect;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Msg;
import net.sourceforge.docfetcher.enumeration.Pref;
import net.sourceforge.docfetcher.model.Serializer;

/**
 * This aspect checks if the paths DocFetcher wants to write to are writable. If
 * not, it disables some of the disk writing operations in order to avoid
 * annoying pop-up messages, which typically occur if the user has put a
 * portable version of DocFetcher on a CD-ROM.
 * 
 * @author Tran Nam Quang
 */
public privileged aspect DiskWritingSuppressor {
	
	private boolean writable = true;
	
	after(): set(Boolean Const.isPortable) {
		if (Const.isPortable && ! Const.PROGRAM_FOLDER.canWrite())
			writable = false;
	}
	
	after(): execution(* DocFetcher.createContents(..)) {
		if (! writable)
			DocFetcher.getInst().setStatus(Msg.write_warning.value());
	}
	
	void around(): execution(* Pref.save()) {
		if (writable) proceed();
	}
	
	void around(): execution(* Serializer.save(..)) {
		if (writable) proceed();
	}

}
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

package net.sourceforge.docfetcher.enumeration;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.docfetcher.Const;

/**
 * An enumeration of localized message strings.
 * 
 * @author Tran Nam Quang
 */
public enum Msg {
	
	// Generic
	confirm_operation,
	invalid_operation,
	system_error,
	report_bug,
	read_error,
	write_error,
	write_warning,
	yes,
	no,
	ok,
	cancel,
	already_running,
	close,
	
	// Filter panel
	filesize_group_label,
	filetype_group_label,
	invert_selection,
	filetype_abi,
	filetype_chm,
	filetype_doc,
	filetype_ppt,
	filetype_xls,
	filetype_vsd,
	filetype_html,
	filetype_odt,
	filetype_ods,
	filetype_odg,
	filetype_odp,
	filetype_pdf,
	filetype_rtf,
	filetype_txt,
	
	// Search scope panel
	search_scope,
	create_index,
	contains_file,
	invalid_dnd_source,
	update_index,
	rebuild_index,
	remove_index,
	remove_sel_indexes,
	check_toplevel_only,
	uncheck_toplevel_only,
	check_all,
	uncheck_all,
	open_folder,
	list_docs,
	folders_not_found_title,
	folders_not_found,
	create_subfolder,
	enter_folder_name,
	enter_folder_name_new,
	create_subfolder_failed,
	rename_folder,
	rename_requires_full_rebuild,
	enter_new_foldername,
	cant_rename_folder,
	untitled,
	delete_folder,
	delete_folder_q,
	paste_into_folder,
	open_target_folder,
	file_already_exists,
	file_already_exists_dot,
	folder_already_exists,
	folder_not_found,
	file_transfer,
	no_files_in_cb,
	moving_files,
	copying,
	deleting,
	
	// Main panel
	prev_page,
	next_page,
	show_filterpanel,
	show_preview,
	preferences,
	occurrence_count,
	prev_occurrence,
	next_occurrence,
	open_manual,
	use_embedded_html_viewer,
	change_preview_pos,
	browser_stop,
	browser_refresh,
	browser_launch_external,
	loading,
	cant_read_file,
	preview_limit_hint,
	systray_not_available,
	restore_app,
	exit,
	jobs,
	open_file_error,
	
	// Search
	enter_nonempty_string,
	invalid_query,
	invalid_query_syntax,
	wildcard_first_char,
	search_scope_empty,
	minsize_not_greater_maxsize,
	filesize_out_of_range,
	no_filetypes_selected,
	
	// Status bar
	press_help_button,
	num_results,
	num_results_detail,
	page_m_n,
	num_sel_results,
	num_documents_added,
	
	// Preferences
	pref_manual_on_startup,
	pref_watch_fs,
	pref_hide_in_systray,
	pref_highlight,
	pref_highlight_color,
	pref_text_ext,
	pref_html_ext,
	pref_skip_regex,
	pref_max_results,
	pref_max_results_range,
	keybox_title,
	keybox_msg,
	pref_hotkey,
	restore_defaults,
	help,
	
	// Indexing config dialog
	scope_folder_title,
	scope_folder_msg,
	index_management,
	target_folder,
	ipref_text_ext,
	ipref_html_ext,
	select_exts,
	ipref_skip_regex,
	ipref_detect_html_pairs,
	run,
	regex_matches_file_yes,
	regex_matches_file_no,
	target_folder_deleted,
	not_a_regex,
	add_to_queue,
	inters_indexes,
	inters_queue,
	choose_regex_testfile_title,
	discard_incomplete_index,
	
	// Indexing feedback
	progress,
	html_pairing,
	waiting_in_queue,
	file_skipped,
	finished,
	finished_with_errors,
	total_elapsed_time,
	errors,
	error_type,
	out_of_jvm_memory,
	file_not_found,
	file_not_readable,
	file_corrupted,
	unsupported_encoding,
	doc_pw_protected,
	parser_error,
	
	// Result panel
	open,
	open_parent,
	open_limit,
	copy,
	delete_file,
	confirm_delete_file,
	empty_folders,
	empty_folders_msg,
	property_title,
	property_score,
	property_size,
	property_name,
	property_type,
	property_path,
	property_author,
	property_lastModified,
	
	;
	
	/**
	 * The message string the enumeration item corresponds to.
	 */
	private String value = "(Error: Resources not loaded properly.)"; //$NON-NLS-1$
	
	/**
	 * Returns the localized message string represented by this enumeration.
	 * Returns an error message if the load process during startup has failed.
	 */
	public String value() {
		return value;
	}
	
	/**
	 * Returns a string created from a <tt>java.text.MessageFormat</tt>
	 * with the given argument(s).
	 */
	public String format(Object... obj) {
		return MessageFormat.format(value, obj);
	}
	
	/**
	 * The internal ResourceBundle to load the messages from.
	 */
	private static ResourceBundle bundle = ResourceBundle.getBundle(Const.RESOURCE_BUNDLE);
	
	/*
	 * Loads the localized messages from disk.
	 */
	static {
		try {
			for (Msg msg : Msg.values()) {
				msg.value = bundle.getString(msg.name());
			}
		} catch (MissingResourceException e) {
			e.printStackTrace();
		}
	}

}

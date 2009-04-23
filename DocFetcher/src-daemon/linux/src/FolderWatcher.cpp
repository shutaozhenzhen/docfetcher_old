/*******************************************************************************
 * Copyright (c) 2009 Tonio Rush.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tonio Rush - initial API and implementation
 *******************************************************************************/

#include <string>
#include <map>
#include <iostream>
#include <fstream>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/ioctl.h>
#include <linux/inotify.h>
#include <errno.h>
#include <stdio.h>

#include <unistd.h>

#include <sys/stat.h>
#include <stdlib.h>

#include "FolderWatcher.h"

#include "inotify-syscalls.h"

#include "Logger.h"


/**
 * constructor
 *
 */
FolderWatcher::FolderWatcher():CHAR_MODIFIED('#') {
}

/**
 * destructor
 *
 */
FolderWatcher::~FolderWatcher() {
}


bool FolderWatcher::findIndexesFile() {
	// Portable version -> the directory ./indexes exists
	struct stat st;

	char * wd = getcwd(NULL, 0);

	std::string indexes_directory = wd;

	free(wd);

	indexes_directory += "/indexes";


	if(stat(indexes_directory.c_str(), &st) == 0) {
		_indexes_file_path = indexes_directory + "/.indexes.txt";
		log("Portable version : working with file %s", _indexes_file_path.c_str());
		return true;
	}else{
		// Normal version -> indexes.txt is in HOME/.docfetcher/
		log("Directory %s does not exist -> installed version", indexes_directory.c_str());

		_indexes_file_path = getenv("HOME");
		_indexes_file_path += "/.docfetcher/.indexes.txt";
		log("Normal version : working with file %s", _indexes_file_path.c_str());
    	return true;
	}
}


/**
 * Initialization, called at the beginning
 *
 * initializes the map of indexed folders and adds the watches
 *
 */
bool FolderWatcher::startWatch() {

	// read file
	std::string line;
	std::ifstream in (_indexes_file_path.c_str());

	if(!in){
		log("Cannot open index file (%s)", _indexes_file_path.c_str());
		return false;
	}

	_fd = inotify_init();


	std::string file_name;

	const long notifyFilter =  	//  IN_ACCESS			//File was accessed (read) (*)
								//|IN_ATTRIB			//Metadata changed (permissions, timestamps, extended attributes, etc.) (*)
								 IN_CLOSE_WRITE		//File opened for writing was closed (*)
								//|IN_CLOSE_NOWRITE	//File not opened for writing was closed (*)
								|IN_CREATE			//File/directory created in watched directory (*)
								|IN_DELETE			//File/directory deleted from watched directory (*)
								|IN_DELETE_SELF		//Watched file/directory was itself deleted
								|IN_MODIFY			//File was modified (*)
								|IN_MOVE_SELF		//Watched file/directory was itself moved
								|IN_MOVED_FROM		//File moved out of watched directory (*)
								|IN_MOVED_TO			//File moved into watched directory (*)
								//|IN_OPEN				//File was opened (*)
								;



	WatchedFolder aWatchedFolder;
	aWatchedFolder._modified = false;

 	while(std::getline(in,line)){
		if(line.empty()){
			continue;
		}else if(line.at(0) == CHAR_MODIFIED){
			log("folder already signaled modified : %s", line.c_str());
			continue;
		}else if(line.size()>=2 && line.substr(0,2) == "//"){
			// a comment line, ignore
			continue;
		}else{

			file_name = line;

			int watchId = inotify_add_watch(_fd, file_name.c_str(), notifyFilter);

			if(watchId == 0 ) {
				log("error add_watch for dir=%s", line.c_str());
			}else{
				log("Watch installed for directory %s", line.c_str());
				aWatchedFolder._path = line;
				_indexed_folders.insert(std::make_pair(watchId, aWatchedFolder));
			}

		}

	}

	return (_indexed_folders.size() > 0);
}

/**
 * Initialization, called at the beginning
 *
 * initializes the map of indexed folders and adds the watches
 *
 */
bool FolderWatcher::stopWatch() {
	close(_fd);
	_fd = -1;
	return true;
}




/**
 * Watches' callback
 *
 * Removes the watch and updates the indexes file
 *
 */
void FolderWatcher::callback(int watchID, int action) {
	log("callback : watchID=%d,action=%d", watchID, action);

	if(_indexed_folders.find(watchID) == _indexed_folders.end()) {
		log("id unknown ???");
		return;
	}

	inotify_rm_watch(_fd, watchID);

	if(_indexed_folders[watchID]._modified == true){
		log("already done...");
		return;

	}

	_indexed_folders[watchID]._modified = true;

	if(!updateIndexesFile()){
		log("updateIndexesFile failed");
		return;
	}

}



/**
 * Writes the indexes file
 *
 * If all folders are modified, we exit the daemon
 *
 */
bool FolderWatcher::updateIndexesFile() {

	bool bAllFoldersModified = true;

	log("Writing into %s", _indexes_file_path.c_str());

	std::ofstream out(_indexes_file_path.c_str());
	out << "//Updated by daemon" << std::endl;

	folders_container_type::const_iterator itFolder;
	for(itFolder = _indexed_folders.begin() ; itFolder != _indexed_folders.end() ; ++itFolder) {
		if(itFolder->second._modified) {
			out << CHAR_MODIFIED;
		} else {
			bAllFoldersModified = false;
		}
		out << itFolder->second._path << std::endl;
	}

	return true;
}


void FolderWatcher::run()
{
	static int BUF_LEN = 4096;
    char buf[BUF_LEN];
    int len, i = 0;

	while (_fd != -1)
	{
	    len = read (_fd, buf, BUF_LEN);

	    while (i < len)
	    {
	        struct inotify_event *event = (struct inotify_event *) &buf[i];
	       	callback(event->wd, event->mask);

	        i += sizeof (struct inotify_event) + event->len;
	    }
	    i=0;
	}
}


std::string FolderWatcher::getLockFile() {
	return _indexes_file_path + ".lock";
}


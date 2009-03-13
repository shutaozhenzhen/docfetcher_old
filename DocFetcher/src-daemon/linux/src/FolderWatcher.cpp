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

#include "FolderWatcher.h"

#include "inotify-syscalls.h"

FolderWatcher *_this = NULL;

#include "Logger.h"

/**
 * inotify fd.
 */
int fd = -1;



/**
 * constructor
 *
 */
FolderWatcher::FolderWatcher():CHAR_MODIFIED('#') {
	_this = this;
}

/**
 * destructor
 *
 */
FolderWatcher::~FolderWatcher() {
	_this = NULL;
}

/**
 * Initialization, called at the beginning
 *
 * initializes the map of indexed folders and adds the watches
 *
 */
bool FolderWatcher::initialize() {

	// find indexes.txt file
	if(!getIndexesFile()) {
		log("Cannot get indexes file.");
		return false;
	}

	// read file

	std::string line;
	std::ifstream in (_indexes_file_path.c_str());

	if(!in){
		log("Cannot open index file (%s)", _indexes_file_path.c_str());
		return false;
	}

	std::string file_name;
	int error;
	const long notifyFilter = 0;
	const bool watchSubdirs = true;

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

			int watchId = inotify_add_watch(fd, file_name.c_str(), notifyFilter);

			if(watchId == 0 ) {
				log("error add_watch for dir=%s err=%d",line.c_str(),error);
			}else{
				log("Watch installed for directory %s",line.c_str());
				aWatchedFolder._path = line;
				_indexed_folders.insert(std::make_pair(watchId, aWatchedFolder));
			}

		}

	}

	return (_indexed_folders.size() > 0);

}

/**
 * Watches' callback
 *
 * Removes the watch and updates the indexes file
 *
 */
void FolderWatcher::callback(int watchID, int action, const char* rootPath, const char* filePath) {
	log("callback : watchID=%d,action=%d,rootPath=%ls,filePath=%ls",watchID,action,rootPath,filePath);

	if(_this == NULL) {
		log("_this == NULL ???");
		return;
	}

	if(_this->_indexed_folders.find(watchID) == _this->_indexed_folders.end()) {
		log("id unknown ???");
		return;
	}

	// impossible to remove here,
	inotify_rm_watch(fd, watchID);

	if(_this->_indexed_folders[watchID]._modified == true){
		log("already done...");
		return;

	}


	_this->_indexed_folders[watchID]._modified = true;

	if(!_this->updateIndexesFile()){
		log("updateIndexesFile failed");
		return;
	}

}

bool FolderWatcher::getIndexesFile() {



	return false;
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

	if(bAllFoldersModified) {
		log("asking to quit");

//		::PostMessage(_hwndMain,WM_DESTROY,0,0);
	}

	return true;
}


int runLoop()
{
	if (fd == -1)
	{
		return 1;
	}

	static int BUF_LEN = 4096;
    char buf[BUF_LEN];
    int len, i = 0;

	while (fd != -1)
	{
	    len = read (fd, buf, BUF_LEN);

	    while (i < len)
	    {
	        struct inotify_event *event = (struct inotify_event *) &buf[i];
	       	_this->callback(event->wd, event->mask, event->name, event->name);

	        i += sizeof (struct inotify_event) + event->len;
	    }
	    i=0;
	}


	return 0;
}

void dispatch(struct inotify_event *event)
{
}

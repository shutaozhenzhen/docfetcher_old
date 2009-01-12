//============================================================================
// Name        : FolderWatcher.cpp
// Author      : Tonio Rush
// Version     :
// Copyright   :
// Description : FolderWatcher
//============================================================================


#include "jnotify_win32/Win32FSHook.h"
#include "jnotify_win32/Logger.h"

#include <shlobj.h>
#include <fstream>

#include "FolderWatcher.h"

/**
 * RaiiLocker
 * Pattern : Ressource Acquisition Is Initialization
 * locks and unlocks the given lock
 * Stores a REFERENCE to the lock
 *
 */
class RaiiLocker {
public:
	RaiiLocker(Lock &lock):_lock(lock) {
		_lock.lock();
	}
	~RaiiLocker(){
		_lock.unlock();
	}
private:
	Lock &_lock;
};


extern Win32FSHook *_win32FSHook;

FolderWatcher *_this = NULL;

extern HWND _hwndMain;


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
 * Initializtion, called at the beginning
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

	WCHAR  file_name [MB_CUR_MAX];
	DWORD error;
	const long notifyFilter = FILE_NOTIFY_CHANGE_LAST_WRITE;
	const bool watchSubdirs = true;

	WatchedFolder aWatchedFolder;
	aWatchedFolder._modified = false;

	RaiiLocker aLock(_lock);

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

			int count_chars = mbstowcs( NULL, line.c_str(), 0);
			mbstowcs(file_name, line.c_str(), count_chars);

			// null terminated string
			file_name[count_chars] = 0;

			int watchId = _win32FSHook->add_watch((const WCHAR *)file_name,notifyFilter,watchSubdirs,error,&callback);
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
void FolderWatcher::callback(int watchID, int action, const WCHAR* rootPath, const WCHAR* filePath) {
	RaiiLocker aLock(_this->_lock);
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
	::PostMessageW(_hwndMain, WM_REMOVE_WATCH, 0, watchID);

//	_win32FSHook->remove_watch(watchID);

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


	TCHAR szPath[MAX_PATH];

	if(SUCCEEDED(SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, 0, szPath)))
	{
		_indexes_file_path = szPath;
		_indexes_file_path += "\\DocFetcher\\indexes.txt";
		return true;
	}

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

	log("Writing into %s",_indexes_file_path.c_str());

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

		::PostMessage(_hwndMain,WM_DESTROY,0,0);
	}

	return true;
}


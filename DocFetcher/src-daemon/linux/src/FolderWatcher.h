#ifndef FOLDERWATCHER_H_
#define FOLDERWATCHER_H_

struct WatchedFolder {
	std::string _path;
	bool _modified;
};

class FolderWatcher {
public:
	typedef std::map<int,WatchedFolder> folders_container_type;

	FolderWatcher();
	virtual ~FolderWatcher();

	bool initialize();

	void run();

private:
	void callback(int watchID, int action);

	std::string _indexes_file_path;
	const char CHAR_MODIFIED;


	bool getIndexesFile();
	bool updateIndexesFile();

	folders_container_type _indexed_folders;

};

#endif /*FOLDERWATCHER_H_*/

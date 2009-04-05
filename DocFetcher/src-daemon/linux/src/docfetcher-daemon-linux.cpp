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

#include <sys/types.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/ioctl.h>
#include <linux/inotify.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

#include <string>
#include <map>

#include <pthread.h>

#include "FolderWatcher.h"
#include "Logger.h"
void dispatch(struct inotify_event *);
int runLoop();

extern bool dbg;

FolderWatcher _folderWatcher;


void *start(void *threadid);

int main(){
	dbg = true;
	if(!_folderWatcher.findIndexesFile()) {
		log("findIndexesFile failed");
	}

	std::string lock_file = _folderWatcher.getLockFile();
	log("lock file : %s", lock_file.c_str());
	bool watching = false;
	for(;;){
		remove(lock_file.c_str());
		int error = errno;
		log("errno = %d", error);

		if(error == EACCES){
			// the file is used by DocFetcher, so stop watching
			if(watching) {
				log("stopWatch");
				_folderWatcher.stopWatch();
				watching = false;
			}
		}else{
			// the file is not used by DocFetcher, so start watching
			if(!watching) {
				log("startWatch");
				_folderWatcher.startWatch();
				watching = true;

				pthread_t thread;
				int err = pthread_create(&thread, NULL, start, NULL);
			}
		}

		// Check is done every 2 seconds
		sleep(2);
	}


	log("exit");

}

void *start(void *threadid) {
	_folderWatcher.run();
	return NULL;
}



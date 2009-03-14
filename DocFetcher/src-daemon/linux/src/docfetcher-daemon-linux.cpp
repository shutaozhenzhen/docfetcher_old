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

#include "FolderWatcher.h"
#include "Logger.h"
void dispatch(struct inotify_event *);
int runLoop();

extern bool dbg;

int main(){
	dbg = true;
	FolderWatcher watcher;
	if(!watcher.initialize()) {
		log("initialize failed");
	}

	watcher.run();

	log("exit");

}



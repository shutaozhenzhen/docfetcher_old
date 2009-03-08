//============================================================================
// Name        : docfetcher-daemon-win.cpp
// Author      : Tonio Rush
// Version     :
// Copyright   :
// Description : Entry point for docfetcher-daemon-win
//============================================================================

#include <iostream>
#include <sstream>

#include "windows.h"
#include "TCHAR.h"

#include <psapi.h>

#include "jnotify_win32/Logger.h"
#include "jnotify_win32/Win32FSHook.h"

#include "FolderWatcher.h"


/**
 * used by JNotify
 *
 */
extern bool dbg;

/**
 * Globals
 *
 */

HINSTANCE _hInstance;
HWND _hwndMain;
Win32FSHook *_win32FSHook;

/**
 * This module functions
 *
 */
LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
bool InitInstance();
bool isDocFectcherRunning();
BOOL CALLBACK FindDocFetcherWindow( HWND hwnd, LPARAM lParam ) ;
bool isDocFetcherProcess(DWORD pid);

/**
 * Entry point
 *
 * installs the watches
 *
 */
int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
		LPTSTR lpCmdLine, int nCmdShow) {
	UNREFERENCED_PARAMETER(lpCmdLine);

	log("docfetcher-daemon-win starting...");

	_hInstance = hInstance;

	// If the main window cannot be created, terminate the application.
	if(!InitInstance()){
		log("InitInstance failed");
		return 1;
	}

	// Check if this is the only instance
	// It is done by creating a mutex with the daemon exe's full path
	// to distinguish various installations of Docfetcher
	TCHAR mutex_name [MAX_PATH] = {0};
	::GetModuleFileName(NULL, mutex_name, MAX_PATH);

	// replace \ by _
	for(TCHAR* ch = mutex_name; *ch != _T('\0'); ++ch)
		if(*ch == _T('\\')) *ch = _T('_');

	if(::OpenMutex(MUTEX_ALL_ACCESS, FALSE, mutex_name) != NULL) {
		log("a daemon is already running");
		return 1;
	}

	if(::CreateMutex(NULL, TRUE, mutex_name) == NULL) {
		log("cannot create unicity mutex");
		return 1;
	}

	if(isDocFectcherRunning()) {
		log("DocFectcher is running !");
		return 1;
	}

	// JNotify_win32 traces
	dbg = false;
	_win32FSHook = new Win32FSHook();
	_win32FSHook->init(NULL);


	FolderWatcher watcher;

	if (!watcher.initialize()) {
		log("Initialization failed. Exit.");
		return 1;
	}

	MSG msg;

	// Main message loop:
	while (GetMessage(&msg, NULL, 0, 0)) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	log("docfetcher-daemon-win exiting...");

	return 0;
}

/**
 * Messages received by the window
 * 	WM_DESTROY
 * 	WM_REMOVE_WATCH
 *
 */
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
	switch (message) {
	case WM_DESTROY:
		PostQuitMessage(0);
		break;
	case WM_REMOVE_WATCH:
		_win32FSHook->remove_watch(lParam);
		log("watch %d removed", lParam);
		return 0;
	default:
		return DefWindowProc(hWnd, message, wParam, lParam);
	}
	return 0;
}

/**
 * Creates main window
 *
 * Necessary to be able to receive messages
 *
 */
bool InitInstance() {

	HICON hDocFetcherIcon = ::ExtractIcon(_hInstance, "DocFetcher.ico", 0);

	if (hDocFetcherIcon == NULL) {
		log("icon not found...");
		hDocFetcherIcon = LoadIcon((HINSTANCE) NULL, IDI_APPLICATION);
	}


	WNDCLASS wc;

	const char *CLASS_DOCFETCHER_WND = "DocFectcherDaemonWnd";

	wc.style = 0;
	wc.lpfnWndProc = (WNDPROC) WndProc;
	wc.cbClsExtra = 0;
 	wc.cbWndExtra = 0;
	wc.hInstance = _hInstance;
	wc.hIcon = hDocFetcherIcon;
	wc.hCursor = LoadCursor((HINSTANCE) NULL, IDC_ARROW);
	wc.hbrBackground = NULL;
	wc.lpszMenuName = "MainMenu";
	wc.lpszClassName = CLASS_DOCFETCHER_WND;

	if (!RegisterClass(&wc))
		return false;

	// Create the main window.
	_hwndMain = CreateWindow(CLASS_DOCFETCHER_WND, "Sample",
			WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT,
			CW_USEDEFAULT, CW_USEDEFAULT, (HWND) NULL,
			(HMENU) NULL, _hInstance, (LPVOID) NULL);

	if (!_hwndMain) {
		return false;
	}

	//   ShowWindow(hWnd, nCmdShow);
	//   UpdateWindow(hWnd);

	return true;
}

/**
 * Searchs all windows, and finds Docfetcher's
 *
 *
 */
bool isDocFectcherRunning() {
    // Get the list of process identifiers.

    DWORD aProcesses[10024], cbNeeded, cProcesses;
    unsigned int i;

    log("before enum");
    if ( !::EnumProcesses( aProcesses, sizeof(aProcesses), &cbNeeded ) )
        return false;

    log("after enum");
    // Calculate how many process identifiers were returned.

    cProcesses = cbNeeded / sizeof(DWORD);

    // Print the name and process identifier for each process.

    for ( i = 0; i < cProcesses; i++ ){
        if( aProcesses[i] != 0 ){
        	if(isDocFetcherProcess( aProcesses[i] )){
        		return true;
        	}

        }
    }
    return false;


//	bool docfethcer_running_is = false;
//
//    ::EnumWindows((WNDENUMPROC)FindDocFetcherWindow, (LPARAM) &docfethcer_running_is) ;
//	return docfethcer_running_is;
}

bool isDocFetcherProcess(DWORD pid) {
    // Get a handle to the process.

    HANDLE hProcess = OpenProcess( PROCESS_QUERY_INFORMATION |
                                   PROCESS_VM_READ,
                                   FALSE, pid );

    TCHAR path[MAX_PATH+1] = {0};
	::GetModuleFileNameEx(hProcess, NULL, path, MAX_PATH);
	log(path);
	return false;
}



/**
 * Callback passed to EnumWindows
 *
 * lParam contains a pointer to a boolean, to be set if Docfetcher window is found
 *
 */
BOOL CALLBACK FindDocFetcherWindow( HWND hwnd, LPARAM lParam ){
	TCHAR title [MAX_PATH];

	::GetWindowText(hwnd, title, MAX_PATH);

	if(_tcscmp(title, "DocFetcher") == 0) {
		bool *docfethcer_running_is = (bool*)lParam;
		*docfethcer_running_is = true;
		return FALSE;
	}else{
		// continue searching
		return TRUE ;
	}
}


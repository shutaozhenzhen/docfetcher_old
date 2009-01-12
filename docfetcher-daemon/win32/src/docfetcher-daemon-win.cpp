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

#include "jnotify_win32/Logger.h"
#include "jnotify_win32/Win32FSHook.h"

#include "FolderWatcher.h"


extern bool dbg;

HINSTANCE _hInstance;
HWND _hwndMain;
Win32FSHook *_win32FSHook;

LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
bool InitInstance();

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

//
//  FUNCTION: WndProc(HWND, UINT, WPARAM, LPARAM)
//
//  PURPOSE:  Processes messages for the main window.
//
//  WM_DESTROY	- post a quit message and return
//
//
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

//
//   FUNCTION: InitInstance(HINSTANCE, int)
//
//   PURPOSE: creates main window
//
//
bool InitInstance() {

	HICON hDocFetcherIcon = ::ExtractIcon(_hInstance, "DocFetcher.ico", 0);

	if (hDocFetcherIcon == NULL) {
		log("icon not found...");
		hDocFetcherIcon = LoadIcon((HINSTANCE) NULL, IDI_APPLICATION);
	}


	WNDCLASS wc;

	const char *className = "DocFectcherDaemonWnd";

	wc.style = 0;
	wc.lpfnWndProc = (WNDPROC) WndProc;
	wc.cbClsExtra = 0;
	wc.cbWndExtra = 0;
	wc.hInstance = _hInstance;
	wc.hIcon = hDocFetcherIcon;
	wc.hCursor = LoadCursor((HINSTANCE) NULL, IDC_ARROW);
	wc.hbrBackground = NULL;
	wc.lpszMenuName = "MainMenu";
	wc.lpszClassName = className;

	if (!RegisterClass(&wc))
		return false;

	// Create the main window.
	_hwndMain = CreateWindow(className, "Sample",
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


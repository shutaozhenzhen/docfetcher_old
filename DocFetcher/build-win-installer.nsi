; This script should be run _after_ the build script, because
; the latter fixes EOL issues in the text files. 

!define VERSION 0.9.5.1

SetCompress force
SetCompressor /SOLID lzma
Name "DocFetcher ${VERSION}"
XPStyle on
OutFile build\docfetcher_${VERSION}_win32_setup.exe
InstallDir $PROGRAMFILES\DocFetcher
Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles
AutoCloseWindow true

Function .onInit
	ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher" "UninstallString"
	StrCmp $R0 "" done
	MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
	"DocFetcher is already installed. $\n$\nClick 'OK' to remove the \
	previous version or 'Cancel' to cancel this upgrade." \
	IDOK uninst
	Abort
	
	uninst:
		ClearErrors
		ExecWait '$R0 /S _?=$INSTDIR'
		IfErrors no_remove_uninstaller
			Delete $INSTDIR\uninstaller.exe
			RMDIR $INSTDIR
		no_remove_uninstaller:
	done:
FunctionEnd

Section "DocFetcher"
	; Copy files
    SetOutPath $INSTDIR
    File resources\DocFetcher.exe
    File resources\DocFetcher.bat
    File build\temp_${VERSION}\ChangeLog.txt
    File build\temp_${VERSION}\Readme.txt
	
	SetOutPath $INSTDIR\licenses
	File /r /x .svn resources\licenses\*.*
    
    SetOutPath $INSTDIR\help
    File /r /x DocFetcher_Manual.html /x .svn resources\help\*.*
    File build\temp_${VERSION}\DocFetcher_Manual.html
    
    SetOutPath $INSTDIR\icons
    File /r /x .svn resources\icons\*.*
    
    SetOutPath $INSTDIR\lib
    File /x *.so /x swt-*-linux-gtk.jar /x .svn lib\*.*
    File build\net.sourceforge.docfetcher_*.jar
    
    ; Uninstaller
    WriteUninstaller $INSTDIR\uninstaller.exe
    
    ; Start menu entries
    CreateDirectory $SMPROGRAMS\DocFetcher
    CreateShortCut $SMPROGRAMS\DocFetcher\DocFetcher.lnk $INSTDIR\DocFetcher.exe
    CreateShortCut "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk" $INSTDIR\uninstaller.exe
    CreateShortCut $SMPROGRAMS\DocFetcher\Manual.lnk $INSTDIR\help\DocFetcher_Manual.html
    CreateShortCut $SMPROGRAMS\DocFetcher\Readme.lnk $INSTDIR\Readme.txt
    CreateShortCut $SMPROGRAMS\DocFetcher\ChangeLog.lnk $INSTDIR\ChangeLog.txt
    
    ; Write to registry
    Var /GLOBAL regkey
    Var /GLOBAL homepage
    StrCpy $regkey "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
    StrCpy $homepage "http://docfetcher.sourceforge.net"
    WriteRegStr HKLM $regkey "DisplayName" "DocFetcher"
    WriteRegStr HKLM $regkey "UninstallString" "$INSTDIR\uninstaller.exe"
    WriteRegStr HKLM $regkey "InstallLocation" $INSTDIR
    WriteRegStr HKLM $regkey "DisplayIcon" "$INSTDIR\DocFetcher.exe,0"
    WriteRegStr HKLM $regkey "HelpLink" $homepage
    WriteRegStr HKLM $regkey "URLUpdateInfo" $homepage
    WriteRegStr HKLM $regkey "URLInfoAbout" $homepage
    WriteRegStr HKLM $regkey "DisplayVersion" "${VERSION}"
    WriteRegDWORD HKLM $regkey "NoModify" 1
    WriteRegDWORD HKLM $regkey "NoRepair" 1
SectionEnd

Section "un.Uninstall"
	; Remove program folder
	Delete $INSTDIR\DocFetcher.exe
	Delete $INSTDIR\uninstaller.exe
	Delete $INSTDIR\DocFetcher.bat
	Delete $INSTDIR\user.properties
	Delete $INSTDIR\ChangeLog.txt
	Delete $INSTDIR\Readme.txt
    Delete $INSTDIR\hs_err_pid*.log
    
	Delete $INSTDIR\help\DocFetcher_Manual.html
	RMDir /r $INSTDIR\help\DocFetcher_Manual_files
    RMDir $INSTDIR\help
    
	Delete $INSTDIR\icons\*.gif
	Delete $INSTDIR\icons\*.png
    RMDir $INSTDIR\icons
    
	Delete $INSTDIR\lib\*.jar
	Delete $INSTDIR\lib\*.dll
    RMDir $INSTDIR\lib
    
    RMDir /r $INSTDIR\indexes
	RMDir /r $INSTDIR\licenses
	RMDIR $INSTDIR
    
    ; Remove application data folder
    RMDir /r $APPDATA\DocFetcher
	
	; Remove start menu entries
	Delete $SMPROGRAMS\DocFetcher\DocFetcher.lnk
	Delete "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk"
	Delete $SMPROGRAMS\DocFetcher\Manual.lnk
	Delete $SMPROGRAMS\DocFetcher\Readme.lnk
	Delete $SMPROGRAMS\DocFetcher\ChangeLog.lnk
	RMDir $SMPROGRAMS\DocFetcher
    
    ; Remove registry key
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
SectionEnd

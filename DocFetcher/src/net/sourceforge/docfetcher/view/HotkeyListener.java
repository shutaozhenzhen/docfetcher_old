package net.sourceforge.docfetcher.view;

import jxgrabkey.JXGrabKey;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.enumeration.Key;

import com.melloware.jintellitype.JIntellitype;

/**
 * Listener for hot keys
 * 
 * @author Tonio Rush
 * 
 */
public class HotkeyListener {
	protected static final int HOTKEY_TO_FRONT_IDX = 1;

	/**
	 * Platform specific stuffs
	 */
	HotkeyListenerImpl implementation;

	/**
	 * All implementations have this methods
	 *
	 */
	interface HotkeyListenerImpl {
		public void initialize(HotkeyListener listener);
		public void registerSwingHotkey(int id, int mask, int key);
		public void shutdown();
	};


	/**
	 * 
	 */
	public HotkeyListener() {
		
		if(Const.IS_WINDOWS){
			implementation = new HotkeyListenerWindowsImpl();
		}else if(Const.IS_LINUX){
			// TODO: test on linux
			//implementation = new HotkeyListenerLinuxImpl();
			return;
		}else{
			// Mac ?
			return;
		}
			
		
		implementation.initialize(this);

		implementation.registerSwingHotkey(HOTKEY_TO_FRONT_IDX,
				Key.HotKeyToFront.stateMask, Key.HotKeyToFront.keyCode);

		/**
		 * uninstall hotkeys when DocFetcher shuts down 
		 */
		DocFetcher.getInst().getShell().addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent arg0) {
				implementation.shutdown();
			}
		});
		
	}
	
	/*
	 * When a hotkey is pressed, we are called from a dll
	 * To make actions on the UI, we must call the display to enter in
	 * the UI thread
	 */
	protected void onHotKey(final int hotkey_idx) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				switch (hotkey_idx) {
				case HOTKEY_TO_FRONT_IDX:
					if (DocFetcher.getInst().isInSystemTray()) {
						DocFetcher.getInst().restoreFromSystemTray();
					} else {
						Shell shell = DocFetcher.getInst().getShell();
						shell.setVisible(true);
						shell.forceActive();
					}

					break;
				default:
					// future hotkeys
				}
			}
		});
	}

	/**
	 * Windows implementation with JIntellitype
	 * 
	 * @author Tonio Rush
	 *
	 */
	class HotkeyListenerWindowsImpl implements HotkeyListenerImpl {

		public void initialize(final HotkeyListener listener) {
			JIntellitype.getInstance();

			JIntellitype.getInstance().addHotKeyListener(new com.melloware.jintellitype.HotkeyListener() {
				public void onHotKey(int hotkey_idx) {
					listener.onHotKey(hotkey_idx);
				}
			});
		}

		public void registerSwingHotkey(int id, int mask, int key) {
			JIntellitype.getInstance().registerSwingHotKey(id, mask, key);


		}

		public void shutdown() {
			JIntellitype.getInstance().cleanUp();
			
		}
		
	}
	/**
	 * Linux implementation with JXGrabKey
	 * 
	 * @author Tonio Rush
	 *
	 */
	class HotkeyListenerLinuxImpl implements HotkeyListenerImpl {

		public void initialize(final HotkeyListener listener) {
            System.loadLibrary("libJXGrabKey");
			
			JXGrabKey.getInstance().addHotkeyListener(new jxgrabkey.HotkeyListener(){
				public void onHotkey(int hotkey_idx) {
					listener.onHotKey(hotkey_idx);
				}
	        });
			
		}

		public void registerSwingHotkey(int id, int mask, int key) {
	        JXGrabKey.getInstance().registerSwingHotkey(id, mask, key);
		}

		public void shutdown() {
	        JXGrabKey.getInstance().cleanUp();
		}
		
	}

}

package net.sourceforge.docfetcher.view;

import jxgrabkey.JXGrabKey;
import net.sourceforge.docfetcher.Const;
import net.sourceforge.docfetcher.DocFetcher;
import net.sourceforge.docfetcher.Event.Listener;
import net.sourceforge.docfetcher.enumeration.Pref;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

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
		public void unregisterHotkey(int id);
		public void shutdown();
	};


	/*
	 * Installs a listener on hotkey to bring the app to top
	 * The hotkey is registred in Pref.IntArray.HotKeyToFront
	 * 
	 */
	public HotkeyListener() {
		if(Const.IS_WINDOWS){
			implementation = new HotkeyListenerWindowsImpl();
		}else if(Const.IS_LINUX){
			// TODO: test on linux
			implementation = new HotkeyListenerLinuxImpl();
		}else{
			// Mac ?
			return;
		}
			
		
		implementation.initialize(this);
		
		Pref.IntArray.HotKeyToFront.evtChanged.add(new Listener<int[]>() {
			public void update(int[] eventData) {
				implementation.unregisterHotkey(HOTKEY_TO_FRONT_IDX);
				
				implementation.registerSwingHotkey(HOTKEY_TO_FRONT_IDX,
						eventData[0], eventData[1]);
				
			}
		});

		implementation.registerSwingHotkey(HOTKEY_TO_FRONT_IDX,
				Pref.IntArray.HotKeyToFront.value()[0], Pref.IntArray.HotKeyToFront.value()[1]);

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

		public void unregisterHotkey(int id) {
			JIntellitype.getInstance().unregisterHotKey(id);
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
			System.loadLibrary("JXGrabKey");
			
			JXGrabKey.setDebugOutput(true);

			JXGrabKey.getInstance().addHotkeyListener(new jxgrabkey.HotkeyListener(){
				public void onHotkey(int hotkey_idx) {
					listener.onHotKey(hotkey_idx);
				}
	        });
			
		}

		public void registerSwingHotkey(int id, int mask, int key) {
	        JXGrabKey.getInstance().registerSwingHotkey(id, mask, key);
		}
		public void unregisterHotkey(int id) {
			JXGrabKey.getInstance().unregisterHotKey(id);
		}

		public void shutdown() {
	        JXGrabKey.getInstance().cleanUp();
		}
		
	}

}

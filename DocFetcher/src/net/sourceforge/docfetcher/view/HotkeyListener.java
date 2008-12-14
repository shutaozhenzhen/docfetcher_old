package net.sourceforge.docfetcher.view;

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
	protected static final int HOTKEY_TO_FRONT_IDX = 0;

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
		}else{
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

		@Override
		public void initialize(final HotkeyListener listener) {
			JIntellitype.getInstance();

			JIntellitype.getInstance().addHotKeyListener(new com.melloware.jintellitype.HotkeyListener() {
				public void onHotKey(int hotkey_idx) {
					listener.onHotKey(hotkey_idx);
				}
			});
		}

		@Override
		public void registerSwingHotkey(int id, int mask, int key) {
			JIntellitype.getInstance().registerHotKey(id, mask, key);


		}

		@Override
		public void shutdown() {
			JIntellitype.getInstance().cleanUp();
			
		}
		
	}

	
	
}

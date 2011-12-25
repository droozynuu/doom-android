// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package com.xianle.doomtnt;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	private String[] mFileList;
	private String[] mFileNoSuffixList;
	private File mPath = new File("/sdcard/doom");
	private String mChosenFile;
	private static final String FTYPE = ".wad";
	private static final int DIALOG_LOAD_FILE = 1000;
	private static final int DIALOG_NO_FILE = 1001;
	static String TAG = "doom";

	private void loadFileList() {
		// try {
		// mPath.mkdirs();
		// } catch (SecurityException e) {
		// Log.e(TAG, "unable to write on the sd card " + e.toString());
		// }
		if (mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					// File sel = new File(dir, filename);
					return (filename.contains(FTYPE)
							|| filename.contains(".WAD")
							|| filename.contains(".bak")) && !filename.equalsIgnoreCase("prboom.wad");
				}
			};
			mFileList = mPath.list(filter);
			mFileNoSuffixList = new String[mFileList.length];
			String temp;
			for(int i = 0; i < mFileList.length; i++) {
				temp = mFileList[i];
				if(temp.endsWith(".bak"))
					temp = temp.substring(0, temp.length()-4);
				this.mFileNoSuffixList[i] = temp;
			}
		} else {
			mFileList = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Globals.wadFile = this.getIntent().getStringExtra("wad");
		// Log.v("Doom", "wad file"+Globals.wadFile);
		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(
				android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		_tv = new TextView(this);
		_tv.setText("Initializing");

		_videoLayout = new FrameLayout(this);
		_videoLayout.addView(_tv);

		setContentView(_videoLayout);
		// AdView adView = (AdView)findViewById(R.id.ad);
		// adView.requestFreshAd();
		if (mAudioThread == null) // Starting from background (should not
									// happen)
		{
			mLoadLibraryStub = new LoadLibrary();
			mAudioThread = new AudioThread(this);
			Settings.Load(this);
			startDownloader();
		}
	}

	public void startDownloader() {
		if (downloader == null)
			downloader = new DataDownloader(this, _tv);
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		switch (id) {
		case DIALOG_NO_FILE:
			builder.setTitle("Choose your file");
			if (mFileList == null) {
				Log.e(TAG, "Showing file picker before loading the file list");
				dialog = builder.create();
				return dialog;
			}
			builder.setMessage("can't find any Data file");
			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							MainActivity.this.finish();
						}
					});
			break;
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose one");
			if (mFileList == null) {
				Log.e(TAG, "Showing file picker before loading the file list");
				dialog = builder.create();
				return dialog;
			}
			builder.setItems(mFileNoSuffixList, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mChosenFile = mFileList[which];
					String temp;
					for(int i = 0 ; i< mFileList.length; i++) {
						temp = mFileList[i];
						if(i == which) 
							continue;
						if(temp.endsWith(".wad") ||temp.endsWith(".WAD")) {
							wad2bakFile(temp);
						}
						//if(mChosenFile)
					}
					if (mChosenFile.endsWith(".bak")) {
						Globals.wadFile = mChosenFile.substring(0,
								mChosenFile.length() - 4);
						bak2wadFile(mChosenFile);

					} else {
						Globals.wadFile = mChosenFile;
					}
					Log.v(TAG, "wad ddd file is:" + Globals.wadFile);
					startGame();
					// you can do stuff with the file here too
				}
			});
			break;
		}
		dialog = builder.show();
		return dialog;
	}

	private void startGame() {
		mGLView = new DemoGLSurfaceView(this);

		_videoLayout = new FrameLayout(this);
		// setContentView(_videoLayout);
		// mGLView = new DemoGLSurfaceView(this);
		_videoLayout.addView(mGLView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		// setContentView(mGLView);
		// Receive keyboard events

		setContentView(_videoLayout, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mGLView.setFocusableInTouchMode(true);
		mGLView.setFocusable(true);
		mGLView.requestFocus();
	}
	//change file wad to wad.bak
	private void wad2bakFile(String filename) {
		File f = new File(Globals.outFilesDir + File.separator
				+ filename);
		if(f.exists()) {
			f.renameTo( new File(Globals.outFilesDir + File.separator
				+ filename+".bak"));
		}
	}
	
	private void bak2wadFile(String filename) {
		File f = new File(Globals.outFilesDir + File.separator
				+ filename);
		if(f.exists()) {
			f.renameTo( new File(Globals.outFilesDir + File.separator
				+ filename.substring(0, filename.length()-4)));
		}
	}
	public void initSDL() {
		if (sdlInited)
			return;

		sdlInited = true;
		// ////////bellow is new added code for let user select a fill 2011.11.3
		this.loadFileList();
		if (mFileList == null) {
			this.showDialog(DIALOG_NO_FILE);
			return;
		}
		if (mFileList.length == 1) {
			// just 1 wad file. start it
			if (mFileList[0].endsWith(".bak")) {
				Globals.wadFile = mFileList[0].substring(0,
						mFileList[0].length() - 4);
				bak2wadFile(mFileList[0]);

			} else {
				Globals.wadFile = mFileList[0];
			}
			Log.v(TAG, "wad file is:" + Globals.wadFile);
			startGame();
			return;
		} else {
			this.showDialog(DIALOG_LOAD_FILE);
			return;
		}
		// ///////end added 2011.11.3
		// startGame();
	}

	@Override
	protected void onPause() {
		if (downloader != null) {
			synchronized (downloader) {
				downloader.setParent(null, null);
			}
		}

		super.onPause();
		if (mGLView != null)
			mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null)
			mGLView.onResume();
		else if (downloader != null) {
			synchronized (downloader) {
				downloader.setParent(this, _tv);
				if (downloader.DownloadComplete)
					initSDL();
			}
		}
	}

	@Override
	protected void onStop() {
		if (downloader != null) {
			synchronized (downloader) {
				downloader.setParent(null, null);
			}
		}
		if (mGLView != null)
			mGLView.exitApp();
		super.onStop();
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {

		if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				&& !Globals.keyBindingUseVolumeButton()) {
			return super.onKeyDown(keyCode, event);
		}
		// Overrides Back key to use in our app

		if (_screenKeyboard != null)
			_screenKeyboard.onKeyDown(keyCode, event);
		else if (mGLView != null)
			mGLView.nativeKey(keyCode, 1);
		else if (keyCode == KeyEvent.KEYCODE_BACK && downloader != null) {
			if (downloader.DownloadFailed)
				System.exit(1);
			if (!downloader.DownloadComplete)
				onStop();
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {

		if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				&& !Globals.keyBindingUseVolumeButton()) {
			return super.onKeyDown(keyCode, event);
		}

		if (_screenKeyboard != null)
			_screenKeyboard.onKeyUp(keyCode, event);
		else if (mGLView != null)
			mGLView.nativeKey(keyCode, 0);
		return true;
	}

	@Override
	public boolean dispatchTouchEvent(final MotionEvent ev) {

		if (_screenKeyboard != null)
			_screenKeyboard.dispatchTouchEvent(ev);
		else if (mGLView != null)
			mGLView.onTouchEvent(ev);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Do nothing here
	}

	public void setText(final String t) {
		class Callback implements Runnable {
			public TextView Status;
			public String text;

			public void run() {
				if (Status != null)
					Status.setText(text);
			}
		}
		Callback cb = new Callback();
		cb.text = new String(t);
		cb.Status = _tv;
		this.runOnUiThread(cb);
	}

	public void hideScreenKeyboard() {
		if (_screenKeyboard == null)
			return;

		synchronized (textInput) {
			String text = _screenKeyboard.getText().toString();
			for (int i = 0; i < text.length(); i++) {
				// Log.v("doom", "mainActivity input char :"+text.charAt(i) );
				DemoRenderer.nativeTextInput((int) text.charAt(i),
						(int) text.codePointAt(i));
			}
		}
		_videoLayout.removeView(_screenKeyboard);
		_screenKeyboard = null;
		mGLView.setFocusableInTouchMode(true);
		mGLView.setFocusable(true);
		mGLView.requestFocus();
	};

	public void showScreenKeyboard() {
		if (_screenKeyboard != null)
			return;
		class myKeyListener implements OnKeyListener {
			MainActivity _parent;

			myKeyListener(MainActivity parent) {
				_parent = parent;
			};

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_UP)
						&& ((keyCode == KeyEvent.KEYCODE_ENTER) || (keyCode == KeyEvent.KEYCODE_BACK))) {
					_parent.hideScreenKeyboard();
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						synchronized (textInput) {
							DemoRenderer.nativeTextInput(13, 13); // send return
						}
					}
					return true;
				}
				if ((event.getAction() == KeyEvent.ACTION_UP)
						&& (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_CLEAR)) {
					synchronized (textInput) {
						DemoRenderer.nativeTextInput(8, 8);
						// textInput.addLast(8); // send backspace keycode
						// textInput.addLast(8);
					}
					return false; // and proceed to delete text in keyboard
					// input field
				}
				return false;
			}
		}
		;
		_screenKeyboard = new EditText(this);
		_screenKeyboard.setOnKeyListener(new myKeyListener(this));
		_videoLayout.addView(_screenKeyboard, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		_screenKeyboard.setFocusableInTouchMode(true);
		_screenKeyboard.setFocusable(true);
		_screenKeyboard.requestFocus();
	}

	private static DemoGLSurfaceView mGLView = null;
	private static LoadLibrary mLoadLibraryStub = null;
	private static AudioThread mAudioThread = null;
	private static DataDownloader downloader = null;
	private TextView _tv = null;
	private boolean sdlInited = false;
	private FrameLayout _videoLayout = null;
	private EditText _screenKeyboard = null;
	public LinkedList<Integer> textInput = new LinkedList<Integer>();

}
/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nexes.manager;

import java.io.File;
import java.util.Calendar;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;


public final class Main extends ListActivity {
	public static final String ACTION_WIDGET = "com.nexes.manager.Main.ACTION_WIDGET";
	
	private static final String PREFS_NAME = "ManagerPrefsFile";	//user preference file name
	private static final String PREFS_HIDDEN = "hidden";
	private static final String PREFS_COLOR = "color";
	private static final String PREFS_THUMBNAIL = "thumbnail";
	private static final String PREFS_SORT = "sort";

	private static final int D_MENU_RENAME = 0x06;			//context menu id
	private static final int F_MENU_RENAME = 0x0b;			//context menu id
	private static final int F_MENU_ATTACH = 0x0c;			//context menu id

	private FileManager mFileMag;
	private EventHandler mHandler;
	private EventHandler.TableRow mTable;
	
	private SharedPreferences mSettings;
	private boolean mReturnIntent = false;
	private boolean mUseBackKey = true;
	private String mSelectedListItem;				//item from context menu
	private TextView  mPathLabel;

	static Main _inst;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        /*read settings*/
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = mSettings.getBoolean(PREFS_HIDDEN, true);
        boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);
        int color = mSettings.getInt(PREFS_COLOR, -1);
        int sort = mSettings.getInt(PREFS_SORT, 2);
        
        mFileMag = new FileManager();
        mFileMag.setShowHiddenFiles(hide);
        mFileMag.setSortType(sort);
        
        if (savedInstanceState != null)
        	mHandler = new EventHandler(Main.this, mFileMag, savedInstanceState.getString("location"));
        else
        	mHandler = new EventHandler(Main.this, mFileMag);
        
        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        
        /* register context menu for our list view */
        registerForContextMenu(getListView());
        
        mPathLabel = (TextView)findViewById(R.id.path_label);
        mPathLabel.setText(Environment.getExternalStorageDirectory().getPath());
        
        mHandler.setUpdateLabels(mPathLabel);
        
        /* setup buttons */
        int[] img_button_id = {R.id.back_button, R.id.home_button};

        int[] button_id = {R.id.memory_button};
        
        ImageButton[] bimg = new ImageButton[img_button_id.length];
        Button[] bt = new Button[button_id.length];
        
        for(int i = 0; i < img_button_id.length; i++) {
			bimg[i] = (ImageButton) findViewById(img_button_id[i]);
			bimg[i].setOnClickListener(mHandler);
		}

		for(int j = 0; j < button_id.length; j++) {
        	bt[j] = (Button)findViewById(button_id[j]);
        	bt[j].setOnClickListener(mHandler);
        }
    
        Intent intent = getIntent();
        
        if(intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
			mReturnIntent = true;
        
        } else if (intent.getAction().equals(ACTION_WIDGET)) {
			Log.e("MAIN", "Widget action, string = " + intent.getExtras().getString("folder"));
        	mHandler.updateDirectory(mFileMag.getNextDir(intent.getExtras().getString("folder"), true));

        }

		_inst = this;
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("location", mFileMag.getCurrentDir());
	}
	
	/*(non Java-Doc)
	 * Returns the file that was selected to the intent that
	 * called this activity. usually from the caller is another application.
	 */
	private void returnIntentResults(File data) {
		mReturnIntent = false;
		
		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		
		finish();
	}

	public void updateView () {

		GridView gridView = (GridView)findViewById(R.id.grid);
		ListView listView = (ListView)findViewById(android.R.id.list);

		if (mFileMag.getCurrentDir().equalsIgnoreCase(Environment.getExternalStorageDirectory().getPath()) || mFileMag.getCurrentDir().equalsIgnoreCase("/sdcard")) {
			gridView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.INVISIBLE);
		} else {
			gridView.setVisibility(View.INVISIBLE);
			listView.setVisibility(View.VISIBLE);
		}

	}

	/**
	 *  To add more functionality and let the user interact with more
	 *  file types, this is the function to add the ability. 
	 *  
	 *  (note): this method can be done more efficiently 
	 */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
    	final String item = mHandler.getData(position);
    	File file = new File(mFileMag.getCurrentDir() + "/" + item);
    	String item_ext = null;
    	
    	try {
    		item_ext = item.substring(item.lastIndexOf("."), item.length());
    		
    	} catch(IndexOutOfBoundsException e) {	
    		item_ext = ""; 
    	}
    	
		if (file.isDirectory()) {
			if(file.canRead()) {
				mHandler.stopThumbnailThread();
				mHandler.updateDirectory(mFileMag.getNextDir(item, false));
				updateView();
				mPathLabel.setText(mFileMag.getCurrentDir());

				/*set back button switch to true
				 * (this will be better implemented later)
				 */
				if(!mUseBackKey)
					mUseBackKey = true;

			} else {
				Toast.makeText(this, "Can't read folder due to permissions",
								Toast.LENGTH_SHORT).show();
			}
		}

		/*music file selected--add more audio formats*/
		else if (item_ext.equalsIgnoreCase(".mp3") ||
				 item_ext.equalsIgnoreCase(".m4a")||
				 item_ext.equalsIgnoreCase(".mp4")) {

			if(mReturnIntent) {
				returnIntentResults(file);
			} else {
				Intent i = new Intent();
				i.setAction(android.content.Intent.ACTION_VIEW);
				i.setDataAndType(Uri.fromFile(file), "audio/*");
				startActivity(i);
			}
		}

		/*photo file selected*/
		else if(item_ext.equalsIgnoreCase(".jpeg") ||
				item_ext.equalsIgnoreCase(".jpg")  ||
				item_ext.equalsIgnoreCase(".png")  ||
				item_ext.equalsIgnoreCase(".gif")  ||
				item_ext.equalsIgnoreCase(".tiff")) {

			if (file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent picIntent = new Intent();
					picIntent.setAction(android.content.Intent.ACTION_VIEW);
					picIntent.setDataAndType(Uri.fromFile(file), "image/*");
					startActivity(picIntent);
				}
			}
		}

		/*video file selected--add more video formats*/
		else if(item_ext.equalsIgnoreCase(".m4v") ||
				item_ext.equalsIgnoreCase(".3gp") ||
				item_ext.equalsIgnoreCase(".wmv") ||
				item_ext.equalsIgnoreCase(".mp4") ||
				item_ext.equalsIgnoreCase(".ogg") ||
				item_ext.equalsIgnoreCase(".wav")) {

			if (file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent movieIntent = new Intent();
					movieIntent.setAction(android.content.Intent.ACTION_VIEW);
					movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
					startActivity(movieIntent);
				}
			}
		}

		/* gzip files, this will be implemented later */
		else if(item_ext.equalsIgnoreCase(".gzip") ||
				item_ext.equalsIgnoreCase(".gz")) {

			if(mReturnIntent) {
				returnIntentResults(file);

			} else {
				//TODO:
			}
		}

		/*pdf file selected*/
		else if(item_ext.equalsIgnoreCase(".pdf")) {

			if(file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent pdfIntent = new Intent();
					pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
					pdfIntent.setDataAndType(Uri.fromFile(file),
											 "application/pdf");

					try {
						startActivity(pdfIntent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(this, "Sorry, couldn't find a pdf viewer",
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		/*Android application file*/
		else if(item_ext.equalsIgnoreCase(".apk")){

			if(file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent apkIntent = new Intent();
					apkIntent.setAction(android.content.Intent.ACTION_VIEW);
					apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
					startActivity(apkIntent);
				}
			}
		}

		/* HTML file */
		else if(item_ext.equalsIgnoreCase(".html")) {

			if(file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent htmlIntent = new Intent();
					htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
					htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");

					try {
						startActivity(htmlIntent);
					} catch(ActivityNotFoundException e) {
						Toast.makeText(this, "Sorry, couldn't find a HTML viewer",
											Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		/* text file*/
		else if(item_ext.equalsIgnoreCase(".txt")) {

			if(file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent txtIntent = new Intent();
					txtIntent.setAction(android.content.Intent.ACTION_VIEW);
					txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");

					try {
						startActivity(txtIntent);
					} catch(ActivityNotFoundException e) {
						txtIntent.setType("text/*");
						startActivity(txtIntent);
					}
				}
			}
		}

		/* generic intent */
		else {
			if(file.exists()) {
				if(mReturnIntent) {
					returnIntentResults(file);

				} else {
					Intent generic = new Intent();
					generic.setAction(android.content.Intent.ACTION_VIEW);
					generic.setDataAndType(Uri.fromFile(file), "text/plain");

					try {
						startActivity(generic);
					} catch(ActivityNotFoundException e) {
						Toast.makeText(this, "Sorry, couldn't find anything " +
									   "to open " + file.getName(),
									   Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	super.onCreateContextMenu(menu, v, info);
    	
    	AdapterContextMenuInfo _info = (AdapterContextMenuInfo)info;
    	mSelectedListItem = mHandler.getData(_info.position);

    	/* is it a directory and is multi-select turned off */
    	if(mFileMag.isDirectory(mSelectedListItem)) {
    		menu.setHeaderTitle("Folder operations");
        	menu.add(0, D_MENU_RENAME, 0, "Rename Folder");

        /* is it a file and is multi-select turned off */
    	} else if(!mFileMag.isDirectory(mSelectedListItem)) {
        	menu.setHeaderTitle("File Operations");
    		menu.add(0, F_MENU_RENAME, 0, "Rename File");
    		menu.add(0, F_MENU_ATTACH, 0, "Email File");
    	}	
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {

    	switch(item.getItemId()) {

			case D_MENU_RENAME:
			case F_MENU_RENAME:
				AlertDialog.Builder alert;
				alert = new AlertDialog.Builder(this);

				alert.setTitle("Rename " + mSelectedListItem);
				final EditText input = new EditText(this);
				input.setText(mSelectedListItem);
				input.selectAll();
				alert.setView(input);

				alert.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {

						if (mFileMag.renameTarget(mFileMag.getCurrentDir() + "/" + mSelectedListItem, input.getText().toString()) == 0) {
							Toast.makeText(Main.this, mSelectedListItem + " was renamed to " + input.getText().toString(),
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(Main.this, mSelectedListItem + " was not renamed", Toast.LENGTH_LONG).show();
						}

						String temp = mFileMag.getCurrentDir();
						mHandler.updateDirectory(mFileMag.getNextDir(temp, true));
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				alert.show();

				return true;

    		case F_MENU_ATTACH:
				File file = new File(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    			Intent mail_int = new Intent();
    			
    			mail_int.setAction(android.content.Intent.ACTION_SEND);
    			mail_int.setType("application/mail");
    			mail_int.putExtra(Intent.EXTRA_BCC, "");
    			mail_int.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
    			startActivity(mail_int);
    			return true;

    	}
    	return false;
    }
    
    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
   public boolean onKeyDown(int keycode, KeyEvent event) {
    	String current = mFileMag.getCurrentDir();
    	
    	if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && !current.equals("/")) {
			//stop updating thumbnail icons if its running
			mHandler.stopThumbnailThread();
			mHandler.updateDirectory(mFileMag.getPreviousDir());
			updateView();
			mPathLabel.setText(mFileMag.getCurrentDir());
    		return true;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && current.equals("/")) {
    		Toast.makeText(Main.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();
    		
    		mUseBackKey = false;
    		mPathLabel.setText(mFileMag.getCurrentDir());
    		
    		return false;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey && current.equals("/")) {
    		finish();
    		
    		return false;
    	}
    	return false;
    }
}

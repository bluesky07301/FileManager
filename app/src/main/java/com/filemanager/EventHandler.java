
package com.filemanager;

import java.io.File;
import java.util.ArrayList;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class EventHandler implements OnClickListener {
	/*
	 * Unique types to control which file operation gets
	 * performed in the background
	 */
    private final Context mContext;
	private final FileManager mFileMang;
	private ThumbnailCreator mThumbnail;
	private TableRow mDelegate;
	private GridRow mGridDelegate;

	//private boolean multi_select_flag = false;
	private boolean thumbnail_flag = true;
	private int mColor = Color.WHITE;
	
	//the list used to feed info into the array adapter and when multi-select is on
	private ArrayList<String> mDataSource;
	private TextView mPathLabel;

	
	/**
	 * Creates an EventHandler object. This object is used to communicate
	 * most work from the Main activity to the FileManager class.
	 * 
	 * @param context	The context of the main activity e.g  Main
	 * @param manager	The FileManager object that was instantiated from Main
	 */
	public EventHandler(Context context, final FileManager manager) {
		mContext = context;
		mFileMang = manager;
		
		mDataSource = new ArrayList<String>(mFileMang.setHomeDir
				(Environment.getExternalStorageDirectory().getPath()));
	}
	
	/**
	 * This constructor is called if the user has changed the screen orientation
	 * and does not want the directory to be reset to home. 
	 * 
	 * @param context	The context of the main activity e.g  Main
	 * @param manager	The FileManager object that was instantiated from Main
	 * @param location	The first directory to display to the user
	 */
	public EventHandler(Context context, final FileManager manager, String location) {
		mContext = context;
		mFileMang = manager;
		
		mDataSource = new ArrayList<String>(mFileMang.getNextDir(location, true));
	}

	/**
	 * This method is called from the Main activity and this has the same
	 * reference to the same object so when changes are made here or there
	 * they will display in the same way.
	 * 
	 * @param adapter	The TableRow object
	 */
	public void setListAdapter(TableRow adapter) {
		mDelegate = adapter;
	}

    public void setGridListAdapter(GridRow adapter) {
        mGridDelegate = adapter;
    }

	/**
	 * This method is called from the Main activity and is passed
	 * the TextView that should be updated as the directory changes
	 * so the user knows which folder they are in.
	 * 
	 * @param path	The label to update as the directory changes
	 */
	public void setUpdateLabels(TextView path) {
		mPathLabel = path;
	}
	
	/**
	 * 
	 * @param color
	 */
	public void setTextColor(int color) {
		mColor = color;
	}
	
	/**
	 * Set this true and thumbnails will be used as the icon for image files. False will
	 * show a default image. 
	 * 
	 * @param show
	 */
	public void setShowThumbnails(boolean show) {
		thumbnail_flag = show;
	}
	
	/**
	 * this will stop our background thread that creates thumbnail icons
	 * if the thread is running. this should be stopped when ever 
	 * we leave the folder the image files are in.
	 */
	public void stopThumbnailThread() {
		if (mThumbnail != null) {
			mThumbnail.setCancelThumbnails(true);
			mThumbnail = null;
		}
	}

	/**
	 *  This method, handles the button presses of the top buttons found
	 *  in the Main activity. 
	 */
	@Override
	public void onClick(View v) {
		
		switch(v.getId()) {
		
			case R.id.back_button:			
				if (mFileMang.getCurrentDir() != "/") {
					stopThumbnailThread();
					updateDirectory(mFileMang.getPreviousDir());
					Main._inst.updateView();
                    if(mPathLabel != null)
                        mPathLabel.setText(mFileMang.getCurrentDir());

				}
				break;
			
			case R.id.home_button:
				stopThumbnailThread();
				updateDirectory(mFileMang.setHomeDir(Environment.getExternalStorageDirectory().getPath()));
				Main._inst.updateView();
                if(mPathLabel != null)
                    mPathLabel.setText(mFileMang.getCurrentDir());

				break;
				
			case R.id.memory_button:
				Intent memory = new Intent(mContext, MemoryManager.class);
				mContext.startActivity(memory);
				break;

			case R.id.help_button:
				Intent help = new Intent(mContext, HelpManager.class);
				mContext.startActivity(help);
				break;
		}
	}
	
	/**
	 * will return the data in the ArrayList that holds the dir contents. 
	 * 
	 * @param position	the indext of the arraylist holding the dir content
	 * @return the data in the arraylist at position (position)
	 */
	public String getData(int position) {
		
		if(position > mDataSource.size() - 1 || position < 0)
			return null;
		
		return mDataSource.get(position);
	}

	/**
	 * called to update the file contents as the user navigates there
	 * phones file system. 
	 * 
	 * @param content	an ArrayList of the file/folders in the current directory.
	 */
	public void updateDirectory(ArrayList<String> content) {

		if(!mDataSource.isEmpty())
			mDataSource.clear();
		
		for(String data : content)
			mDataSource.add(data);
		
		mDelegate.notifyDataSetChanged();
        mGridDelegate.notifyDataSetChanged();
	}

	/**
	 * This private method is used to display options the user can select when
	 * the tool box button is pressed. The WIFI option is commented out as it doesn't
	 * seem to fit with the overall idea of the application. However to display it, just 
	 * uncomment the below code and the code in the AndroidManifest.xml file.
	 */

	private static class ViewHolder {
		TextView topView;
		TextView bottomView;
		ImageView icon;
	}

    private static class GridViewHolder {
        TextView name;
        ImageView icon;
    }



    /**
	 * A nested class to handle displaying a custom view in the ListView that
	 * is used in the Main activity. If any icons are to be added, they must
	 * be implemented in the getView method. This class is instantiated once in Main
	 * and has no reason to be instantiated again. 
	 * 
	 */
    public class TableRow extends ArrayAdapter<String> {
    	private final int KB = 1024;
    	private final int MG = KB * KB;
    	private final int GB = MG * KB;    	
    	private String display_size;

    	public TableRow() {
    		super(mContext, R.layout.tablerow, mDataSource);    		
    	}

    	public String getFilePermissions(File file) {
    		String per = "-";
    	    		
    		if(file.isDirectory())
    			per += "d";
    		if(file.canRead())
    			per += "r";
    		if(file.canWrite())
    			per += "w";
    		
    		return per;
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder mViewHolder;
    		int num_items = 0;
    		String temp = mFileMang.getCurrentDir();
    		File file = new File(temp + "/" + mDataSource.get(position));
    		String[] list = file.list();
    		
    		if(list != null)
    			num_items = list.length;
   
    		if(convertView == null) {
    			LayoutInflater inflater = (LayoutInflater) mContext.
    						getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			convertView = inflater.inflate(R.layout.tablerow, parent, false);
    			
    			mViewHolder = new ViewHolder();
    			mViewHolder.topView = (TextView)convertView.findViewById(R.id.top_view);
    			mViewHolder.bottomView = (TextView)convertView.findViewById(R.id.bottom_view);
    			mViewHolder.icon = (ImageView)convertView.findViewById(R.id.row_image);

    			convertView.setTag(mViewHolder);
    			
    		} else {
    			mViewHolder = (ViewHolder)convertView.getTag();
    		}   			
    		  		
    		mViewHolder.topView.setTextColor(mColor);
    		mViewHolder.bottomView.setTextColor(mColor);
    		
    		if(mThumbnail == null)
    			mThumbnail = new ThumbnailCreator(52, 52);
    		
    		if(file != null && file.isFile()) {
    			String ext = file.toString();
    			String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);
    			
    			/* This series of else if statements will determine which 
    			 * icon is displayed 
    			 */
    			if (sub_ext.equalsIgnoreCase("pdf")) {
    				mViewHolder.icon.setImageResource(R.drawable.pdf);
    			
    			} else if (sub_ext.equalsIgnoreCase("mp3") || 
    					   sub_ext.equalsIgnoreCase("wma") || 
    					   sub_ext.equalsIgnoreCase("m4a") || 
    					   sub_ext.equalsIgnoreCase("m4p")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.music);
    			
    			} else if (sub_ext.equalsIgnoreCase("png") ||
    					   sub_ext.equalsIgnoreCase("jpg") ||
    					   sub_ext.equalsIgnoreCase("jpeg")|| 
    					   sub_ext.equalsIgnoreCase("gif") ||
    					   sub_ext.equalsIgnoreCase("tiff")) {
    				
    				if(thumbnail_flag && file.length() != 0) {
    					Bitmap thumb = mThumbnail.isBitmapCached(file.getPath());

    					if (thumb == null) {
    						final Handler handle = new Handler(new Handler.Callback() {
    							public boolean handleMessage(Message msg) {
    								notifyDataSetChanged();
    								
    								return true;
    							}
    						});
    										
    						mThumbnail.createNewThumbnail(mDataSource, mFileMang.getCurrentDir(), handle);
    						
    						if (!mThumbnail.isAlive()) 
    							mThumbnail.start();
    						
    					} else {
    						mViewHolder.icon.setImageBitmap(thumb);
    					}
	    				
    				} else {
    					mViewHolder.icon.setImageResource(R.drawable.image);
    				}
    				
    			} else if (sub_ext.equalsIgnoreCase("zip")  || 
    					   sub_ext.equalsIgnoreCase("gzip") ||
    					   sub_ext.equalsIgnoreCase("gz")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.zip);
    			
    			} else if(sub_ext.equalsIgnoreCase("m4v") ||
    					  sub_ext.equalsIgnoreCase("wmv") ||
    					  sub_ext.equalsIgnoreCase("3gp") || 
    					  sub_ext.equalsIgnoreCase("mp4")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.movies);
    			
    			} else if(sub_ext.equalsIgnoreCase("doc") || 
    					  sub_ext.equalsIgnoreCase("docx")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.word);
    			
    			} else if(sub_ext.equalsIgnoreCase("xls") || 
    					  sub_ext.equalsIgnoreCase("xlsx")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.excel);
    				
    			} else if(sub_ext.equalsIgnoreCase("ppt") ||
    					  sub_ext.equalsIgnoreCase("pptx")) {
    				
    				mViewHolder.icon.setImageResource(R.drawable.ppt);   	
    				
    			} else if(sub_ext.equalsIgnoreCase("html")) {
    				mViewHolder.icon.setImageResource(R.drawable.html32);  
    				
    			} else if(sub_ext.equalsIgnoreCase("xml")) {
    				mViewHolder.icon.setImageResource(R.drawable.xml32);
    				
    			} else if(sub_ext.equalsIgnoreCase("conf")) {
    				mViewHolder.icon.setImageResource(R.drawable.config32);
    				
    			} else if(sub_ext.equalsIgnoreCase("apk")) {
    				mViewHolder.icon.setImageResource(R.drawable.appicon);
    				
    			} else if(sub_ext.equalsIgnoreCase("jar")) {
    				mViewHolder.icon.setImageResource(R.drawable.jar32);
    				
    			} else {
    				mViewHolder.icon.setImageResource(R.drawable.text);
    			}
    			
    		} else if (file != null && file.isDirectory()) {
    			mViewHolder.icon.setImageResource(R.drawable.folder);
    		}
    		    		
    		String permission = getFilePermissions(file);
    		
    		if(file.isFile()) {
    			double size = file.length();
        		if (size > GB)
    				display_size = String.format("%.2f Gb ", (double)size / GB);
    			else if (size < GB && size > MG)
    				display_size = String.format("%.2f Mb ", (double)size / MG);
    			else if (size < MG && size > KB)
    				display_size = String.format("%.2f Kb ", (double)size/ KB);
    			else
    				display_size = String.format("%.2f bytes ", (double)size);
        		
        		if(file.isHidden())
        			mViewHolder.bottomView.setText("(hidden) | " + display_size +" | "+ permission);
        		else
        			mViewHolder.bottomView.setText(display_size +" | "+ permission);
        		
    		} else {
    			if(file.isHidden())
    				mViewHolder.bottomView.setText("(hidden) | " + num_items + " items | " + permission);
    			else
    				mViewHolder.bottomView.setText(num_items + " items | " + permission);
    		}
    		
    		mViewHolder.topView.setText(file.getName());
    		
    		return convertView;
    	}

    }


    public class GridRow extends ArrayAdapter<String> {

        public GridRow() {
            super(mContext, R.layout.grid_item, mDataSource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final GridViewHolder mViewHolder;
            String temp = mFileMang.getCurrentDir();
            File file = new File(temp + "/" + mDataSource.get(position));

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.grid_item, parent, false);

                mViewHolder = new GridViewHolder();
                mViewHolder.name = (TextView)convertView.findViewById(R.id.name);
                mViewHolder.icon = (ImageView)convertView.findViewById(R.id.image);

                convertView.setTag(mViewHolder);

            } else {
                mViewHolder = (GridViewHolder)convertView.getTag();
            }

            if(mThumbnail == null)
                mThumbnail = new ThumbnailCreator(-1, -1);

            if(file != null && file.isFile()) {
                String ext = file.toString();
                String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);

    			/* This series of else if statements will determine which
    			 * icon is displayed
    			 */
                if (sub_ext.equalsIgnoreCase("pdf")) {
                    mViewHolder.icon.setImageResource(R.drawable.pdf);

                } else if (sub_ext.equalsIgnoreCase("mp3") ||
                        sub_ext.equalsIgnoreCase("wma") ||
                        sub_ext.equalsIgnoreCase("m4a") ||
                        sub_ext.equalsIgnoreCase("m4p")) {

                    mViewHolder.icon.setImageResource(R.drawable.music);

                } else if (sub_ext.equalsIgnoreCase("png") ||
                        sub_ext.equalsIgnoreCase("jpg") ||
                        sub_ext.equalsIgnoreCase("jpeg")||
                        sub_ext.equalsIgnoreCase("gif") ||
                        sub_ext.equalsIgnoreCase("tiff")) {

                    if(thumbnail_flag && file.length() != 0) {
                        Bitmap thumb = mThumbnail.isBitmapCached(file.getPath());

                        if (thumb == null) {
                            final Handler handle = new Handler(new Handler.Callback() {
                                public boolean handleMessage(Message msg) {
                                    notifyDataSetChanged();

                                    return true;
                                }
                            });

                            mThumbnail.createNewThumbnail(mDataSource, mFileMang.getCurrentDir(), handle);

                            if (!mThumbnail.isAlive())
                                mThumbnail.start();

                        } else {
                            mViewHolder.icon.setImageBitmap(thumb);
                        }

                    } else {
                        mViewHolder.icon.setImageResource(R.drawable.image);
                    }

                } else if (sub_ext.equalsIgnoreCase("zip")  ||
                        sub_ext.equalsIgnoreCase("gzip") ||
                        sub_ext.equalsIgnoreCase("gz")) {

                    mViewHolder.icon.setImageResource(R.drawable.zip);

                } else if(sub_ext.equalsIgnoreCase("m4v") ||
                        sub_ext.equalsIgnoreCase("wmv") ||
                        sub_ext.equalsIgnoreCase("3gp") ||
                        sub_ext.equalsIgnoreCase("mp4")) {

                    mViewHolder.icon.setImageResource(R.drawable.movies);

                } else if(sub_ext.equalsIgnoreCase("doc") ||
                        sub_ext.equalsIgnoreCase("docx")) {

                    mViewHolder.icon.setImageResource(R.drawable.word);

                } else if(sub_ext.equalsIgnoreCase("xls") ||
                        sub_ext.equalsIgnoreCase("xlsx")) {

                    mViewHolder.icon.setImageResource(R.drawable.excel);

                } else if(sub_ext.equalsIgnoreCase("ppt") ||
                        sub_ext.equalsIgnoreCase("pptx")) {

                    mViewHolder.icon.setImageResource(R.drawable.ppt);

                } else if(sub_ext.equalsIgnoreCase("html")) {
                    mViewHolder.icon.setImageResource(R.drawable.html32);

                } else if(sub_ext.equalsIgnoreCase("xml")) {
                    mViewHolder.icon.setImageResource(R.drawable.xml32);

                } else if(sub_ext.equalsIgnoreCase("conf")) {
                    mViewHolder.icon.setImageResource(R.drawable.config32);

                } else if(sub_ext.equalsIgnoreCase("apk")) {
                    mViewHolder.icon.setImageResource(R.drawable.appicon);

                } else if(sub_ext.equalsIgnoreCase("jar")) {
                    mViewHolder.icon.setImageResource(R.drawable.jar32);

                } else {
                    mViewHolder.icon.setImageResource(R.drawable.text);
                }

            } else if (file != null && file.isDirectory()) {
                mViewHolder.icon.setImageResource(R.drawable.icon);
            }

            mViewHolder.name.setText(file.getName());

            return convertView;
        }

    }

}

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

import android.app.ActivityManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MemoryManager extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.memory_layout);
		
		TextView internal_total_value = (TextView)findViewById(R.id.internal_total_value);
		TextView internal_free_value = (TextView)findViewById(R.id.internal_free_value);
		TextView internal_used_value = (TextView)findViewById(R.id.internal_used_value);
		TextView ram_total_value = (TextView)findViewById(R.id.ram_total_value);
		TextView ram_free_value = (TextView)findViewById(R.id.ram_free_value);
		TextView ram_used_value = (TextView)findViewById(R.id.ram_used_value);

		long total, free, used;
		int kb = 1024;

		StatFs fs = new StatFs(Environment.
				getExternalStorageDirectory().getPath());

		total = fs.getBlockCount() * (fs.getBlockSize() / kb);
		free = fs.getAvailableBlocks() * (fs.getBlockSize() / kb);
		used = total - free;

		internal_total_value.setText(String.format("  %.2f GB ",	(double) total / (kb * kb)));
		internal_free_value.setText(String.format("  %.2f GB", (double) free / (kb * kb)));
		internal_used_value.setText(String.format("  %.2f GB", (double) used / (kb * kb)));


		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		ProgressBar progressBar_internal = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		LinearLayout internalLayout = (LinearLayout)findViewById(R.id.internal_layout);
		progressBar_internal.setIndeterminate(false);
		progressBar_internal.setMax((int) total);
		progressBar_internal.setProgress((int) used);
		internalLayout.addView(progressBar_internal, params);


		RandomAccessFile reader = null;
		String load = null;
		String[] buf = null;
		long ram_total = 0;

		try {
			reader = new RandomAccessFile("/proc/meminfo", "r");
			load = reader.readLine();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		if (load != null) {
			buf = load.split("\\s+");
			ram_total = Long.valueOf(buf[1]);
			ram_total_value.setText(String.format("  %.2f MB ",	(double) ram_total / kb));
		}


		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		ram_free_value.setText(String.format("  %.2f MB ",	(double) mi.availMem / (kb * kb)));

		long ram_used = ram_total * kb - mi.availMem;
		ram_used_value.setText(String.format("  %.2f MB ",	(double) ram_used / (kb * kb)));

		LinearLayout.LayoutParams params_ram = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		ProgressBar progressBar_ram = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		LinearLayout ramLayout = (LinearLayout)findViewById(R.id.ram_layout);
		progressBar_ram.setIndeterminate(false);
		progressBar_ram.setMax((int) ram_total);
		progressBar_ram.setProgress((int) ram_used / kb);
		ramLayout.addView(progressBar_ram, params_ram);

	}

}

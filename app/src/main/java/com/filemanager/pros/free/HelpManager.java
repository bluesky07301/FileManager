
package com.filemanager.pros.free;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;


public class HelpManager extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.help_layout);

	}
}

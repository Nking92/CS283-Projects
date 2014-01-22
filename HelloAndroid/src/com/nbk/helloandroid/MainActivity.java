package com.nbk.helloandroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "onCreate called");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart called");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume called");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause called");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop called");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy called");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}

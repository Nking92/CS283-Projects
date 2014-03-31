package edu.vu.kingnb.tictactoe.client;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import edu.vanderbilt.cs283.kingnb.tictactoe.R;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private TicTacToe mTicTacToe = new TicTacToe();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void turn(int i, int j, View v) {
		if (mTicTacToe.isGameOver()) return;
		mTicTacToe.makeTurn(i, j);
		Button b = (Button) v;
		b.setText("" + mTicTacToe.getSymbol(i, j));
		if (mTicTacToe.isGameOver()) {
			if (mTicTacToe.isTie()) {
				Toast.makeText(this, "Tie", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, mTicTacToe.getWinner() + " wins!",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	public void onTopLeftClick(View v) {
		Log.d(TAG, "top left click");
		turn(0, 0, v);
	}

	public void onTopCenterClick(View v) {
		Log.d(TAG, "top center click");
		turn(0, 1, v);
	}

	public void onTopRightClick(View v) {
		Log.d(TAG, "top right click");
		turn(0, 2, v);
	}

	public void onMiddleLeftClick(View v) {
		Log.d(TAG, "middle left click");
		turn(1, 0, v);
	}

	public void onMiddleCenterClick(View v) {
		Log.d(TAG, "middle center click");
		turn(1, 1, v);
	}

	public void onMiddleRightClick(View v) {
		Log.d(TAG, "middle right click");
		turn(1, 2, v);
	}

	public void onBottomLeftClick(View v) {
		Log.d(TAG, "bottom left click");
		turn(2, 0, v);
	}

	public void onBottomCenterClick(View v) {
		Log.d(TAG, "bottom center click");
		turn(2, 1, v);
	}

	public void onBottomRightClick(View v) {
		Log.d(TAG, "bottom right click");
		turn(2, 2, v);
	}

	public void onGameStart(View v) {
		Log.d(TAG, "game start click");
	}

}

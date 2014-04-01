
package edu.vu.kingnb.tictactoe.client;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import edu.vanderbilt.cs283.kingnb.tictactoe.R;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    public static final int MOVE_RECEIVED = 0;

    public static final int GAME_READY = 1;

    // ======================================================================
    // VIEWS
    // ======================================================================

    private Button mTopLeft, mTopCenter, mTopRight, mMiddleLeft, mMiddleCenter, mMiddleRight,
            mBottomLeft, mBottomCenter, mBottomRight;

    // ======================================================================
    // END VIEWS
    // ======================================================================

    private TicTacToe mTicTacToe = new TicTacToe();

    private TicTacToeService mService;

    private boolean mBound = false;

    private boolean mConnectRequested = false;

    private boolean mGameStarted = false;

    private boolean isMyTurn = false;

    private static class MyHandler extends Handler {

        WeakReference<MainActivity> mRef;

        MyHandler(MainActivity a) {
            mRef = new WeakReference<MainActivity>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity a = mRef.get();
            if (a == null)
                return;
            switch (msg.what) {
                case MOVE_RECEIVED:
                    a.opponentTurn(msg.arg1, msg.arg2);
                    break;
                case GAME_READY:
                    a.startGame(msg.arg1);
                default:
                    super.handleMessage(msg);
            }
        }

    }

    private Messenger mMessenger = new Messenger(new MyHandler(this));

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName cn, IBinder binder) {
            mService = ((TicTacToeService.TicTacToeBinder)binder).getService();
            mService.setMessenger(mMessenger);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TicTacToeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void opponentTurn(int row, int col) {
    }

    private void turn(int row, int col, View v) {
        if (mTicTacToe.isGameOver()) {
            Toast.makeText(this, "The game has already ended", Toast.LENGTH_LONG).show();
            return;
        } else if (!isMyTurn) {
            Toast.makeText(this, "It is not your turn.", Toast.LENGTH_LONG).show();
            return;
        }
        mTicTacToe.makeTurn(row, col);
        mService.sendMove(row, col);
        Button b = (Button)v;
        b.setText("" + mTicTacToe.getSymbol(row, col));
        if (mTicTacToe.isGameOver()) {
            if (mTicTacToe.isTie()) {
                Toast.makeText(this, "Tie", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, mTicTacToe.getWinner() + " wins!", Toast.LENGTH_LONG).show();
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

    public void onGameStartClick(View v) {
        Log.d(TAG, "game start click");
        if (!mConnectRequested && mBound) {
            mService.connectToServer();
            mConnectRequested = true;
        }
    }

    public void startGame(int myTurn) {
        mGameStarted = true;
        String toastMsg = "Game started! ";
        if (myTurn == 1) {
            isMyTurn = true;
            toastMsg += "You have the first turn.";
        } else {
            toastMsg += "Your opponent goes first.";
        }
        Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
    }

}

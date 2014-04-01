
package edu.vu.kingnb.tictactoe.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TicTacToeService extends Service {

    private static final String TAG = "TicTacToeService";

    private static final Random RAND = new Random();

    private static final int ACK_TIMEOUT = 5;

    private static final int MAX_PACKET_SIZE = 512;

    public static final String AMAZON_SERVER_IP = "54.186.61.49";

    public static final int AMAZON_SERVER_PORT = 20000;

    private static final InetSocketAddress mSocketAddress = new InetSocketAddress(AMAZON_SERVER_IP,
            AMAZON_SERVER_PORT);

    private static final Map<String, ScheduledExecutorService> PENDING_RESPONSE_MAP = Collections
            .synchronizedMap(new HashMap<String, ScheduledExecutorService>());

    private final IBinder mBinder = new TicTacToeBinder();

    private DatagramSocket mSocket;

    private Integer mClientId;

    private int mRoll = RAND.nextInt();

    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private AtomicBoolean mOpponentFound = new AtomicBoolean(false);

    private AtomicBoolean mGameReady = new AtomicBoolean(false);

    private AtomicBoolean mUnbound = new AtomicBoolean(false);

    private AtomicBoolean mMyTurn = new AtomicBoolean(false);

    private ScheduledExecutorService mConnectExecutor;
    
    private Thread mReceiveThread = new Thread() {

        @Override
        public void run() {
            while (!mUnbound.get()) {
                byte[] buf = new byte[MAX_PACKET_SIZE];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    mSocket.receive(p);
                    String response = new String(p.getData(), 0, p.getData().length);
                    new ReactorThread(response).start();
                } catch (IOException e) {
                    Log.e(TAG, "Error receiving connection response", e);
                }
            }
        }

    };
    
    private Messenger mActivityMessenger;

    private class ReactorThread extends Thread {
        final String mResponse;

        ReactorThread(String response) {
            mResponse = response;
        }

        @Override
        public void run() {
            if (mResponse.startsWith("received")) {
                onAckReceived(mResponse);
                return;
            }

            String[] arr = mResponse.split(",");
            String ack_id = arr[0].split("=")[1];

            String msg = arr[1];
            if (msg.startsWith("client_id")) {
                onConnected(msg);
            } else if (msg.startsWith("opponent_found")) {
                onOpponentFound();
            } else if (msg.startsWith("random_num")) {
                onRollReceived(msg);
            } else if (msg.startsWith("row")) {
                onMoveReceived(arr[1], arr[2]);
            }

            acknowledge(ack_id);
        }
    }

    // ======================================================================
    // RESPONSE HANDLERS
    // ======================================================================
    private void onConnected(String msg) {
        mClientId = Integer.parseInt(msg.split("=")[1]);
        mConnected.set(true);
    }

    private void onOpponentFound() {
        mOpponentFound.set(true);
        sendRoll();
    }

    private void onRollReceived(String msg) {
        int opponentRoll = Integer.parseInt(msg.split("=")[1]);
        // Higher roller goes first
        if (opponentRoll < mRoll) {
            mMyTurn.set(true);
            notifyGameReady(true);
        } else if (opponentRoll == mRoll) {
            // Reroll
            synchronized (RAND) {
                mRoll = RAND.nextInt();
            }
            sendRoll();
        } else {
            mMyTurn.set(false);
            notifyGameReady(false);
        }
    }
    
    private void notifyGameReady(boolean myTurn) {
        mGameReady.set(true);
        Message m = Message.obtain(null, MainActivity.GAME_READY, (myTurn ? 1 : 0), 0);
        try {
            mActivityMessenger.send(m);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while sending message", e);
        }
    }

    private void onMoveReceived(String rowMsg, String colMsg) {
        if (!mMyTurn.get()) {
            int row = Integer.parseInt(rowMsg.split("=")[1]);
            int col = Integer.parseInt(colMsg.split("=")[1]);
            Message message = Message.obtain(null, MainActivity.MOVE_RECEIVED, row, col);
            try {
                mActivityMessenger.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while sending message", e);
            }
        } else {
            // ignore message
        }
    }

	private void onAckReceived(String msg) {
	    String ack = msg.split("=")[1];
		ScheduledExecutorService sx = PENDING_RESPONSE_MAP.remove(ack);
		if (sx != null) {
			sx.shutdownNow();
			try {
				sx.awaitTermination(ACK_TIMEOUT + 1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Got unknown ack: " + ack);
		}
	}

    public class TicTacToeBinder extends Binder {
        public TicTacToeService getService() {
            return TicTacToeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        try {
            mSocket = new DatagramSocket();
            mReceiveThread.start();
        } catch (SocketException e) {
            Log.e(TAG, "Error creating socket", e);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mUnbound.set(true);
        return false;
    }
    
    public void setMessenger(Messenger m) {
        mActivityMessenger = m;
    }

    public void connectToServer() {
        mConnectExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable connectRunnable = new Runnable() {

            @Override
            public void run() {
                String msg = "connect";
                byte[] data = msg.getBytes();
                try {
                    DatagramPacket p = new DatagramPacket(data, data.length, mSocketAddress);
                    mSocket.send(p);
                } catch (SocketException e) {
                    Log.e(TAG, "Error creating connect packet", e);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending connect packet", e);
                }
            }

        };
        mConnectExecutor.scheduleAtFixedRate(connectRunnable, 0, ACK_TIMEOUT, TimeUnit.SECONDS);
    }
    
    public void sendMove(int row, int col) {
        send("row=" + row + ",col=" + col);
    }

    private void acknowledge(final String ack_id) {
        String msg = "received=" + ack_id;
        byte[] buf = msg.getBytes();
        final DatagramPacket p = new DatagramPacket(buf, buf.length);
        new Thread() {
            @Override
            public void run() {
                try {
                    mSocket.send(p);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't send acknowledgement for ack_id=" + ack_id);
                }
            }
        }.start();
    }

    private void sendRoll() {
        send("random_num=" + mRoll);
    }

    private String generateAck() {
        synchronized (RAND) {
            return "ack_id=" + RAND.nextInt() % 10000;
        }
    }

    // send a string, wrapped in a UDP packet, to the specified remote endpoint
    // every 5 seconds until an ack is received
    public void send(String pload) {
        if (!pload.endsWith("\n")) {
            pload = pload + "\n";
        }
        String ack = generateAck();
        while (PENDING_RESPONSE_MAP.containsKey(ack)) {
            ack = generateAck();
        }
        // Append the ACK and client id to the beginning of the payload
        final String payload = ack + ",client_id=" + mClientId + "," + pload;
        final ScheduledExecutorService sx = Executors.newSingleThreadScheduledExecutor();
        Runnable sendRunnable = new Runnable() {
            public void run() {
                try {
                    DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
                            payload.length(), mSocketAddress);
                    TicTacToeService parent = TicTacToeService.this;
                    if (!parent.mSocket.isClosed()) {
                        Log.d(TAG, "Sending " + payload + " to server");
                        parent.mSocket.send(txPacket);
                    } else {
                        Log.d(TAG, "Failed to send " + payload + " because of closed socket");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while trying to send message " + payload, e);
                    // Shut down the executor so we don't keep sending to a
                    // bad connection
                    sx.shutdown();
                }
            }
        };
        sx.scheduleAtFixedRate(sendRunnable, 0, ACK_TIMEOUT, TimeUnit.SECONDS);
        PENDING_RESPONSE_MAP.put(ack, sx);
    }

}

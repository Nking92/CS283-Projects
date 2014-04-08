package edu.vanderbilt.pub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	private static final Logger LOGGER = LoggerFactory.getLogger("Server");
	private static final int DEFAULT_PORT = 20050;
	private static final int NUM_ORDERS_TO_STORE = 75;

	private final ServerSocket mServerSocket;
	private final ExecutorService mExecutor = Executors.newCachedThreadPool();
	private final Queue<Integer> mOrderNums = new LinkedList<Integer>();
	private String mCached;
	private AtomicBoolean isCacheGood = new AtomicBoolean(false);

	private Server(int port) throws IOException {
		mServerSocket = new ServerSocket(port);
	}

	private void serve() throws IOException {
		while (true) {
			Socket s = mServerSocket.accept();
			try {
				mExecutor.submit(new ResponseHandler(s));
			} catch (RejectedExecutionException e) {
				LOGGER.error("Task rejected", e);
			}
		}
	}

	public static void main(String[] args) {
		try {
			new Server(DEFAULT_PORT).serve();
		} catch (Exception e) {
			LOGGER.error("Fatal: Caught exception in main", e);
		}
		LOGGER.error("Server shutting down");
	}

	private class ResponseHandler implements Runnable {

		private Socket mSocket;

		public ResponseHandler(Socket s) {
			mSocket = s;
		}

		@Override
		public void run() {
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(
						mSocket.getInputStream()));
				String line = rdr.readLine();
				String[] arr = line.split(" ");
				String request = arr[0].toLowerCase();
				if (request.equals("post")) {
					addOrder(arr[1]);
				} else if (request.equals("read")) {
					sendOrders(mSocket);
				} else {
					LOGGER.error("Got invalid request {}", request);
				}
			} catch (IOException e) {
				LOGGER.error("Response handler exception", e);
			} finally {
				try {
					mSocket.close();
				} catch (IOException e) {
					LOGGER.error("Error closing socket", e);
				}
			}
		}

		private void addOrder(String order) {
			Integer orderNum = Integer.valueOf(order);
			LOGGER.info("Adding order number {}", order);
			synchronized (mOrderNums) {
				if (mOrderNums.size() > NUM_ORDERS_TO_STORE) {
					mOrderNums.remove();
				}
				mOrderNums.offer(orderNum);
				isCacheGood.set(false);
			}
		}

		private String getOrderStr() {
			if (!isCacheGood.get()) {
				synchronized (mOrderNums) {
					StringBuilder sb = new StringBuilder();
					for (Integer i : mOrderNums) {
						sb.append(i).append(",");
					}
					if (sb.length() > 0) {
						// delete trailing comma
						sb.deleteCharAt(sb.length() - 1);
					}
					mCached = sb.toString();
					isCacheGood.set(true);
				}
			}
			return mCached;
		}

		private void sendOrders(Socket s) throws IOException {
			PrintStream writer = new PrintStream(s.getOutputStream());
			writer.println(getOrderStr());
			writer.close();
		}
	}

}

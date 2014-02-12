package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BenchmarkClient implements Client {

	private static final int NUM_MSG = 1000;
	private static final String[] STRINGS = {
			"oaiwjfioj2jiosjfoijg8290jtjkgj,avz.sadfo2",
			"oifjdiwojfjbz,.jxfjozjsodifjoi24", "str3",
			"sdfjsfdWDFWDFWDFGJGHKHJKJGKLASJHGKLJSLAGKSLFKL>.s,asf..s",
			"wdoifjsoijsogijgwes/a//a//////" };

	private InetAddress mAddress;
	private int mPort;
	private Runnable mQueryRunnable = new Runnable() {
		private Random mRandom = new Random();

		@Override
		public void run() {
			Socket socket = null;
			PrintWriter writer = null;
			try {
				socket = new Socket(mAddress, mPort);
				writer = new PrintWriter(socket.getOutputStream(), true);
				for (int i = 0; i < NUM_MSG; i++) {
					writer.println(randomString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Helpers.closeIfNotNull(socket, writer);
			}
		}

		private String randomString() {
			int i = mRandom.nextInt(5);
			return STRINGS[i];
		}
	};

	public BenchmarkClient(InetAddress addr, int port) {
		mAddress = addr;
		mPort = port;
	}

	@Override
	public void request() {
		// Spawn 1000 threads, let them send messages to the server 1000 times
		// each, and time how long it takes
		List<Thread> threads = new ArrayList<Thread>(20);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Thread thread = new Thread(mQueryRunnable);
			threads.add(thread);
			thread.start();
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		long end = System.currentTimeMillis();
		System.out.println("Benchmark took " + (end - start) + " milliseconds");
	}

}

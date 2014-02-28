
package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class BenchmarkClient implements Client {

    private static final int NUM_THREADS = 50;

    private InetAddress mAddress;

    private AtomicBoolean mIsRunning = new AtomicBoolean(true);
    private CountDownLatch mLatch = new CountDownLatch(1);
    private List<Integer> mCounts = new ArrayList<Integer>();

    private int mPort;
    
    private Runnable mThroughputRunnable = new Runnable() {
    	
    	@Override public void run() {
    		try {
				mLatch.await();
			} catch (InterruptedException e) { }
    		Socket socket = null;
            PrintStream writer = null;
            BufferedReader reader = null;
            try {
                socket = new Socket(mAddress, mPort);
                writer = new PrintStream(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int count = 0;
                while (mIsRunning.get()) {
                    writer.println("jello");
                    reader.readLine();
                    count++;
                }
                
                synchronized(mCounts) {
                	mCounts.add(count);
                }
                
                socket.shutdownOutput();

                // Wait for all the responses
                while (reader.readLine() != null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Helpers.closeIfNotNull(writer, socket);
            }
    	}
    	
    };

    public BenchmarkClient(InetAddress addr, int port) {
        mAddress = addr;
        mPort = port;
    }

    @Override
    public void request() {
        // Spawn 50 threads
        List<Thread> threads = new ArrayList<Thread>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
        	Thread thread = new Thread(mThroughputRunnable);
            threads.add(thread);
            thread.start();
        }

        // Let the threads run
        mLatch.countDown();

        // Wait for 10 seconds and stop the threads
        try {
			Thread.sleep(10*1000);
		} catch (InterruptedException e1) {
		}
        
        mIsRunning.set(false);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Reduce the counts
        int totalCount = 0;
        for (Integer n : mCounts) {
        	totalCount += n;
        }
        
        System.out.println("Server served " + totalCount + " queries in 10 seconds");
    }

    @Override
    public void shutdown() {
        // Threads shut down their own resources when they are finished
    }

}

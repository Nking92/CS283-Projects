
package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BenchmarkClient implements Client {

    private static final int NUM_THREADS = 50;

    private static final int NUM_MSG = 3000;

    private static final String[] STRINGS = {
            "oaiwjfioj2jiosjfoijg8290jtjkgj,avz.sadfo2", "oifjdiwojfjbz,.jxfjozjsodifjoi24",
            "str3", "sdfjsfdWDFWDFWDFGJGHKHJKJGKLASJHGKLJSLAGKSLFKL>.s,asf..s",
            "wdoifjsoijsogijgwes/a//a//////"
    };

    private InetAddress mAddress;

    private Object mLock = new Object();

    private volatile boolean mIsRunning = false;

    private int mPort;

    private Runnable mQueryRunnable = new Runnable() {
        private Random mRandom = new Random();

        @Override
        public void run() {
            while (!mIsRunning) {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Thread " + Thread.currentThread().getId() + " awake");
            Socket socket = null;
            PrintStream writer = null;
            BufferedReader reader = null;
            try {
                socket = new Socket(mAddress, mPort);
                writer = new PrintStream(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                for (int i = 0; i < NUM_MSG; i++) {
                    writer.println(randomString());
                }
                socket.shutdownOutput();

                // Wait for all the responses
                while (reader.readLine() != null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Helpers.closeIfNotNull(writer, socket);
                System.out.println("Thread " + Thread.currentThread().getId() + " shutting down");
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
        // Spawn 50 threads, let them send messages to the server 10000 times
        // each, and time how long it takes
        List<Thread> threads = new ArrayList<Thread>(NUM_THREADS);
        long start = System.currentTimeMillis();
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread(mQueryRunnable);
            threads.add(thread);
            thread.start();
        }

        mIsRunning = true;

        synchronized (mLock) {
            mLock.notifyAll();
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

    @Override
    public void shutdown() {
        // Threads shut down their own resources when they are finished
    }

}

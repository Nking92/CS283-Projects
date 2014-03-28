package bananabank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class BananaBankServer {

	public static final int BANANA_PORT = 2000;
	private static final String LINE_REGEX = "\\d+\\s+\\d+\\s+\\d+\\s*";
	private static final String ACCOUNT_FILE = "accounts.txt";
	private static final String SHUTDOWN_STR = "SHUTDOWN";
	private static WorkerThread shutdownThread;

	public static void main(String args[]) {
		System.out.println("Starting server");
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(BANANA_PORT);
		} catch (IOException e) {
			System.out.println("Couldn't create server socket");
			e.printStackTrace();
			System.exit(1);
		}

		BananaBank bank = null;
		try {
			bank = new BananaBank(ACCOUNT_FILE);
		} catch (IOException e) {
			System.out.println("Error opening account file");
			e.printStackTrace();
			System.exit(1);
		}

		List<WorkerThread> threads = new LinkedList<WorkerThread>();
		try {
			while (true) {
				Socket s = ss.accept();
				WorkerThread t = new WorkerThread(s, bank, ss, threads);
				threads.add(t);
				t.start();
			}
		} catch (IOException e) {
			if (!e.getMessage().equals("Socket closed")) {
				e.printStackTrace();
			}
		} finally {
			Helpers.closeIfNotNull(ss);
			if (shutdownThread != null) {
				try {
					shutdownThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Shutdown complete");
	}

	public static int total(Collection<Account> c) {
		int total = 0;
		for (Account a : c) {
			total += a.getBalance();
		}
		return total;
	}

	private static class WorkerThread extends Thread {
		private Socket mSocket;
		private ServerSocket mServerSocket;
		private BananaBank mBank;
		private List<WorkerThread> mWorkers;

		public WorkerThread(Socket socket, BananaBank bank, ServerSocket ss,
				List<WorkerThread> workers) {
			mSocket = socket;
			mBank = bank;
			mServerSocket = ss;
			mWorkers = workers;
		}

		@Override
		public void run() {
			BufferedReader reader = null;
			PrintStream ps = null;
			try {
				String line;
				reader = new BufferedReader(new InputStreamReader(
						mSocket.getInputStream()));
				ps = new PrintStream(mSocket.getOutputStream());
				while ((line = reader.readLine()) != null) {
					if (line.equals(SHUTDOWN_STR)) {
						if (isLocalAddress(mSocket.getInetAddress()
								.getHostAddress())) {
							mServerSocket.close();
							shutdownThread = this;
							for (WorkerThread t : mWorkers) {
								try {
									if (t.getId() != Thread.currentThread()
											.getId()) {
										t.join();
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							ps.println(total(mBank.getAllAccounts()));
							break;
						} else {
							System.out
									.println("Ignoring shutdown request from remote address "
											+ mSocket.getInetAddress());
						}
					} else if (!line.matches(LINE_REGEX)) {
						ps.println("Input \""
								+ line
								+ "\" does not match the required format. Ignoring.");
					} else {
						Scanner scan = new Scanner(line);
						int amount = scan.nextInt();
						int from = scan.nextInt();
						int to = scan.nextInt();
						Account src = null;
						Account dst = null;
						synchronized (mBank) {
							src = mBank.getAccount(from);
							dst = mBank.getAccount(to);
						}
						if (src == null) {
							ps.println("Account number " + from
									+ " invalid. Ignoring transfer " + line);
						} else if (dst == null) {
							ps.println("Account number " + to
									+ " invalid. Ignoring transfer " + line);
						} else {
							// Acquire lock for lower numbered account first
							Account lower = (from < to ? src : dst);
							Account upper = (from < to ? dst : src);
							synchronized (lower) {
								synchronized (upper) {
									src.transferTo(amount, dst);
									ps.println(amount
											+ " transferred from account "
											+ from + " to account " + to + ".");
								}
							}
						}
					}
				}
			} catch (IOException e) {
				if (!e.getMessage().equals("Socket closed")) {
					e.printStackTrace();
				}
			} finally {
				Helpers.closeIfNotNull(reader, ps, mSocket);
			}
		}

	}

	private static boolean isLocalAddress(String ipAddr) {
		return ipAddr.equals("127.0.0.1");
	}

}

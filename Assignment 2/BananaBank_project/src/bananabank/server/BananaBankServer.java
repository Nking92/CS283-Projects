package bananabank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
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
		while (true) {
			try {
				Socket s = ss.accept();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(s.getInputStream()));
				PrintStream ps = new PrintStream(s.getOutputStream());
				String line = reader.readLine();

				if (line.equals(SHUTDOWN_STR)) {
					if (isLocalAddress(s.getInetAddress().getHostAddress())) {
						System.out.println("Beginning shutdown");
						System.out.println("Waiting for worker threads");
						for (Thread t : threads) {
							t.join();
						}
						System.out.println("Writing account info to file");
						bank.save(ACCOUNT_FILE);
						ps.println(total(bank.getAllAccounts()));
						System.out.println("Closing server socket.");
						s.close();
						ss.close();
						System.out.println("Sockets closed");
						break;
					} else {
						System.out.println("Ignoring shutdown request from remote address "
								+ s.getInetAddress());
					}
				}

				new WorkerThread(s, bank, reader, ps, line).start();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
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
		private BananaBank mBank;
		private PrintStream mPs;
		private BufferedReader mReader;
		private String mFirstLine;

		public WorkerThread(Socket socket, BananaBank bank,
				BufferedReader reader, PrintStream ps, String firstLine) {
			mSocket = socket;
			mBank = bank;
			mReader = reader;
			mPs = ps;
			mFirstLine = firstLine;
		}

		@Override
		public void run() {
			try {
				boolean firstRead = false;
				String line;
				while ((line = (!firstRead ? mFirstLine : mReader.readLine())) != null) {
					if (line.equals(SHUTDOWN_STR)) {
						if (isLocalAddress(mSocket.getInetAddress()
								.getHostAddress())) {
							// Tell the server to shut down
							Socket s = new Socket("localhost", BANANA_PORT);
							PrintStream out = new PrintStream(
									s.getOutputStream());
							out.println(SHUTDOWN_STR);
							// Echo the total
							mPs.println(new BufferedReader(
									new InputStreamReader(s.getInputStream()))
									.readLine());
							Helpers.closeIfNotNull(out, s);
							break;
						} else {
							System.out.println("Ignoring shutdown request from remote address "
									+ mSocket.getInetAddress());
						}
					} else if (!line.matches(LINE_REGEX)) {
						mPs.println("Input \""
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
							mPs.println("Account number " + from
									+ " invalid. Ignoring transfer " + line);
						} else if (dst == null) {
							mPs.println("Account number " + to
									+ " invalid. Ignoring transfer " + line);
						} else {
							// Acquire lock for lower numbered account first
							Account lower = (from < to ? src : dst);
							Account upper = (from < to ? dst : src);
							synchronized (lower) {
								synchronized (upper) {
									src.transferTo(amount, dst);
									mPs.println(amount
											+ " transferred from account "
											+ from + " to account " + to + ".");
								}
							}
						}
					}
					firstRead = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Helpers.closeIfNotNull(mReader, mPs, mSocket);
			}
		}

	}

	private static boolean isLocalAddress(String ipAddr) {
		return ipAddr.equals("127.0.0.1");
	}

}

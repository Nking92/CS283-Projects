package udpgroupchat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import udpgroupchat.server.Server;

public class Client {

	// The possible channels
	// The server isn't limited to these channels, but this is needed to test
	// the server
	public static final String[] CHANNELS = { "CS283", "Interns", "Cars",
			"Toys", "Gamers", "Hackers" };
	// A list of words for making random messages
	public static final String[] WORDS = { "I", "me", "his", "her", "cook",
			"cars", "videos", "chicken", "your", "you", "she", "he",
			"chocolate", "physics", "music", "polite", "blue", "charisma",
			"pudding", "pencil", "laptop", "red", "green", "gray", "clock",
			"time", "power", "star", "moon", "purple", "lamp", "random" };
	public static final String[] NAMES = { "Jack", "Jill", "Bob", "Stacy",
			"Keenan", "Zack", "Lucy", "Nick", "Carl", "Cody", "Megan", "Lee",
			"Rachel", "Michael", "Ray" };

	InetSocketAddress serverSocketAddress;
	String[] myChannels = new String[3];
	int id;
	DatagramSocket socket;

	// constructor
	Client(String serverAddress, int serverPort) {
		serverSocketAddress = new InetSocketAddress(serverAddress, serverPort);
	}

	// start up the server
	public void start() {
		try {

			socket = new DatagramSocket();
			send("REGISTER");

			byte[] buf = new byte[Server.MAX_PACKET_SIZE];
			final DatagramPacket rxPacket = new DatagramPacket(buf, buf.length);
			socket.receive(rxPacket);
			String payload = new String(rxPacket.getData(), 0,
					rxPacket.getLength());
			sendAck(payload);
			Scanner scan = new Scanner(payload);
			// Get the id the server gave us
			// Skip the ack and success message
			scan.next();
			scan.next();
			id = scan.nextInt();
			System.out.println("Registered with server.  My id is: " + id);

			// Set a random name
			setRandomName();
			
			// List the channels
			listChannels();

			// Join 3 of the 6 channels stored in the static array
			Random rand = new Random();
			for (int i = 0; i < 3; i++) {
				myChannels[i] = CHANNELS[rand.nextInt(CHANNELS.length)];
				joinChannel(myChannels[i]);
			}

			// Send random messages to a random group every 2 seconds
			// Poll the server for messages every 5 seconds
			final ScheduledExecutorService msgEx = Executors
					.newSingleThreadScheduledExecutor(), pollEx = Executors
					.newSingleThreadScheduledExecutor();

			Runnable msgRun = new Runnable() {
				@Override
				public void run() {
					sendRandomMsg();
				}
			};

			Runnable pollRun = new Runnable() {
				@Override
				public void run() {
					pollServer();
				}
			};

			msgEx.scheduleAtFixedRate(msgRun, 0, 2, TimeUnit.SECONDS);
			pollEx.scheduleAtFixedRate(pollRun, 0, 5, TimeUnit.SECONDS);

			// Keep retrieving responses for a random number of seconds between
			// 30 and 45 before unregistering
			int numSecs = rand.nextInt(15) + 30;
			Runnable run = new Runnable() {
				public void run() {
					try {
						while (!Thread.interrupted()) {
							try {
								socket.receive(rxPacket);
							} catch (IOException e) {
								e.printStackTrace();
							}
							String payload = new String(rxPacket.getData(), 0,
									rxPacket.getLength());
							sendAck(payload);
							// Echo the payload
							System.out.print(payload);
						}
                        sendUnregister();
					} finally {
						msgEx.shutdownNow();
						pollEx.shutdownNow();
						if (socket != null && !socket.isClosed())
							socket.close();
					}
				}
			};
			Thread t = new Thread(run);
			t.start();

			Thread.sleep(numSecs * 1000);
			System.out.println("Shutting down client");
			t.interrupt();
		} catch (IOException e) {
			// we jump out here if there's an error
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// let the thread close the socket
		}
	}

	private void sendAck(String payload) {
		Scanner scan = new Scanner(payload);
		String ack = scan.next();
		send(ack);
	}

	private void joinChannel(String channel) {
		String msg = "JOIN " + id + " " + channel;
		send(msg);
	}

	private void sendRandomMsg() {
		Random rand = new Random();
		String channel = myChannels[rand.nextInt(3)];
		String msg = "SEND " + id + " " + channel + ": ";
		// Append 4 random words
		for (int i = 0; i < 4; i++) {
			msg += WORDS[rand.nextInt(WORDS.length)] + " ";
		}
		msg = msg.trim();
		send(msg);
	}
	
	private void listChannels() {
		send("LIST .*");
	}

	private void pollServer() {
		String msg = "POLL " + id;
		send(msg);
	}

	private void setRandomName() {
		Random rand = new Random();
		String name = NAMES[rand.nextInt(NAMES.length)] + rand.nextInt(100);
		String msg = "SETNAME " + id + " " + name;
		send(msg);
	}

	private void sendUnregister() {
		send("UNREGISTER " + id);
	}

	private void send(String msg) {
		if (!socket.isClosed()) {
			byte[] bytes = msg.getBytes();
			try {
				DatagramPacket p = new DatagramPacket(bytes, bytes.length,
						serverSocketAddress);
				socket.send(p);
			} catch (SocketException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// main method
	public static void main(String[] args) {
		int serverPort = Server.DEFAULT_PORT;
		String serverAddress = "localhost";

		// check if server address and port were given as command line arguments
		if (args.length > 0) {
			serverAddress = args[0];
		}

		if (args.length > 1) {
			try {
				serverPort = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid serverPort specified: " + args[0]);
				System.out.println("Using default serverPort " + serverPort);
			}
		}

		// instantiate the client
		Client client = new Client(serverAddress, serverPort);

		// start it
		client.start();
	}

}

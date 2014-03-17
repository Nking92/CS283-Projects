package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import udpgroupchat.data.Message;

public class Server {

	// constants
	public static final int DEFAULT_PORT = 20000;
	public static final int MAX_PACKET_SIZE = 512;

	// port number to listen on
	protected int port;

	// set of clientEndPoints
	// note that this is synchronized, i.e. safe to be read/written from
	// concurrent threads without additional locking
	// Maps client IDs to IP addresses
	/* package */static final Map<Integer, ClientEndPoint> clientMap = Collections
			.synchronizedMap(new HashMap<Integer, ClientEndPoint>());
	// Maps client IDs to names
	/* package */static final Map<Integer, String> clientNameMap = Collections
			.synchronizedMap(new HashMap<Integer, String>());
	// Maps client IDs to unsent messages
	/* package */static final Map<Integer, List<Message>> clientMsgMap = Collections
			.synchronizedMap(new HashMap<Integer, List<Message>>());
	// Maps group names to client IDs
	/* package */static final Map<String, List<Integer>> clientGroupMap = Collections
			.synchronizedMap(new HashMap<String, List<Integer>>());

	/* package */static final AtomicBoolean isShutdown = new AtomicBoolean(
			false);

	/* package */static final List<WorkerThread> threads = Collections
			.synchronizedList(new LinkedList<WorkerThread>());

	/* package */static DatagramSocket socket;

	// constructor
	Server(int port) {
		this.port = port;
	}

	// start up the server
	public void start() {
		try {
			// create a datagram socket, bind to port port. See
			// http://docs.oracle.com/javase/tutorial/networking/datagrams/ for
			// details.

			socket = new DatagramSocket(port);

			// receive packets until shutdown initiated (so we don't keep
			// creating
			// new WorkerThreads during shutdown)
			while (!isShutdown.get()) {
				// create an empty UDP packet
				byte[] buf = new byte[Server.MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				WorkerThread t = new WorkerThread(packet, socket);
				threads.add(t);
				t.start();
			}

			while (!socket.isClosed())
				;
		} catch (IOException e) {
			// we jump out here if there's an error, or if the worker thread (or
			// someone else) closed the socket
			if (!e.getMessage().contains("Socket closed")) {
				e.printStackTrace();
			}
		} finally {
			if (socket != null && !socket.isClosed())
				socket.close();
			System.out.println("Socket closed.  Server shut down.");
		}
	}

	// main method
	public static void main(String[] args) {
		int port = Server.DEFAULT_PORT;

		// check if port was given as a command line argument
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port specified: " + args[0]);
				System.out.println("Using default port " + port);
			}
		}

		// instantiate the server
		Server server = new Server(port);

		System.out
				.println("Starting server. Connect with netcat (nc -u localhost "
						+ port
						+ ") or start multiple instances of the client app to test the server's functionality.");

		// start it
		server.start();

	}

}

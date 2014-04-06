package edu.vanderbilt.pub;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	private static final Logger LOGGER = LoggerFactory.getLogger("Server");

	private DatagramSocket mSocket;
	private Map<ClientToken, ClientEndPoint> TOKEN_TO_CLIENT_MAP = Collections
			.synchronizedMap(new HashMap<ClientToken, ClientEndPoint>());
	private Map<ClientEndPoint, ClientToken> CLIENT_TO_TOKEN_MAP = Collections
			.synchronizedMap(new HashMap<ClientEndPoint, ClientToken>());
	private final Random rand = new Random();

	public Server(int port) throws SocketException {
		mSocket = new DatagramSocket(port);
	}

	public void serve() throws IOException {
		try {
			while (true) {
				// create an empty UDP packet
				byte[] buf = new byte[Constants.MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				mSocket.receive(packet);
				LOGGER.debug("Accepted packet from {}:{}", packet.getAddress(),
						packet.getPort());
				WorkerThread t = new WorkerThread(packet, mSocket, this);
				t.start();
			}
		} finally {
			mSocket.close();
		}
	}

	public ClientToken addClient(ClientEndPoint cep) {
		synchronized (TOKEN_TO_CLIENT_MAP) {
			synchronized (CLIENT_TO_TOKEN_MAP) {
				if (CLIENT_TO_TOKEN_MAP.containsKey(cep)) {
					return CLIENT_TO_TOKEN_MAP.get(cep);
				} else {
					ClientToken tok = getUnusedToken();
					TOKEN_TO_CLIENT_MAP.put(tok, cep);
					CLIENT_TO_TOKEN_MAP.put(cep, tok);
					return tok;
				}
			}
		}
	}

	private ClientToken getUnusedToken() {
		Set<ClientToken> tokens = TOKEN_TO_CLIENT_MAP.keySet();
		ClientToken candidate = getRandomToken();
		while (tokens.contains(candidate)) {
			candidate = getRandomToken();
		}
		return candidate;
	}
	
	
	private ClientToken getRandomToken() {
		return new ClientToken(rand.nextInt(Constants.MAX_TOKEN));
	}

	public static void main(String[] args) {
		try {
			new Server(Constants.DEFAULT_PORT).serve();
		} catch (Exception e) {
			LOGGER.error("Fatal: Caught exception in main", e);
		}
		LOGGER.error("Server shutting down");
	}

}

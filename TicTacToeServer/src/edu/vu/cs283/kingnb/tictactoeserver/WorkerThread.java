package edu.vu.cs283.kingnb.tictactoeserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerThread extends Thread {

	private static final Random RAND = new Random();
	private static final Logger LOGGER = LoggerFactory
			.getLogger("WorkerThread");
	private static final int ACK_TIMEOUT = 60;
	private static final Map<String, ScheduledExecutorService> PENDING_RESPONSE_MAP = Collections
			.synchronizedMap(new HashMap<String, ScheduledExecutorService>());

	protected DatagramPacket mPacket;
	protected DatagramSocket mSocket;
	protected TicTacToeServer mServer;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket,
			TicTacToeServer t) {
		mPacket = packet;
		mSocket = socket;
		mServer = t;
	}

	@Override
	public void run() {
		String payload = new String(mPacket.getData(), 0, mPacket.getLength())
				.trim();
		LOGGER.debug("Parsing payload: {}", payload);

		if (payload.startsWith("connect")) {
			onConnect();
		} else if (payload.startsWith("ack_id")) {
			onArbitraryMessage(payload);
		} else if (payload.startsWith("received")) {
			String[] arr = payload.split("=");
			onAckReceived("ack_id=" + arr[1]);
		} else {
			LOGGER.debug("Ignoring {}", payload);
		}
	}

	private void onConnect() {
		int id = RAND.nextInt() % 1000;
		ClientEndPoint client = new ClientEndPoint(mPacket.getAddress(),
				mPacket.getPort());
		if (!mServer.hasClient(client)) {
			mServer.addClient(id, client);
			LOGGER.info("Accepted connect request from client {}:{}. Id={}",
					client.getAddress(), client.getPort(), id);
			send("id=" + id);
		} else {
			// ignore connect request
			LOGGER.info("Got a duplicate connect request from {}:{}",
					client.getAddress(), client.getPort());
		}
	}

	private void onArbitraryMessage(String payload) {
		String[] arr = payload.split(",");
		String ackId = arr[0].split("=")[1];
		String clientId = arr[1].split("=")[1];
		int secondCommaIx = payload.indexOf(',', payload.indexOf(',') + 1);
		String message = payload.substring(secondCommaIx + 1);

		// Acknowledge the message
		sendReply("received=" + ackId, mPacket.getAddress(), mPacket.getPort());

		Integer opponentId = mServer.getOpponentOf(Integer.parseInt(clientId));
		ClientEndPoint opponent = mServer.getClientById(opponentId);
		// Forward the message to the opponent
		if (opponentId != null && opponent != null) {
			LOGGER.debug("Forwarding message \n{}\nto {}:{}", message,
					opponent.getAddress(), opponent.getPort());
			send(message, opponent.getAddress(), opponent.getPort());
		}
	}

	public void send(String payload) {
		send(payload, mPacket.getAddress(), mPacket.getPort(), generateAck());
	}

	public void send(String payload, InetAddress address, int port) {
		send(payload, address, port, generateAck());
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	// every 5 seconds until an ack is received
	public void send(String pload, final InetAddress address, final int port,
			String ack) {
		if (!pload.endsWith("\n")) {
			pload = pload + "\n";
		}
		while (WorkerThread.PENDING_RESPONSE_MAP.containsKey(ack)) {
			ack = generateAck();
		}
		// Append the ACK to the beginning of the payload
		final String payload = ack + "," + pload;
		final ScheduledExecutorService sx = Executors
				.newSingleThreadScheduledExecutor();
		Runnable sendRunnable = new Runnable() {
			public void run() {
				DatagramPacket txPacket = new DatagramPacket(
						payload.getBytes(), payload.length(), address, port);
				try {
					WorkerThread parent = WorkerThread.this;
					if (!parent.mSocket.isClosed()) {
						LOGGER.debug("Sending {} to {}:{}", payload, address,
								port);
						parent.mSocket.send(txPacket);
					} else {
						LOGGER.error(
								"Could not send {} because socket was closed",
								payload);
					}
				} catch (IOException e) {
					LOGGER.error("", e);
					// Shut down the executor so we don't keep sending to a
					// bad connection
					sx.shutdown();
				}
			}
		};
		sx.scheduleAtFixedRate(sendRunnable, 0, ACK_TIMEOUT, TimeUnit.SECONDS);
		WorkerThread.PENDING_RESPONSE_MAP.put(ack, sx);
	}

	public void sendReply(String payload, InetAddress address, int port) {
		if (!payload.endsWith("\n")) {
			payload = payload + "\n";
		}
		DatagramPacket packet = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		try {
			mSocket.send(packet);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	private String generateAck() {
		synchronized (RAND) {
			return "ack_id=" + RAND.nextInt() % 10000;
		}
	}

	private void onAckReceived(String ack) {
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

}
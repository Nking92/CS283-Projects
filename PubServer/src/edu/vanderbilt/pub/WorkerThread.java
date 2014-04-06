package edu.vanderbilt.pub;

import static edu.vanderbilt.pub.Constants.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

public class WorkerThread extends Thread {

	private static final Random RAND = new Random();
	private static final Logger LOGGER = LoggerFactory
			.getLogger("WorkerThread");
	private static final Map<Acknowledgment, ScheduledExecutorService> PENDING_RESPONSE_MAP = Collections
			.synchronizedMap(new HashMap<Acknowledgment, ScheduledExecutorService>());

	protected DatagramPacket mPacket;
	protected DatagramSocket mSocket;
	protected Server mServer;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket, Server t) {
		mPacket = packet;
		mSocket = socket;
		mServer = t;
	}

	@Override
	public void run() {
		byte[] data = mPacket.getData();
		LOGGER.debug("Received packet {}",
				Utils.packetToHex(data, mPacket.getLength()));

		byte type = data[PACKET_TYPE];
		switch (type) {
		case ORDER_NUM_RECEIVED:
			onOrderReceived(data);
			break;
		case CLIENT_CONNECT_REQUEST:
			onClientConnect(data);
			break;
		case CLIENT_SUBSCRIBE:
			onClientSubscribe(data);
			break;
		case PING:
			onPingReceived(data);
			break;
		case ACK:
			onAckReceived(data);
		default:
			LOGGER.error("Got packet with invalid header {}", type);
		}
	}

	private void onOrderReceived(byte[] data) {
		int orderNum = Ints.fromBytes(data[ORDER_NUM_START],
				data[ORDER_NUM_START + 1], data[ORDER_NUM_START + 2],
				data[ORDER_NUM_START + 3]);
		postOrder(orderNum);
		ack(data);
	}

	private void onClientConnect(byte[] data) {
		// If this is a duplicate connect request, the server will give us its
		// current token for the client
		ClientToken token = mServer.addClient(new ClientEndPoint(mPacket
				.getAddress(), mPacket.getPort()));
		byte[] response = new byte[MAX_PACKET_SIZE];
		response[0] = CONNECT_ACKNOWLEDMENT;

		// Write the token bytes
		byte[] tokenBytes = Ints.toByteArray(token.getToken());
		for (int i = 0; i < 4; i++) {
			// Client token starts at byte index 6
			response[CLIENT_TOKEN_START + i] = tokenBytes[i];
		}

		send(response);
	}

	private void onClientSubscribe(byte[] data) {
		ClientToken token = tokenFromData(data);
		int orderNum = orderNumFromData(data);
		mServer.subscribe(token, orderNum);
		ack(data);
	}

	private void onAckReceived(byte[] data) {
		Acknowledgment ack = new Acknowledgment(Ints.fromBytes(data[ACK_START],
				data[ACK_START + 1], data[ACK_START + 2], data[ACK_START + 3]));
		ScheduledExecutorService sx = PENDING_RESPONSE_MAP.remove(ack);
		if (sx != null) {
			sx.shutdownNow();
			try {
				sx.awaitTermination(ACK_TIMEOUT + 1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			LOGGER.warn("Got unknown ack: {}", ack.getValue());
		}
	}

	private void onPingReceived(byte[] data) {
		mServer.updateEndpoint(tokenFromData(data),
				new ClientEndPoint(mPacket.getAddress(), mPacket.getPort()));
		ack(data);
	}

	private static ClientToken tokenFromData(byte[] data) {
		return new ClientToken(Ints.fromBytes(data[CLIENT_TOKEN_START],
				data[CLIENT_TOKEN_START + 1], data[CLIENT_TOKEN_START + 2],
				data[CLIENT_TOKEN_START + 3]));
	}

	private static int orderNumFromData(byte[] data) {
		return Ints.fromBytes(data[ORDER_NUM_START], data[ORDER_NUM_START + 1],
				data[ORDER_NUM_START + 2], data[ORDER_NUM_START + 3]);
	}

	private Acknowledgment generateUniqueAck() {
		Set<Acknowledgment> ackSet = PENDING_RESPONSE_MAP.keySet();
		Acknowledgment ack;
		synchronized (PENDING_RESPONSE_MAP) {
			ack = new Acknowledgment(RAND.nextInt());
			while (ackSet.contains(ack)) {
				ack = new Acknowledgment(RAND.nextInt());
			}
		}
		return ack;
	}

	private void send(final byte[] data) {
		final ScheduledExecutorService sx = Executors
				.newSingleThreadScheduledExecutor();

		synchronized (PENDING_RESPONSE_MAP) {
			Acknowledgment ack = generateUniqueAck();
			byte[] ackBytes = Ints.toByteArray(ack.getValue());
			for (int i = 0; i < 4; i++) {
				data[ACK_START + i] = ackBytes[i];
			}
			send(data, ack, mPacket.getAddress(), mPacket.getPort());
			WorkerThread.PENDING_RESPONSE_MAP.put(ack, sx);
		}

		Runnable sendRunnable = new Runnable() {
			public void run() {
				InetAddress address = mPacket.getAddress();
				int port = mPacket.getPort();
				DatagramPacket txPacket = new DatagramPacket(data, data.length,
						address, port);
				try {
					WorkerThread parent = WorkerThread.this;
					if (!parent.mSocket.isClosed()) {
						LOGGER.debug("Sending packet to {}:{}", address, port);
						parent.mSocket.send(txPacket);
					} else {
						LOGGER.error("Could not send packet because socket was closed");
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
	}

	private void ack(byte[] data) {
		byte[] ackData = new byte[MAX_PACKET_SIZE];
		ackData[0] = ACK;
		for (int i = 0; i < 4; i++) {
			ackData[ACK_START + i] = data[ACK_START + i];
		}
		DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length,
				mPacket.getAddress(), mPacket.getPort());
		try {
			mSocket.send(ackPacket);
		} catch (IOException e) {
			LOGGER.error("Exception while sending acknowledgment", e);
		}
	}

	// Send a UDP packet to the specified remote endpoint
	// every 5 seconds until an ack is received
	private void send(final byte[] data, final Acknowledgment ack,
			final InetAddress address, final int port) {

	}

}
package edu.vu.cs283.kingnb.tictactoeserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicTacToeServer {

	public static final int DEFAULT_PORT = 20000;
	public static final int MAX_PACKET_SIZE = 512;
	
	private static final Logger LOGGER = LoggerFactory.getLogger("TicTacToeServer");

	private final DatagramSocket mSocket, mMatchSocket1, mMatchSocket2;
	private final List<WorkerThread> mThreads = new LinkedList<WorkerThread>();
	private final Map<Integer, Integer> mOpponents = Collections
			.synchronizedMap(new HashMap<Integer, Integer>());
	private final Map<Integer, ClientEndPoint> mIdMap = Collections
			.synchronizedMap(new HashMap<Integer, ClientEndPoint>());
	private Integer mUnmatchedId;

	private TicTacToeServer(int port) throws SocketException {
		mSocket = new DatagramSocket(port);
		mMatchSocket1 = new DatagramSocket(port - 1);
		mMatchSocket2 = new DatagramSocket(port - 2);
	}

	private void serve() {
		try {
			while (true) {
				// create an empty UDP packet
				byte[] buf = new byte[MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				mSocket.receive(packet);
				LOGGER.debug("Accepted packet from {}:{}", packet.getAddress(), packet.getPort());
				WorkerThread t = new WorkerThread(packet, mSocket, this);
				mThreads.add(t);
				t.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void addClient(Integer id, ClientEndPoint client) {
		mIdMap.put(id, client);
		if (mUnmatchedId == null) {
			mUnmatchedId = id;
		} else {
			LOGGER.info("Matched client id={} to id={}", id, mUnmatchedId);
			mOpponents.put(id, mUnmatchedId);
			mOpponents.put(mUnmatchedId, id);
			// Notify the clients that they have a match
			notifyMatch(mIdMap.get(id), mSocket);
			notifyMatch(mIdMap.get(mUnmatchedId), mSocket);
			mUnmatchedId = null;
		}
	}

	private void notifyMatch(ClientEndPoint client, DatagramSocket socket) {
		byte[] buf = new byte[MAX_PACKET_SIZE];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		WorkerThread t = new MatchFoundThread(p, socket, this, client);
		mThreads.add(t);
		t.start();
	}

	public Integer getOpponentOf(Integer clientId) {
		return mOpponents.get(clientId);
	}

	public ClientEndPoint getClientById(Integer clientId) {
		return mIdMap.get(clientId);
	}
	
	public boolean hasClient(ClientEndPoint client) {
		return mIdMap.containsKey(client);
	}

	public static void main(String[] args) throws SocketException {
		TicTacToeServer server = new TicTacToeServer(DEFAULT_PORT);
		server.serve();
	}
}

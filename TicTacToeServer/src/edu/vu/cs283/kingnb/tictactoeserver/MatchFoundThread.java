package edu.vu.cs283.kingnb.tictactoeserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MatchFoundThread extends WorkerThread {
	
	private ClientEndPoint mClient;

	public MatchFoundThread(DatagramPacket packet, DatagramSocket socket,
			TicTacToeServer t, ClientEndPoint client) {
		super(packet, socket, t);
		mClient = client;
	}

	@Override
	public void run() {
		send("opponent_found", mClient.getAddress(), mClient.getPort());
	}
}

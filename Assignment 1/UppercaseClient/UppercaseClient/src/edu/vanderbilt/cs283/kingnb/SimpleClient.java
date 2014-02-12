package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SimpleClient implements Client {

	private Socket mSocket;

	public SimpleClient(InetAddress address, int port) throws IOException {
		mSocket = new Socket(address, port);
	}

	@Override
	public void request() throws IOException {
		try {
			PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					mSocket.getInputStream()));
			writer.println("hello server");
			String line = reader.readLine();
			System.out.println("Server response: " + line);
		} finally { }
	}
}

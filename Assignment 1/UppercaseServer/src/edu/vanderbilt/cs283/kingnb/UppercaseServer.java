package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class UppercaseServer implements Server {

	protected void echoUppercase(Socket socket) {
		BufferedReader reader = null;
		PrintWriter writer = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String input = reader.readLine();
			System.out.println("Received input: " + input);
			String outStr = input.toUpperCase();
			System.out.println("Sending output: " + outStr);
			writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(outStr);
		} catch (IOException e) {
			System.err.println("Something went wrong while sending a response");
			e.printStackTrace();
		} finally {
			Helpers.closeIfNotNull(socket, reader, writer);
		}
	}

}

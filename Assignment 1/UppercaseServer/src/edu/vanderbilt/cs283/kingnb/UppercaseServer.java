
package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public abstract class UppercaseServer implements Server {

    protected void echoUppercase(Socket socket) {
        BufferedReader reader = null;
        PrintStream writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            while ((input = reader.readLine()) != null) {
                String outStr = input.toUpperCase();
                writer = new PrintStream(socket.getOutputStream());
                writer.println(outStr);
            }
            socket.shutdownOutput();
        } catch (IOException e) {
            System.err.println("Something went wrong while sending a response");
            e.printStackTrace();
        } finally {
            Helpers.closeIfNotNull(reader, writer, socket);
        }
    }

}

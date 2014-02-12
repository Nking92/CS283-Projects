package edu.vanderbilt.cs283.kingnb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    
    private ServerSocket mSocket;

    SimpleServer(int port) throws IOException {
        mSocket = new ServerSocket(port);
        while (true) {
            Socket socket = mSocket.accept();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            StringBuilder output = new StringBuilder();
            while ((input = rdr.readLine()) != null) {
                System.out.println("Received input: " + input);
                if (output.length() > 0) output.append("\n");
                output.append(input.toUpperCase());
            }
            String outStr = output.toString();
            System.out.println("Sending output: " + outStr);
        }
    }
    
    

}

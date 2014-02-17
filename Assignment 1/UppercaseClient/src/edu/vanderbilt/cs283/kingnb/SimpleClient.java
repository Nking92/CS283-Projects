
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
    public void request() {
        try {
            PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    mSocket.getInputStream()));
            writer.println("hello server");
            mSocket.shutdownOutput();
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Server response: " + line);
            }
        } catch (IOException e) {
            System.out.println("Something went wrong while sending a request");
            e.printStackTrace();
        }
    }
    
    @Override
    public void shutdown() {
        if (!mSocket.isClosed()) {
            Helpers.closeIfNotNull(mSocket);
        }
    }
}

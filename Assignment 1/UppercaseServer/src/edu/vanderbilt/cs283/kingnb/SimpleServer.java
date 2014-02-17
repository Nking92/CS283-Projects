
package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer extends UppercaseServer {

    private ServerSocket mSocket;

    SimpleServer(int port) throws IOException {
        mSocket = new ServerSocket(port);
    }

    @Override
    public void serve() {
        while (true) {
            Socket socket = null;
            try {
                socket = mSocket.accept();
                // socket will be closed by echoUppercase
                echoUppercase(socket);
            } catch (IOException e) {
                System.err.println("Error accepting socket connection");
                e.printStackTrace();
            }
        }
    }

}

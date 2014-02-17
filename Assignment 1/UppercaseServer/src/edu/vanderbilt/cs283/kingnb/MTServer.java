
package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MTServer extends UppercaseServer {

    private ServerSocket mServerSocket;

    public MTServer(int port) throws IOException {
        mServerSocket = new ServerSocket(port);
    }

    @Override
    public void serve() {
        while (true) {
            try {
                Socket socket = mServerSocket.accept();
                new ServerThread(socket, this).start();
            } catch (IOException e) {
                System.err.println("Error accepting Socket connection");
                e.printStackTrace();
            }
        }
    }

    private static class ServerThread extends Thread {
        Socket mSocket;

        UppercaseServer mServer;

        ServerThread(Socket socket, UppercaseServer server) {
            mSocket = socket;
            mServer = server;
        }

        @Override
        public void run() {
            mServer.echoUppercase(mSocket);
        }
    }

}

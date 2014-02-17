package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    
    private static final int PORT = 8000;

    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        Server server;
        System.out.print("Multithreaded server? (y/n) ");
        if (input.next().toLowerCase().startsWith("y")) {
        	System.out.println("Starting multithreaded server on port " + PORT);
        	server = new MTServer(PORT);
        } else {
        	System.out.println("Starting single threaded server on port " + PORT);
        	server = new SimpleServer(PORT);
        }
        server.serve();
    }

}

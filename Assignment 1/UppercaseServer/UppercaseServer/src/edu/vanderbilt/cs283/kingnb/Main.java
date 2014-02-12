package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        Server server;
        System.out.print("Multithreaded server? (y/n) ");
        if (input.next().toLowerCase().startsWith("y")) {
        	System.out.println("Starting multithreaded server on port 8000");
        	server = new MTServer(8000);
        } else {
        	System.out.println("Starting single threaded server on port 8000");
        	server = new SimpleServer(8000);
        }
        server.serve();
    }

}

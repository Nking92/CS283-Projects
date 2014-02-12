package edu.vanderbilt.cs283.kingnb;
import java.net.InetAddress;
import java.util.Scanner;

public class Main {
	
	public static void main(String[] args) throws Throwable {
		System.out.print("Benchmark? (y/n) ");
		Scanner input = new Scanner(System.in);
		Client client;
		if (input.next().toLowerCase().equals("y")) {
			System.out.println("Starting BenchmarkClient");
			client = new BenchmarkClient(InetAddress.getLocalHost(), Client.DEFAULT_PORT);
		} else {
			System.out.println("Starting SimpleClient");
			client = new SimpleClient(InetAddress.getLocalHost(), Client.DEFAULT_PORT);
		}
		client.request();
	}
	
}

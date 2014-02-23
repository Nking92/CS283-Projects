package bananabank.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class BasicBananaBankClient {

	public static void main(String[] args) {
		try {
			Socket socket = new Socket(BananaBankBenchmark.SERVER_ADDRESS,
					BananaBankBenchmark.PORT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			PrintStream ps = new PrintStream(socket.getOutputStream());
			ps.println("1 11111 22222");
			ps.println("5 11111 55555");
			ps.println("50 44444 88888");
			ps.println("25 33333 77777");
			
			// Some edge conditions
			// Unknown account
			ps.println("20 11111 99");
			// Nonsense string
			ps.println("9 93 2 1 3 4 02jg");
			
			ps.println("SHUTDOWN");
			
			ps.flush();
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

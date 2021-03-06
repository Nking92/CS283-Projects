package bananabank.server;

import java.io.Closeable;
import java.io.IOException;

public class Helpers {
	
	public static void closeIfNotNull(Closeable... closables) {
		for(Closeable c : closables) {
			closeIfNotNull(c);
		}
	}

	public static void closeIfNotNull(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException e) {
			System.err.println("Error while closing resource");
			e.printStackTrace();
		}
	}
	
	// Reflective implementations allow us to close Sockets
	public static void closeIfNotNull(Object... objects) {
		for (Object o : objects) {
			closeIfNotNull(o);
		}
	}
	
	public static void closeIfNotNull(Object object) {
		if (object == null) return;
		try {
			object.getClass().getMethod("close", (Class<?>[])null)
					.invoke(object, (Object[])null);
		} catch (Exception e) {
			System.err.println("Error while closing resource");
			e.printStackTrace();
		}
	}

}

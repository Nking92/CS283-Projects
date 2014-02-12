package edu.vanderbilt.cs283.kingnb;

import java.io.IOException;

public interface Client {
	
	int DEFAULT_PORT = 8000;

	public void request() throws IOException;

}

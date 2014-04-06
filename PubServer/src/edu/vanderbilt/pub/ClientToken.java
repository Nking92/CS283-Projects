package edu.vanderbilt.pub;

public class ClientToken {
	
	private int token;
	private long timestamp;
	
	public ClientToken(int tok) {
		token = tok;
		timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public int getToken() {
		return token;
	}
	
	// Note: Two ClientTokens are equal if and only if their token values are equal.
	// It is up to the client to ensure that no two ClientTokens with the same
	// token value but different timestamps are around at the same time.
	
	@Override
	public int hashCode() {
		return token;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ClientToken)) {
			return false;
		}
		ClientToken other = (ClientToken) o;
		return token == other.token;
	}

}

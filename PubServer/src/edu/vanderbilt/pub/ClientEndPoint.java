package edu.vanderbilt.pub;

import java.net.InetAddress;

public class ClientEndPoint {
	protected final InetAddress address;
	protected final int port;
	
	public ClientEndPoint(InetAddress addr, int port) {
		this.address = addr;
		this.port = port;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}

	@Override
	public int hashCode() {
		// the hashcode is the exclusive or (XOR) of the port number and the hashcode of the address object
		return this.port ^ this.address.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ClientEndPoint)) {
			return false;
		}
		ClientEndPoint other = (ClientEndPoint) o;
		return address.equals(other.address) && port == other.port;
	}
	
	
}

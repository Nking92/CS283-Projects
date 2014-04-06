package edu.vanderbilt.pub;

public class Utils {
	
	// This code was modified from an answer at
	// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String packetToHex(byte[] bytes, int packetLength) {
	    char[] hexChars = new char[packetLength * 2];
	    for ( int j = 0; j < packetLength; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

}

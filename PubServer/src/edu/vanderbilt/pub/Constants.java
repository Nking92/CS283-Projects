package edu.vanderbilt.pub;

public interface Constants {

	int DEFAULT_PORT = 20050;
	int MAX_PACKET_SIZE = 512;
	int ACK_TIMEOUT = 10;
	// Max time, in milliseconds, that we keep the client connected before
	// releasing its token
	int CLIENT_TIME_TO_LIVE = 5 /* hours */ * 60  /* minutes/hr */ * 60 /* seconds/min */
			                  * 1000 /* ms / sec */;
	// Max valid token
	int MAX_TOKEN = 65535;
	
	// Client is subscribing to all orders
	int ALL_ORDERS_SENTINEL = 65535;

	// PACKET HEADER CONSTANTS
	byte ORDER_NUM_RECEIVED = 3;
	byte CLIENT_CONNECT_REQUEST = 7;
	byte CONNECT_ACKNOWLEDMENT = 11;
	byte CLIENT_SUBSCRIBE = 13;
	byte PING = 17;
	byte ACK = 29;
	
	// PACKET LOCATION CONSTANTS
	int PACKET_TYPE = 0;
	int ACK_START = 1;
	int CLIENT_TOKEN_START = 6;
	int ORDER_NUM_START = 10;
}

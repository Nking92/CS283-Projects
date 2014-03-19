package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import udpgroupchat.data.Message;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;
	private static final Random RAND = new Random();
	private static final int ACK_TIMEOUT = 5;
	private static final Map<String, ScheduledExecutorService> pendingResponseMap = Collections
			.synchronizedMap(new HashMap<String, ScheduledExecutorService>());

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();

		// dispatch request handler functions based on the payload's prefix

		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("UNREGISTER")) {
			onUnregisterRequested(payload);
			return;
		}

		if (payload.startsWith("SEND")) {
			onSendRequested(payload);
			return;
		}

		if (payload.startsWith("SETNAME")) {
			onSetNameRequested(payload);
			return;
		}

		if (payload.startsWith("POLL")) {
			onPollRequested(payload);
			return;
		}
		
		if (payload.startsWith("LIST")) {
			onListRequested(payload);
			return;
		}
		
		if (payload.startsWith("LEAVE")) {
			onLeaveRequested(payload);
			return;
		}

		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload);
			return;
		}

		if (payload.startsWith("ACK")) {
			onAckReceived(payload);
			return;
		}
		
		if (payload.startsWith("SHUTDOWN")) {
			onShutdownReceived(payload);
			return;
		}

		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	public void send(String payload, InetAddress address, int port) {
		send(payload, address, port, generateAck());
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	// every 5 seconds until an ack is received
	public void send(final String pload, final InetAddress address,
			final int port, String ack) {
		if (!Server.isShutdown.get()) {
			while (WorkerThread.pendingResponseMap.containsKey(ack)) {
				ack = generateAck();
			}
			// Append the ACK to the beginning of the payload
			final String payload = ack + " " + pload;
			ScheduledExecutorService sx = Executors
					.newSingleThreadScheduledExecutor();
			Runnable sendRunnable = new Runnable() {
				public void run() {
					DatagramPacket txPacket = new DatagramPacket(
							payload.getBytes(), payload.length(), address, port);
					try {
						WorkerThread parent = WorkerThread.this;
						if (!parent.socket.isClosed()) {
							parent.socket.send(txPacket);
						} else {
							System.out.println("Could not send " + payload
									+ " because socket was closed");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			sx.scheduleAtFixedRate(sendRunnable, 0, ACK_TIMEOUT,
					TimeUnit.SECONDS);
			WorkerThread.pendingResponseMap.put(ack, sx);
		}
	}

	private String generateAck() {
		synchronized (RAND) {
			return "ACK" + RAND.nextInt();
		}
	}

	private void onRegisterRequested(String payload) {
		// get the address of the sender from the rxPacket
		InetAddress address = this.rxPacket.getAddress();
		// get the port of the sender from the rxPacket
		int port = this.rxPacket.getPort();

		ClientEndPoint cep = new ClientEndPoint(address, port);
		if (Server.clientMap.values().contains(cep)) {
			send("REGISTER_FAIL: " + address.getHostAddress() + ":" + port
					+ " ALREADY REGISTERED\n", rxPacket.getAddress(),
					rxPacket.getPort());
			return;
		}

		// create a client object, and put it in the map that assigns integers
		// to client objects
		int clientId;
		synchronized (RAND) {
			clientId = RAND.nextInt();
			while (Server.clientMap.keySet().contains(clientId)) {
				clientId = RAND.nextInt();
			}
		}

		Server.clientMap.put(clientId, new ClientEndPoint(address, port));

		// Acknowledge the client and append the assigned ID
		String response = "REGISTER_SUCCESS " + clientId;

		send(response + "\n", this.rxPacket.getAddress(),
				this.rxPacket.getPort());
	}

	private void onUnregisterRequested(String payload) {
		Integer clientId = extractClientId(payload);

		// check if client is in the set of registered clientEndPoints
		if (Server.clientMap.containsKey(clientId)) {
			// yes, remove it
			Server.clientMap.remove(clientId);
			Server.clientNameMap.remove(clientId);
			Server.clientMsgMap.remove(clientId);
			for (List<Integer> list : Server.clientGroupMap.values()) {
				list.remove(clientId);
			}
			send("UNREGISTERED\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} else {
			// no, send back a message
			send("CLIENT NOT REGISTERED\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		}
	}

	private void onSendRequested(String payload) {
		int clientId = extractClientId(payload);
		String trimmed = trimHeader(payload);
		String group = trimmed.substring(0, trimmed.indexOf(":"));
		synchronized (Server.clientGroupMap) {
			List<Integer> subs = Server.clientGroupMap.get(group);
			if (subs != null && subs.contains(clientId)) {
				String msgStr = trimmed.substring(trimmed.indexOf(":") + 1)
						.trim();
				Message message = new Message(msgStr,
						Server.clientNameMap.get(clientId), group);
				synchronized (Server.clientMsgMap) {
					for (Integer sub : subs) {
						List<Message> msgs = Server.clientMsgMap.get(sub);
						if (msgs == null) {
							msgs = new ArrayList<Message>();
							Server.clientMsgMap.put(sub, msgs);
						}
						msgs.add(message);
					}
				}
				send("SEND_SUCCESS\n", rxPacket.getAddress(),
						rxPacket.getPort());
			} else {
				send("SEND_FAIL\n", rxPacket.getAddress(), rxPacket.getPort());
			}
		}
	}
	

	private void onBadRequest(String payload) {
		send("BAD REQUEST\n", this.rxPacket.getAddress(),
				this.rxPacket.getPort());
	}

	private void onSetNameRequested(String payload) {
		String name = trimHeader(payload);
		int clientId = extractClientId(payload);
		Server.clientNameMap.put(clientId, name);
		send("SETNAME_SUCCESS " + name + "\n", rxPacket.getAddress(),
				rxPacket.getPort());
	}

	private void onAckReceived(String ack) {
		ScheduledExecutorService sx = pendingResponseMap.remove(ack);
		if (sx != null) {
			sx.shutdownNow();
			try {
				sx.awaitTermination(ACK_TIMEOUT + 1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			send("UNKNOWN ACK: " + ack + "\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		}
		WorkerThread.pendingResponseMap.remove(ack);
	}

	private void onPollRequested(String payload) {
		int clientId = extractClientId(payload);
		List<Message> msgs = Server.clientMsgMap.get(clientId);
		if (msgs != null) {
			synchronized (msgs) {
				for (int i = 0; i < msgs.size(); i++) {
					Message msg = msgs.get(i);
					String str = "MSG FROM " + msg.getFrom() + " TO "
							+ msg.getGroup() + ": " + msg.getMessage() + "\n";
					send(str, rxPacket.getAddress(), rxPacket.getPort());
					msgs.remove(i);
				}
			}
		}
	}

	private void onJoinRequested(String payload) {
		int clientId = extractClientId(payload);
		String groupName = trimHeader(payload);
		synchronized (Server.clientGroupMap) {
			List<Integer> subscribers = Server.clientGroupMap.get(groupName);
			if (subscribers == null) {
				subscribers = new LinkedList<Integer>();
				Server.clientGroupMap.put(groupName, subscribers);
			}
			subscribers.add(clientId);
		}
		send("JOIN_SUCCESS " + groupName + "\n", rxPacket.getAddress(),
				rxPacket.getPort());
	}
	
	private void onLeaveRequested(String payload) {
		int clientId = extractClientId(payload);
		String groupName = trimHeader(payload);
		boolean success;
		synchronized (Server.clientGroupMap) {
			List<Integer> list = Server.clientGroupMap.get(groupName);
			if (list != null) {
				success = list.remove(Integer.valueOf(clientId));
			} else {
				success = false;
			}
		}
		String msg = "LEAVE_" + (success ? "SUCCESS" : "FAIL") + "\n";
		send(msg, rxPacket.getAddress(), rxPacket.getPort());
	}

	private void onListRequested(String payload) {
		StringBuilder bldr = new StringBuilder("LIST_SUCCESS LISTS: ");
		String regex = payload.substring(5); // 5 is the index after LIST 
		synchronized (Server.clientGroupMap) {
			Set<String> groups = Server.clientGroupMap.keySet();
			if (groups.isEmpty()) {
				bldr.append("<NONE>");
			} else {
				for (String group : Server.clientGroupMap.keySet()) {
					if (group.matches(regex)) {
						bldr.append(group).append(", ");
					}
				}
				// Delete the trailing comma
				bldr.delete(bldr.length()-2, bldr.length());
			}
		}
		bldr.append("\n");
		send(bldr.toString(), rxPacket.getAddress(), rxPacket.getPort());
	}

	private void onShutdownReceived(String payload) {
		if (isLocalAddress(rxPacket.getAddress().getHostAddress())) {
			Server.isShutdown.set(true);
			for (WorkerThread wt : Server.threads) {
				if (Thread.currentThread().getId() != wt.getId()) {
					try {
						wt.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			byte[] bytes = "SHUTDOWN_SUCCESS\n".getBytes();
			try {
				socket.send(new DatagramPacket(bytes, bytes.length, rxPacket
						.getAddress(), rxPacket.getPort()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (ScheduledExecutorService ex : pendingResponseMap.values()) {
				// Tell the executors to shut down
				ex.shutdownNow();
			}
			// Sleep for 6 seconds so that all executors will have time to shut
			// down
			try {
				sleep((ACK_TIMEOUT + 1) * 1000 /* millis */);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Server.socket.close();
		} else {
			String msg = "SHUTDOWN_UNAUTHORIZED FROM "
					+ rxPacket.getAddress().getHostAddress() + "\n";
			send(msg, rxPacket.getAddress(), rxPacket.getPort());
		}
	}

	private static String trimHeader(String payload) {
		int ix = payload.indexOf(" ", payload.indexOf(" ") + 1);
		return payload.substring(ix, payload.length()).trim();
	}

	private static int extractClientId(String payload) {
		Scanner tokenizer = new Scanner(payload);
		tokenizer.next();
		return tokenizer.nextInt();
	}

	private static boolean isLocalAddress(String addr) {
		if (addr.equals("127.0.0.1")) {
			return true;
		} else {
			// IPv6
			return addr.startsWith("0:0:0:0:0:0:0:1");
		}
	}

}

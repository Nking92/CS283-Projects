package udpgroupchat.data;

public class Message {
	
	private final String mMessage, mFrom, mGroup;
	
	public Message(String message, String from, String toGroup) {
		mMessage = message;
		mFrom = from == null ? "Unknown" : from;
		mGroup = toGroup;
	}
	
	public String getMessage() {
		return mMessage;
	}
	
	public String getFrom() {
		return mFrom;
	}
	
	public String getGroup() {
		return mGroup;
	}

}

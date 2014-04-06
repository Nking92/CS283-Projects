package edu.vanderbilt.pub;

public class Acknowledgment {
	
	private int mValue;
	private long mTimestamp;
	
	public Acknowledgment(int val) {
		mValue = val;
		mTimestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return mTimestamp;
	}
	
	public int getValue() {
		return mValue;
	}
	
	// Note: Two Acknowledgments are equal if and only if their values are equal.
	// It is up to the client to ensure that no two Acknowledgments with the same
	// value but different timestamps are around at the same time.
	
	@Override
	public int hashCode() {
		return mValue;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Acknowledgment)) {
			return false;
		}
		Acknowledgment other = (Acknowledgment) o;
		return mValue == other.mValue;
	}

}
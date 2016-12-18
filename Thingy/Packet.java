package Thingy;
import java.io.Serializable;

@SuppressWarnings("serial")
public class Packet implements Serializable {
	private boolean mbAck;
	private int mSeq;
	
	public Packet(boolean bAck, int nSeq) {
		mbAck = bAck;
		mSeq = nSeq;
	}
	
	public boolean isAck() {
		return mbAck;
	}
	public int getSeq() {
		return mSeq;
	}
	
} // class

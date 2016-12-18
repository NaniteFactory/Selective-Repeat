package Thingy;

// 애니메이션의 날아가는 패킷
public class Piece {
	private int mY; // relative position
	private boolean mbAck;
	private boolean mbVisible;
	
	public Piece(int y, boolean isAck, boolean visible) {
		mY = y;
		mbAck = isAck;
		mbVisible = visible;
	} // func
	
	public int getY() { return mY; }
	public void setY(int y) { mY = y; }
	public boolean isAck() { return mbAck; }
	public void setAck(boolean isAck) { mbAck = isAck; }
	public boolean isVisible() { return mbVisible; }
	public void setVisible(boolean visible) { mbVisible = visible; }
	
} // public class

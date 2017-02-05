package MainWindow;
import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JFrame;

import Connection.ConnectionThread;
import Connection.Receiver;
import Connection.Sender;
import Thingy.BufferSlot;
import Thingy.Piece;

// SR = mUi 에서 모든 데이터가 관리되며 그리는 처리 수행
// 송신측 sender와 수신측 receiver의 매개 (양자는 상대의 버퍼를 서로 볼 수 없으므로 필요함)
@SuppressWarnings("serial")
public class SR extends JFrame {
	// var
	private final static int SIZE_BUFFER = 20;
	private BufferSlot[] mBufferSender = new BufferSlot[SIZE_BUFFER];	
	private BufferSlot[] mBufferReceiver = new BufferSlot[SIZE_BUFFER];
	private Piece[] mFlying = new Piece[SIZE_BUFFER];
	//
	private final static int SIZE_WINDOW = 5; // 양측
	private int mBaseSnd = 0; // 송신측의 base
	private int mNextSeqNum = 0; // 송신측의 nextseqnum
	private int mBaseRcv = 0; // 수신측의 base
	//
	private ConnectionThread mSender;
	private ConnectionThread mReciever;

	// init
	public SR() throws SocketException, UnknownHostException {
		for(int i = 0; i < mBufferSender.length; ++i) { mBufferSender[i] = new BufferSlot(); }
		for(int i = 0; i < mBufferReceiver.length; ++i) { mBufferReceiver[i] = new BufferSlot(); }
		for(int i = 0; i < mFlying.length; ++i) { mFlying[i] = new Piece(0, false, false); }
		mSender = new Sender(this);
		mReciever = new Receiver(this);
		mSender.start();
		mReciever.start();
		//
		setLayout(null);
		initBtnSnd();
		setSize(1024, 600);
		setBackground(Color.WHITE);
		setTitle("SelectiveRepeat");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	} // func
	
	private void initBtnSnd() {
		final int x = OFFSET_X_SENDER_SLOT + ((MARGIN_SLOT + WIDTH_SLOT) * (mBufferSender.length - 1)) + DISTANCE_TO_TEXT;
		final int y = OFFSET_Y_SENDER_SLOT + (DISTANCE_BETWEEN_BUFFERS / 2);
		Button btnSnd = new Button("SEND NEW");
		btnSnd.setBounds(x, y, 100, 50);
		btnSnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				// System.out.println("clicked");
				int seq = getSeqSndNew();
				if (seq == -1) { return; }
				try {
					mSender.send(seq);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		this.add(btnSnd);
	}
	
	/*
	private boolean isWithinWindow(int seq, int base, int sizeWindow) {
		return ((seq >= base) && (seq < base + sizeWindow));
	}
	*/
	
	// 윈도우 이동 수신측
	public void updateBaseRcv() {
		int pivot = findFirstEmptySlotFromBufferRcv();
		for(int i = 0; i < mBufferReceiver.length; ++i) {
			if(i < pivot) { 
				mBufferReceiver[i].setState(BufferSlot.RECEIVED);
				if (i < mBufferReceiver.length - SIZE_WINDOW) { mBaseRcv = i + 1; } // 윈도우의 끝
			} // if
		} // for
	} // func
	
	// kinda like findNextSequence()
	private int findFirstEmptySlotFromBufferRcv() {
		int i;
		for(i = 0; i < mBufferReceiver.length; ++i) {
			if(mBufferReceiver[i].getState() == BufferSlot.EMPTY) { return i; }
		}
		return i + 1;
	} // func

	// 윈도우 이동 송신측
	public void updateBaseSnd() {
		mBaseSnd = findBaseSnd();
	} // func
	
	private int findBaseSnd() {
		int i;
		for(i = 0; i < mBufferSender.length - SIZE_WINDOW; ++i) { // 윈도우의 끝
			if(mBufferSender[i].getState() != BufferSlot.ACKED) { return i; }
		}
		return i;
	} // func
	
	private int getSeqSndNew() {
		for(int i = mBaseSnd; i < mBaseSnd + SIZE_WINDOW; ++i) {
			if(mBufferSender[i].getState() == BufferSlot.EMPTY) {
				return i;
			} // if
		} // for
		return -1;
	} // func

	// 업데이트 송신측
	public void updateNextSequenceSnd() {
		mNextSeqNum = findNextSequenceSnd();
	} // func
	
	private int findNextSequenceSnd() {
		int i;
		for(i = 0; i < mBufferSender.length; ++i) {
			if(mBufferSender[i].getState() == BufferSlot.EMPTY) { return i; }
		}
		return i;
	} // func
	
	// const ui offsets and stuff
	public static final int OFFSET_X_SENDER_SLOT = 50;
	public static final int OFFSET_Y_SENDER_SLOT = 140;
	public static final int MARGIN_SLOT = 10;
	public static final int WIDTH_SLOT = 20;
	public static final int HEIGHT_SLOT = 60;
	public static final int DISTANCE_BETWEEN_BUFFERS = 300;
	public static final int OFFSET_X_RECEIVER_SLOT = OFFSET_X_SENDER_SLOT;
	public static final int OFFSET_Y_RECEIVER_SLOT = OFFSET_Y_SENDER_SLOT + DISTANCE_BETWEEN_BUFFERS;
	public static final int DISTANCE_TO_TEXT = 50;
	
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		// var
	    Image image = createImage(getSize().width, getSize().height);
	    Graphics graphics = image.getGraphics();
	    
	    drawBufferSender(graphics, 
	    		OFFSET_X_SENDER_SLOT, OFFSET_Y_SENDER_SLOT, DISTANCE_TO_TEXT, 
	    		MARGIN_SLOT, WIDTH_SLOT, HEIGHT_SLOT);
	    drawBufferReceiver(graphics, 
	    		OFFSET_X_RECEIVER_SLOT, OFFSET_Y_RECEIVER_SLOT, DISTANCE_TO_TEXT, 
	    		MARGIN_SLOT, WIDTH_SLOT, HEIGHT_SLOT);
	    drawWindow(graphics, mBaseSnd, 
	    		OFFSET_X_SENDER_SLOT, OFFSET_Y_SENDER_SLOT, 
	    		MARGIN_SLOT, WIDTH_SLOT, HEIGHT_SLOT);
	    drawWindow(graphics, mBaseRcv, 
	    		OFFSET_X_RECEIVER_SLOT, OFFSET_Y_RECEIVER_SLOT, 
	    		MARGIN_SLOT, WIDTH_SLOT, HEIGHT_SLOT);
	    drawFlyings(graphics, 
	    		OFFSET_X_SENDER_SLOT, OFFSET_Y_SENDER_SLOT, 
	    		MARGIN_SLOT, WIDTH_SLOT, HEIGHT_SLOT);
	    drawCredits(graphics);
	    
	    g.drawImage(image, 0, 0, this);
	} // func
	
	private void drawCredits(Graphics g) {
		g.setColor(Color.GRAY);
		g.drawString("2016월 12월 19일 / 2012154021 문동선 / 2010152003 김민수", 30, 70);
	}
	
	private void drawFlyings(Graphics g, 
			int offsetBufferSenderX, int offsetBufferSenderY, 
			int maginSlot, int widthSlot, int heightSlot) { 
		int x = 0;
    	int y = 0;
		for(int i = 0; i < mFlying.length; ++i) {
			if (!mFlying[i].isVisible()) { continue; }
			x = offsetBufferSenderX + ((maginSlot + widthSlot) * i);
			y = offsetBufferSenderY + mFlying[i].getY();
			g.setColor(Color.BLACK);
			g.draw3DRect(x, y, widthSlot, heightSlot, true);	
			if (mFlying[i].isAck()) { // 수신측에서 보내는 것은 노란색
				g.setColor(Color.YELLOW);
			    g.fill3DRect(x, y, widthSlot, heightSlot, true);
			} else if (!mFlying[i].isAck()) { // 송신측에서 보내는 것은 분홍색
				g.setColor(Color.PINK);
			    g.fill3DRect(x, y, widthSlot, heightSlot, true);
			} else {
				System.out.println("this never happens");
			} // if
		} // for
	} // func
	
	private void drawBufferSender(Graphics g, 
			int offsetBufferX, int offsetBufferY, int distToText, 
			int maginSlot, int widthSlot, int heightSlot) {
		int x = 0;
    	int y = 0;
	    g.setFont(new Font(g.getFont().getName(), Font.PLAIN, g.getFont().getSize()));
	    for(int i = 0; i < mBufferSender.length; ++i) {
	    	x = offsetBufferX + ((maginSlot + widthSlot) * i);
	    	y = offsetBufferY;
	    	g.setColor(Color.BLACK);
	    	g.drawString("" + i, x, y - 10);
		    g.draw3DRect(x, y, widthSlot, heightSlot, true);
		    switch (mBufferSender[i].getState()) {
		    case BufferSlot.SENT:
				g.setColor(Color.PINK);
			    g.fill3DRect(x, y, widthSlot, heightSlot, true);
				break;
		    case BufferSlot.ACKED:
				g.setColor(Color.RED);
			    g.fill3DRect(x, y, widthSlot, heightSlot, true);
				break;
			}
	    }
	    g.setColor(Color.MAGENTA);
	    g.setFont(new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize()));
	    g.drawString("Sender", x + distToText, y);	    	    
	    g.setColor(Color.BLACK);
	    g.setFont(new Font(g.getFont().getName(), Font.PLAIN, g.getFont().getSize()));
	    g.drawString("Window size: " + SIZE_WINDOW, x + distToText, y + 15);	
	    g.drawString("base: " + mBaseSnd, x + distToText, y + 30);	
	    g.drawString("nextseqnum: " + mNextSeqNum, x + distToText, y + 45);	
	}
	
	private void drawBufferReceiver(Graphics g, 
			int offsetBufferX, int offsetBufferY, int distToText, 
			int maginSlot, int widthSlot, int heightSlot) {
		int x = 0;
		int y = 0;
		g.setFont(new Font(g.getFont().getName(), Font.PLAIN, g.getFont().getSize()));
	    for(int i = 0; i < mBufferSender.length; ++i) {
	    	x = offsetBufferX + ((maginSlot + widthSlot) * i);
	    	y = offsetBufferY;
	    	g.setColor(Color.BLACK);
	    	g.drawString("" + i, x, y - 10);
		    g.draw3DRect(x, y, widthSlot, heightSlot, true);
		    switch (mBufferReceiver[i].getState()) {
		    case BufferSlot.BUFFERED:
				g.setColor(Color.YELLOW);
				g.fill3DRect(x, y, widthSlot, heightSlot, true);
				break;
		    case BufferSlot.RECEIVED:
				g.setColor(Color.ORANGE);
				g.fill3DRect(x, y, widthSlot, heightSlot, true);
				break;
			}
	    }
	    g.setColor(Color.MAGENTA);
	    g.setFont(new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize()));
	    g.drawString("Receiver", x + distToText, y);	
	    g.setColor(Color.BLACK);
	    g.setFont(new Font(g.getFont().getName(), Font.PLAIN, g.getFont().getSize()));
	    g.drawString("Window size: " + SIZE_WINDOW, x + distToText, y + 15);
	    g.drawString("base: " + mBaseRcv, x + distToText, y + 30);
	}
	
	private void drawWindow(Graphics g, int base,
			int offsetBufferX, int offsetBufferY, 
			int marginSlot, int widthSlot, int heightSlot) {
		final int MARGIN_WINDOW = 10;
		g.setColor(Color.BLACK);
		g.drawRect(
				offsetBufferX + ((marginSlot + widthSlot) * base) - MARGIN_WINDOW, 
				offsetBufferY - MARGIN_WINDOW, 
				((marginSlot + widthSlot) * SIZE_WINDOW) + MARGIN_WINDOW, 
				heightSlot + (MARGIN_WINDOW * 2)
				);
	}
	
	// buffers
	public BufferSlot getBufferSlotSender(int seq) {
		return mBufferSender[seq];
	}
	public void updateBufferSlotSender(int seq, int state) {
		mBufferSender[seq].setState(state);
	}
	public BufferSlot getBufferSlotReceiver(int seq) {
		return mBufferReceiver[seq];
	}
	public void updateBufferSlotReceiver(int seq, int state) {
		mBufferReceiver[seq].setState(state);
	}
	public Piece getFlyingPiece(int seq) {
		return mFlying[seq];
	}
	// 송신측 재전송
	public void retransmit() throws IOException {
		// System.out.println("retransmit");
		for(int i = 0; i < mBufferSender.length; ++i) {
			if(mBufferSender[i].getState() == BufferSlot.SENT) {
				mBufferSender[i].cancelTimerTimeout();
				if(!mFlying[i].isVisible()) { 
					mSender.send(i);
				}
			} // if
		} // for
	} // func
	
	// main
	public static void main(String[] args) throws SocketException, UnknownHostException {
		new SR();
	}
	
} // public class



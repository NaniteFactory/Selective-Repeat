package Connection;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.TimerTask;

import MainWindow.SR;
import Thingy.BufferSlot;
import Thingy.Packet;

public class Sender extends ConnectionThread implements Network {
	private InetAddress mAddrReciever;
	private DatagramSocket mSocket;
	private byte[] mQuickBuffer = new byte[SIZE_PACKET];
	private SR mUi = null;

	public Sender(SR parentContext) throws SocketException, UnknownHostException {
		// TODO Auto-generated constructor stub
		super();
		mUi = parentContext;
		mAddrReciever = InetAddress.getLocalHost();
		mSocket = new DatagramSocket(PORT_SENDER);
	} // func

	@Override
	public void run() {
		// TODO Auto-generated method stub
		initListen(new OnReceivedListener() {
			@Override
			public void onReceived() throws Exception {
				// TODO Auto-generated method stub
				mSocket.receive(new DatagramPacket(mQuickBuffer, mQuickBuffer.length));
				Packet packet = (Packet) deserialization(mQuickBuffer);
				if(packet.isAck()) {
					mUi.updateBufferSlotSender(packet.getSeq(), BufferSlot.ACKED);
					mUi.updateBaseSnd();
					mUi.getBufferSlotSender(packet.getSeq()).cancelTimerTimeout();
				}
			}
		});
	} // func
	
	// sender가 해당 순서번호의 패킷을 송신하며 재전송 타이머 실행함
	@Override
	protected void fakeSend(int seq) throws IOException {		
		// 보내는 측으로부터의 애니메이션 재생
		mUi.updateBufferSlotSender(seq, BufferSlot.SENT);
		mUi.updateNextSequenceSnd();
		mUi.getFlyingPiece(seq).setY(0);
		mUi.getFlyingPiece(seq).setAck(false);
		mUi.getFlyingPiece(seq).setVisible(true);
		mUi.getBufferSlotSender(seq).startTimerAnimation(new TimerTask() {
			int times = 0;
			private final int timesMax = (BufferSlot.SEC_DELIVERY * 1000) / BufferSlot.MSEC_ANIM;
			private final double distStep = SR.DISTANCE_BETWEEN_BUFFERS / timesMax;
			@Override
			public void run() { // 타이머 주기마다 실행
				// TODO Auto-generated method stub
				++times;
				if(times > timesMax / 2) { // 패킷이 전체 거리의 절반만 여행하고 손실
					mUi.getFlyingPiece(seq).setVisible(false);
					mUi.repaint();
					mUi.getBufferSlotSender(seq).cancelTimerAnimation();
					// System.out.println("test");
					return;
				} // if
				mUi.getFlyingPiece(seq).setY((int)(distStep * times)); // 한 스텝 전진
				mUi.repaint();
			}
		});
		// 타임아웃 재전송 타이머 실행
		mUi.getBufferSlotSender(seq).startTimerTimeout(new TimerTask() {
			@Override
			public void run() { // retransmit
				// TODO Auto-generated method stub
				try {
					mUi.retransmit();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	} // func

	// sender가 해당 순서번호의 패킷을 송신하며 재전송 타이머 실행함
	@Override
	protected void niceSend(int seq) throws IOException {		
		// 보내는 측으로부터의 애니메이션 재생
		mUi.updateBufferSlotSender(seq, BufferSlot.SENT);
		mUi.updateNextSequenceSnd();
		mUi.getFlyingPiece(seq).setY(0);
		mUi.getFlyingPiece(seq).setAck(false);
		mUi.getFlyingPiece(seq).setVisible(true);
		mUi.getBufferSlotSender(seq).startTimerAnimation(new TimerTask() {
			int times = 0;
			private final int timesMax = (BufferSlot.SEC_DELIVERY * 1000) / BufferSlot.MSEC_ANIM;
			private final double distStep = SR.DISTANCE_BETWEEN_BUFFERS / timesMax;
			@Override
			public void run() { // 타이머 주기마다 실행
				// TODO Auto-generated method stub
				++times;
				if(times > timesMax) { // 애니메이션의 끝
					try { // 보내는 애니메이션이 끝나면 실제로 패킷을 보낸다
						byte[] serializedMessage = serialization(new Packet(false, seq));
						mSocket.send(
								new DatagramPacket(
									serializedMessage, serializedMessage.length, 
									mAddrReciever, PORT_RECEIVER
								)
							);
						// System.out.println("sent");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mUi.getFlyingPiece(seq).setVisible(false);
					mUi.repaint();
					mUi.getBufferSlotSender(seq).cancelTimerAnimation();
					// System.out.println("test");
					return;
				} // if
				mUi.getFlyingPiece(seq).setY((int)(distStep * times)); // 한 스텝 전진
				mUi.repaint();
			}
		});
		// 타임아웃 재전송 타이머 실행
		mUi.getBufferSlotSender(seq).startTimerTimeout(new TimerTask() {
			@Override
			public void run() { // retransmit
				// TODO Auto-generated method stub
				try {
					mUi.retransmit();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	} // func
	
} // public class

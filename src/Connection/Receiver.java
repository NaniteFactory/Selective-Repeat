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

public class Receiver extends ConnectionThread implements Network {
	private InetAddress mAddrSender;
	private DatagramSocket mSocket;
	private byte[] mQuickBuffer = new byte[SIZE_PACKET];
	private SR mUi = null;
	
	public Receiver(SR parentContext) throws UnknownHostException, SocketException {
		// TODO Auto-generated constructor stub
		super();
		mUi = parentContext;
		mAddrSender = InetAddress.getLocalHost();
		mSocket = new DatagramSocket(PORT_RECEIVER);
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
				if(!packet.isAck()) {
					mUi.updateBufferSlotReceiver(packet.getSeq(), BufferSlot.BUFFERED);
					mUi.updateBaseRcv();
					// sender에게 패킷을 받으면 ack로 응답한다
					send(packet.getSeq());
				} // if
			} // func
		}); // initListen()
	} // func
	
	// 수신측이 ack를 송신하지만 전달되는 중간에 패킷은 손실된다
	@Override
	protected void fakeSend(int seq) {
		// 보내는 측으로부터의 애니메이션 재생
		mUi.getFlyingPiece(seq).setY(SR.DISTANCE_BETWEEN_BUFFERS);
		mUi.getFlyingPiece(seq).setAck(true);
		mUi.getFlyingPiece(seq).setVisible(true);
		mUi.getBufferSlotReceiver(seq).startTimerAnimation(new TimerTask() {
			int times = 0;
			private final int timesMax = (BufferSlot.SEC_DELIVERY * 1000) / BufferSlot.MSEC_ANIM;
			private final double distStep = SR.DISTANCE_BETWEEN_BUFFERS / timesMax;
			@Override
			public void run() { // 타이머 주기마다 실행
				// TODO Auto-generated method stub
				++times;
				if(times > timesMax / 2) { // 우리의 패킷은 총 거리의 절반 만큼만 여행한다
					mUi.getFlyingPiece(seq).setVisible(false);
					mUi.repaint();
					mUi.getBufferSlotReceiver(seq).cancelTimerAnimation();
					// System.out.println("test");
					return;
				} // if
				// 한 스텝 전진
				mUi.getFlyingPiece(seq).setY(
						(int)(SR.DISTANCE_BETWEEN_BUFFERS - (distStep * times))
						);
				mUi.repaint();
			}
		}); // startTimerAnimation()
	} // func
	
	// receiver가 확인응답 패킷을 송신함
	@Override
	protected void niceSend(int seq) throws IOException {	
		// 받는 측으로부터의 ack 송신 애니메이션 재생
		mUi.getFlyingPiece(seq).setY(SR.DISTANCE_BETWEEN_BUFFERS);
		mUi.getFlyingPiece(seq).setAck(true);
		mUi.getFlyingPiece(seq).setVisible(true);
		mUi.getBufferSlotReceiver(seq).startTimerAnimation(new TimerTask() {
			int times = 0;
			private final int timesMax = (BufferSlot.SEC_DELIVERY * 1000) / BufferSlot.MSEC_ANIM;
			private final double distStep = SR.DISTANCE_BETWEEN_BUFFERS / timesMax;
			@Override
			public void run() { // 타이머 주기마다 실행
				// TODO Auto-generated method stub
				++times;
				if(times > timesMax) { // 애니메이션의 끝
					try { // 보내는 애니메이션이 끝나면 실제로 패킷을 보낸다
						byte[] serializedMessage = serialization(new Packet(true, seq));
						mSocket.send(
								new DatagramPacket(
									serializedMessage, serializedMessage.length, 
									mAddrSender, PORT_SENDER
								)
							);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mUi.getFlyingPiece(seq).setVisible(false);
					mUi.repaint();
					mUi.getBufferSlotReceiver(seq).cancelTimerAnimation();
					// System.out.println("test");
					return;
				} // if
				// 한 스텝 전진
				mUi.getFlyingPiece(seq).setY(
						(int)(SR.DISTANCE_BETWEEN_BUFFERS - (distStep * times))
						);
				mUi.repaint();
			}
		}); // startTimerAnimation()
	} // func
	
} // public class

package Thingy;

import java.util.Timer;
import java.util.TimerTask;

public class BufferSlot {
	public final static int EMPTY = 0;
	// sender
	public final static int SENT = 1; // 이미 응답됨: 보냈지만 ack를 기다리는 상태
	public final static int ACKED = 2; // 전송 후 응답 안 됨: ack 받아 종결한 슬롯
	// receiver
	public final static int BUFFERED = 3; // 확인 응답됨: 받았지만 앞 순서번호를 기다리느라 저장만 되어 있음
	public final static int RECEIVED = 4; // 확인 응답됨: 윈도우가 넘어가서 수신 완료되었음이 확실한 곳
	
	private int mState;
	
	public final static int SEC_TIMEOUT = 10;
	public final static int SEC_DELIVERY = 3;
	public final static int MSEC_ANIM = 100;
	private Timer mTimerTimeout = null;	// sender only
	private Timer mTimerAnimation = null;
	
	public BufferSlot() { 
		mState = BufferSlot.EMPTY; 
	}
	
	public int getState() { return mState; }
	public void setState(int state) { mState = state; } 
	
	// 주의: 타이머는 일회용이기 때문에 한 번 취소되면 더이상 스케쥴할 수 없다. 재사용 불가능하다.
	public void startTimerTimeout(TimerTask onTimeout) {
		if(mTimerTimeout != null) { cancelTimerTimeout(); }
		mTimerTimeout = new Timer();
		mTimerTimeout.schedule(onTimeout, SEC_TIMEOUT * 1000);
	}
	public void cancelTimerTimeout() { if(mTimerTimeout != null) { mTimerTimeout.cancel(); } }

	public void startTimerAnimation(TimerTask onTimeAnimate) {
		if(mTimerAnimation != null) { cancelTimerAnimation(); }
		mTimerAnimation = new Timer();
		mTimerAnimation.scheduleAtFixedRate(onTimeAnimate, 0, MSEC_ANIM);
	}
	public void cancelTimerAnimation() { if(mTimerAnimation != null) { mTimerAnimation.cancel(); } }
	
} // class


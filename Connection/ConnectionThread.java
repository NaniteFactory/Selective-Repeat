package Connection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class ConnectionThread extends Thread {
	public static interface OnReceivedListener {
		void onReceived() throws Exception;
	}
	
	private boolean mbListening = false;
	
	protected void initListen(OnReceivedListener listener) {
		if(!mbListening) {
			mbListening = true;
			new Thread() { // 듣게 함
				public void run() {
					for (;;) {
						try {
							listener.onReceived();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} // for(;;)
				};
			}.start();
		} // if
	} // func
	
	public void send(int seq) throws IOException {
		if(Math.random() < 0.8) { // 80% 확률로 온전한 패킷을 보낸다
			niceSend(seq);
		} else { // 20% 확률로 패킷을 중간에 잃는다
			fakeSend(seq);
		}
	} // func
	protected abstract void niceSend(int seq) throws IOException;
	protected abstract void fakeSend(int seq) throws IOException;
	
	// Serializable 객체를 바이트의 나열로 변환하여 반환
	protected byte[] serialization(Serializable serializable) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream); 
		objectOutputStream.writeObject(serializable);
		byte[] serializedMessage = byteArrayOutputStream.toByteArray();
		objectOutputStream.close();
		byteArrayOutputStream.close();
		return serializedMessage;
	}
	
	// 바이트의 나열을 Serializable 객체로 변환하여 반환 (받은 다음 캐스팅해서 쓸 것)
	protected Object deserialization(byte[] recvBytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(recvBytes);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
		Object deserializedObject = objectInputStream.readObject();
		objectInputStream.close();
		byteArrayInputStream.close();
		return deserializedObject;
	}
	
} // public class

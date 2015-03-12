package AndroidChat;

import java.util.Calendar;

import ASN1.Encoder;

public
class DatedChatMessage {
	public DatedChatMessage(ChatMessage cmsg, Calendar _time) {
		time = _time;
		msg = cmsg;
	}
	
	public Calendar time;
	public ChatMessage msg;
	public boolean received = false;
	public String peerGID;
	
	public String toString() {
		return "DCM[rcv="+received
				+ " time="+Encoder.getGeneralizedTime(time)
				+ "\n msg="+msg
				+"\n]";
	}
}
package net.ddp2p.common.examplePlugin;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;
public
class AndroidChatReceiver implements ChatReceiver {
	private static final boolean DEBUG = true;
	public void addSentMessage(ChatMessage cm, String peer_GID, ChannelDataOut crt) {
	   	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  addSentMessage: enter");
		crt.messages.add(new DatedChatMessage(cm, Util.CalendargetInstance()));
		for (int k = 0; k < crt.messages.size(); k ++)
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver addSentMessage: -> idx="+k+" msg="+crt.messages.get(k));
		try{ 
			Chat crtChat;
			if ((crtChat = Chat.getCrtChat()) == null) return;
			String peerGID = crtChat.getPeerGID();
			if (peerGID == null) return;
			crtChat.setChatEntityList(getListEntity(peerGID));
		} catch (Exception e) {
			e.printStackTrace();
		}
	   	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  addSentMessage: exit");
	}
	@Override
	public boolean receiveMessage(BigInteger first_in_this_sequence,
			BigInteger sequence, String msgStr, String peerName,
			String peer_GID, byte[] session_id, ChatMessage cmsg, ChannelDataIn crt) {
    	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  receiveMessage: enter");
		if (peerName != null) crt.peerName = peerName;
		if (! Util.equalBytes_null_or_not(crt.session_ID, session_id)) {
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: Unexpected Mismatched sessionID in message vs local: "+Util.byteToHex(session_id)+" vs "+Util.byteToHex(crt.session_ID));
			crt.setNewSession(cmsg.session_id, first_in_this_sequence, sequence);
		}
		if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: old msg#="+crt.messages.size());
		int index = sequence.subtract(first_in_this_sequence).intValue();
		int old_index_plus_1 = 0;
		old_index_plus_1 = crt.messages.size();
		for (int k = old_index_plus_1; k <= index; k++)
			crt.messages.add(null);
		if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: new msg->#="+crt.messages.size());
		if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: set at idx="+index+" msg="+cmsg);
		crt.messages.set(index, new DatedChatMessage(cmsg, Util.CalendargetInstance()));
		for (int k = 0; k < crt.messages.size(); k ++)
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: -> idx="+k+" msg="+crt.messages.get(k));
    	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  receiveMessage: done inner");
		try {
			Chat crtChat;
			if ((crtChat = Chat.getCrtChat()) == null) return true;
			if (peerName != null) crtChat.setPeerName(peerName);
			String peerGID = crtChat.getPeerGID();
			if (peerGID == null) return false;
			crtChat.setChatEntityList(AndroidChatReceiver.getListEntity(peerGID));
		} catch (Exception e) {
			e.printStackTrace();
		}
    	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  receiveMessage: exit");
		return false;
	}
	@Override
	public void confirmMessages(BigInteger lastAckInSequence,
			ArrayList<BigInteger> out_of_sequence, String peer_GID,
			byte[] session_id, ChannelDataOut crt) {
		if (!Util.equalBytes_null_or_not(session_id, crt.session_ID)) {
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: Mismatched sessionID in acknowledgment vs local: "+Util.byteToHex(session_id)+" vs "+Util.byteToHex(crt.session_ID));
			return;
		}
		if (lastAckInSequence != null) {
			int index = lastAckInSequence.subtract(crt.firstInSequence).intValue();
			int old_index_plus_1 = 0;
			if (crt.sequence_ack != null) old_index_plus_1 = crt.sequence_ack.subtract(crt.firstInSequence).intValue()+1;
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: set true from: " + old_index_plus_1 + " to " + (Math.min(crt.received.size() - 1, index)));
			for (int k = old_index_plus_1; k <= Math.min(crt.received.size() - 1, index); k ++)
				crt.received.set(k, true);
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: add true from: " + crt.received.size() + " to " + index);
			for (int k = crt.received.size(); k <= index; k++)
				crt.received.add(true);
		}		
		if (out_of_sequence != null) {
			for (int i = 0; i < out_of_sequence.size(); i ++ ) {
				int _index = out_of_sequence.get(i).subtract(crt.firstInSequence).intValue();
				if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: AOS add true from: " + crt.received.size() + " to " + _index);
				for (int k = crt.received.size(); k <= _index; k ++)
					crt.received.add(false);
				crt.received.set(_index, true);
			}
		}
		try {
			Chat crtChat;
			if ((crtChat = Chat.getCrtChat()) == null) return;
			String peerGID = crtChat.getPeerGID();
			if (peerGID == null) return;
			crtChat.setChatEntityList(AndroidChatReceiver.getListEntity(peerGID));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void jamMessages(BigInteger sequence,
			String peer_GID,
			byte[] session_id, ChannelDataOut crt) {
		int index = sequence.subtract(crt.firstInSequence).intValue();
		for (int k = crt.jam.size(); k <= index; k++)
			crt.jam.add(false);
		crt.jam.set(index, true);
		try {
			Chat crtChat;
			if ((crtChat = Chat.getCrtChat()) == null) return;
			String peerGID = crtChat.getPeerGID();
			if (peerGID == null) return;
			crtChat.setChatEntityList(AndroidChatReceiver.getListEntity(peerGID));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * merge step of merge sort 
	 * @param peerGID
	 * @return
	 */
	public static List<ChatEntity> getListEntity(String peerGID) {
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: enter ="+peerGID);
		ChannelDataIn _recv = ChannelDataIn.get(peerGID); 
		ChannelDataOut _sent = ChannelDataOut.get(peerGID);
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: recv="+_recv);
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: sent="+_sent);
		ArrayList<DatedChatMessage> messages_in;
		ArrayList<DatedChatMessage> messages_out;
		messages_in = new ArrayList<DatedChatMessage>();
		if (_recv != null && _recv.messages_old != null) messages_in.addAll(_recv.messages_old);
		if (_recv != null && _recv.messages != null) messages_in.addAll(_recv.messages);
		if (_sent == null || _sent.messages == null) messages_out = new ArrayList<DatedChatMessage>();
		else messages_out = _sent.messages;
		List<ChatEntity> result = new ArrayList<ChatEntity>();
		if (messages_out.size() == 0) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: null send");
			for (DatedChatMessage cm : messages_in) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add all r: "+ce);
				result.add(ce);
			}
			return result;
		}
		if (messages_in.size() == 0) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: null recv");
			for (DatedChatMessage cm : messages_out) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add all s: "+ce);
				result.add(ce);
			}
			return result;
		}
		int i = 0, j=0;
		while (i < messages_in.size() || j < messages_out.size()) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: i="+i+" j="+j);
			if (i == messages_in.size()) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: i done j="+j);
				for (; j < messages_out.size(); j++) {
					DatedChatMessage cm = messages_out.get(j);
					if (cm == null || cm.time == null || cm.msg == null) {
						if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result i done add: UNK");
						result.add(new ChatEntity(0, "UNK", null, false, false, null)); 
					} else {
						ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
						if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 0 add s: "+ce);
						result.add(ce);
					}
				}
				break;
			}
			if (j == messages_out.size()) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: j done i="+i);
				for (; i < messages_in.size(); i++) {
					DatedChatMessage cm = messages_in.get(i);
					if (cm == null || cm.time == null || cm.msg == null) {
						if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result j done add: UNK");
						result.add(new ChatEntity(0, "UNK", null, false, false, null)); 
					} else {
						ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
						if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 0 add r:  "+ce);
						result.add(ce);
					}
				}
				break;
			}
			DatedChatMessage l = messages_in.get(i);
			DatedChatMessage r = messages_out.get(j);
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add: l="+l);
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add: r="+r);
			if (l == null || l.time == null) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 1 add: UNK");
				result.add(new ChatEntity(0, "UNK", null, false, false, null)); i++; continue;}
			if (r == null || r.time == null) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 2 add: UNK");
				result.add(new ChatEntity(0, "UNK", null, true, false, null)); i++; continue;}
			if (l.time.compareTo(r.time) <= 0) { 
				ChatEntity ce = new ChatEntity(0, l.msg.msg, Encoder.getGeneralizedTime(l.time), true, true, l.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 3 add: "+ce);
				result.add(ce);
				i++;
				continue;
			} else {
				ChatEntity ce = new ChatEntity(0, r.msg.msg, Encoder.getGeneralizedTime(r.time), false, r.received, r.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result 4 add: "+ce);
				result.add(ce);
				j++;
				continue;
			}
		}
		for (ChatEntity r : result) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: get->"+r);
		}
		ChatEntity[] r = result.toArray(new ChatEntity[0]);
		Arrays.sort(r, new Comparator<ChatEntity>() {
			@Override
			public int compare(ChatEntity o1, ChatEntity o2) {
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return o1.getTime().compareTo(o2.getTime());
			}});
		return result;
	}
}

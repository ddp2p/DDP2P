package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import util.Util;
import ASN1.Encoder;

import com.HumanDecisionSupportSystemsLaboratory.DDP2P.Chat;
import com.HumanDecisionSupportSystemsLaboratory.DDP2P.ChatEntity;

class DatedChatMessage {
	public DatedChatMessage(ChatMessage cmsg, Calendar _time) {
		time = _time;
		msg = cmsg;
	}
	Calendar time;
	ChatMessage msg;
	boolean received = false;
	String peerGID;
}
class ChannelChatMessage {
	byte[] session_id;
	String peerName;
	BigInteger first_in_sequence;
	BigInteger sequence_ack;
	Calendar time;
	
	ArrayList<DatedChatMessage> messages = new ArrayList<DatedChatMessage>();
	ArrayList<Boolean> received = new ArrayList<Boolean>();
	ArrayList<Boolean> jam = new ArrayList<Boolean>();
}
public
class AndroidChatReceiver implements ChatReceiver {
	static Hashtable<String,ChannelChatMessage> messages_received = new Hashtable<String,ChannelChatMessage>();
	static Hashtable<String,ChannelChatMessage> messages_sent = new Hashtable<String,ChannelChatMessage>();

	public static void addSentMessage(ChatMessage cm, String peer_GID) {
		ChannelChatMessage crt = messages_sent.get(peer_GID);
		if (crt == null) {
			crt = new ChannelChatMessage();
			messages_sent.put(peer_GID, crt);
			crt.first_in_sequence = cm.first_in_this_sequence;
			crt.session_id = cm.session_id;
			crt.time = Util.CalendargetInstance();
			crt.messages = new ArrayList<DatedChatMessage>();
		}
		int index = cm.sequence.subtract(cm.first_in_this_sequence).intValue();
		crt.messages.add(index, new DatedChatMessage(cm, Util.CalendargetInstance()));
		
		Chat crtChat;
		if ((crtChat = Chat.getCrtChat()) == null) return;
		String peerGID = crtChat.getPeer().getGID();
		crtChat.setChatEntityList(getListEntity(peerGID));
	}

	@Override
	public void receiveMessage(BigInteger first_in_this_sequence,
			BigInteger sequence, String msgStr, String peerName,
			String peer_GID, byte[] session_id, ChatMessage cmsg) {
		// announce message received;
		ChannelChatMessage crt = messages_received.get(peer_GID);
		if (crt == null) {
			crt = new ChannelChatMessage();
			messages_received.put(peer_GID, crt);
		}
		if (peerName != null) crt.peerName = peerName;
		if (! Util.equalBytes_null_or_not(crt.session_id, session_id)) {
			crt.first_in_sequence = first_in_this_sequence;
			crt.session_id = crt.session_id;
			crt.time = Util.CalendargetInstance();
			crt.messages = new ArrayList<DatedChatMessage>();
		}
		int index = sequence.subtract(first_in_this_sequence).intValue();
		crt.messages.add(index, new DatedChatMessage(cmsg, Util.CalendargetInstance()));
		
		Chat crtChat;
		if ((crtChat = Chat.getCrtChat()) == null) return;
		String peerGID = crtChat.getPeer().getGID();
		crtChat.setChatEntityList(this.getListEntity(peerGID));
	}
	

	@Override
	public void confirmMessages(BigInteger sequence,
			ArrayList<BigInteger> out_of_sequence, String peer_GID,
			byte[] session_id) {
		ChannelChatMessage crt = messages_sent.get(peer_GID);
		if (crt == null) {
			crt = new ChannelChatMessage();
			messages_sent.put(peer_GID, crt);
		}
		int index = sequence.subtract(crt.first_in_sequence).intValue();
		// extending the array of received flags
		for (int k = crt.received.size(); k <= index; k++)
			crt.received.add(false);
		// add the received flag for the message sequence
		for (int k = crt.sequence_ack.intValue() + 1; k <= sequence.intValue(); k++ )
			crt.received.set(index, true);
		if (out_of_sequence != null) {
			for (int i = 0; i < out_of_sequence.size(); i ++ ) {
				int _index = out_of_sequence.get(i).subtract(crt.first_in_sequence).intValue();
				
				for (int k = crt.received.size(); k <= _index; k++)
					crt.received.add(false);
				// add the received flag for the message sequence
				for (int k = crt.sequence_ack.intValue() + 1; k <= sequence.intValue(); k++ )
					crt.received.set(_index, true);
			}
		}
		
		Chat crtChat;
		if ((crtChat = Chat.getCrtChat()) == null) return;
		String peerGID = crtChat.getPeer().getGID();
		crtChat.setChatEntityList(this.getListEntity(peerGID));
	}
	public void jamMessages(BigInteger sequence,
			String peer_GID,
			byte[] session_id) {
		ChannelChatMessage crt = messages_sent.get(peer_GID);
		if (crt == null) {
			crt = new ChannelChatMessage();
			messages_sent.put(peer_GID, crt);
		}
		int index = sequence.subtract(crt.first_in_sequence).intValue();
		// extending the array of received flags
		for (int k = crt.received.size(); k <= index; k++)
			crt.jam.add(false);
		// add the received flag for the message sequence
		for (int k = crt.sequence_ack.intValue() + 1; k <= sequence.intValue(); k++ )
			crt.jam.set(index, true);
		
		Chat crtChat;
		if ((crtChat = Chat.getCrtChat()) == null) return;
		String peerGID = crtChat.getPeer().getGID();
		crtChat.setChatEntityList(this.getListEntity(peerGID));
	}
	/*
	public List<ChatEntity> getListEntity(String peerGID) {
		ChannelChatMessage recv = AndroidChatReceiver.messages_received.get(peerGID);
		ChannelChatMessage sent = AndroidChatReceiver.messages_sent.get(peerGID);
		List<ChatEntity> result = new ArrayList<ChatEntity>();
		if (recv != null) {
			for (DatedChatMessage cm : recv.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
				result.add(ce);
			}
		}
		if (sent != null) {
			for (DatedChatMessage cm : sent.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
				result.add(ce);
			}
		}
		ChatEntity[] ces = result.toArray(new ChatEntity[0]);
		Arrays.sort(ces, new Comparator<ChatEntity>() {

			@Override
			public int compare(ChatEntity lhs, ChatEntity rhs) {
				if (lhs == null) return -1;
				if (rhs == null) return 1;
				return lhs.getTime().compareTo(rhs.getTime());
			}
			
		});
		return result;
	}
	*/
	/**
	 * merge step of merge sort 
	 * @param peerGID
	 * @return
	 */
	public static List<ChatEntity> getListEntity(String peerGID) {
		ChannelChatMessage recv = AndroidChatReceiver.messages_received.get(peerGID);
		ChannelChatMessage sent = AndroidChatReceiver.messages_sent.get(peerGID);
		List<ChatEntity> result = new ArrayList<ChatEntity>();
		if (sent == null) {
			for (DatedChatMessage cm : recv.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
				result.add(ce);
			}
			return result;
		}
		if (recv == null) {
			for (DatedChatMessage cm : sent.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
				result.add(ce);
			}
			return result;
		}
		int i = 0, j=0;
		while (i < recv.messages.size() && j < sent.messages.size()) {
			if (i == recv.messages.size()) {
				for (; j < sent.messages.size(); j++) {
					DatedChatMessage cm = sent.messages.get(j);
				
					ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
					result.add(ce);
				}				
			}
			if (j == sent.messages.size()) {
				for (; i < recv.messages.size(); i++) {
					DatedChatMessage cm = recv.messages.get(i);
					
					ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
					result.add(ce);
				}
			}
			DatedChatMessage l = recv.messages.get(i);
			DatedChatMessage r = sent.messages.get(j);
			
			if (l == null) { result.add(new ChatEntity(0, "UNK", null, false, false, null)); i++; continue;}
			
			if (l.time.compareTo(r.time) <= 0) { 
				ChatEntity ce = new ChatEntity(0, l.msg.msg, Encoder.getGeneralizedTime(l.time), true, true, l.msg);
				result.add(ce);
				i++;
				continue;
			} else {
				ChatEntity ce = new ChatEntity(0, r.msg.msg, Encoder.getGeneralizedTime(r.time), false, r.received, r.msg);
				result.add(ce);
				j++;
				continue;
			}
		}
		return result;
	}

}

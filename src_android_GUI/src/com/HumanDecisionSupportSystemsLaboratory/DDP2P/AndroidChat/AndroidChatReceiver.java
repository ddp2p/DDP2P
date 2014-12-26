/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Khalid Alhamed, Marius Silaghi <silaghi@fit.edu>
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

//package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;
//import com.HumanDecisionSupportSystemsLaboratory.DDP2P.Chat;
//import com.HumanDecisionSupportSystemsLaboratory.DDP2P.ChatEntity;
package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.HumanDecisionSupportSystemsLaboratory.DDP2P.Chat;
import com.HumanDecisionSupportSystemsLaboratory.DDP2P.ChatEntity;

import util.Util;
import ASN1.Encoder;



//class ChannelChatMessage {
//	byte[] session_id;
//	String peerName;
//	BigInteger first_in_sequence;
//	BigInteger sequence_ack;
//	Calendar time;
//	
//	ArrayList<DatedChatMessage> messages = new ArrayList<DatedChatMessage>();
//	ArrayList<Boolean> received = new ArrayList<Boolean>();
//	ArrayList<Boolean> jam = new ArrayList<Boolean>();
//}

public
class AndroidChatReceiver implements ChatReceiver {
//	static Hashtable<String,ChannelDataIn> messages_received = new Hashtable<String,ChannelDataIn>();
//	static Hashtable<String,ChannelDataOut> messages_sent = new Hashtable<String,ChannelDataOut>();

	private static final boolean DEBUG = true;
	public void addSentMessage(ChatMessage cm, String peer_GID, ChannelDataOut crt) {
	   	if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver:  addSentMessage: enter");
		//int index = cm.sequence.subtract(cm.first_in_this_sequence).intValue();
		//for (int i = crt.messages.size(); i <= index; i++) crt.messages.add(null); // create position
		//crt.messages.add(index, new DatedChatMessage(cm, Util.CalendargetInstance()));
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
		// announce message received;
//		ChannelDataIn crt = messages_received.get(peer_GID);
//		if (crt == null) {
//			crt = new ChannelDataIn();
//			messages_received.put(peer_GID, crt);
//		}
		if (peerName != null) crt.peerName = peerName;
		if (! Util.equalBytes_null_or_not(crt.session_ID, session_id)) {
			if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver receiveMessages: Mismatched sessionID in acknowledgment vs local: "+Util.byteToHex(session_id)+" vs "+Util.byteToHex(crt.session_ID));
			crt.firstInSequence = first_in_this_sequence;
			crt.session_ID = cmsg.session_id;
			crt.time = Util.CalendargetInstance();
			crt.messages = new ArrayList<DatedChatMessage>();
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
		
		// printout for debug the list of messages after addition
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
		    // extending the array of received flags (set to true)
		    for (int k = old_index_plus_1; k <= Math.min(crt.received.size() - 1, index); k ++)
			crt.received.set(k, true);

		    if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: add true from: " + crt.received.size() + " to " + index);
		    for (int k = crt.received.size(); k <= index; k++)
			crt.received.add(true);
		}
//		// add the received flag for the message sequence
//		for (BigInteger k = crt.sequence_ack.add(BigInteger.ONE); k.compareTo(lastAckInSequence) <= 0; k = k.add(BigInteger.ONE) )
//			crt.received.set(index, true);
		
		if (out_of_sequence != null) {
			for (int i = 0; i < out_of_sequence.size(); i ++ ) {
				int _index = out_of_sequence.get(i).subtract(crt.firstInSequence).intValue();
				
				if (DEBUG) System.out.println("PLUGIN CHAT: ChatReceiver confirmMessages: AOS add true from: " + crt.received.size() + " to " + _index);
				for (int k = crt.received.size(); k <= _index; k ++)
					crt.received.add(false);
				
				// add the received flag for the message sequence
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
		// extending the array of received flags
		for (int k = crt.jam.size(); k <= index; k++)
			crt.jam.add(false);
		// add the received flag for the message sequence
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
		synchronized(Main.monitor) {
			return _getListEntity(peerGID);
		}
	}
	public static List<ChatEntity> _getListEntity(String peerGID) {
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: enter ="+peerGID);
		ChannelDataIn recv = ChannelDataIn.get(peerGID); //AndroidChatReceiver.messages_received.get(peerGID);
		ChannelDataOut sent = ChannelDataOut.get(peerGID);
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: recv="+recv);
		if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: sent="+sent);
		List<ChatEntity> result = new ArrayList<ChatEntity>();
		if (sent == null || sent.messages == null || sent.messages.size() == 0) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: null send");
			for (DatedChatMessage cm : recv.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), true, true, cm.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add all r: "+ce);
				result.add(ce);
			}
			return result;
		}
		if (recv == null || recv.messages == null || recv.messages.size() == 0) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: null recv");
			for (DatedChatMessage cm : sent.messages) {
				ChatEntity ce = new ChatEntity(0, cm.msg.msg, Encoder.getGeneralizedTime(cm.time), false, cm.received, cm.msg);
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: result add all s: "+ce);
				result.add(ce);
			}
			return result;
		}
		int i = 0, j=0;
		while (i < recv.messages.size() || j < sent.messages.size()) {
			if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: i="+i+" j="+j);
			if (i == recv.messages.size()) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: i done j="+j);
				for (; j < sent.messages.size(); j++) {
					DatedChatMessage cm = sent.messages.get(j);
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
			if (j == sent.messages.size()) {
				if (DEBUG) System.out.println("AndroidChatReceiver: getListEntity: j done i="+i);
				for (; i < recv.messages.size(); i++) {
					DatedChatMessage cm = recv.messages.get(i);
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
			DatedChatMessage l = recv.messages.get(i);
			DatedChatMessage r = sent.messages.get(j);
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
		return result;
	}

}

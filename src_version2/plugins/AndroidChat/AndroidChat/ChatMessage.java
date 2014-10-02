package com.HumanDecisionSupportSystemsLaboratory.DDP2P.AndroidChat;
//package dd_p2p.plugin;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
import util.Util; 

public class ChatMessage extends ASN1.ASNObj {
     public static final int MT_TEXT = 1;
     public static final int MT_IMAGE = 2;
     public static final int MT_EMPTY = 0; // just to inform session_id, session_id_ack, first_in_this_sequence, last_acknowledged_in_sequence, received_out_of_sequence
     int version = 0;
     int message_type = MT_EMPTY; // 1-TEXT (message is in "msg"), 2-image, 3-request full element
     byte[] session_id; // array of 8 random numbers
     byte[] session_id_ack; // array of 8 random numbers
     BigInteger requested_element;
     String msg;
     ArrayList<ChatElem> content;
     BigInteger first_in_this_sequence; // the id of the first message in this sequence (>0)
     BigInteger sequence;
     BigInteger sub_sequence;
     BigInteger sub_sequence_ack;
     BigInteger last_acknowledged_in_sequence;
     ArrayList<BigInteger> received_out_of_sequence;
     public String toString () {
     	return 
     		"ChatMessage ["
     			+ "\n version="+version
     			+ "\n msg_type="+message_type
     			+ "\n session_id="+Util.byteToHex(session_id)
     			+ "\n session_id_ack="+Util.byteToHex(session_id_ack)
     			+ "\n req="+requested_element
     			+ "\n msg="+msg
     			+ "\n content #="+Util.concat(content,",",null)
     			+ "\n first_seq="+first_in_this_sequence
     			+ "\n sequence="+sequence
     			+ "\n sub_sequence="+sub_sequence
     			+ "\n sub_seq_ack="+sub_sequence_ack
     			+ "\n last_ack="+last_acknowledged_in_sequence
     			+ "\n recv_out_of_seq=" + received_out_of_sequence
     			+"\n]"	;
     }

     static byte[] createSessionID() {
         byte[] rnd = new byte[8];
         Random r = new SecureRandom();
         r.nextBytes(rnd);
         return rnd;
     }

     @Override
     public Encoder getEncoder() {
         Encoder enc = new Encoder().initSequence();
         enc.addToSequence(new Encoder(version));
         enc.addToSequence(new Encoder(message_type));
         enc.addToSequence(new Encoder(session_id));
         if (session_id_ack != null) enc.addToSequence(new Encoder(session_id_ack).setASN1Type(DD.TAG_AP1));
         if (requested_element != null) enc.addToSequence(new Encoder(requested_element).setASN1Type(DD.TAG_AP2));
         if (msg != null) enc.addToSequence(new Encoder(msg).setASN1Type(DD.TAG_AP3));
         if (content != null) enc.addToSequence(Encoder.getEncoder(content).setASN1Type(DD.TAG_AC4));
         if (first_in_this_sequence != null) enc.addToSequence(new Encoder(first_in_this_sequence).setASN1Type(DD.TAG_AP5));
         if (sequence != null) enc.addToSequence(new Encoder(sequence).setASN1Type(DD.TAG_AP6));
         if (sub_sequence != null) enc.addToSequence(new Encoder(sub_sequence).setASN1Type(DD.TAG_AP7));
         if (sub_sequence_ack != null) enc.addToSequence(new Encoder(sub_sequence_ack).setASN1Type(DD.TAG_AP8));
         if (last_acknowledged_in_sequence != null) enc.addToSequence(new Encoder(last_acknowledged_in_sequence).setASN1Type(DD.TAG_AP9));
         if (received_out_of_sequence != null) {BigInteger[] bigIntArray=null; received_out_of_sequence.toArray(bigIntArray); 
                                                enc.addToSequence(new Encoder(bigIntArray).setASN1Type(DD.TAG_AC10));
                                               }
         return enc;
     }

     @Override
     public ChatMessage decode(Decoder dec) throws ASN1DecoderFail {
         Decoder d = dec.getContent();
         version = d.getFirstObject(true).getInteger().intValue();
         message_type = d.getFirstObject(true).getInteger().intValue();
         session_id = d.getFirstObject(true).getBytes();
         if (d.getTypeByte() == DD.TAG_AP1) session_id_ack = d.getFirstObject(true).getBytes();
         if (d.getTypeByte() == DD.TAG_AP2) requested_element = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AP3) msg = d.getFirstObject(true).getString();
         if (d.getTypeByte() == DD.TAG_AC4) content = d.getFirstObject(true).getSequenceOfAL(ChatElem.getASN1Type(), new ChatElem());
         if (d.getTypeByte() == DD.TAG_AP5) first_in_this_sequence = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AP6) sequence = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AP7) sub_sequence = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AP8) sub_sequence_ack = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AP9) last_acknowledged_in_sequence = d.getFirstObject(true).getInteger();
         if (d.getTypeByte() == DD.TAG_AC10) received_out_of_sequence = new ArrayList<BigInteger>(Arrays.asList(d.getFirstObject(true).getBNIntsArray()));
         return this;
     }
 }
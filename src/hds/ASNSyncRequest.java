/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
 package hds;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JOptionPane;

import ciphersuits.Cipher;
import ciphersuits.SK;

import config.Application;
import config.DD;
import data.D_PeerAddress;
import data.D_PluginData;

import streaming.OrgFilter;
import streaming.SpecificRequest;
import util.P2PDDSQLException;
import util.Summary;
import util.Util;
import static util.Util._;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
UDPFragment := IMPLICIT [APPLICATION 12] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgType INTEGER,
	msgID UTF8String,
	fragments INTEGER,
	sequence INTEGER,
	data OCTET STRING
}
UDPFragmentAck := IMPLICIT [APPLICATION 11] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgID UTF8String,
	transmitted OCTET STRING,
}
UDPFragmentNAck := IMPLICIT [APPLICATION 15] UDPFragmentNAck;
UDPReclaim := IMPLICIT [APPLICATION 16] UDPFragmentNAck;
 */
/*
class Address extends ASNObj{
	String domain; //PrintableString
	int port;
	Address(){}
	Address(String _domain, int _port){
		domain = _domain; port=_port;
	}
	Address(String domain_port){
		if(domain_port == null) return;
		String dp[] = domain_port.split(":");
		if(dp.length!=2) return;
		domain = dp[0]; port = Integer.parseInt(dp[1]);
	}
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(domain).setASN1Type(Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(port));
		return enc;
	}
	public Address decode(Decoder dec){
		Decoder content=dec.getContent();
		domain = content.getFirstObject(true).getString();
		port = content.getFirstObject(true).getInteger().intValue();
		return this;
	}
}
*/

class UDPMessage {
	public int type;
	UDPFragment[] fragment;
	byte[] transmitted;
	int[] sent_attempted; //not packed (used only at sender)
	int unacknowledged = 0;
	long date;
	int received;
	public String msgID;
	SocketAddress sa;
	
	//Acknowledgment data
	UDPFragmentAck uf;
	byte[] ack;
	boolean ack_changed=false;
	Object lock_ack=new Object();
	
	public String destinationID, senderID;
	public int checked = 0; // how many requests are dropped waiting to send a message
	public String toString() {
		String res = "UDPMesage:" +
				" ID="+msgID+
				"\n type="+type+
				"\n date: "+date+"ms"+
				"\n unack: "+unacknowledged+
				"\n rec="+received+"/"+fragment.length+
		"\n transmitted=";
		for(int k=0; k<fragment.length;k++)
			res+="."+transmitted[k];
		res += "\n attempts";
		for(int k=0; k<fragment.length;k++)
			res+="."+sent_attempted[k];
		return res;
	}
	UDPMessage(int len){
		fragment = new UDPFragment[len];
		transmitted = new byte[len];
		sent_attempted = new int[len];
		date = Util.CalendargetInstance().getTimeInMillis();
		
		uf = new UDPFragmentAck();
	}
	public byte[] assemble(){
		if(received<fragment.length) return null;
		int MTU = fragment[0].data.length;
		int msglen =(fragment.length-1)*MTU + 
			fragment[fragment.length-1].data.length;
		byte[] msg = new byte[msglen];
		for(int i=0; i<fragment.length; i++) {
			Util.copyBytes(msg, i*MTU, fragment[i].data, 
					fragment[i].data.length, 0);
		}
		return msg;
	}
	public boolean no_ack_received() {
		for(int i=0; i<fragment.length; i++) {
			if(this.transmitted[i] == 1) return false;
		}
		return true;
	}
}
/**
UDPFragmentAck := IMPLICIT [APPLICATION 11] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgID UTF8String,
	transmitted OCTET STRING,
}
 */
class UDPFragmentAck extends ASNObj {
	String senderID;
	byte[] signature;
	String destinationID;
	String msgID;
	byte[] transmitted;
	public String toString() {
		String res = "UDPFragAck: ID="+msgID+"\n sID: "+Util.trimmed(senderID)+
		"\n dID="+Util.trimmed(destinationID)+
		"\n sign="+Util.byteToHexDump(signature)+
		"\n transmitted=";
		for(int k=0; k<transmitted.length;k++)
			res+="."+transmitted[k];
		return res;
	}
/**
UDPFragmentAck := IMPLICIT [APPLICATION 11] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgID UTF8String,
	transmitted OCTET STRING,
}
UDPFragmentNAck := IMPLICIT [APPLICATION 15] UDPFragmentNAck;
UDPReclaim := IMPLICIT [APPLICATION 16] UDPFragmentNAck;
 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(senderID));
		enc.addToSequence(new Encoder(signature));
		enc.addToSequence(new Encoder(destinationID));
		enc.addToSequence(new Encoder(msgID));
		enc.addToSequence(new Encoder(transmitted));
		enc.setASN1Type(DD.TAG_AC11);
		return enc;
	}
	@Override
	public UDPFragmentAck decode(Decoder dec) throws ASN1DecoderFail {
		Decoder content=dec.getContent();
		senderID = content.getFirstObject(true).getString();
		signature = content.getFirstObject(true).getBytes();
		destinationID = content.getFirstObject(true).getString();
		msgID = content.getFirstObject(true).getString();
		transmitted = content.getFirstObject(true).getBytes();
		return this;
	}
}
/**
UDPReclaim := IMPLICIT [APPLICATION 16] UDPFragmentNAck;
*/
class UDPReclaim extends UDPFragmentAck{
	@Override
	public Encoder getEncoder() {
		Encoder enc = super.getEncoder();
		enc.setASN1Type(DD.TAG_AC16);
		return enc;
	}	
	public String toString() {
		String res = "UDPReclaim: ID="+msgID+"\n sID: "+Util.trimmed(senderID)+
		"\n dID="+Util.trimmed(destinationID)+
		"\n sign="+Util.byteToHexDump(signature)+
		"\n transmitted=";
		for(int k=0; k<transmitted.length;k++)
			res+="."+transmitted[k];
		return res;
	}
}
/**
UDPFragmentNAck := IMPLICIT [APPLICATION 15] UDPFragmentNAck;
 * @author msilaghi
 *
 */
class UDPFragmentNAck extends UDPFragmentAck{
	@Override
	public Encoder getEncoder() {
		Encoder enc = super.getEncoder();
		enc.setASN1Type(DD.TAG_AC15);
		return enc;
	}	
	public String toString() {
		String res = "UDPFragNAck: ID="+msgID+"\n sID: "+Util.trimmed(senderID)+
		"\n dID="+Util.trimmed(destinationID)+
		"\n sign="+Util.byteToHexDump(signature)+
		"\n transmitted=";
		for(int k=0; k<transmitted.length;k++)
			res+="."+transmitted[k];
		return res;
	}
}
/**
UDPFragment := IMPLICIT [APPLICATION 12] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgType INTEGER,
	msgID UTF8String,
	fragments INTEGER,
	sequence INTEGER,
	data OCTET STRING
}
 *
 * @author msilaghi
 *
 */
class UDPFragment extends ASNObj {
	String senderID;
	byte[] signature;
	String destinationID;
	String msgID;
	int sequence, fragments, msgType;
	byte[] data;
	//int offset, length;
	/**
UDPFragment := IMPLICIT [APPLICATION 12] SEQUENCE {
	senderID UTF8String,
	signature OCTET STRING,
	destinationID UTF8String,
	msgType INTEGER,
	msgID UTF8String,
	fragments INTEGER,
	sequence INTEGER,
	data OCTET STRING
}
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(senderID));
		enc.addToSequence(new Encoder(signature));
		enc.addToSequence(new Encoder(destinationID));
		enc.addToSequence(new Encoder(msgType));
		enc.addToSequence(new Encoder(msgID));
		enc.addToSequence(new Encoder(fragments));
		enc.addToSequence(new Encoder(sequence));
		enc.addToSequence(new Encoder(data));
		enc.setASN1Type(DD.TAG_AC12);
		return enc;
	}

	@Override
	public UDPFragment decode(Decoder dec) throws ASN1DecoderFail {
		Decoder content=dec.getContent();
		senderID = content.getFirstObject(true).getString();
		signature = content.getFirstObject(true).getBytes();
		destinationID = content.getFirstObject(true).getString();
		msgType = content.getFirstObject(true).getInteger().intValue();
		msgID = content.getFirstObject(true).getString();
		fragments = content.getFirstObject(true).getInteger().intValue();
		sequence = content.getFirstObject(true).getInteger().intValue();
		data = content.getFirstObject(true).getBytes();
		return this;
	}
	public String toString() {
		return "UDPFragment: sID="+Util.trimmed(senderID)+
		"\n signature="+Util.byteToHexDump(signature)+
		"\n dID="+Util.trimmed(destinationID)+
		"\n type="+msgType+
		"\n ID="+msgID+
		"\n fragments="+fragments+
		"\n sequence="+sequence+
		"\n data["+data.length+"]="+Util.byteToHexDump(data);
	}
}
/**
 * UDPEmptyPing = IMPLICIT [APPLICATION 20] SEQUENCE {}
 * @author msilaghi
 *
 */
class UDPEmptyPing extends ASNObj{
	public String toString() {
		return "UDPEmptyPing: ping";
	}
	@Override
	public Encoder getEncoder() {
		return new Encoder().initSequence().setASN1Type(DD.MSGTYPE_EmptyPing);
	}

	@Override
	public UDPEmptyPing decode(Decoder dec) throws ASN1DecoderFail {
		return this;
	}
	
}
class OrgCRL extends ASNObj{
	String org_id;
	byte[] crl;
	Calendar date;
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(org_id, Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(crl));
		enc.addToSequence(new Encoder(date));
		return enc;
	}
	public OrgCRL decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		org_id = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		crl = dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		date = dec.getGeneralizedTimeCalender(Encoder.TAG_GeneralizedTime);
		return this;
	}
}
class ASNTranslation extends ASNObj {
	String id; //Printa
	String object; //UTF
	String object_lang; //Printa
	String context; // Printa
	String translation; // UTF8
	String translation_lang; //Printa
	String translation_flavor; //Printa
	String translation_charset; //Printa
	String submitterID; //printa
	byte[] signature; //OCT STR OPTIONAL
	public String toString(){
		return "ASNTranslation: "+
		"; id="+id+
		"; object="+object+
		"; object_lang="+object_lang+
		"; context="+context+
		"; translation="+translation+
		"; translation_lang="+translation_lang+
		"; translation_flavor="+translation_flavor+
		"; translation_charset="+translation_charset+
		"; submitterID="+submitterID+
		"; signature="+Util.byteToHex(signature, ":")+
		"";
	}
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding ASNTranslation: "+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id,false));
		enc.addToSequence(new Encoder(object));
		enc.addToSequence(new Encoder(object_lang,false));
		enc.addToSequence(new Encoder(context,false));
		enc.addToSequence(new Encoder(translation));
		enc.addToSequence(new Encoder(translation_lang,false));
		enc.addToSequence(new Encoder(translation_flavor,false));
		enc.addToSequence(new Encoder(translation_charset,false));
		enc.addToSequence(new Encoder(submitterID,false));
		if(signature != null) enc.addToSequence(new Encoder(signature));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded Translation: "+this);
		return enc;
	}	
	@Override
	public ASNTranslation decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding TRANSLATION: "+this);
		Decoder dec = decoder.getContent();
		id=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		object=dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		object_lang=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		context=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		
		translation_lang=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);;
		translation_flavor=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		translation_charset=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		submitterID=dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING)
			signature=dec.getFirstObject(true).getBytes(Encoder.TAG_OCTET_STRING);
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding Translation: "+this);
		return this;
	}
}
class ASNNews  extends ASNObj{
	String id; //Printable
	Calendar date;
	String news; //UTF8
	String submitterID; //Printable
	byte[] signature; //OCT STR OPT
	public String toString(){
		return "ASNNews="+
		"; id="+id+
		"; date="+date+
		"; submitterID="+submitterID+
		"; signature="+Util.byteToHex(signature, ":")+
		"; news=\""+news+
		"\"";
	}
	@Override
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(date));
		enc.addToSequence(new Encoder(submitterID, false));
		enc.addToSequence(new Encoder(signature));
		return enc;
	}
	@Override
	public ASNNews decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		id = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		date = dec.getFirstObject(true).getGeneralizedTimeCalender(Encoder.TAG_GeneralizedTime);
		news = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		submitterID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		signature = dec.getFirstObject(true).getBytes(Encoder.TAG_OCTET_STRING);
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding Translation: "+this);
		return this;
	}
}
@Deprecated
class ASNLocationItem extends ASNObj {
	int[] oid; //oid!!!
	String name; //UTF8
	public static String lang;
	public static String hierarchy;
	public String toString() {
		return "ASNLocationItem: "+
		"; oid="+Util.OID2String(oid)+
		"; name="+name+"; lang="+lang+"; hierarchy="+hierarchy+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(oid));
		enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(lang));
		enc.addToSequence(new Encoder(hierarchy));
		return enc;
	}

	@Override
	public ASNLocationItem decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		oid = dec.getFirstObject(true).getOID(Encoder.TAG_OID);
		name= dec.getFirstObject(true, Encoder.TAG_UTF8String).getString();
		lang= dec.getFirstObject(true).getString();
		hierarchy= dec.getFirstObject(true).getString();
		return this;
	}
	public ASNLocationItem instance(){
		return new ASNLocationItem();
	}	
}
@Deprecated
class ASNWitness extends ASNObj{
	String id; //Print
	String hash_witness_alg; //Print
	byte[] hash_witness; //OCT
	int stance; //ENUM
	Calendar date;
	String neighborhoodID; //Print OPT
	boolean sense_Y_N;
	String sourceID; //Print
	String targetID; //Print OPT
	byte[] signature; //OCT Str OPT
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id,false));
		enc.addToSequence(new Encoder(hash_witness_alg, false));
		enc.addToSequence(new Encoder(hash_witness));
		enc.addToSequence(new Encoder(stance));
		enc.addToSequence(new Encoder(date));
		if(this.neighborhoodID!=null) enc.addToSequence(new Encoder(this.neighborhoodID, false));
		enc.addToSequence(new Encoder(sense_Y_N));
		enc.addToSequence(new Encoder(sourceID, false));
		if(targetID!=null) enc.addToSequence(new Encoder(targetID, false));
		if(signature!=null) enc.addToSequence(new Encoder(signature));
		return enc;
	}

	@Override
	public Object decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		id = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_witness_alg = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_witness = dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		stance = dec.getFirstObject(true).getInteger().intValue();
		date = dec.getFirstObject(true).getGeneralizedTimeCalender(Encoder.TAG_GeneralizedTime);
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)
			neighborhoodID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		sense_Y_N = dec.getFirstObject(true).getBoolean();
		sourceID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)
			targetID = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING)
			signature = dec.getFirstObject(true).getBytes();
		if(dec.getTypeByte()!=Encoder.TAG_EOC) throw new ASN1DecoderFail("Redundant objects in ASNWitness");
		return dec;
	}
	public ASNWitness instance(){
		return new ASNWitness();
	}	
	public String toString(){
		return "ASNWitness:"+
		"; id="+id+
		"; hash_witness_alg="+hash_witness_alg+
		"; hash_witness="+Util.byteToHex(hash_witness, ":")+
		"; stance="+stance+
		"; neighborhoodID="+neighborhoodID+
		"; sense_Y_N="+sense_Y_N+
		"; sourceID="+sourceID+
		"; targetID="+targetID+
		"; signature="+Util.byteToHex(signature, ":");
	}
}
class ASNMotion extends ASNObj{
	String id; //Print
	String hash_motion_alg; //Print;
	byte[] hash_motion;
	String motion_title; //UTF8
	String constituentID; //Print
	Calendar date;
	String enhancesID; //Print
	int formatType; //ENUM
	//String status; //Print
	String motion_text; //UTF8
	byte[] signature;
	public String toString() {
		return "ASNMotion: "+
		"; id="+id+
		"; hash_motion_alg="+hash_motion_alg+
		"; hash_motion="+Util.byteToHex(hash_motion, ":")+
		"; constituentID="+constituentID+
		"; date="+Encoder.getGeneralizedTime(date)+
		"; enhancesID="+enhancesID+
		"; formatType="+formatType+
		"; motion_title="+motion_title+
		"; motion_text="+motion_text+
		"; signature="+Util.byteToHex(signature, ":")+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(hash_motion_alg,false));
		enc.addToSequence(new Encoder(hash_motion));
		enc.addToSequence(new Encoder(motion_title));
		enc.addToSequence(new Encoder(constituentID));
		enc.addToSequence(new Encoder(date));
		enc.addToSequence(new Encoder(enhancesID, false));
		enc.addToSequence(new Encoder(formatType));
		enc.addToSequence(new Encoder(motion_text));
		enc.addToSequence(new Encoder(signature));
		return enc;
	}

	@Override
	public Object decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		id=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_motion_alg=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_motion=dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		motion_title=dec.getFirstObject(true, Encoder.TAG_UTF8String).getString();
		constituentID=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		date=dec.getFirstObject(true, Encoder.TAG_GeneralizedTime).getGeneralizedTimeCalenderAnyType();
		enhancesID=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		formatType=dec.getFirstObject(true, Encoder.TAG_INTEGER).getInteger().intValue();
		motion_text=dec.getFirstObject(true, Encoder.TAG_UTF8String).getString();
		signature=dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();		
		return dec;
	}
}
class ASNJustifications extends ASNObj{
	String id; //Print
	String hash_just_alg; //Print
	byte[] hash_just;
	String justification_title; //UTF
	String justification_text; //UTF
	String answerToID; //Print
	String constituentID; //Print
	Calendar date;
	byte[] signature; //OPT
	public String toString(){
		return "ASNJustifications: "+
		"; id="+id+
		"; hash_just_alg="+hash_just_alg+
		"; hash_just="+hash_just+
		"; justification_title="+justification_title+
		"; justification_text="+justification_text+
		"; answerToID="+answerToID+
		"; constituentID="+constituentID+
		"; date="+Encoder.getGeneralizedTime(date)+
		"; signature="+signature+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc=new Encoder().initSequence();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(hash_just_alg, false));
		enc.addToSequence(new Encoder(hash_just));
		enc.addToSequence(new Encoder(justification_title));
		enc.addToSequence(new Encoder(justification_text));
		enc.addToSequence(new Encoder(answerToID, false));
		enc.addToSequence(new Encoder(constituentID, false));
		enc.addToSequence(new Encoder(date));
		if(signature != null)enc.addToSequence(new Encoder(signature));
		return enc;
	}

	@Override
	public ASNJustifications decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec=decoder.getContent();
		id=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_just_alg=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_just=dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		justification_title=dec.getFirstObject(true, Encoder.TAG_UTF8String).getString();
		justification_text=dec.getFirstObject(true, Encoder.TAG_UTF8String).getString();
		answerToID=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		constituentID=dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		date=dec.getFirstObject(true, Encoder.TAG_GeneralizedTime).getGeneralizedTimeCalenderAnyType();
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING) signature=dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		return this;
	}
	
}
class ASNJustificationSets extends ASNObj{
	String motion_id; //Print
	ASNJustifications[] justifications;
	public String toString() {
		return "ASNJustificationSets: "+
		"; motion_id="+motion_id+
		"; justifications=["+Util.concat(justifications, ",")+
		"]";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder();
		enc.addToSequence(new Encoder(motion_id, false));
		enc.addToSequence(Encoder.getEncoder(justifications));
		return enc;
	}

	@Override
	public ASNJustificationSets decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec=decoder.getContent();
		motion_id = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		justifications = dec.getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNJustifications[]{}, new ASNJustifications());
		return this;
	}	
}
class ASNSignature extends ASNObj {
	String id; //Print
	String hash_sig_alg; //Print
	byte[] hash_sig;
	String constituentID;
	Calendar date;
	String justificationID; //Print
	String motionID; //Print
	int status;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id, false));
		enc.addToSequence(new Encoder(hash_sig_alg, false));
		enc.addToSequence(new Encoder(hash_sig));
		enc.addToSequence(new Encoder(constituentID, false));
		enc.addToSequence(new Encoder(date));
		enc.addToSequence(new Encoder(justificationID, false));
		enc.addToSequence(new Encoder(motionID, false));
		enc.addToSequence(new Encoder(status));
		return enc;
	}

	@Override
	public ASNSignature decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		id = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_sig_alg = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		hash_sig = dec.getFirstObject(true, Encoder.TAG_OCTET_STRING).getBytes();
		constituentID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		date = dec.getFirstObject(true, Encoder.TAG_GeneralizedTime).getGeneralizedTimeCalenderAnyType();
		justificationID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		motionID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString();
		status = dec.getFirstObject(true, Encoder.TAG_INTEGER).getInteger().intValue();
		return this;
	}
	public String toString(){
		return "ASNSignature: "+
		"; id="+id+
		"; hash_sig_alg="+hash_sig_alg+
		"; hash_sig="+Util.byteToHex(hash_sig, ":")+
		"; constituentID="+constituentID+
		"; date="+Encoder.getGeneralizedTime(date)+
		"; justificationID="+justificationID+
		"; motionID="+motionID+
		"; status="+status+
		"";
	}
}
class DA_Answer extends ASNObj{
	boolean result;
	public String toString(){
		return "Directory Answer: "+result;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder();
		enc.initSequence();
		enc.addToSequence(new Encoder(result));
		enc.setASN1Type(DD.TAG_AC14);
		return enc;
	}

	@Override
	public DA_Answer decode(Decoder decoder) throws ASN1DecoderFail {
		if(decoder.getTypeByte()!=DD.TAG_AC14) throw new ASN1DecoderFail("Wrong tag");
		Decoder dec = decoder.getContent();
		if(dec.getFirstObject(false)==null) throw new ASN1DecoderFail("Missing boolean: "+decoder.dumpHex());
		result=dec.getFirstObject(true).getBoolean();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		return this;
	}
}
/**
 * ASNUDPPing = IMPLICIT [APPLICATION 13] SEQUENCE {
 * 		senderIsPeer BOOLEAN,
 * 		senderIsInitiator BOOLEAN,
 * 		peer_port INTEGER,
 * 		initiator_port INTEGER,
 * 		peer_domain UTF8String,
 * 		initiator_domain UTF8String,
 * 		peer_globalID	PrintableString OPTIONAL
 * 		initiator_globalID	PrintableString OPTIONAL
 * }
 * @author msilaghi
 *
 */
class ASNUDPPing extends ASNObj{
	boolean senderIsPeer=false;
	boolean senderIsInitiator=false;
	String peer_globalID; // contacted peer ID
	int peer_port;		 // ping sender port
	//String peer_domains[]; // ping sender domains
	String peer_domain; // ping sender domains

	String initiator_globalID; // initiator ID
	int initiator_port;		 // ping initiator port
	public String initiator_domain;
	@Override
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding ASNUDPPing");
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(senderIsPeer));
		enc.addToSequence(new Encoder(senderIsInitiator));
		enc.addToSequence(new Encoder(new BigInteger(""+peer_port)));
		enc.addToSequence(new Encoder(new BigInteger(""+initiator_port)));
		//enc.addToSequence(Encoder.getStringEncoder(peer_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(peer_domain));
		//enc.addToSequence(Encoder.getStringEncoder(initiator_domains, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(initiator_domain));
		if(peer_globalID!=null) enc.addToSequence(new Encoder(peer_globalID, false));
		if(initiator_globalID!=null) enc.addToSequence(new Encoder(initiator_globalID,false));
		enc.setASN1Type(DD.TAG_AC13);
		return enc;
	}
	@Override
	public ASNUDPPing decode(Decoder dec) throws ASN1DecoderFail {
		try{
		Decoder d = dec.getContent();
		senderIsPeer = d.getFirstObject(true).getBoolean();
		senderIsInitiator = d.getFirstObject(true).getBoolean();
		peer_port = d.getFirstObject(true).getInteger().intValue();
		initiator_port = d.getFirstObject(true).getInteger().intValue();
		//peer_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		peer_domain = d.getFirstObject(true).getString();
		//initiator_domains = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		initiator_domain = d.getFirstObject(true).getString();
		peer_globalID = d.getFirstObject(true).getString();
		initiator_globalID = d.getFirstObject(true).getString();
		}catch(RuntimeException e){
			//e.printStackTrace();
			//System.out.println(e+"\n: "+dec);
			throw new ASN1DecoderFail(e+"");
		}
		return this;
	}
	public String toString() {
		return "ASNUDPPing: fromPeer:"+senderIsPeer+" fromInitiator:"+senderIsInitiator
		//+" peer port:"+peer_port+"; domains:"+Util.concat(peer_domains, ",")+"; ID="+peer_globalID
		+"\n peer port:"+peer_port+";\n    domains:"+peer_domain+";\n    ID="+Util.trimmed(peer_globalID)
		//+" initiator port:"+initiator_port+"; domains:"+Util.concat(initiator_domains, ",")+"; ID="+initiator_globalID;
		+"\n initiator port:"+initiator_port+";\n     domains:"+initiator_domain+";\n     ID="+Util.trimmed(initiator_globalID);
	}
}
/**
TableName := IMPLICIT [PRIVATE 0] UTF8String
NULLOCTETSTRING := CHOICE {
	OCTET STRING,
	NULL
}
ASNSyncRequest := IMPLICIT [APPLICATION 7] SEQUENCE {
	version UTF8String, -- currently 2
	lastSnapshot GeneralizedTime OPTIONAL,
	tableNames [APPLICATION 0] SEQUENCE OF TableName OPTIONAL,
	orgFilter [APPLICATION 1] SEQUENCE OF OrgFilter OPTIONAL,
	address [APPLICATION 2] D_PeerAddress OPTIONAL,
	request [APPLICATION 3] SpecificRequest OPTIONAL,
	plugin_msg [APPLICATION 4] D_PluginData OPTIONAL,
	plugin_info [APPLICATION 6] SEQUENCE OF ASNPluginInfo OPTIONAL,
	pushChanges ASNSyncPayload OPTIONAL,
	signature NULLOCTETSTRING, -- prior to version 2 it was [APPLICATION 5] 
}
 * @author msilaghi
 *
 */
public class ASNSyncRequest extends ASNObj implements Summary {
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String version="2";
	public Calendar lastSnapshot;
	public String[] tableNames;
	public OrgFilter[] orgFilter;
	public SpecificRequest request;
	public D_PluginData plugin_msg;
	public ASNPluginInfo plugin_info[];
	public D_PeerAddress address=null; //requester
	//Address directory=null;
	ASNSyncPayload pushChanges=null;
	byte[] signature; // covers version,lastSnapshot,tableNames,orgFilter,address,pushChanges
	public ASNSyncRequest() {
	}
	public String toSummaryString() {
		String result="[SyncReq ver="+version+"] ";
		result = "{lastSnapshot="+Encoder.getGeneralizedTime(lastSnapshot)+"}, ";
		if(orgFilter != null) result += "; orgFilter=["+Util.concat(orgFilter, "; ")+"];";
		if(request != null) result += "; request="+request+"; ";
		if(pushChanges != null) result += "; pushChanges="+pushChanges.toSummaryString();
		return result +"}";
	}
	public String toString() {
		String result="[SyncReq ver="+version+"] ";
		if(lastSnapshot==null) {
			result += "{lastSnapshot=null, ";
		}else{
			result += "{lastSnapshot="+Encoder.getGeneralizedTime(lastSnapshot)+", ";
		}
		if(tableNames == null) result += "tables=null,";
		else{
				result += "[";
				for(int k=0; k<tableNames.length; k++) {
					result+=tableNames[k]+", ";
				}
				result += "],";
		}
		result += "; orgFilter=["+Util.concat(orgFilter, "; ")+"];";
		result += "; address="+address+"; ";
		result += "; request="+request+"; ";
		result += "; plugin_msg="+plugin_msg+"; ";
		result += "; plugin_info="+Util.nullDiscrimArray(plugin_info,"|||")+"; ";
		//if(directory==null) result += "directory=null,";
		result += "; pushChanges="+pushChanges;
		return result +"}";
	}
	public byte[] encode() {
		return getEncoder().getBytes();
	}
	byte getASN1TAG(){
		return DD.TAG_AC7;
	}
	public ASNSyncRequest decode(Decoder decoder) throws ASN1DecoderFail {
		//System.out.println("Decoding ASNSyncReq: "+decoder.dumpHex());
		if(decoder.getTypeByte() != getASN1TAG()) throw new ASN1DecoderFail("No right type");
		Decoder dec = decoder.getContent();
		version = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime){
			lastSnapshot = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		}
		if(dec.getTypeByte()==DD.TAG_AC0){
//			Decoder d_tn = dec.getFirstObject(true, DD.TAG_AC0).getContent();
//			ArrayList<String> tableNames = new ArrayList<String>();
//			for(;;) {
//				Decoder c_tn = d_tn.getFirstObject(true, DD.TYPE_TableName);
//				if(c_tn==null) break;
//				tableNames.add(c_tn.getString(DD.TYPE_TableName));
//			}
//			this.tableNames = tableNames.toArray(new String[]{});
			this.tableNames = dec.getFirstObject(true).getSequenceOf(DD.TYPE_TableName);
		}
		if(dec.getTypeByte()==DD.TAG_AC1){
			orgFilter = dec.getFirstObject(true)
			.getSequenceOf(OrgFilter.getASN1Type(), new OrgFilter[]{}, new OrgFilter());
		}
		if(dec.getTypeByte()==DD.TAG_AC2)
			address = new D_PeerAddress().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC3)
			request = new SpecificRequest().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC4)
			plugin_msg = new D_PluginData().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)
			this.plugin_info = dec.getFirstObject(true)
			.getSequenceOf(ASNPluginInfo.getASN1Type(), new ASNPluginInfo[0], new ASNPluginInfo());
		//if(dec.getTypeByte()==DD.TAG_AC3)directory = new Address().decode(dec);
		if(dec.getTypeByte()==SyncAnswer.getASN1Type()) // DD.TAG_AC8
			this.pushChanges = new SyncAnswer().decode(dec.getFirstObject(true));
		//System.out.println("Got to pushChanges: "+this);
		byte tag_sign = Encoder.TAG_OCTET_STRING;
		if(!this.versionAfter(version,1)) // new versions remains OCTET_STRING
				tag_sign = DD.TYPE_SignSyncReq;
		//		(dec.getTypeByte()==DD.TYPE_SignSyncReq))
		if(dec.getTypeByte() == tag_sign) // optional decoding, but should be always there!!!
			signature = dec.getFirstObject(true).getBytes();
		else
			if(_DEBUG)System.out.println("ASNSyncReq:decode:**********SHOULD HAVE SIGNATURE");

		Decoder rest = dec.getFirstObject(false); 
		if(rest!=null){
			if(_DEBUG)System.out.println("ASNSyncReq:decode:*******************************");
			if(_DEBUG)System.out.println("ASNSyncReq:decode:**********SHOULD HAVE HAD SIGNATURE!");
			if(_DEBUG)System.out.println("ASNSyncReq:decode:*******************************");
			if(_DEBUG)System.out.println("ASNSyncReq:decode: so far got:"+this);
			if(_DEBUG)System.out.println("ASNSyncReq:decode: remains:"+rest);
			//throw new ASN1DecoderFail("Extra Objects in decoder");
		}
		return this;
	}
	/**
TableName := IMPLICIT [PRIVATE 0] UTF8String
NULLOCTETSTRING := CHOICE {
	OCTET STRING,
	NULL
}
ASNSyncRequest := IMPLICIT [APPLICATION 7] SEQUENCE {
	version UTF8String, -- currently 2
	lastSnapshot GeneralizedTime OPTIONAL,
	tableNames [APPLICATION 0] SEQUENCE OF TableName OPTIONAL,
	orgFilter [APPLICATION 1] SEQUENCE OF OrgFilter OPTIONAL,
	address [APPLICATION 2] D_PeerAddress OPTIONAL,
	request [APPLICATION 3] SpecificRequest OPTIONAL,
	plugin_msg [APPLICATION 4] D_PluginData OPTIONAL,
	plugin_info [APPLICATION 6] SEQUENCE OF ASNPluginInfo OPTIONAL,
	pushChanges ASNSyncPayload OPTIONAL,
	signature NULLOCTETSTRING, -- prior to version 2 it was [APPLICATION 5] 
}
	 * @author msilaghi
	 *
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if(lastSnapshot!=null) enc.addToSequence(new Encoder(lastSnapshot).setASN1Type(Encoder.TAG_GeneralizedTime));
		
		if(tableNames!=null) {
			enc.addToSequence(
					Encoder.getStringEncoder(tableNames, DD.TYPE_TableName).setASN1Type(DD.TAG_AC0));
//			Encoder encTableNames = new Encoder().initSequence();
//			for(int i=0; i<tableNames.length; i++) {
//				encTableNames.addToSequence(new Encoder(tableNames[i]).
//					setASN1Type(DD.TYPE_TableName));
//			}
//			enc.addToSequence(encTableNames.setASN1Type(DD.TAG_AC0));
		}
		if(orgFilter!=null) {
			enc.addToSequence(Encoder.getEncoder(orgFilter).setASN1Type(DD.TAG_AC1));
//			
//			Encoder encOF=new Encoder().initSequence();
//			for(int i=0; i<orgFilter.length; i++) {
//				encOF.addToSequence(orgFilter[i].getEncoder());
//			}
//			enc.addToSequence(encOF.setASN1Type(DD.TAG_AC1));
		}
		if(address!=null)
			enc.addToSequence(address.getEncoder().setASN1Type(DD.TAG_AC2));
		if(request!=null)
			enc.addToSequence(request.getEncoder().setASN1Type(DD.TAG_AC3));
		if(plugin_msg!=null)
			enc.addToSequence(plugin_msg.getEncoder().setASN1Type(DD.TAG_AC4));
		if(plugin_info!=null) enc.addToSequence(Encoder.getEncoder(this.plugin_info).setASN1Type(DD.TAG_AC6));
		/*
		if(directory!=null) {
			Encoder encPA = directory.getEncoder();
			enc.addToSequence(encPA.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, (byte)3));			
		}
		*/
		if(pushChanges!=null)
			enc.addToSequence(pushChanges.getEncoder());// DD.TAG_AC8
		
		if(signature!=null) {
			Encoder sign = new Encoder(this.signature);
			if(!versionAfter(version,1)) // In new versions should be OCTET_STRING
				sign.setASN1Type(DD.TYPE_SignSyncReq);
			enc.addToSequence(sign);
		}else{
			if(_DEBUG)System.out.println("ASNSyncReq:encode:*******************************");
			if(_DEBUG)System.out.println("ASNSyncReq:encode:**********SHOULD HAVE HAD SIGNATURE!");
			if(_DEBUG)System.out.println("ASNSyncReq:encode:*******************************");
		}
		enc.setASN1Type(getASN1TAG());
		return enc;
	}
	private boolean versionAfter(String v1, String v2) {
		if(v1==null) return false;
		if(v2==null) return true;
		try{
			int V1 = Integer.parseInt(v1);
			int V2 = Integer.parseInt(v2);
			return V1>V2;
		}catch(Exception e){return false;}
	}
	/**
	 * Tells if V1>V2
	 * false for null V1
	 * Always true for negative V2
	 * @param v1
	 * @param V2
	 * @return
	 */
	public boolean versionAfter(String v1, int V2) {
		if(v1==null) return false;
		if(V2<0) return true;
		try{
			int V1 = Integer.parseInt(v1);
			return V1>V2;
		}catch(Exception e){return false;}
	}
	public boolean verifySignature() {
		if(address==null){
			System.err.println("ASNSyncRequest:verifySignature: null address="+this);
			return false;
		}
		if(address.globalID==null){
			System.err.println("ASNSyncRequest:verifySignature: null address.GID="+this);
			return false;
		}
		ciphersuits.PK pk;
		try {
			pk = ciphersuits.Cipher.getPK(this.address.globalID);
		}catch(Exception e){
			e.printStackTrace();
			if(DD.WARN_OF_FAILING_SIGNATURE_ONRECEPTION){
				Application.warning(_("Failed signature verification with:")+" "+this.address.globalID, _("Failed Signature Verification"));
			}
			System.err.println("ASNSyncRequest:verifySignature: Faulty message="+this);
			return false;
		}
		if(!address.verifySignature(pk)){
			System.err.println("ASNSyncRequest:verifySignature: Faulty msg="+address);
			System.err.println("ASNSyncRequest:verifySignature: Faulty address="+address);
			System.err.println("ASNSyncRequest:verifySignature: Faulty address key="+pk);
			if(0==Application.ask(_("Should we fix wrong signature for your peer address?")+"\n"+address.toSummaryString(),
					_("Wrong Signature"), JOptionPane.YES_NO_OPTION)){
				address.creation_date=Util.CalendargetInstance();
				address.sign(Util.getStoredSK(this.address.globalID));
				try {
					address.storeVerified();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}else
				return false;
		}
		boolean result = verifySignature(pk);
		if(!result) {
			System.err.println("ASNSyncRequest:verifySignature: Faulty message="+this);
			System.err.println("ASNSyncRequest:verifySignature: Faulty msg key="+pk);			
		}
		return result;
		/*
		byte[] sgn = signature;
		signature = new byte[0];
		byte[] msg = this.getEncoder().getBytes();
		signature = sgn;
		return Util.verifySign(msg, ciphersuits.Cipher.getPK(this.address.globalID), sgn);
		*/
	}
	public SK sign() {
		if ((this.address == null) || (this.address.globalID == null)) {
			if(DD.WARN_OF_FAILING_SIGNATURE_ONSEND) {
				Application.warning(
						_("Failure to sign request to remote peers: Peer identity is not sent!\nThis may be due to an internal inconsistency.\n Report a bug, and restart!"),
						_("Failure to sign requests!"));
			}
			return null;
		}
		SK sk = Util.getStoredSK(address.globalID);
		if(DD.WARN_OF_WRONG_SYNC_REQ_SK && !sk.sameAs(DD.getMyPeerSK())){
			Application.warning(
					_("Inconsistency in my identity.\n Report bug code 1762!\n and restart :)"),
					_("Inconsistency in my identity!")
			);			
		}
		sign(sk);
		return sk;
	}
	public void sign(SK sk) {
		//byte[] sgn = signature;
		signature = new byte[0];
		byte[] msg = this.getEncoder().getBytes();
		if(ClientSync.DEBUG)System.out.println("ASR: signing msg ="+msg.length);
		if(ClientSync.DEBUG)System.out.println("ASR: signing msg hash ="+Util.stringSignatureFromByte(Util.simple_hash(msg,Cipher.MD5)));
		if(ClientSync.DEBUG)System.out.println("ASR: signing sk ="+sk);
		signature = Util.sign(msg,sk);
	}
	public boolean verifySignature(ciphersuits.PK pk) {
		byte[] sgn = signature;
		signature = new byte[0];
		byte[] msg = this.getEncoder().getBytes();
		if(ClientSync.DEBUG)System.out.println("ASR:VerSigning msg ="+msg.length);
		if(ClientSync.DEBUG)System.out.println("ASR:VerSigning msg hash ="+Util.stringSignatureFromByte(Util.simple_hash(msg,Cipher.MD5)));
		if(ClientSync.DEBUG)System.out.println("ASR:VerSigning pk ="+pk);
		signature = sgn;
		return Util.verifySign(msg, pk, sgn);
	}
}

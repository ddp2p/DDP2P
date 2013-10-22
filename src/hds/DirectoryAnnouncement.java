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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;

import ciphersuits.Cipher;
import ciphersuits.PK;

import config.DD;
import data.D_PeerAddress;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Encoder;
import ASN1.Decoder;
import util.Util;
/**
DirectoryAnnouncement ::= [APPLICATION 0] IMPLICIT SEQUENCE {
	version INTEGER DEFAULT(0), -- 0 if not provided
	globalID PrintableString,
	date GeneralizedTime OPTIONAL,
	address DirectoryAnnouncement_Address,
	certificate OCTETSTRING OPTIONAL,
	signature OCTETSTRING
}
	 * DirectoryRequest = SEQUENCE {
	 * 		globalID PrintableString,
	 * 		initiator_globalID PrintableString,
	 * 		UDP_port INTEGER
	 * }
 * @author msilaghi
 *
 */
public class DirectoryAnnouncement extends ASNObj{
	public final static byte TAG=0;
	private static final int V1 = 1;
	private static final int V2 = 2;
	public int version = V1; // V2 is new
	public int agent_version[] =  Util.getMyVersion();
	public String globalID;
	public String globalIDhash;
	public String instance;
	public DirectoryAnnouncement_Address address = new DirectoryAnnouncement_Address(version);
	public byte[] challenge;
	public Calendar date;
	public byte[] certificate=new byte[0];
	public byte[] signature=new byte[0];
	
	boolean signed = DD.SIGN_DIRECTORY_ANNOUNCEMENTS;
	
	@Override
	public Encoder getEncoder() {
		switch(version){
		case 0:
		case 1:
			return getEncoder_1();
		case 2:
		default:
			return getEncoder_2();
		}
	}
	public Encoder getEncoder_1() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		if(version!=0) da.addToSequence(new Encoder(version));
		Encoder enc=new Encoder(globalID, false);
		//enc.print();
		da.addToSequence(enc);
		if(date!=null)  da.addToSequence(new Encoder(date));
		da.addToSequence(address.getEncoder());
		if(version==0) da.addToSequence(new Encoder(certificate));
		else if(certificate.length>0) da.addToSequence(new Encoder(certificate, DD.TAG_AC1));
		da.addToSequence(new Encoder(signature));
		return da;
	}
	public Encoder getEncoder_2() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		da.addToSequence(new Encoder(version));
		if(version>=3) da.addToSequence(new Encoder(agent_version).setASN1Type(DD.TAG_AC2));
		if(globalID!=null) da.addToSequence(new Encoder(globalID, false).setASN1Type(DD.TAG_AC3));
		if(globalID == null)
			if(globalIDhash!=null)
				da.addToSequence(new Encoder(globalIDhash, false).setASN1Type(DD.TAG_AC4));
		if(instance!=null) da.addToSequence(new Encoder(instance, false).setASN1Type(DD.TAG_AC5));
		if(address!=null)da.addToSequence(address.getEncoder());
		if(signed) {
			if(date!=null)  da.addToSequence(new Encoder(date));
			if(challenge.length>0) da.addToSequence(new Encoder(challenge, DD.TAG_AC6));
			if(certificate.length>0) da.addToSequence(new Encoder(certificate, DD.TAG_AC1));
			if(signature.length>0)da.addToSequence(new Encoder(signature, DD.TAG_AC7));
		}
		return da;
	}
	@Override
	public DirectoryAnnouncement decode(Decoder d) throws ASN1DecoderFail {
		Decoder dec = d.getContent(); version = 0;
		if(dec.getTypeByte() == Encoder.TAG_INTEGER) version =  dec.getFirstObject(true).getInteger().intValue();
		switch(version){
		case 0:
		case 1:
			return decode_1(dec);
		case 2:
		default:
			return decode_2(dec);
		}
	}
	public DirectoryAnnouncement decode_1(Decoder dec) throws ASN1DecoderFail {
		globalID=dec.getFirstObject(true).getString();
		if(dec.getTypeByte() == Encoder.TAG_GeneralizedTime) date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		//out.println("ID="+globalID);
		Decoder addr = dec.getFirstObject(true).getContent();
		//addr.printHex("addr=");
		//address.addresses = addr.getFirstObject(true).getString();
		String addresses = addr.getFirstObject(true).getString();
		address.setAddresses(addresses);
		//out.println("domain: "+address.domain);
		//addr.printHex("addr=");
		address.udp_port = addr.getFirstObject(true).getInteger().intValue();
		//out.println("port: "+address.port);
		//addr.printHex("addr=");
		if(version==0) certificate = dec.getFirstObject(true).getBytes();
		else if(dec.getTypeByte() == DD.TAG_AC1)
			//version =  dec.getFirstObject(true).getInteger().intValue();
			 certificate =  dec.getFirstObject(true).getBytes();
		signature = dec.getBytes();
		return this;
	}
	public DirectoryAnnouncement decode_2(Decoder dec) throws ASN1DecoderFail {
		if(dec.isFirstObjectTagByte(DD.TAG_AC2))
			agent_version = dec.getFirstObject(true).getIntsArray();
		if(dec.isFirstObjectTagByte(DD.TAG_AC3)) {
			globalID=dec.getFirstObject(true).getString();
			globalIDhash = D_PeerAddress.getGIDHashFromGID(globalID);
		}
		if(dec.isFirstObjectTagByte(DD.TAG_AC4))
			globalIDhash = dec.getFirstObject(true).getString();
		if(dec.isFirstObjectTagByte(DD.TAG_AC5))
			instance = dec.getFirstObject(true).getString();

		if(dec.isFirstObjectTagByte(DirectoryAnnouncement_Address.getASN1Tag())) // TAG_AC10
			address = new DirectoryAnnouncement_Address(version).decode(dec);
		
//		Decoder addr = dec.getFirstObject(true).getContent();
//		//addr.printHex("addr=");
//		address.setAddresses(addr.getFirstObject(true).getString());
//		//out.println("domain: "+address.domain);
//		//addr.printHex("addr=");
//		address.udp_port = addr.getFirstObject(true).getInteger().intValue();
//		//out.println("port: "+address.port);
//		//addr.printHex("addr=");
		if(dec.getTypeByte() == Encoder.TAG_GeneralizedTime) date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		if(dec.getTypeByte() == DD.TAG_AC6) challenge =  dec.getFirstObject(true).getBytes();
		if(dec.getTypeByte() == DD.TAG_AC1) certificate =  dec.getFirstObject(true).getBytes();
		if(dec.getTypeByte() == DD.TAG_AC7) signature = dec.getFirstObject(true).getBytes();
		return this;
	}
	public DirectoryAnnouncement() {}
	public DirectoryAnnouncement(byte[] buffer, int peek,
			InputStream is) throws IOException, ASN1DecoderFail {
		int bytes = peek;
		if(peek==0) bytes = is.read(buffer);
		Decoder dec = new Decoder(buffer);
		int msglen = dec.contentLength()+dec.typeLen()+dec.lenLen();
		while(msglen > bytes){
			int inc = is.read(buffer, bytes, buffer.length-bytes);
			if(inc<0) throw new IOException("Too short data: "+bytes+" vs. "+msglen+" peek="+peek+" inc="+inc);
			bytes+= inc;
			dec = new Decoder(buffer);
		}
		decode(dec);
	}
	public DirectoryAnnouncement(Decoder dec) throws ASN1DecoderFail {
		decode(dec);
	}
	public String toString() {
		String result=" ID="+Util.trimmed(globalID)+"\n address="+address+"\n certif='"+Util.byteToHexDump(certificate)+"'\n sign='"+Util.byteToHexDump(signature)+"'";
		return result;
	}
	public String toSummaryString() {
		String result=" ID="+Util.getGIDhash(globalID)+"\n address="+address+"'";
		return result;
	}
	public boolean verifySignature() {
		byte[] sign = signature;
		String GID = this.globalID;
		String GIDhash = this.globalIDhash;
		signature = new byte[0];
		globalID = null;
		globalIDhash = null;
		PK pk = Cipher.getPK(globalID);
		boolean r = Util.verifySign(this, pk, sign);

		signature = sign;
		globalID = GID;
		globalIDhash = GIDhash;
		return r;
	}
}

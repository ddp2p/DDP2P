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

import config.DD;

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
	public int version=1;
	public String globalID;
	public DirectoryAnnouncement_Address address = new DirectoryAnnouncement_Address();
	Calendar date;
	byte[] certificate=new byte[0];
	byte[] signature=new byte[0];
	
	@Override
	public Encoder getEncoder() {
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
	@Override
	public Object decode(Decoder d) throws ASN1DecoderFail {
		Decoder dec = d.getContent(); version = 0;
		if(dec.getTypeByte() == Encoder.TAG_INTEGER) version =  dec.getFirstObject(true).getInteger().intValue();
		globalID=dec.getFirstObject(true).getString();
		if(dec.getTypeByte() == Encoder.TAG_GeneralizedTime) date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		//out.println("ID="+globalID);
		Decoder addr = dec.getFirstObject(true).getContent();
		//addr.printHex("addr=");
		address.addresses = addr.getFirstObject(true).getString();
		//out.println("domain: "+address.domain);
		//addr.printHex("addr=");
		address.udp_port = addr.getFirstObject(true).getInteger().intValue();
		//out.println("port: "+address.port);
		//addr.printHex("addr=");
		if(version==0) certificate = dec.getFirstObject(true).getBytes();
		else if(dec.getTypeByte() == DD.TAG_AC1) version =  dec.getFirstObject(true).getInteger().intValue();
		signature = dec.getBytes();
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
}

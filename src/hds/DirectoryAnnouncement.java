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
import static java.lang.System.out;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;

import ASN1.ASN1DecoderFail;
import ASN1.Encoder;
import ASN1.Decoder;
import util.Util;
/**
 * 	 * DirectoryAnnouncement = IMPLICIT [APPLICATION 0] SEQUENCE {
	 * 		globalID PrintableString,
	 * 		date GeneralizedTime OPTIONAL,
	 * 		address DirectoryAnnouncement_Address,
	 * 		certificate OCTETSTRING,
	 * 		signature OCTETSTRING
	 * }
	 * DirectoryRequest = SEQUENCE {
	 * 		globalID PrintableString,
	 * 		initiator_globalID PrintableString,
	 * 		UDP_port INTEGER
	 * }
 * @author msilaghi
 *
 */
public class DirectoryAnnouncement{
	public final static byte TAG=0;
	public String globalID;
	public DirectoryAnnouncement_Address address = new DirectoryAnnouncement_Address();
	Calendar date;
	byte[] certificate=new byte[0];
	byte[] signature=new byte[0];
	
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
	private void decode(Decoder dec) throws ASN1DecoderFail {
		dec = dec.getContent();
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
		certificate = dec.getFirstObject(true).getBytes();
		signature = dec.getBytes();		
	}
	public String toString() {
		String result=" ID="+Util.trimmed(globalID)+"\n address="+address+"\n certif='"+Util.byteToHexDump(certificate)+"'\n sign='"+Util.byteToHexDump(signature)+"'";
		return result;
	}
	/**
	 * @return
	 */
	public byte[] encode() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		Encoder enc=new Encoder(globalID, false);
		//enc.print();
		da.addToSequence(enc);
		if(date!=null)  da.addToSequence(new Encoder(date));
		da.addToSequence(address.getEncoder())
		.addToSequence(new Encoder(certificate))
		.addToSequence(new Encoder(signature));
		return da.getBytes();
	}
}

class DirectoryRequest {
	private static final boolean DEBUG = false;
	public String globalID;
	public String initiator_globalID;
	public int UDP_port;
	public String toString() {
		return "DirectoryRequest: gID="+Util.trimmed(globalID)+
		       "\n  from:"+Util.trimmed(initiator_globalID)+"\n  UDPport="+UDP_port;
	}
	byte buffer[] = new byte[DirectoryServer.MAX_DR_DA];
	public DirectoryRequest(byte[]_buffer, int peek, InputStream is) throws Exception {
		assert(DirectoryServer.MAX_DR_DA>=peek);
		Decoder dec=new Decoder(_buffer);
		if(dec.contentLength()>DirectoryServer.MAX_DR_DA) throw new Exception("Max buffer DirectoryServer.MAX_DR_DA="+DirectoryServer.MAX_DR_DA+
				" is smaller than request legth: "+dec.contentLength());
		Encoder.copyBytes(this.buffer, 0, _buffer, peek, 0);
		read(_buffer, peek, is);
	}
	public DirectoryRequest(InputStream is) throws Exception {
		read(buffer, 0, is);
	}
	public DirectoryRequest(String global_peer_ID, String ini_ID, int udp) {
		globalID = global_peer_ID;
		this.initiator_globalID = ini_ID;
		this.UDP_port = udp;
	}
	void read(byte[]buffer, int peek, InputStream is)  throws Exception{
		if(DEBUG)out.println("dirRequest read: ["+peek+"]="+ Util.byteToHexDump(buffer, peek));
		int bytes=peek;
		if(peek==0){
			bytes=is.read(buffer);
			out.println("dirRequest reread: ["+bytes+"]="+ Util.byteToHexDump(buffer, " "));
		}
		int content_length, type_length, len_length, request_length;
		if (bytes<1){
			out.println("dirRequest exiting: bytes<1 ="+bytes);
			return;
		}
		Decoder asn = new Decoder(buffer);
		if(asn.type()!=Encoder.TYPE_SEQUENCE){
			out.println("dirRequest exiting, not sequence: ="+asn.type());
			return;
		}
		do{
			type_length = asn.typeLen();
			if(type_length <=0) {
				out.println("dirRequest reread type ="+type_length);
				if(bytes == DirectoryServer.MAX_DR_DA) throw new Exception("Buffer Type exceeded!");
				if(is.available()<=0)  throw new Exception("Data not available for type!");
				bytes += is.read(buffer, bytes, DirectoryServer.MAX_DR_DA-bytes);
			}
		}while(type_length <= 0);
		if(DEBUG)out.println(" dirRequest type ="+type_length);
		do{
			len_length = asn.lenLen();
			if(len_length <=0) {
				out.println("dirRequest reread len len ="+len_length);
				if(bytes == DirectoryServer.MAX_DR_DA) throw new Exception("Buffer Length exceeded!");
				if(is.available()<=0)  throw new Exception("Data not available for length!");
				bytes += is.read(buffer, bytes, DirectoryServer.MAX_DR_DA-bytes);
			}
		}while(len_length <= 0);
		if(DEBUG)out.println(" dirRequest len len ="+len_length);
		content_length = asn.contentLength();
		request_length = content_length + type_length + len_length;
		if(DEBUG)out.println(" dirRequest req_len ="+request_length);
		if(request_length > DirectoryServer.MAX_LEN){
			throw new Exception("Buffer Content exceeded!");
		}
		byte[] buffer_all = buffer;
		if(bytes < request_length) {
			buffer_all = new byte[request_length];
			Encoder.copyBytes(buffer_all, 0, buffer, bytes);
			do{
				//if(is.available()<=0)  throw new Exception("Data not available for length!");;
				bytes += is.read(buffer_all,bytes,request_length - bytes);
			}while(bytes < request_length);
		}
		
		Decoder dr = new Decoder(buffer_all);
		dr = dr.getContent();
		globalID = dr.getFirstObject(true).getString();
		initiator_globalID = dr.getFirstObject(true).getString();
		this.UDP_port = dr.getFirstObject(true).getInteger().intValue();
	}
	/**
	 * @return
	 */
	byte[] encode() {
		return 
		new Encoder().
		initSequence().
		addToSequence(new Encoder(globalID, false)).
		addToSequence(new Encoder(this.initiator_globalID, false)).
		addToSequence(new Encoder(this.UDP_port)).
		getBytes();
	}
}

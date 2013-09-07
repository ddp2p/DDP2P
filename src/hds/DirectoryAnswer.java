/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import config.DD;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
/**
Payment_Request ::= SEQUENCE {
  method INTEGER,
  amount REAL
}
DIR_Terms_Requested ::= SEQUENCE {
	version INTEGER DEFAULT 0,
	topic [APPLICATION 1] BOOLEAN OPTIONAL,
	payment [APPLICATION 2] Payment_Request OPTIONAL,
	services_available [APPLICATION 3] BITSTRING OPTIONAL,
	ad [APPLICATION 4] BOOLEAN OPTIONAL,
	plaintext [APPLICATION 5] BOOLEAN OPTIONAL,
}
-- Answer to DirectoryRequest
DirectoryAnswer ::= SEQUENCE {
	version INTEGER DEFAULT 0,
	timestamp GeneralizedTime,
	remote_GIDhash PrintableString OPTIONAL,
	addresses_enc SEQUENCE OF SEQUENCE {
		domain UTF8String,
		tcp_port INTEGER,
		udp_port INTEGER
	} OPTIONAL,
	terms [APPLICATION 4] SEQUENCE OF DIR_Terms_Requested OPTIONAL
}
 * @author msilaghi
 *
 */
class DIR_Payment_Request extends ASNObj{
	float amount;
	String method;
	@Override
	public Encoder getEncoder() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DIR_Payment_Request decode(Decoder dec) throws ASN1DecoderFail {
		// TODO Auto-generated method stub
		return null;
	}
}
class DIR_Terms_Requested extends ASNObj {
	public int version=0;
	public String topic;
	public DIR_Payment_Request payment;
	public byte[] services_available;
	public int ad=-1, plaintext=-1;
	@Override
	public ASNObj instance() throws CloneNotSupportedException{return (ASNObj) new DIR_Terms_Requested();}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(version != 0) enc.addToSequence(new Encoder(version));
		if(topic != null) enc.addToSequence(new Encoder(topic).setASN1Type(DD.TAG_AP1));
		if(payment != null) enc.addToSequence(payment.getEncoder().setASN1Type(DD.TAG_AC2));
		if(services_available != null) enc.addToSequence(new Encoder(services_available).setASN1Type(DD.TAG_AP3));
		if(ad > 0) enc.addToSequence(new Encoder(ad).setASN1Type(DD.TAG_AP4));
		if(plaintext > 0) enc.addToSequence(new Encoder(plaintext).setASN1Type(DD.TAG_AP5));
		return enc.setASN1Type(getASN1Type());
	}
	@Override
	public DIR_Terms_Requested decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent(); version = 0;
		if(d.getTypeByte()==Encoder.TAG_INTEGER) version = d.getFirstObject(true).getInteger().intValue();
		if(d.getTypeByte()==DD.TAG_AP1) topic = d.getFirstObject(true).getString();
		if(d.getTypeByte()==DD.TAG_AC2) payment = new DIR_Payment_Request().decode(d.getFirstObject(true));
		if(d.getTypeByte()==DD.TAG_AP3) services_available = d.getFirstObject(true).getBytesAnyType();
		if(d.getTypeByte()==DD.TAG_AP4) ad = d.getFirstObject(true).getInteger().intValue();
		if(d.getTypeByte()==DD.TAG_AP5) plaintext = d.getFirstObject(true).getInteger().intValue();
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
}
public class DirectoryAnswer extends ASNObj {
	static final int MAX_DA = 1000;
	private static final int MAX_LEN = 10000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 2;
	String remote_GIDhash;
	public Calendar date = Util.CalendargetInstance();
	//ArrayList<Address> address=new ArrayList<InetSocketAddress>();
	public ArrayList<Address> addresses=new ArrayList<Address>();
	public DIR_Terms_Requested[] terms;
	
	@Override
	public Encoder getEncoder() {
		Encoder da = new Encoder().initSequence();
		if(version != 0) da.addToSequence(new Encoder(version));
		switch(version) {
		case 0: return getEncoder_0(da);
		case 1: return getEncoder_1(da);
		case 2:
		default:
			return getEncoder_2(da);
		}
	}
	private Encoder getEncoder_2(Encoder da) {
		da.addToSequence(new Encoder(date));
		da.addToSequence(Encoder.getEncoder(addresses));
		if(terms!=null) da.addToSequence(Encoder.getEncoder(terms).setASN1Type(DD.TAG_AC4));
		if(remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash));
		return da;
	}
	private Encoder getEncoder_1(Encoder da) {
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		if(remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash, false));//v1
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k=0; k<this.addresses.size(); k++) {
			Address cA = this.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));//.toString().split(":")[0]));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if(DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		if((terms!=null)) da.addToSequence(Encoder.getEncoder(terms).setASN1Type(DD.TAG_AC4));//v1
		return da;
	}
	private Encoder getEncoder_0(Encoder da) {
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k=0; k<this.addresses.size(); k++) {
			Address cA = this.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));//.toString().split(":")[0]));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if(DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		return da;
	}
	@Override
	public DirectoryAnswer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder dec_da_content=dec.getContent();
		version = 0;
		if(dec_da_content.getTypeByte()==Encoder.TAG_INTEGER)
			version = dec_da_content.getFirstObject(true).getInteger().intValue();
		switch(version) {
		case 0:
			return decode_0(dec_da_content);
		case 1:
			return decode_1(dec_da_content);
		case 2:
		default:
			return decode_2(dec_da_content);
		}
	}
	
	private DirectoryAnswer decode_2(Decoder d) throws ASN1DecoderFail {
		date = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		if(d.isFirstObjectTagByte(DD.TAG_AC4))
			terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		if(d.isFirstObjectTagByte(DD.TAG_AC5))
			remote_GIDhash = d.getFirstObject(true).getString();
		return this;
	}
	private DirectoryAnswer decode_1(Decoder dec_da_content) throws ASN1DecoderFail {
		String gdate = dec_da_content.getFirstObject(true).
				getGeneralizedTime(Encoder.TAG_GeneralizedTime);
		if(DEBUG) out.println("Record date: "+gdate);
		//date.setTime(new Date(gdate));
		date = Util.getCalendar(gdate);
		if((dec_da_content.getTypeByte()==Encoder.TAG_PrintableString))
			remote_GIDhash = dec_da_content.getFirstObject(true).getString();
		Decoder dec_addresses = dec_da_content.getFirstObject(true).getContent();
		while(dec_addresses.type()!=Encoder.TAG_EOC) {
			if(DEBUG) out.println("Reading record: "+dec_addresses.dumpHex());
			Decoder dec_addr = dec_addresses.getFirstObject(true).getContent();
			String domain = dec_addr.getFirstObject(true).getString();
			if(DEBUG) out.println("domain: "+domain);
			if(DEBUG) out.println("port object: "+dec_addr.dumpHex());
			int tcp_port = dec_addr.getFirstObject(true).getInteger().intValue();
			if(DEBUG) out.println("tcp_port: "+tcp_port);
			int udp_port = dec_addr.getInteger().intValue();
			if(DEBUG) out.println("udp_port: "+udp_port);
			//address.add(new InetSocketAddress(domain, port));
			addresses.add(new Address(domain, tcp_port,udp_port));
		}
		if((dec_da_content.getTypeByte()==DD.TAG_AC4))
			terms = dec_da_content.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		return this;
	}
	private DirectoryAnswer decode_0(Decoder dec_da_content) throws ASN1DecoderFail {
		String gdate = dec_da_content.getFirstObject(true).
				getGeneralizedTime(Encoder.TAG_GeneralizedTime);
		if(DEBUG) out.println("Record date: "+gdate);
		//date.setTime(new Date(gdate));
		date = Util.getCalendar(gdate);
		Decoder dec_addresses = dec_da_content.getFirstObject(true).getContent();
		while(dec_addresses.type()!=Encoder.TAG_EOC) {
			if(DEBUG) out.println("Reading record: "+dec_addresses.dumpHex());
			Decoder dec_addr = dec_addresses.getFirstObject(true).getContent();
			String domain = dec_addr.getFirstObject(true).getString();
			if(DEBUG) out.println("domain: "+domain);
			if(DEBUG) out.println("port object: "+dec_addr.dumpHex());
			int tcp_port = dec_addr.getFirstObject(true).getInteger().intValue();
			if(DEBUG) out.println("tcp_port: "+tcp_port);
			int udp_port = dec_addr.getInteger().intValue();
			if(DEBUG) out.println("udp_port: "+udp_port);
			//address.add(new InetSocketAddress(domain, port));
			addresses.add(new Address(domain, tcp_port,udp_port));
		}
		return this;
	}
	public DirectoryAnswer() {}
	public String toString() {
		String result = Encoder.getGeneralizedTime(date)+" @#"+addresses.size()+" [";
		for(int k=0; k<addresses.size(); k++) {
			result += addresses.get(k)+",";
		}
		return result+"]";
	}
	public DirectoryAnswer(InputStream is) throws Exception {
		byte[] buffer = new byte[MAX_DA];
		int len=is.read(buffer);
		Decoder dec_da = new Decoder(buffer);
		int req_len=dec_da.objectLen();
		if(req_len > MAX_LEN)  throw new Exception("Unacceptable package!");
		if(req_len<0) throw new Exception("Not enough bytes received in first package!");
		if(req_len>len) {
			if(dec_da.objectLen()>MAX_DA) {
				byte[] old_buffer = buffer;
				buffer = new byte[req_len];
				Encoder.copyBytes(buffer, 0, old_buffer, len, 0);
			}
			len += is.read(buffer, len, req_len-len);
			if(req_len!=len) throw new Exception("Not enough bytes received!");
			dec_da = new Decoder(buffer);
		}
		decode(dec_da);
	}
}
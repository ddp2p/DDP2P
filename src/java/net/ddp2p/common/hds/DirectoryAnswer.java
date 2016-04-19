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
package net.ddp2p.common.hds;
/**
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
 */
import static java.lang.System.out;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;
public class DirectoryAnswer extends ASNObj {
	static final int MAX_DA = 1000;
	public static final int MAX_LEN = 10000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 2;
	boolean known = false;
	int[] agent_version;
	String remote_GIDhash;
	String instance;
	public Calendar date;
	public ArrayList<Address> addresses;
	public DIR_Terms_Requested[] terms;
	byte[] signature_peer = new byte[0];
	byte[] signature_directory;
	public String toLongString() {
		String result = "DirectoryAnswer[v="+version+" known="+known+
				" av="+Util.concat(agent_version, ".", "NULL")+
				" date="+Encoder.getGeneralizedTime(date)+
				" remoteGIDH="+remote_GIDhash+
				" instance="+instance+
				" signature_peer="+Util.byteToHexDump(signature_peer)+
				" signature_dir="+Util.byteToHexDump(signature_directory)+
				" terms#"+((terms==null)?"null":terms.length)+
				" @#"+((addresses==null)?"null":addresses.size())+
				" [";
		for(int k=0; k<addresses.size(); k++) {
			result += addresses.get(k)+",";
		}
		for(int k=0; k<terms.length; k++) {
			result += terms[k]+",";
		}
		return result+"]]";
	}
	void init() {
		date = Util.CalendargetInstance();
		addresses=new ArrayList<Address>();
		signature_peer = new byte[0];
		signature_directory = new byte[0];
	}
	void init_is() {
		known = true;
		addresses=new ArrayList<Address>();
		signature_peer = new byte[0];
		signature_directory = new byte[0];
	}
	/**
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
	 */
	@Override
	public Encoder getEncoder() {
		Encoder da = new Encoder().initSequence();
		if (version != 0) da.addToSequence(new Encoder(version));
		switch (version) {
		case 0: da = getEncoder_0(da); break;
		case 1: da = getEncoder_1(da); break;
		case 2:
			da = getEncoder_2(da); break;
		case 3:
			da = getEncoder_3(da); break;
		default:
			da = getEncoder_3(da); break;
		}
		return da;
	}
	/**
-- Answer to V3
DirectoryAnswer ::= SEQUENCE {
        agent_version [AC2] SEQUENCE OF INTEGER OPTIONAL,
        date GeneralizedTime,
        addresses SEQUENCE OF Address,
        terms [APPLICATION 4] SEQUENCE OF DIR_Terms_Requested OPTIONAL,
        remote_GIDhash [AC5] PrintableString OPTIONAL,
        instance [AC6] UTF8String OPTIONAL,
		signature_peer [AC7] NULLOCTETSTRING OPTIONAL,
		signature_directory [AC8] NULLOCTETSTRING OPTIONAL,
}
	 * @param da
	 * @return
	 */
	private Encoder getEncoder_3(Encoder da) {
		if (agent_version != null) da.addToSequence(Encoder.getEncoderArray(agent_version).setASN1Type(DD.TAG_AC2));
		da.addToSequence(new Encoder(date));
		da.addToSequence(Encoder.getEncoder(addresses));
		if (terms != null) da.addToSequence(Encoder.getEncoder(terms).setASN1Type(DD.TAG_AC4));
		if (remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash).setASN1Type(DD.TAG_AC5));
		if (instance != null) da.addToSequence(new Encoder(instance).setASN1Type(DD.TAG_AC6));
		if (signature_peer.length > 0) da.addToSequence(new Encoder(signature_peer, DD.TAG_AC7));
		if (signature_directory.length > 0) da.addToSequence(new Encoder(signature_directory, DD.TAG_AC8));
		return da;
	}
	private Encoder getEncoder_2(Encoder da) {
		da.addToSequence(new Encoder(date));
		da.addToSequence(Encoder.getEncoder(addresses));
		if(terms!=null) da.addToSequence(Encoder.getEncoder(terms).setASN1Type(DD.TAG_AC4));
		if(remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash).setASN1Type(DD.TAG_AC5));
		return da;
	}
	private Encoder getEncoder_1(Encoder da) {
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		if(remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash, false));
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k=0; k<this.addresses.size(); k++) {
			Address cA = this.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if(DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		if((terms!=null)) da.addToSequence(Encoder.getEncoder(terms).setASN1Type(DD.TAG_AC4));
		return da;
	}
	private Encoder getEncoder_0(Encoder da) {
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k=0; k<this.addresses.size(); k++) {
			Address cA = this.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));
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
			return decode_2(dec_da_content);
		case 3:
			return decode_3(dec_da_content);
		default:
			return decode_3(dec_da_content);
		}
	}
	private DirectoryAnswer decode_3(Decoder d) throws ASN1DecoderFail {
		if(d.isFirstObjectTagByte(DD.TAG_AC2))
			agent_version = d.getFirstObject(true).getIntsArray();
		date = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		if(d.isFirstObjectTagByte(DD.TAG_AC4))
			terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		if(d.isFirstObjectTagByte(DD.TAG_AC5))
			remote_GIDhash = d.getFirstObject(true).getString();
		if(d.isFirstObjectTagByte(DD.TAG_AC6))
			instance = d.getFirstObject(true).getString();
		if(d.isFirstObjectTagByte(DD.TAG_AC7))
			signature_peer = d.getFirstObject(true).getBytes();
		if(d.isFirstObjectTagByte(DD.TAG_AC8))
			signature_directory = d.getFirstObject(true).getBytes();
		return this;
	}
	private DirectoryAnswer decode_2(Decoder d) throws ASN1DecoderFail {
		date = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		Decoder d_addr = d.getFirstObject(true);
		if(d_addr == null) {
			addresses = new ArrayList<Address>();
			return this;
		}
		addresses = d_addr.getSequenceOfAL(Address.getASN1Type(), new Address());
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
			addresses.add(new Address(domain, tcp_port,udp_port));
		}
		return this;
	}
	public DirectoryAnswer() {init();}
	public String toString() {
		String result = "[date="+Encoder.getGeneralizedTime(date)+" @#"+addresses.size()+" [";
		for(int k=0; k<addresses.size(); k++) {
			result += addresses.get(k)+",";
		}
		return result+"]]";
	}
	public DirectoryAnswer(InputStream is) throws Exception {
		init_is();
		byte[] buffer = Util.readASN1Message(is, MAX_DA, MAX_LEN); 
		Decoder dec_da = new Decoder(buffer);
		decode(dec_da);
	}
}

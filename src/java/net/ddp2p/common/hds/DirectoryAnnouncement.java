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
 package net.ddp2p.common.hds;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.Util;
public class DirectoryAnnouncement extends ASNObj{
	public final static byte TAG=0;
	private static final int V1 = 1;
	private static final int V2 = 2;
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	/**
	 * Version up to (but not equal to) which the structure-version is V1
	 */
	public static final String AGENT_VERSION_V1 = "0.9.50";
	//public int version = V1; // V2 is new
	//private int address_version;
	public int agent_version[] =  DD.getMyVersion();
	/**
	 * Only one of the GID or GIDH is needed. GIDH can be obtained from GID. For efficiency send GIDH
	 * when you are sure that the server already has the GID (e.g. if one has pre-registered with it). 
	 */
	private String myGID;
	private String myGIDH;
	public String branch;
	public String name;
	public String instance;
	public DirectoryAnnouncement_Address address = new DirectoryAnnouncement_Address();
	/**
	 * A challenge may have been received from the directory server to avoid repeat attacks.
	 */
	public byte[] challenge;
	/**
	 * This is the date when the signature is made
	 */
	public Calendar date;
	/**
	 * A certificate is a X509 certificate or some other signature of this GID with a key recognized by the directory server.
	 * I think this is not yet fully implemented
	 */
	public byte[] certificate=new byte[0];
	public byte[] signature=new byte[0];
	
	boolean signed = DD.SIGN_DIRECTORY_ANNOUNCEMENTS;
	
	public String toString() {
		String result =
				"DA [agent_version="+Util.concat(agent_version, ".", "NULL")+"\n"+
						" branch=" + branch+"\n"+
						" name=" + name+"\n"+
						" ID="+Util.trimmed(getGID())+"\n"+
						" IDh="+Util.trimmed(getGIDH())+"\n"+
						" instance="+Util.trimmed(instance)+"\n"+
						" date ="+Encoder.getGeneralizedTime(date)+"\n"+
						" address="+address+"\n"+
						" certif='"+Util.byteToHexDump(certificate)+"'\n"+
						" challenge='"+Util.byteToHexDump(challenge)+"\n"+
						" sign='"+Util.byteToHexDump(signature)+"'"+
						"]";
		return result;
	}
	public String toSummaryString() {
		String result = "I="+instance+" B="+branch+" AV="+Util.concat(agent_version, ".", "NULL")+" ID="+Util.getGIDhash(getGID())+"\n address="+address+"'";
		return result;
	}
	public void setVersion(int version) {
		address.version = version;
	}
	/**
	 * For the version of the embedded address structures
	 * @param version
	 */
	public void setAddressVersion(int version) {
		address.address_version = version;
	}
	public int getVersion(){return address.version;}
	public int getAddressVersion(){return address.address_version;}
	
	@Override
	public Encoder getEncoder() {
		Encoder enc;
		switch(getVersion()){
		case 0:
			enc = getEncoder_0();
			if (DEBUG) System.out.println("DA:0");
			break;
		case 1:
			enc = getEncoder_1();
			if (DEBUG) System.out.println("DA:1");
			break;
		case 2:
		default:
			if (DEBUG) System.out.println("DA:2");
			enc = getEncoder_2();
		}
		/*
		if (_DEBUG) System.out.println("DA:"+this);
		
		
		Decoder dec = new Decoder(enc.getBytes());
		try {
			DirectoryAnnouncement da = new DirectoryAnnouncement(dec);
			if (_DEBUG) System.out.println("DA:decoded:"+da);
		} catch (ASN1DecoderFail e) {
			//System.out.println("DA:encoded="+this);
			e.printStackTrace();
		}
		*/
		
		return enc;
	}
	/**
	DirectoryAnnouncement ::= [AC0] IMPLICIT SEQUENCE {
		version INTEGER OPTIONAL DEFAULT(0), -- 0 if not provided
		globalID PrintableString,
		date GeneralizedTime OPTIONAL DEFAULT (NULL),
		address DirectoryAnnouncement_Address,
		certificate NULLOCTETSTRING,
		signature NULLOCTETSTRING
	}
	 * @author msilaghi
	 *
	 */
	public Encoder getEncoder_0() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		Encoder enc = new Encoder(getGID(), false);
		da.addToSequence(enc);
		if (date != null)  da.addToSequence(new Encoder(date));
		da.addToSequence(address.getEncoder());
		da.addToSequence(new Encoder(certificate));
		da.addToSequence(new Encoder(signature));
		return da;
	}
	/**
	DirectoryAnnouncement ::= [AC0] IMPLICIT SEQUENCE { --V1
		version INTEGER OPTIONAL DEFAULT(0)
		globalID PrintableString,
		date GeneralizedTime OPTIONAL DEFAULT (NULL),
		address DirectoryAnnouncement_Address,
		certificate [AC1] IMPLICIT NULLOCTETSTRING OPTIONAL -- byte[0] otherwise,
		signature NULLOCTETSTRING
	}

	 * @return
	 */
	public Encoder getEncoder_1() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		da.addToSequence(new Encoder(getVersion()));
		
		Encoder enc=new Encoder(getGID(), false);
		//enc.print();
		da.addToSequence(enc);
		
		if(date!=null)  da.addToSequence(new Encoder(date));
		
		da.addToSequence(address.getEncoder());
		
		if (certificate != null && certificate.length > 0) da.addToSequence(new Encoder(certificate, DD.TAG_AC1));
		//System.out.println("DA:Enc sign="+Util.byteToHexDump(signature));
		da.addToSequence(new Encoder(signature));
		return da;
	}
/**
	DirectoryAnnouncement ::= [AC0] IMPLICIT SEQUENCE { --V2
		version INTEGER OPTIONAL DEFAULT(0)
		agent_version [AC2] IMPLICIT SEQUENCE OF INTEGER,
		CHOICE {
			globalID [AC3] IMPLICIT PrintableString OPTIONAL,
			globalID_hash [AC4] IMPLICIT PrintableString OPTIONAL,
		}
		instance [AC5] IMPLICIT PrintableString OPTIONAL,
		address DirectoryAnnouncement_Address OPTIONAL,
		date GeneralizedTime OPTIONAL DEFAULT (NULL),
		challenge [AC6] IMPLICIT NULLOCTETSTRING OPTIONAL -- byte[0] otherwise,
		certificate [AC1] IMPLICIT NULLOCTETSTRING OPTIONAL -- byte[0] otherwise,
		signature [AC7] IMPLICIT NULLOCTETSTRING OPTIONAL -- byte[0] otherwise,
	}

 * @return
 */
	public Encoder getEncoder_2() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		da.addToSequence(new Encoder(getVersion()));
		if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added version: "+getVersion());
		// V3?
		da.addToSequence(Encoder.getEncoderArray(agent_version).setASN1Type(DD.TAG_AC2));
		if (getGID() != null) da.addToSequence(new Encoder(getGID(), false).setASN1Type(DD.TAG_AC3));
		if (getGID() == null)
			if (getGIDH() != null)
				da.addToSequence(new Encoder(getGIDH(), false).setASN1Type(DD.TAG_AC4));
		if (instance != null) da.addToSequence(new Encoder(instance, false).setASN1Type(DD.TAG_AC5));
		if (address != null) da.addToSequence(address.getEncoder()); // has tag AC10
		if (signed) {
			if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added signed");
			if (date != null) {
				da.addToSequence(new Encoder(date));
				if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added date=");
			}
			if (challenge != null && challenge.length > 0) {
				da.addToSequence(new Encoder(challenge, DD.TAG_AC6));
				if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added date=");
			}
			if (certificate != null && certificate.length > 0) {
				da.addToSequence(new Encoder(certificate, DD.TAG_AC1));
			}
			if (signature != null && signature.length > 0) da.addToSequence(new Encoder(signature, DD.TAG_AC7));
		}
		if (branch != null) da.addToSequence(new Encoder(branch, false).setASN1Type(DD.TAG_AC8));
		if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added branch="+branch);
		if (name != null) da.addToSequence(new Encoder(name, false).setASN1Type(DD.TAG_AC9));
		if (DEBUG) System.out.println("directoryAnnouncement: getEncoder2: Added name="+name);
		return da;
	}
	/**
	 * Encoder without signature and GID/GIDH (and with sign=true)
	 * @return
	 */
	public Encoder getSignatureEncoder() {
		Encoder da = new Encoder()
		.initSequence()
		.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, DirectoryAnnouncement.TAG);
		//da.print();
		da.addToSequence(new Encoder(getVersion())); // V3?
		da.addToSequence(Encoder.getEncoderArray(agent_version).setASN1Type(DD.TAG_AC2));
//		if (globalID != null) da.addToSequence(new Encoder(globalID, false).setASN1Type(DD.TAG_AC3));
//		if (globalID == null)
//			if (globalIDhash != null)
//				da.addToSequence(new Encoder(globalIDhash, false).setASN1Type(DD.TAG_AC4));
		if (instance != null) da.addToSequence(new Encoder(instance, false).setASN1Type(DD.TAG_AC5));
		if (address != null) da.addToSequence(address.getEncoder());
		//if (signed) {
			if (date != null)  da.addToSequence(new Encoder(date));
			if (challenge != null && challenge.length > 0) da.addToSequence(new Encoder(challenge, DD.TAG_AC6));
			if (certificate != null && certificate.length > 0) da.addToSequence(new Encoder(certificate, DD.TAG_AC1));
			//if (signature.length > 0) da.addToSequence(new Encoder(signature, DD.TAG_AC7));
		//}
		if (branch != null) da.addToSequence(new Encoder(branch, false).setASN1Type(DD.TAG_AC8));
		if (name != null) da.addToSequence(new Encoder(name, false).setASN1Type(DD.TAG_AC9));
		return da;
	}
	@Override
	public DirectoryAnnouncement decode(Decoder d) throws ASN1DecoderFail {
		Decoder dec = d.getContent();
		setVersion(0);
		if (dec.getTypeByte() == Encoder.TAG_INTEGER)
			setVersion ( dec.getFirstObject(true).getInteger().intValue() );
		switch ( getVersion() ) {
		case 0:
			if (DEBUG) System.out.println("DA:dec:0");
			return decode_0(dec);
		case 1:
			if (DEBUG) System.out.println("DA:dec:1");
			return decode_1(dec);
		case 2:
		default:
			if (DEBUG) System.out.println("DA:dec:2");
			return decode_2(dec);
		}
	}
	public DirectoryAnnouncement decode_0(Decoder dec) throws ASN1DecoderFail {
		setGID(dec.getFirstObject(true).getString());
		if (dec.getTypeByte() == Encoder.TAG_GeneralizedTime)
			date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		//Decoder addr = dec.getFirstObject(true).getContent();
		//String addresses = addr.getFirstObject(true).getString();
		//address.setAddresses(addresses);
		//address.udp_port = addr.getFirstObject(true).getInteger().intValue();
		address = new DirectoryAnnouncement_Address(getVersion()).decode(dec);
		certificate = dec.getFirstObject(true).getBytes();
		signature = dec.getBytes();
		return this;
	}
	public DirectoryAnnouncement decode_1(Decoder dec) throws ASN1DecoderFail {
		setGID(dec.getFirstObject(true).getString());
		if (dec.getTypeByte() == Encoder.TAG_GeneralizedTime)
			date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		//Decoder addr = dec.getFirstObject(true).getContent();
		//String addresses = addr.getFirstObject(true).getString();
		//address.setAddresses(addresses);
		//address.udp_port = addr.getFirstObject(true).getInteger().intValue();
		address = new DirectoryAnnouncement_Address(getVersion()).decode(dec.getFirstObject(true));
		if (dec.getTypeByte() == DD.TAG_AC1)
			 certificate =  dec.getFirstObject(true).getBytes(DD.TAG_AC1);
		//System.out.println("DA:old sign="+Util.byteToHexDump(signature));
		signature = dec.getFirstObject(true).getBytes();
		//System.out.println("DA:Got sign="+Util.byteToHexDump(signature));
		return this;
	}
	public DirectoryAnnouncement decode_2(Decoder dec) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("directoryAnnouncement: decoder2: start="+dec.getTypeByte());
		if(dec.isFirstObjectTagByte(DD.TAG_AC2))
			agent_version = dec.getFirstObject(true).getIntsArray();
		if(dec.isFirstObjectTagByte(DD.TAG_AC3)) {
			setGID(dec.getFirstObject(true).getString(DD.TAG_AC3));
			setGIDH(D_Peer.getGIDHashFromGID(getGID()));
		}
		if(dec.isFirstObjectTagByte(DD.TAG_AC4))
			setGIDH(dec.getFirstObject(true).getString(DD.TAG_AC4));
		if(dec.isFirstObjectTagByte(DD.TAG_AC5))
			instance = dec.getFirstObject(true).getString(DD.TAG_AC5);

		if (DEBUG) System.out.println("directoryAnnouncement: decoder2: before address="+dec.getTypeByte());
		if (dec.isFirstObjectTagByte(DirectoryAnnouncement_Address.getASN1Tag())) { // TAG_AC10
			address = new DirectoryAnnouncement_Address(getVersion());
			//System.out.println("DA:decode_2:addr="+address);
			address.decode(dec.getFirstObject(true));
		}
		if (DEBUG) System.out.println("directoryAnnouncement: decoder2: Got decoder");
		if (dec.getTypeByte() == Encoder.TAG_GeneralizedTime) date = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no date="+dec.getTypeByte());

		if (dec.getTypeByte() == DD.TAG_AC6) challenge = dec.getFirstObject(true).getBytes(DD.TAG_AC6);
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no challenge="+dec.getTypeByte());

		if (dec.getTypeByte() == DD.TAG_AC1) certificate = dec.getFirstObject(true).getBytes(DD.TAG_AC1);
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no certificate="+dec.getTypeByte());

		if (dec.getTypeByte() == DD.TAG_AC7) signature = dec.getFirstObject(true).getBytes(DD.TAG_AC7);
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no signature="+dec.getTypeByte());

		if (dec.isFirstObjectTagByte(DD.TAG_AC8))
			branch = dec.getFirstObject(true).getString(DD.TAG_AC8);
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no branch="+dec.getTypeByte());

		if (dec.isFirstObjectTagByte(DD.TAG_AC9))
			name = dec.getFirstObject(true).getString(DD.TAG_AC9);
		else if (DEBUG) System.out.println("directoryAnnouncement: decoder2: no name="+dec.getTypeByte());
		if (DEBUG) System.out.println("directoryAnnouncement: decoder2: done?="+dec.getTypeByte());
		return this;
	}
	public DirectoryAnnouncement() {}
	public DirectoryAnnouncement(String dir_address) {
		setAddressVersionForDestination(new Address(dir_address));
	}
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
	public boolean verifySignature() {
		PK pk = Cipher.getPK(getGID());
		boolean r = false;
		switch(getVersion()) {
		case 0:
		case 1: 
			{
				byte[] sign = signature;
				String GID = this.getGID();
				String GIDhash = this.getGIDH();
				signature = new byte[0];
				setGID(null);
				setGIDH(null);
				r = Util.verifySign(this, pk, sign);
				signature = sign;
				setGID(GID);
				setGIDH(GIDhash);
			}
			break;
		case 2:
		default:
			byte[] msg = this.getSignatureEncoder().getBytes();
			r = Util.verifySign(msg, pk, signature);
		}
		return r;
	}
	public byte[] sign() {
		assert(getVersion() >= 2);
		this.date = Util.CalendargetInstance();
		byte[] msg = this.getSignatureEncoder().getBytes();
		this.signature = Util.sign(msg, net.ddp2p.common.data.HandlingMyself_Peer.getMyPeerSK());
		this.signed = true;
		return this.signature;
	}
	/**
	 * If adr.branch is not same as DD.BRANCH and both no-null, use version V1.
	 * If adr.agent_version is older than DirectoryAnnouncement.AGENT_VERSION_V1 (0.9.50), use V1
	 * else use V2 with setVersion(V2)
	 * @param adr
	 * @return
	 * Returns the current version
	 */
	public int setAddressVersionForDestination(Address adr) {
		setAddressVersion ( Address.getStructureVersion(adr) );
		if (DEBUG) System.out.println("DirectoryAnnouncement:setAddressVersionForDestination: address_version="+getAddressVersion());
		if (! Util.equalStrings_or_one_null(adr.branch, DD.BRANCH)) {
			this.setVersion (V1);
			if (DEBUG) System.out.println("DirectoryAnnouncement:setAddressVersionForDestination: branch return="+getVersion());
			return getVersion();
		}
		if (Util.isVersionNewer(AGENT_VERSION_V1, adr.agent_version)) {
			setVersion (V1);
			if (DEBUG) System.out.println("DirectoryAnnouncement:setAddressVersionForDestination: newer return="+getVersion());
			return getVersion();
		}
		setVersion (V2);
		if (DEBUG) System.out.println("DirectoryAnnouncement:setAddressVersionForDestination: return="+getVersion());
		return getVersion();
	}
	public String getGID() {
		return myGID;
	}
	public void setGID(String gID) {
		this.myGID = gID;
	}
	public String getGIDH() {
		return myGIDH;
	}
	public void setGIDH(String gIDH) {
		this.myGIDH = gIDH;
	}
}

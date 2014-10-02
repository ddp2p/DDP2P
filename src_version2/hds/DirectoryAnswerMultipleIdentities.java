package hds;

import static java.lang.System.out;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import util.Util;

import config.Application;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
Payment_Request ::= SEQUENCE {
 method INTEGER,
 amount REAL
}
Dir_Terms_Requested ::= SEQUENCE {
	version OPTIONAL INTEGER DEFAULT(0),
	topic [AP1] OPTIONAL UTF8String DEFAULT(NULL),
	payment [AC2] OPTIONAL Dir_Payment_Request DEFAULT(NULL),
	services_available [AP3] OPTIONAL OCTETSTRING DEFAULT(NULL),
	ad [AP4] OPTIONAL INTEGER DEFAULT(0),
	plaintext [AP5] INTEGER DEFAULT (0)
}
DirectoryAnswerInstance ::= SEQUENCE {
	instance UTF8String,
	branch UTF8String,
	agent_version SEQUENCE OF INTEGER,
	date_last_contact GeneralizedTime,
	instance_terms SEQUENCE OF Dir_Terms_Requested,
	signature_peer OCTETSTRING
}
DirectoryAnswerMultipleIdentities ::= [AC19] SEQUENCE {
	version INTEGER,
	known BOOLEAN,
	remote_GIDhash PrintableString,
	date_most_recent_among_instances [AP1] IMPLICIT OPTIONAL GeneralizedTime,
	global_terms [AC2] IMPLICIT OPTIONAL SEQUENCE OF Dir_Terms_Requested,
	instances [AC3] SEQUENCE OF DirectoryAnswerInstance,
	signature_directory OCTET STRING
}
*/
public class DirectoryAnswerMultipleIdentities extends ASNObj {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int V0 = 0;
	public static final int V1 = 1;
	public static final int V2 = 2;
	public static final int V3 = 3;
	int version = V3;
	boolean known = false;
	String remote_GIDhash;
	public DIR_Terms_Requested[] global_terms;
	byte[] signature_directory = new byte[0];
	public Calendar date_most_recent_among_instances;
	/**
	 * Send only instances that qualified (e.g., info free or pre-approved)
	 */
	public ArrayList<DirectoryAnswerInstance> instances = new ArrayList<DirectoryAnswerInstance>();
	public String directory_domain;
	public int directory_udp_port;
	
	@Override
	public String toString(){
		String r = "DirAnswerMI: [";
		r += "\n\tversion="+version+" known="+known;
		r += "\n\tremote_GIDhash="+remote_GIDhash;
		if (global_terms != null) {
			for (int i = 0; i < global_terms.length; i++) {
				r += "global_terms "+i+": "+global_terms[i];
			}
		}
		r += "\n\tdate_most_recent_among_instances="+Encoder.getGeneralizedTime(date_most_recent_among_instances);
		r += "\n\tsignature_directory="+Util.byteToHexDump(signature_directory);
		for ( DirectoryAnswerInstance inst : instances) {
			r += "\n\t instances:"+inst;
		}
		r += "]";
		return r;
	}
	public DirectoryAnswerMultipleIdentities() {}
	public DirectoryAnswerMultipleIdentities(Decoder dec_da) throws ASN1DecoderFail {
		this.decode(dec_da);
	}
	public DirectoryAnswerMultipleIdentities(InputStream is) throws Exception {
		//init_is();
		byte[] buffer = Util.readASN1Message(is, DirectoryAnswer.MAX_DA, DirectoryAnswer.MAX_LEN); 
		Decoder dec_da = new Decoder(buffer);
		decode(dec_da);
	}	
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
			System.err.println("DirectoryAnswerMultipleIndentity:encoder: Unknown version = "+this.version);
			da = getEncoder_3(da); break;
		}
		da.setASN1Type(getASN1Type());
		return da;
	}
	/**
--- V3
DirectoryAnswerMultipleIdentities ::= [AC19] SEQUENCE {
	known BOOLEAN,
	remote_GIDhash PrintableString,
	date_most_recent_among_instances [AP1] GeneralizedTime OPTIONAL,
	global_terms [AC2] SEQUENCE OF DIR_Terms_Requested OPTIONAL
	instances [AC3] SEQUENCE OF DirectoryAnswerInstance,
	signature_directory NULLOCTETSTRING
}
	 * @param enc
	 * @return
	 */
	private Encoder getEncoder_3(Encoder enc) {
		//Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(known));
		enc.addToSequence(new Encoder(remote_GIDhash, false));
		if (date_most_recent_among_instances != null)
			enc.addToSequence(new Encoder(date_most_recent_among_instances).setASN1Type(DD.TAG_AP1));
		if (global_terms != null) 
			enc.addToSequence(Encoder.getEncoder(global_terms).setASN1Type(DD.TAG_AC2));
		enc.addToSequence(Encoder.getEncoder(instances).setASN1Type(DD.TAG_AC3));
		if (this.directory_domain != null) enc.addToSequence(new Encoder(directory_domain).setASN1Type(DD.TAG_AP4));
		if (this.directory_udp_port > 0) enc.addToSequence(new Encoder(directory_udp_port).setASN1Type(DD.TAG_AP5));
		
		enc.addToSequence(new Encoder(signature_directory));
		return enc;
	}
	
	/*
	private Encoder getEncoder_3b(Encoder da) {
		if(instances.size() <= 0) {
			da.addToSequence(Encoder.getEncoder(new ArrayList<Address>()));
			return da;
		}
		DirectoryAnswerInstance dia = instances.get(0);
		if (dia.agent_version!=null) da.addToSequence(Encoder.getEncoderArray(dia.agent_version).setASN1Type(DD.TAG_AC2));
		da.addToSequence(new Encoder(dia.date_last_contact));
		da.addToSequence(Encoder.getEncoder(dia.addresses));
		if (dia.instance_terms!=null) da.addToSequence(Encoder.getEncoder(dia.instance_terms).setASN1Type(DD.TAG_AC4));
		if (remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash).setASN1Type(DD.TAG_AC5));
		if (dia.instance != null) da.addToSequence(new Encoder(dia.instance).setASN1Type(DD.TAG_AC6));
		if (dia.signature_peer.length > 0) da.addToSequence(new Encoder(dia.signature_peer, DD.TAG_AC7));
		if (signature_directory.length > 0) da.addToSequence(new Encoder(signature_directory, DD.TAG_AC8));
		return da;
	}
*/	
	private Encoder getEncoder_2(Encoder da) {
		if(instances.size() <= 0) {
			da.addToSequence(Encoder.getEncoder(new ArrayList<Address>()));
			return da;
		}
		DirectoryAnswerInstance dia = instances.get(0);

		da.addToSequence(new Encoder(dia.date_last_contact));
		da.addToSequence(Encoder.getEncoder(dia.addresses));
		if (dia.instance_terms!=null) da.addToSequence(Encoder.getEncoder(dia.instance_terms).setASN1Type(DD.TAG_AC4));
		if (remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash).setASN1Type(DD.TAG_AC5));
		return da;
	}
	private Encoder getEncoder_1(Encoder da) {
		if(instances.size() <= 0) {
			da.addToSequence(Encoder.getEncoder(new ArrayList<Address>()));
			return da;
		}
		DirectoryAnswerInstance dia = instances.get(0);
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		if(remote_GIDhash != null) da.addToSequence(new Encoder(remote_GIDhash, false));//v1
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k = 0; k < dia.addresses.size(); k++) {
			Address cA = dia.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));//.toString().split(":")[0]));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if(DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		if((dia.instance_terms != null)) da.addToSequence(Encoder.getEncoder(dia.instance_terms).setASN1Type(DD.TAG_AC4));//v1
		return da;
	}
	/**
	 * @param da
	 * @return
	 */
	private Encoder getEncoder_0(Encoder da) {
		if (instances.size() <= 0) {
			da.addToSequence(Encoder.getEncoder(new ArrayList<Address>()));
			return da;
		}
		DirectoryAnswerInstance dia = instances.get(0);
		da.addToSequence(new Encoder(Util.CalendargetInstance()));
		Encoder addresses_enc = new Encoder().initSequence();
		for (int k = 0; k < dia.addresses.size(); k ++) {
			Address cA = dia.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));//.toString().split(":")[0]));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if (DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		return da;
	}

	@Override
	public DirectoryAnswerMultipleIdentities decode(Decoder dec) throws ASN1DecoderFail {
		Decoder dec_da_content=dec.getContent();
		version = 0;
		if (dec_da_content.getTypeByte()==Encoder.TAG_INTEGER)
			version = dec_da_content.getFirstObject(true).getInteger().intValue();
		switch (version) {
		case 0:
			return decode_0(dec_da_content);
		case 1:
			return decode_1(dec_da_content);
		case 2:
			return decode_2(dec_da_content);
		case 3:
			//return decode_3b(dec_da_content);
		//case 4:
			return decode_3(dec_da_content);
		default:
			//Application.warning(_("Got Directory Answer"), title)
			throw new ASN1DecoderFail("DirectoryAnswer unknown version:"+version);
			//return decode_4(dec_da_content);
		}
	}

	private DirectoryAnswerMultipleIdentities decode_3(Decoder d) throws ASN1DecoderFail {
		//Decoder d = dec.getContent();
		//version = d.getFirstObject(true).getInteger().intValue();
		known = d.getFirstObject(true).getBoolean();
		remote_GIDhash = d.getFirstObject(true).getString();
		if (d.isFirstObjectTagByte(DD.TAG_AP1))
			date_most_recent_among_instances = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		if (d.isFirstObjectTagByte(DD.TAG_AC2))
			global_terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[0], new DIR_Terms_Requested());
		if (d.isFirstObjectTagByte(DD.TAG_AC3))
			instances = d.getFirstObject(true).getSequenceOfAL(DirectoryAnswerInstance.getASN1Type(), new DirectoryAnswerInstance()); 
		if (d.isFirstObjectTagByte(DD.TAG_AP4))
			this.directory_domain = d.getFirstObject(true).getString(DD.TAG_AP4);
		if (d.isFirstObjectTagByte(DD.TAG_AP5))
			this.directory_udp_port = d.getFirstObject(true).getInteger(DD.TAG_AP5).intValue();
		signature_directory = d.getFirstObject(true).getBytes();
		return this;
	}
	/*
	private DirectoryAnswerMultipleIdentities decode_3b(Decoder d) throws ASN1DecoderFail {
		DirectoryAnswerInstance dai = new DirectoryAnswerInstance();
		if(d.isFirstObjectTagByte(DD.TAG_AC2))
			dai.agent_version = d.getFirstObject(true).getIntsArray();
		dai.date_last_contact = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		dai.addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		if(d.isFirstObjectTagByte(DD.TAG_AC4))
			dai.instance_terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		if(d.isFirstObjectTagByte(DD.TAG_AC5))
			remote_GIDhash = d.getFirstObject(true).getString();
		if(d.isFirstObjectTagByte(DD.TAG_AC6))
			dai.instance = d.getFirstObject(true).getString();
		if(d.isFirstObjectTagByte(DD.TAG_AC7))
			dai.signature_peer = d.getFirstObject(true).getBytes();
		if(d.isFirstObjectTagByte(DD.TAG_AC8))
			signature_directory = d.getFirstObject(true).getBytes();
		this.instances.add(dai);
		return this;
	}
	*/
	private DirectoryAnswerMultipleIdentities decode_2(Decoder d) throws ASN1DecoderFail {
		DirectoryAnswerInstance dai = new DirectoryAnswerInstance();
		dai.date_last_contact = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		Decoder d_addr = d.getFirstObject(true);
		if(d_addr == null) {
			dai.addresses = new ArrayList<Address>();
			return this;
		}
		dai.addresses = d_addr.getSequenceOfAL(Address.getASN1Type(), new Address());
		if(d.isFirstObjectTagByte(DD.TAG_AC4))
			dai.instance_terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		if(d.isFirstObjectTagByte(DD.TAG_AC5))
			remote_GIDhash = d.getFirstObject(true).getString();
		this.instances.add(dai);
		return this;
	}
	private DirectoryAnswerMultipleIdentities decode_1(Decoder dec_da_content) throws ASN1DecoderFail {
		DirectoryAnswerInstance dai = new DirectoryAnswerInstance();
		String gdate = dec_da_content.getFirstObject(true).
				getGeneralizedTime(Encoder.TAG_GeneralizedTime);
		if(DEBUG) out.println("Record date: "+gdate);
		//date.setTime(new Date(gdate));
		dai.date_last_contact = Util.getCalendar(gdate);
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
			dai.addresses.add(new Address(domain, tcp_port,udp_port));
		}
		if((dec_da_content.getTypeByte()==DD.TAG_AC4))
			dai.instance_terms = dec_da_content.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[]{}, new DIR_Terms_Requested());
		this.instances.add(dai);
		return this;
	}
	private DirectoryAnswerMultipleIdentities decode_0(Decoder dec_da_content) throws ASN1DecoderFail {
		DirectoryAnswerInstance dai = new DirectoryAnswerInstance();
		String gdate = dec_da_content.getFirstObject(true).
				getGeneralizedTime(Encoder.TAG_GeneralizedTime);
		if(DEBUG) out.println("Record date: "+gdate);
		//date.setTime(new Date(gdate));
		dai.date_last_contact = Util.getCalendar(gdate);
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
			dai.addresses.add(new Address(domain, tcp_port,udp_port));
		}
		this.instances.add(dai);
		return this;
	}
	/**
	 * TAG_AC19
	 * @return
	 */
	public static byte getASN1Type() {
		return DD.TAG_AC19;
	}

}
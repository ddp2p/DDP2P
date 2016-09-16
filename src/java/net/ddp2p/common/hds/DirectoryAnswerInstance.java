package net.ddp2p.common.hds;

import java.util.ArrayList;
import java.util.Calendar;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;

/**
DirectoryAnswerInstance SEQUENCE {
	instance UTF8String,
	branch UTF8String,
	agent_version SEQUENCE OF INTEGER,
	date_last_contact GeneralizedTime,
	instance_terms SEQUENCE OF Dir_Terms_Requested,
	addresses SEQUENCE OF Address,
	signature_peer OCTETSTRING
}
*/
public 
class DirectoryAnswerInstance extends ASNObj {
	public String instance;
	int[] agent_version;
	String branch;
	public Calendar date_last_contact;
	public DIR_Terms_Requested[] instance_terms;
	byte[] signature_peer = new byte[0];
	/**
	 * addresses filled only if terms met (pre-approved)
	 */
	public ArrayList<Address> addresses = new ArrayList<Address>();
	
	@Override
	public String toString () {
		String r = "DirAnsInst [";
		r += "\t\ninstance="+instance;
		r += "\t\nbranch="+branch;
		r += "\t\nagent_version="+Util.concat(agent_version,".","NULL");
		r += "\t\nsignature_peer="+Util.byteToHexDump(signature_peer);
		r += "\t\ndate_last_contact="+Encoder.getGeneralizedTime(date_last_contact);
		if (instance_terms != null) {
			for (int i = 0; i < instance_terms.length; i++) {
				r += "\t\ninstance_terms="+instance_terms[i];
			}
		}
		r += "\t\n addr="+Util.concat(addresses, " , ", "NULL");
		return r+"]";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(instance));
		enc.addToSequence(new Encoder(branch));
		enc.addToSequence(Encoder.getEncoderArray(agent_version));
		if (date_last_contact == null) date_last_contact = Util.CalendargetInstance();
		enc.addToSequence(new Encoder(date_last_contact));
		enc.addToSequence(Encoder.getEncoder(instance_terms));
		enc.addToSequence(Encoder.getEncoder(addresses));
		enc.addToSequence(new Encoder(this.signature_peer));
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	@Override
	public DirectoryAnswerInstance decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		instance = d.getFirstObject(true).getString();
		branch = d.getFirstObject(true).getString();
		agent_version = d.getFirstObject(true).getIntsArray();
		date_last_contact = d.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		instance_terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Requested.getASN1Type(), new DIR_Terms_Requested[0], new DIR_Terms_Requested());
		addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		signature_peer = d.getFirstObject(true).getBytes();
		return this;
	}
	public DirectoryAnswerInstance instance() {
		return new DirectoryAnswerInstance();
	}
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
}
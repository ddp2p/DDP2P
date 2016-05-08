package net.ddp2p.common.hds;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;
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
public class UDPFragment extends ASNObj {
	private String senderID;
	byte[] signature;
	private String destinationID;
	private String msgID;
	private int sequence;
	private int fragments;
	private int msgType;
	private byte[] data;
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
		enc.addToSequence(new Encoder(getSenderID()));
		enc.addToSequence(new Encoder(signature));
		enc.addToSequence(new Encoder(getDestinationID()));
		enc.addToSequence(new Encoder(getMsgType()));
		enc.addToSequence(new Encoder(getMsgID()));
		enc.addToSequence(new Encoder(getFragments()));
		enc.addToSequence(new Encoder(getSequence()));
		enc.addToSequence(new Encoder(getData()));
		enc.setASN1Type(DD.TAG_AC12);
		return enc;
	}
	@Override
	public UDPFragment decode(Decoder dec) throws ASN1DecoderFail {
		Decoder content=dec.getContent();
		setSenderID(content.getFirstObject(true).getString());
		signature = content.getFirstObject(true).getBytes();
		setDestinationID(content.getFirstObject(true).getString());
		setMsgType(content.getFirstObject(true).getInteger().intValue());
		setMsgID(content.getFirstObject(true).getString());
		setFragments(content.getFirstObject(true).getInteger().intValue());
		setSequence(content.getFirstObject(true).getInteger().intValue());
		setData(content.getFirstObject(true).getBytes());
		return this;
	}
	public String toString() {
		return "UDPFragment: sID="+Util.trimmed(getSenderID())+
		"\n signature="+Util.byteToHexDump(signature)+
		"\n dID="+Util.trimmed(getDestinationID())+
		"\n type="+getMsgType()+
		"\n ID="+getMsgID()+
		"\n fragments="+getFragments()+
		"\n sequence="+getSequence()+
		"\n data["+getData().length+"]="+Util.byteToHexDump(getData());
	}
	public String getMsgID() {
		return msgID;
	}
	public void setMsgID(String msgID) {
		this.msgID = msgID;
	}
	public int getMsgType() {
		return msgType;
	}
	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}
	public int getFragments() {
		return fragments;
	}
	public void setFragments(int fragments) {
		this.fragments = fragments;
	}
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public String getDestinationID() {
		return destinationID;
	}
	public void setDestinationID(String destinationID) {
		this.destinationID = destinationID;
	}
	public String getSenderID() {
		return senderID;
	}
	public void setSenderID(String senderID) {
		this.senderID = senderID;
	}
}

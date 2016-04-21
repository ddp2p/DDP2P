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
	String senderID;
	byte[] signature;
	String destinationID;
	String msgID;
	int sequence, fragments, msgType;
	byte[] data;
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

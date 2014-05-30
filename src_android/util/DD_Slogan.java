package util;

import java.util.regex.Pattern;

import config.DD;
import data.D_Peer;
import data.HandlingMyself_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class DD_Slogan extends ASNObj implements StegoStructure {
	String version = "1";
	String slogan;
	@Override
	public void save() throws P2PDDSQLException {
		D_Peer me = HandlingMyself_Peer.get_myself();
		me = D_Peer.getPeerByPeer_Keep(me);
		me.setSlogan(slogan);
		me.setCreationDate();
		me.storeAct();
		me.releaseReference();
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		Decoder d = new Decoder(asn1);
		decode(d);
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return slogan;
	}

	@Override
	public String getString() {
		return "DD_Slogan:"+version+" "+slogan;
	}

	@Override
	public boolean parseAddress(String content) {
		String s[] = content.split(Pattern.quote(" "), 2);
		if (s.length > 1) slogan = s[1];
		return true;
	}

	@Override
	public short getSignShort() {
		return DD.STEGO_SLOGAN;
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(slogan));
		return enc;
	}

	@Override
	public DD_Slogan decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		slogan = d.getFirstObject(true).getString();
		return this;
	}
	
}
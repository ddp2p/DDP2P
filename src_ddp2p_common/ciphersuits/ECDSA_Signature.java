package ciphersuits;

import java.math.BigInteger;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public
class ECDSA_Signature extends ASNObj{
	BigInteger r;
	BigInteger s;
	public String toString(){
		return "ECDSA_SG[r="+Util.toString16(r)+" s="+Util.toString16(s)+"]";
	}
	ECDSA_Signature(BigInteger r, BigInteger s) {
		this.r = r;
		this.s = s;
	}
	public ECDSA_Signature(byte[] signature) throws ASN1DecoderFail {
		this.decode(new Decoder(signature));
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(r));
		enc.addToSequence(new Encoder(s));
		return enc;
	}
	@Override
	public ECDSA_Signature decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		r = d.getFirstObject(true).getInteger();
		s = d.getFirstObject(true).getInteger();
		return this;
	}
}
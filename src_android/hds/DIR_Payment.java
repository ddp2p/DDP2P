package hds;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public
class DIR_Payment extends ASNObj{
	float amount;
	String method;
	String details;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(amount));
		enc.addToSequence(new Encoder(method, false));
		enc.addToSequence(new Encoder(details, false));
		return enc;
	}
	@Override
	public DIR_Payment decode(Decoder dec) throws ASN1DecoderFail {
		Decoder dr = dec.getContent();
		amount = dr.getFirstObject(true).getInteger().floatValue();
		method = dr.getFirstObject(true).getString();
		details = dr.getFirstObject(true).getString();
		return this;
	}
	public boolean satisfies(DIR_Payment_Request payment) {
		return amount >= payment.amount;
	}
}
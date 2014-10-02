package hds;

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
public
class DIR_Payment_Request extends ASNObj{
	double amount;
	String method;
	@Override
	public Encoder getEncoder() {
		Encoder r = new Encoder().initSequence();
		r.addToSequence(new Encoder(method));
		r.addToSequence(new Encoder(amount));
		return r;
	}
	@Override
	public DIR_Payment_Request decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		method = d.getFirstObject(true).getString();
		amount = d.getFirstObject(true).getReal();
		return this;
	}
	public boolean satisfied(DIR_Payment payment) {
		return amount <= payment.amount;
	}
}
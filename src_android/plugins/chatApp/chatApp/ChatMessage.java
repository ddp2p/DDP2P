package chatApp;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

class ChatMessage extends ASNObj{
	String txtMessage; //PrintableString  array of String
	//Object obj[];
	//Image icon; // Emotion Icons images (binary data) 
	ChatMessage(){}
	ChatMessage(String message){
		txtMessage = message;
	}
	
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(txtMessage).setASN1Type(Encoder.TAG_PrintableString));
		return enc;
	}
	public ChatMessage decode(Decoder dec){
		Decoder content=dec.getContent();
		txtMessage = content.getFirstObject(true).getString();
		return this;
	}
}

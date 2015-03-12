//package AndroidChat;
package dd_p2p.plugin;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

 class ChatElemCtl extends ChatElem {
    public ChatElem instance() {
        return new ChatElem();
    }
    @Override
    public Encoder getEncoder() {
        Encoder enc = new Encoder().initSequence();
        // TODO Auto-generated method stub
        return enc;
    }

    @Override
    public  ChatElem decode(Decoder dec) throws ASN1DecoderFail {
        // TODO Auto-generated method stub
        return this;
    }

    public static byte getASN1Type() {
        return DD.TAG_AC19;
    }
   
}

public class ChatElem extends ASN1.ASNObj {
    public ChatElem instance() {
        return new ChatElem();
    }
	int version = 0; // 0 - name, 1 - email ,....
	int type; // 0 - name, 1 - email ,....
	String val;
    @Override
    public Encoder getEncoder() {
        Encoder enc = new Encoder().initSequence();
        enc.addToSequence(new Encoder(version));
        enc.addToSequence(new Encoder(type));
        enc.addToSequence(new Encoder(val));
        return enc.setASN1Type(getASN1Type());
       // return enc;
    }

    @Override
    public  ChatElem decode(Decoder dec) throws ASN1DecoderFail {
 		Decoder d = dec.getContent();
 		version = d.getFirstObject(true).getInteger().intValue();
 		type = d.getFirstObject(true).getInteger().intValue();
 		val = d.getFirstObject(true).getString();
        // TODO Auto-generated method stub
        return this;
    }

    public static byte getASN1Type() {
        return DD.TAG_AC19;
    }
   
}
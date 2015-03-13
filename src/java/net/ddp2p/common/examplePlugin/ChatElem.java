package net.ddp2p.common.examplePlugin;
//package dd_p2p.plugin;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;

 
public class ChatElem extends net.ddp2p.ASN1.ASNObj {
    public ChatElem instance() {
        return new ChatElem();
    }
	 int version = 0; 
	 int type; // 0 - name, 1 - email ,....
	 String val;
	 
	 public String toString() {
		 return
				 "ChatElem[v="+version
				 +"\n type="+type
				 +"\n val ="+val
				 +"\n]";
	 }
	 
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
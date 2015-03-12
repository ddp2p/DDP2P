/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Khalid Alhamed, Marius Silaghi <silaghi@fit.edu>
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat;
//package dd_p2p.plugin;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

 
public class ChatElem extends ASN1.ASNObj {
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
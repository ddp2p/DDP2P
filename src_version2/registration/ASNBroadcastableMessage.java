/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Ossamah Dhanoon
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

package registration;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
@Deprecated
public class ASNBroadcastableMessage   extends ASNObj {
	String slogan="";

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(slogan));
		return enc;
	}

	@Override
	public ASNBroadcastableMessage decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		slogan = d.getFirstObject(true).getString();
		return this;
	}

}

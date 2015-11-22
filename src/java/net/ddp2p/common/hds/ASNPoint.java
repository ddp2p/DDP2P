/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.common.hds;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
public 
class ASNPoint extends ASNObj{
	public double latitude; 
	public double longitude;
	public String toString(){
		return "Point:"+
		"lat="+latitude+"lon="+longitude+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(latitude));
		enc.addToSequence(new Encoder(longitude));
		return enc;
	}
	@Override
	public ASNPoint decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		latitude = dec.getFirstObject(true).getReal();
		longitude = dec.getFirstObject(true).getReal();
		return this;
	}
}

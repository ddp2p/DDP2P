/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package net.ddp2p.common.data;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.hds.ASNSyncRequest;
public
class ASNNeighborhoodOP extends ASNObj implements net.ddp2p.common.util.Summary{
	//public ASNNeighborhood neighborhood;
	public D_Neighborhood neighborhood;
	public int op=0; // DEFAULT add(0)
	public String toString() {
		return "ASNNeighborhoodOP: "+
		"; op="+op+
		"; neighborhood="+neighborhood+
		"";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(neighborhood.getEncoder());
		if(op!=0) enc.addToSequence(new Encoder(op));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded ASNNeighborhoodOP");
		return enc;
	}
	@Override
	public ASNNeighborhoodOP decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		//neighborhood=new ASNNeighborhood().decode(dec.getFirstObject(true));		
		neighborhood = D_Neighborhood.getEmpty().decode(dec.getFirstObject(true));		
		if(dec.getTypeByte()==Encoder.TAG_INTEGER) op = dec.getFirstObject(true).getInteger().intValue();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoded ASNNeighborhoodOP");
		return this;
	}
	public ASNNeighborhoodOP instance(){
		return new ASNNeighborhoodOP();
	}
	@Override
	public String toSummaryString() {
		return this.neighborhood.toSummaryString();
	}
}

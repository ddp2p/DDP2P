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

package hds;

import java.util.ArrayList;
import java.util.Calendar;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class ASNDatabase extends ASNObj{
	public Table tables[];
	public Calendar snapshot;
	public String toString() {
		String result = "[ASNDatabase]snapshot="+Encoder.getGeneralizedTime(snapshot);
		result+="\n tables=";
		for(int k=0; k<tables.length; k++) {
			result += "\n  \t["+k+"]"+tables[k];
		}
		return result;
	}
	public String toSummaryString() {
		String result = " [ASNDatabase]snapshot="+Encoder.getGeneralizedTime(snapshot);
		result+="\n  tables=";
		for(int k=0; k<tables.length; k++) {
			result += "\n   \t["+k+"]"+tables[k].toSummaryString();
		}
		return result;
	}	
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding ASNDatabase");
		Encoder enc = new Encoder().initSequence();
		Encoder tables_enc = new Encoder().initSequence();
		for(int k=0; k<tables.length; k++){
			tables_enc.addToSequence(tables[k].getEncoder());
			if(ASNSyncRequest.DEBUG)System.out.println("Encoding table: "+k);
			//tables_enc.print();
		}
		enc.addToSequence(tables_enc);
		//System.err.println("ASNDATABASE encoder - created table:"+Util.byteToHex(enc.getBytes()," "));
		if(snapshot != null) {
			Encoder e2=new Encoder(snapshot).setASN1Type(Encoder.TAG_GeneralizedTime);
			//System.err.println("snapshot bytes="+Util.byteToHex(e2.getBytes()," "));
			enc.addToSequence(e2);
		}
		//System.err.println("ASNDATABASE encoder - created table:"+Util.byteToHex(enc.getBytes()," "));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded ASNDatabase:");
		//enc.print();
		return enc;
	}
	public ASNDatabase decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEcoding Database");
		Decoder dec = decoder.getContent();
		Decoder tables_dec = dec.getFirstObject(true, Encoder.TYPE_SEQUENCE).getContent();
		ArrayList<Table> tables_list = new ArrayList<Table>();
		for(;;){
			Decoder d_t = tables_dec.getFirstObject(true, Encoder.TYPE_SEQUENCE);
			if(d_t==null) break;
			tables_list.add(new Table().decode(d_t));
		}
		tables = tables_list.toArray(new Table[]{});
		if(dec.getTypeByte() == Encoder.TAG_GeneralizedTime) snapshot = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEcoded ASNDatabase");
		return this;
	}
}
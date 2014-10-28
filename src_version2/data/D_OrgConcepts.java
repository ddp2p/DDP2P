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

package data;

import hds.ASNSyncRequest;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
public
class D_OrgConcepts extends ASNObj {
	
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public String[] name_forum;
	public String[] name_justification;
	public String[] name_motion;
	public String[] name_organization;
	@Override
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		if(name_forum!=null)enc.addToSequence(Encoder.getStringEncoder(name_forum,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC0));
		if(name_justification!=null)enc.addToSequence(Encoder.getStringEncoder(name_justification,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(name_motion!=null)enc.addToSequence(Encoder.getStringEncoder(name_motion,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC2));
		if(name_organization!=null)enc.addToSequence(Encoder.getStringEncoder(name_organization,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC3));
		return enc;
	}
	@Override
	public D_OrgConcepts decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)name_forum = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC1)name_justification = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC2)name_motion = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC3)name_organization = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		return this;
	}
	public String toString() {
		return "OrgConcepts: ["+
		";\n   name_forum="+Util.nullDiscrim(name_forum,Util.concat(name_forum, "; "))+
		";\n   name_justification="+Util.nullDiscrim(name_justification,Util.concat(name_justification, "; "))+
		";\n   name_motion="+Util.nullDiscrim(name_motion,Util.concat(name_motion, "; "))+
		";\n   name_organization="+Util.nullDiscrim(name_organization,Util.concat(name_organization, "; "))+
		 "\n  ]"
		;
	}
	/**
	 * For null return null
	 * @param in
	 * @return
	 */
	public static String stringFromStringArray(String[] in){
		String result = null;
		if (in == null) return null;
		byte[] data = new ASN64String_2_StringArray(in).encode(); //Encoder.getStringEncoder(in,Encoder.TAG_UTF8String).getBytes();
		result = Util.stringSignatureFromByte(data);
		return result;
	}
	/**
	 * If null return null, else return a string representation of the ASN encoding
	 * @param in
	 * @return
	 */
	public static String[] stringArrayFromString(String in){
		return stringArrayFromString(in, DEBUG);
	}
	public static String[] stringArrayFromString(String in, boolean DEBUG){
		if (DEBUG) System.err.println("OrgConcepts:stringArrayFromString: parsing \""+in);
		String result[] = null;
		//if (in == null) return null;
		//if (in.equals("null")) return null;
		//byte[] data = Util.byteSignatureFromString(in);
		try {
			//if (data == null)  throw new ASN1DecoderFail("Wrong hex for String Array");
			//Decoder dec = new Decoder(data);
//			if (dec.getTypeByte() != Encoder.TAG_SEQUENCE) {
//				 System.err.println("OrgConcepts:stringArrayFromString: parsing \""+in+"\"");
//				 System.err.println("OrgConcepts:stringArrayFromString: parsing data:\"["+data.length+"]="+Util.byteToHex(data));
//				throw new ASN1DecoderFail("Wrong tag for String Array");
//			}
//			result = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
			//result = new ASN_StringArray(dec).getStrsArray();
			result = new ASN64String_2_StringArray(in).getStrsArray();
		} catch (ASN1DecoderFail e) {
			if(DEBUG) System.err.println("OrgConcepts:stringArrayFromString: fail to parse an array out of string \""+in+"\" error:"+e.getMessage());
			e.printStackTrace();
			result = null;
		}
		if(DEBUG) System.err.println("OrgConcepts:stringArrayFromString: got \""+Util.concat(result, "::"));
		return result;
	}
}

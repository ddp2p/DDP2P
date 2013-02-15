/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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

import util.Util;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;


// comes with a new VersionInfo
public class D_TesterInfo extends ASNObj{
	public static final String TEST_SEP = ";";
	public static boolean DEBUG = false;
	public String name;
	public String public_key_hash;
	public float[] tester_QoT;
	public float[] tester_RoT;
	public byte[] signature; // of signed structure
	public static D_TesterInfo[] reconstructArrayFromString(String s) throws ASN1DecoderFail {
		if (s==null) return null;
		byte[] data = util.Base64Coder.decode(s);
		Decoder dec = new Decoder(data);
		D_TesterInfo result[] = dec.getSequenceOf(getASNType(), new D_TesterInfo[0], new D_TesterInfo());
		return result;
	}
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		result += this.name+"\r\n";
		result += this.public_key_hash+"\r\n";
		result += Util.mkArrayCounter(this.tester_QoT.length, TEST_SEP)+"\r\n";
		result += Util.concat(this.tester_QoT, TEST_SEP)+"\r\n";
		result += Util.concat(this.tester_RoT, TEST_SEP)+"\r\n";
		result += Util.stringSignatureFromByte(signature)+"\r\n";
		return result;
	}
	public static String encodeArray(D_TesterInfo[] a) {
		if(a==null)	return null;
		Encoder enc = Encoder.getEncoder(a);
	//	enc.setASN1Type(getASNType());
		byte[] b = enc.getBytes();
		return new String(util.Base64Coder.encode(b));
	}
	public static byte getASNType() {
		if(DEBUG) System.out.println("DD.TAG_AC23= "+ DD.TAG_AC23);
		return DD.TAG_AC23;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(public_key_hash));
		enc.addToSequence(Encoder.getEncoderArray(tester_QoT));
		enc.addToSequence(Encoder.getEncoderArray(tester_RoT));
		enc.addToSequence(new Encoder(signature));
		enc.setASN1Type(getASNType());
		return enc;
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(public_key_hash));
		enc.addToSequence(Encoder.getEncoderArray(tester_QoT));
		enc.addToSequence(Encoder.getEncoderArray(tester_RoT));
		enc.setASN1Type(getASNType());
		return enc;
	}
	@Override
	public D_TesterInfo decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		name = d.getFirstObject(true).getString();
		public_key_hash = d.getFirstObject(true).getString();
		tester_QoT = d.getFirstObject(true).getFloatsArray();
		tester_RoT = d.getFirstObject(true).getFloatsArray();
		signature = d.getFirstObject(true).getBytes();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new D_TesterInfo();}
	public static void main(String[] arg){
		D_TesterInfo[] a = new D_TesterInfo[1];
		a[0] = new D_TesterInfo();
		a[0].name="testerA"; a[0].public_key_hash="0i0i0"; 
		a[0].tester_QoT= new float[2];a[0].tester_QoT[0]=11;a[0].tester_QoT[1]=22;
	    a[0].tester_RoT= new float[2];a[0].tester_RoT[0]=0.5f;a[0].tester_RoT[1]=0.9f;
		String encoded = D_TesterInfo.encodeArray(a);
		System.out.println(encoded);
		D_TesterInfo[] b=null;
		try {
			b = D_TesterInfo.reconstructArrayFromString(encoded);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		System.out.print(b[0].name);
	}
}
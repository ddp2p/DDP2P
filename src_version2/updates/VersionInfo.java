/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
package updates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

import config.DD;

import ciphersuits.PK;
import ciphersuits.SK;
import data.D_ReleaseQuality;
import data.D_SoftwareUpdatesReleaseInfoByTester;
import util.Base64Coder;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;


public class VersionInfo extends ASN1.ASNObj{
	public String version;
	public String branch; // Do we need signature + pkey of the main repository?? or testers did that verfication!
	public Calendar date;
	public String script;
	public Downloadable[] data;
	//public byte[] signature;   // signature of one one tester (main?) should be removed
	//public String trusted_public_key;
	public D_SoftwareUpdatesReleaseInfoByTester testers_data[];
	public D_ReleaseQuality releaseQD[];
	//public byte[][] signatures;
	@Override
	public
	boolean equals(Object in) {
		if(in instanceof VersionInfo) return equals((VersionInfo)in);
		return false;
	}
	boolean equals(VersionInfo in) {
		if(!Util.equalStrings_null_or_not(version,in.version)) return false;
		//if(!date.equals(in.date)) return false;
		if(!Util.equalStrings_null_or_not(script,in.script)) return false;
		if((data==null)&&(in.data==null)) return true;
		if((data==null)||(in.data==null)) return false;
		if(data.length!=in.data.length) return false;
		for(int i=0; i<data.length; i++) {
			if(!Util.equalBytes_null_or_not(data[i].digest, in.data[i].digest)) return false;
			if(!Util.equalStrings_null_or_not(data[i].filename, in.data[i].filename)) return false;
		}
		return true;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version, false));
		enc.addToSequence(new Encoder(date));
		//enc.addToSequence(new Encoder(signature));
		enc.addToSequence(new Encoder(script, false));
		enc.addToSequence(Encoder.getEncoder(data));
		if(releaseQD!=null) enc.addToSequence(Encoder.getEncoder(releaseQD)).setASN1Type(DD.TAG_AC12);
		if(testers_data!=null) enc.addToSequence(Encoder.getEncoder(testers_data)).setASN1Type(DD.TAG_AC13);
		return enc;
	}
	/**
	 * Testers info are not encoded since they are encoded separatey in D_TEsterSignedData
	 * @return
	 */
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version, false));
		enc.addToSequence(new Encoder(date));
		//enc.addToSequence(new Encoder(signature));
		enc.addToSequence(new Encoder(script, false));
		Downloadable _data[] = getSignableDownloadables(data);
		enc.addToSequence(Encoder.getEncoder(_data));
		//if(releaseQD!=null) enc.addToSequence(Encoder.getEncoder(releaseQD));
		return enc;
	}
	public static Downloadable[] getSignableDownloadables(Downloadable data[]){
		if(data == null) return null;
		Downloadable[] _data = new Downloadable[data.length];
		for(int i=0;i<data.length;i++) {
			if(data[i]==null) continue;
			_data[i] = data[i].getSignableDownloadable();
		}
		return _data;
	}
	@Override
	public VersionInfo decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		date = d.getFirstObject(true).getGeneralizedTimeCalender(Encoder.TAG_GeneralizedTime);
		//signature = d.getFirstObject(true).getBytes();
		script = d.getFirstObject(true).getString();
		data = d.getFirstObject(true).getSequenceOf(Downloadable.getASN1Type(), new Downloadable[0], new Downloadable());
		Decoder d_qd = d.getFirstObject(true);
		if((d_qd!=null)&&(d_qd.getTypeByte()==DD.TAG_AC12))
			releaseQD = d_qd.getSequenceOf(D_ReleaseQuality.getASNType(), new D_ReleaseQuality[0], new D_ReleaseQuality());
		d_qd = d.getFirstObject(true);
		if((d_qd!=null)&&(d_qd.getTypeByte()==DD.TAG_AC13))
			testers_data = d_qd.getSequenceOf(D_SoftwareUpdatesReleaseInfoByTester.getASNType(), new D_SoftwareUpdatesReleaseInfoByTester[0], new D_SoftwareUpdatesReleaseInfoByTester());
		return this;
	}
	public String toString(){
		return 
				"VersionInfo [\n v="+version+
				"\n date="+Encoder.getGeneralizedTime(date)
				+"\n script="+script
				+"\n data="+Util.nullDiscrimArray(data, "\n|||")
				+"\n testers="+Util.nullDiscrimArray(this.testers_data, "\n|||")
				//"\n sig="+Util.stringSignatureFromByte(signature)
				+"]"
				;
	}
//	public boolean verifySignature(PK pk) {
//		//if(!ClientUpdates.newer(this.version, "0.0.11")) return verifySignature_0_0_11(pk);
//		
//		//byte[] _sign = signature;
//		//signature = new byte[0];
//		byte signable[] = this.getSignableEncoder().getBytes();
//		boolean result = Util.verifySign(signable, pk, signature);
//		//signature = _sign;
//		return result;
//	}
//	public boolean verifySignature_0_0_11(PK pk) {
//		byte[] _sign = signature;
//		signature = new byte[0];
//		boolean result = Util.verifySign(this, pk, _sign);
//		signature = _sign;
//		return result;
//	}
//	@Deprecated
//	public byte[] sign(SK sk) {
//		if(!ClientUpdates.newer(this.version, "0.0.11")) return sign_0_0_11(sk);
//		//signature = new byte[0];
//		byte signable[] = this.getSignableEncoder().getBytes();
//		byte[] _signature = Util.sign(signable, sk);
//		return _signature;
//	}
//	@Deprecated
//	public byte[] sign_0_0_11(SK sk) {
//		signature = new byte[0];
//		signature = Util.sign(this, sk);
//		return signature;
//	}
	public String warningPrint() {
		return "\nVersionInfo \n[\n v="+version+"\n date="+Encoder.getGeneralizedTime(date)+"\n script="+script+"\n data="+
		Util.nullDiscrimArray(data, "|||")+
		//"\n sig="+Util.byteToHexDump(signature)+
		"\n]" +" File: " +data[0].filename + " H : "+ Util.stringSignatureFromByte(data[0].digest);
	}
	public String toTXT() {
		String result = ClientUpdates.START+"\r\n";
		result += version+"\r\n";
		result += Encoder.getGeneralizedTime(date)+"\r\n";
		//result += Util.stringSignatureFromByte(signature)+"\r\n";
		result += this.data.length+"\r\n";
		result += script+"\r\n";
		for(Downloadable d:data) result+= d.toTXT();
		result += ClientUpdates.TESTERS+"\r\n";
		result += this.testers_data.length+"\r\n";
		result += getBase64(this.releaseQD)+"\r\n";
		for(D_SoftwareUpdatesReleaseInfoByTester t: this.testers_data) result+=t.toTXT();
		result += ClientUpdates.STOP+"\r\n";
		//if(this.trusted_public_key!=null) result += this.trusted_public_key+"\r\n";
		return result;
	}
	private static String getBase64(D_ReleaseQuality[] releaseQD2) {
		Encoder enc = Encoder.getEncoder(releaseQD2);
		return new String(Base64Coder.encode(enc.getBytes()));
	}
	public void updateHash(String filePath) {
		for(Downloadable d:data)
			try {
				d.updateHash(filePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	public static void main(String args[]) throws FileNotFoundException {
		VersionInfo i = ClientUpdates.fetchLastVersionNumberAndSiteTXT_BR(new BufferedReader(new FileReader("Update.txt")));
		System.err.println("i="+i);
		VersionInfo i2 = ClientUpdates.fetchLastVersionNumberAndSiteTXT_BR(new BufferedReader(new FileReader("Update2.txt")));
		System.err.println("j="+i2);
		
	}
}
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
package net.ddp2p.common.updates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.util.Util;

public class Downloadable extends net.ddp2p.ASN1.ASNObj{
	private static final boolean DEBUG = false;
	public String url;
	public String filename;
	public byte[] digest;
	static byte TAG = Encoder.TAG_SEQUENCE;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(filename));
		enc.addToSequence(new Encoder(url));
		enc.addToSequence(new Encoder(digest));
		enc.setASN1Type(TAG);
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		filename = d.getFirstObject(true).getString();
		url = d.getFirstObject(true).getString();
		digest = d.getFirstObject(true).getBytes();
		return this;
	}
	public Downloadable instance(){
		return new Downloadable();
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public String toString() {
		return "\n   Downloadable [\n    local="+filename+",\n    url="+url+"\n    hash="+Util.byteToHexDump(digest,"")+"] ";
	}
	public String toTXT() {
		return filename+"\r\n"+url+"\r\n"+Util.stringSignatureFromByte(digest)+"\r\n";
	}
	/**
	 * Computes the SHA256 digest of the file found on the path in parameter.
	 * Stores the result in the "digest" member
	 * @param filepath
	 * @throws IOException
	 */
	public void updateHash(String filepath) throws IOException {
    	filepath += filename;
    	FileInputStream fos = new FileInputStream(filename);
    	long len=0;
    	int crt=0;
    	//FileChannel ch = fos.getChannel();
    	
    	byte[] message = new byte[1<<14];
    	MessageDigest _digest;
    	String hash_alg = Cipher.SHA256;
    	try {
    		if(DEBUG)System.out.println("Downloadable downloadNewer:hash = "+hash_alg);
    		_digest = MessageDigest.getInstance(hash_alg);
    	} catch (NoSuchAlgorithmException e) {
    		e.printStackTrace(); return;}
    	do{
    		
			//crt=istream.read(message);
 			crt = fos.read(message);
 			if (crt <= 0) break;
 			_digest.update(message, 0, crt);
    		//crt=ch.transferFrom(rbc, len, 1 << 32);
    		//if(crt>0) len+=crt;
			//if(DEBUG)System.out.println("ClientUpdates downloadNewer: done crt="+crt+" len="+len);
    	} while((crt > 0));
    	fos.close();
    	digest = _digest.digest();
	}
	/**
	 * Makes a clone containing only the data that will be signed (e.g. not the url)
	 * @return
	 */
	public Downloadable getSignableDownloadable() {
		Downloadable result = new Downloadable();
		result.filename = filename;
		result.digest = digest;
		return result;
	}
}
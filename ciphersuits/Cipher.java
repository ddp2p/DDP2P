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

package ciphersuits;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import util.Util;

/**
 * 
 * @author silaghi
 *
 * first getCipher("RSA", SHA-256, "comments")
 * then genKey(2056)
 * then getSK() and getPK();
 */

abstract public class Cipher{
	public String comment;
	String hash_alg;
	public static final String RSA = "RSA";
	public static final String EC_EG = "ECElGamal";
	public static final String MD5 = "MD5";
	public static final String MD2 = "MD2";
	public static final String SHA1 = "SHA-1";
	public static final String SHA256 = "SHA-256";
	public static final String SHA384 = "SHA-384";
	public static final String SHA512 = "SHA-512";
	final public static String cipherTypeSeparator="::";
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	/**
	 * Generate keys with "size" primes and DD_PRIME_CERTAINTY and saves them in this object
	 * @param size
	 * @return
	 */
	public abstract SK genKey(int size);
	public abstract String getType();
	public abstract PK getPK();
	public abstract SK getSK();
	/**
	 * Padding for encryption
	 * @param toencryption
	 * @return
	 */
	public abstract byte[] padding(byte[] toencryption);
	/**
	 * Unpadding after decryption
	 * @param decrypted
	 * @return
	 */
	public abstract byte[] unpad(byte[] decrypted);
	/**
	 * Hash and salt for signature
	 * @param msg
	 * @return
	 */
	public abstract byte[] hash_salt(byte[] msg);
	public abstract byte[] sign(byte[] m);
	public abstract boolean verify(byte[] signature, byte[] message);
	/**
	 * @param cipher "RSA"
	 * @param _hash_alg: MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
	 * @return
	 */
	public static Cipher getCipher(String cipher, String _hash_alg, String _comments){
		Cipher result=null;
		if(RSA.equals(cipher)){
			result = new RSA();
			result.hash_alg = _hash_alg;
			result.comment = _comments;
		}else
			if(DEBUG) System.err.println("Cipher:getCipher: unknown cipher:"+cipher);
		return result;
	}
	public byte[] encrypt(byte[] m){
		return getPK().encrypt(m);
	}
	public byte[] decrypt(byte[] c){
		return getSK().decrypt(c);
	}
	/**
	 * Get the PK from a string globalID
	 * @param senderID
	 * @return
	 */
	public static PK getPK(String senderID) {
		if(DEBUG) System.err.println("Cipher:getPK: start");
		if(senderID == null) return null;
		//String[]splits = Util.splitHex(senderID);
		//if(DEBUG) System.err.println("Cipher:getPK: splits="+splits.length);
		byte[] sID = null;
		try{
			sID = Util.byteSignatureFromString(senderID); // Util.hexToBytes(splits);
		}catch(Exception e) {
			e.printStackTrace();
			System.err.println("Cipher:geePK: Fail to parse: "+senderID);
		}
		if((sID==null)||sID.length==0){
			System.err.println("Cipher:getPK: null bytes="+sID);
			return null;
		}
		if(DEBUG) System.err.println("Cipher:getPK: bytes="+sID.length);
		Decoder decoder = new Decoder(sID);
		if(decoder.getTypeByte()!=Encoder.TAG_SEQUENCE) {
			Util.printCallPath("Failure to decode");
			//throw new ASN1DecoderFail("Wrong PK");
			return null;
		}
		Decoder dec;
		try {
			dec = decoder.getContent();
		} catch (ASN1DecoderFail e1) {
			e1.printStackTrace();
			return null;
		}
		if(dec == null) return null;
		Decoder dec_type = dec.getFirstObject(true);
		if(dec_type==null) return null;
		String type = dec_type.getString();
		if(RSA.equals(type)){
			if(DEBUG) System.err.println("Cipher:getPK: found RSA");
			RSA_PK pk = new RSA_PK();
			try {
				if(DEBUG) System.err.println("Cipher:getPK: decoding");
				pk.decode(decoder);
				if(DEBUG) System.err.println("Cipher:getPK: decoded");
			} catch (ASN1DecoderFail e) {
				if(DEBUG) System.err.println("Cipher:getPK: failed decoding");
				return null;
			}
			return pk;
		}
		if(DEBUG) System.err.println("Cipher:getPK: unknown");
		return null;
	}
	/**
	 * Get the SK from a string globalID
	 * @param sk
	 * @return
	 */
	public static SK getSK(String _sk) {
		//String[]splits = Util.splitHex(senderID);
		byte[] sID = Util.byteSignatureFromString(_sk); //Util.hexToBytes(splits);
		if((sID==null)||(sID.length==0)) {
	    	if(_DEBUG)System.err.println("SK:getSK: wrong null input: \""+_sk+"\"");
			return null;
		}
		Decoder decoder = new Decoder(sID);
		Decoder dec;
		try {
			dec = decoder.getContent();
		} catch (ASN1DecoderFail e1) {
			e1.printStackTrace();
			return null;
		}
		String type = dec.getFirstObject(true).getString();
		if(RSA.equals(type)){
			RSA_SK sk = new RSA_SK();
			try {
				sk.decode(decoder);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				return null;
			}
			return sk;
		}
    	if(_DEBUG)System.err.println("SK:getSK: Unsupported Cipher type: "+type);
		return null;
	}
	public static Cipher getCipher(SK sk, PK pk) {
		if(sk==null) return null;
		if(sk instanceof RSA_SK) {
			if(pk==null) pk = sk.getPK();
			return new ciphersuits.RSA((RSA_SK)sk,(RSA_PK)pk);
		}
		return null;
	}
	public static boolean isPair(SK sk, PK pk) {
		if((sk==null)||pk==null) return false;
		if(!RSA.equals(sk.getType())) return false;
		RSA_PK _pk = (RSA_PK) (sk.getPK());
		if(_pk.equals(pk)) return true;
		return false;
	}
	public static boolean equalPK(PK p1, PK p2){
		if(!(p1 instanceof RSA_PK)) return false;
		if(!(p2 instanceof RSA_PK)) return false;
		RSA_PK r1 = (RSA_PK)p1;
		RSA_PK r2 = (RSA_PK)p2;
		return r1.equals(r2);
	}
}

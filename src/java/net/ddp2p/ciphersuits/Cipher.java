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

package net.ddp2p.ciphersuits;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

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
	public static final String ECDSA = "ECDSA";
	public static final String DSA = "DSA";
//	public static final String EC_EG = "ECElGamal";
	public static final String MD5 = "MD5";
	public static final String MD2 = "MD2";
	public static final String SHA1 = "SHA-1";
	public static final String SHA256 = "SHA-256";
	public static final String SHA384 = "SHA-384";
	public static final String SHA512 = "SHA-512";
	public static final String HNULL = "HNULL";
	final public static String cipherTypeSeparator="::";
	private static final boolean _DEBUG = true;
	protected static final boolean DEBUG = false;
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
	 * @param comments : comments to be stored in the cipher
	 * @return
	 */
	public static Cipher getCipher(String cipher, String _hash_alg, String _comments){
		Cipher result=null;
		if(RSA.equals(cipher)){
			result = new RSA();
			result.hash_alg = _hash_alg;
			result.comment = _comments;
			return result;
		}
		if(ECDSA.equals(cipher)){
			result = new ECDSA();
			result.hash_alg = _hash_alg;
			result.comment = _comments;
			return result;
		}
		if(DSA.equals(cipher)){
			result = new DSA();
			result.hash_alg = _hash_alg;
			result.comment = _comments;
			return result;
		}
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
		//boolean DEBUG = true;
		if (DEBUG) System.err.println("Cipher:getPK: start: "+senderID);
		if (senderID == null) return null;
		//String[]splits = Util.splitHex(senderID);
		//if(DEBUG) System.err.println("Cipher:getPK: splits="+splits.length);
		byte[] sID = null;
		try {
			sID = Util.byteSignatureFromString(senderID); // Util.hexToBytes(splits);
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Cipher:geePK: Fail to parse: "+senderID);
		}
		if ((sID == null) || sID.length == 0) {
			System.err.println("Cipher:getPK: null bytes="+sID);
			return null;
		}
		if (DEBUG) System.err.println("Cipher:getPK: bytes="+sID.length);
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
		if (dec == null) return null;
		Decoder dec_type = dec.getFirstObject(true);
		if (dec_type==null) return null;
		String type = dec_type.getString();
		if (RSA.equals(type)){
			if (DEBUG) System.err.println("Cipher:getPK: found RSA");
			RSA_PK pk = new RSA_PK();
			try {
				if (DEBUG) System.err.println("Cipher:getPK: decoding");
				pk.decode(decoder);
				if (DEBUG) System.err.println("Cipher:getPK: decoded");
			} catch (ASN1DecoderFail e) {
				if (DEBUG) System.err.println("Cipher:getPK: failed decoding");
				return null;
			}
			return pk;
		}
		if (ECDSA.equals(type)) {
			if (DEBUG) System.err.println("Cipher:getPK: found ECDSA");
			ECDSA_PK pk = null;
			try {
				if (DEBUG) System.err.println("Cipher:getPK: decoding");
				pk = new ECDSA_PK(decoder);
				if (DEBUG) System.err.println("Cipher:getPK: decoded");
			} catch (ASN1DecoderFail e) {
				if (_DEBUG) System.err.println("Cipher:getPK: failed decoding");
				return null;
			}
			return pk;
		}
		if (DSA.equals(type)) {
			if (DEBUG) System.err.println("Cipher:getPK: found DSA");
			DSA_PK pk = null;
			try {
				if (DEBUG) System.err.println("Cipher:getPK: decoding");
				pk = new DSA_PK(decoder);
				if (DEBUG) System.err.println("Cipher:getPK: decoded");
			} catch (ASN1DecoderFail e) {
				if (_DEBUG) System.err.println("Cipher:getPK: failed decoding");
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
		if ((sID == null) || (sID.length == 0)) {
	    	if(_DEBUG)System.err.println("SK:getSK: wrong null input: \""+_sk+"\"");
			return null;
		}
		return getSK(sID);
	}
	public static SK getSK(byte[] sID) {
		Decoder decoder = new Decoder(sID);
		return getSK(decoder);
	}
	public static SK getSK(Decoder decoder) {
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
		if(ECDSA.equals(type)){
			ECDSA_SK sk = null; //new RSA_SK();
			try {
				sk = new ECDSA_SK(decoder);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				return null;
			}
			return sk;
		}
		if(DSA.equals(type)){
			DSA_SK sk = null;
			try {
				sk = new DSA_SK(decoder);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				return null;
			}
			return sk;
		}
    	if(_DEBUG)System.err.println("SK:getSK: Unsupported Cipher type: "+type);
		return null;
	}
	/**
	 * Sets sk to null;
	 * @param pk
	 * @return
	 */
	public static Cipher getCipher(PK pk) {
		if (pk == null) return null;
		if (pk instanceof RSA_PK) {
			return new net.ddp2p.ciphersuits.RSA(null, (RSA_PK)pk);
		}
		if (pk instanceof ECDSA_PK) {
			return new net.ddp2p.ciphersuits.ECDSA(null, (ECDSA_PK)pk);
		}
		if (pk instanceof DSA_PK) {
			return new net.ddp2p.ciphersuits.DSA(null, (DSA_PK)pk);
		}
		return null;
	}
	/**
	 * Requires sk, and builds a Cipher block. It recomputes also the public key
	 * @param sk
	 * @param pk optional parameter to avoid its re-computation from sk.
	 * @return
	 */
	public static Cipher getCipher(SK sk, PK pk) {
		if (sk == null)
			return getCipher(pk);
	
		if (sk instanceof RSA_SK) {
			if (pk == null) pk = sk.getPK();
			return new net.ddp2p.ciphersuits.RSA((RSA_SK)sk, (RSA_PK)pk);
		}
		if (sk instanceof ECDSA_SK) {
			if (pk == null) pk = sk.getPK();
			return new net.ddp2p.ciphersuits.ECDSA((ECDSA_SK)sk, (ECDSA_PK)pk);
		}
		if (sk instanceof DSA_SK) {
			if (pk == null) pk = sk.getPK();
			return new net.ddp2p.ciphersuits.DSA((DSA_SK)sk, (DSA_PK)pk);
		}
		return null;
	}
	public static boolean isPair(SK sk, PK pk) {
		// boolean DEBUG = true;
		if ((sk == null) || (pk == null)) {
			if (DEBUG) System.out.println("Cipher:isPair: null sk or pk");
			return false;
		}
		if (RSA.equals(sk.getType())) {
			if (DEBUG) System.out.println("Cipher:isPair: compare RSA");
			RSA_PK _pk = (RSA_PK) (sk.getPK());
			if (_pk.__equals(pk)) return true;
			if (DEBUG) System.out.println("Cipher:isPair: RSA <> \n"+pk+"\n\n"+_pk);
		}
		if (ECDSA.equals(sk.getType())) {
			if (DEBUG) System.out.println("Cipher:isPair: compare ECDSA");
			ECDSA_PK _pk = (ECDSA_PK) (sk.getPK());
			if(_pk.__equals(pk)) return true;
			if (DEBUG) System.out.println("Cipher:isPair: ECDSA <> \n"+pk+"\n\n"+_pk);
		}
		if (DSA.equals(sk.getType())) {
			if (DEBUG) System.out.println("Cipher:isPair: compare DSA");
			DSA_PK _pk = (DSA_PK) (sk.getPK());
			if(_pk.__equals(pk)) return true;
			if (DEBUG) System.out.println("Cipher:isPair: DSA <> \n"+pk+"\n\n"+_pk);
		}
		return false;
	}
	public static boolean equalPK(PK p1, PK p2){
		if ((p1 instanceof RSA_PK) && (p2 instanceof RSA_PK)) {
			RSA_PK r1 = (RSA_PK) p1;
			RSA_PK r2 = (RSA_PK) p2;
			return r1.__equals(r2);
		}
		if ((p1 instanceof ECDSA_PK) && (p2 instanceof ECDSA_PK)) {
			ECDSA_PK r1 = (ECDSA_PK) p1;
			ECDSA_PK r2 = (ECDSA_PK) p2;
			return r1.__equals(r2);
		}
		if ((p1 instanceof DSA_PK) && (p2 instanceof DSA_PK)) {
			DSA_PK r1 = (DSA_PK) p1;
			DSA_PK r2 = (DSA_PK) p2;
			return r1.__equals(r2);
		}
		return false;
	}
	public static String[] getAvailableCiphers() {
		return new String[]{Cipher.RSA, Cipher.ECDSA, Cipher.DSA};
	}
	public static Cipher_Sizes getAvailableSizes(String cipher) {
		if(cipher == null) {
			System.out.println("Cipher:getAvailableSizes null");
			return null;
		}
		
		if(Cipher.RSA.equals(cipher)) {
			if (DEBUG) System.out.println("Cipher:getAvailableSizes RSA");
			return new Cipher_Sizes(2048, Cipher_Sizes.INT_RANGE, new int[]{230,20000});
		}
		if(Cipher.DSA.equals(cipher)) {
			if (DEBUG) System.out.println("Cipher:getAvailableSizes DSA");
			return new Cipher_Sizes(1024, Cipher_Sizes.INT_RANGE, new int[]{230,20000});
		}
		if (Cipher.ECDSA.equals(cipher)) {
			if (DEBUG) System.out.println("Cipher:getAvailableSizes ECDSA");
			return new Cipher_Sizes(
					3,
					Cipher_Sizes.LIST,
					new String[]{
					"P-119"  //+ciphersuits.ECDSA.P_119
					,"P-256" //+ciphersuits.ECDSA.P_256
					,"P-384" //+ciphersuits.ECDSA.P_384
					,"P-521" //+ciphersuits.ECDSA.P_521
					});
		}
		System.out.println("Cipher:getAvailableSizes: "+cipher);

		return null;//new String[]{Cipher.RSA, Cipher.ECDSA};
	}
	public static String[] getHashAlgos(String cipher, int size) {
		if(cipher == null) return null;
		switch(cipher) {
		case Cipher.RSA:
			if(size > 256+30) return new String[]{Cipher.SHA256, Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
			else if(size > 160+30) return new String[]{Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
			else if(size > 128+30) return new String[]{Cipher.MD5, Cipher.HNULL};
			return null;
		case Cipher.DSA:
			return new String[]{Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
		case Cipher.ECDSA:
			size = ECC.getCurveID(size);
			switch(size) {
			case 119:
			case net.ddp2p.ciphersuits.ECDSA.P_119: return new String[]{Cipher.MD5, Cipher.HNULL};
			case 256:
			case net.ddp2p.ciphersuits.ECDSA.P_256: return new String[]{Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
			case 384:
			case net.ddp2p.ciphersuits.ECDSA.P_384: return new String[]{Cipher.SHA256, Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
			case 521:
			case net.ddp2p.ciphersuits.ECDSA.P_521: return new String[]{Cipher.SHA384, Cipher.SHA256, Cipher.SHA1, Cipher.MD5, Cipher.HNULL};
			}
			return null;
		}
		return null;
	}
	public static String getDefaultCipher() {
		return Cipher.ECDSA;
	}
	/*
	public static String buildCiphersuitID(String ciphersuit, String hash) {
		return ciphersuit+Cipher.cipherTypeSeparator+hash;
	}
	*/
	/**
	 * Key comment based on:  key_comment+"://"+seed+now
	 * @param ciphersuit
	 * @param _hash_alg
	 * @param ciphersize
	 * @param storage_comment
	 * @param key_comment
	 * @param seed
	 * @param now
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Cipher mGetStoreCipher(String ciphersuit, String _hash_alg,
			int ciphersize, String storage_comment, String key_comment, String seed,
			String now) throws P2PDDSQLException {
		Cipher keys;
		//keys = Util.getKeyedGlobalID(key_comment, seed+now);
		String body = seed+now;
    	keys = net.ddp2p.ciphersuits.Cipher.getCipher(ciphersuit, _hash_alg, key_comment+"://"+body);
		keys.genKey(ciphersize);
		DD.storeSK(keys, storage_comment, now);
		return keys;
	}
	public static CipherSuit getCipherSuite(PK pk) {
		return pk.getCipherSuite();
	}
	public static Cipher getNewCipher(CipherSuit cipherSuite, String comments) {
		net.ddp2p.ciphersuits.Cipher keys;
		keys = Cipher.getCipher(cipherSuite.cipher, cipherSuite.hash_alg,
				comments);
		keys.genKey(cipherSuite.ciphersize);
		return keys;
	}
}

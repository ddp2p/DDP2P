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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import config.DD;

import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

class RSA_PK extends PK{
	//final static String type="RSA";
	final static String V0="0";
	String version = V0; // version to write out, decoding converts to this version
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String hash_alg=Cipher.SHA256;//"MD5";
	BigInteger N;
	BigInteger e;
	static SecureRandom sr = new SecureRandom();
	public String toString() {
		return "\nRSA_PK: [\ne="+e+"\n,N="+N+"]";
	}
	public boolean __equals(PK _pk){
		if (DEBUG) System.out.println("RSA_PK:equal: start");
		boolean result = _equals(_pk);
		if(!result) {
			if(_DEBUG)System.out.println("RSA_PK:equal: this="+this+"\nvs "+_pk);
		}
		return result;
	}
	public boolean _equals(PK _pk){
		if(DEBUG)System.out.println("RSA_PK:equal: start");
		if(!(_pk instanceof RSA_PK)) return false;
		RSA_PK pk = (RSA_PK)_pk;
		if(pk ==null) return false;
		if(N ==null) return false;
		if(e ==null) return false;
		if(!N.equals(pk.N)) return false;
		if(!e.equals(pk.e)) return false;
		return true;
	}
	public BigInteger encrypt(BigInteger m) {
		if(DEBUG)System.out.println("RSA_PK:encrypt: m="+m);
		if(DEBUG)System.out.println("RSA_PK:encrypt: e="+e);
		if(DEBUG)System.out.println("RSA_PK:encrypt: N="+N);
		BigInteger result = m.modPow(e, N);
		if(DEBUG)System.out.println("RSA_PK:encrypt: ciphertext="+result);
		return result;
	}
	public byte[] encrypt(byte[] padded_msg) {
		byte[] result = null;
		if(DEBUG)System.out.println("RSA_PK:encrypt: msg="+Util.byteToHex(padded_msg, ":"));
		result = encrypt(new BigInteger(padded_msg)).toByteArray();
		if(DEBUG)System.out.println("RSA_PK:encrypt: ciphertext="+Util.byteToHex(result, ":"));
		return result;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(RSA.type));
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(e));
		enc.addToSequence(new Encoder(N));
		enc.addToSequence(new Encoder(hash_alg));
		//if(comment!=null)enc.addToSequence(new Encoder(comment));
		return enc;
	}
	@Override
	public RSA_PK decode(Decoder decoder) throws ASN1DecoderFail {
		//boolean DEBUG = true;
		String ver=null;
		if(DEBUG)System.out.println("RSA_PK:decode: start");
		Decoder dec = decoder.getContent();
		if( dec==null) throw new ASN1DecoderFail("Not RSA");
		if (DEBUG) System.out.println("RSA_PK:decode: content");
		String rsa;
		if (dec.getFirstObject(false).getTypeByte() != Encoder.TAG_UTF8String) throw new ASN1DecoderFail("Not RSA");
		rsa = dec.getFirstObject(true).getString();
		if (0 != RSA.type.compareTo(rsa)) throw new ASN1DecoderFail("Not RSA");
		
		if (DEBUG)System.out.println("RSA_PK:decode: RSA");
		if (dec.getFirstObject(false).getTypeByte() == DD.TAG_AC0){
			ver = dec.getFirstObject(true).getString(DD.TAG_AC0);
			if (DEBUG)System.out.println("RSA_PK:decode: version="+ver);
		} else {
			if (DEBUG)System.out.println("RSA_PK:decode: not version: "+dec.getFirstObject(false).getTypeByte() +" vs "+ DD.TAG_AC0);
		}
		Decoder dec_e = dec.getFirstObject(true);
		if (dec_e != null) e = dec_e.getInteger(); else  throw new ASN1DecoderFail("Not RSA e");
		if (DEBUG)System.out.println("RSA_PK:decode: e="+e);
		Decoder dec_N = dec.getFirstObject(true);
		if (dec_N != null) N = dec_N.getInteger(); else  throw new ASN1DecoderFail("Not RSA N");
		if (DEBUG) System.out.println("RSA_PK:decode: N="+N);
		if (ver != null) {
			Decoder dec_alg = dec.getFirstObject(true);
			if(dec_alg!=null)hash_alg=dec_alg.getString(); else hash_alg=Cipher.SHA256;
		} else hash_alg = Cipher.SHA256;
		if(DEBUG)System.out.println("RSA_PK:decode: hash="+hash_alg);
		//if(dec.getFirstObject(false)!=null) comment=dec.getFirstObject(true).getString();
		return this;
	}
	public boolean verify_unpad(byte[] signature, byte[] unpadded_msg) {
		if((signature == null) || (signature.length == 0)){
			if(DEBUG)System.out.println("RSA:verify: null signature");
			return false;
		}
		return verify_unpad(new BigInteger(signature), unpadded_msg);
	}
	public boolean verify(byte[] signature, byte[] unpadded_msg) {
		if((signature == null) || (signature.length == 0)){
			if(DEBUG)System.out.println("RSA:verify: null signature");
			return false;
		}
		return verify(new BigInteger(signature), unpadded_msg);
	}
	public boolean verify_unpad(BigInteger signature, byte[] unpadded_msg) {
		return Util.equalBytes(unpadd(encrypt(signature)),unpadded_msg);
	}
	public boolean verify(BigInteger signature, byte[] unpadded_msg) {
		return encrypt(signature).equals(new BigInteger(unpadded_msg));
	}
	private byte[] unpadd(BigInteger signed) {
		int sLen = 10;
		byte[] msg = signed.toByteArray();
		if(DEBUG)System.out.println("RSA:unpadd: message="+Util.byteToHex(msg," "));
		int new_size = msg.length-8+sLen;
		if (new_size < 0){
			if(DEBUG)System.err.println("RSA:unpadd: message["+msg.length+"]="+Util.byteToHex(msg," "));
			return null;
		}
		byte[] unpadded_msg = new byte[msg.length-8-sLen];
		int hLen = msg.length-8-sLen;
		for(int k=sLen+hLen; k<msg.length; k++) if(msg[k]!=0) return null;
		Util.copyBytes(unpadded_msg, 0, msg, hLen, sLen);
		if(DEBUG)System.out.println("RSA:unpadd: unpadded_msg="+Util.byteToHex(unpadded_msg," "));
		return unpadded_msg;
	}
	/**
	 * padding for encryption
	 * @param msg
	 * @param pk
	 * @return
	 */
	public static byte[] padding(byte[] msg, RSA_PK pk){
		int k = (pk.N.bitLength()+7)/8;
		int j=0;
		if(msg.length > k-11) throw new RuntimeException("Message too long!");
		byte[] M = new byte[k-1];
		//M[j++] = 0;
		M[j++] = 2;
		byte ps[] = new byte[k-msg.length-3];
		sr.nextBytes(ps);
		for(int i=0; i<ps.length; i++) M[j++] = (ps[i]==0)?1:ps[i];
		M[j++] = 0;
		for(int i=0; i<msg.length; i++) M[j++] = msg[i];
		return M;
	}
	public byte[] padding(byte[]msg){
		return padding(msg, this);
	}
	@Override
	public byte[] encrypt_pad(byte[] m) {
		return encrypt(padding(m));
	}
	/**
	 * Will hash the message and compare to the unpadded decrypted signature 
	 */
	@Override
	public boolean verify_unpad_hash(byte[] signature, byte[] message) {
		if(DEBUG)System.out.println("RSA:verify_unpad_hash: start");
		if((signature == null) || (signature.length == 0) || (message == null)){
			if(DEBUG)System.out.println("RSA:verify_unpad_hash: null signature["+((signature!=null)?signature.length:"null")+"]: "+signature+" or message: "+message);
			if(DEBUG)Util.printCallPath("RSA null what?");
			return false;
		}
		
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(hash_alg);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return false;}
		if(DEBUG)System.out.println("RSA:verify_unpad_hash: digest");
		digest.update(message);
		byte unpadded_msg[] = digest.digest();
		
		if(DEBUG)System.err.println("RSA: verify_unpad_hash: unpadded_msg=["+unpadded_msg.length+"]"+Util.byteToHex(unpadded_msg, ":"));///_DEBUG
		//if(DEBUG)System.out.println("RSA:verify_unpad_hash:unpadded_msg="+Util.byteToHex(unpadded_msg, " "));
		boolean result = this.verify_unpad(signature, unpadded_msg);
		if(DEBUG)System.out.println("RSA:verify_unpad_hash: return "+result);
		return result;
	}
	@Override
	public CipherSuit getCipherSuite() {
		CipherSuit result = new CipherSuit();
		result.cipher = Cipher.RSA;
		result.ciphersize = N.bitLength();
		result.hash_alg = hash_alg;
		return result;
	}
}
class RSA_SK extends SK{
	//final static String type="RSA";
	final static String V0="0";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String hash_alg=Cipher.SHA256;//"SHA-256";//"MD5";
	BigInteger p;
	BigInteger q;
	BigInteger N;
	BigInteger d;		
	BigInteger dp;
	BigInteger dq;
	BigInteger qInv;
	String version=V0;
	static SecureRandom sr = new SecureRandom();
	@Override
	public boolean sameAs(SK sk) {
		if(!RSA.type.equals(sk.getType())) return false;
		RSA_SK _sk = (RSA_SK)sk;
		if(N!=null){
			if(!N.equals(_sk.N)) return false;
		}else{
			if(!p.equals(_sk.p)) return false;
			if(!q.equals(_sk.q)) return false;
		}
		if(!d.equals(_sk.d)) return false;
		return true;
	}
	public String getType() {
		return RSA.type;
	}
	public String toString() {
		return "\nRSA_SK: [\nd="+d+"\n,N="+N+"]";
	}
	public byte[] decrypt(byte[] c) {
		return decrypt(new BigInteger(c)).toByteArray();
	}
	public BigInteger decrypt(BigInteger c) {
		BigInteger m1 = c.modPow(dp, p);
		BigInteger m2 = c.modPow(dq, q);
		BigInteger h = (m1.subtract(m2)).multiply(qInv).mod(p);
		return m2.add(q.multiply(h));
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(RSA.type));
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(p));
		enc.addToSequence(new Encoder(q));
		enc.addToSequence(new Encoder(N));
		enc.addToSequence(new Encoder(d));
		enc.addToSequence(new Encoder(dp));
		enc.addToSequence(new Encoder(dq));
		enc.addToSequence(new Encoder(qInv));
		enc.addToSequence(new Encoder(hash_alg));
		if(comment!=null)enc.addToSequence(new Encoder(comment));
		return enc;
	}
	@Override
	public RSA_SK decode(Decoder decoder) throws ASN1DecoderFail {
		String ver=null;
		try{
		Decoder dec = decoder.getContent();
		if(0!=RSA.type.compareTo(dec.getFirstObject(true).getString())) throw new ASN1DecoderFail("Not RSA");
		if (dec.getFirstObject(false).getTypeByte()==DD.TAG_AC0){
			ver = dec.getFirstObject(true).getString(DD.TAG_AC0);
		}
		p=dec.getFirstObject(true).getInteger();
    	if(DEBUG)System.out.println("p = "+p);		
		q=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("q = "+p);		
		N=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("N = "+N);
		d=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("d = "+d);	
		dp=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("dp = "+dp);	
		dq=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("dq = "+dq);	
		qInv=dec.getFirstObject(true).getInteger();
	   	if(DEBUG)System.out.println("qInv = "+qInv);	
	   	if(ver!=null) {
	   		hash_alg=dec.getFirstObject(true).getString();
	   		if(DEBUG)System.out.println("hash_alg = "+hash_alg);	
	   	}else
	   		hash_alg=Cipher.SHA256;
		if(dec.getFirstObject(false)!=null){
			comment=dec.getFirstObject(true).getString();
		} else comment = null;
	   	if(DEBUG)System.out.println("comment = "+comment);	
		}catch(java.lang.NullPointerException e){e.printStackTrace();throw new ASN1DecoderFail("Not RSA SK");};
		return this;
	}
	public byte[] hash_salt(byte[] msg) {
		if(DEBUG)System.out.println("RSA:hash_salt: msg="+Util.byteToHex(msg, ":"));
		return hash_salt(msg, this);
	}
	/**
	 * 
	 * @param msg
	 * @param sk
	 * @return
	 */
	public static byte[] hash_salt(byte[] msg, RSA_SK sk) {
		if(DEBUG)System.out.println("RSA:hash_salt: sk="+sk);
		return hash_salt(msg, sk.hash_alg, sk.N.bitLength());
	}
	/**
	 * 
	 * @param msg
	 * @param hash_alg
	 * @param N_bitLength
	 * @return  02 | salt[9] | hash[126] | 0[8]
	 */
	public static byte[] hash_salt(byte[] msg, String hash_alg, int N_bitLength) {
		if(DEBUG)System.out.println("RSA:hash_salt: ha="+hash_alg+", NbL="+N_bitLength);
		int hLen = 126;
		int sLen = 10;
		int emBits=N_bitLength-1; // emBits>8hLen+8sLen+9
		int emLen = (emBits+7)/8;
		MessageDigest digest;
		try {
		   	if(DEBUG)System.out.println("RSA:hash_salt = "+hash_alg);
			digest = MessageDigest.getInstance(hash_alg);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return null;}
		if(DEBUG)System.err.println("RSA: hash_salt: msg=["+msg.length+"]"); ///
		digest.update(msg);
		byte mHash[] = digest.digest();
		if(DEBUG)System.err.println("RSA: hash_salt: mHash=["+mHash.length+"]"+Util.byteToHex(mHash, ":")); ///_DEBUG
		hLen = mHash.length;
		if(emLen <= hLen+sLen+8){
			if(DEBUG)System.out.println("RSA:hash_salt: emLen:"+emLen+"<=8+hlen:"+hLen+"+sLen:"+sLen);
			return null;
		}
		byte salt[] = new byte[sLen];
		sr.nextBytes(salt);
		
		byte[] M1 = new byte[8+hLen+sLen];
		Util.copyBytes(M1, 0, salt, sLen, 0);
		Util.copyBytes(M1, sLen, mHash, hLen, 0);
		M1[0]=2;
		if(DEBUG)System.out.println("RSA:hash_salt:result="+Util.byteToHex(M1, " "));
		return M1;
	}
	/**
	 * Unpadding after decryption
	 * @param M
	 * @param sk
	 * @return
	 */
	public static byte[] unpad(byte[] M, RSA_SK sk){
		int k = (sk.N.bitLength()+7)/8;
		int j=0;
		if((M.length!=k-1)||((M.length==k-1)&&(M[0]!=2))) return null;
		//if((M.length<k-1)||(M[k-2]!=2)) return null;
		for(j=1;j<M.length;j++){
			if(M[j]==0) break;
		}
		if(j<1+11) return null;
		byte msg[] = new byte[M.length-j-1];
		j++;
		for(int i=0; i<msg.length; i++) msg[i] = M[j++];
		return msg;
	}
	public byte[] unpad(byte[] M){
		return unpad(M, this);
	}
	@Override
	public byte[] sign(byte[] padded_msg) {
		if(padded_msg == null){
			System.out.println("RSA:sign: null padded_msg");
			return null;
		}
		return sign(new BigInteger(padded_msg)).toByteArray();
	}
	public BigInteger sign(BigInteger padded_msg) {
		return this.decrypt(padded_msg);
	}
	@Override
	public byte[] decrypt_unpad(byte[] c) {
		return unpad(decrypt(c));
	}
	@Override
	public byte[] sign_pad_hash(byte[] c) {
		byte[] hash_salt=this.hash_salt(c);
		if(DEBUG)System.err.println("RSA: sign_pad_hash: hash_salt=["+hash_salt.length+"]"+Util.byteToHex(hash_salt, ":"));
		return sign(hash_salt);
	}
	@Override
	public RSA_PK getPK() {
		RSA_PK result = new RSA_PK();
		result.N = this.N;
		BigInteger totient = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
		result.e = d.modInverse(totient);
		result.hash_alg = hash_alg;
		result.comment = this.comment;
		return result;
	}
}
public class RSA extends ciphersuits.Cipher {
	public final static String type = Cipher.RSA;
	private static final int DD_PRIME_CERTAINTY = 10;
	RSA_SK sk = null; //new RSA_SK();
	RSA_PK pk = null; //new RSA_PK();
	public RSA(){}
	public RSA(RSA_SK _sk, RSA_PK _pk) {
		sk = _sk;
		pk = _pk;
	}
	public static SecureRandom sr = new SecureRandom();
	/**
	 * Generate keys with "size" primes and DD_PRIME_CERTAINTY and saves them in this object
	 * @param DD_PRIME_SIZE
	 * @param DD_PRIME_CERTAINTY
	 */
	public void genKey(int DD_PRIME_SIZE, int DD_PRIME_CERTAINTY) {
		sk = new RSA_SK();
		pk = new RSA_PK();
		genKey( DD_PRIME_SIZE, DD_PRIME_CERTAINTY,sr,sk,pk);
		sk.hash_alg = hash_alg;
		pk.hash_alg = hash_alg;
		sk.comment = this.comment;
		pk.comment = this.comment;
	}
	/**
	 * Generate keys and save them in preallocated parameters sk and pk
	 * @param DD_PRIME_SIZE
	 * @param DD_PRIME_CERTAINTY
	 * @param sr
	 * @param sk
	 * @param pk
	 */
	public static void genKey(int DD_PRIME_SIZE, int DD_PRIME_CERTAINTY, SecureRandom sr, RSA_SK sk, RSA_PK pk) {
		sk.p = new BigInteger(DD_PRIME_SIZE, DD_PRIME_CERTAINTY, sr);
		sk.q = new BigInteger(DD_PRIME_SIZE, DD_PRIME_CERTAINTY, sr);
		sk.N = sk.p.multiply(sk.q);
		pk.N = sk.N;
		BigInteger totient = sk.p.subtract(BigInteger.ONE).multiply(sk.q.subtract(BigInteger.ONE));
		boolean first = true;
		do{
			if(!first){pk.e = new BigInteger(DD_PRIME_SIZE/4, sr);}  // standard
			else {pk.e = new BigInteger("65537"); first = false;} //2^16+1    // fast
		}while(totient.gcd(pk.e).compareTo(BigInteger.ONE) != 0);
		sk.d = pk.e.modInverse(totient);
		sk.dp = sk.d.mod(sk.p.subtract(BigInteger.ONE));
		sk.dq = sk.d.mod(sk.q.subtract(BigInteger.ONE));
		sk.qInv = sk.q.modInverse(sk.p);
	}
	/**
	 * Generate keys and save them in preallocated parameters sk and pk
	 * @param sk
	 * @param p
	 * @param q
	 * @param e
	 */
	public static void genKey(RSA_SK sk, BigInteger p, BigInteger q, BigInteger e) {
		sk.p = p;
		sk.q = q;
		sk.N = sk.p.multiply(sk.q);
		BigInteger totient = sk.p.subtract(BigInteger.ONE).multiply(sk.q.subtract(BigInteger.ONE));
		sk.d = e.modInverse(totient);
		sk.dp = sk.d.mod(sk.p.subtract(BigInteger.ONE));
		sk.dq = sk.d.mod(sk.q.subtract(BigInteger.ONE));
		sk.qInv = sk.q.modInverse(sk.p);
	}
	public BigInteger encrypt(BigInteger m){
		return pk.encrypt(m);
	}
	@Override
	public byte[] encrypt(byte[] m) {
		return encrypt(new BigInteger(m)).toByteArray();
	}
	public BigInteger decrypt(BigInteger c){
		return decrypt(c, sk);
	}
	public static BigInteger decrypt(BigInteger c, RSA_SK sk){
		return sk.decrypt(c);
	}
	@Override
	public byte[] decrypt(byte[] c) {
		return decrypt(new BigInteger(c)).toByteArray();
	}
	public static BigInteger decrypt(byte[] c, RSA_SK sk){
		return sk.decrypt(new BigInteger(c));
	}
	/**
	 * padding for encryption
	 */
	public byte[] padding(byte[] msg){
		return pk.padding(msg);
	}
	/**
	 * unpadding after decryption
	 * @param m
	 * @return
	 */
	public byte[] unpad(BigInteger m){
		return unpad(m,sk);
	}
	/**
	 * Unpadding after decryption
	 * @param m
	 * @param sk
	 * @return
	 */
	public static byte[] unpad(BigInteger m, RSA_SK sk){
		return sk.unpad(m.toByteArray());
	}
	@Override
	public byte[] unpad(byte[] m) {
		return RSA_SK.unpad(m,sk);
	}
	/**
	 * Generate keys with "size" primes and DD_PRIME_CERTAINTY and saves them in this object
	 */
	@Override
	public SK genKey(int size) {
		this.genKey(size, DD_PRIME_CERTAINTY);
		return sk;
	}
	@Override
	public PK getPK() {
		return pk;
	}
	@Override
	public SK getSK() {
		return sk;
	}
	@Override
	public byte[] sign(byte[] m) {
		return sk.sign_pad_hash(m);
	}
	@Override
	public boolean verify(byte[] signature, byte[] message) {
		return pk.verify_unpad_hash(signature, message);
	}
	@Override
	public byte[] hash_salt(byte[] msg) {
		return sk.hash_salt(msg);
	}
	public static void umain(String[]args){
		RSA_PK pk = new RSA_PK();
		RSA_SK sk = new RSA_SK();
		ciphersuits.RSA.genKey(2048,8, ciphersuits.RSA.sr, sk,pk);
		byte[] msg = new byte[]{1,2,3,4,5};
		System.out.println("msg="+Util.byteToHex(msg, " "));
		byte[]c=pk.encrypt_pad(msg);
		System.out.println("c="+Util.byteToHex(c, " "));
		byte[]p=sk.decrypt_unpad(c);
		System.out.println("p="+Util.byteToHex(p, " "));
	}
	public static void main(String[]args){
		//RSA_PK pk = new RSA_PK();
		//RSA_SK sk = new RSA_SK();
		Cipher rsa = Cipher.getCipher("RSA", "SHA1",null);
		SK sk= rsa.genKey(2048);
		PK pk = rsa.getPK();
		byte[] msg = new byte[]{1,2,3,4,5};
		System.out.println("msg="+Util.byteToHex(msg, " "));
		byte[]c=pk.encrypt_pad(msg);
		System.out.println("c="+Util.byteToHex(c, " "));
		byte[]p = sk.decrypt_unpad(c);
		System.out.println("p="+Util.byteToHex(p, " "));
	}
	public static void smain(String[]args){
		Cipher rsa = Cipher.getCipher("RSA", "SHA1",null);
		SK sk= rsa.genKey(2048);
		PK pk = rsa.getPK();
		byte[] msg = new byte[]{1,2,3,4,5};
		System.out.println("msg="+Util.byteToHex(msg, " "));
		byte[] sign = rsa.sign(msg);
		System.out.println("sign="+Util.byteToHex(sign, " "));
		boolean verif = rsa.verify(sign, msg);
		System.out.println("verif="+verif);
	}
	@Override
	public String getType() {
		return Cipher.RSA+this.cipherTypeSeparator+this.hash_alg;
	}
}

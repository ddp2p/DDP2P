package ciphersuits;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

class DSA_PK extends PK {
	final static String V0="0";
	String version = V0; // version to write out, decoding converts to this version
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String hash_alg=Cipher.SHA256;

	public DSA_PK(Decoder decoder) throws ASN1DecoderFail {
		decode(decoder);
	}

	/**
	 * Not applicable
	 */
	@Override
	public byte[] encrypt(byte[] m) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}

	@Override
	public byte[] encrypt_pad(byte[] m) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}

	/**
	 * This is the "academic" version without hashing/padding of messages
	 */
	@Override
	public boolean verify(byte[] signature, byte[] hashed) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * This is the method that performs the whole verification from a message
	 */
	@Override
	public boolean verify_unpad_hash(byte[] signature, byte[] message) {
		return verify(signature, CryptoUtils.digest(message, this.hash_alg));
	}

	@Override
	public CipherSuit getCipherSuite() {
		CipherSuit result = new CipherSuit();
		result.cipher = Cipher.DSA;
		//result.ciphersize = p.bitLength();
		result.hash_alg = hash_alg;
		return result;
	}
	/**
	 * To test if the keys have the same components
	 */
	@Override
	public boolean __equals(PK o) {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * ASN1 encoder: not required in this project
	 */
	@Override
	public Encoder getEncoder() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * ASN1 decoder: not required in this project
	 */
	@Override
	public DSA_PK decode(Decoder dec) throws ASN1DecoderFail {
		// TODO Auto-generated method stub
		return this;
	}
}

class DSA_SK extends SK {
	//final static String type="DSA";
	final static String V0="0";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String hash_alg;
	/**
	 * Create a new key
	 * @param size
	 * @param ddPrimeCertainty
	 */
	public DSA_SK(int size, int ddPrimeCertainty) {
		// TODO Auto-generated constructor stub
	}
	public DSA_SK(Decoder d) throws ASN1DecoderFail {
		decode(d);
	}
	/**
	 * Not applicable
	 */
	@Override
	public byte[] decrypt(byte[] c) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}
	@Override
	public byte[] decrypt_unpad(byte[] c) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}
	/**
	 * The academic version: without padding
	 */
	@Override
	public byte[] sign(byte[] message) {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * The standard version: with padding
	 */
	@Override
	public byte[] sign_pad_hash(byte[] message) {
		return sign(CryptoUtils.digest(message, this.hash_alg));
	}
	/**
	 * Extract the public key
	 */
	@Override
	public DSA_PK getPK() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Test equality between secret keys
	 */
	@Override
	public boolean sameAs(SK myPeerSK) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Object getType() {
		return DSA.type;
	}
	/**
	 * Not requested in this project
	 */
	@Override
	public Encoder getEncoder() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Not requested in this project
	 */
	@Override
	public DSA_SK decode(Decoder dec) throws ASN1DecoderFail {
		// TODO Auto-generated method stub
		return this;
	}
//	/**
//	 * hash and add salt to the message
//	 * @param msg
//	 * @return
//	 */
//	public byte[] hash_salt(byte[] msg) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	public static byte[] unpad(byte[] m, DSA_SK sk) {
//		// TODO Auto-generated method stub
//		return null;
//	}
}

public class DSA extends ciphersuits.Cipher {
	public final static String type = Cipher.DSA;
	private static final int DD_PRIME_CERTAINTY = 10;
	DSA_SK sk = null; //new RSA_SK();
	DSA_PK pk = null; //new RSA_PK();
	public DSA(){}

	public DSA(DSA_SK sk2, DSA_PK pk2) {
		sk = sk2;
		pk = pk2;
	}

	@Override
	public SK genKey(int size) {
		this.genKey(size, DD_PRIME_CERTAINTY);
		return sk;
	}
	/**
	 * Implement a function generating a key with the given size and prime certainty
	 * @param size
	 * @param ddPrimeCertainty
	 */
	private DSA_SK genKey(int size, int ddPrimeCertainty) {
		sk = new DSA_SK(size, ddPrimeCertainty);
		sk.hash_alg = hash_alg;
		return sk;
	}

	@Override
	public String getType() {
		return Cipher.DSA + Cipher.cipherTypeSeparator + this.hash_alg;
	}

	@Override
	public PK getPK() {
		if (pk != null)
			return pk;
		if (sk != null) return sk.getPK();
		return null;
	}

	@Override
	public SK getSK() {
		return sk;
	}

	@Override
	public byte[] padding(byte[] msg) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}

	@Override
	public byte[] unpad(byte[] m) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}

	@Override
	public byte[] hash_salt(byte[] msg) {
		Util.printCallPath("N/A");
		throw new RuntimeException("Not Applicable");
	}

	@Override
	public byte[] sign(byte[] m) {
		if(sk == null) return null;
		return sk.sign(m);
	}

	@Override
	public boolean verify(byte[] signature, byte[] message) {
		//return pk.verify_unpad_hash(signature, message);
		if ((pk == null) && (sk !=null))
			pk = sk.getPK();
		if(pk == null) return false;
		return pk.verify(signature, message);
	}
	public static void main(String[]args){
		Cipher dsa = Cipher.getCipher("DSA", "SHA1",null);
		SK sk= dsa.genKey(2048);
		PK pk = dsa.getPK();
		byte[] msg = new byte[]{1,2,3,4,5};
		System.out.println("msg="+Util.byteToHex(msg, " "));
		byte[]c=pk.encrypt_pad(msg);
		System.out.println("c="+Util.byteToHex(c, " "));
		byte[]p = sk.decrypt_unpad(c);
		System.out.println("p="+Util.byteToHex(p, " "));
	}
}
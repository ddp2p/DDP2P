package ciphersuits;

import java.math.BigInteger;

import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

class ECC_PK extends PK {
	final static String type="ECC";
	final static String V0="0";
	String version = V0; // version to write out, decoding converts to this version
	private static final boolean DEBUG = false;
	BigInteger p;
	BigInteger a;
	BigInteger b;
	BigInteger x1; // base point
	BigInteger y1;
	BigInteger xn; // public key
	BigInteger yn;

	@Override
	public byte[] encrypt(byte[] m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] encrypt_pad(byte[] m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verify(byte[] signature, byte[] hashed) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verify_unpad_hash(byte[] signature, byte[] message) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Encoder getEncoder() {
		Encoder r = new Encoder().initSequence();
		r.addToSequence(new Encoder(type));
		r.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		r.addToSequence(new Encoder(p));
		r.addToSequence(new Encoder(a));
		r.addToSequence(new Encoder(b));
		r.addToSequence(new Encoder(x1));
		r.addToSequence(new Encoder(y1));
		r.addToSequence(new Encoder(xn));
		r.addToSequence(new Encoder(yn));
		return r;
	}

	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		String ver;
		Decoder d = dec.getContent();
		if(0!=type.compareTo(d.getFirstObject(true).getString())) throw new ASN1DecoderFail("Not ECC");
		if(DEBUG)System.out.println("ECC_PK:decode: ECC");
		if(d.getFirstObject(false).getTypeByte()==DD.TAG_AC0){
			ver = d.getFirstObject(true).getString();
		}
		p = d.getFirstObject(true).getInteger();
		a = d.getFirstObject(true).getInteger();
		b = d.getFirstObject(true).getInteger();
		x1 = d.getFirstObject(true).getInteger();
		y1 = d.getFirstObject(true).getInteger();
		xn = d.getFirstObject(true).getInteger();
		yn = d.getFirstObject(true).getInteger();
		return this;
	}
	
}

class ESS_SK extends SK{
	final static String type="ECC";
	final static String V0="0";
	String version = V0; // version to write out, decoding converts to this version
	BigInteger p;
	BigInteger a;
	BigInteger b;
	BigInteger x1; // base point
	BigInteger y1;
	BigInteger n; // secret key
	BigInteger xn; // public key
	BigInteger yn;

	@Override
	public byte[] decrypt(byte[] c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] decrypt_unpad(byte[] c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] sign(byte[] c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] sign_pad_hash(byte[] c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PK getPK() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean sameAs(SK myPeerSK) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Encoder getEncoder() {
		Encoder r = new Encoder().initSequence();
		r.addToSequence(new Encoder(type));
		r.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		r.addToSequence(new Encoder(p));
		r.addToSequence(new Encoder(a));
		r.addToSequence(new Encoder(b));
		r.addToSequence(new Encoder(x1));
		r.addToSequence(new Encoder(y1));
		r.addToSequence(new Encoder(n));
		r.addToSequence(new Encoder(xn));
		r.addToSequence(new Encoder(yn));
		return r;
	}

	@Override
	public ESS_SK decode(Decoder dec) throws ASN1DecoderFail {
		String ver;
		Decoder d = dec.getContent();
		if(0!=type.compareTo(d.getFirstObject(true).getString())) throw new ASN1DecoderFail("Not ECC");
		if(d.getFirstObject(false).getTypeByte()==DD.TAG_AC0){
			ver = d.getFirstObject(true).getString();
		}
		p = d.getFirstObject(true).getInteger();
		a = d.getFirstObject(true).getInteger();
		b = d.getFirstObject(true).getInteger();
		x1 = d.getFirstObject(true).getInteger();
		y1 = d.getFirstObject(true).getInteger();
		n = d.getFirstObject(true).getInteger();
		xn = d.getFirstObject(true).getInteger();
		yn = d.getFirstObject(true).getInteger();
		return this;
	}
	
}
public class ECC extends ciphersuits.Cipher{

	@Override
	public SK genKey(int size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PK getPK() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SK getSK() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] padding(byte[] toencryption) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] unpad(byte[] decrypted) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] hash_salt(byte[] msg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] sign(byte[] m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verify(byte[] signature, byte[] message) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
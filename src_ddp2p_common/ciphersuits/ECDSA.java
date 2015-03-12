/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Marius C. Silaghi
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
import java.security.SecureRandom;
import java.util.Hashtable;

import util.Util;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
class ECDSA_PK extends PK {
	final static String V0="0";
	String version = V0; // version to write out, decoding converts to this version
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int ECC_curve_ID;
	ECC curve;
	//BigInteger p;
	//BigInteger a;
	//BigInteger b;
	EC_Point x;
	//BigInteger x1; // base point
	//BigInteger y1;
	EC_Point y;
	//BigInteger xn; // public key
	//BigInteger yn;
	BigInteger n;
	String hash_alg = Cipher.SHA1;
	public boolean _equals(PK _pk){
		//boolean DEBUG = true;
		if(DEBUG)System.out.println("ECDSA_PK:_equal: start");
		if(!(_pk instanceof ECDSA_PK)) return false;
		ECDSA_PK pk = (ECDSA_PK)_pk;
		if (pk == null) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: pk null");
			return false;
		}
		if (curve == null) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: curve null");
			return false;
		}
		if (x == null) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: x null");
			return false;
		}
		if (n == null) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: n null");
			return false;
		}
		if (hash_alg == null) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: ha null");
			return false;
		}
		if (!x.equals(pk.x)) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: x==x null");
			return false;
		}
		if (!n.equals(pk.n)) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: n==n null");
			return false;
		}
		if (!hash_alg.equals(pk.hash_alg)) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: ha==ha null");
			return false;
		}
		if (ECC_curve_ID != pk.ECC_curve_ID) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: EI==EI null");
			return false;
		}
		if (!curve.equals(pk.curve)) {
			if(DEBUG)System.out.println("ECDSA_PK:equal: c==c null");
			return false;
		}
		if(DEBUG)System.out.println("ECDSA_PK:equal: true");
		return true;
	}
	@Override
	public boolean __equals(PK __pk){
		//if (!(__pk instanceof PK)) return false;
		PK _pk = (PK) __pk;
		if(DEBUG)System.out.println("ECDSA_PK:__equal: start");
		boolean result = _equals(_pk);
		if(!result) {
			if(_DEBUG)System.out.println("ECDSA_PK:equal: this="+this+"\nvs "+_pk);
		}
		return result;
	}
	public String toString() {
		return "ECDSA_PK[ID="+this.ECC_curve_ID+"\n"
				+ " y="+EC_Point.toString(y,curve)+" \n"
				+ " x="+EC_Point.toString(x,curve)+" \n"
//				+ " y="+y+"\n"
//				+ " x="+x+"\n"
				+ " curve="+curve+"\n"
				+ "]";
	}
	/**
	 * Decodes from ASN1
	 * @param d
	 * @throws ASN1DecoderFail
	 */
	public ECDSA_PK(Decoder d) throws ASN1DecoderFail {
		decode(d);
		if (DEBUG) System.out.println("ECCPK="+this);
	}
	/**
	 * Get the public key for curve curve_ID, with public key point _y=m*_x
	 * @param curve_ID
	 * @param y
	 */
	public ECDSA_PK(int curve_ID, EC_Point y) {
		init(curve_ID, y);
	}
	/**
	 * Get the public key for curve curve_ID, with public key point (_x,_y)
	 * @param curve_ID
	 * @param _x
	 * @param _y
	 */
	public ECDSA_PK(int curve_ID, BigInteger _Qx, BigInteger _Qy) {
		/**
		 * No need to set the curve now since it is set in init.
		 */
		EC_Point y = new EC_Point(_Qx, _Qy, null);
		init(curve_ID, y);
	}
	/**
	 * Get the public key for curve eCC_curve_ID/_curve (unprocessed -- as they are provided),
	 *  with base point _x of order _n, and public key point _y=m*_x
	 * (for unknown ms).
	 * @param eCC_curve_ID
	 * @param _curve
	 * @param _x
	 * @param _y
	 * @param _n
	 */
	public ECDSA_PK(int eCC_curve_ID, ECC _curve, EC_Point _x, EC_Point _y, BigInteger _n) {
		ECC_curve_ID = eCC_curve_ID;
		curve = _curve;
		x = _x;
		y = _y;
		n = _n;
	}
	/**
	 * Identifies and load the curve with the proper ID (verified with ECC.getCurveID).
	 * Then it loads the curve itself, and sets it in y.
	 * @param curve_ID
	 * @param y
	 */
	void init(int curve_ID, EC_Point y) {
		ECC_curve_ID = ECC.getCurveID(curve_ID);
		ECS ecs = ECS.getECS(curve_ID);
		curve = ecs.curve;
		x = ecs.g;
		this.y = y;
		y._curve = curve;
		this.n = ecs.n;
	}
	
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

	/**
	 * Assumes parameter is already hashed
	 */
	@Override
	public boolean verify(byte[] signature, byte[] hashed) {
		ECDSA_Signature sign;
		try {
			if (signature == null) {
				return false;
			}
			sign = new ECDSA_Signature(signature);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return false;
		}
		BigInteger w = (sign.s.modInverse(n));
		BigInteger i = w.multiply(Util.bigIntegerFromUnsignedBytes(hashed)).mod(n);
		BigInteger j = w.multiply(sign.r).mod(n);
		EC_Point iA = ECC.mul(x, i);
		EC_Point jB = ECC.mul(y, j);
		EC_Point _sign = ECC.static_add(iA, jB);
		if(DEBUG) System.out.println("ECDSA: w="+w+" i="+i+" j="+j+" iA="+iA+" jB="+jB+" sg="+_sign+
				" x="+x+" y="+y);
		return _sign.getX().mod(n).equals(sign.r);
	}
	/**
	 * Computes hash and then verifies
	 */
	@Override
	public boolean verify_unpad_hash(byte[] signature, byte[] message) {
		return verify(signature, CryptoUtils.digest(message, this.hash_alg));
	}

	@Override
	public Encoder getEncoder() {
		Encoder r = new Encoder().initSequence();
		r.addToSequence(new Encoder(ECDSA.type));
		r.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		if(this.ECC_curve_ID < 0) return getEncoderRaw(r);
		
		r.addToSequence(new Encoder(ECC_curve_ID));
		r.addToSequence(y.getEncoder());
		r.addToSequence(new Encoder(hash_alg));
		return r;
	}
	public Encoder getEncoderRaw(Encoder r) {
			r.addToSequence(curve.getEncoder());
			r.addToSequence(x.getEncoder());
			r.addToSequence(y.getEncoder());
			r.addToSequence(new Encoder(n));
			r.addToSequence(new Encoder(hash_alg));
			return r;
	}

	@Override
	public ECDSA_PK decode(Decoder dec) throws ASN1DecoderFail {
		String ver;
		Decoder d = dec.getContent();
		if (0 != ECDSA.type.compareTo(d.getFirstObject(true).getString())) throw new ASN1DecoderFail("Not ECC");
		if (DEBUG) System.out.println("ECC_PK:decode: ECC");
		if (d.getFirstObject(false).getTypeByte()==DD.TAG_AC0){
			ver = d.getFirstObject(true).getString(DD.TAG_AC0);
		}
		if (d.getFirstObject(false).getTypeByte() != Encoder.TAG_INTEGER)
			return decodeRaw(d);
		ECC_curve_ID = d.getFirstObject(true).getInteger().intValue();
		y = new EC_Point(d.getFirstObject(true));
		init(ECC_curve_ID, y);
		if (d.getTypeByte() != 0)
			hash_alg = d.getFirstObject(true).getString();
		return this;
	}		
	public ECDSA_PK decodeRaw(Decoder d) throws ASN1DecoderFail {
		curve = new ECC(d.getFirstObject(true));
		x = new EC_Point(d.getFirstObject(true));
		y = new EC_Point(d.getFirstObject(true));
		n = d.getFirstObject(true).getInteger();
		if (d.getTypeByte() != 0)
			hash_alg = d.getFirstObject(true).getString();
		return this;
	}

	@Override
	public CipherSuit getCipherSuite() {
		CipherSuit result = new CipherSuit();
		result.cipher = Cipher.ECDSA;
		result.ciphersize = this.ECC_curve_ID;
		result.hash_alg = hash_alg;
		return result;
	}
	
}
class ECDSA_SK extends SK{
	final static String V0="0";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	String version = V0; // version to write out, decoding converts to this version
	int ECC_curve_ID;
	//BigInteger p;
	//BigInteger a;
	//BigInteger b;
	ECC curve;
	EC_Point x; // base point
	//BigInteger x1; // base point
	//BigInteger y1;
	//boolean _y1;
	BigInteger m; // secret key
	EC_Point y; //public key
	//BigInteger xm; // public key
	//BigInteger ym;
	//boolean _ym;
	BigInteger n; // order of x
	String hash_alg = Cipher.SHA1;
	public String toString() {
		return "ECDSA_SK[ID="+ECC_curve_ID+"\n"
				+ " m="+m+" \n"
				+ " y="+EC_Point.toString(y,curve)+" \n"
				+ " x="+EC_Point.toString(x,curve)+" \n"
				+ " curve="+curve+"\n"
				+ " hash="+hash_alg+"\n"
				+ "]";
	}

	public ECDSA_SK(Decoder d) throws ASN1DecoderFail{
		decode(d);
	}
	/**
	 * Curve id and secret point m
	 * @param id
	 * @param _m
	 */
	public ECDSA_SK(int id, BigInteger _m) {
		init(id);
		m = _m;
		y = ECC.mul(x, m);
		//System.out.println("m="+m.toString(16));
		//System.out.println("y="+y);
	}
	public ECDSA_SK(int curve_ID) {
		init(curve_ID);
		SecureRandom random = new SecureRandom();
		do {
			m = new BigInteger(n.bitLength(), random);
		} while(
				(m.compareTo(n)>=0)
				|| m.equals(BigInteger.ZERO)
				|| (m.gcd(n).compareTo(BigInteger.ONE) != 0)
				);
		y = ECC.mul(x, m);
	}
	public ECDSA_SK(ECC ec, EC_Point x2, BigInteger m2, BigInteger n2) {
		this.curve = ec;
		this.x = x2;
		this.m = m2;
		this.n = n2;
		y = ECC.mul(x, m);
	}

	void init(int curve_ID) {
		ECC_curve_ID = ECC.getCurveID(curve_ID);
		ECS ecs = ECS.getECS(curve_ID);
		if (ecs == null) {
			Util.printCallPath("curveID: "+curve_ID);
			//return;
			throw new RuntimeException("ECDSA_SK: no curveID: "+curve_ID);
		}
		curve = ecs.curve;
		x = ecs.g;
		n = ecs.n;
		
//		System.out.println("a="+curve.a);
//		System.out.println("b="+curve.b.toString(16));
//		System.out.println("p="+curve.p);
//		System.out.println("n="+n);
//		System.out.println("x="+x);

		
		EC_Point test = new EC_Point(x);
		test.compress();
		test.y = null;
		
		test.decompress();

		//System.out.println("Test4: "+test+" vs "+x);
		
		if(!test.getY().equals(x.getY())) {
			System.out.println("Wrong base point Gx: "+x);
			throw new RuntimeException("Wrong base point Gx-> "+test); 
			//x = test;
		}else{
			if (DEBUG) System.out.println("Right x: "+test+" vs "+x);
		}
		//p = curve.curve.p;
		//a = curve.curve.a;
		//b = curve.curve.b;
		//x1 = curve.g.x;
		//y1 = curve.g.getY();
		//_y1 = curve.g.getCompressedY();
	}
	
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
	/**
	 * 
	 * @param hashed
	 * @param k
	 * @return
	 */
	public byte[] sign(byte[] hashed, BigInteger k) {
		k = k.mod(n);
		BigInteger k_1 = k.modInverse(n);
		EC_Point kA = ECC.mul(x, k);
		BigInteger r = kA.getX().mod(n);
		BigInteger s = k_1.multiply(Util.bigIntegerFromUnsignedBytes(hashed).add(m.multiply(r))).mod(n);
		return new ECDSA_Signature(r,s).encode();
	}
	/**
	 * Parameter is assumed already hashed.
	 * The randomizing k factor is built by hashing
	 *  the hash of the incoming message
	 *  and the secret key;
	 * 
	 */
	@Override
	public byte[] sign(byte[] hashed) {
		//SecureRandom random = new SecureRandom();
		//BigInteger k = new BigInteger (n.bitLength(), random);
		byte[][]el = new byte[][]{hashed, m.toByteArray()};
		byte[] hash = CryptoUtils.digest(el, RSA.SHA256);
		BigInteger k = Util.bigIntegerFromUnsignedBytes(hash);
		return sign(hashed, k);
	}
	/**
	 * Parameter will be hashed first
	 */
	@Override
	public byte[] sign_pad_hash(byte[] message) {
		return sign(CryptoUtils.digest(message, this.hash_alg));
	}

	@Override
	public ECDSA_PK getPK() {
		ECDSA_PK e = new ECDSA_PK(ECC_curve_ID, curve, x, y, n);
		e.hash_alg = hash_alg;
		return e;
	}

	@Override
	public boolean sameAs(SK myPeerSK) {
		if(!(myPeerSK instanceof ECDSA_SK)) return false;
		ECDSA_SK esk = (ECDSA_SK) myPeerSK;
		if(this.ECC_curve_ID != esk.ECC_curve_ID) return false;
		if(!this.m.equals(esk.m)) return false;
		return true;
	}

	@Override
	public Object getType() {
		return ECDSA.type;
	}


	@Override
	public Encoder getEncoder() {
		Encoder r = new Encoder().initSequence();
		r.addToSequence(new Encoder(ECDSA.type));
		r.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC0));
		if (hash_alg != null) r.addToSequence(new Encoder(hash_alg).setASN1Type(DD.TAG_AC1));
		if (this.ECC_curve_ID < 0) return getEncoderRaw(r);
		
		r.addToSequence(new Encoder(ECC_curve_ID));
		//r.addToSequence(x.getEncoder());
		r.addToSequence(new Encoder(m));
		r.addToSequence(y.getEncoder());
		if (comment!=null) r.addToSequence(new Encoder(comment));
		return r;
	}
	public Encoder getEncoderRaw(Encoder r) {
			r.addToSequence(curve.getEncoder());
			r.addToSequence(x.getEncoder());
			r.addToSequence(new Encoder(m));
			r.addToSequence(y.getEncoder());
			r.addToSequence(new Encoder(n));
			if (comment!=null) r.addToSequence(new Encoder(comment));
			return r;
	}

	@Override
	public ECDSA_SK decode(Decoder dec) throws ASN1DecoderFail {
		String ver;
		//byte tag;
		Decoder d = dec.getContent();
		if (0 != ECDSA.type.compareTo(d.getFirstObject(true).getString()))
			throw new ASN1DecoderFail("Not ECC");
		if (DEBUG) System.out.println("ECC_PK:decode: ECC");
		if (d.getFirstObject(false).getTypeByte()==DD.TAG_AC0) {
			ver = d.getFirstObject(true).getString(DD.TAG_AC0);
		}
		if (d.getFirstObject(false).getTypeByte()==DD.TAG_AC1) {
			hash_alg = d.getFirstObject(true).getString(DD.TAG_AC1);
		}
		// could test ver with version... if there are more versions
		if (d.getFirstObject(false).getTypeByte() != Encoder.TAG_INTEGER)
			return decodeRaw(d);
		ECC_curve_ID = d.getFirstObject(true).getInteger().intValue();
		init(ECC_curve_ID);
		//x = new EC_Point(curve, d.getFirstObject(true));
		m = d.getFirstObject(true).getInteger();
		if ((d.getFirstObject(false) != null) && (d.getTypeByte() == EC_Point.getASN1TAG() )) {
			if (DEBUG) System.out.println("ECDSA: decode: y");
			y = new EC_Point(curve, d.getFirstObject(true));
			if (DEBUG) System.out.println("ECDSA: decode: y ="+y);
			//y = x.mul(m);
			//if (_DEBUG) System.out.println("ECDSA: decode: y?="+y);
		} else {
			y = x.mul(m);
			if (DEBUG) System.out.println("ECDSA: decode: y<="+y);
		}
		if (d.getFirstObject(false) != null) {
			comment = d.getFirstObject(true).getString();
		} else comment = null;
		return this;
	}
	public ECDSA_SK decodeRaw(Decoder d) throws ASN1DecoderFail {
		curve = new ECC(d.getFirstObject(true));
		x = new EC_Point(curve, d.getFirstObject(true));
		m = d.getFirstObject(true).getInteger();
		y = new EC_Point(curve, d.getFirstObject(true));
		n = d.getFirstObject(true).getInteger();
		if(d.getFirstObject(false)!=null){
			comment=d.getFirstObject(true).getString();
		} else comment = null;
		return this;
	}
	
}

/**
 * A class for standard elliptic curves.
 * @author msilaghi
 *
 */
class ECS {
	static
	class ECSDesc {
		public ECSDesc(String[] s) {
			name = s[0];
			p = Util.cleanInnerSpaces(s[1]);
			n = Util.cleanInnerSpaces(s[2]);
			SEED = Util.cleanInnerSpaces(s[3]);
			c = Util.cleanInnerSpaces(s[4]);
			b = Util.cleanInnerSpaces(s[5]);
			Gx = Util.cleanInnerSpaces(s[6]);
			Gy = Util.cleanInnerSpaces(s[7]);
			a = Util.cleanInnerSpaces(s[8]);
			//System.out.println("s="+s[1]);
			//System.out.println("p="+p);
		}
		String name;
		String p;
		String n;
		String SEED; // seed of SHA-1
		String c; // output of SHA-1
		String b;
		String Gx;
		String Gy;
		String a;
	}
	static String[][] standards = {
//		{
//			"P-192",
//			"6277101735386680763835789423207666416083908700390324961279",
//			"6277101735386680763835789423176059013767194773182842284081",
//			"3045ae6fc8422f64ed579528d38120eae12196d5",
//			"3099d2bbbfcb2538542dcd5fb078b6ef5f3d6fe2c745de65",
//			"64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1",
//			"188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012",
//			"07192b95ffc8da78631011ed6b24cdd573f977a11e794811",
//			"-3"
//		},
			{
				"P-192", // p = 2^192 - 2^64 - 1
				"6277101735386680763835789423207666416083908700390324961279",
				"6277101735386680763835789423176059013767194773182842284081",
				"3045ae6f c8422f64 ed579528 d38120ea e12196d5",
				"3099d2bb bfcb2538 542dcd5f b078b6ef 5f3d6fe2 c745de65",
				"64210519 e59c80e7 0fa7e9ab 72243049 feb8deec c146b9b1",
				"188da80e b03090f6 7cbf20eb 43a18800 f4ff0afd 82ff1012",
				"07192b95 ffc8da78 631011ed 6b24cdd5 73f977a1 1e794811",
				"-3",
			},
			{
				"P-224", // p = 2^224 - 2^96 + 1
				"26959946667150639794667015087019630673557916260026308143510066298881",
				"26959946667150639794667015087019625940457807714424391721682722368061",
				"bd71344799d5c7fcdc45b59fa3b9ab8f6a948bc5",
				"5b056c7e11dd68f40469ee7f3c7a7d74f7d121116506d031218291fb",
				"b4050a850c04b3abf54132565044b0b7d7bfd8ba270b39432355ffb4",
				"b70e0cbd6bb4bf7f321390b94a03c1d356c21122343280d6115c1d21",
				"bd376388b5f723fb4c22dfe6cd4375a05a07476444d5819985007e34",
				"-3"
			},
//			{
//				"P-224", // p = 2^224 - 2^96 + 1
//				"26959946667150639794667015087019630673557916260026308143510066298881",
//				"26959946667150639794667015087019625940457807714424391721682722368061",
//				"bd713447 99d5c7fc dc45b59f a3b9ab8f 6a948bc5",
//				"5b056c7e 11dd68f4 0469ee7f 3c7a7d74 f7d12111 6506d031 218291fb",
//				"b4050a85 0c04b3ab f5413256 5044b0b7 d7bfd8ba 270b3943 2355ffb4",
//				"b70e0cbd 6bb4bf7f 321390b9 4a03c1d3 56c21122 343280d6 115c1d21",
//				"bd376388 b5f723fb 4c22dfe6 cd4375a0 5a074764 44d58199 85007e34",
//				"-3"
//			},
			{
				"P-256", // p=2^256 - 2^224 +2^192 + 2^96 - 1
				"115792089210356248762697446949407573530086143415290314195533631308867097853951",
				"115792089210356248762697446949407573529996955224135760342422259061068512044369",
				"c49d3608 86e70493 6a6678e1 139d26b7 819f7e90",
				"7efba166 2985be94 03cb055c 75d4f7e0 ce8d84a9 c5114abc af317768 0104fa0d",
				"5ac635d8 aa3a93e7 b3ebbd55 769886bc 651d06b0 cc53b0f6 3bce3c3e 27d2604b",
				"6b17d1f2 e12c4247 f8bce6e5 63a440f2 77037d81 2deb33a0 f4a13945 d898c296",
				"4fe342e2 fe1a7f9b 8ee7eb4a 7c0f9e16 2bce3357 6b315ece cbb64068 37bf51f5",
				"-3"
			},
			{
				"P-384", // p=2^384 - 2^128 -2^96 +2^32 - 1
				"3940200619639447921227904010014361380507973927046544666794 8293404245721771496870329047266088258938001861606973112319",
				"3940200619639447921227904010014361380507973927046544666794 6905279627659399113263569398956308152294913554433653942643",
				"a335926a a319a27a 1d00896a 6773a482 7acdac73",
				"79d1e655 f868f02f ff48dcde e14151dd b80643c1 406d0ca1 0dfe6fc5 2009540a 495e8042 ea5f744f 6e184667 cc722483",
				"b3312fa7 e23ee7e4 988e056b e3f82d19 181d9c6e fe814112 0314088f 5013875a c656398d 8a2ed19d 2a85c8ed d3ec2aef",
				"aa87ca22 be8b0537 8eb1c71e f320ad74 6e1d3b62 8ba79b98 59f741e0 82542a38 5502f25d bf55296c 3a545e38 72760ab7",
				"3617de4a 96262c6f 5d9e98bf 9292dc29 f8f41dbd 289a147c e9da3113 b5f0b8c0 0a60b1ce 1d7e819d 7a431d7c 90ea0e5f",
				"-3"
			},
			{
				"P-521", // p = 2^521 - 1
				"686479766013060971498190079908139321726943530014330540939 446345918554318339765605212255964066145455497729631139148 0858037121987999716643812574028291115057151",
				"686479766013060971498190079908139321726943530014330540939 446345918554318339765539424505774633321719753296399637136 3321113864768612440380340372808892707005449",
				"d09e8800 291cb853 96cc6717 393284aa a0da64ba",
				"0b4 8bfa5f42 0a349495 39d2bdfc 264eeeeb 077688e4 4fbf0ad8 f6d0edb3 7bd6b533 28100051 8e19f1b9 ffbe0fe9 ed8a3c22 00b8f875 e523868c 70c1e5bf 55bad637",
				"051 953eb961 8e1c9a1f 929a21a0 b68540ee a2da725b 99b315f3 b8b48991 8ef109e1 56193951 ec7e937b 1652c0bd 3bb1bf07 3573df88 3d2c34f1 ef451fd4 6b503f00",
				"0c6 858e06b7 0404e9cd 9e3ecb66 2395b442 9c648139 053fb521 f828af60 6b4d3dba a14b5e77 efe75928 fe1dc127 a2ffa8de 3348b3c1 856a429b f97e7e31 c2e5bd66",
				"118 39296a78 9a3bc004 5c8a5fb4 2c7d1bd9 98f54449 579b4468 17afbd17 273e662c 97ee7299 5ef42640 c550b901 3fad0761 353c7086 a272c240 88be9476 9fd16650",
				"-3"
			}
	};
	static Hashtable<Integer, ECS> curves = new Hashtable<Integer, ECS>();
	ECC curve;
	EC_Point g;
	BigInteger n;
	int curve_ID;
	/**
	 * Construct a curve with the values provided as parameters.
	 * @param ID
	 * @param curve
	 * @param g
	 * @param n
	 */
	public ECS(int ID, ECC curve, EC_Point g, BigInteger n) {
		this.curve_ID = ID;
		this.curve = curve;
		this.g = g;
		g._curve = curve;
		this.n = n;
	}
	/**
	 * Gets a standard curve with the provided ID
	 * @param ID
	 * @return
	 */
	public static ECS getECS(int ID) {
		ECS ecs;
		/**
		 * First converts nonstandard IDs (e.g., key sizes) into the curve IDs, repeating the work of procedure: ECC.getCurveID
		 */
		switch(ID) {
		case 119: ID = ECDSA.P_119; break;
		case 224: ID = ECDSA.P_224; break;
		case 256: ID = ECDSA.P_256; break;
		case 384: ID = ECDSA.P_384; break;
		case 521: ID = ECDSA.P_521; break;
		}
		
		switch (ID) {
		// First Handle some IDs not in the list of standards!
		case ECDSA.Curve25519:
			ecs = curves.get(new Integer(ID));
			if(ecs == null) {
				ecs = new ECS(
							ECDSA.Curve25519,
							new ECC(
									(ECC.TWO.pow(255)).subtract(new BigInteger("19"))
									,new BigInteger("486662")
									,new BigInteger("1"), ECC.ECC_TYPE_MONTGOMERY
									),
							new EC_Point(new BigInteger("9"), true, null),
							null
							);
				curves.put(new Integer(ID), ecs);
			}
			return ecs;
			default:
				
				/**
				 * In the end, get the curve from the list of standards!s
				 */
				BigInteger p, a, b, n;
				EC_Point x;
				ECC ec;
				ecs = curves.get(new Integer(ID));
				if (ecs == null) {
					ECSDesc ed;
					if (ID >= standards.length)
						break;
					ed = new ECSDesc(standards[ID]);
					ecs = new ECS(
							ID,
							ec = new ECC(
									p=new BigInteger(ed.p)
									,a=new BigInteger(ed.a)
									,b=new BigInteger(ed.b,16), ECC.ECC_TYPE_P
									),
							x=new EC_Point(new BigInteger(ed.Gx,16),
									new BigInteger(ed.Gy,16), null),
							n=new BigInteger(ed.n)
							);
					curves.put(new Integer(ID), ecs);
//					System.out.println("Test P- ="+ecs.curve.b.multiply(ecs.curve.b).multiply(new BigInteger(ed.c, 16)).add(new BigInteger("27")).mod(ecs.curve.p));
//					System.out.println(" y^2=\n"+ec.evaluate_y2(x.getX())+" = \n" +
//							""+x.getY().multiply(x.getY()).mod(p));
//					x.compress(); x.y = null; x.decompress();
//					System.out.println(x.y.multiply(x.y).mod(p));
//					verifySummarilyPrimality(p);
//					verifySummarilyPrimality(n);
				};
				return ecs;
		}
		return null;
	}
	private static boolean verifySummarilyPrimality(BigInteger p) {
		for (int a = 2; a < 4; a ++) {
			if (! new BigInteger(""+a).modPow(p.subtract(BigInteger.ONE), p).equals(BigInteger.ONE)) {
				System.out.println("Primality fails at "+a+" for "+p);
				return false;
			}
		}
		System.out.println("Primality succeeds for "+p);
		return true;
	}
	
}
public class ECDSA extends ciphersuits.Cipher{
	public final static String type=Cipher.ECDSA; //"ECC";
	
	public static final int P_119 = 0;
	public static final int P_224 = 1;
	public static final int P_256 = 2;
	public static final int P_384 = 3;
	public static final int P_521 = 4;
	public static final int Curve25519 = 101;
	ECDSA_SK sk;
	ECDSA_PK pk;
	/*
	EC curve;
	ECP q;
	BigInteger secret, g;
	int curve_ID;
	*/
	
	public ECDSA(ECDSA_SK sk2, ECDSA_PK pk2) {
		sk = sk2;
		pk = pk2;
	}

	public ECDSA() {
	}

	@Override
	public SK genKey(int size) {
		sk = new ECDSA_SK(size);
		sk.hash_alg = hash_alg;
		return sk;
	}

	@Override
	public String getType() {
		return type;
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
		if (sk != null) return sk;
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
		if(sk == null) return null;
		return sk.sign(m);
	}

	@Override
	public boolean verify(byte[] signature, byte[] message) {
		if ((pk == null) && (sk !=null))
			pk = sk.getPK();
		if(pk == null) return false;
		return pk.verify(signature, message);
	}
	public static void ___main(String[] args) {
		Cipher c = Cipher.getCipher(Cipher.ECDSA, Cipher.SHA1, "Test");
		SK sk = c.genKey(P_521);
		byte message[] = new byte[]{1,2,3};
		byte sign[] = sk.sign(message);// sk.sign_pad_hash(message);
		PK pk = sk.getPK();
		boolean v = pk.verify(sign, message);// pk.verify_unpad_hash(sign, message);
		System.out.println("ECDSA: s="+Util.byteToHexDump(sign)+"\nv="+v);
	}
	public static void __main(String[] args) {
		BigInteger p = new BigInteger("11");
		BigInteger a = new BigInteger("1");
		BigInteger b = new BigInteger("6");
		ECC ec = new ECC(p, a, b);
		EC_Point x = new EC_Point(new BigInteger("2"), true, ec);
		BigInteger m = new BigInteger("7");
		BigInteger n = new BigInteger("13");
		ECDSA_SK sk = new ECDSA_SK(ec, x, m, n);
		System.out.println("ECDSA: sk="+sk);
		byte message[] = new byte[]{4};
		byte sign[] = sk.sign(message, new BigInteger("3"));// sk.sign_pad_hash(message);
		ECDSA_Signature sg = null;
		try {
			sg = new ECDSA_Signature(sign);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		PK pk = sk.getPK();
		System.out.println("ECDSA: pk="+pk);
		boolean v = pk.verify(sign, message);// pk.verify_unpad_hash(sign, message);
		System.out.println("ECDSA: s="+sg+" ="+Util.byteToHexDump(sign)+"\nv="+v);
	}
	public static void main(String[] args) {
		int id = Integer.parseInt(args[0]);
		BigInteger d = new BigInteger(args[1],10);
		//BigInteger k = new BigInteger(args[2],16);
		byte[] msg = Util._byteSignatureFromString(args[3]);//new BigInteger(args[3],16);
		//Cipher c = Cipher.getCipher(Cipher.ECDSA, Cipher.SHA1, "Test");
		ECDSA_SK sk = new ECDSA_SK(id, d);
		sk.hash_alg = Cipher.SHA384;
		//EC_Point ecp = new EC_Point(sk.x.getX(),true, sk.curve);
		//ecp.decompress();
		//System.out.println("ECDSA: point: ="+ecp+"\n minus="+ecp.minus());
		byte message[] = msg;//new byte[]{1,2,3};
		//byte sign[] = sk.sign(message, k);
		byte sign[] = sk.sign_pad_hash(message);
		System.out.println("ECDSA: sign="+Util.byteToHex(sign));
		ECDSA_Signature sg = null;
		try {
			sg = new ECDSA_Signature(sign);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		PK pk = sk.getPK();
		//boolean v = pk.verify(sign, message);
		System.out.println("ECDSA: v: sign="+Util.byteToHex(sign)+"\n hash="+Util.byteToHex(Util.simple_hash(sign, Cipher.MD5)));
		System.out.println("ECDSA: v: msg="+Util.byteToHex(msg)+"\n hash="+Util.byteToHex(Util.simple_hash(msg, Cipher.MD5)));
		System.out.println("ECDSA: v: pk="+pk);
		boolean v = pk.verify_unpad_hash(sign, message);
		System.out.println("ECDSA: s="+sg+"\nv="+v);
		System.out.println("ECDSA: sk="+sk);
	}
}

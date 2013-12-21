package ciphersuits;

import java.math.BigInteger;

import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
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
	public static void main(String[] args){
		try{
			BigInteger alpha= new BigInteger(args[0]);
			BigInteger beta= new BigInteger(args[1]);
			BigInteger a[] = new BigInteger[3];
			BigInteger m[] = new BigInteger[3];
			a[0] = new BigInteger("11");
			a[1] = alpha;
			a[2] = beta;
			m[0] = new BigInteger("17");
			m[1] = new BigInteger("13");
			m[2] = new BigInteger("16");
			BigInteger A = CryptoUtils.CRT(a,m);
			System.out.println("1. CRT = "+A);
			BigInteger a_prim = alpha;
			if(alpha.equals(BigInteger.ONE) || alpha.equals(BigInteger.ZERO) )
				a_prim = EC.TWO;
			BigInteger order =
					CryptoUtils.order(a_prim, 
							new BigInteger("53"),
							CryptoUtils.toBigInts(new long[]{2,13}),
							CryptoUtils.toBigInts(new long[]{2,1}));
			System.out.println("2. Order("+a_prim+") = "+order);
			
			BigInteger[] DH_A =
					CryptoUtils.DiffieHellman(
							new BigInteger("11"),
							new BigInteger("2"),
							alpha,
							null,
							null);
			BigInteger[] DH_B =
					CryptoUtils.DiffieHellman(
							new BigInteger("11"),
							new BigInteger("2"),
							beta,
							null,
							DH_A[0]);
			BigInteger[] DH_A2 =
					CryptoUtils.DiffieHellman(
							new BigInteger("11"),
							new BigInteger("2"),
							alpha,
							DH_A[1],
							DH_B[0]);
			System.out.println("4. DH exchange: " +
					" yA="+DH_A[0]+
					" yB="+DH_B[0]+
					" K_A="+DH_A2[1]+
					" K_B="+DH_B[1]
					);
			
			System.out.println("5.");
			BigInteger p = new BigInteger("11");
			BigInteger q = new BigInteger("13");
			BigInteger e = new BigInteger("7");
			BigInteger n = p.multiply(q);
			BigInteger msg = CryptoUtils.BlindPrepRSA(n, e, a_prim, beta);
			BigInteger sgn_blind = CryptoUtils.BlindSignRSA(msg, p, q, n, e);
			BigInteger sgn = CryptoUtils.BlindFinishRSA(n, sgn_blind, a_prim);
			
			System.out.println(" Blinded = "+msg);
			System.out.println(" Signed_blind = "+sgn_blind);
			System.out.println(" Signed = "+sgn+" verif="+sgn.modPow(e, n));

			EC ec = new EC(p, alpha, beta);
			ECP P=null, Q=null;
			System.out.println("7.a Valid = "+ec.valid());
			try{
				P = new ECP(new BigInteger("1"), true, ec);
				System.out.println("7.b P = "+P+" "+P.minus());
			}catch(Exception e1){System.out.println("7.b "+ e1);}
			try{
				Q = new ECP(new BigInteger("2"), true, ec);
				System.out.println("7.c Q = "+Q+" "+Q.minus());
			}catch(Exception e1){System.out.println("7.c "+e1);}

			if((P!=null) && (Q!=null))
				System.out.println("7.d P+Q = "+(ec.add(P,Q)));
			
			if((P!=null)&&(Q==null))
				System.out.println("7.d P+P = "+(ec.add(P,P)));
			
			if((P==null)&&(Q!=null))
				System.out.println("7.d Q+Q = "+(ec.add(Q,Q)));
			if((P==null)&&(Q==null)){
				System.out.println("7.d need to find P ");
				for(int t=0; t<10; t++){
					if((t==1)||(t==2)) continue;
					try{
						P = new ECP(new BigInteger(""+t), true, ec);
					}catch(Exception e1){
						System.out.println("7.d t="+t+" "+ e1);
						continue;
					}
					System.out.println("7.d P = "+P);
					System.out.println("7.d P+P = "+ec.add(P, P));
					break;
				}
			}
			/*
			BigInteger a= new BigInteger(args[0]);
			BigInteger b= new BigInteger(args[1]);
			BigInteger p= new BigInteger(args[2]);
	
			BigInteger Qx= new BigInteger(args[3]);
			BigInteger Qy= new BigInteger(args[4]);
	
			BigInteger Px= new BigInteger(args[5]);
			BigInteger Py= new BigInteger(args[6]);
			
			EC ec = new EC(p,a,b);
			ECP r = ec.add(new ECP(Qx, Qy, ec), new ECP(Px, Py, ec));
			System.out.println("Got: "+r);
			*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
/**
 * Implements a point on an elliptic curve (with compression).
 * A null is considered to be a point at INFINITY
 * @author msilaghi
 *
 */
class ECP extends ASNObj{
	public static final ECP INFINITY = null;
	private static final boolean DEBUG = false;
	EC _curve;
	public static EC default_curve;
	boolean inf = false; // null is infinity
	boolean compressed = false;
	boolean compressed_y;
	BigInteger x;
	BigInteger y;
	public ECP(BigInteger x, BigInteger y, EC curve) {
		this.x = x;
		this.y = y;
		this._curve = curve;
	}
	public ECP minus() {
		EC curve = getCurve();
		return new ECP(x, curve.minus(y), curve);
		//return new ECP(x, curve.p.subtract(y).mod(curve.p), curve);
	}
	public ECP(BigInteger x, boolean b, EC ec) {
		this.x = x;
		compressed=true;
		compressed_y = b;
		_curve = ec;
		decompress();
	}
	EC getCurve(){
		if(_curve==null) return default_curve;
		return _curve;
	}
	public BigInteger getY() {
		if(inf) throw new RuntimeException("Wrong point at inf");
		if(y!=null) return y;
		if(compressed_y){
			decompress();
			if(y!=null) return y;
		}
		throw new RuntimeException("Wrong point");
	}
	public void setEC(EC curve, boolean global){
		if(global){
			default_curve = curve;
		}else
			this._curve = curve;
	}
	public void decompress() {
		if(getCurve()==null) throw new RuntimeException("Set EC!");
		y = getCurve().evaluate_y(x);
		if((compressed_y)&&(!y.testBit(0)))
				y = getCurve().minus(y);
	}
	public void compress() {
		if(inf || (y==null)) return;
		compressed_y = y.testBit(0);
		compressed = true;
	}
	public BigInteger getX() {
		return x;
	}
	public boolean equals(ECP a){
		if(!a.getX().equals(getX())) return false;
		if(!a.getY().equals(getY())) return false;
		return true;
	}
	public String toString(){
		if(inf) return "INFINITY";
		return "("+getX()+","+y+"/"+compressed_y+")";
	}
	@Override
	public Encoder getEncoder() {
		Encoder e = new Encoder().initSequence();
		compress();
		if(inf){
			e.addToSequence(new Encoder(inf));
		}else{
			e.addToSequence(new Encoder(x));
			e.addToSequence(new Encoder(compressed_y));
		}
		return e.setASN1Type(getASN1TAG());
	}
	private byte getASN1TAG() {
		return DD.TAG_AC13;
	}
	@Override
	public ECP decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if(d.getFirstObject(false).getTypeByte()==Encoder.TAG_BOOLEAN){
			inf = true;
			return this;
		}
		x = d.getFirstObject(true).getInteger();
		compressed_y = d.getFirstObject(true).getBoolean();
		compressed = true;
		if(getCurve()!=null) decompress();
		return this;
	}
	public boolean nullEnd() {
		boolean r = BigInteger.ZERO.equals(getY());
		if(DEBUG) System.out.println("nullEnd: "+r+" due to y="+getY());
		return r;
	}
}
/**
 * Class to implement an Elliptic Curve
 * @author msilaghi
 *
 */
class EC{
	BigInteger p;
	BigInteger a;
	BigInteger b;
	static final BigInteger TWO = new BigInteger("2");
	static final BigInteger THREE = new BigInteger("3");
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	EC(BigInteger p, BigInteger a, BigInteger b) {
		init(p,a,b);
	}
	/**
	 * (p-y) mod p
	 * @param y
	 * @return
	 */
	public BigInteger minus(BigInteger y) {
		return (p.subtract(y)).mod(p);
	}
	public BigInteger evaluate_y2(BigInteger x) {
		return ((x.modPow(THREE, p))
				.add(a.multiply(x))
				.add(b)).mod(p);
	}
	public BigInteger evaluate_y(BigInteger x) {
		BigInteger y2 = evaluate_y2(x);
		BigInteger exponent = p.subtract(BigInteger.ONE)
				.shiftRight(1);
		BigInteger test = y2.modPow(exponent, p);
		if(!hasSquare(test))
				throw new RuntimeException("Not square: "+y2+" test="+test+" exp="+exponent+" p="+p);
		return modSquareRoot(y2);
	}
	boolean hasSquare(BigInteger test){
		return ((BigInteger.ZERO.equals(test)||BigInteger.ONE.equals(test)));
	}
	private BigInteger modSquareRoot(BigInteger y2) {
		BigInteger m = p.shiftRight(2);
		if(p.testBit(1)){ //3
			BigInteger r = y2.modPow(m.add(BigInteger.ONE), p);
			if(DEBUG) System.out.println("sqrt: "+y2+"^("+m+"+1) mod "+p+"="+r);
			return r;
		}
		throw new RuntimeException("p=1 mod 3 not implemented");
		//return null;
	}
	boolean valid(){
		return !(a.pow(3)).shiftLeft(2). //4a^3
		add(modSquare(b).multiply(new BigInteger("27")))//27b^2
		.mod(p).equals(BigInteger.ZERO);
	}
	void init(BigInteger p, BigInteger a, BigInteger b){
		this.p=p;
		this.a=a;
		this.b=b;
		if(!valid())
			throw new RuntimeException("Wrong Curve");
	}
	/**
	 * P+Q
	 * @param a
	 * @param b
	 * @return
	 */
	ECP add(ECP a, ECP b){
		if(DEBUG) System.out.println("add "+a+" + "+b);
		if((a==ECP.INFINITY)||a.inf){
			if(DEBUG) System.out.println("a inf");
			return b;
		}
		if((b==ECP.INFINITY)||b.inf){
			if(DEBUG) System.out.println("b inf");
			return a;
		}
		if((a.equals(b))&&(!a.nullEnd())){
			if(DEBUG) System.out.println("add equals");
			ECP r = times_2(a);
			if(DEBUG) System.out.println("add equals -> "+r);
			return r;
		}
		if(a.getX().equals(b.getX())){
			if(DEBUG) System.out.println("add inverses");
			return ECP.INFINITY; //infinity
		}
		if(DEBUG) System.out.println("add differents");
		return add2(a,b);
	}
	/**
	 * P+Q where P!=Q and P+Q != O
	 * @param P
	 * @param Q
	 * @return
	 */
	private ECP add2(ECP P, ECP Q) {
		BigInteger lambda =
				modDivision(
						Q.getY().subtract(P.getY()),
						Q.getX().subtract(P.getX())
				);
		if(_DEBUG) System.out.println("add2: l ="+lambda);
			
		BigInteger xR = 
				(modSquare(lambda)
						.subtract(P.getX())
						.subtract(Q.getX())
				).mod(p);
		if(DEBUG) System.out.println("add2: xR="+xR);
		BigInteger yR = (lambda
						.multiply(
								(P.getX().subtract(xR))
								)
						.subtract(P.getY())
				).mod(p);
		BigInteger yxR = (
				P.getY().add(lambda.multiply(
						(xR.subtract(P.getX()))))
		).mod(p);
		return new ECP(
				xR,
				yxR,
				this
				);
	}
	/**
	 * P+P
	 * @param P
	 * @return
	 */
	private ECP times_2(ECP P) {
		BigInteger lambda =
				modDivision(
						(modSquare(P.getX()).multiply(THREE)).add(a),
						P.getY().shiftLeft(1)
				);
		if(_DEBUG) System.out.println("x2: l ="+lambda);
		BigInteger xR = 
				(modSquare(lambda)
						.subtract(P.getX().shiftLeft(1))
				).mod(p);
		if(DEBUG) System.out.println("x2: xR="+xR);
		// yR is the minus of the answer
		//BigInteger yR=((lambda.multiply((P.getX().subtract(xR)))).subtract(P.getY())).mod(p);
		BigInteger yxR = (
						P.getY().add(lambda.multiply(
								(xR.subtract(P.getX()))))
				).mod(p);
		return new ECP(
				xR,
				yxR,
				this
				);
	}
	// double is return y.shiftLeft(1).mod(p);
	/**
	 * square of y mod p
	 * @param y
	 * @return
	 */
	private BigInteger modSquare(BigInteger y) {
		return y.modPow(TWO, p);
	}
	/**
	 * x/y mod p
	 * @param x
	 * @param y
	 * @return
	 */
	BigInteger modDivision(BigInteger x, BigInteger y){
		if (_DEBUG) System.out.println("modDiv: x="+x+"/"+y+" ("+y.modInverse(p)+") mod "+p);
		return x.multiply(y.modInverse(p)).mod(p);
	}
}
class CryptoUtils{
	private static final boolean _DEBUG = true;
	/**
	 * Chinese Remainder Theorem with remainders a[] modulo moduli m[]
	 * @param a
	 * @param m
	 * @return
	 */
	public	static BigInteger CRT(BigInteger[]a, BigInteger[]m){
		if((m.length<1)||(a.length!=m.length)) return null;
		BigInteger M = m[0];
		for(int k = 1; k<m.length; k++) M = M.multiply(m[k]);
		BigInteger _M[] = new BigInteger[m.length];
		for(int k = 0; k<m.length; k++) _M[k] = M.divide(m[k]);
		BigInteger A = BigInteger.ZERO;
		for(int k = 0; k<m.length; k++){
			BigInteger val = (a[k].multiply(_M[k])).
					multiply(_M[k].modInverse(m[k]));
			A = A.add(val);
		}
		return A.mod(M);
	}
	/**
	 * Remove Blinding factor from a blinded signature mod n
	 * @param n
	 * @param sgn_blind
	 * @param k
	 * @return
	 */
	public static BigInteger BlindFinishRSA(BigInteger n, BigInteger sgn_blind,
			BigInteger k) {
		BigInteger r = sgn_blind.multiply(k.modInverse(n)).mod(n);
		if(_DEBUG) System.out.println("blind finish: "+ sgn_blind+"*"+k+"^(-1) mod "+n+"="+r);
		return r;
	}
	/**
	 * Sign msg (blindly)
	 * @param msg
	 * @param p
	 * @param q
	 * @param e
	 * @return
	 */
	public static BigInteger BlindSignRSA(BigInteger msg, BigInteger p,
			BigInteger q, BigInteger n, BigInteger e) {
		BigInteger totient =
				p.subtract(BigInteger.ONE)
				.multiply(q.subtract(BigInteger.ONE));
		BigInteger d = e.modInverse(totient);
		//if(_DEBUG) System.out.println(" d="+d);
		BigInteger r = msg.modPow(d, n);
		if(_DEBUG) System.out.println("blind sign: "+msg+"^"+d+" mod "+n+"="+r);
		return r;
	}
	/**
	 * Prepare a message by blinding m with a factor k
	 * @param n
	 * @param e
	 * @param k
	 * @param m
	 * @return
	 */
	public static BigInteger BlindPrepRSA(BigInteger n, BigInteger e,
			BigInteger k, BigInteger m) {
		BigInteger r = (m.multiply(k.modPow(e, n))).mod(n);
		if(_DEBUG) System.out.println("blindprep: "+m+"*"+k+"^"+e+" mod "+n+"="+r);
		return r;
	}
	/**
	 * Find the order of a in Z_p where p-1 has the factors in array f
	 * each with the corresponding exponent in array e
	 * @param a
	 * @param p
	 * @param f
	 * @param e
	 * @return
	 */
	public static BigInteger order(BigInteger a, BigInteger p, 
			BigInteger f[], BigInteger e[]){
		BigInteger order = p.subtract(BigInteger.ONE);
		for(int k=0; k<f.length; k++) {
			int _e = e[k].intValue();
			// could do binary search on e
			for(int i=0; i<_e; i++) {
				BigInteger exponent = order.divide(f[k]);
				BigInteger test = a.modPow(exponent, p);
				if(BigInteger.ONE.equals(test)){
					order = exponent;
				}else break;
			}
		}
		return order;
	}
	/**
	 * Create an array of BigIntegers from longs
	 * @param a
	 * @return
	 */
	public static BigInteger[] toBigInts(long[] a){
		BigInteger[] r = new BigInteger[a.length];
		for (int k=0; k<a.length; k++) {
			r[k] = new BigInteger(""+a[k]);
		}
		return r;
	}
	/**
	 * 
	 * @param p
	 * @param g
	 * @param x
	 * @param y (can be null if unknown)
	 * @param y_B (can be null if unknown)
	 * @return  ([y,k] where k is non-null iff y_B is known)
	 */
	public static BigInteger[] DiffieHellman(BigInteger p, BigInteger g,
			BigInteger x, BigInteger y, BigInteger y_B){
		if(y==null) y = g.modPow(x, p);
		BigInteger k = null;
		if(y_B!=null) k = y_B.modPow(x, p);
		return new BigInteger[]{y,k};
	}
}
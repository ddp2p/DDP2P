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

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 * Class to implement an Elliptic Curve
 * y^2 = x^3 + ax +b
 * @author msilaghi
 *
 */
public
class ECC extends ASNObj {
	public static final int ECC_TYPE_P = 0;
	public static final int ECC_TYPE_WEIERSTRASS = 0;
	public static final int ECC_TYPE_GF = 1;
	public static final int ECC_TYPE_MONTGOMERY = 3;
	BigInteger p;
	BigInteger a;
	BigInteger b;
	int type = ECC_TYPE_WEIERSTRASS; // e.g. P (WEIERSTRASS), GF, MONTGOMERY
	static final BigInteger TWO = new BigInteger("2");
	static final BigInteger THREE = new BigInteger("3");
	static final BigInteger MINUS_THREE = new BigInteger("-3");
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String toString() {
		return "[p="+p+" a="+a+" b="+b+"]";
	}
	/**
	 * 
	 * @param p
	 * @param a
	 * @param b
	 */
	ECC(BigInteger p, BigInteger a, BigInteger b) {
		init(p,a,b);
	}
	/**
	 * 
	 * @param p
	 * @param a
	 * @param b
	 * @param montgomery
	 */
	public ECC(BigInteger p, BigInteger a,
			BigInteger b, int type) {
		this.type = type;
		init(p,a,b);
	}
	public ECC(Decoder d) throws ASN1DecoderFail {
		decode(d);
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
	/**
	 * 
	 * @param a
	 * @return
	 */
	private BigInteger modSquareRoot(BigInteger a) {
		return CryptoUtils.modSquareRoot(a, p, b_order_evens_4m);
	}
	/**
	 * To save the value b for square roots
	 */
	public BigInteger b_order_evens_4m[] = new BigInteger[1];
	void setP(BigInteger p) {
		this.p = p;
		b_order_evens_4m = new BigInteger[1];
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
	EC_Point add(EC_Point a, EC_Point b){
		if(DEBUG) System.out.println("add "+a+" + "+b);
		if((a==EC_Point.INFINITY)||a.inf){
			if(DEBUG) System.out.println("a inf");
			return b;
		}
		if((b==EC_Point.INFINITY)||b.inf){
			if(DEBUG) System.out.println("b inf");
			return a;
		}
		if((a.equals(b))&&(!a.nullEnd())){
			if(DEBUG) System.out.println("add equals");
			EC_Point r = times_2(a);
			if(DEBUG) System.out.println("add equals -> "+r);
			return r;
		}
		if(a.getX().equals(b.getX())){
			if(DEBUG) System.out.println("add inverses");
			return EC_Point.INFINITY; //infinity
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
	private EC_Point add2(EC_Point P, EC_Point Q) {
		BigInteger lambda =
				modDivision(
						Q.getY().subtract(P.getY()),
						Q.getX().subtract(P.getX())
				);
		if(DEBUG) System.out.println("add2: l ="+lambda);
			
		BigInteger xR = 
				(modSquare(lambda)
						.subtract(P.getX())
						.subtract(Q.getX())
				).mod(p);
		if(DEBUG) System.out.println("add2: xR="+xR);
		BigInteger yxR = (lambda
						.multiply(
								(P.getX().subtract(xR))
								)
						.subtract(P.getY())
				).mod(p);
		/* minus
		BigInteger yR = (
				P.getY().add(lambda.multiply(
						(xR.subtract(P.getX()))))
		).mod(p);
		*/
		return new EC_Point(
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
	private EC_Point times_2(EC_Point P) {
		if((P==null)||P.inf) return P;
		BigInteger lambda =
				modDivision(
						(modSquare(P.getX()).multiply(THREE)).add(a),
						P.getY().shiftLeft(1)
				);
		if(DEBUG) System.out.println("x2: l ="+lambda);
		BigInteger xR = 
				(modSquare(lambda)
						.subtract(P.getX().shiftLeft(1))
				).mod(p);
		if(DEBUG) System.out.println("x2: xR="+xR);
		// yR is the minus of the answer
		BigInteger yxR=((lambda.multiply((P.getX().subtract(xR)))).subtract(P.getY())).mod(p);
		//BigInteger yxR = (P.getY().add(lambda.multiply((xR.subtract(P.getX()))))).mod(p);
		return new EC_Point(
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
		if (DEBUG) System.out.println("modDiv: x="+x+"/"+y+" ("+y.modInverse(p)+") mod "+p);
		return x.multiply(y.modInverse(p)).mod(p);
	}
	public static EC_Point mul(EC_Point a, BigInteger k) {
		return ECC.double_add(a, k);
		// return ECC.double_add_subtract(a, k);
	}
	public static EC_Point static_add(EC_Point a1, EC_Point a2) {
		if ((a1==null) && (a2==null)) return EC_Point.INFINITY;
		if (a1==null) return a2;
		if (a2==null) return a1;
		if (a1._curve != null)
			return a1._curve.add(a1, a2);
		return a2._curve.add(a1, a2);
	}
	/**
	 * Optimized;
	 * @param elem
	 * @param k
	 * @return
	 */
	public static EC_Point double_add_subtract (EC_Point elem, BigInteger k) {
		if ((elem == null) || elem.inf) return EC_Point.INFINITY;
		byte[] exp = to_one_minone(k);
		int i = exp.length - 1;
		for(; i >= 0; i --) {
			if (exp[i] != 0) break;
		}
		EC_Point result = elem;
		for(i --; i >= 0; i --) {
			result = elem._curve.add(result,result);
			switch(exp[i]) {
			case 0:
				break;
			case 1:
				result = elem._curve.add(result, elem);
				break;
			case -1:
				result = elem._curve.add(result, elem.minus());
				break;
			default:
				throw new RuntimeException("Unknown bit: "+exp[i]);
			}
		}
		return result;
	}
	/**
	 * Optimized;
	 * @param elem
	 * @param k
	 * @return
	 */
	public static EC_Point double_add (EC_Point elem, BigInteger k) {
		if ((elem == null) || elem.inf) return EC_Point.INFINITY;
		ECC thiscurve = elem.getCurve();
		if (thiscurve == null) System.out.println("ECC: missing curve for: "+elem);
		byte[] exp = to_byte_bits(k, 0);
		int i = exp.length - 1;
		for(; i >= 0; i --) {
			if (exp[i] != 0) break;
		}
		EC_Point result = elem;
		for(i --; i >= 0; i --) {
			if(result != null)
				result = thiscurve.times_2(result);
			switch(exp[i]) {
			case 0:
				break;
			case 1:
				result = thiscurve.add(result, elem);
				break;
			default:
				throw new RuntimeException("Unknown bit: "+exp[i]);
			}
		}
		return result;
	}
	/**
	 * General structure.
	 * @param elem
	 * @param k
	 * @return
	 */
	public static EC_Point _double_add_subtract (EC_Point elem, BigInteger k) {
		if ((elem == null) || elem.inf) return EC_Point.INFINITY;
		byte[] exp = to_one_minone(k);
		EC_Point result = EC_Point.INFINITY;
		
		for(int i = exp.length - 1; i >= 0; i --) {
			result = static_add(result,result);
			switch(exp[i]) {
			case 0:
				break;
			case 1:
				result = static_add(result, elem);
				break;
			case -1:
				result = static_add(result, elem.minus());
				break;
			default:
				throw new RuntimeException("Unknown bit: "+exp[i]);
			}
		}
		return result;	
	}
	/**
	 * Translates k into an array of bits (one per byte)
	 * @param k : a non-negative integer
	 * @param extra : number of 0 bits in from of the first "1"
	 * @return
	 */
	private static byte[] to_byte_bits(BigInteger k, int extra) {
		if((k==null)||(k.compareTo(BigInteger.ZERO)<0)) return new byte[]{0};
		int bitcount = k.bitLength();
		byte[] bits = new byte[bitcount+extra];
		for(int i = 0; i < bitcount; i ++) {
			bits[i] = (byte) (k.testBit(i) ? 1 : 0);
		}
		return bits;
	}
	private static byte[] to_one_minone(BigInteger k) {
		byte[] bits = to_byte_bits(k, 1);
		if(DEBUG) System.out.println("to_one_minone: "+k+" -> "+Util.byteToHex(bits, " "));
		while(to_one_minone(bits));
		return bits;
	}
	/**
	 * returns true on changes (to repeat until it returns false)
	 * @param bits
	 * @return
	 */
	private static boolean to_one_minone(byte[] bits) {
		boolean changes = false;
		if(DEBUG) System.out.println("to_one_minone: "+Util.byteToHex(bits, " "));
		int len = bits.length;
		boolean in_seq = false;
		int start = -1;
		for (int k = 0; k < len; k ++) {
			if (in_seq) {
				if (bits[k] != 1) {
					if (k == start + 1) {
						in_seq = false;
						continue;
					}

					changes = true;
					
					bits[k] ++;
					bits[start] = -1;
					for(int i = start + 1; i < k; i ++) bits[i] = 0;
					
					if(bits[k] != 1) in_seq = false;
					else start = k;
				}
			} else {
				if(bits[k] == 1) {
					in_seq = true;
					start = k;
				}
			}
		}
		if(DEBUG) System.out.println("to_one_minone: got "+Util.byteToHex(bits, " "));
		return changes;
	}	
	public static AdditiveGroup double_add_subtract (AdditiveGroup elem, BigInteger k) {
		return double_add_subtract((EC_Point)elem, k);
	}
	public static AdditiveGroup mul(AdditiveGroup a, BigInteger k) {
		System.out.println("ECC MUL AdditiveGroup");
		return ECC.double_add_subtract(a, k);
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(type));
		enc.addToSequence(new Encoder(p));
		enc.addToSequence(new Encoder(b));
		if(!a.equals(MINUS_THREE))
			enc.addToSequence(new Encoder(a));
		return enc;
	}
	@Override
	public ECC decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		type = d.getFirstObject(true).getInteger().intValue();
		p = d.getFirstObject(true).getInteger();
		b = d.getFirstObject(true).getInteger();
		if(d.getTypeByte() == Encoder.TAG_INTEGER)
			a = d.getFirstObject(true).getInteger();
		else
			a = MINUS_THREE;
		return this;
	}
	public static void main(String[] args) {
		BigInteger alpha= new BigInteger(args[0]);
		BigInteger beta= new BigInteger(args[1]);
		BigInteger p = new BigInteger(args[2]);
		BigInteger k = new BigInteger(args[3]);
		BigInteger P_x_start = new BigInteger(args[4]);
		BigInteger P_y = null;
		BigInteger Q_x = null;
		BigInteger Q_y = null;
		if(args.length>5) P_y = new BigInteger(args[5]);
		if(args.length>6) Q_x = new BigInteger(args[6]);
		if(args.length>7) Q_y = new BigInteger(args[7]);
		System.out.println("In: Py="+P_y+" Qx="+Q_x+" Qy="+Q_y);
		ECC ec = new ECC(p, alpha, beta);
		EC_Point P=null, Q=null;
		if(!ec.valid()) System.out.println("Valid EC = "+ec.valid());
		BigInteger x = new BigInteger(""+P_x_start); //BigInteger.ZERO;
		do {
			try{
				P = new EC_Point(x, true, ec);
				System.out.println("P = "+P+" "+P.minus());
				if(P_y!=null)
					P =  new EC_Point(x,P_y,ec);
				if(Q_y!=null)
					Q =  new EC_Point(Q_x,Q_y,ec);
			}catch(Exception e1){
				System.out.println("Base selection "+ e1);
				x = x.add(BigInteger.ONE);
				if(x.compareTo(p) >= 0) return;
			}
		} while (P == null);
		
		System.out.println("P*k = "+P+" * "+k+" = "+P.mul(k));
		if (Q != null) 
			System.out.println("P+Q = "+P+" + "+Q+" = "+P.add(P,Q));
	}
	public static int getCurveID(int curve_ID) {
		switch(curve_ID) {
		case 119: return ECDSA.P_119;
		//case 224: return ECDSA.P_224;
		case 256: return ECDSA.P_256;
		case 384: return ECDSA.P_384;
		case 521: return ECDSA.P_521;
		}
		return curve_ID;
	}
}

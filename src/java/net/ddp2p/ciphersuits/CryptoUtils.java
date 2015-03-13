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
package net.ddp2p.ciphersuits;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.ddp2p.common.util.Util;

class CryptoUtils{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
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
	/**
	 * Assignment cryptology 2013
	 * @param args
	 */
	public static void _main(String[] args) {
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
			BigInteger A = CRT(a,m);
			System.out.println("1. CRT = "+A);
			BigInteger a_prim = alpha;
			if(alpha.equals(BigInteger.ONE) || alpha.equals(BigInteger.ZERO) )
				a_prim = ECC.TWO;
			BigInteger order =
					order(a_prim, 
							new BigInteger("53"),
							toBigInts(new long[]{2,13}),
							toBigInts(new long[]{2,1}));
			System.out.println("2. Order("+a_prim+") = "+order);
			
			BigInteger[] DH_A =
					DiffieHellman(
							new BigInteger("11"),
							new BigInteger("2"),
							alpha,
							null,
							null);
			BigInteger[] DH_B =
					DiffieHellman(
							new BigInteger("11"),
							new BigInteger("2"),
							beta,
							null,
							DH_A[0]);
			BigInteger[] DH_A2 =
					DiffieHellman(
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
			BigInteger msg = BlindPrepRSA(n, e, a_prim, beta);
			BigInteger sgn_blind = BlindSignRSA(msg, p, q, n, e);
			BigInteger sgn = BlindFinishRSA(n, sgn_blind, a_prim);
			
			System.out.println(" Blinded = "+msg);
			System.out.println(" Signed_blind = "+sgn_blind);
			System.out.println(" Signed = "+sgn+" verif="+sgn.modPow(e, n));
	
			ECC ec = new ECC(p, alpha, beta);
			EC_Point P=null, Q=null;
			System.out.println("7.a Valid = "+ec.valid());
			try{
				P = new EC_Point(new BigInteger("1"), true, ec);
				System.out.println("7.b P = "+P+" "+P.minus());
			}catch(Exception e1){System.out.println("7.b "+ e1);}
			try{
				Q = new EC_Point(new BigInteger("2"), true, ec);
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
						P = new EC_Point(new BigInteger(""+t), true, ec);
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
	/**
	 * Compute digest
	 * @param message
	 * @param hash_alg
	 * @return
	 */
	public static byte[] digest(byte[] message, String hash_alg) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(hash_alg); //Cipher.SHA256
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return null;}
		if(DEBUG) System.out.println("ECC:digest");
		digest.update(message);
		byte result[] = digest.digest();
		return result;
	}
	public static byte[] digest(byte[][] messages, String hash_alg) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(hash_alg); //Cipher.SHA256
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return null;}
		if(DEBUG) System.out.println("ECC:digest");
		for(int k=0; k<messages.length; k++)
			digest.update(messages[k]);
		byte result[] = digest.digest();
		return result;
	}
	/**
	 * Computes digest and then transform to int
	 * @param c
	 * @param hash_alg
	 * @return
	 */
	public static BigInteger bigIntegerDigestFromBytes(byte[] c, String hash_alg) {
		byte[] val = digest(c, hash_alg);
		return Util.bigIntegerFromUnsignedBytes(val);
	}
	/**
	 * Efficient version to same m and b
	 * @param m
	 * @param a
	 * @param _b_order_evens_4m : must be an array with at least one element available
	 * 			set the element to ZERO or null if not precomputed
	 * @return
	 */
	public static BigInteger modSquareRoot1Mod4(BigInteger m, BigInteger a, BigInteger p, BigInteger _b_order_evens_4m[]) {
		BigInteger b_order_evens_4m = _b_order_evens_4m[0];
		BigInteger p_1 = p.subtract(BigInteger.ONE);
		BigInteger m2 = p.shiftRight(1);
		if(b_order_evens_4m == null) {
			b_order_evens_4m = _b_order_evens_4m[0] = BigInteger.ONE;
		}
		if(b_order_evens_4m.equals(BigInteger.ONE)) {
			boolean bp = false;
			do {
				b_order_evens_4m = b_order_evens_4m.add(BigInteger.ONE);
				BigInteger b2m = b_order_evens_4m.modPow(m2, p);
				bp = b2m.equals(p_1);
				if(ECC.DEBUG) System.out.println("b="+b_order_evens_4m+" b2m="+b2m);
			} while(!bp);
			_b_order_evens_4m[0] = b_order_evens_4m;
		}
		
		BigInteger i = m2, j = BigInteger.ZERO;
		
		do {
			i = i.shiftRight(1);
			j = j.shiftRight(1);
			BigInteger aibj = a.modPow(i, p) .multiply(b_order_evens_4m.modPow(j, p)) .mod(p);
			if (aibj.equals(p_1))
				j = j.add(m2);
		} while (!i.testBit(0));
		
		i.add(BigInteger.ONE);
		i = i.shiftRight(1);
		j = j.shiftRight(1);
		return a.modPow(i, p) .multiply(b_order_evens_4m.modPow(j, p)) .mod(p);
	}
	public static BigInteger modSquareRoot(BigInteger a, BigInteger p) {
		return modSquareRoot(a, p, new BigInteger[]{BigInteger.ONE});
	}
	public static BigInteger modSquareRoot(BigInteger a, BigInteger p, BigInteger[] b_order_evens_4m) {
		BigInteger m = p.shiftRight(2);
		if(p.testBit(1)){ //3
			BigInteger r = a.modPow(m.add(BigInteger.ONE), p);
			if(ECC.DEBUG) System.out.println("sqrt: "+a+"^("+m+"+1) mod "+p+"="+r);
			return r;
		}
		return modSquareRoot1Mod4(m, a, p, b_order_evens_4m);
	}
	public static BigInteger modSquareRoot1Mod4(BigInteger a, BigInteger p) {
		return modSquareRoot1Mod4(p.shiftLeft(2),a,p,new BigInteger[]{BigInteger.ONE});
	}
}
package tools;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ElGamal {
	BigInteger p;
	BigInteger g;
	public static boolean testGenerator(BigInteger n, BigInteger g){
		return false;
	}
	public static boolean testPrime(BigInteger n){
		boolean result;
		int len = n.bitLength();
		System.out.println("Size of "+n+" is " + len);
		SecureRandom random = new SecureRandom();

		BigInteger n1 = n.subtract(BigInteger.ONE);

		for(int k=0; k<0; k++){
			BigInteger a = new BigInteger(len - 1, random);
			BigInteger r = a.modPow(n1, n);
			if(!r.equals(BigInteger.ONE)) {
				System.out.println("Not prime at: " + a);
				return false;
			}
		}
		result = n.isProbablePrime(10);
		System.out.println("Primality = "+result);
		return result;
	}
	public static void main(String[] arg) {
		BigInteger x=null, g=null, n = new BigInteger(arg[0]);
		testPrime(n);
		if(arg.length>1) g = new BigInteger(arg[1]);
		else return;
		testGenerator(n, g);
		if(arg.length>2) x = new BigInteger(arg[2]);
		else return;
		System.out.println("Pow = "+g.modPow(x, n));
	}
}

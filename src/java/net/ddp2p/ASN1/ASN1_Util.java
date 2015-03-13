package net.ddp2p.ASN1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class ASN1_Util {
	private static final boolean _DEBUG = false;
	public static final int MAX_ASN1_DUMP = 20;
	public final static BigInteger BN128 = new BigInteger("128");
	public final static BigInteger BN127 = new BigInteger("127");
    static String HEX[]={"0","1","2","3","4","5","6","7","8","9",
		"A","B","C","D","E","F"};
    public static String byteToHex(byte[] b, int off, int len, String separator){
    	if(b==null) return "NULL";
    	String result="";
    	if (off<0) return result;
    	for(int i=off; i<off+len; i++){
    		if(i>=b.length) break;
    		result = result+separator+HEX[(b[i]>>4)&0x0f]+HEX[b[i]&0x0f];
    	}
    	return result;
    }
    public static String byteToHex(byte[] b, String sep){
    	if(b==null) return "NULL";
    	String result="";
    	for(int i=0; i<b.length; i++)
    		result = result+sep+HEX[(b[i]>>4)&0x0f]+HEX[b[i]&0x0f];
    	return result;
    }
    public static String byteToHex(byte[] b){
    	return ASN1_Util.byteToHex(b,"");
    }
	/**
	 * Return now at UTC
	 * @return
	 */
	public static Calendar CalendargetInstance(){
		return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	}
	/**
	 * Get a Calendar for this gdate, or null in case of failure
	 * @param gdate
	 * @return
	 */
	public static Calendar getCalendar(String gdate) {
		return getCalendar(gdate, null);
	}
	/**
	 * Get a Calendar for this gdate, or ndef in case of failure
	 * @param gdate
	 * @param def
	 * @return
	 */
	public static Calendar getCalendar(String gdate, Calendar def) {
		if((gdate!=null)&&(gdate.length()<14)) gdate = gdate+"00000000000000";
		if((gdate==null) || (gdate.length()<14)) {
			return def;
		}
		Calendar date = CalendargetInstance();
		try{
		date.set(Integer.parseInt(gdate.substring(0, 4)),
				Integer.parseInt(gdate.substring(4, 6))-1, 
				Integer.parseInt(gdate.substring(6, 8)),
				Integer.parseInt(gdate.substring(8, 10)),
				Integer.parseInt(gdate.substring(10, 12)),
				Integer.parseInt(gdate.substring(12, 14)));
		date.set(Calendar.MILLISECOND, Integer.parseInt(gdate.substring(15, 18)));
		}catch(Exception e){return def;}
		//System.out.println("getCalendar "+gdate+" into "+date);
		return date;
	}
	/**
	 * 
	 * @param val
	 * @return
	 */
	public static short getUnsignedShort(byte val) {
		if (val >= 0) return val;
		return (short)(val + 256);
	}
	/**
	 * Convert to base 128 (bigendian), using shifts.
	 * @param val
	 * @return
	 */
	public static ArrayList<Integer> base128(BigInteger val) {
		ArrayList<Integer> result = new ArrayList<Integer> ();
		int part = val.and(BN127).intValue();
		val = val.shiftRight(7);
		result.add(0, new Integer(part));
		while (! val.equals(BigInteger.ZERO)) {
			part=val.and(BN127).intValue();
			val = val.shiftRight(7);
			part += 128;
			result.add(0, new Integer(part));
		};
		return result;
	}
	/**
	 * This is based on bit shifting (using base128()).
	 * Returns array of bytes.
	 * @param val
	 * @return
	 */
	public static byte[] toBase_128(BigInteger val) {
		ArrayList<Integer> al = base128(val);
		byte[] result = new byte[al.size()];
		for(int k = 0; k < result.length; k++) result[k] = al.get(k).byteValue();;
		return result;
	}
	/**
	 * Decodes from base 128 under the assumption that it is bigendian, terminated by a byte smaller than 128,
	 * all other bytes being OR-ed with 128.
	 * @param b128
	 * @param offset
	 * @param limit
	 * @return
	 */
	public static BigInteger fromBase128(byte[] b128, int offset, int limit) {
		BigInteger result = BigInteger.ZERO;
		int k = offset;
		while ((k < limit) && ((b128[k] & 0x80) != 0)) {
			// result = result.multiply(Util.BN128).add(new BigInteger("" + (Util.getUnsignedShort(b128[k]) - 128)));
			result = result.shiftLeft(7).or(new BigInteger(""+(b128[k] & 0x7f)));
			//len++;
			k++;
		}
		if (k < limit) {
			// result = result.multiply(Util.BN128).add(new BigInteger("" + Util.getUnsignedShort(b128[k])));
			if ((b128[k] & 0x80) != 0) {if (_DEBUG) System.out.println("Util: fromBase_128: last byte > 127"); ASN1_Util.printCallPath("");}
			result = result.shiftLeft(7).or(new BigInteger(""+(b128[k]))); // here  & 0x7f would be redundant
		}
		return result;
		
	}
	/**
	 * Print the call stack in the current point
	 * @param text
	 */
	public static void printCallPath(String text) {
		try{throw new Exception(text);}catch(Exception e){e.printStackTrace();}
	}
	public static void printCallPathTop(String text) {
		try{throw new Exception(text);}catch(Exception e){
			StackTraceElement[] l = e.getStackTrace();
			System.out.println("DDTrace: ["+e.getLocalizedMessage()+"] "+l[1]+"\n"+l[2]+"\n"+l[l.length-1]);}
	}
}
/*   Copyright (C) 2011 Marius C. Silaghi
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
 package net.ddp2p.common.util;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.ECDSA_Signature;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerOrgs;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.handling_wb.BroadcastQueueRequested;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryServerCache.D_DirectoryEntry;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.P2PDDSQLException;
public class Util {
    private static ResourceBundle myResources = Util.getResourceBundle();
    private static final byte[] HEX_R = init_HEX_R();
	private static final int DD_PRIME_SIZE = 2048;
	private static final int DD_PRIME_CERTAINTY = 200;
	public static final boolean DEBUG = false; 
	public static final boolean _DEBUG = true;
	public static final int MAX_DUMP = 20;
	public static final int MAX_UPDATE_DUMP = 400;
	private static final int MAX_CONTAINER_SIZE = 1000000;
	static Random rnd = new Random(); 
    public static String usedCipherGenkey = Cipher.RSA;
    public static String usedMDGenkey = Cipher.SHA256;
    /**
     * 
     * @param site
     * @return
     */
    public static ArrayList<InetAddress> getLocalIPs(boolean site) {
    	ArrayList<InetAddress> result = new ArrayList<InetAddress>();
    	Enumeration<NetworkInterface> e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
	    	while(e.hasMoreElements())
	    	{
	    	    NetworkInterface n = (NetworkInterface) e.nextElement();
    	        if (DEBUG) System.out.println("Util: "+n);
	    	    Enumeration<InetAddress> ee = n.getInetAddresses();
	    	    while (ee.hasMoreElements())
	    	    {
	    	        InetAddress i = (InetAddress) ee.nextElement();
	    	        if (DEBUG) System.out.println("Util: getLocalIP: "+i+" is site="+i.isSiteLocalAddress()+" link="+i.isLinkLocalAddress()+" local="+i.isAnyLocalAddress()+" loop="+i.isLoopbackAddress());
	    	        if (i.isLoopbackAddress()) continue;
	    	        if (i.isLinkLocalAddress() || i.isSiteLocalAddress()) {
	    	        	if (site) result.add(i);
	    	        } else {
	    	        	if (!site) result.add(i);
	    	        }
	    	    }
	    	}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
    	return result;
    }
    /**
     * Trims input and returns result. If result is empty, returns null;
     * @param toTrim
     * @return
     */
    public static String trimToNull(String toTrim) {
    	if (toTrim == null) return null;
    	toTrim = toTrim.trim();
    	if (toTrim.length() == 0) return null;
    	return toTrim;
    }
    public static String concat(String[] array, String sep) {
		return concat(array, sep, "null");
	}
    /**
     * For null or empty array return null
     * replace null with ""
     * @param array
     * @param sep
     * @param def
     * @return
     */
	public static String concat(String[] array, String sep, String def) {
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=((array[0]==null)?"":array[0].toString());
		for(int k=1; k<array.length; k++) result = result + sep + ((array[k]==null)?"":array[k]);
		return result;
	}
	public static String concat_pairs(String[] array, String[] array2, String sep, String eq, String def) {
		if ((array == null) ) return def;
		if ((array2 == null) ) return def;
		if ((array.length == 0)) return def;
		if ((array.length != array2.length)) return def;
		String result=((array[0]==null)?"":array[0].toString()) +eq+ ((array2[0]==null)?"":array2[0]);
		for(int k=1; k<array.length; k++) result = result + sep + ((array[k]==null)?"":array[k]) +eq+ ((array2[k]==null)?"":array2[k]);
		return result;
	}
	public static String concat(Collection<D_DirectoryEntry> array,
			String sep, String def) {
		String result = null;
		if ((array == null) ) return def;
		if ((array.size() == 0)) return def;
		for(D_DirectoryEntry a : array){
			if (result != null )
				result = result + sep + ((a==null)?"":a.toSummary());
			else
				result = ((a==null)?"":(a.toSummary()));
		}
		return result;
	}
	public static String concatSI(Hashtable<String, Integer> array,
			String sep, String def) {
		if ((array == null)) return def;
		if ((array.size() == 0)) return def;
		String result=((array.get(0)==null)?"":array.get(0).toString());
		for(String s: array.keySet()) result = result+ sep + s+":"+array.get(s);
		return result;
	}
	public static String concat(Hashtable<String, String> array, String sep,
			String def) {
		if ((array == null)) return def;
		if ((array.size() == 0)) return def;
		String result=((array.get(0)==null)?"":array.get(0).toString());
		for(String s: array.keySet()) result = result+ sep + s+":"+array.get(s);
		return result;
	}
	public static <T> T[] selectRange(T[] lines, int offset, T[] result) {
		if(offset+result.length > lines.length) return null;
		for(int i=0; i<result.length; i++)
			result[i] = lines[i+offset];
		return result;
	}
	   public static <T> String concat(ArrayList<T> array, String sep, String def) {
			if ((array == null)) return def;
			if ((array.size() == 0)) return def;
			String result=((array.get(0)==null)?"":array.get(0).toString());
			for(int k=1; k<array.size(); k++) result = result+ sep +((array.get(k)==null)?"":array.get(k).toString());
			return result;
		}
	   public static String concatA(ArrayList<Address> array, String sep, String def) {
			if ((array == null)) return def;
			if ((array.size() == 0)) return def;
			String result=((array.get(0)==null)?"":array.get(0).toLongString());
			for(int k=1; k<array.size(); k++) result = result+ sep +((array.get(k)==null)?"":array.get(k).toLongString());
			return result;
		}
    public static <T> String concatSummary(ArrayList<?> array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.size() == 0)) return def;
		String result=((array.get(0)==null)?"":((Summary)array.get(0)).toSummaryString());
		for(int k=1; k<array.size(); k++) result = result+ sep +((array.get(k)==null)?"":((Summary)array.get(k)).toSummaryString());
		return result;
	}
    /**
     * 
     * @param hash
     * @param sep
     * @param def
     * @param key_sep : ": "
     * @return
     */
	public static String concat(Hashtable<String, String> hash,
			String sep, String def, String key_sep) {
		if(hash==null) return def;
		if(hash.size() == 0) return def;
		String result=null;
		for(String key : hash.keySet()) {
			String data = hash.get(key);
			if(result == null) result = "\""+key+"\""+key_sep+data;
			else result += sep + "\""+key+"\""+key_sep+data;
		}
		return result;
	}
	public static String concat(long[] array, String sep, String def) {
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=array[0]+"";
		for(int k=1; k<array.length; k++) result = result + sep + array[k];
		return result;
	}
	public static String concatUnsigned(byte[] array, String sep, String def) {
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=array[0]+"";
		for(int k=1; k<array.length; k++) result = result + sep + array[k];
		return result;
	}
	/**
	 * Prints unsigned values of the byte (converted with positive)
	 * @param array
	 * @param sep
	 * @param def
	 * @return
	 */
	public static String concat(byte[] array, String sep, String def) {
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=positive(array[0])+"";
		for(int k=1; k<array.length; k++) result = result + sep + positive(array[k]);
		return result;
	}
	public static <T> String nullDiscrimArray(T o[], String sep){
    	if(o==null) return "null";
    	return "#["+o.length+"]=\""+Util.concat(o, sep)+"\"";
    }
    public static <T> String nullDiscrimArraySummary(Summary o[], String sep){
    	if(o==null) return "null";
    	return "#["+o.length+"]=\""+Util.concatSummary(o, sep, "null")+"\"";
    }
    public static <T> String nullDiscrimArraySummary(ArrayList<T> o, String sep){
    	if(o==null) return "null";
    	try{
    		return "#["+o.size()+"]=\""+Util.concatSummary(o.toArray(new Summary[0]), sep, "null")+"\"";
    	}catch(Exception e){
    		return e.getLocalizedMessage();
    	}
    }
    public static <T> String nullDiscrimArray(ArrayList<T> o, String sep){
    	if(o==null) return "null";
    	try{
    		return "#["+o.size()+"]=\""+Util.concat(o.toArray(new Object[0]), sep, "null")+"\"";
    	}catch(Exception e){
    		return e.getLocalizedMessage();
    	}
    }
	public static String nullDiscrimArrayNumbered(Object[] o) {
    	if(o==null) return "null";
    	String result = "["+o.length+"]\"\n";
    	Object s;
    	for (int k=0; k<o.length; k++) {
    		result+=k+":   \""+o[k]+"\"\n";
    	}
    	return result;
	}
	public static <T> String concat(T[] array, String sep) {
		return concat(array, sep, "NULL");
	}
	public static <T> String concat(T[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result=((array[0]==null)?"":array[0].toString());
		for(int k=1; k<array.length; k++) result = result+ sep +((array[k]==null)?"":array[k].toString());
		return result;
	}
	public static <T> String concatNonNull(T[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result = null; 
		for (int k = 0; k < array.length; k ++) {
			if (array[k] == null) continue;
			if (result == null) { result = array[k].toString(); continue; }
			result = result+ sep +array[k].toString();
		}
		return result;
	}
	public static <T> String concatSummary(T[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result=((array[0]==null)?"":((Summary)array[0]).toSummaryString());
		for(int k=1; k<array.length; k++) result = result+ sep +((array[k]==null)?"":((Summary)array[k]).toSummaryString());
		return result;
	}
	public static String concatOrgs(D_PeerOrgs[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result=((array[0]==null)?"":array[0].toLongString());
		for(int k=1; k<array.length; k++) result = result+ sep +((array[k]==null)?"":array[k].toLongString());
		return result;
	}
	/**
	 * concatenate the ints in the array.
	 * Default is returned if array is null or 0 length
	 * @param array
	 * @param sep
	 * @param def
	 * @return
	 */
	public static String concat(int[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result=array[0]+"";
		for(int k=1; k<array.length; k++) result = result+ sep +array[k];
		return result;
	}
	/**
	 *  Returns null for null or 0 length array
	 * @param oid
	 * @return
	 */
	public static String OID2String(int[] oid) {
		if ((oid == null) || (oid.length == 0)) return null;
		String sep=".";
		String result=oid[0]+"";
		for(int k=1; k<oid.length; k++) result = result+ sep +oid[k];
		return result;
	}
	/**
	 * Returns null for null or 0 length array
	 * @param oid
	 * @return
	 */
	public static String BNOID2String(BigInteger[] oid) {
		if ((oid == null) || (oid.length == 0)) return null;
		String sep=".";
		String result=oid[0]+"";
		for(int k=1; k<oid.length; k++) result = result+ sep +oid[k];
		return result;
	}
	/**
	 * returns null for null or empty string, or for parsing failure
	 * @param soid
	 * @return
	 */
	public static int[] string2OID(String soid){
		System.out.println("Util:string2OID: start "+soid);
		int[] result = null;
		if(soid==null){
			System.out.println("Util:string2OID: oid null "+soid);
			return null;
		}
		soid = soid.trim();
		if("".equals(soid)){
			System.out.println("Util:string2OID: oid empty "+soid);
			return null;
		}
		String[] comps = soid.split(Pattern.quote("."));
		System.out.println("Util:string2OID: oids # "+comps.length);
		if(comps.length==0) return null;
		try {
			int[] _result;
			_result = new int[comps.length];
			for(int k=0; k<comps.length; k++){
				BigInteger bi;
				{
					try{
						bi = new BigInteger(comps[k]);
					}catch(Exception e){
						System.err.println("Util:string2BIOID: fail to parse integer:" + comps[k]);
						e.printStackTrace();
						return null;
					}
				}
				_result[k] = bi.intValue();
				System.out.println("Util:string2OID: oid "+k+": bi="+bi+" from:"+comps[k]+" -> "+_result[k]);
			}
			result = _result;
		}catch(Exception e) {
			e.printStackTrace();
			if(DEBUG) System.out.println("Util:string2OID: fail to convert OID string: \""+soid+"\" of #components="+comps.length);
		}
		return result;
	}
	/**
	 * returns null for null or empty string, or for parsing failure
	 * @param soid
	 * @return
	 */
	public static BigInteger[] string2BIOID(String soid){
		BigInteger[] result;
		if(soid==null) return null;
		soid = soid.trim();
		if("".equals(soid)){
			System.out.println("Util:string2BIOID: oid empty "+soid);
			return null;
		}
		String[] comps = soid.split(Pattern.quote("."));
		if(comps.length==0) return null;
		result = new BigInteger[comps.length];
		for(int k=0; k<comps.length; k++){
			try{
				result[k] = new BigInteger(comps[k]);
			}catch(Exception e){
				System.err.println("Util:string2BIOID: fail to parse integer: ["+k+"]=\"" + comps[k]+"\"of \""+soid+"\"");
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}
    private static byte[] init_HEX_R() {
		byte[] result = new byte[255];
		for (int k=0;k<result.length;k++) result[k] = 127;
		result['0'] =  0;result['1'] =  1;result['2'] =  2;result['3'] =  3;result['4'] = 4;
		result['5'] =  5;result['6'] =  6;result['7'] =  7;result['8'] =  8;result['9'] = 9;
		result['a'] = 10;result['b'] = 11;result['c'] = 12;result['d'] = 13;result['e'] = 14;result['f'] = 15;
		result['A'] = 10;result['B'] = 11;result['C'] = 12;result['D'] = 13;result['E'] = 14;result['F'] = 15;
		return result;
	}
	public static void copyBytes(byte[] results, int offset, int int32){
		byte[]src=new byte[4];
		src[0]=(byte) (int32 & 0xff);
		src[1]=(byte) ((int32>>8) & 0xff);
		src[2]=(byte) ((int32>>16) & 0xff);
		src[3]=(byte) ((int32>>24) & 0xff);
		copyBytes(results, offset, src, 4, 0);
	}
	public static void copyBytes(byte[] results, int offset, short int16){
		byte[]src=new byte[2];
		src[0]=(byte) (int16 & 0xff);
		src[1]=(byte) ((int16>>8) & 0xff);
		copyBytes(results, offset, src, 2, 0);
	}
	public static void copyBytes(byte[] results, int offset, byte[]src, int length, int src_offset){
		if(results.length<length+offset)
			System.err.println("Destination too short: "+results.length+" vs "+offset+"+"+length);
		if(src.length<length+src_offset)
			System.err.println("Source too short: "+src.length+" vs "+src_offset+"+"+length);
		for(int k=0; k<length; k++) {
			results[k+offset] = src[src_offset+k];
		}
	}
	public static void copyBytes(byte[] results, int offset, byte[]src, int length){
		copyBytes(results, offset, src,length,0);
	}
	public static boolean copyBytes_src_dst( byte[] source, int sourceOffset, byte[] destination, int destinationOffset, int size ) {
		if( size > (destination.length - destinationOffset) ) {
			System.out.println("Destination buffer is too small for input: " + source.length + " : " + (destination.length - destinationOffset) );
			return false;
		}
		for( int i = 0; i < source.length && i < size; i++ ) {
			destination[ i + destinationOffset ] = source[ sourceOffset + i ];
		}
		return true;
	}
	public static boolean copyBytes( byte[] source, byte[] destination, int destinationOffset, int size ) {
		return copyBytes_src_dst( source, 0, destination, destinationOffset, size );
	}
	public static boolean copyBytes( byte source, byte[] destination, int destinationOffset, int size ) {
		byte[] tmp = ByteBuffer.allocate( 1 ).put( source ).array();
		return copyBytes( tmp, destination, destinationOffset, size );
	}
	public static boolean copyBytes( short source, byte[] destination, int destinationOffset, int size ) {
		byte[] tmp = ByteBuffer.allocate( 2 ).putShort( source ).array();
		return copyBytes( tmp, destination, destinationOffset, size );
	}
	public static boolean copyBytes( int source, byte[] destination, int destinationOffset, int size ) {
		byte[] tmp = ByteBuffer.allocate( 4 ).putInt( source ).array();
		return copyBytes( tmp, destination, destinationOffset, size );
	}
	public static int ceil(double a){
		return (int)Math.round(Math.ceil(a));
	}
	public static short extBytes(byte[] src, int offset, short int16){
		int16=0;
		int16 |= (src[offset+1] & 0xff);
		int16 <<= 8;
		int16 |= (src[offset+0] & 0xff);
		return int16;
	}
	public static int extBytes(byte[] src, int offset, int int32){
		int32=0;
		int32 |= (src[offset+3] & 0xff);
		int32 <<= 8;
		int32 |= (src[offset+2] & 0xff);
		int32 <<= 8;
		int32 |= (src[offset+1] & 0xff);
		int32 <<= 8;
		int32 |= (src[offset+0] & 0xff);
		return int32;
	}
    public static ResourceBundle getResourceBundle(){
    	try{
    		return ResourceBundle.getBundle("DebateDecide");
    	}catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }
	public static String __(String s) {
    	String result;
    	try{
    		result = myResources.getString(s);
    		if(result==null) result =s;
    	}catch(Exception e){
    		result = s;
    	}
    	return result;
    }
    public static String getString(Object o){
    	return getString(o,null);
    }
    /**
     * If o is null, return default, else get a string from o;
     * @param o
     * @return
     */
	public static String getString(Object o, String _default) {
    	if(o==null) return _default;
    	return o.toString();
	}
	public static String getBString(byte[] bytes){
		if(bytes==null) return null;
		return new String(bytes);
	}
    /**
     * Get the long value from this Object (converted via a string)
     * @param obj
     * @param _default
     * @return
     */
	public static long lval(Object obj, long _default){
		if(obj == null) return _default;
		try{
			return Long.parseLong(obj.toString());
		}catch(Exception e){
			return _default;
		}
	}
	/**
	 * Returns "-1" on null.
	 * @param obj
	 * @return
	 */
	public static long lval(Object obj){
		return lval(obj, -1);
	}
	public static float fval(Object obj, float _default){
		if(obj == null) return _default;
		try{
			return Float.parseFloat(obj.toString());
		}catch(Exception e){
			return _default;
		}
	}
	public static Float Fval(Object obj, Float _default){
		if (obj == null) return _default;
		if (obj instanceof Float) return (Float)obj;
		try {
			return Float.parseFloat(obj.toString());
		} catch(Exception e){
			return _default;
		}
	}
    /**
     * Get the int value from this Object (converted via a string)
     * @param obj
     * @param _default
     * @return
     */
	public static int ival(Object obj, int _default){
		if (obj == null) return _default;
		try {
			return Integer.parseInt(obj.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return _default;
	}
    /**
     * Get the String value from this Object (converted via a string)
     * @param obj
     * @param _default
     * @return
     */
	public static String sval(Object obj, String _default){
		if(obj == null) return _default;
		return obj.toString();
	}
    public static int[] getInts(int n, int skip) {
    	int ints[]=new int[n];
    	int i=0, j=0;
    	for (; i<n; i++,j++){
    		if(j==skip) j++;
    		ints[i]=j;
    	}
    	return ints;
    }
    static String HEX[]={"0","1","2","3","4","5","6","7","8","9",
    			"A","B","C","D","E","F"};
    public static String getHEX(byte b){
    	return HEX[(b>>4)&0x0f]+HEX[b&0x0f];
    }
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
    /**
     * Nice printing "null" when o is null versus "\"o\"" when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrim(Object o){
    	if (o == null) return "null";
    	return "\""+o+"\"";
    }
    /**
     * Nice printing when o is null versus when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrimArray(String o[]){
    	if (o == null) return "null";
    	return "["+o.length+"]\""+Util.concat(o, ":")+"\"";
    }
    /**
     * Nice printing when o is null, print "null" versus "\"descr\"" when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrim(Object o, String descr){
    	if (o == null) return "null";
     	return "\""+descr+"\"";
    }
    public static String byteToHex(byte[] b, String sep){
    	if(b==null) return "NULL";
    	String result="";
    	for(int i=0; i<b.length; i++)
    		result = result+sep+HEX[(b[i]>>4)&0x0f]+HEX[b[i]&0x0f];
    	return result;
    }
    public static String byteToHex(byte[] b){
    	return Util.byteToHex(b,"");
    }
	/**
	 *  Added by Chip Widmer
	 * 
	 * Equivalent to toHex()
	 * @param bytes
	 * @return
	 */
    public static String getDecimalString( byte[] bytes )
    {
        String s = "";
        for ( byte b : bytes )
            s += String.format( "%d ", b & 0xff );
        return s;
    }
    public static String byteToHexDump(byte[] b){
    	if(b==null) return "NULL";
    	return Util.byteToHex(b,0,Math.min(b.length, Util.MAX_DUMP)," ")+((b.length>MAX_DUMP)?"...":"");
    }
    public static String byteToHexDump(byte[] b, String sep){
    	if(b==null) return "NULL";
    	return Util.byteToHex(b,0,Math.min(b.length, Util.MAX_DUMP),sep)+((b.length>MAX_DUMP)?"...":"");
    }
	public static String byteToHexDump(byte[] buffer, int peek) {
    	if(buffer==null) return "NULL";
	   	return Util.byteToHex(buffer,0,Math.min(peek,Math.min(buffer.length, Util.MAX_DUMP))," ")+((buffer.length>MAX_DUMP)?"...":"");
	}
    /**
     * For resources such as: motion, comment,
     * @param type
     * @param body
     * @return
     */
	@Deprecated
    public static String getGlobalID (String type, String body) {
		if(DEBUG)System.err.println("BEGIN Util.getGlobalID: type='"+type+"', body='"+body+"'");
		String result=null;
    	Date time = new Date();
    	byte md5sum[];
    	try {
    		MessageDigest digest = MessageDigest.getInstance(DD.APP_ID_HASH);
    		digest.update(type.getBytes());
    		digest.update(body.getBytes());
    		md5sum = digest.digest();
    	}catch(Exception e){
    		result = type+":"+body+":"+time;
    		if(DEBUG)System.err.println("DONE Util.getGlobalID: result="+result);
    		return result;
    	}
    	result = Util.stringSignatureFromByte(md5sum);
		if(DEBUG)System.err.println("DONE Util.getGlobalID: result="+result);
    	return result;
    }
    /**
     * Used for generating secret keys and certificate 
     * for peers, organization or constituents
     * @param type
     * @param body the comment is set in the SK part
     * @return type+"://"+body+"+RSA+"+N.toString(16)+"="+p.toString(16)+"*"+q.toString(16);
     */
	@Deprecated
    public static Cipher getKeyedGlobalID (String type, String body) {
    	Cipher suit = net.ddp2p.ciphersuits.Cipher.getCipher(usedCipherGenkey, usedMDGenkey,type+"://"+body);
    	return suit;
    }
	/**
	 * 
	 * Returns stringSignatureFromByte of input
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDPK(byte[] pk) {
		return Util.stringSignatureFromByte(pk); 
	}
	/**
	 * Returns Public Key for key
	 * Used to be 
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDPK(Cipher keys) {
		if(DEBUG)System.err.println("BEGIN Util.getKeyedIDPK: keys="+keys);
		PK _pk = keys.getPK();
		if(_pk==null){
			System.err.println("BEGIN Util.getKeyedIDPK: keys has no PK");
			return null;
		}
		byte[] pk = _pk.encode();
		return Util.stringSignatureFromByte(pk); 
	}
	public static String getKeyedIDPK(PK _pk) {
		byte[] pk = _pk.encode();
		return Util.stringSignatureFromByte(pk); 
	}
	/**
	 * Returns Public Key for key (N,e of RSA)
	 * Used to be 
	 * @param keys
	 * @return
	 */
	public static byte[] getKeyedIDPKBytes(Cipher keys) {
		if(DEBUG)System.err.println("BEGIN Util.getKeyedIDPK: keys="+keys);
		PK _pk = keys.getPK();
		if(_pk==null){
			System.err.println("BEGIN Util.getKeyedIDPK: keys has no PK");
			return null;
		}
		if(DEBUG) System.out.println("Util:getKeyedIDPKBytes "+_pk);
		byte[] pk = _pk.encode();
		if(DEBUG) System.out.println("Util:getKeyedIDPKBytes #"+pk.length);
		return pk;
	}
	/**
	 * Get the hash of the public key (not prepended with any marker)
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDPKhash(Cipher keys) {
		if(DEBUG) System.err.println("BEGIN Util.getKeyedIDPKhash: keys="+keys);
		if(keys == null) return null;
		PK _pk = keys.getPK();
		if(_pk==null){
			System.err.println("BEGIN Util.getKeyedIDPKhash: keys has no PK");
			return null;
		}
		String pk = Util.getKeyedIDPK(keys);
		if(DEBUG) System.out.println("Util:getKeyedIDPKhash "+_pk);
		return Util.getGIDhash(pk);
	}
	/**
	 * Returns Secret Key for key 
	 * @param keys 
	 * @return
	 */
	public static String getKeyedIDSK(Cipher keys) {
		SK _sk = keys.getSK();
		if(DEBUG) System.out.println("Util:getKeyedIDSK #"+_sk);
		byte[] sk = _sk.encode();
		if(DEBUG) System.out.println("Util:getKeyedIDSK #"+sk.length);
		return Util.stringSignatureFromByte(sk); 
	}
	/**
	 * Get the type of cipher and message digest, separated by a :: (Cipher.separator)
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDType(Cipher keys) {
		return keys.getType();
	}
	/**
	 * Verify the data signed based on the public key of local_peer_ID.
	 * @param data
	 * @param local_peer_ID
	 * @param signature
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean verifySignByPeerID(ASNObj data, String local_peer_ID, byte[] signature) throws P2PDDSQLException {
		D_Peer p = D_Peer.getPeerByLID_NoKeep(local_peer_ID, true);
		String _pk = p.getGID();
		if (_pk==null) return false;
		PK pk = Cipher.getPK(_pk);
		return verifySign(data, pk, signature);
	}
	/**
	 * Verify the data signed based on the public key of local_peer_ID.
	 * @param data
	 * @param local_peer_ID
	 * @param signature
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean verifySignByID(ASNObj data, String local_pk_ID, byte[] signature) {
		if (local_pk_ID == null) return false;
		PK pk = Cipher.getPK(local_pk_ID);
		return verifySign(data, pk, signature);
	}
	/**
	 * Verify the data signed based on the public key of pk_ID.
	 * @param msg
	 * @param local_peer_ID
	 * @param signature
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean verifySignByID(byte[] msg, String pk_ID, byte[] signature) {
		if (pk_ID == null){
			if(_DEBUG) System.err.println("\nUtil:verifySignByID:  sign null PK");
			return false;
		}
		PK pk = Cipher.getPK(pk_ID);
		if(DEBUG) System.out.println("\nUtil:verifySignByID:  sign="+Util.byteToHexDump(signature, ":"));
		if(DEBUG) System.out.println("\nUtil:verifySignByID:  pk="+pk);
		if(pk==null){
			Util.printCallPath("Failed to retrieve a publick key for verifying");
			Application_GUI.warning(__("Failed to retrieve a publick key for verifying"), __("No public key!"));
		}
		return verifySign(msg, pk, signature);
	}
	/**
	 * Verify the data signed based on the public key senderPK.
	 * @param data
	 * @param senderPK
	 * @param signature
	 * @return
	 */
	public static boolean verifySign(ASNObj data, PK senderPK, byte[] signature) {
		if(DEBUG)System.out.println("Util:verifySign: start");
		if (senderPK==null) return false;
		if (data==null) return false;
		byte [] message = data.encode();
		if(DEBUG)System.out.println("Util:verifySign: msg["+message.length+"]"+Util.byteToHexDump(message));
		return senderPK.verify_unpad_hash(signature, message);
	}
	/**
	 * Verify the data signed based on the public key senderPK.
	 * @param message
	 * @param senderPK
	 * @param signature
	 * @return
	 */
	public static boolean verifySign(byte[] message, PK senderPK, byte[] signature) {
		boolean result;
		if (DEBUG) System.err.println("Util: verifySign: start ");
		if (senderPK == null){
			System.err.println("Util: verifySign: null PK ");
			Util.printCallPath("verifying");
			return false;
		}
		try {
			if(DEBUG)System.out.println("Util: verifySign: v: sign="+Util.byteToHex(signature)+"\n hash="+Util.byteToHex(Util.simple_hash(signature, Cipher.MD5)));
			if(DEBUG)System.out.println("Util: verifySign: v: msg="+Util.byteToHex(message)+"\n hash="+Util.byteToHex(Util.simple_hash(message, Cipher.MD5)));
			if(DEBUG)System.out.println("Util: verifySign: v: pk="+senderPK);
			result = senderPK.verify_unpad_hash(signature, message);
			ECDSA_Signature sg = null; 
			try {
				if (DEBUG) sg = new ECDSA_Signature(signature);
			} catch (ASN1DecoderFail e) {
			}
			if(DEBUG)System.out.println("Util: verifySign: s="+sg+"\nv="+result);
		}catch(Exception e) {
				e.printStackTrace();
			return false;
		}
		if(!result) {
			if(DD.WARN_ABOUT_OTHER)Util.printCallPath("verifying failed");
		}
		if(DEBUG)System.err.println("Util:verifySign: return "+result);
		return result;
	}
	/**
	 * Sing using the current peer key
	 * @param data
	 * @param destinationID
	 * @return
	 */
	public static byte[] sign_peer(ASNObj data) {
		byte[] msg = data.encode();
		return sign_peer(msg);
	}
	/**
	 * Sign using the current peer key
	 * @param msg
	 * @return
	 */
	public static byte[] sign_peer(byte[] msg) {
		return sign(msg, HandlingMyself_Peer.getMyPeerSK());
	}
	/**
	 * Sing using the provided key
	 * @param data
	 * @param destinationID
	 * @return
	 */
	public static byte[] sign(ASNObj data, SK key) {
		byte[] msg = data.encode();
		return sign(msg,key);
	}
	/**
	 * Sing using the provided key
	 * @param msg
	 * @param destinationID
	 * @return
	 */
	public static byte[] sign(byte[] msg, SK key) {
		if(DEBUG) System.out.println("Util:sign "+Util.byteToHexDump(msg));
		if (key == null) {
			System.err.println("Util:sig:signing with empty key");
			Util.printCallPath("Empty key");
			return new byte[0];
		}
		if (DEBUG) System.err.println("Util: sign: msg["+msg.length+"]="+Util.byteToHexDump(msg, ":")+"   \n"+Util.getGID_as_Hash(msg));
		byte[] signature = key.sign_pad_hash(msg);
		if (DEBUG) System.err.println("Util: sign: sign["+signature.length+"]="+Util.byteToHexDump(signature, ":")+"   \n"+Util.getGID_as_Hash(signature));
		return signature; 
	}
    /**
     * Extract the extension from name, looking at the last dot
     * @param f
     * @return
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
    /**
     * Convert an array of string hexa bytes to array of bytes
     * @param splits
     * @return
     */
	public static byte[] hexToBytes(String[] splits) {
		if(splits==null) return null;
		byte[] res = new byte[splits.length];
		for(int k=0; k<splits.length; k++) {
			try{
				res[k] = hexToByte(splits[k]);
				if(DEBUG) System.out.println("res: \""+res[k]+"\"");
			}catch(RuntimeException e){
				if(DEBUG)e.printStackTrace();
				if(DEBUG) System.out.println("Length: "+k+"/"+splits.length);
				if(DEBUG) System.out.println("bres: \""+res[k]+"\"");
				return null;
			}
		}
		return res;
	}
	/**
	 * A two char string is converted to a byte
	 * @param string
	 * @return
	 */
	private static byte hexToByte(String string) {
		byte result=0;
			byte high = HEX_R[string.charAt(0)];
			if(DEBUG) if(high>15) throw new RuntimeException("Wrong 1st hex \""+string+"\" -- "+high);
			result += high<<4;
			high = HEX_R[string.charAt(1)];
			if(DEBUG) if(high>15) throw new RuntimeException("Wrong 2nd hex \""+string+"\" -- "+high);
			result += high;
		return result;
	}
	/**
	 * Return the text trimmed to length MAX_DUMP. If anything is removed, append "..."
	 * @param text
	 * @return
	 */
	public static String trimmed(String text) {
		return trimmed(text, MAX_DUMP);
	}
	public static String trimmedStrObj(byte[] text) {
		if(text == null) return null; 
		return trimmed(new String(text), MAX_DUMP);
	}
	/**
	 * Return the text trimmed to length len. If anything is removed, append "..."
	 * @param text
	 * @param len
	 * @return
	 */
	public static String trimmed(String text,int len) {
		if(text==null) return null;
		if(text.length()<=len) return text;
		return text.substring(0,len)+((text.length()>len)?"...":"");
	}
	/**
	 * Return the array where each component text trimmed removing spaces.
	 * @param text
	 * @param len
	 * @return
	 */
	public static String[] trimmed(String[] arr) {
		if(arr==null) return null;
		for(int k=0;k<arr.length;k++) arr[k] = arr[k].trim();
		return arr;
	}
	/**
	 * Prepend p in front of each element of the array arr
	 * @param arr
	 * @param p
	 * @return
	 */
	public static String[] prepend(String[] arr, String p) {
		if(arr==null) return null;
		for(int k=0;k<arr.length;k++) arr[k] = p+arr[k];
		return arr;
	}
	/**
	 * Add alias+"." in front of each string in the "," separated list of strings
	 * @param fields_list
	 * @param alias
	 * @return
	 */
	public static String setDatabaseAlias(String fields_list, String alias) {
		return Util.concat(Util.prepend(Util.trimmed(fields_list.split(Pattern.quote(","))),alias+"."), ",");
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
	public static void printCallPath(StackTraceElement[] l,
			String text, String prefix) {
		System.out.println("DDTrace: other path ["+text+"] ");
		if (l == null || l.length < 2) {
			System.out.println("0:" + l);
			return;
		}
		for (int i = 1; i < l.length-1; i ++) {
			System.out.println(prefix+"at "+l[i]);
		}
	}
	public static StackTraceElement[] getCallPath() {
		try{throw new Exception("getCallPath");}catch(Exception e){
			StackTraceElement[] l = e.getStackTrace();
			return l;
		}
	}
	/**
	 * Increment Calendar with inc milliseconds
	 * @param cal
	 * @param inc
	 * @return
	 */
	public static Calendar incCalendar(Calendar cal, int inc) {
		cal.setTimeInMillis(cal.getTimeInMillis() + inc);
		return cal;
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
		return date;
	}
	/**
	 * Get generalized time for this moment
	 * @return
	 */
    public static String getGeneralizedTime(){
    	return Encoder.getGeneralizedTime(CalendargetInstance());
    }
	/**
	 * Get the generalized date for the date at "days_ago" days ago
	 * @param days_ago
	 * @return
	 */
	public static String getGeneralizedDate(int days_ago) {
		Calendar c = CalendargetInstance();
		c.setTimeInMillis(c.getTimeInMillis()-days_ago*(60000*60*24l));
		return Encoder.getGeneralizedTime(c);
	}
	/**
	 * Return now at UTC
	 * @return
	 */
	public static Calendar CalendargetInstance(){
		return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	}
	/**
	 * Generate a random number between [0 and max)
	 * @param max
	 * @return
	 */
	public static float random(float max){
		float result = rnd.nextFloat()*max;
		return result;
	}
	/**
	 * To select a choice among options A0(prob pA0), A1(prob pA1), ... An(prob pAn)
	 * @param distrib[pA0,pA1,...pA(n-1)]
	 * @return i (index of selected choice)
	 */
	public static int pick_randomly(float[] distrib){
		float dice = random(1.0f);
		float cummulated = 0;
		for(int i=0; i<distrib.length; i++) {
			cummulated += distrib[i];
			if(dice < cummulated) return i;
		}
		return distrib.length;
	}
	/**
	 * Return randomly a "YES" or a "NO"
	 * @return
	 */
	public static String random_Y_N() {
		int rnd=get_one_or_zero();
		if(rnd==0) return "Yes";
		else if(rnd==1) return "No";
		else return null;
	}
	/**
	 * Return randomly a 0 or a 1
	 * @return
	 */
	public static int get_one_or_zero() {
		int randomInt4=rnd.nextInt(2);
		return randomInt4;
	}
	public static boolean equalBytes_null_or_not(byte[] a, byte[] b) {
		if((a==null) && (b==null)) return true;
		return equalBytes(a, b);
	}
	/**
	 * Check if the two non-null arrays are identical
	 * @param a
	 * @param b
	 * @return return false if any parameter is null
	 */
	public static boolean equalBytes(byte[] a, byte[] b) {
		if((a==null)||(b==null)){
			if(DEBUG) System.out.println("Util:equalBytes: null? a="+Util.byteToHex(a, ":"));
			if(DEBUG) System.out.println("Util:equalBytes: null? b="+Util.byteToHex(b, ":"));
			return false;
		}
		if(a.length!=b.length) {
			if(DEBUG) System.out.println("Util:equalBytes: a["+a.length+"]="+Util.byteToHex(a, ":"));
			if(DEBUG) System.out.println("Util:equalBytes: b["+b.length+"]="+Util.byteToHex(b, ":"));
			return false;
		}
		for(int k=0; k<a.length; k++)
			if(a[k]!=b[k]){
				if(DEBUG) System.out.println("Util:equalBytes: a["+k+"/"+a.length+"]="+Util.byteToHex(a, ":"));
				if(DEBUG) System.out.println("Util:equalBytes: b["+k+"/"+b.length+"]="+Util.byteToHex(b, ":"));
				return false;
			}
		if(DEBUG) System.out.println("Util:equalBytes: good a["+a.length+"]="+Util.byteToHex(a, ":"));
		if(DEBUG) System.out.println("Util:equalBytes: good b["+b.length+"]="+Util.byteToHex(b, ":"));
		return true;
	}
	/**
	 * Split a string of hex numbers in substrings of pairs of 2
	 * @param senderID
	 * @returnpublic static final String 
	 */
	public static String[] splitHex(String senderID) {
		if ((senderID==null)||(senderID.length()%2==1)) return null;
		String splits[] = new String[senderID.length()/2];
		for(int k=0; k<splits.length; k++)
			splits[k] = senderID.substring(k*2, k*2+2);
		return splits;
	}
	/**
	 * Return the hash of msg with the algorithm alg,
	 *  prefixed as: "DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(msg))"
	 *  
	 * @param msg
	 * @param alg such as DD.APP_ID_HASH, Cipher.SHA256
	 * @return
	 */
	public static String getHash(byte[] bPK, String alg) {
		byte[] md = simple_hash(bPK, alg);
		return alg+DD.APP_ID_HASH_SEP+Util.stringSignatureFromByte(md); 
	}
	/**
	 * Return the hash of msg with the algorithm alg
	 * @param msg
	 * @param alg for example Cipher.MD5, DD.APP_OTHER_HASH
	 * @return
	 */
	public static byte[] simple_hash(byte[] msg, String hash_alg) {
		return simple_hash(msg, 0, msg.length, hash_alg);
	}
	/**
	 * Return the hash of msg with the algorithm alg
	 * @param msg
	 * @param off
	 * @param len
	 * @param alg for example Cipher.MD5, DD.APP_OTHER_HASH
	 * @return
	 */
	public static byte[] simple_hash(byte[] msg, int off, int len, String hash_alg) {
		MessageDigest digest;
		try {
		   	if(DEBUG)System.out.println("hash = "+hash_alg);
			digest = MessageDigest.getInstance(hash_alg);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return null;}
		digest.update(msg, off, len);
		byte mHash[] = digest.digest();
		return mHash;
	}
	/**
	 * Returns false if any is null, or if different
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equalStrings_and_not_null(String a, String b) {
		if((a==null)||(b==null)) return false;
		return a.equals(b);
	}
    /**
     * Returns true only if they are equal, or one null
     * @param a
     * @param b
     * @return
     */
	public static boolean equalStrings_or_one_null(String a, String b){
    	if((a==null) && (b==null)) return false;
		if((a==null)||(b==null)) return true;
		return (a.compareTo(b)==0);
	}
    /**
     * Returns true only if they are equal (or both null)
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equalStrings_null_or_not(String s1, String s2) {
    	if((s1==null) && (s2==null)) return true;
    	if((s1==null)||(s2==null)) return false;
    	return s1.equals(s2);
    }
	/**
	 * Create string with "1" for true and "0" for false
	 * @param value
	 * @return
	 */
	public static String getIntStringBool(Object value) {
		if ((value==null)|| !(value instanceof Boolean)) return "0";
		Boolean v = (Boolean)value;
		if(v.booleanValue()) return "1";
		return "0";
	}
	public static byte[] readAllBytes(InputStream in) throws IOException {
		byte[] buffer = new byte[Util.MAX_CONTAINER_SIZE];
		return readAllBytes(in, buffer);
	}
	/**
	 * returns an array filled with data read (of the size of the data read
	 * @param in
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
	public static byte[] readAllBytes(InputStream in, byte[] buffer) throws IOException {
		int len = readAll(in, buffer);
		while (len == buffer.length) {
			buffer = Arrays.copyOf(buffer, len*2);
			len = readAll(in, len, buffer, len);
		}
		byte[] result =	Arrays.copyOf(buffer, len);
		return result;		
	}
	/**
	 * Read all the expected data from a stream, in a buffer
	 * @param in : stream
	 * @param stg : buffer
	 * @return the amount of data read
	 * @throws IOException
	 */
	public static int readAll(InputStream in, byte[] stg) throws IOException {
		return readAll(in, stg, stg.length);
	}
	/**
		 * Read all the expected data from a stream, in a buffer
		 * @param in : stream
		 * @param stg : buffer
		 * @param length : #bytes expected
		 * @return the amount of data read
		 * @throws IOException
	 */
	public static int readAll(InputStream in, byte[] stg, int length) throws IOException {
		int got = 0;
		return readAll(in, got, stg, length);
	}
	/**
	 * Read at most "length" bytes starting from address got into buffer
	 * @param in
	 * @param got
	 * @param stg
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public static int readAll(InputStream in, int got, byte[] stg, int length) throws IOException {
		int remaining = length;
		int crt;
		if (length > stg.length) throw new IOException("Buffer too small!");
		do {
			crt = in.read(stg, got, remaining);
			if(crt < 0) return got;
			got += crt;
			remaining -= crt;
		} while(remaining > 0);
		return got;
	}
	static char b64 = '_';
	/**
	 * Standardized conversion from byte[] to String for signatures
	 * uses base64 preceded by a ""
	 * @param signature
	 * @return
	 */
	public static String stringSignatureFromByte(byte[] signature) {
		if(signature==null) return null;
		return new String(Base64Coder.encode(signature));
	}
	/**
	 * Conversion based on hex representation
	 * @param signature
	 * @return
	 */
	public static String _stringSignatureFromByte(byte[] signature) {
		if(signature==null) return null;
		return Util.byteToHex(signature);
	}
	/**
	 * Standardized conversion from String to byte[] for signatures
	 * uses base64 (old version only if the string starts with "_", and used hex otherwise)
	 * @param signature
	 * @return
	 */
	public static byte[] byteSignatureFromString(String signature) {
		return byteSignatureFromString(signature, true);
	}
	/**
	 * 
	 * @param signature
	 * @param verbose : silent of error (expected)
	 * @return
	 */
	public static byte[] byteSignatureFromString(String signature, boolean verbose) {
		byte[] result = null;
		if(signature==null) return null;
		try {
			char[] s = signature.toCharArray();
			result = Base64Coder.decode(s, 0, s.length);
		} catch(Exception e) {
			if (verbose) {
				System.err.println("Util:byteSignatureFromString:Decoding:\n\""+signature+"\"");
				e.printStackTrace();
			}
		}
		return result;
	}
	/**
	 * This uses hex
	 * @param signature
	 * @return
	 */
	public static byte[] _byteSignatureFromString(String signature) {
		byte[] result = null;
		if(signature==null) return null;
		try {
			result = Util.hexToBytes(Util.splitHex(signature));
		}catch(Exception e){e.printStackTrace();}
		return result;
	}
	/**
	 * Standard algorithm to get a globalID_hash from a hex string, using DD.APP_OTHER_HASH
	 * used for constituents. verbose=true
	 * 
	 * DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(decode_64(ID)))
	 * 
	 * 
	 * @param globalID
	 * @return
	 */
	public static String getGIDhash(String globalID) {
		return getGIDhashFromGID(globalID, true);
	}
	/**
	 * Get has hash of the public key
	 * Return the hash of msg with the default algorithm DD.APP_ID_HASH
	 * as: DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(msg))
	 * @param msg
	 * @return
	 */
	public static String getGID_as_Hash(byte[] msg) {
		return getHash(msg, DD.APP_ID_HASH);
	}
	/**
	 * Called when transforming a GID to a GIDhash
	 * DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+bse64(hash(msg))
	 * 
	 * @param msg
	 * @return
	 */
	public static String getGIDhashFromGID(byte[] msg) {
		return getGID_as_Hash(msg);
	}
	/**
	 * Standard algorithm to get a globalID_hash from a signature string, using DD.APP_OTHER_HASH
	 * DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(decode_64(ID)))
	 * 
	 * @param globalID
	 * @param verbose : false for silent on expected exceptions
	 * @return
	 */
	public static String getGIDhashFromGID(String globalID, boolean verbose) {
		byte[] idbytes = Util.byteSignatureFromString(globalID, verbose);
		if(idbytes == null){
			if(DEBUG)System.out.println("Server:update_insert_peer null id bytes");
			return null;
		}
		String IDhash = getGIDhashFromGID(idbytes);
		return IDhash;
	}
	/**
	 * 
	 * @param string
	 * @return
	 */
	public static String trimInterFieldSpaces(String string) {
		String[]strings = string.split(Pattern.quote(","));
		strings=Util.trimmed(strings);
		return Util.concat(strings, ",");
	}
	public static byte[] getHashFromGID(String id) {
		return Util.byteSignatureFromString(id);
	}
	/**
	 * searches a GID in the database
	 * @param gID
	 * @return
	 */
	public static SK getStoredSK(String gID) {
		return getStoredSK(gID,null);
	}
	/**
	 * Current implementation uses only public_gID if available.
	 * @param public_gID
	 * @param id_hash
	 * @return
	 */
	public static SK getStoredSK(String public_gID, String id_hash) {
		if (public_gID == null) return getStoredSKByHash(id_hash);
		String sql = " SELECT "+net.ddp2p.common.table.key.secret_key+
				" FROM "+net.ddp2p.common.table.key.TNAME+
				" WHERE "+net.ddp2p.common.table.key.public_key+"= ?;";
		java.util.ArrayList<java.util.ArrayList<Object>> o;
		try {
			o = Application.getDB().select(sql, new String[]{public_gID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if (o.size() < 1) return null;
		String _sk = Util.getString(o.get(0).get(0));
		return Cipher.getSK(_sk);
	}
	/**
	 * 
	 * @param id_hash
	 * @return
	 */
	private static SK getStoredSKByHash(String id_hash) {
		if(id_hash == null) return null;
		String sql = " SELECT "+net.ddp2p.common.table.key.secret_key+
				" FROM "+net.ddp2p.common.table.key.TNAME+
				" WHERE "+net.ddp2p.common.table.key.ID_hash+"= ?;";
		java.util.ArrayList<java.util.ArrayList<Object>> o;
		try {
			o = Application.getDB().select(sql, new String[]{id_hash}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(o.size()<1)return null;
		return Cipher.getSK(Util.getString(o.get(0).get(0)));
	}
	public static String concatPeerOrgs(net.ddp2p.common.data.D_PeerOrgs[] array) {
		String def = "";
		String sep = ":";
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=Util.stringSignatureFromByte(array[0].getEncoder().getBytes());
		for(int k=1; k<array.length; k++) result = result + sep + Util.stringSignatureFromByte(array[k].getEncoder().getBytes());
		return result;
	}
	public static String concatPeerOrgsTrimmed(net.ddp2p.common.data.D_PeerOrgs[] array) {
		String def = "";
		String sep = ":";
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=Util.trimmed(Util.stringSignatureFromByte(array[0].getEncoder().getBytes()),5);
		for(int k=1; k<array.length; k++) result = result + sep + Util.trimmed(Util.stringSignatureFromByte(array[k].getEncoder().getBytes()),5);
		return result;
	}
	/**
	 * Used when parsing an address from DDAddress
	 * @param pos
	 * @return
	 */
	public static net.ddp2p.common.data.D_PeerOrgs[] parsePeerOrgs(String pos) {
		String def = "";
		String sep = ":";
		if(pos == null) return null;
		if(def.equals(pos)) return null;
		String[] po = pos.split(Pattern.quote(sep));
		net.ddp2p.common.data.D_PeerOrgs[] result = new net.ddp2p.common.data.D_PeerOrgs[po.length];
		for(int k=0; k<po.length; k++) {
			net.ddp2p.ASN1.Decoder d = new net.ddp2p.ASN1.Decoder(Util.byteSignatureFromString(po[k]));
			try {
				result[k] = new net.ddp2p.common.data.D_PeerOrgs().decode(d);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	/**
	 * Conversion via ASN1
	 * @param utf8
	 * @return
	 */
	public static String utf8_ascii(String utf8){
		if(utf8==null) return null;
		return Util.stringSignatureFromByte(new Encoder(utf8).getBytes());
	}
	/**
	 * Conversion via ASN1
	 * @param ascii
	 * @return
	 */
	public static String ascii_utf8(String ascii){
		if(ascii==null) return null;
		return new Decoder(Util.byteSignatureFromString(ascii)).getString();
	}
    /**
	 * Return null if <=0, else return string. 
	 * @param id
	 * @return
	 */
	public static String getStringID(long id) {
		if (id<=0)return null;
		return ""+id;
	}
	public static double dval(Object o, double _l) {
		if(o==null) return _l;
		try {
			double result = new Double(o.toString()).doubleValue();
			return result;
		}catch(Exception e) {
			e.printStackTrace();
			return _l;
		}
	}
	public static String bool2StringInt(boolean val) {
		return val?"1":"0";
	}
	/**
	 * Converts 1, true, True ... to true
	 * @param object
	 * @param b
	 * @return
	 */
	public static boolean stringInt2bool(Object object, boolean b) {
		String val = Util.getString(object);
		if(val == null) return b;
		String _val = Util.getString(val);
		if(val.equals("1")) return true;
		if(val.equals("0")) return false;
		if(_val.toLowerCase().equals("true")) return true;
		if(_val.toLowerCase().equals("false")) return false;
		return b;
	}
	public static ArrayList<String> AL_AL_O_2_AL_S(
			ArrayList<ArrayList<Object>> in) {
		if(DEBUG && (in!=null)) out.println("Util:AL_AL_O_2_AL_S: in=#"+in.size());
		ArrayList<String> result = new ArrayList<String>();
		if ((in==null)||(in.size()==0)) return result;
		for(ArrayList<Object> o: in){
			result.add(Util.getString(o.get(0)));
		}
		return result;
	}
	/**
	 * 
	 * @param in
	 * 	<(GIDH,creation_date),(GIDH,creation_date),...>
	 * @return
	 * <GIDH,creation_date>
	 */
	public static Hashtable<String, String> AL_AL_O_2_HSS_SS(
			ArrayList<ArrayList<Object>> in) {
		if (DEBUG && (in != null)) out.println("Util:AL_AL_O_2_HSS_SS: in=#"+in.size());
		Hashtable<String, String> result = new Hashtable<String, String>();
		if ((in == null) || (in.size() == 0)) return result;
		for (ArrayList<Object> o: in) {
			String value = Util.getString(o.get(1));
			String key = Util.getString(o.get(0));
			if (value == null) {
				value = "00000000000000000Z";
				if (DEBUG) Util.printCallPath("Null value for key=" + key);
			}
			if (value == null || key == null) {
				if (DEBUG) Util.printCallPath("Null value for key=" + key + " val=" + value);
				continue;
			}
			result.put(key, value);
		}
		return result;
	}
	/**
	 * Overwrites/appends where the FileWriter opens
	 * @param file
	 * @param string
	 * @throws IOException
	 */
	public static void storeStringInFile(File file, String string) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write(string);
		out.close();
	}
	public static void storeStringInFile(String fileName, String string) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		out.write(string);
		out.close();
	}
	public static String loadFile(String fileName) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		String result ="";
		String crt;
		while(( crt = in.readLine())!=null) {
			result += crt;
		}
		in.close();
		return result;
	}
	public static String getParent(String file){
		File _linux_path = new File(file);
		String _parent = _linux_path.getParent();
		return _parent;
	}
	public static String getManifestInfo() {
	    Enumeration resEnum;
	    try {
	        resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
	        while (resEnum.hasMoreElements()) {
	            try {
	                URL url = (URL)resEnum.nextElement();
	                InputStream is = url.openStream();
	                if (is != null) {
	                    Manifest manifest = new Manifest(is);
	                    Attributes mainAttribs = manifest.getMainAttributes();
	                    for(Object val : mainAttribs.keySet()){
	                    	System.out.println("Key: "+val+": "+mainAttribs.get(val));
	                    }
	                    String version = mainAttribs.getValue("Implementation-Version");
	                    if(version != null) {
	                        return version;
	                    }
	                }
	            }
	            catch (Exception e) {
	            }
	        }
	    } catch (IOException e1) {
	    }
	    return null; 
	}
    public static String readAll(InputStream is) throws IOException {
		BufferedReader bri = new BufferedReader (new InputStreamReader(is));
		return readAll(bri);
	}
	public static String readAll(BufferedReader bri) throws IOException {
		String result = "";
		do {
			String tmp = bri.readLine();
			if(tmp == null) break;
			result += tmp+"\n";
		}while(true);
		return result;
	}
	/**
	 * Extracts simple IP ([10,0,0,102]) from "google/10.0.0.102"
	 * not used
	 * @param hostName_IP
	 * @return
	 */
	public static byte[] getInetAddressBytes_from_HostName_IP(String hostName_IP) {
		if (hostName_IP == null) return null;
		String val[]=hostName_IP.split(Pattern.quote("/"));
		if((val==null) || (val.length==0)) return null;
		String clean = val[val.length-1];
		if(DEBUG)System.out.println("Util:getInetAddressBytes_from_HostName_IP: clean : "+clean);
		return getBytesFromCleanIPString(clean);
	}
	/**
	 * Extracts simple IP ([10,0,0,102]) from "10.0.0.102/255.0.0.0"
	 * @param iP_Mask
	 * @return
	 */
	public static byte[] getInetAddressBytes_from_IP_Mask(String iP_Mask) {
		if (iP_Mask == null) return null;
		String val[]=iP_Mask.split(Pattern.quote("/"));
		if((val==null) || (val.length==0)) return null;
		if(DEBUG)
			for(int i=0;i<val.length;i++)
				System.out.println("val : ["+i+"] "+val[i]);
		String clean = val[0];
		if(DEBUG)System.out.println("Util:getInetAddressBytes_from_IP_Mask : "+clean);
		byte[] result = getBytesFromCleanIPString(clean);
		return result;
	}
	/**
	 * Extracts simple IP ([10,0,0,102]) from "10.0.0.102/255.0.0.0"
	 * @param ip
	 * @param _mask
	 * @return
	 */
	public static byte[] get_broadcastIP_from_IP_and_NETMASK(String ip, String _mask){
		byte[] iP = getBytesFromCleanIPString(ip);
		byte[] mask = getBytesFromCleanIPString(_mask);
		byte[] result = new byte[4];
		byte ff = (byte)0xff;
		if((iP == null)||(mask==null)) return new byte[]{ff,ff,ff,ff};
		for(int i=0; i<=3; i++) {
			result[i] = (mask[i]==ff)?iP[i]:ff;
		}
		return result;
	}
	/**
	 * returns true if this IP is a valid IP to be forwarded on a network
	 *  with base _base and mask _mask
	 * @param ip
	 * @param _base
	 * @param _mask
	 * @return
	 */
	public static boolean ip_compatible_with_network_mask(String ip,
			String _base, String _mask) {
		if(ip.compareTo(DD.WINDOWS_NO_IP)==0) return false;
		byte[] iP = getBytesFromCleanIPString(ip);
		if(iP==null) return false;
		if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: ip  =="+Util.byteToHex(iP));
		byte[] mask = getBytesFromCleanIPString(_mask);
		if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: mask=="+Util.byteToHex(mask));
		if(mask==null) return false;
		byte[] base = getBytesFromCleanIPBaseFromString(_base);
		if(base == null) base = new byte[0];
		if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: base=="+Util.byteToHex(base));
		for(int k=0;k<base.length; k++) {
			if((mask[k]&iP[k]) != (base[k]&mask[k])) return false;
		}
		return true;
	}
	public static byte[] getBytesFromCleanIPBaseFromString(String clean){
		if(clean==null) return new byte[0];
		String[] bytes=clean.split(Pattern.quote("."));
		if((bytes==null)||(bytes.length>4)) return new byte[0];
		byte[] result = new byte[bytes.length];
		try{
			for(int i=0; i<bytes.length; i++){
				if(DEBUG) System.out.println("Util:getBytesFromCleanIPBaseFromString: parsing base=="+i+" "+bytes[i]);
				result[i]= (byte)Integer.parseInt(bytes[i]);
			}
		}catch(Exception e){
			System.err.println("Util:getBytesFromCleanIPBaseFromString: Error Message: "+e.getMessage());
			return null;
		}
		return result;
	}
	/**
	 * Extracts simple IP ([10,0,0,102]) from "10.0.0.102"
	 * @param clean
	 * @return
	 */
	public static byte[] getBytesFromCleanIPString(String clean){
		try{
			byte[] result = new byte[4];
			String[] bytes=clean.split(Pattern.quote("."));
			if((bytes==null)||(bytes.length!=4)) return null;
			for(int i=0; i<4; i++){
				int val = Integer.parseInt(bytes[i]);
				if((val< -256)||(val > 255)) return null;
				result[i] = (byte)val;
			}
			return result;
		}catch(Exception e) {
			System.err.println("Util:getBytesFromCleanIPString: error="+e.getLocalizedMessage());
			return null;
		}
	}
	/**
	 * Extracts simple IP ("10.0.0.102") from "host/10.0.0.102:54321"
	 * @param addr
	 * @return
	 */
	public static String get_IP_from_SocketAddress(String addr) {
		String val[]=addr.split(Pattern.quote("/"));
		if((val==null) || (val.length==0)) return null;
		String ip_port = val[val.length-1];
		String[] split = ip_port.split(Pattern.quote(":"));
		if(split.length==0) return null;
		return split[0];
	}
	/**
	 * from sock addr
	 * @param clientAddress
	 * @return
	 */
	public static String get_IP_from_SocketAddress(SocketAddress clientAddress) {
		if (clientAddress == null) return null;
		String addr = clientAddress.toString();
		if (addr == null) return null;
		return get_IP_from_SocketAddress(addr);
	}
	public static final Object scripts_monitor = new Object();
	public static Process crtScriptProcess = null;
	public static final int INTERRUPTED = 325;
	private static final boolean _OLD_CODE = false;
	public static void stopCrtScript(){
		try{
			if(crtScriptProcess!=null) crtScriptProcess.destroy();
		}catch(Exception e){}
	}
	static public void main(String[] arg) {
		System.out.println("Result ="+Util.prefixNumber(arg[0]));
		if(true) return;
		main3(arg);
		if(true) return;
		System.out.println(getManifestInfo());
	}
	public static void main2 (String[]args){
		get_broadcastIP_from_IP_and_NETMASK("10.0.0.12","255.0.0.0");
	}
	public static void main3 (String[]args){
		boolean r = Util.ip_compatible_with_network_mask("10.5.3.1", "10.0.0.","255.0.0.0");
		System.out.println("result = "+r);
	}
	public static Map<? extends String, ? extends String> getHSS(
			Set<String> keySet) {
		Hashtable<String,String> result = new Hashtable<String, String>();
		for(String s: keySet) {
			result.put(s, DD.EMPTYDATE);
		}
		return result;
	}
	public static String getNonBlockingHostName(InetAddress domain) {
		return getHostName(domain, null);
	}
	public static String getNonBlockingHostName(InetSocketAddress sock_addr) {
		return getHostName(null, sock_addr);
	}
	public static String getHostName(InetAddress domain, InetSocketAddress sock_addr) {
		if(DEBUG) System.out.println("Util:getHostNamexx: start");
		GetHostName gh = new GetHostName(domain, sock_addr);
		if(DEBUG) System.out.println("Util:getHostNamexx: inited");
		synchronized(gh) {
			if(DEBUG) System.out.println("Util:getHostNamexx: sync");
			try {
				while(gh.hostName == null) {
					gh.wait(DD.GETHOSTNAME_TIMEOUT_MILLISECONDS);
				}
				if("".equals(gh.hostName)) gh.wait(DD.GETHOSTNAME_TIMEOUT_MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			gh.interrupt();
		}
		if(DEBUG) System.out.println("Util:getHostNamexx: done="+gh.hostName);
		return gh.hostName;
	}
	public static InetAddress getNonBlockingHostIA(String domain) {
		return getHostIA(domain);
	}
	final static Hashtable<String, GetHostIA> hostIA_addresses = new Hashtable<String, GetHostIA>();
	public static InetAddress getHostIA(String domain) {
		if(DEBUG) System.out.println("Util:getHostIAxx: start");
		GetHostIA gh;
		synchronized(hostIA_addresses) {
			gh = hostIA_addresses.get(domain);
			if (gh == null) 
				hostIA_addresses.put(domain, gh = new GetHostIA(domain));
		}
		if(DEBUG) System.out.println("Util:getHostIAxx: inited");
		synchronized(gh) {
			if(DEBUG) System.out.println("Util:getHostIAxx: sync");
			try {
				while(!gh.started ) {
					gh.wait(DD.GETHOSTNAME_TIMEOUT_MILLISECONDS);
				}
				if(null == gh.hostIA) gh.wait(DD.GETHOSTNAME_TIMEOUT_MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			gh.interrupt();
		}
		synchronized(hostIA_addresses) {
			if (gh.hostIA == null) {
				hostIA_addresses.remove(domain);
				if(DEBUG) System.out.println("Util:getHostIAxx: removed");
			}
		}
		if(DEBUG) System.out.println("Util:getHostIAxx: done="+gh.hostIA);
		return gh.hostIA;
	}
	public static String validateIP(String candidate, String orig) {
		byte[] ip = Util.getBytesFromCleanIPString(candidate);
		if (ip==null) return orig;
		return candidate;
	}
	public static String validateIPBase(String candidate, String orig) {
		byte[] base = Util.getBytesFromCleanIPBaseFromString(candidate);
		if(base == null) return orig;
		String result = makeIPBase(base);
		if(DEBUG) System.out.println("Util:validateIPBase: candidate="+candidate+" got="+result);
		return result;
	}
	public static String makeIPBase(byte[] base){
		if(DEBUG) System.out.println("Util:makeIPBase: got="+Util.byteToHex(base));		
		if((base == null)||(base.length == 0)) return Util.getStringIP(base);
		if(base.length == 4) return Util.getStringIP(base);
		else return Util.getStringIP(base)+".";
	}
	public static String getStringIP(byte[] base) {
		if(DEBUG) System.out.println("Util:getStringIP: got="+Util.byteToHex(base));		
		String result = "";
		if((base == null) || (base.length==0)) return result;
		result += base[base.length-1];
		for(int k=base.length-2; k>=0; k--) {
			result = base[k]+"."+result;
		}
		if(DEBUG) System.out.println("Util:getStringIP: got="+result);		
		return result;
	}
	public static String makeIPFromBaseAndByte(String _byte) {
		byte[] base = Util.getBytesFromCleanIPBaseFromString(DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
		int len;
		if(base == null) base = new byte[0];
		len = 4-base.length;
		String result = makeIPBase(base);
		for(int i=0; i<len; i++) {
			if (!((i==0) && (base.length==0)))
				result += ".";
			result += _byte;
		}
		return result;
	}
	/**
	 * Extract an int from longest prefix of s
	 * @param s
	 * @return
	 */
	public static int prefixNumber(String s) {
		if(s == null) return 0;
		if(s.length() == 0) return 0;
		int k=0;
		for (; k < s.length(); k++){
			if (s.charAt(k) < '0') break;
			if (s.charAt(k) > '9') break;
		}
		if (k == 0) return 0;
		return Integer.parseInt(s.substring(0, k));
	}
	/**
	 * Tests whether the calendars are euqal or both null
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static boolean equalCalendars_null_or_not(Calendar c1,
			Calendar c2) {
		if ((c1 == null) && (c2 == null)) return true;
		if ((c1 == null) || (c2 == null)) return false;
		return c1.equals(c2);
	}
	public static int[] mkIntArray(String[] iDs_strings) {
		if(DEBUG&&(iDs_strings!=null))System.out.println("Util:mkIntArray: input #"+iDs_strings.length+"="+Util.concat(iDs_strings, ";"));
		int[] result = new int[iDs_strings.length];
		for(int i=0; i<result.length; i++)
			try{result[i] = Integer.parseInt(iDs_strings[i]);}catch(Exception e){e.printStackTrace();}
		return result;
	}
	public static String mkArrayCounter(int length, String sep) {
		if(length<=0) return "";
		String result="0";
		for(int i=1;i<length; i++)
			result+=sep+i;
		return result;
	}
	public static String concat(float[] t, String sep) {
		if(t.length<=0) return "";
		String result=t[0]+"";
		for(int i=1;i<t.length; i++)
			result+=sep+t[i];
		return result;
	}
	public static void insertSort(ArrayList<BroadcastQueueRequested.Received_Interest_Ad> list,
			BroadcastQueueRequested.Received_Interest_Ad item, int i, int j) {
		if(i==j){
			list.add(i, item);
			return;
		}
		int mid = (i+j)/2;
		if(item.interest_expiration_date < list.get(mid).interest_expiration_date){
			insertSort(list, item, 0, mid);
		}else{
			insertSort(list, item, mid+1, j);			
		}
	}
	public static long get_long(Object o) {
		if(o==null) return 0;
		if(o instanceof Long) return ((Long) o).longValue();
		if(o instanceof Integer) return ((Integer) o).longValue();
		return Long.parseLong(o.toString());
	}
	public static int get_int(Object o) {
		if(o==null) return 0;
		if(o instanceof Long) return ((Long) o).intValue();
		if(o instanceof Integer) return ((Integer) o).intValue();
		return Integer.parseInt(o.toString());
	}
	public static Integer Ival(Object i) {
		if(i==null) return null;
		if(i instanceof Integer) return (Integer)i;
		return new Integer(""+Integer.parseInt(i.toString()));
	}
	public static Long Lval(Object i) {
		if (i == null) return null;
		if (i instanceof Long) return (Long)i;
		return new Long(""+Long.parseLong(i.toString()));
	}
	public static boolean emptyString(String in) {
		if(in==null) return true;
		return "".equals(in);
	}
	public static String sanitizeFileName(String name) {
		String result="";
		if(name==null) return result;
		for(int k=0; k<name.length(); k++){
			char c = name.charAt(k);
			if(Character.isLetterOrDigit(c)) result+=c;
			else result+="_";
		}
		return result;
	}
	/**
	 * Is the val available in the list?
	 * Return the index, or "-1"
	 * @param list
	 * @param val
	 * @return
	 */
	public static int contains(short[] list, short val) {
		int absent = -1;
		if(list == null) return absent;
		for(int k=0; k<list.length; k++){
			if(DEBUG) System.out.println("Util:contains: "+k+"/"+list.length+":"+val+" in "+list[k]);
			if(val==list[k]){
				if(DEBUG) System.out.println("Util:contains: found "+k+":"+val+" in "+list[k]);
				return k;
			}
		}
		return absent;
	}
	/**
	 * returns -1 on error
	 * @param list
	 * @param val
	 * @return
	 */
	public static int contains(String[] list, String val) {
		int absent = -1;
		if(val == null) return -1;
		val = val.trim();
		if(list == null) return absent;
		for(int k = 0; k<list.length; k++) {
			if(list[k]==null) continue;
			if(val.equals(list[k].trim())) return k;
		}
		return absent;
	}
	/**
	 * 
	 * @param params
	 * @param len
	 * @return
	 */
	public static String[] extendArray(String[] list, int len) {
		if(len < 0) return null;
		String[] result = new String[len];
		if(list == null) return result;
		int cpy = Math.min(len, list.length);
		for(int k=0; k<cpy; k++){
			result[k] = list[k];
		}
		return result;
	}
	/**
	 * Concatenates with a dot
	 * @param version
	 * @return
	 */
	public static String getVersion(int[] version) {
		return Util.concat(version, ".", null);
	}
	/**
	 * Assumes parameter is a dot separated sequence of ints
	 * @param version
	 * @return
	 */
	public static int[] getVersion(String version){
		String parsed_version[];
		if(version == null){
			if(ClientUpdates.DEBUG)System.out.println("ClientUpdates newer: start v_server null");
			return null;
		}
		parsed_version = version.split(Pattern.quote("."));
		if(parsed_version.length<3){
			if(ClientUpdates.DEBUG)System.out.println("ClientUpdates newer: start v_server < 3 : "+parsed_version.length+" "+concat(parsed_version, "--"));
			return null;
		}
		if(ClientUpdates.DEBUG)System.out.println("ClientUpdates newer: server["+concat(parsed_version,",")+"]");
		int int_version[] = new int[3];
		for(int k=0; k<3; k++){
			try{
				int_version[k] = Integer.parseInt(parsed_version[k]);
			}catch(Exception e){
				int_version[k] = prefixNumber(parsed_version[k]);
			}
		}
		return int_version;
	}
	/**
		 * 
		 * @param version_server
		 * @param version_local
		 * @return : true if server is newer than local
		 */
		public static boolean isVersionNewer(String version_server, String version_local) {
			if (DEBUG)System.out.println("Util newer: start server="+version_server+" vs local="+version_local);
			boolean result = false;
			String v_server[];
			String v_local[];
			if (version_server == null) {
				if (DEBUG)System.out.println("Util newer: start v_server null");
				return false;
			}
			v_server = version_server.split(Pattern.quote("."));
			if (v_server.length < 3) {
				if (DEBUG) System.out.println("Util newer: start v_server < 3 : "+v_server.length+" "+concat(v_server, "--"));
				return false;
			}
			if (version_local == null) {
				if (DEBUG) System.out.println("Util newer:  local null");
				return true;
			}
			v_local = version_local.split(Pattern.quote("."));
			if (v_local.length < 3) {
				if (DEBUG) System.out.println("Util newer:  v_local < 3");
				return true;
			}
			if (DEBUG) System.out.println("Util newer: server["+concat(v_server,",")+"] ? v_local ["+concat(v_local,",")+"]");
			result = false;
			int i_server[] = new int[3];
			int i_local[] = new int[3];
			for (int k = 0; k < 3; k ++) {
				try {
					i_server[k] = Integer.parseInt(v_server[k]);
					i_local[k] = Integer.parseInt(v_local[k]);
					if (i_server[k] < i_local[k]) return false;
					if (i_server[k] > i_local[k]) return true;
				} catch(Exception e){
					int s = prefixNumber(v_server[k]);
					int l = prefixNumber(v_local[k]);
					if(s<l) return false;
					if(s>l) return true;
					if(v_server[k].compareTo(v_local[k]) > 0) return true; 
					if(v_server[k].compareTo(v_local[k]) < 0) return false; 
				}
			}
			if (DEBUG) System.out.println("Util: newer: "+result);
			return result;
		}
		/**
	 * Get a row in a table (returned by select)
	 * @param table
	 * @param i
	 * @return
	 */
	public static ArrayList<Object> getColumnInDBselectResult(ArrayList<ArrayList<Object>> table,
			int i) {
		ArrayList<Object> result = new ArrayList<Object>();
		for(ArrayList<Object> r: table){
			result.add(r.get(i));
		}
		return result;
	}
	public static byte[] cpu_to_16le(int i) {
		byte r[] = new byte[2];
		r[0] = (byte) (i & 0xFF);
		r[1] = (byte) ((i>>8) & 0xFF);
		return r;
	}
	public static byte[] cpu_to_16be(int i, byte[] r, int off) {
		r[off+1] = (byte) (i & 0xFF);
		r[off] = (byte) ((i>>8) & 0xFF);
		return r;
	}
	/**
	 * Returns true on success
	 * @param buf
	 * @param file
	 * @return
	 */
	public static boolean writeFile(byte[] buf, File file) {
		try {
			FileOutputStream o = new FileOutputStream(file);
			o.write(buf);
			o.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static int bytePack(byte[] _uncompressed, int initial_code_size,
			byte[] bs) {
		int M = 0;
		for(int i=0; i<bs.length; i++) {
			M = 8/initial_code_size;
			if(i==bs.length-1) M = _uncompressed.length % M;
			bs[i] = 0;
			for(int k=0; k<M; k++){
				bs[i] |= _uncompressed[i]<<(k*initial_code_size);
			}
		}
		return M*initial_code_size;
	}
	/**
	 * Array of characters is converted to a string
	 * @param data
	 * @return
	 */
	public static String byteToString(byte[] data) {
		if(data==null) return null;
		String result = "";
		for (int k=0; k<data.length; k++) {
			result += Character.toString((char)(data[k]));
		}
		return result;
	}
	public static int le16_to_cpu(byte[] b) {
		return le16_to_cpu(b, 0);
	}
	public static int le16_to_cpu(byte[] b, int off) {
		int r;
		r = (byte_to_uint(b[off+1])<<8) + byte_to_uint(b[off]);
		return r;
	}
	public static int be16_to_cpu(byte[] b, int off) {
		int r;
		r = (byte_to_uint(b[off])<<8) + byte_to_uint(b[off+1]);
		return r;
	}
	public static void cpu_to_be32(long val, byte[] b, int off) {
		int i = (int) ((val >> 16) & 0x00FFFF);
		Util.cpu_to_16be(i, b, off);
		int j = (int) ((val) & 0x00FFFF);
		Util.cpu_to_16be(j, b, off+2);
	}
	public static void cpu_to_le32(long val, byte[] b, int off) {
		int i = (int) ((val >> 16) & 0x00FFFF);
		Util.cpu_to_16be(i, b, off+2);
		int j = (int) ((val) & 0x00FFFF);
		Util.cpu_to_16be(j, b, off);
	}
	public static long be32_to_cpu(byte[] b, int off) {
		long r = be16_to_cpu(b, off);
		return (r<<16)+be16_to_cpu(b, off+2);
	}
	public static long le32_to_cpu(byte[] b, int off) {
		long r = le16_to_cpu(b, off+2);
		return (r<<16)+le16_to_cpu(b, off);
	}
	/**
	 * same as byte_to_uint
	 * @param b
	 * @return
	 */
    public static int positive(byte b) {
    	if(b>=0) return b;
		return b+256;
	}
    /**
     * same as (positive)
     * @param b
     * @return
     */
	public static int byte_to_uint(byte b) {
		return (b+256)%256;
	}
	public static String byteToHex(byte b) {
		return ""+byteToHex(new byte[]{b});
	}
	/**
	 * Compute int from hash-sized array of bytes (not hashed here)
	 * @param hash
	 * @return
	 */
	public static BigInteger bigIntegerFromUnsignedBytes(byte[] hash) {
		byte[] _val = new byte[hash.length+1];
		copyBytes(_val, 1, hash, hash.length, 0);
		return new BigInteger(_val);
	}
	public static String cleanInnerSpaces(String s) {
		return Util.concat(s.split(" "), "");
	}
	public static String toString16(BigInteger i) {
		if (i == null) return null;
		return i.toString(16);
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
	public final static BigInteger BN128 = new BigInteger("128");
	public final static BigInteger BN127 = new BigInteger("127");
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
	 * This is based on bit shifting
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
	 * Returns bigendian base 128 (Util.BN128), less efficient
	 * @param i
	 * @return
	 */
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
			result = result.shiftLeft(7).or(new BigInteger(""+(b128[k] & 0x7f)));
			k++;
		}
		if (k < limit) {
			if ((b128[k] & 0x80) != 0) {if (_DEBUG) System.out.println("Util: fromBase_128: last byte > 127"); Util.printCallPath("");}
			result = result.shiftLeft(7).or(new BigInteger(""+(b128[k]))); // here  & 0x7f would be redundant
		}
		return result;
	}
	public static boolean isUpperCase(byte b) {
		return (b >= 'A') && (b <= 'Z');
	}
	public static boolean isLowerCase(byte b) {
		return (b >= 'a') && (b <= 'z');
	}
	public static boolean isAsciiAlpha(byte b) {
		return isUpperCase(b) || isLowerCase(b);
	}
	public static byte[] readAll(File f) {
		int cnt;
		BufferedInputStream br;
		try {
			br = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
		byte data[] = new byte[(int)f.length()];
		try {
			cnt = br.read(data);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}
	 /**
	 * Return prefix string of a separator, null if sep does not exist in the str, else return prefix string. 
	 * @param str : string
	 ** @param sep : string
	 * @return prefix string
	 */
	 public static String getPrefix(String str, String sep){
    	if (str.indexOf(sep) == -1) return null;
    	return str.substring(0,str.indexOf(sep));
    }
		/**
		 * Read the amount of data needed to contain the whole ASN message.
		 * @param is
		 * @param PREFERRED_BUFFER
		 * @param MAX_BUFFER
		 * @return
		 * @throws Exception
		 */
		public static byte[] readASN1Message(InputStream is, int PREFERRED_BUFFER, int MAX_BUFFER) throws Exception {
			int sys_result;
			byte[] buffer = new byte[PREFERRED_BUFFER];
			int len;
			sys_result = is.read(buffer);
			if (sys_result < 0) throw new Exception("Peer Connection Close!");
			len = sys_result;
			Decoder dec_da = new Decoder(buffer);
			int req_len = dec_da.objectLen();
			if (req_len > MAX_BUFFER)  throw new Exception("Unacceptable package!");
			if (req_len < 0) throw new Exception("Not enough bytes received in first package!");
			if (req_len > len) {
				if (dec_da.objectLen() > PREFERRED_BUFFER) {
					byte[] old_buffer = buffer;
					buffer = new byte[req_len];
					Encoder.copyBytes(buffer, 0, old_buffer, len, 0);
				}
				do {
					sys_result = is.read(buffer, len, req_len - len);
					if (sys_result > 0) len += sys_result;
					if (DEBUG) System.out.println("Util: readASN1Message: len="+len+" result="+sys_result+"/"+req_len);
				} while ((sys_result >= 0) && (len < req_len));
				if (req_len != len) throw new Exception("Not enough bytes received!");
			}
			return buffer;
		}
		public static byte[] readASN1Message(InputStream is, byte[] buffer) throws Exception {
			int sys_result;
			int len;
			sys_result = is.read(buffer);
			if (sys_result < 0) throw new Exception("Peer Connection Close!");
			len = sys_result;
			Decoder dec_da = new Decoder(buffer);
			int req_len = dec_da.objectLen();
			if (req_len > buffer.length)  throw new Exception("Unacceptable package!");
			if (req_len < 0) throw new Exception("Not enough bytes received in first package!");
			if (req_len > len) {
				do {
					sys_result = is.read(buffer, len, req_len - len);
					if (sys_result > 0) len += sys_result;
					if (DEBUG) System.out.println("Util: readASN1Message: len="+len+" result="+sys_result+"/"+req_len);
				} while ((sys_result >= 0) && (len < req_len));
				if (len < req_len) throw new Exception("Not enough bytes received!");
			}
			return buffer;
		}
		public static Decoder readASN1Decoder(InputStream is, int PREFERRED_BUFFER, int MAX_BUFFER) throws Exception {
			int sys_result;
			byte[] buffer = new byte[PREFERRED_BUFFER];
			int len;
			sys_result = is.read(buffer);
			if (sys_result < 0) throw new Exception("Peer Connection Close!");
			len = sys_result;
			Decoder dec_da = new Decoder(buffer);
			int req_len = dec_da.objectLen();
			if (req_len > MAX_BUFFER)  throw new Exception("Unacceptable package!");
			if (req_len < 0) throw new Exception("Not enough bytes received in first package! #"+sys_result);
			if (req_len > len) {
				if (dec_da.objectLen() > PREFERRED_BUFFER) {
					byte[] old_buffer = buffer;
					buffer = new byte[req_len];
					Encoder.copyBytes(buffer, 0, old_buffer, len, 0);
				}
				do {
					sys_result = is.read(buffer, len, req_len - len);
					if (sys_result > 0) len += sys_result;
					if (DEBUG) System.out.println("Util: readASN1Message: len="+len+" result="+sys_result+"/"+req_len);
				} while ((sys_result >= 0) && (len < req_len));
				if (req_len != len) throw new Exception("Not enough bytes received!");
				dec_da = new Decoder(buffer);
			}
			return dec_da;
		}
		public final static String DEFAULT_NONULL_STRING = "DEFAULT";
		/**
		 * for null returns DEFAULT_NONULL_STRING. Otherwise add a " to the beginning of the name.
		 * Warns if a " was already there.
		 * 
		 * Returns a non-null string
		 * @param o
		 * @return
		 */
		public static String getStringNonNullUnique(Object o) {
			if (o == null) return DEFAULT_NONULL_STRING;
			if (o.toString().startsWith("\"")) Util.printCallPath(o.toString());
			return "\""+o.toString();
		}
		/**
		 * Transforms DEFAULT_NONULL_STRING in null and removes " from the beginning of other strings
		 * @param o
		 * @return
		 */
		public static String getStringNullUnique(String o) {
			if (o == null) return null;
			if (DEFAULT_NONULL_STRING.equals(o)) return null;
			if (! o.startsWith("\"")) {
				Util.printCallPath(o.toString());
				return o;
			}
			return o.substring(1);
		}
		public static void dumpThreads() {
			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
			for ( Thread t : threadSet) {
				System.out.println(">> "+t+" dem="+t.isDaemon()+" st=\n\t"+concat(t.getStackTrace(),"\n\t"));
			}
		}
		/**
		 * Generate a digest from the current state of an encoder (for tests)
		 * @param e
		 * @return
		 */
		public static String hashEncoder(Encoder e) {
			if (e == null) return null;
			byte[] b = e.getBytes();
			return getHash(b, Cipher.MD5);
		}
		/**
		 * Returns the index of the first match
		 * @param old_ad
		 * @param d
		 * @return
		 */
		public static int contains(Address[] old_ad, DirectoryAddress d) {
			Address a;
			for (int k = 0; k < old_ad.length; k++) {
				a = old_ad[k];
				if (
				Util.equalStrings_null_or_not(a.domain, d.domain)
				&&
				(a.tcp_port == d.tcp_port)
				)
					return k;
			}
			return -1;
		}
		/**
		 * Returns the index of the first match
		 * @param old_ad
		 * @param d
		 * @return
		 */
		public static int contains(DirectoryAddress[] old_ad, DirectoryAddress d) {
			DirectoryAddress a;
			for (int k = 0; k < old_ad.length; k++) {
				a = old_ad[k];
				if (
				Util.equalStrings_null_or_not(a.domain, d.domain)
				&&
				(a.tcp_port == d.tcp_port)
				)
				return k;
			}
			return -1;
		}
		/**
		 * This should be used for command lines that cannot be broken, such as "netsh ip .."
		 * @param disconnect
		 * @return
		 * @throws IOException
		 */
		public static BufferedReader getProcessOutputConcatParams(String[] disconnect, Object ctx) throws IOException {
			return Util.getProcessOutput(new String[]{concat(disconnect, " ")}, true, ctx);
		}
		public static BufferedReader getProcessOutput(String[] cmd_names, String[] env, File dir, Object ctx) throws IOException {
			return Util.getProcessOutput(cmd_names, false, env, dir, ctx);
		}
		public static BufferedReader getProcessOutput(String[] cmd_names, Object ctx) throws IOException {
			return Util.getProcessOutput(cmd_names, false, ctx);
		}
		public static BufferedReader getProcessOutput(String[] cmd_names, boolean concats, Object ctx) throws IOException {
			return Util.getProcessOutput(cmd_names, concats, null, null, ctx);
		}
		public static BufferedReader getProcessOutput(String[] cmd_names, boolean concats, String[] env, File dir, Object ctx) throws IOException {
			String cmd = concat(cmd_names, "\" \"");
			String result = null;
			synchronized(scripts_monitor){
				if (DEBUG) System.out.println("Util:getProcessOutput: Calling: \""+cmd+"\" with concats="+concats);
				if (ctx != null) Application_GUI.updateProgress(ctx, "Start: \""+cmd+"\"");
				Process crtScriptProcess;
				if (concats) crtScriptProcess =  Runtime.getRuntime().exec(cmd_names[0]);
				else crtScriptProcess = Runtime.getRuntime().exec(cmd_names, env, dir);
				BufferedReader bri1 = new BufferedReader (new InputStreamReader(crtScriptProcess.getInputStream()));
				for(;;) {
					String line = bri1.readLine();
					if(line == null) break;
					if(result == null) result = line;
					else result += "\n"+line;
				}
				bri1.close();
				crtScriptProcess.destroy();
				int exit = INTERRUPTED;
				try {
					crtScriptProcess.waitFor();
					exit = crtScriptProcess.exitValue();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (0 != exit) {
					if(DD.SCRIPTS_ERRORS_WARNING)
						Application_GUI.warning(__("Process:"+"\n"+cmd+"\n"+__("exits with:")+exit), __("Process exit error"));
					if(_DEBUG)System.out.println("Util:getProcessOutput: cmd="+ cmd + " in dir="+dir +" exit with: "+exit+" output=\n"+result+"\n*******");
				}
				if(DEBUG)System.out.println("Util:getProcessOutput: output=\n"+result+"\n*******");
				if (ctx != null) Application_GUI.updateProgress(ctx, "Done: \""+cmd+"\"");
			}
			if (result == null) {
				if (DEBUG) System.out.println("Util:getProcessOutput: output null result\n*******");
				return null;
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes())));
			return r;
		}
		/**
		 * Dumping the content of the field "D_Peer.peer_contacts", which is displayed in the debugging "peer contacts" widgets
		 * @param prefix
		 */
		public static void printPeerContacts(String prefix) {
			out.println("Util: printPeerContacts: " + prefix);
			prefix += "\t";
			for ( String pc_key : D_Peer.peer_contacts.keySet()) {
				out.println(prefix+ "available pc_key="+ pc_key);
				Hashtable<String, Hashtable<String, Hashtable<String,String>>> _otpc =  D_Peer.peer_contacts.get(pc_key);
				for (String _ktpc : _otpc.keySet()) {
					out.println(prefix+ "av instance=\t"+ _ktpc);
					Hashtable<String, Hashtable<String,String>> _tpc =  _otpc.get(_ktpc);
					for ( String tpc_key : _tpc.keySet()) {
						out.println(prefix+ "available tpc_key=\t "+ tpc_key);
						Hashtable<String,String> _tpce =  _tpc.get(tpc_key);
						for ( String tpce_key : _tpce.keySet()) {
							out.println(prefix+ "available tpce_subkey=\t\t"+ tpce_key+" -> "+_tpce.get(tpce_key));
						}
					}
				}
			}
		}
		public static String concat(String prefix,
				Hashtable<String, ArrayList<Address>> adr_addresses,
				String sep, String def ) {
			String r = "";
			for (String i: adr_addresses.keySet()) {
				r += prefix + ": "+Util.concat(adr_addresses.get(i), sep, def) + "\n"; 
			}
			return r;
		}
		/**
		 * Returns true if second is null or first is after second (in time)
		 * @param first
		 * @param second
		 * @return
		 */
		public static boolean newerDateStr(String first,
				String second) {
			if (second == null) return true;
			if (first == null) return false;
			return first.compareTo(second) > 0;
		}
		/**
		 * Returns true if second is null or first is after second (in time)
		 * @param first
		 * @param second
		 * @return
		 */
		public static boolean newerDate(Calendar first,
				Calendar second) {
			if (second == null) return true;
			if (first == null) return false;
			return first.compareTo(second) > 0;
		}
		public static String getNonNullDate(String creationDate) {
			if (creationDate != null) return creationDate;
			return DD.EMPTYDATE;
		}
		public static String concat(Hashtable<String, RequestData> sr,
				String sep) {
			String result="";
			for (String a: sr.keySet()) {
				result += "["+a+sep+sr.get(a)+"]";
			}
			return result;
		}
		public static String readAll(URL url) {
			if (url == null) return null;
			try {
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(url.openStream()));
				return readAll(in);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		/**
		 * Removes spaces and new lines.
		 * @param addressASN1B64
		 * @return
		 */
		public static String B64Join(String addressASN1B64) {
			String result;
			String []chunks = addressASN1B64.split(Pattern.quote(" "));
			result = Util.concat(chunks, "");
			chunks = result.split(Pattern.quote("\n"));
			result = Util.concat(chunks, "");
			return result;
		}
		/**
		 * Adds spaces or new lines.
		 * @param addressASN1B64
		 * @param size
		 * @param sep
		 * @return
		 */
		public static String B64Split(String addressASN1B64, int size, String sep) {
			ArrayList<String> results = new ArrayList<String>();
			String result;
			int beginIndex = 0;
			if (size <= 0) return addressASN1B64;
			if (addressASN1B64 == null) return addressASN1B64;
			if (addressASN1B64.length() <= size) return addressASN1B64;
			do {
				int endIndex = Math.min(size + beginIndex, addressASN1B64.length());
				results.add(addressASN1B64.substring(beginIndex, endIndex));
				beginIndex += size;
			} while (beginIndex < addressASN1B64.length());
			result = Util.concat(results, sep, null);
			return result;
		}
		public static final int CALENDAR_SPACED = 1;
		public static final int CALENDAR_TIME_ONLY = 2;
		public static final int CALENDAR_DAY_ONLY = 3;
		public static final int CALENDAR_DASHED = 4;
		/**
		 *  
		 * @param value
		 * @param mode2
		 * values in: <p>
		public static final int CALENDAR_SPACED = 1; <br>
		public static final int CALENDAR_TIME_ONLY = 2; 
		public static final int CALENDAR_DAY_ONLY = 3; 
		public static final int CALENDAR_DASHED = 4;
		 * @return
		 */
		public static String renderNicely(Calendar value, int mode2) {
			if (value == null) return null;
			String result;
			switch (mode2) {
			case CALENDAR_SPACED:
				result = String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tLZ",value);	
				return result;
			case CALENDAR_TIME_ONLY:
				result = String.format("%1$tH:%1$tM:%1$tS.%1$tLZ",value);	
				return result;
			case CALENDAR_DAY_ONLY:
				result = String.format("%1$tY/%1$tm/%1$td",value);	
				return result;
			case CALENDAR_DASHED:
				result = String.format("%1$tY-%1$tm-%1$td %1$tH%1$tM",value);	
				return result;
			default:
				return Encoder.getGeneralizedTime(value);
			}
		}
		/**
		 * 
		 * @param date
		 * @return 2015-13-13 1233 (In the format of the input, i.e. potentially GMT!)
		 */
		public static String renderNicely(String date) {
			return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + "-" + date.substring(8, 12);
		}
		/**
		 * Transforms in binary, 8 bits per byte
		 * @param bytes
		 * @return
		 */
		public static String getBitString( byte[] bytes ) {
			String s = "";
			for( byte b : bytes ) {
				s += Integer.toBinaryString( ( b & 255 | 256 ) ).substring( 1 );
			}
			return s;
		}
		/**
		 * Transforms parameter into binary
		 * @param integer
		 * @param useShort
		 * @return
		 */
		public static String getBitString( int integer, boolean useShort ) {
			String s = "";
			if( useShort ) {
				s += Integer.toBinaryString( 0xFFFF & integer );
			}
			else {
				s += Integer.toBinaryString( integer );
			}
			return s;
		}
}
class GetHostName extends net.ddp2p.common.util.DDP2P_ServiceThread {
	String hostName = null;
	InetAddress domain;
	InetSocketAddress sock_addr;
	GetHostName(InetAddress domain, InetSocketAddress sock_addr) {
		super("GetHostName: "+domain+"::"+sock_addr, true);
		this.domain = domain;
		this.sock_addr = sock_addr;
		this.start();
	}
	public void _run(){
		hostName = "";
		if (domain != null) {
			hostName = domain.getHostName();
		} else
			if (sock_addr != null) {
				hostName = sock_addr.getHostName();
			}
		synchronized(this){
			this.notifyAll();
		}
	}
}
class GetHostIA extends net.ddp2p.common.util.DDP2P_ServiceThread {
	static final boolean DEBUG = false;
	public boolean started = false;
	String domain = null;
	InetAddress hostIA = null;
	GetHostIA(String domain) {
		super("GetHostIA: "+domain, true);
		this.domain = domain;
		this.start();
	}
	public void _run(){
		hostIA = null;
		if(domain!=null) {
			try {
				started = true;
				hostIA = InetAddress.getByName(domain);
			} catch (UnknownHostException e) {
				if(DEBUG)e.printStackTrace();
			} catch (Exception e){
				if(DEBUG)e.printStackTrace();
			}
		}
		synchronized(this){
			this.notifyAll();
		}
	}
}

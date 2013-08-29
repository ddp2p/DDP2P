/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
 package util;


import static java.lang.System.out;

import handling_wb.BroadcastQueueRequested;
import handling_wb.BroadcastQueueRequested.Received_Interest_Ad;
import hds.OrgInfo;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.util.ArrayList;
import java.util.Calendar;
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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;
import javax.swing.text.View;

import wireless.Detect_interface;

import util.P2PDDSQLException;

import config.Application;
import config.DD;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import data.D_PeerOrgs;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
public class Util {
    private static ResourceBundle myResources = Util.getResourceBundle();
    private static final JLabel resizer = new JLabel();
	private static final byte[] HEX_R = init_HEX_R();
	private static final int DD_PRIME_SIZE = 2048;
	private static final int DD_PRIME_CERTAINTY = 200;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int MAX_DUMP = 20;
	public static final int MAX_UPDATE_DUMP = 400;
	static Random rnd = new Random(); // for simulation
	
    public static String usedCipherGenkey = Cipher.RSA;
    public static String usedMDGenkey = Cipher.SHA256;


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
			if(result == null) result = key+key_sep+data;
			else result += sep + key+key_sep+data;
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
	public static String concat(byte[] array, String sep, String def) {
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=array[0]+"";
		for(int k=1; k<array.length; k++) result = result + sep + array[k];
		return result;
	}
    public static <T> String nullDiscrimArray(T o[], String sep){
    	if(o==null) return "null";
    	return "#["+o.length+"]=\""+Util.concat(o, sep)+"\"";
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
	public static <T> String concatSummary(T[] array, String sep, String def) {
		if ((array == null)) return def;
		if ((array.length == 0)) return def;
		String result=((array[0]==null)?"":((Summary)array[0]).toSummaryString());
		for(int k=1; k<array.length; k++) result = result+ sep +((array[k]==null)?"":((Summary)array[k]).toSummaryString());
		return result;
	}
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
				BigInteger bi;//new BigInteger(comps[k]);
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
		//System.err.println("will copy bytes off "+offset);
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
	public static int ceil(double a){
		return (int)Math.round(Math.ceil(a));
	}
	public static byte[] getBytes(BufferedImage bi, int start, int length){
		byte[]useful = new byte[length];
		//ColorModel cm = bi.getColorModel();
		int height=bi.getHeight();
		int width=bi.getWidth();
		int stegoBytes = length;
		int lastbyte = start+length;
        for(int d=0,k=start; k<lastbyte; ){
        	//System.err.println(d+"/"+stegoBytes+" of max "+length);
        	int p = k/3;
        	int w = p % width; //FIREFOX
        	//int w = width-1 - (p % width);
        	int h = height - 1 - ((p-w) / height);// FIREFOX
        	//int h = height-1 - ((p-(p%width)) / height);
        	int pix=bi.getRGB(w,h);
        	//System.err.println("getBytes: "+pix);
        	if(k%3 == 0){
        		useful[d++]= (byte)((pix>>16) & 0xff);k++; //(byte)cm.getBlue(pix);k++;
        	}else if(k%3 == 1){
        		useful[d++]= (byte)((pix>>8) & 0xff);k++; //(byte)cm.getGreen(pix);k++;
        	}else if(k%3 == 2){
        		useful[d++]= (byte)((pix) & 0xff);k++; //(byte)cm.getRed(pix);k++;
        	}
        	//System.err.println(d+"/"+stegoBytes);
        	if(d>stegoBytes) break;
        	/*
            			useful[d++]=(byte)cm.getGreen(pix);k++;
               			if(d>stegoBytes) break;
            			useful[d++]=(byte)cm.getRed(pix);k++;
               			if(d>stegoBytes) break;
              */
        }
    	//System.err.println("Done getBytes");
		return useful;
	}
	public static byte[] getPixBytes(BufferedImage bi, int x, int y){
		//System.err.println("will get pix "+x);
		int pix=bi.getRGB(x,y);
		byte[]useful = new byte[4];
		/*
		System.err.println("will get pix "+1);
		ColorModel cm = bi.getColorModel();
		System.err.println("will get pix "+2);
		System.err.println("will get pix 3 "+pix+" cm="+cm);
		useful[0]=(byte)cm.getRed(pix);
		System.err.println("will get pix 3b ");
		useful[1]=(byte)cm.getGreen(pix);
		System.err.println("will get pix "+4);
		useful[2]=(byte)cm.getBlue(pix);
		System.err.println("will get pix "+5);
		useful[3]=(byte)cm.getAlpha(pix);
		System.err.println("will copy Bytes "+y);
		//Util.copyBytes(useful, 4, pix);
		 */
		Util.copyBytes(useful, 0, pix);
		//System.err.println("Extracted: "+pix+" ... "+Util.byteToHex(useful, " "));
		return useful;
	}
	public static short extBytes(byte[] src, int offset, short int16){
		int16=0;
		int16 |= (src[offset+1] & 0xff);
		int16 <<= 8;
		int16 |= (src[offset+0] & 0xff);
		//System.out.println("Extract from: "+Util.byteToHex(src, " ")+" to: "+int16);
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
		//System.out.println("Extract from: "+Util.byteToHex(src, " ")+" to: "+int32);
		return int32;
	}
	public static void copyBytes(byte[] results, int offset, byte[]src, int length){
		copyBytes(results, offset, src,length,0);
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
    public static ResourceBundle getResourceBundle(){
    	try{
    		return ResourceBundle.getBundle("DebateDecide");
    	}catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }
	public static java.awt.Dimension getPreferredSize(String html, boolean width, int prefSize) {
		resizer.setText(html);
		View view = (View) resizer.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
		view.setSize(width?prefSize:0, width?0:prefSize);
		float w = view.getPreferredSpan(View.X_AXIS);
		float h = view.getPreferredSpan(View.Y_AXIS);
		return new java.awt.Dimension((int) Math.ceil(w),(int) Math.ceil(h));
	}
    public static String _(String s) {
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
    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(String path,
				     String description) {
	return new ImageIcon(path, description);
	/*
	java.net.URL imgURL = getClass().getResource(path);
	if (imgURL != null) {
	    return new ImageIcon(imgURL, description);
	} else {
	    System.err.println("Couldn't find file: " + path);
	    return null;
	}
	*/
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
	public static float fval(Object obj, float _default){
		if(obj == null) return _default;
		try{
			return Float.parseFloat(obj.toString());
		}catch(Exception e){
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
		if(obj == null) return _default;
		return Integer.parseInt(obj.toString());
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
     * Nice printing when o is null versus when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrim(Object o){
    	if(o==null) return "null";
    	return "\""+o+"\"";
    }
    /**
     * Nice printing when o is null versus when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrimArray(String o[]){
    	if(o==null) return "null";
    	return "["+o.length+"]\""+Util.concat(o, ":")+"\"";
    }
    /**
     * Nice printing when o is null versus when it is "null"
     * @param o
     * @return
     */
    public static String nullDiscrim(Object o, String descr){
    	if(o==null) return "null";
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
    		//digest.update(time.toString().getBytes());
    		md5sum = digest.digest();
    	}catch(Exception e){
    		result = type+":"+body+":"+time;
    		if(DEBUG)System.err.println("DONE Util.getGlobalID: result="+result);
    		return result;
    	}
    	result = Util.stringSignatureFromByte(md5sum);//byteToHex(md5sum);
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
    public static Cipher getKeyedGlobalID (String type, String body) {
    	Cipher suit = ciphersuits.Cipher.getCipher(usedCipherGenkey, usedMDGenkey,type+"://"+body);
    	return suit;
    }
	/**
	 * Returns Public Key for key (N,e of RSA)
	 * Used to be 
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDPK(byte[] pk) {
		return Util.stringSignatureFromByte(pk); //.byteToHex(pk);
	}
	/**
	 * Returns Public Key for key (N,e of RSA)
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
		return Util.stringSignatureFromByte(pk); //.byteToHex(pk);
	}
	public static String getKeyedIDPK(PK _pk) {
		byte[] pk = _pk.encode();
		return Util.stringSignatureFromByte(pk); //.byteToHex(pk);
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
		byte[] pk = _pk.encode();
		return pk;
	}
	/**
	 * Get has hash of the public key
	 * @param keys
	 * @return
	 */
	//@Deprecated
	public static String getKeyedIDPKhash(Cipher keys) {
		if(DEBUG) System.err.println("BEGIN Util.getKeyedIDPK: keys="+keys);
		PK _pk = keys.getPK();
		if(_pk==null){
			System.err.println("BEGIN Util.getKeyedIDPK: keys has no PK");
			return null;
		}
		return Util.getGIDhash(Util.getKeyedIDPK(keys));
	}
	/**
	 * Returns Secret Key for key 
	 * @param keys 
	 * @return
	 */
	public static String getKeyedIDSK(Cipher keys) {
		byte[] sk = keys.getSK().encode();
		return Util.stringSignatureFromByte(sk); //.byteToHex(sk);
	}
	/**
	 * Get the type of cipher and message digest, separated by a :: (Cipher.separator)
	 * @param keys
	 * @return
	 */
	public static String getKeyedIDType(Cipher keys) {
		return keys.getType();//"RSA";
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
		String _pk = table.peer.getGlobalPeerID(local_peer_ID);
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
			Application.warning(_("Failed to retrieve a publick key for verifying"), _("No public key!"));
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
		byte[]message = data.encode();
		if(DEBUG)System.out.println("Util:verifySign: msg["+message.length+"]"+Util.byteToHexDump(message));
		return senderPK.verify_unpad_hash(signature, message);
		//return true;
	}
	/**
	 * Verify the data signed based on the public key senderPK.
	 * @param message
	 * @param senderPK
	 * @param signature
	 * @return
	 */
	public static boolean verifySign(byte[] message, PK senderPK, byte[] signature) {
		//boolean DEBUG = true;
		boolean result;
		if(DEBUG)System.err.println("Util: verifySign: start ");
		if (senderPK==null){
			System.err.println("Util: verifySign: null PK ");
			Util.printCallPath("verifying");
			return false;
		}
		try{
			if(DEBUG)System.err.println("Util: verifySign: msg["+message.length+"]="+Util.byteToHexDump(message, ":")+"   \n"+Util.getGID_as_Hash(message));
			if(DEBUG)System.err.println("Util: verifySign: sign["+signature.length+"]="+Util.byteToHexDump(signature, ":")+"   \n"+Util.getGID_as_Hash(signature));
			result = senderPK.verify_unpad_hash(signature, message);
		}catch(Exception e){
			//if(DEBUG)
				e.printStackTrace();
			return false;
		}
		if(!result) {
			Util.printCallPath("verifying failed");
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
		return sign(msg, DD.getMyPeerSK());
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
		if(key==null){
			System.err.println("Util:sig:signing with empty key");
			Util.printCallPath("Empty key");
			return new byte[0];
		}
		//if(_DEBUG) System.out.println("Util:sign: msg["+msg.length+"]="+Util.byteToHexDump(msg));
		if(DEBUG)System.err.println("Util: sign: msg["+msg.length+"]="+Util.byteToHexDump(msg, ":")+"   \n"+Util.getGID_as_Hash(msg));
		byte[] signature = key.sign_pad_hash(msg);
		if(DEBUG)System.err.println("Util: sign: sign["+signature.length+"]="+Util.byteToHexDump(signature, ":")+"   \n"+Util.getGID_as_Hash(signature));
		return signature; 
	}

    /**
     * Used to find the main window of a component, for showing alerts
     * @param c
     * @return
     */
    public static Window findWindow(Component c) {
    	    if (c == null) {
    	        return JOptionPane.getRootFrame();
    	    } else if (c instanceof Window) {
    	        return (Window) c;
    	    } else {
    	        return findWindow(c.getParent());
    	    }
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
		//System.out.println("getCalendar "+gdate+" into "+date);
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
	//return random integer 0 or 1.
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
		return alg+DD.APP_ID_HASH_SEP+Util.stringSignatureFromByte(md); //byteToHex(md);
	}
	/**
	 * Return the hash of msg with the algorithm alg
	 * @param msg
	 * @param alg for example Cipher.MD5, DD.APP_OTHER_HASH
	 * @return
	 */
	public static byte[] simple_hash(byte[] msg, String hash_alg) {
		return simple_hash(msg,0,msg.length,hash_alg);
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
		int remaining = length;
		int crt;
		if(length > stg.length) throw new IOException("Buffer too small!");
		do{
			crt = in.read(stg, got, remaining);
			if(crt < 0) return got;
			got += crt;
			remaining -= crt;
		}while(remaining > 0);
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
			//if(!signature.startsWith("")) return _byteSignatureFromString(signature);
			char[]s = signature.toCharArray();
			result = Base64Coder.decode(s, 0, s.length);
			//result = Base64Coder.decode(s, 1, s.length-1);
			//result = Base64Coder.decode(signature.substring(1));
		}catch(Exception e){
			if(verbose) {
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
		byte[] idbytes = Util.byteSignatureFromString(globalID, verbose);//Util.hexToBytes(idstrings);
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
	public static SK getStoredSK(String constGID) {
		return getStoredSK(constGID,null);
	}
	public static SK getStoredSK(String public_gID, String id_hash) {
		if(public_gID == null) return getStoredSKByHash(id_hash);
		String sql = " SELECT "+table.key.secret_key+
				" FROM "+table.key.TNAME+
				" WHERE "+table.key.public_key+"= ?;";
		java.util.ArrayList<java.util.ArrayList<Object>> o;
		try {
			o = Application.db.select(sql, new String[]{public_gID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(o.size()<1)return null;
		String _sk = Util.getString(o.get(0).get(0));
		//if(DEBUG) System.out.println("Util:getStoredKey: Got secret key: "+_sk);
		return Cipher.getSK(_sk);
	}
	private static SK getStoredSKByHash(String id_hash) {
		if(id_hash == null) return null;
		String sql = " SELECT "+table.key.secret_key+
				" FROM "+table.key.TNAME+
				" WHERE "+table.key.ID_hash+"= ?;";
		java.util.ArrayList<java.util.ArrayList<Object>> o;
		try {
			o = Application.db.select(sql, new String[]{id_hash}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(o.size()<1)return null;
		return Cipher.getSK(Util.getString(o.get(0).get(0)));
	}
	public static String concatPeerOrgs(data.D_PeerOrgs[] array) {
		String def = "";
		String sep = ":";
		if ((array == null) ) return def;
		if ((array.length == 0)) return def;
		String result=Util.stringSignatureFromByte(array[0].getEncoder().getBytes());
		for(int k=1; k<array.length; k++) result = result + sep + Util.stringSignatureFromByte(array[k].getEncoder().getBytes());
		return result;
	}
	public static String concatPeerOrgsTrimmed(data.D_PeerOrgs[] array) {
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
	public static data.D_PeerOrgs[] parsePeerOrgs(String pos) {
		String def = "";
		String sep = ":";
		if(pos == null) return null;
		if(def.equals(pos)) return null;
		String[] po = pos.split(Pattern.quote(sep));
		data.D_PeerOrgs[] result = new data.D_PeerOrgs[po.length];
		for(int k=0; k<po.length; k++) {
			ASN1.Decoder d = new ASN1.Decoder(Util.byteSignatureFromString(po[k]));
			try {
				result[k] = new data.D_PeerOrgs().decode(d);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	/**
	 * Break subdivisions and reassemble after extracting out the prefix ending with crt
	 * returns all if crt is not found
	 * @param names_subdivisions
	 * @param crt
	 * @return
	 */
	public static String getChildSubDivision(String names_subdivisions, String crt){
		int k;
		String result=table.neighborhood.SEP_names_subdivisions;
		if(DEBUG) System.out.println("Util:getChildSubDivision:Child Subdivisions: "+names_subdivisions);
		if (names_subdivisions==null) return null;
		String[]splits = names_subdivisions.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		for(k=1; k<splits.length; k++)
			if (splits[k].equals(crt)) break;
		if(k==splits.length) return names_subdivisions;
		for(k++; k<splits.length; k++)
			if(!"".equals(splits[k])) result = result+splits[k]+table.neighborhood.SEP_names_subdivisions;
		return result;
	}
	/**
	 * Break subdivisions and reassemble after extracting out the prefix ending with crt
	 * returns all if crt is not found
	 * @param names_subdivisions
	 * @param crt
	 * @return
	 */
	public static String[] getChildSubDivisions(String names_subdivisions, String crt) {
		String result_c = getChildSubDivision(names_subdivisions, crt);
		if(result_c == null) return null;
		if(DEBUG) System.out.println("Util:getChildSubDivisions: input "+names_subdivisions);
		String[] splits = result_c.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		if(splits.length < 2) return null;
		String[] result = new String[splits.length - 1];
		int i=1;
		for(int k=0; k<result.length; k++,i++) {
			result[k] = splits[i];
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
    public static String getJFieldText(JComponent com) {
    	if(DEBUG)System.out.println("ConstituentActions:getText:"+com);
    	String value;
		if(com instanceof JTextField){
			value = ((JTextField)com).getText();
	    	if(DEBUG)System.out.println("ConstituentActions:getText: got textfield"+value);
		}else{
			value = ((JComboBox)com).getSelectedItem().toString();
	    	if(DEBUG)System.out.println("ConstituentActions:getText: got "+value);
		}
		return value;
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
	public static Hashtable<String, String> AL_AL_O_2_HSS_SS(
			ArrayList<ArrayList<Object>> in) {
		if(DEBUG && (in!=null)) out.println("Util:AL_AL_O_2_HSS_SS: in=#"+in.size());
		Hashtable<String, String> result = new Hashtable<String, String>();
		if ((in==null)||(in.size()==0)) return result;
		
		for(ArrayList<Object> o: in){
			result.put(Util.getString(o.get(0)), Util.getString(o.get(1)));
		}
		return result;
	}
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
	public static void cleanFileSelector(JFileChooser filterUpdates){
		File f = filterUpdates.getCurrentDirectory();
		filterUpdates.setSelectedFile(new File(" "));
		filterUpdates.setCurrentDirectory(f);
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
	                // Silently ignore wrong manifests on classpath?
	            }
	        }
	    } catch (IOException e1) {
	        // Silently ignore wrong manifests on classpath?
	    }
	    return null; 
	}
    /**
     * This function is supposed to read an image from a filename pictureImage and convert it to a byte[]
     * Unfortunately not portable ..., based on JPEGImageEncoder
     * 
     * see
     * http://mindprod.com/jgloss/imageio.html#TOBYTES
     * @param pictureImage
     * @return
     */
    public static byte[] getImage(String pictureImage){
    	
    	byte[] byteArray=null;
    	ImageIcon imageIcon;
    	try{
    		// iconData is the original array of bytes
    		imageIcon = new ImageIcon(pictureImage);
    		Image img = imageIcon.getImage();
    		Image imageResize = img.getScaledInstance(100, 100, 0);
    		ImageIcon imageIconResize = new ImageIcon (imageResize);
    		int resizeWidth = imageIconResize.getIconWidth();
    		int resizeHeight = imageIconResize.getIconHeight();
    		Panel p = new Panel();
    		BufferedImage bi = new BufferedImage(resizeWidth, resizeHeight,
    				BufferedImage.TYPE_INT_RGB);
//    		Graphics2D big = bi.createGraphics();
//    		big.drawImage(imageResize, 0, 0, p);
//    		ByteArrayOutputStream os = new ByteArrayOutputStream();
//    		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
//    		encoder.encode(bi);
//    		byteArray = os.toByteArray();
    		
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
     		boolean success = ImageIO.write(bi, DD.CONSTITUENT_PICTURE_FORMAT, baos);
    		if(success) byteArray = baos.toByteArray();
    		else System.err.println("ConstituentAction:getImage: appropriate picture writter missing");
    		
    	}catch(RuntimeException ev){ev.printStackTrace();}
    	catch (Exception e) {e.printStackTrace();}
    	return byteArray;
    	//throw new RuntimeException("Not implemented JPEG!");
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
		//if(result == null) result=new byte[]{0,0,0,0};
		return result;
	}
	/**
	 * Extracts simple IP ([10,0,0,102]) from "10.0.0.102/255.0.0.0"
	 * @param ip
	 * @param _mask
	 * @return
	 */
	public static byte[] get_broadcastIP_from_IP_and_NETMASK(String ip, String _mask){
		//if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: broadcast from ip="+ip+" mask="+_mask);
		byte[] iP = getBytesFromCleanIPString(ip);
		//if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: ip  =="+Util.byteToHex(iP));
		byte[] mask = getBytesFromCleanIPString(_mask);
		//if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: mask=="+Util.byteToHex(mask));
		byte[] result = new byte[4];
		byte ff = (byte)0xff;
		if((iP == null)||(mask==null)) return new byte[]{ff,ff,ff,ff};
		for(int i=0; i<=3; i++) {
			result[i] = (mask[i]==ff)?iP[i]:ff;
		}
		//if(DEBUG) System.out.println("Util:get_broadcastIP_from_IP_and_NETMASK: broadcast="+Util.byteToHex(result));
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
		if(ip.compareTo(Detect_interface.WINDOWS_NO_IP)==0) return false;
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
			//e.printStackTrace();
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
	public static final JLabel crtProcessLabel = new JLabel("",SwingConstants.LEFT);
	public static Process crtScriptProcess = null;
	public static final int INTERRUPTED = 325;
	public static void stopCrtScript(){
		try{
			if(crtScriptProcess!=null) crtScriptProcess.destroy();
		}catch(Exception e){}
	}
	public static BufferedReader getProcessOutput(String[] cmd_names, String[] env, File dir) throws IOException {
		return getProcessOutput(cmd_names, false, env, dir);
	}
	public static BufferedReader getProcessOutput(String[] cmd_names) throws IOException {
		return getProcessOutput(cmd_names, false);
	}
	public static BufferedReader getProcessOutput(String[] cmd_names, boolean concats) throws IOException {
		return getProcessOutput(cmd_names, concats, null, null);
	}
	public static BufferedReader getProcessOutput(String[] cmd_names, boolean concats, String[] env, File dir) throws IOException {
		//boolean DEBUG = false;
		String cmd = Util.concat(cmd_names, "\" \"");
		String result = null;
		synchronized(scripts_monitor){
			//Util.printCallPath("script");
			if(DEBUG) System.out.println("Util:getProcessOutput: Calling: \""+cmd+"\"");
			if(crtProcessLabel!=null) EventQueue.invokeLater(new RunnableCmd("Start: \""+cmd+"\""));
			Process crtScriptProcess;
			if(concats) crtScriptProcess =  Runtime.getRuntime().exec(cmd_names[0]);
			else crtScriptProcess = Runtime.getRuntime().exec(cmd_names, env, dir);
			BufferedReader bri1 = new BufferedReader (new InputStreamReader(crtScriptProcess.getInputStream()));
			for(;;){
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
			if(0!=exit){
				if(DD.SCRIPTS_ERRORS_WARNING)
					Application.warning(_("Process:"+"\n"+cmd+"\n"+_("exits with:")+exit), _("Process exit error"));
				if(_DEBUG)System.out.println("Util:getProcessOutput: exit with: "+exit+" output=\n"+result+"\n*******");
			}
			if(DEBUG)System.out.println("Util:getProcessOutput: output=\n"+result+"\n*******");
			if(crtProcessLabel!=null) EventQueue.invokeLater(new RunnableCmd("Done: \""+cmd+"\""));
		}
		if(result == null) return null;
		BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes())));
		return r;
	}
	/**
	 * This should be used for command lines that cannot be broken, such as "netsh ip .."
	 * @param disconnect
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader getProcessOutputConcatParams(String[] disconnect) throws IOException {
		return getProcessOutput(new String[]{Util.concat(disconnect, " ")}, true);
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
	public static InetAddress getHostIA(String domain) {
		if(DEBUG) System.out.println("Util:getHostIAxx: start");
		GetHostIA gh = new GetHostIA(domain);
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
	public static int prefixNumber(String s) {
		if(s == null) return 0;
		if(s.length() == 0) return 0;
		int k=0;
		for(; k<s.length(); k++){
			if(s.charAt(k)<'0') break;
			if(s.charAt(k)>'9') break;
		}
		if(k==0) return 0;
		return Integer.parseInt(s.substring(0, k));
	}
	public static boolean equalCalendars_null_or_not(Calendar c1,
			Calendar c2) {
		if((c1==null) && (c2==null)) return true;
		if((c1==null) || (c2==null)) return false;
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
		if(i==null) return null;
		if(i instanceof Long) return (Long)i;
		return new Long(""+Long.parseLong(i.toString()));
	}
	public static boolean emptyString(String in) {
		if(in==null) return true;
		return "".equals(in);
	}
}
class GetHostName extends Thread{
	String hostName = null;
	InetAddress domain;
	InetSocketAddress sock_addr;
	GetHostName(InetAddress domain, InetSocketAddress sock_addr) {
		this.domain = domain;
		this.sock_addr = sock_addr;
		this.start();
	}
	public void run(){
		hostName = "";
		if(domain!=null) {
			hostName = domain.getHostName();
		}else
			if(sock_addr != null) {
				hostName = sock_addr.getHostName();
			}
		synchronized(this){
			this.notifyAll();
		}
	}
}
class GetHostIA extends Thread{
	static final boolean DEBUG = false;
	public boolean started = false;
	String domain = null;
	InetAddress hostIA = null;
	//InetSocketAddress sock_addr;
	GetHostIA(String domain) {
		this.domain = domain;
		//this.sock_addr = sock_addr;
		this.start();
	}
	public void run(){
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

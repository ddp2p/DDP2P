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
 package net.ddp2p.ASN1;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;
 
public
class Decoder {
	public static final boolean DEBUG = false;
	public static final boolean _DEBUG = true;
	public static boolean DEBUG_TODO = true;
	/**
	 * The buffer with encoded data
	 */
	byte[] data;
	/**
	 * The offset in data where the current decoder head is positioned
	 */
	int offset;
	/**
	 * The length of the current component being decoded, after which null is returned even if "data" has more data
	 */
	int length;
	
	public String toString() {
		byte[] val=new byte[length];
		Encoder.copyBytes(val, 0, data, length, offset);

		return ASN1_Util.byteToHex(val, " ");
	}
	/**
	 * 
	 * @param data 
	 * 		should be not null
	 */
	public Decoder(byte[] data){
		if(data==null) throw new RuntimeException("parameter data should not be null");
		init(data,0,data.length);
	}
	public Decoder(byte[] data, int offset){
		init(data, offset, data.length-offset);
	}
	public Decoder(byte[] data, int offset,int length) {
		init(data,offset,length);
	}
	public String dumpHex() {
		return ASN1_Util.byteToHex(data, offset, length, " ");
	}
	public String dumpHex(int head){
		return ASN1_Util.byteToHex(data, offset, head, " ");
	}
	public void printHex(String name){
		System.out.println(name+dumpHex());
	}
	public void init(byte[] data, int offset,int length) {
		if (data == null) throw new RuntimeException("parameter data should not be null");
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
	/**
	 * Returns the first byte of the type
	 * 
	 * @return
	 */
	public byte type(){
		if (length <= 0) return 0;
		return data[offset];
	}
	/**
	 * Returns the first byte of the type by a call to type().
	 * @return
	 */
	public byte getTypeByte() {
		return type();
	}
	/**
	 * If no data here
	 * @return
	 */
	public boolean isEmpty() {
		return ((data == null) || (length <= 0));
	}
	/**
	 * Returns the tag, 0x1f in case there are more bytes!
	 * @return
	 */
	public int tagVal() {
		if (length <= 0) return 0;
		return data[offset] & 0x1f;
	}
	/**
	 * Returns the class value, which can be one of:
	 * Encoder.CLASS_APPLICATION,
	 * Encoder.CLASS_CONTEXT,
	 * Encoder.CLASS_PRIVATE,
	 * Encoder.CLASS_UNIVERSAL
	 * @return
	 */
	public int typeClass() {
		if (length <= 0) return 0;
		return (data[offset] & 0xc0) >> 6;
	}
	/**
	 * Returns the type of the object which can be one of:
	 *  Encoder.PC_CONSTRUCTED,
	 *  Encoder.PC_PRIMITIVE
	 * @return
	 */
	public int typePC() {
		if (length <= 0) return 0;
		return (data[offset]&0x20)>>5;
	}
	public boolean isType(Class<? extends ASNObjArrayable> c) {
		ASN1Type a = c.getAnnotation(ASN1Type.class);
		if (a == null) throw new RuntimeException("Missing Annotation");
		
		int _class = a._class()+a._CLASS().ordinal();
		if (_class != typeClass()) return false;
		
		int _pc = a._pc()+a._PC().ordinal();
		if (_pc != typePC()) return false;
		
		// here if class and pc match
		if ((a._tag() < 31) && (a._tag() >= 0) && (a._tag() == tagVal())) return true;
		BigInteger lTag = null; // only extract local BN tag once
		if ((!"".equals(a._stag())) && (new BigInteger(a._stag()).equals(lTag = this.getTagValueBN()))) return true;
		if (this.tagVal() < 31) return false;
		if (a._tag() >= 31) {
			if (lTag == null) lTag = this.getTagValueBN();
			if (new BigInteger(""+a._tag()).equals(lTag)) return true;
		}
		return false;
	}
	/**
	 * Can use tagVal() if it is less than 31;
	 * @return
	 */
	public BigInteger getTagValueBN() {
		int len = typeLen();
		if (len == 1) {
			return new BigInteger(""+tagVal());
		}
		assert(this.tagVal() == 0x1f);
		//byte[] tag = new byte[len -1];
		//BigInteger bn128 = ASN1_Util.BN128;
		return ASN1_Util.fromBase128(data, offset + 1, offset + length);
	}
	public boolean hasType(int ASN1class, int ASN1type, BigInteger tag) {
		if (this.typeClass() != ASN1class) return false;
		if (this.typePC() != ASN1type) return false;
		if (!this.getTagValueBN().equals(tag)) return false;
		return true;
	}
	public int typeLen(){
		if (length <= 0) return 0;
		if (tagVal() != 0x1f) return 1; 
		int k = offset+1;
		int len = 2;
		while ((k < length+offset) && (ASN1_Util.getUnsignedShort(data[k]) > 127)) {len++; k++;}
		if (len > length) return 0;
		return len;
	}
	public int contentLength() {
		//System.err.println("in ContentLength: length="+length);
		if (length < 2) {
			//System.err.println("in ContentLength: 2>length="+length);
			return 0;
		}
		int tlen = typeLen();
		//System.err.println("in ContentLength: tlen="+tlen);
		if (tlen < 1) {
			return 0;
		}
		if (ASN1_Util.getUnsignedShort(data[offset+tlen]) < 128) {
			//System.err.println("in ContentLength: len_len=="+data[offset+tlen]+" off="+offset);
			return data[offset+tlen];
		}
		//System.err.println("Len_len="+data[offset+tlen]+" gs="+gs(data[offset+tlen]));
		int len_len=ASN1_Util.getUnsignedShort(data[offset+tlen])-128;
		//System.err.println("actual Len_len="+len_len);
		if (length < len_len+tlen+1){
			//System.err.println("in ContentLength: length<1_tlen+len_len="+len_len);
			return 0;
		}
		byte[] len=new byte[len_len];
		Encoder.copyBytes(len, 0, data, len_len, offset+tlen+1);
		//System.err.println("len_bytes:"+ASN1_Util.byteToHex(len, " "));
		BigInteger bi = new BigInteger(len);
		int ilen = bi.intValue();
		//System.err.println("done ContentLength: "+ilen);
		return ilen;
	}
	public int lenLen() {
		//System.err.println("lenLen:"+ASN1_Util.byteToHex(data, offset, 5, " "));
		int tlen = typeLen();
		//System.err.println(tlen+"=tlen +length byte="+data[offset+tlen]);
		if(length<2) return 0;
		if(ASN1_Util.getUnsignedShort(data[offset+tlen])<128) return 1;
		return 1+ASN1_Util.getUnsignedShort(data[offset+tlen])-128;
	}
	/**
	 * 
	 * @return -1 : not enough bytes in buffer to determine type and length of length
	 * 			-k: not enough bytes (k-1 needed) to determine length of length
	 * 		   larger than 0: returns length of the buffer needed to store the whole first ASN1 object
	 * 
	 */
	public int objectLen() {
		int type_len = typeLen();
		if ((type_len == 0) || (type_len>=length)) return -1; // insufficient to find
		int len_len = lenLen();
		if((len_len==0) || (len_len+type_len>length)) return -(type_len+len_len+1);
		int content_len = contentLength();
		return type_len+len_len+content_len;
	}
	public String[] getSequenceOf(byte type) throws ASN1DecoderFail{
		return getSequenceOfAL(type).toArray(new String[]{});
	}
	/**
	 * 
	 * @param type
	 * @param alist : from ArrayList?
	 * If alist is set, then each element is translated in an entry where the value is a generalized time
	 * computed from the position.
	 * Otherwise it extracts a Hashtable, each element from a different SEQUENCE
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Hashtable<String, String> getSequenceOfHSS(byte type, boolean alist) throws ASN1DecoderFail {
		Decoder dec = getContent();
		Hashtable<String,String> al = new Hashtable<String,String>();
		long pos = 0;
		for (;;) {
			Decoder d_t = dec.getFirstObject(true);
			if (d_t == null) break;
			if (! alist)
				d_t = d_t.getContent();
			String k = d_t.getFirstObject(true).getString(type);
			String val=null;
			if (! alist) {
				d_t = d_t.getFirstObject(true);
				if (d_t != null) val = d_t.getString(type);
			}
			if (val == null) {
				if (DEBUG_TODO) System.out.println("Decoder: transient versions!");
				//if(_DEBUG)ASN1_Util.printCallPath("Who?");
				Calendar c = ASN1_Util.CalendargetInstance();
				c.setTimeInMillis(pos+Encoder.CALENDAR_DISPLACEMENT);
				//val = DD.EMPTYDATE;
				val = Encoder.getGeneralizedTime(c);
			}
			al.put(k, val);
			pos ++;
		}
		return al;
	}
	public ArrayList<String> getSequenceOfAL(byte type) throws ASN1DecoderFail{
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		Decoder dec = getContent();
		ArrayList<String> al= new ArrayList<String>();
		for(;;) {
			Decoder d_t = dec.getFirstObject(true, type);
			if(d_t==null) break;
			al.add(d_t.getString(type));
		}
		return al;
	}
	/**
	 * 
	 * @param type
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public ArrayList<BigInteger> getSequenceOfBNs(byte type) throws ASN1DecoderFail{
		if (getTypeByte() == Encoder.TAG_NULL) return null;
		Decoder dec = getContent();
		ArrayList<BigInteger> al= new ArrayList<BigInteger>();
		for(;;) {
			Decoder d_t = dec.getFirstObject(true, type);
			if (d_t == null) break;
			al.add(d_t.getInteger(type));
		}
		return al;
	}
	/**
	 * Requires types that implement ASNArrayableObj
	 * @param type
	 * @param inst
	 * @return
	 * @throws ASN1DecoderFail
	 */
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getSequenceOfAL(byte type, T inst) throws ASN1DecoderFail{
		if (getTypeByte() == Encoder.TAG_NULL) return null;
		Decoder dec = getContent();
		ArrayList<T> al= new ArrayList<T>();
		int k = 0;
		for(; ; k ++) {
			Decoder d_t;
			try {
				d_t = dec.getFirstObject(true, type);
			} catch (Exception e) {if (DEBUG) System.out.println("Decoder:getSeqOfAl:k="+k); if (e instanceof ASN1DecoderFail) throw e; else throw new ASN1DecoderFail(e.getMessage());}
			if (d_t==null) break;
			try {
				al.add(((T) ((ASNObjArrayable)inst).instance().decode(d_t)));
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return al;
	}
	/**
	 * 
	 * @param <T>
	 * @param type the ASN type of each element in the sequence
	 * @param temp a empty array prototype
	 * @param inst a instance prototype
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public <T> T[] getSequenceOf(byte type, T[] temp, T inst) throws ASN1DecoderFail{
		ArrayList<T> al;
		al = getSequenceOfAL(type, inst);
		if (al == null) return null;
		return al.toArray(temp);
	}
	/**
	 * 
	 * @param extract  Changes this to get rid of the first Object
	 * @return A new Decoder Object for the first in list
	 */
	public Decoder getFirstObject(boolean extract) {
		if (length <= 0) return null;
		int tlen = typeLen();
		int llen = lenLen();
		int cLen = contentLength();
		//System.out.println("Decoder: lens="+tlen+"+"+llen+"+"+cLen+"<="+length +" off="+offset);
		int new_len = tlen + llen + cLen;
		if (new_len > length || new_len < 0) throw new ASNLenRuntimeException("ASN1:Decoding:Invalid object length ["+new_len+"]: Too long given available data:"+length);
		int old_offset = offset;
		if (extract) {
			offset += new_len;
			length -= new_len;
			//System.out.println("Decoder: remains lens="+tlen+"+"+llen+"+"+cLen+"<="+length +" off="+offset);
			if (offset < 0 || length < 0) throw new ASNLenRuntimeException("Arrive at negative offset after extracting " + new_len+"/"+length);
		}
		//System.out.println("Decoder: extracts lens="+new_len+"<="+length +" off="+old_offset+"/"+data.length);
		return new Decoder(data, old_offset, new_len);
	}
	/**
	 * 
	 * @param extract  Changes this to get rid of the first Object
	 * @return A new Decoder Object for the first in list
	 * @throws ASN1DecoderFail 
	 */
	public Decoder getFirstObject(boolean extract, byte type) throws ASN1DecoderFail {
		if (length <= 0) return null;
		byte found = getTypeByte();
		if ((found != type)&&(getTypeByte()!=Encoder.TAG_NULL))
			throw new ASN1DecoderFail("No type: "+type+" but "+found+" in "+this.dumpHex());
		int new_len = typeLen()+lenLen()+contentLength();
		if(new_len > length || new_len < 0) throw new ASNLenRuntimeException("Too long");
		int old_offset = offset;
		if(extract){
			offset += new_len;
			length -= new_len;
			if (offset < 0 || length < 0) throw new ASNLenRuntimeException("Arrive at negative offset after extracting " + new_len+"/"+length);
		}
		return new Decoder(data,old_offset,new_len);
	}
	/**
	 * Default is to consider the tag was implicit.
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Decoder getContent() throws ASN1DecoderFail {
		return getContentImplicit();
	}	
	/**
	 * Lets this unchanged and returns a new Decoder.
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Decoder getContentImplicit() throws ASN1DecoderFail {
		if (DEBUG) System.err.println("getContent: Length: "+length);
		if (length <= 0) throw new ASNLenRuntimeException("Container length 0");
		int new_len;
		new_len = contentLength();
		if (DEBUG) System.err.println("getContent: new_Length: "+new_len);
		int new_off = typeLen()+lenLen();
		//System.err.println("getContent: new offset: "+new_off);
		if(new_off>length) throw new ASN1DecoderFail("Content "+new_off+":"+new_len+" exceeds container "+length);
		if(new_off<0) throw new ASN1DecoderFail("Content "+new_off+":"+new_len+" has negative offset in container length "+length);
		if(new_off+new_len>length) throw new ASN1DecoderFail("Content "+new_off+":"+new_len+" exceeds container "+length);
		if(new_len>length) throw new ASN1DecoderFail("Too long");
		if(new_len < 0) throw new ASN1DecoderFail("Negative length");
		return new Decoder(data,offset+new_off,new_len);
	}
	/**
	 * 
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Decoder getContentExplicit() throws ASN1DecoderFail {
		Decoder d = this.getContent();
		return d.getFirstObject(true).getContent();
	}
	/**
	 * Just remove an explicit tag, leaving the (default) implicit one.
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Decoder removeExplicitASN1Tag() throws ASN1DecoderFail {
		Decoder d = this.getContent();
		return d.getFirstObject(true);
	}
	public boolean getBoolean() {
		if(length<=0) throw new ASNLenRuntimeException("Boolean length");
		//assert
		if (!(data[offset]==Encoder.TAG_BOOLEAN))
			ASN1_Util.printCallPathTop("Encoder: getBoolean: Failed Encoder:"+getTypeByte());
		assert(data[offset+1]==1);
		assert(data[offset+2] == 0 || data[offset+2] == -1);
		return data[offset+2] != 0;
	}
	public boolean getBoolean(byte type) throws ASN1DecoderFail {
		if(length<=0) throw new ASNLenRuntimeException("Boolean length");
		if(data[offset]!=type) throw new ASN1DecoderFail("Wrong boolean type");
		//assert(data[offset]==type);
		assert(data[offset+1]==1);
		assert(data[offset+2] == 0 || data[offset+2] == -1);
		return data[offset+2] != 0;
	}
	public BigInteger getInteger() {
		//boolean DEBUG = true;
		try {
			if (DEBUG) System.out.println("Decoder: getInteger: type="+data[offset]+" vs="+Encoder.TAG_INTEGER);
			//assert
			if (! (data[offset] == Encoder.TAG_INTEGER)) {
				ASN1_Util.printCallPathTop("Encoder: getInteger: Failed Encoder:"+getTypeByte());
			}
		} catch (Exception e) {
			System.out.println("Decoder: getInteger: type="+data[offset]);
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("Decoder: getInteger: result = "+ASN1_Util.byteToHex(data, offset, typeLen()+lenLen(), " "));
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		BigInteger result = new BigInteger(value);
		if (DEBUG) System.out.println("Decoder: getInteger: result = "+result);
		return result;
	}
	public BigInteger getInteger(byte tagInteger) throws ASN1DecoderFail {
		if(getTypeByte() == Encoder.TAG_NULL) return null;
		if (data[offset] != tagInteger) throw new ASN1DecoderFail("Wrong tag");
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return new BigInteger(value);
	}
	/**
	 * For OCTETSTRING
	 * @return
	 */
	public byte[] getBytes() {
		//assert
		if (!((data[offset]==Encoder.TAG_OCTET_STRING)||
				(data[offset]==Encoder.TAG_BIT_STRING)||
				(data[offset]==Encoder.TAG_NULL))){
			ASN1_Util.printCallPathTop("Encoder: getBytes: Failed Encoder:"+getTypeByte());
		}
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return value;
	}
	public byte[] getBytesAnyType() {
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return value;
	}
	/**
	 * The number of padding bits is stored in the parameter if this has at least one byte;
	 * @param bits_padding
	 * @return
	 */
	public byte[] getBitString_AnyType(byte[] bits_padding) {
		byte[] value = new byte[contentLength() - 1];
		Encoder.copyBytes(value, 0, data, contentLength()-1, offset+typeLen()+lenLen()+1);
		if ((bits_padding != null) && (bits_padding.length > 0))
			bits_padding[0] = data[offset+typeLen()+lenLen()];
		return value;
	}
	public byte[] getBytes(byte type) throws ASN1DecoderFail {
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		if(data[offset]!=type) throw new ASN1DecoderFail("OCTET STR: "+type+" != "+data[offset]);
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return value;
	}
	public int[] getOID(byte type) throws ASN1DecoderFail {
		if(data[offset]!=type) throw new ASN1DecoderFail("OCTET STR: "+type+" != "+data[offset]);
		byte[] b_value = new byte[contentLength()];
		Encoder.copyBytes(b_value, 0, data, contentLength(),offset+typeLen()+lenLen());
		int len=2;
		for(int k=1; k < b_value.length; k++) if (b_value[k]>=0) len++;
		int[] value = new int[len];
		value[0] = get_u32(b_value[0])/40;
		value[1] = get_u32(b_value[0])%40;
		for(int k=2, crt=1; k<value.length; k++) {
			value[k]=0;
			while (b_value[crt] < 0) {
				value[k] <<= 7;
				value[k] += b_value[crt++]+128;//get_u32(b_value[crt++])-128;
			}
			value[k] <<= 7;
			value[k] += b_value[crt++];
			continue;
		}
		return value;
	}
	public BigInteger[] getBNOID(byte type) throws ASN1DecoderFail {
		if(data[offset]!=type) throw new ASN1DecoderFail("OCTET STR: "+type+" != "+data[offset]);
		byte[] b_value = new byte[contentLength()];
		Encoder.copyBytes(b_value, 0, data, contentLength(),offset+typeLen()+lenLen());
		int len = 2;
		for (int k = 1; k < b_value.length; k ++) if (b_value[k] >= 0) len ++;
		BigInteger[] value = new BigInteger[len];
		int value0 = get_u32(b_value[0]) / 40;
		int value1 = get_u32(b_value[0]) % 40;
		if (value0 > 2) {
			value1 += 40 * (value0 - 2);
			value0 = 2;
		}
		value[0] = new BigInteger(""+value0);
		value[1] = new BigInteger(""+value1);
		for (int k = 2, crt = 1; k < value.length; k ++) {
			//value[k] = ASN1_Util.fromBase128(b_value, k, this.offset + this.length);
			
			value[k] = BigInteger.ZERO;
			while (b_value[crt] < 0) {
				value[k] = value[k].shiftLeft(7);
				value[k] = value[k].or(new BigInteger("" + (b_value[crt ++] + 128))); //get_u32(b_value[crt++])-128;
			}
			value[k] = value[k].shiftLeft(7);
			value[k] = value[k].or(new BigInteger("" + (b_value[crt ++]))); //get_u32(b_value[crt++]);
			continue;
			
		}
		return value;
	}
	public static int get_u32(byte val){
		if (val>=0) return val;
		return 256+(int)val;
	}
	public byte[] getAny() {
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return value;
	}
	public double getReal(){
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return new Double(new String(value)).doubleValue();
	}
	public String getString() {
		if (getTypeByte()==Encoder.TAG_NULL) return null;
		
		//assert
		if(!((data[offset]==Encoder.TAG_IA5String)||
				(data[offset]==Encoder.TAG_UTF8String)||
				(data[offset]==Encoder.TAG_PrintableString)))
			ASN1_Util.printCallPathTop("Encoder: getString: Failed Encoder:String type:"+getTypeByte());
		
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		try {
			return new String(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new String(value);
	}
	public String getString(byte type) throws ASN1DecoderFail {
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		if(data[offset]!=type) throw new ASN1DecoderFail("String: exp="+type+" != in="+data[offset]);
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		try {
			return new String(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new String(value);
	}
	public String getStringAnyType() throws ASN1DecoderFail {
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		try {
			return new String(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new String(value);
	}
	/**
	 * Any type (assume type was previously tested)
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public String getGeneralizedTimeAnyType() throws ASN1DecoderFail {
		int cLength = contentLength();
		if(cLength == 0) return null;
		byte[] value = new byte[cLength];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		String str = new String(value);
		//Calendar result=GregorianCalender(str);
		return str;
	}
	public String getGeneralizedTime(byte type) throws ASN1DecoderFail {
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		if(data[offset]!=type) throw new ASN1DecoderFail("generalizedTime: "+type+" != "+data[offset]);
		return getGeneralizedTimeAnyType();
	}
	/**
	 *  Only works assuming type = Encoder.TAG_GeneralizedTime
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public String getGeneralizedTime_() throws ASN1DecoderFail {
		return getGeneralizedTime(Encoder.TAG_GeneralizedTime);
	}
	public Calendar getGeneralizedTimeCalender(byte type) throws ASN1DecoderFail {
		return ASN1_Util.getCalendar(this.getGeneralizedTime(type));
	}
	/**
	 * Only works assuming type = Encoder.TAG_GeneralizedTime
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Calendar getGeneralizedTimeCalender_() throws ASN1DecoderFail {
		return ASN1_Util.getCalendar(this.getGeneralizedTime(Encoder.TAG_GeneralizedTime));
	}
	/**
	 * Only works assuming type previously tested
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Calendar getGeneralizedTimeCalenderAnyType() throws ASN1DecoderFail {
		return ASN1_Util.getCalendar(this.getGeneralizedTimeAnyType());
	}
	/**
	 * Currently not expanding the buffer but rather abandon if too small. 
	 * Also returns false on end of stream.
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public boolean fetchAll(InputStream is) throws IOException {
		//Decoder dec = new Decoder(sr,0,msglen);
		while (true) {
			int asrlen = objectLen();
			if(DEBUG)System.out.println("Object size="+asrlen+"; Buffer size="+data.length+"; Current size="+length);
			if ((asrlen > 0) && (asrlen > data.length - offset)) {
				if (DEBUG) System.err.println("Object size="+asrlen+"; Buffer size="+data.length);
				return false; // not enough space
			}
			if ((asrlen < 0) || (length < asrlen)) {
				if (DEBUG)System.out.println("Object size="+asrlen+"; Current size="+length);
				if (length == data.length - offset) return false; // at end
				int inc = is.read(data, length, data.length-length);
				if (inc <= 0) return false;
				length += inc;
				//dec = new Decoder(data,0,length);
				continue;
			}
			break; // enough data
		}
		return true;
	}
	public int getMSGLength() {
		return length;
	}
    public String dumpHexDump() {
        return dumpHex(ASN1_Util.MAX_ASN1_DUMP);
    }
    /**
     * Returns an array of ints
     * @return
     */
	public int[] getIntsArray() {
		if (this.getTypeByte() == Encoder.TAG_NULL) return null;
		Decoder dec;
		try {
			dec = this.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		ArrayList<BigInteger> ints = new ArrayList<BigInteger>();
		for(;;){
			Decoder val = dec.getFirstObject(true);
			if(val==null) break;
			ints.add(val.getInteger());
		}
		int result[] = new int[ints.size()];
		for(int k = 0; k<ints.size(); k++) {
			result[k] = ints.get(k).intValue();
		}
		return result;
	}
	public BigInteger[] getBNIntsArray() {
		if (this.getTypeByte() == Encoder.TAG_NULL) return null;
		Decoder dec;
		try {
			dec = this.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		ArrayList<BigInteger> ints = new ArrayList<BigInteger>();
		for (;;) {
			Decoder val = dec.getFirstObject(true);
			if (val == null) break;
			ints.add(val.getInteger());
		}
//		BigInteger result[] = BigInteger int[ints.size()];
//		for(int k = 0; k<ints.size(); k++) {
//			result[k] = ints.get(k).intValue();
//		}
		return ints.toArray(new BigInteger[0]); //result;
	}
	public float[] getFloatsArray() {
		if (this.getTypeByte() == Encoder.TAG_NULL) return null;
		Decoder dec;
		try {
			dec = this.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		ArrayList<String> ints = new ArrayList<String>();
		for(;;){
			Decoder val = dec.getFirstObject(true);
			if(val==null) break;
			ints.add(val.getString());
		}
		float result[] = new float[ints.size()];
		for(int k = 0; k<ints.size(); k++) {
			result[k] = Float.parseFloat(ints.get(k));
		}
		return result;
	}
	// Test by Andreas Bjoru
	//@Test
	public static void encodeDecodeCalendar() throws ASN1DecoderFail {
	        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	        
	        Encoder enc = new Encoder(cal);
	        Decoder dec = new Decoder(enc.getBytes());
	        Calendar res = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();//.getGeneralizedTimeCalendar();

	        int m1 = cal.get(Calendar.MONTH);
	        int m2 = res.get(Calendar.MONTH);
	        System.out.println("Compared: "+m1+" vs "+m2);
	        //Assert.assertEquals(m1, m2);
	}
	/**
	 * compares the parameter tag with the result of getTypeByte() when there is a next object (peeked)
	 * @param tag
	 * @return
	 */
	public boolean isFirstObjectTagByte(byte tag) {
		return (getFirstObject(false)!=null)&&(getFirstObject(false).getTypeByte()==tag);
	}
	/**
	 * Peeks the next object and compares to null
	 * @return
	 */
	public boolean isEmptyContainer() {
		return (getFirstObject(false)==null);
	}
	/**
	 * Test example.
	 * @param nb
	 * @throws ASN1DecoderFail
	 */
	public static void encodeDecodeBNTAG(String nb) throws ASN1DecoderFail {
		byte msg[];
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence (new Encoder(new BigInteger("1")));
		
		msg = enc.getBytes();
		if (DEBUG) System.out.println("BN: "+ASN1_Util.byteToHex(msg));

		enc.setASN1Type(Encoder.CLASS_CONTEXT, Encoder.PC_CONSTRUCTED, new BigInteger(nb,16));
		
		msg = enc.getBytes();
		if (DEBUG) System.out.println("BN: "+ASN1_Util.byteToHex(msg));
		
		Decoder dec = new Decoder(msg);
		System.out.println("BN: decoder "+dec);

		System.out.println("BN: decoder tag: "+dec.getTagValueBN());
		
		Decoder d = dec.getContent();
		System.out.println("BN: dec con "+d);

		System.out.println("BN: integer "+d.getFirstObject(true).getInteger());
	}
    public static void main(String[]args) {
    	try {
    		encodeDecodeBNTAG(args[0]);
			//encodeDecodeCalendar();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
    }
}

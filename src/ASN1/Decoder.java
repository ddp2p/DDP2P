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
 package ASN1;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import util.Util;
 
public
class Decoder {
	private static final boolean DEBUG = false;
	byte[] data;
	int offset;
	int length;
	public String toString(){
		byte[] val=new byte[length];
		Encoder.copyBytes(val, 0, data, length, offset);
		return Util.byteToHex(val, " ");
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
	public String dumpHex(){
		return Util.byteToHex(data, offset, length, " ");
	}
	public String dumpHex(int head){
		return Util.byteToHex(data, offset, head, " ");
	}
	public void printHex(String name){
		System.out.println(name+dumpHex());
	}
	public void init(byte[] data, int offset,int length) {
		if(data==null) throw new RuntimeException("parameter data should not be null");
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
	public byte type(){
		if(length<=0) return 0;
		return data[offset];
	}
	public byte getTypeByte(){
		return type();
	}
	public int tagVal(){
		if(length<=0) return 0;
		return data[offset]&0x1f;
	}
	public int typeClass(){
		if(length<=0) return 0;
		return (data[offset]&0xc0)>>6;
	}
	public int typePC(){
		if(length<=0) return 0;
		return (data[offset]&0x20)>>5;
	}
	public int typeLen(){
		if(length<=0) return 0;
		if(tagVal() != 0x1f) return 1; 
		int k=offset+1;
		int len=2;
		while((k<length+offset) && (gs(data[k])>127)) {len++; k++;}
		if (len > length) return 0;
		return len;
	}
	public static short gs(byte val) {
		if(val >= 0) return val;
		return (short)(val+256);
	}
	public int contentLength() {
		//System.err.println("in ContentLength: length="+length);
		if(length<2){
			//System.err.println("in ContentLength: 2>length="+length);
			return 0;
		}
		int tlen=typeLen();
		//System.err.println("in ContentLength: tlen="+tlen);
		if(tlen<1){
			return 0;
		}
		if(gs(data[offset+tlen])<128){
			//System.err.println("in ContentLength: len_len=="+data[offset+tlen]+" off="+offset);
			return data[offset+tlen];
		}
		//System.err.println("Len_len="+data[offset+tlen]);
		int len_len=gs(data[offset+tlen])-128;
		//System.err.println("actual Len_len="+len_len);
		if(length<len_len+tlen+1){
			//System.err.println("in ContentLength: length<1_tlen+len_len="+len_len);
			return 0;
		}
		byte[] len=new byte[len_len];
		Encoder.copyBytes(len, 0, data, len_len, offset+tlen+1);
		//System.err.println("len_bytes:"+Util.byteToHex(len, " "));
		BigInteger bi = new BigInteger(len);
		int ilen = bi.intValue();
		//System.err.println("done ContentLength: "+ilen);
		return ilen;
	}
	public int lenLen(){
		//System.err.println("lenLen:"+Util.byteToHex(data, offset, 5, " "));
		int tlen = typeLen();
		//System.err.println(tlen+"=tlen +length byte="+data[offset+tlen]);
		if(length<2) return 0;
		if(gs(data[offset+tlen])<128) return 1;
		return 1+gs(data[offset+tlen])-128;
	}
	/**
	 * 
	 * @return -1 : not enough bytes in buffer to determine type and length of length
	 * 			-k: not enough bytes (k-1 needed) to determine length of length
	 * 			>0: returns length of the buffer needed to store the whole first ASN1 object
	 * 
	 */
	public int objectLen(){
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
	public Hashtable<String, String> getSequenceOfHSS(byte type) throws ASN1DecoderFail {
		Decoder dec = getContent();
		Hashtable<String,String> al= new Hashtable<String,String>();
		for(;;) {
			Decoder d_t = dec.getFirstObject(true);
			if(d_t==null) break;
			d_t = d_t.getContent();
			String k = d_t.getFirstObject(true).getString(type);
			String val=null;
			d_t = d_t.getFirstObject(true);
			if(d_t!=null) val = d_t.getString(type);
			al.put(k, val);
		}
		return al;
	}
	public ArrayList<String> getSequenceOfAL(byte type) throws ASN1DecoderFail{
		Decoder dec = getContent();
		ArrayList<String> al= new ArrayList<String>();
		for(;;) {
			Decoder d_t = dec.getFirstObject(true, type);
			if(d_t==null) break;
			al.add(d_t.getString(type));
		}
		return al;
	}
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getSequenceOfAL(byte type, T inst) throws ASN1DecoderFail{
		Decoder dec = getContent();
		ArrayList<T> al= new ArrayList<T>();
		for(;;) {
			Decoder d_t = dec.getFirstObject(true, type);
			if(d_t==null) break;
			try {
				al.add(((T) ((ASNObj)inst).instance().decode(d_t)));
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
		return al.toArray(temp);
	}
	/**
	 * 
	 * @param extract  Changes this to get rid of the first Object
	 * @return A new Decoder Object for the first in list
	 */
	public Decoder getFirstObject(boolean extract) {
		if(length<=0) return null;
		int tlen = typeLen();
		int llen = lenLen();
		int cLen = contentLength();
		//System.out.println("lens="+tlen+"+"+llen+"+"+cLen+"<="+length);
		int new_len = tlen+llen+cLen;
		if(new_len>length) throw new RuntimeException("ASN1:Decoding:Invalid object length ["+new_len+"]: Too long given available data:"+length);
		int old_offset = offset;
		if(extract){
			offset += new_len;
			length -= new_len;
		}
		return new Decoder(data,old_offset,new_len);
	}
	/**
	 * 
	 * @param extract  Changes this to get rid of the first Object
	 * @return A new Decoder Object for the first in list
	 * @throws ASN1DecoderFail 
	 */
	public Decoder getFirstObject(boolean extract, byte type) throws ASN1DecoderFail {
		if(length<=0) return null;
		if((getTypeByte()!=type)&&(getTypeByte()!=Encoder.TAG_NULL)) throw new ASN1DecoderFail("No type: "+type+" in "+this.dumpHex());
		int new_len = typeLen()+lenLen()+contentLength();
		if(new_len>length) throw new ASNLenRuntimeException("Too long");
		int old_offset = offset;
		if(extract){
			offset += new_len;
			length -= new_len;
		}
		return new Decoder(data,old_offset,new_len);
	}
	/**
	 * Lets this unchanged and returns a new Decoder.
	 * 
@param  None
	 * 
@return	A new Encoder
	 * 
	 */
	public Decoder getContent() throws ASN1DecoderFail {
		//System.err.println("getContent: Length: "+length);
		if(length<=0) throw new ASNLenRuntimeException("Container length 0");
		int new_len;
		new_len = contentLength();
		//System.err.println("getContent: new_Length: "+new_len);
		int new_off = typeLen()+lenLen();
		//System.err.println("getContent: new offset: "+new_off);
		if(new_off+new_len>length) throw new ASN1DecoderFail("Content "+new_off+":"+new_len+" exceeds container "+length);
		if(new_len>length) throw new ASN1DecoderFail("Too long");
		return new Decoder(data,offset+new_off,new_len);
	}
	public boolean getBoolean() {
		if(length<=0) throw new ASNLenRuntimeException("Boolean length");
		assert(data[offset]==Encoder.TAG_BOOLEAN);
		assert(data[offset+1]==1);
		return data[offset+2]>0;
	}
	public BigInteger getInteger() {
		assert(data[offset]==Encoder.TAG_INTEGER);
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return new BigInteger(value);
	}
	public BigInteger getInteger(byte tagInteger) throws ASN1DecoderFail {
		if(data[offset]!=Encoder.TAG_INTEGER) throw new ASN1DecoderFail("Wrong tag");
		byte[] value = new byte[contentLength()];
		Encoder.copyBytes(value, 0, data, contentLength(),offset+typeLen()+lenLen());
		return new BigInteger(value);
	}
	public byte[] getBytes() {
		assert((data[offset]==Encoder.TAG_OCTET_STRING)||
				(data[offset]==Encoder.TAG_BIT_STRING)||
				(data[offset]==Encoder.TAG_NULL));
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
		for(int k=1;k<b_value.length; k++) if(b_value[k]>=0) len++;
		int[] value = new int[len];
		value[0] = get_u32(b_value[0])/40;
		value[1] = get_u32(b_value[0])%40;
		for(int k=2, crt=1; k<value.length; k++) {
			value[k]=0;
			while(b_value[crt]<0) {
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
		int len=2;
		for(int k=1;k<b_value.length; k++) if(b_value[k]>=0) len++;
		BigInteger[] value = new BigInteger[len];
		int value0 = get_u32(b_value[0])/40;
		int value1 = get_u32(b_value[0])%40;
		if(value0 > 2) {
			value1 += 40*(value0-2);
			value0 =2;
		}
		value[0] = new BigInteger(""+value0);
		value[1] = new BigInteger(""+value1);
		for(int k=2, crt=1; k<value.length; k++) {
			value[k]=BigInteger.ZERO;
			while(b_value[crt]<0) {
				value[k] = value[k].shiftLeft(7);
				value[k] = value[k].or(new BigInteger(""+(b_value[crt++]+128)));//get_u32(b_value[crt++])-128;
			}
			value[k] = value[k].shiftLeft(7);
			value[k] = value[k].or(new BigInteger(""+(b_value[crt++])));//get_u32(b_value[crt++]);
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
		if(getTypeByte()==Encoder.TAG_NULL) return null;
		assert((data[offset]==Encoder.TAG_IA5String)||
				(data[offset]==Encoder.TAG_UTF8String)||
				(data[offset]==Encoder.TAG_PrintableString));
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
		if(data[offset]!=type) throw new ASN1DecoderFail("String: "+type+" != "+data[offset]);
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
		return Util.getCalendar(this.getGeneralizedTime(type));
	}
	/**
	 * Only works assuming type = Encoder.TAG_GeneralizedTime
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Calendar getGeneralizedTimeCalender_() throws ASN1DecoderFail {
		return Util.getCalendar(this.getGeneralizedTime(Encoder.TAG_GeneralizedTime));
	}
	/**
	 * Only works assuming type previously tested
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public Calendar getGeneralizedTimeCalenderAnyType() throws ASN1DecoderFail {
		return Util.getCalendar(this.getGeneralizedTimeAnyType());
	}
	public boolean fetchAll(InputStream is) throws IOException {
		//Decoder dec = new Decoder(sr,0,msglen);
		while(true) {
			int asrlen = objectLen();
			if(DEBUG)System.out.println("Object size="+asrlen+"; Buffer size="+data.length+"; Current size="+length);
			if((asrlen>0) && (asrlen>data.length)){
				System.out.println("Object size="+asrlen+"; Buffer size="+data.length);
				return false;
			}
			if ((asrlen<0) || (length < asrlen)) {
				if(DEBUG)System.out.println("Object size="+asrlen+"; Current size="+length);
				if(length == data.length) return false;
				int inc = is.read(data, length, data.length-length);
				if(inc == 0) return false;
				length += inc;
				//dec = new Decoder(data,0,length);
				continue;
			}
			break;
		}
		return true;
	}
	public int getMSGLength() {
		return length;
	}
    public String dumpHexDump() {
        return dumpHex(Util.MAX_DUMP);
    }
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
}

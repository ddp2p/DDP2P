/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

import static util.Util.__;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ASN1.ASN1DecoderFail;
import config.Application_GUI;
import config.DD;

public class EmbedInMedia {

	private final static boolean _DEBUG = true;
	public static boolean DEBUG = false;

	public static final int STEGO_BITS = 4;
	//public final static int STEGO_PIX_HEADER=12;
	public final static short STEGO_BYTE_HEADER=48;//STEGO_PIX_HEADER*3;
	public static final int STEGO_LEN_OFFSET = 8;
	public static final int STEGO_SIGN_OFFSET = 0;
	public static final int STEGO_OFF_OFFSET = 4;

	/**
	 * Generates random picture and inserts data of type content_type in it
	 * @param file
	 * @param adr_bytes
	 * @param content_type
	 * @throws IOException
	 */
	public static void saveSteganoBMP(File file, byte[] adr_bytes, short content_type) throws IOException {
		FileOutputStream fo=new FileOutputStream(file);
		fo.write(createSteganoBMP(adr_bytes, content_type));
		//System.out.println("Wrote: "+Util.byteToHex(adr_bytes, " "));
		//System.out.println("Got: "+Util.byteToHex(steg_buffer, " "));
		fo.close();
	}

	public static byte[] createSteganoBMP(byte[] adr_bytes, short content_type) {
		int offset = BMP.DATA;
		int word_bytes=1;
		int bits = 4;
		int Bpp = 3;
		int datasize;// = adr_bytes.length*(8/bits);
		int height = Util.ceil(Math.sqrt(((adr_bytes.length+EmbedInMedia.STEGO_BYTE_HEADER*word_bytes)<<3)/(Bpp*bits))); //10;
		height += 8-height%8;
		int width = EmbedInMedia.getWidth(adr_bytes.length+EmbedInMedia.STEGO_BYTE_HEADER*word_bytes, bits, Bpp, height);
		width += 8-width%8;
		if((width&8) == 0) width+=8;
		if(DEBUG)System.out.println("size="+adr_bytes.length+" width="+width+" h="+height);
		datasize = width*height*3;
		byte[]steg_buffer = new byte[BMP.DATA+datasize];
		BMP data = new BMP(width, height);
		////data.creator = adr_bytes.length;
		data.getHeader(steg_buffer, 0);
		/**
		 * Generate the random pattern
		 */
		if(DEBUG) System.out.println("startdata="+data.startdata);
		int method = (int)Util.random(16);
		for(int k=data.startdata; k<steg_buffer.length; k+=3){
			int p = (k-data.startdata)/3;
			int h = p/width;
			int w = p%width;
			byte v;
			if((method&1)!=0){
				v = (byte)(255*(((p+method)>>3)&1));
				steg_buffer[k]=v;steg_buffer[k+1]=v;steg_buffer[k+2]=v;
			}else{
				v = (byte)(255*(((h>>3)&1)^((w>>3)&1)));
				if(v!=0){
					steg_buffer[k]=v;steg_buffer[k+1]=v;steg_buffer[k+2]=v;
				}else{
					if((w&16)==0)steg_buffer[k]=(byte)255;
					if((w&16)==1)steg_buffer[k+1]=(byte) 128;//(Math.floor(Util.random(256))); 
					if((h&16)==0)steg_buffer[k+2]=(byte)255;
				}
			}
		}
		//System.out.println("Got bytes to write: "+Util.byteToHex(adr_bytes, " "));
		//System.out.println("After header: "+Util.byteToHex(steg_buffer, " "));
		return EmbedInMedia.getSteganoBytes(adr_bytes, steg_buffer,
				offset, word_bytes, bits, content_type);
	}

	public static boolean cannotEmbedInBMPFile(File file,
			byte[] adr_bytes,
			String explain[],
			byte[][] _b,
			BMP[] _data) {
		//System.out.println("EmbedInMedia:BMP file="+file);
		//System.out.println("EmbedInMedia:BMP file len="+file.length());
		boolean fail= false;
		if((explain==null) || (explain.length<1)) explain=new String[1];
		try {
			if(file.length() > DD.LARGEST_BMP_FILE_LOADABLE){
				explain[0] = __("The image file is too large!");
				return fail = true;
			}
			FileInputStream fis = new FileInputStream(file);
			_b[0] = new byte[(int) file.length()];  
			fis.read(_b[0]);
			fis.close();
			//System.out.println("EmbedInMedia:BMP length="+_b[0].length);
			BMP data = _data[0] = new BMP(_b[0], 0);
			if((data.compression!=BMP.BI_RGB)||(data.bpp<24)){
				explain[0] = __("Not supported compression: "+data.compression+" "+data.bpp);
				fail = true;
			}
			if(data.width*data.height*3<(adr_bytes.length*8/EmbedInMedia.STEGO_BITS)+EmbedInMedia.STEGO_BYTE_HEADER){
				explain[0] = __("File too short: "+data.width*data.height*3+" need: "+adr_bytes.length);
				fail = true;
			}
		} catch (FileNotFoundException e1) {
			fail = true;
			explain[0] = e1.getLocalizedMessage();
		} catch (IOException e2) {
			fail = true;
			explain[0] = e2.getLocalizedMessage();
		}
		return fail;
	}

	
//	@Deprecated
//	public static byte[] getSteganoBytes(byte[] ddb, byte[] stg,
//			int offset, int word_bytes, int bits) {
//		return getSteganoBytes(ddb, stg, offset, word_bytes, bits, DD.STEGO_SIGN_PEER);
//	}

	public static byte[] getSteganoBytes(byte[] ddb, byte[] stg,
			int offset, int word_bytes, int bits, short content_type) {
		byte[] len = new byte[4];
		Util.copyBytes(len, 0, ddb.length);
		EmbedInMedia.getSteganoBytesRaw(len, stg, offset+EmbedInMedia.STEGO_LEN_OFFSET, word_bytes, bits);
		byte[] sign = new byte[2];
		Util.copyBytes(sign, 0, content_type);
		EmbedInMedia.getSteganoBytesRaw(sign, stg, offset+EmbedInMedia.STEGO_SIGN_OFFSET, word_bytes, bits);
		Util.copyBytes(sign, 0, EmbedInMedia.STEGO_BYTE_HEADER);
		EmbedInMedia.getSteganoBytesRaw(sign, stg, offset+EmbedInMedia.STEGO_OFF_OFFSET, word_bytes, bits);
		return EmbedInMedia.getSteganoBytesRaw(ddb, stg, offset+EmbedInMedia.STEGO_BYTE_HEADER*word_bytes, word_bytes, bits);
	}

	/**
	 *  Fills buffer with bytes from result
	 * @param ddb input full bytes data
	 * @param stg destination steganodata buffer 
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @return filled stg
	 */
	public static byte[] getSteganoBytesRaw(byte[] ddb, byte[] stg,
			int offset, int word_bytes, int bits) {
		int crt_src=0;
		int crt_bit=0;
		int crt_dst=offset;
		for(;;crt_dst+=word_bytes) {
			if(crt_src >= ddb.length) return stg;
			if(crt_dst >= stg.length) return stg;
			int available_bits = Math.min(8-crt_bit, bits);
			//System.out.println(crt_src+" CRT src: "+Util.getHEX(ddb[crt_src])+" crt="+crt_bit+" av="+available_bits);
			byte b1 = (byte) ((ddb[crt_src]>>crt_bit));
			byte b2 = (byte) (((1<<available_bits) - 1));
			byte b = (byte) ((ddb[crt_src]>>crt_bit) & ((1<<available_bits) - 1));
			//System.out.println(" CRT b: "+Util.getHEX(b)+" CRT b1: "+Util.getHEX(b1)+" CRT b2: "+Util.getHEX(b2));
			crt_bit += available_bits;
			if(crt_bit >=8) {crt_src++; crt_bit=0;}
			if(available_bits<bits) {
				//crt_src++;
				if(crt_src >= ddb.length) return stg;
				//System.out.println(crt_src+" + CRT src: "+Util.getHEX(ddb[crt_src]));
				crt_bit=bits-available_bits;
				b |= (byte) ( (ddb[crt_src] & ((1<<crt_bit) - 1)) << available_bits );
				//System.out.println(" + CRT b: "+Util.getHEX(ddb[crt_src]));
			}
			//System.out.print(crt_dst+" Old: "+Util.getHEX(stg[crt_dst])+" <- "+Util.getHEX(b));
			stg[crt_dst] &= (byte) ~((1<<bits) - 1);
			stg[crt_dst] |= b;
			//System.out.println(" New: "+Util.getHEX(stg[crt_dst]));
		}
	}

	/**
	 *  Convert stegano bytes in pure bytes
	 * @param buffer
	 * @param offset
	 * @param word_bytes : bytes/color (amount to jump before next byte)
	 * @param bits : how many bits are used per word
	 * @param bytes_len : how many bytes to extract
	 * @return newly created buffer
	 */
	public static byte[] extractSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits, int bytes_len){
		byte[]result = new byte[bytes_len];
		int crt_src=offset;
		int crt_dst=0;
		int carry = 0;
		int carry_bits=0;
		for(;crt_dst<result.length;crt_dst++) {
			do{
				if(crt_src >= buffer.length) break;
				byte b = (byte) (buffer[crt_src]&((1<<bits) - 1));
				//carry = b|(carry<<bits);
				carry = carry | (b<<carry_bits);
				carry_bits+=bits;
				crt_src += word_bytes;
			}while(carry_bits<8);
			if(carry_bits == 0) break;
			result[crt_dst]= (byte)(carry & 0x0ff);
			carry_bits -= 8;
			carry = (carry>>8) & ((1<<carry_bits) - 1);
		}
		if(DD_Address.DEBUG) System.out.println("Extracted: "+Util.byteToHex(result, " "));
		return result;
	}

	/**
	 *  Extract all stegano bytes (potentially more than encoded)
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @return
	 */
	public static byte[] extractSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits){
		int bytes_len = (int)Math.round(Math.ceil(bits*(buffer.length - offset)/(word_bytes*8.0)));
		return extractSteganoBytes(buffer, offset, word_bytes, bits, bytes_len);
	}
	public static int getSteganoSize(int size, int bits){
		return (int)Math.round(Math.ceil(size*8.0/bits));
	}

	public static int getWidth(int size, int bits, int Bpp, int height){
		return (int)Math.round(Math.ceil(getSteganoSize(size,bits)/(Bpp*height*1.0)));
	}

	public static byte[] getSteganoBytesAlocBuffer(byte[] ddb, int offset, int word_bytes, int bits, short content_type) {
		byte[] stg = new byte[offset+(int)Math.ceil(ddb.length/(double)bits)*word_bytes];
		return getSteganoBytes(ddb, stg, offset, word_bytes, bits, content_type);
	}

	public static boolean verif_offset_interactive(short _off){
		if(_off != STEGO_BYTE_HEADER) {
			int n = Application_GUI.ask(__("Accept code: ")+_off+"!="+STEGO_BYTE_HEADER,
	    			__("Accept old file?"), Application_GUI.YES_NO_OPTION);
			if (n != Application_GUI.YES_OPTION)
	    		return false;
		}
		return true;
	}

	public static void verif_steg_sign(StegoStructure[]d, int selected[], byte[]buffer, int offset, int word_bytes, int bits) throws ASN1DecoderFail{
		// boolean DEBUG = true;
		if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: start");
		byte[] sign=extractSteganoBytes(buffer, offset+STEGO_SIGN_OFFSET, word_bytes, bits, 2);
		short _sign = 0;
		_sign = Util.extBytes(sign, 0, _sign);
		if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: sign = "+_sign);
		
		int[] types = new int[d.length];
		for(int k=0; k<d.length; k++){
			types[k] = d[k].getSignShort();
			if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: sign["+k+"]="+types[k]);
		}
		for(int k=0; k<d.length; k++){
			types[k] = d[k].getSignShort();
			if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: sign = "+_sign+" vs "+types[k]);
			if(_sign==types[k]){
				if((selected!=null)&&(selected.length>0)) selected[0]= k;
				if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: selected = "+k);
				return;
			}
		}
		//if(_sign!=DD.STEGO_SIGN_PEER) 
		if(DEBUG) System.out.println("EmbedInMedia:verif_steg_sign: done NO SIGNATURE");
		throw new ASN1DecoderFail("Wrong SIGNATURE! "+_sign);
	}

	public static int verif_steg_length(byte[]buffer, int offset, int word_bytes, int bits){
		byte[] len=extractSteganoBytes(buffer, offset+STEGO_LEN_OFFSET, word_bytes, bits, 4);
		int bytes_len=0;
		bytes_len = Util.extBytes(len, 0, bytes_len);
		return bytes_len;
	}

	public static short verif_steg_offset(byte[]buffer, int offset, int word_bytes, int bits){
		byte[] off=extractSteganoBytes(buffer, offset+STEGO_OFF_OFFSET, word_bytes, bits, 4);
		short _off=0;
		_off = Util.extBytes(off, 0, _off);	
		return _off;
	}

	public static byte[] _fromBMPStreamSave(InputStream in) throws IOException, ASN1DecoderFail, P2PDDSQLException {
		return _fromBMPStreamSave(in,
				DD.getAvailableStegoStructureISignatures(),
				//new short[]{DD.STEGO_SIGN_PEER, DD.STEGO_SIGN_CONSTITUENT_VERIF_REQUEST},
				null);
	}
	public static byte[] _fromBMPStreamSave(InputStream in, short[] content_types, short[] content_type) throws IOException, ASN1DecoderFail, P2PDDSQLException {
		int k;
		short sign_val=0, off_val=0;
		byte[] bmp= new byte[BMP.DATA];
		//boolean DEBUG=true;
		if(DD_Address.DEBUG) System.err.println("fromBMPStreamSave: will read header");
		k=Util.readAll(in, bmp);
		if(k<BMP.DATA) throw new IOException("EOF BMP Header");
		BMP bmpheader = new BMP(bmp,0);
		int startdata = bmpheader.startdata;
		byte useless[] = new byte[startdata-BMP.DATA];
		if(useless.length>0) {k=Util.readAll(in, useless); if(k<useless.length) throw new IOException("EOF useless Header");}
		
		byte[] stg = new byte[4];
		byte[] sign;
		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF sign Header");
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS,2);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got mac signature: "+k+"..."+Util.byteToHex(sign, " "));
		sign_val = Util.extBytes(sign, 0, sign_val);

		if((content_type!=null)&&(content_type.length>0)) content_type[0] = sign_val;
		if(Util.contains(content_types, sign_val) == -1){
			throw new IOException("BAD sign Header: "+sign_val);
		}
	
		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF off Header");
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS,2);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got mac offset: "+k+"..."+Util.byteToHex(sign, " "));
		off_val = Util.extBytes(sign, 0, off_val);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got offset: "+off_val);
		
		int length_val=0;
		stg = new byte[8];
		//byte[] sign;
		k=Util.readAll(in, stg); if(k<stg.length) throw new IOException("EOF length Header: "+k);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS, 4);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"..."+Util.byteToHex(sign, " "));
		length_val = Util.extBytes(sign, 0, length_val);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got length: "+length_val);
		
		byte skipped[]=new byte[off_val-16];
		k=Util.readAll(in, skipped);if(k<skipped.length) throw new IOException("EOF skipped Header");
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got skipped: "+skipped.length);
		
		stg=new byte[Util.ceil(length_val*8.0/STEGO_BITS)];
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Will read bmp: "+stg.length);
		//k=in.read(stg);
		k=Util.readAll(in,stg);
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got bmp: "+k);
		if(k<stg.length){
			if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"<"+stg.length);
			throw new IOException("EOF data");
		}
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS, length_val);
		return sign;
	}
	/**
	 * 
	 * @param in
	 * @param d
	 * @param selected
	 * @return
	 * @throws IOException
	 * @throws ASN1DecoderFail
	 * @throws P2PDDSQLException
	 */
	public static boolean fromBMPStreamSave(InputStream in, StegoStructure[] d, int[] selected)
			throws IOException, ASN1DecoderFail, P2PDDSQLException {
		short[] types = new short[d.length];
		for(int k=0; k<types.length; k++) types[k] = d[k].getSignShort();
		short[] type = new short[1];
		byte []sign = _fromBMPStreamSave(in, types, type);
		int k = Util.contains(types, type[0]);
		if((selected != null) && (selected.length > 0)) selected[0] = k;
		if(k == -1) return false;
		d[k].setBytes(sign);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got data: "+d);
		//d[k].save();
		new util.DDP2P_ServiceThread("Stego Saver BMPStream", false, d[k]) {
			public void _run() {
				try {
					((StegoStructure)ctx).save();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}.start();
		if(DEBUG) System.out.println("fromBMPStreamSave: Done");
		return true;
	}
	/**
	 * To get a DDAddress
	 * @param in
	 * @param d
	 * @return
	 * @throws IOException
	 * @throws ASN1DecoderFail
	 * @throws P2PDDSQLException
	 */
	
	@Deprecated
	public static boolean fromBMPStreamSave(InputStream in, StegoStructure d) throws IOException, ASN1DecoderFail, P2PDDSQLException {
		byte []sign = _fromBMPStreamSave(in);
		//DDAddress data = new DDAddress();
		//data.
		d.setBytes(sign);
		//System.out.println("Got DDAddress: "+data);
		//data.
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got bytes ");
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Got address: "+d);
		//d.save();
		new util.DDP2P_ServiceThread("Stego Saver BMPStream", false, d) {
			public void _run() {
				try {
					((StegoStructure)ctx).save();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}.start();
		if(DD_Address.DEBUG) System.out.println("fromBMPStreamSave: Done");
		
		return true;
	}
	
	public static boolean fromBMPFileSave(File file, StegoStructure d[], int[]selected) throws IOException, P2PDDSQLException{
		String explain="";
		boolean fail= false;
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		fis.read(b);
		fis.close();
		BMP data = new BMP(b, 0);
	
		if((data.compression!=BMP.BI_RGB) || (data.bpp<24)){
			explain = " - "+__("Not supported compression: "+data.compression+" "+data.bpp);
			fail = true;
		}else{
			int offset = data.startdata;
			int word_bytes=1;
			int bits = 4;
			try {
				EmbedInMedia.setSteganoBytes(d, selected, b, offset, word_bytes, bits);
			} catch (Exception e1) {
				explain = " - "+ __("No valid data in picture!");
				fail = true;
			}
		}
		if(fail) throw new IOException(explain);
		if (selected[0] != -1) {
			new util.DDP2P_ServiceThread("Stego Saver BMPFile", false, d[selected[0]]) {
				public void _run() {
					try {
						((StegoStructure)ctx).save();
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
		return true;
	}

	/**
	 * Init DDAddres from BMP data
	 * Performs first some standard verifications (which could be factored out...)
	 * 
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @throws ASN1DecoderFail
	 */
	public static void setSteganoBytes(StegoStructure d[], int[]selected, byte[]buffer, int offset, int word_bytes, int bits) throws ASN1DecoderFail{
		if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes: start");
		verif_steg_sign(d, selected, buffer, offset, word_bytes, bits);
		int bytes_len=verif_steg_length(buffer, offset, word_bytes, bits);
		short _off=verif_steg_offset(buffer, offset, word_bytes, bits);
		if(!verif_offset_interactive(_off)) return;
		int final_offset = offset+_off*word_bytes;
		
		if(selected[0] != -1) {
			EmbedInMedia.setSteganoBytes(d[selected[0]], buffer, final_offset, word_bytes, bits, bytes_len);
			if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes: set");
		}else{
			if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes: empty");			
		}
		if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes: end");
		//setBytes(extractSteganoBytes(buffer, offset, word_bytes, bits));
	}

	/**
	 * Init DDAddress from a buffer of stegano bytes of known useful length and offset
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @param bytes_len
	 * @throws ASN1DecoderFail
	 */
	public static void setSteganoBytes(StegoStructure d, byte[]buffer, int offset, int word_bytes, int bits, int bytes_len) throws ASN1DecoderFail{
		if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes(d): start");
		byte[] a = extractSteganoBytes(buffer, offset, word_bytes, bits, bytes_len);
		if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes(d): will set Bytes");
		d.setBytes(a);
		if(DEBUG) System.out.println("EmbedInMedia:setSteganoBytes(d): done");
	}
	public static byte[] getSteganoBytes(StegoStructure d, int offset, int word_bytes, int bits) {
		byte[] ddb = d.getBytes();
		return getSteganoBytesAlocBuffer(ddb, offset, word_bytes, bits, d.getSignShort());
	}

	public static byte[] getSteganoBytes(StegoStructure d, byte[] stg, int offset, int word_bytes, int bits) {
		byte[] ddb = d.getBytes();
		return getSteganoBytes(ddb, stg, offset, word_bytes, bits, d.getSignShort());
	}

}

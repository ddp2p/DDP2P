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


package hds;

import static util.Util._;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import util.BMP;
import util.DD_IdentityVerification_Request;
import util.P2PDDSQLException;
import util.Util;
import ASN1.ASN1DecoderFail;
import config.Application;
import config.DD;
import data.D_PeerAddress;

public class EmbedInMedia {

	private final static boolean _DEBUG = true;
	private static final boolean DEBUG = false;

	public static final int STEGO_BITS = 4;
	//public final static int STEGO_PIX_HEADER=12;
	public final static short STEGO_BYTE_HEADER=48;//STEGO_PIX_HEADER*3;
	public static final int STEGO_LEN_OFFSET = 8;
	public static final int STEGO_SIGN_OFFSET = 0;
	public static final int STEGO_OFF_OFFSET = 4;

	/**
	 * Exporting current Address
	 * use test just to verify parsing (can be null)
	 */
	static void actionExport(JFileChooser fc, Component parent,
			StegoStructure myAddress, StegoStructure test){
		if(DEBUG)System.out.println("EmbedInMedia:actionExport:"+myAddress);
		if(myAddress == null){
			if(ControlPane.DEBUG) System.out.println("EmbedInMedia:actionExport: no address");
			return;
		}
		if(ControlPane.DEBUG) System.out.println("EmbedInMedia:actionExport: Got to write: "+myAddress);
		BMP[] _data=new BMP[1];
		byte[][] _buffer_original_data=new byte[1][]; // old .bmp file 
		byte[] adr_bytes = myAddress.getBytes();
		if(test != null){
			try{
				test.setBytes(adr_bytes);
			}catch (ASN1DecoderFail e1) {
								e1.printStackTrace();
								Application.warning(_("Failed to parse file: \n"+e1.getMessage()), _("Failed to parse address!"));
								return;
							}
		}
		if(ControlPane.DEBUG) System.out.println("EmbedInMedia:actionExport: Got bytes("+adr_bytes.length+"): to write: "+Util.byteToHex(adr_bytes, " "));
		int returnVal = fc.showSaveDialog(parent);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	fc.setName(_("Select file with image or text containing address"));
	        File file = fc.getSelectedFile();
	        String extension = Util.getExtension(file);
	        if(!"bmp".equals(extension)&&!"txt".equals(extension)&&!"ddb".equals(extension)&&!"gif".equals(extension))  {
	        	file = new File(file.getPath()+".bmp");
	        	extension = "bmp";
	        }
	
	       if(file.exists()){
	        	//Application.warning(_("File exists!"));
	        	
	        	JOptionPane optionPane = new JOptionPane(_("Overwrite/Embed in: ")+file+"?",
	        			JOptionPane.QUESTION_MESSAGE,
	        			JOptionPane.YES_NO_OPTION);
	        	int n;
	        	if("gif".equals(extension) && file.isFile()) {
	        		try{
	        			FileOutputStream fos = new FileOutputStream(file, true);
	        			fos.write(adr_bytes);
	        			fos.close();
	        		} catch(FileNotFoundException ex) {
					    System.out.println("EmbedInMedia:actionExport: FileNotFoundException : " + ex);
					} catch(IOException ioe){
					    System.out.println("EmbedInMedia:actionExport: IOException : " + ioe);
					}
	        	}
	        	if("bmp".equals(extension) && file.isFile()) {
					//FileInputStream fis;
					boolean fail= false;
					String _explain[]=new String[]{""};
					fail = cannotEmbedInBMPFile(file, adr_bytes, _explain, _buffer_original_data, _data);
					if(fail)
						JOptionPane.showMessageDialog(parent,
							_("Cannot Embed address in: ")+file+" - "+_explain,
							_("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
							
	        		n = JOptionPane.showConfirmDialog(parent, _("Embed address in: ")+file+"?",
	            			_("Overwrite prior details?"), JOptionPane.YES_NO_OPTION,
	            			JOptionPane.QUESTION_MESSAGE);
	        	}else{
	        		n = JOptionPane.showConfirmDialog(parent, _("Overwrite: ")+file+"?",
	        			_("Overwrite?"), JOptionPane.YES_NO_OPTION,
	        			JOptionPane.QUESTION_MESSAGE);
	        	}
	        	if(n!=JOptionPane.YES_OPTION)
	        		return;
	        	//Application.warning(_("File exists!"));
	        }
	        try {
				if("txt".equals(extension)) {
					BufferedWriter out = new BufferedWriter(new FileWriter(file));
					out.write(myAddress.getString());
					out.close();
				}else
				if("ddb".equals(extension)){
					FileOutputStream fo=new FileOutputStream(file);
					fo.write(myAddress.getBytes());
					fo.close();
				}else
					if("bmp".equals(extension)){
						if(DEBUG )System.out.println("EmbedInMedia:actionExport:bmp");
						if(!file.exists()) {
							saveSteganoBMP(file, adr_bytes, myAddress.getSignShort()); //DD.STEGO_SIGN_PEER);
						}else{
							FileOutputStream fo=new FileOutputStream(file);
							int offset = _data[0].startdata;
							int word_bytes=1;
							int bits = 4;
							////Util.copyBytes(b, BMP.CREATOR, adr_bytes.length);
							fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, _buffer_original_data[0], offset, word_bytes, bits, myAddress.getSignShort()));
							fo.close();
						}
					}
			} catch (IOException e1) {
				Application.warning(_("Error writing file:")+file+" - "+ e1,_("Export Address"));
			}
	    }		
	}
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

	private static boolean cannotEmbedInBMPFile(File file,
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
				explain[0] = _("The image file is too large!");
				return fail = true;
			}
			FileInputStream fis = new FileInputStream(file);
			_b[0] = new byte[(int) file.length()];  
			fis.read(_b[0]);
			fis.close();
			//System.out.println("EmbedInMedia:BMP length="+_b[0].length);
			BMP data = _data[0] = new BMP(_b[0], 0);
			if((data.compression!=BMP.BI_RGB)||(data.bpp<24)){
				explain[0] = _("Not supported compression: "+data.compression+" "+data.bpp);
				fail = true;
			}
			if(data.width*data.height*3<(adr_bytes.length*8/EmbedInMedia.STEGO_BITS)+EmbedInMedia.STEGO_BYTE_HEADER){
				explain[0] = _("File too short: "+data.width*data.height*3+" need: "+adr_bytes.length);
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

	static void actionImport(JFileChooser fc, Component parent, StegoStructure[] adr, int[] selected) throws P2PDDSQLException{
		if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: import file");
		int returnVal = ControlPane.file_chooser_address_container.showOpenDialog(parent);
		if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: selected");
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: ok");
	        File file = fc.getSelectedFile();
	        if(!file.exists()){
	        	Application.warning(_("The file does not exists: "+file),_("Importing Address")); return;
	        }
	        try {
	        	if("txt".equals(Util.getExtension(file))) {
	        		if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: txt");
					String content = new Scanner(file).useDelimiter("\\Z").next();
					int _selected = -1;
					for(int k=0; k<adr.length; k++) {
						if(adr[k].parseAddress(content)){
							_selected = k;
							break;
						}
					}
					if(_selected == -1) {
						Application.warning(_("Failed to parse file: "+file), _("Failed to parse address!"));
						return;
					}
					if((selected!=null)&&(selected.length>0)) selected[0] = _selected;
				}else if("gif".equals(Util.getExtension(file))){
						if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: gif");
						FileInputStream fis=new FileInputStream(file);
						boolean found = false;
						byte[] b = new byte[(int) file.length()];  
						fis.read(b);
				    	fis.close();
				    	int i=0;
						while (i<b.length){
							if(b[i]==(byte) 0x3B) {
								found = true;
								i++;
								break;
							}
							i++;
						}
						if(i>=b.length){
							JOptionPane.showMessageDialog(parent,
										_("Cannot Extract address in: ")+file+_("No valid data in the picture!"),
										_("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
									return;
						}
						byte[] addBy = new byte[b.length-i]; 
						System.arraycopy(b,i,addBy,0,b.length-i);
						// System.out.println("Got bytes ("+addBy.length+") to write: "+Util.byteToHex(addBy, " "));
						
						int _selected = -1;
						for(int k=0; k<adr.length; k++) {
							try {
								adr[k].setBytes(addBy);
								_selected = k;
								break;
							} catch (ASN1DecoderFail e1) {
								if(DEBUG){
									e1.printStackTrace();
									Application.warning(_("Failed to parse file: "+file+"\n"+e1.getMessage()), _("Failed to parse address!"));
								}
							}
						}
						if(_selected == -1){
							Application.warning(_("Failed to parse file: "+file+"\n"), _("Failed to parse address!"));
							return;							
						}
						if((selected!=null)&&(selected.length>0)) selected[0] = _selected;
				}
				else
				if("ddb".equals(Util.getExtension(file))){
					if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: ddb");
					FileInputStream fis=new FileInputStream(file);
					byte[] b = new byte[(int) file.length()];  
					fis.read(b);
					fis.close();
					int _selected = -1;
					for(int k=0; k<adr.length; k++) {
						try {
							adr[k].setBytes(b);
							_selected = k;
							break;
						} catch (ASN1DecoderFail e1) {
							if(DEBUG){
								e1.printStackTrace();
								Application.warning(_("Failed to parse file: "+file+"\n"+e1.getMessage()), _("Failed to parse address!"));
							}
						}
					}
					if(_selected == -1){
						Application.warning(_("Failed to parse file: "+file+"\n"), _("Failed to parse address!"));
						return;							
					}
					if((selected!=null)&&(selected.length>0)) selected[0] = _selected;
				}else
					if("bmp".equals(Util.getExtension(file))) {
						//System.err.println("Got: bmp");
						String explain="";
						boolean fail= false;
						FileInputStream fis=new FileInputStream(file);
						//System.err.println("Got: open");
						//System.err.println("Got: open size:"+file.length());
						byte[] b = new byte[(int) file.length()];
						//System.err.println("Got: alloc="+b.length);
						fis.read(b);
						//System.err.println("Got: read");
						fis.close();
						//System.err.println("Got: close");
						//System.out.println("File data: "+Util.byteToHex(b,0,200," "));
						BMP data = new BMP(b, 0);
						//System.out.println("BMP Header: "+data);
	
						if((data.compression!=BMP.BI_RGB) || (data.bpp<24)){
							explain = " - "+_("Not supported compression: "+data.compression+" "+data.bpp);
							fail = true;
						}else{
							int offset = data.startdata;
							int word_bytes=1;
							int bits = 4;
							try {
								//System.err.println("Got: steg");
								////adr.setSteganoBytes(b, offset, word_bytes, bits,data.creator);
								EmbedInMedia.setSteganoBytes(adr, selected, b, offset, word_bytes, bits);
							} catch (ASN1DecoderFail e1) {
								explain = " - "+ _("No valid data in picture!");
								fail = true;
							}
						}
						if(fail){
								JOptionPane.showMessageDialog(parent,
									_("Cannot Extract address in: ")+file+explain,
									_("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
								return;
						}
					}
	        	if(selected[0] != -1) {
					if(ControlPane.DEBUG)System.err.println("Got Data: "+adr[selected[0]]);
		        	Application.warning(adr[selected[0]].getNiceDescription(), _("Obtained Data"));
		        	adr[selected[0]].save();
	        	}
	        }catch(IOException e3){
	        	
	        }
	    }		
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
		if(DDAddress.DEBUG) System.out.println("Extracted: "+Util.byteToHex(result, " "));
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
	@Deprecated
	public static byte[] _setSteganoImage(BufferedImage bi) throws ASN1DecoderFail, P2PDDSQLException{
		return _setSteganoImage(bi,
				DD.getAvailableStegoStructureISignatures(),
				//new short[]{DD.STEGO_SIGN_PEER, DD.STEGO_SIGN_CONSTITUENT_VERIF_REQUEST},
				null);
	}
	/**
	 * Extract content_type from image. Store its type in content_type[0]
	 * if non-null. Fail if the type is not in content_types
	 * @param bi
	 * @param content_type
	 * @return
	 * @throws ASN1DecoderFail
	 * @throws P2PDDSQLException
	 */
	public static byte[] _setSteganoImage(BufferedImage bi, short[] content_types, short[] content_type)
			throws ASN1DecoderFail, P2PDDSQLException{
		byte[] sign= Util.getBytes(bi,EmbedInMedia.STEGO_SIGN_OFFSET,
				Util.ceil(2*8/EmbedInMedia.STEGO_BITS));
		System.out.println("Got image sign bytes: "+Util.byteToHex(sign, " "));
		/*
		System.out.println("Got image type: "+bi.getType()+" ==?"
				+BufferedImage.TYPE_INT_RGB+" "
				+BufferedImage.TYPE_3BYTE_BGR+" "
				+BufferedImage.TYPE_4BYTE_ABGR+" "
				+BufferedImage.TYPE_4BYTE_ABGR_PRE+" "
				+BufferedImage.TYPE_INT_ARGB+" "
				+BufferedImage.TYPE_INT_BGR+" "
				+BufferedImage.TYPE_USHORT_555_RGB+" "
				+BufferedImage.TYPE_BYTE_BINARY+" "
				+BufferedImage.TYPE_BYTE_GRAY+" "
				+BufferedImage.TYPE_BYTE_INDEXED+" "
				+BufferedImage.TYPE_CUSTOM+" "
				+BufferedImage.TYPE_INT_ARGB_PRE+" "//
				+BufferedImage.TYPE_USHORT_555_RGB+" "
				+BufferedImage.TYPE_USHORT_565_RGB+" "
				+BufferedImage.TYPE_USHORT_GRAY
				);
				*/
		byte[] signature = extractSteganoBytes(sign, 0, 1,
				EmbedInMedia.STEGO_BITS, 2);
		short signature_val = 0;
		signature_val=Util.extBytes(signature, 0, signature_val);
		if((content_type!=null)&&(content_type.length>0)) content_type[0] = signature_val;
		if(Util.contains(content_types, signature_val) == -1){
			JOptionPane.showMessageDialog(JFrameDropCatch.mframe,
	    			_("When trying to use locally saved image got Wrong Signature: "+signature_val+
	    					"\nThe source of the drag might have changed the image content (like Safari/use Firefox!). " +
	    					"\n"+_("We will try other methods")+
	    					"\nYou can also save the file and drag/load it as a file."),
	    			_("Wrong signature"), JOptionPane.WARNING_MESSAGE);
			return null;
		}
		
		byte[] off= Util.getBytes(bi,EmbedInMedia.STEGO_OFF_OFFSET,
				Util.ceil(2*8/EmbedInMedia.STEGO_BITS));
		byte[] offset = extractSteganoBytes(off, 0, 1,
				EmbedInMedia.STEGO_BITS, 2);
		short offset_val = 0;
		offset_val=Util.extBytes(offset, 0, offset_val);
		if(offset_val!=EmbedInMedia.STEGO_BYTE_HEADER){
			int n = JOptionPane.showConfirmDialog(JFrameDropCatch.mframe, _("Accept code: ")+offset_val+"!="+EmbedInMedia.STEGO_BYTE_HEADER,
					_("Accept old file?"), JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if(n!=JOptionPane.YES_OPTION)
				return null;
		}
		
		byte[] len= Util.getBytes(bi,EmbedInMedia.STEGO_LEN_OFFSET,
				Util.ceil(4*8/EmbedInMedia.STEGO_BITS));
		byte[] length = extractSteganoBytes(len, 0, 1,
				EmbedInMedia.STEGO_BITS, 4);
		int bytes_len = 0;
		bytes_len=Util.extBytes(length, 0, bytes_len);
		
		//System.err.println("Imglen:"+Util.byteToHex(len, " ")+" l="+bytes_len);
		int stegoBytes = Util.ceil((bytes_len*8)/EmbedInMedia.STEGO_BITS);
		byte[] useful= Util.getBytes(bi, offset_val, stegoBytes);
		if(DEBUG)System.out.println("StegData:"+Util.byteToHex(useful, " "));
		byte datab[] = extractSteganoBytes(useful, 0, 1, EmbedInMedia.STEGO_BITS, bytes_len);
		return datab;
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
			int n = JOptionPane.showConfirmDialog(null, _("Accept code: ")+_off+"!="+STEGO_BYTE_HEADER,
	    			_("Accept old file?"), JOptionPane.YES_NO_OPTION,
	    			JOptionPane.QUESTION_MESSAGE);
			if(n!=JOptionPane.YES_OPTION)
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
		if(DDAddress.DEBUG) System.err.println("fromBMPStreamSave: will read header");
		k=Util.readAll(in, bmp);
		if(k<BMP.DATA) throw new IOException("EOF BMP Header");
		BMP bmpheader = new BMP(bmp,0);
		int startdata = bmpheader.startdata;
		byte useless[] = new byte[startdata-BMP.DATA];
		if(useless.length>0) {k=Util.readAll(in, useless); if(k<useless.length) throw new IOException("EOF useless Header");}
		
		byte[] stg = new byte[4];
		byte[] sign;
		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF sign Header");
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS,2);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got mac signature: "+k+"..."+Util.byteToHex(sign, " "));
		sign_val = Util.extBytes(sign, 0, sign_val);

		if((content_type!=null)&&(content_type.length>0)) content_type[0] = sign_val;
		if(Util.contains(content_types, sign_val) == -1){
			throw new IOException("BAD sign Header: "+sign_val);
		}
	
		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF off Header");
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS,2);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got mac offset: "+k+"..."+Util.byteToHex(sign, " "));
		off_val = Util.extBytes(sign, 0, off_val);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got offset: "+off_val);
		
		int length_val=0;
		stg = new byte[8];
		//byte[] sign;
		k=Util.readAll(in, stg); if(k<stg.length) throw new IOException("EOF length Header: "+k);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS, 4);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"..."+Util.byteToHex(sign, " "));
		length_val = Util.extBytes(sign, 0, length_val);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got length: "+length_val);
		
		byte skipped[]=new byte[off_val-16];
		k=Util.readAll(in, skipped);if(k<skipped.length) throw new IOException("EOF skipped Header");
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got skipped: "+skipped.length);
		
		stg=new byte[Util.ceil(length_val*8.0/STEGO_BITS)];
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Will read bmp: "+stg.length);
		//k=in.read(stg);
		k=Util.readAll(in,stg);
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got bmp: "+k);
		if(k<stg.length){
			if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"<"+stg.length);
			throw new IOException("EOF data");
		}
		sign=extractSteganoBytes(stg, 0, 1, STEGO_BITS, length_val);
		return sign;
	}
	public static boolean fromBMPStreamSave(InputStream in, StegoStructure[] d, int[] selected)
			throws IOException, ASN1DecoderFail, P2PDDSQLException {
		short[] types = new short[d.length];
		for(int k=0; k<types.length; k++) types[k] = d[k].getSignShort();
		short[] type = new short[1];
		byte []sign = _fromBMPStreamSave(in, types, type);
		int k = Util.contains(types, type[0]);
		if((selected!=null)&&(selected.length>0)) selected[0] = k;
		if(k == -1) return false;
		d[k].setBytes(sign);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got data: "+d);
		d[k].save();
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
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got bytes ");
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Got address: "+d);
		d.save();
		if(DDAddress.DEBUG) System.out.println("fromBMPStreamSave: Done");
		
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
			explain = " - "+_("Not supported compression: "+data.compression+" "+data.bpp);
			fail = true;
		}else{
			int offset = data.startdata;
			int word_bytes=1;
			int bits = 4;
			try {
				EmbedInMedia.setSteganoBytes(d, selected, b, offset, word_bytes, bits);
			} catch (Exception e1) {
				explain = " - "+ _("No valid data in picture!");
				fail = true;
			}
		}
		if(fail) throw new IOException(explain);
		if(selected[0] != -1) d[selected[0]].save();
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
	/**
	 * This also saves (calling save())
	 * Returns the structure that succeded;
	 * @param bi
	 * @param data
	 * @return
	 * @throws ASN1DecoderFail
	 * @throws P2PDDSQLException
	 */
	public static StegoStructure setSteganoImage(BufferedImage bi, StegoStructure[] data, int[] selected) throws ASN1DecoderFail, P2PDDSQLException{
		short[] types = new short[data.length];
		for(int k=0; k<types.length; k++) types[k] = data[k].getSignShort();
		short[] type = new short[1];
		byte []sign = _setSteganoImage(bi, types, type);
		int k = Util.contains(types, type[0]);
		if ((selected != null) && (selected.length > 0)) selected[0] = k;
		if(k == -1) return null;
		data[k].setBytes(sign);
		if(DEBUG)System.out.println(data.toString());
		data[k].save();
		if(DEBUG) System.out.println("setSteganoImage: Done");
		return data[k];
	}
	@Deprecated
	public static StegoStructure setSteganoImage(BufferedImage bi, StegoStructure data) throws ASN1DecoderFail, P2PDDSQLException{
		//DDAddress data = new DDAddress();
		byte[] datab;
		//try{
			datab = _setSteganoImage(bi);
			data.setBytes(datab);
		//}catch(Exception e){
		//	e.printStackTrace();
		//	return null;
		//}
		if(DEBUG)System.out.println(data.toString());
		data.save();
		return data;
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

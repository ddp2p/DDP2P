package hds;

import static util.Util._;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import util.BMP;
import util.P2PDDSQLException;
import util.Util;
import ASN1.ASN1DecoderFail;
import config.Application;
import config.DD;
import data.D_PeerAddress;

public class EmbedInMedia {

	private final static boolean _DEBUG = true;
	private static final boolean DEBUG = false;

	/**
	 * Exporting current Address
	 */
	static void actionExport(JFileChooser fc, Component parent){
		//boolean DEBUG = true;
		DDAddress myAddress;
		try {
			myAddress = D_PeerAddress.getMyDDAddress();
			if(myAddress == null){
				if(ControlPane.DEBUG) System.out.println("EmbedInMedia:actionExport: no address");
				return;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		} 
		if(ControlPane.DEBUG) System.out.println("EmbedInMedia:actionExport: Got to write: "+myAddress);
		BMP[] _data=new BMP[1];
		byte[][] _buffer_original_data=new byte[1][]; // old .bmp file 
		byte[] adr_bytes = myAddress.getBytes();
		try{
		DDAddress  x = new DDAddress();
		 x.setBytes(adr_bytes);
		}catch (ASN1DecoderFail e1) {
							e1.printStackTrace();
							Application.warning(_("Failed to parse file: \n"+e1.getMessage()), _("Failed to parse address!"));
							return;
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
							FileOutputStream fo=new FileOutputStream(file);
							int offset = BMP.DATA;
							int word_bytes=1;
							int bits = 4;
							int datasize;// = adr_bytes.length*(8/bits);
							int height = 10;
							int width = DDAddress.getWidth(adr_bytes.length+DDAddress.STEGO_BYTE_HEADER*word_bytes, bits, 3, height);
							//System.out.println("size="+adr_bytes.length+" width="+width);
							datasize = width*height*3;
							byte[]steg_buffer = new byte[BMP.DATA+datasize];
							BMP data = new BMP(width, height);
							////data.creator = adr_bytes.length;
							data.getHeader(steg_buffer, 0);
							//System.out.println("Got bytes to write: "+Util.byteToHex(adr_bytes, " "));
							//System.out.println("After header: "+Util.byteToHex(steg_buffer, " "));
							fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, steg_buffer,
									offset, word_bytes, bits));
							//System.out.println("Wrote: "+Util.byteToHex(adr_bytes, " "));
							//System.out.println("Got: "+Util.byteToHex(steg_buffer, " "));
							fo.close();
						}else{
							FileOutputStream fo=new FileOutputStream(file);
							int offset = _data[0].startdata;
							int word_bytes=1;
							int bits = 4;
							////Util.copyBytes(b, BMP.CREATOR, adr_bytes.length);
							fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, _buffer_original_data[0], offset, word_bytes, bits));
							fo.close();
						}
					}
			} catch (IOException e1) {
				Application.warning(_("Error writing file:")+file+" - "+ e1,_("Export Address"));
			}
	    }		
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
			if(data.width*data.height*3<(adr_bytes.length*8/DDAddress.STEGO_BITS)+DDAddress.STEGO_BYTE_HEADER){
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

	static void actionImport(JFileChooser fc, Component parent) throws P2PDDSQLException{
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
	        	DDAddress adr = new DDAddress();
	        	if("txt".equals(Util.getExtension(file))) {
	        		if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: txt");
					String content = new Scanner(file).useDelimiter("\\Z").next(); 
					if(!adr.parseAddress(content)){
						Application.warning(_("Failed to parse file: "+file), _("Failed to parse address!"));
						return;
					}
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
						
						try {
						adr.setBytes(addBy);
						} catch (ASN1DecoderFail e1) {
							e1.printStackTrace();
							Application.warning(_("Failed to parse file: "+file+"\n"+e1.getMessage()), _("Failed to parse address!"));
							return;
						}
						
						
					
				
				}
				else
				if("ddb".equals(Util.getExtension(file))){
					if(ControlPane.DEBUG)System.err.println("ControlPane:actionImport: Got: ddb");
					FileInputStream fis=new FileInputStream(file);
					byte[] b = new byte[(int) file.length()];  
					fis.read(b);
					fis.close();
					try {
						adr.setBytes(b);
					} catch (ASN1DecoderFail e1) {
						e1.printStackTrace();
						Application.warning(_("Failed to parse file: "+file+"\n"+e1.getMessage()), _("Failed to parse address!"));
						return;
					}
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
								adr.setSteganoBytes(b, offset, word_bytes, bits);
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
				if(ControlPane.DEBUG)System.err.println("Got DDAddress: "+adr);
	        	adr.save();
	        	Application.warning(adr.getNiceDescription(), _("Obtained Addresses"));
	        }catch(IOException e3){
	        	
	        }
	    }		
	}

	public static byte[] getSteganoBytes(byte[] ddb, byte[] stg,
			int offset, int word_bytes, int bits) {
		byte[] len = new byte[4];
		Util.copyBytes(len, 0, ddb.length);
		EmbedInMedia.getSteganoBytesRaw(len, stg, offset+DDAddress.STEGO_LEN_OFFSET, word_bytes, bits);
		byte[] sign = new byte[2];
		Util.copyBytes(sign, 0, DDAddress.STEGO_SIGN);
		EmbedInMedia.getSteganoBytesRaw(sign, stg, offset+DDAddress.STEGO_SIGN_OFFSET, word_bytes, bits);
		Util.copyBytes(sign, 0, DDAddress.STEGO_BYTE_HEADER);
		EmbedInMedia.getSteganoBytesRaw(sign, stg, offset+DDAddress.STEGO_OFF_OFFSET, word_bytes, bits);
		return EmbedInMedia.getSteganoBytesRaw(ddb, stg, offset+DDAddress.STEGO_BYTE_HEADER*word_bytes, word_bytes, bits);
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

}

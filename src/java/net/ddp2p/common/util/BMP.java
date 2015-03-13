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
 package net.ddp2p.common.util;

import java.io.IOException;

public class BMP {
	public final static int BI_RGB = 0;
	public final static int BI_RLE8 = 1; //8bit/pixel
	public final static int BI_RLE4 = 2; //4bit/pixel
	public final static int BI_BITFIELDS = 3; //HUFFMAN compression with BITMAPCOREHEADER2
	public final static int BI_JPEG = 4; // JPEG or RLE-24 bit/pixel BITMAPCOREHEADER2
	public final static int BI_PNG = 5; // PNG  bit/pixel
	byte bm[]=new byte[]{'B','M'}; //2
	public int filesize=0;
	public int creator=0;
	public int startdata=DATA; //start of pixmap
	int headerlen=40;
	public int width, height;
	public short planes=1, bpp=24;
	public int compression=BI_RGB;
	public int datasize=0; //size of pixmap
	int w_ppm=2835;
	int h_ppm=2835;
	int palette=0;
	int palette_imp=0;
	
	final static int BM = 0;
	final static int FILESIZE = 2;
	public final static int CREATOR = 6;
	final static int STARTDATA = 10;
	final static int HEADERLEN = 14;
	final static int WIDTH = 18;
	final static int HEIGHT = 22;
	final static int PLANES = 26;
	final static int BPP = 28;
	final static int COMPRESSION = 30;
	final static int DATASIZE = 34;
	final static int W_PPM = 38;
	final static int H_PPM = 42;
	final static int PALETTE = 46;
	final static int PALETTE_IMP = 50;
	public final static int DATA = 54;
	public BMP(int _width, int _height) {
		width = _width;
		height = _height;
		datasize = width*height*3;
		filesize = DATA+datasize;
	}
	public String toString(){
		String result = ""+Character.toString((char)bm[0])+Character.toString((char)bm[1])+
		"\nfilesize="+filesize+
		"\ncreator="+creator+
		"\nstartdata="+startdata+
		"\nheaderlen="+headerlen+
		"\nwidth="+width+
		"\nheight="+height+
		"\nplanes="+planes+
		"\nbpp="+bpp+
		"\ncompression="+compression+
		"\ndatasize="+datasize
		;
		return result;
	}
	public BMP(byte[] buffer, int offset) throws IOException {
		extractHeader(buffer, offset);
	}
	public byte[] getHeader(){
		return getHeader(new byte[DATA], 0);
	}
	public void extractHeader(byte[] data, int offset) throws IOException{
		//bm=Util.extBytes(data,offset+BM,bm);
		if((data[offset]!='B') || (data[offset+1]!='M')) throw new IOException("Wrong header: No BMP");
		filesize=Util.extBytes(data, offset+FILESIZE, filesize);
		creator=Util.extBytes(data, offset+CREATOR, creator);
		startdata=Util.extBytes(data, offset+STARTDATA, startdata);
		headerlen=Util.extBytes(data, offset+HEADERLEN, headerlen);
		width=Util.extBytes(data, offset+WIDTH, width);
		height=Util.extBytes(data, offset+HEIGHT, height);
		planes=Util.extBytes(data, offset+PLANES, planes);
		bpp=Util.extBytes(data, offset+BPP, bpp);
		compression=Util.extBytes(data, offset+COMPRESSION, compression);
		datasize=Util.extBytes(data, offset+DATASIZE, datasize);
		w_ppm=Util.extBytes(data, offset+W_PPM, w_ppm);
		h_ppm=Util.extBytes(data, offset+H_PPM, h_ppm);
		palette=Util.extBytes(data, offset+PALETTE, palette);
		palette_imp=Util.extBytes(data, offset+PALETTE_IMP, palette_imp);		
	}
	/**
	 * Fill data array with values from, this header
	 * @param data
	 * @param offset
	 * @return
	 */
	public byte[] getHeader(byte[] data, int offset){
		Util.copyBytes(data,offset+BM,bm,2,0);
		Util.copyBytes(data, offset+FILESIZE, filesize);
		Util.copyBytes(data, offset+CREATOR, creator);
		Util.copyBytes(data, offset+STARTDATA, startdata);
		Util.copyBytes(data, offset+HEADERLEN, headerlen);
		Util.copyBytes(data, offset+WIDTH, width);
		Util.copyBytes(data, offset+HEIGHT, height);
		Util.copyBytes(data, offset+PLANES, planes);
		Util.copyBytes(data, offset+BPP, bpp);
		Util.copyBytes(data, offset+COMPRESSION, compression);
		Util.copyBytes(data, offset+DATASIZE, datasize);
		Util.copyBytes(data, offset+W_PPM, w_ppm);
		Util.copyBytes(data, offset+H_PPM, h_ppm);
		Util.copyBytes(data, offset+PALETTE, palette);
		Util.copyBytes(data, offset+PALETTE_IMP, palette_imp);
		return data;
	}
}

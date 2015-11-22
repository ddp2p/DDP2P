/*   Copyright (C) 2014 Marius C. Silaghi
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
import java.io.File;
import java.util.ArrayList;
interface PNG_Chunk {
	byte[] getMarker();
	int getMarkerInt();
	boolean isPublic();
	boolean isPrivate();
	boolean isCritical();
	boolean isSafeToCopy();
	int load(byte[] data, int off, int limit);
	int save(byte[] data, int off, int limit);
	int length();
}
class PNG_Chunk_ANY implements PNG_Chunk {
	byte marker[];
	int length;
	byte buf[];
	byte crc[];
	PNG_Chunk_ANY(){
	}
	PNG_Chunk_ANY(boolean init_marker){
		marker = new byte[4];
		crc = new byte[4];
	}
	@Override
	public int length() {
		return length;
	}
	@Override
	public String toString() {
		String result = Util.byteToString(marker);
		result += " ["+length+"]";
		return result;
	}
	@Override
	public byte[] getMarker() {
		return marker;
	}
	public int getMarkerInt() {
		return (int)Util.be32_to_cpu(marker, 0);
	}
	@Override
	public int load(byte[] data, int offset, int limit) {
		int off = offset;
		length = (int)Util.be32_to_cpu(data, off);
		off += 4;
		Util.copyBytes(marker, 0, data, 4, off);
		if (!PNG.validMarker(marker, 0))
			throw new RuntimeException("Wrong marker");
		off += 4;
		buf = new byte[length];
		Util.copyBytes(buf, 0, data, length, off);
		Util.copyBytes(crc, 0, data, 4, off+length);
		if(!CRC.verifyCRC(crc, data, 4+length, offset+4))
			throw new RuntimeException("Wrong ANY crc verif");
		return off+length+4;
	}
	@Override
	public int save(byte[] data, int off, int limit) {
		Util.cpu_to_be32(length, data, off);
		off += 4;
		Util.copyBytes(data, off, marker, 4, off);
		off += 4;
		Util.copyBytes(data, off, buf, length, 0);
		Util.copyBytes(data, off, crc, 4, off+length);
		return off+length+4;
	}
	@Override
	public boolean isPublic() {
		return Util.isUpperCase(marker[1]);
	}
	@Override
	public boolean isPrivate() {
		return Util.isLowerCase(marker[1]);
	}
	@Override
	public boolean isCritical() {
		return Util.isUpperCase(marker[0]);
	}
	@Override
	public boolean isSafeToCopy() {
		return Util.isLowerCase(marker[3]);
	}
}
class PNG_Chunk_IHDR extends PNG_Chunk_ANY {
	int width, height;
	byte bit_depth, color_type, compression,
	filter, interlace;
	PNG_Chunk_IHDR() {
		marker = new byte[]{'I','H','D','R'};
		crc = new byte[4];
	}
	@Override
	public String toString() {
		String result = Util.byteToString(marker);
		result += " ["+length+"]";
		result += "\n\t w="+width;
		result += "\n\t h="+height;
		result += "\n\t bit_depth="+bit_depth;
		result += "\n\t color="+getColor(color_type);
		result += "\n\t compress="+compression;
		result += "\n\t filter="+filter;
		result += "\n\t interlace="+interlace;
		return result;
	}
	static private String getColor(byte color_type) {
		String r;
		switch (color_type) {
		case 0: r="GRAYSCALE 1/2/4/8/16"; break;
		case 2: r="RGB_8/16"; break;
		case 3: r="PALLETE_IDX 1/2/4/8"; break;
		case 4: r="GRAYSCALE+ALPHA 8/16"; break;
		case 6: r="RGB+ALPHA 8/16"; break;
		default: r="UNKNOWN";
		}
		return r+" ("+color_type+")";
	}
	@Override
	public int load(byte[] data, int offset, int limit) {
		int off = offset;
		length = (int) Util.be32_to_cpu(data, off);
		if (length != 0x0D)
			throw new RuntimeException("Wrong IHDR length");
		off += 4;
		byte[] in = new byte[4];
		Util.copyBytes(in, 0, data, 4, off);
		if (!Util.equalBytes(marker, in))
			throw new RuntimeException("Wrong IHDR marker");
		off += 4;
		width = (int) Util.be32_to_cpu(data, off);
		off += 4;
		height = (int) Util.be32_to_cpu(data, off);
		off += 4;
		bit_depth = data[off++];
		color_type = data[off++];
		compression = data[off++];
		filter = data[off++];
		interlace = data[off++];
		Util.copyBytes(crc, 0, data, 4, off);
		if(!CRC.verifyCRC(crc, data, 4+length, offset+4))
			throw new RuntimeException("Wrong IHDR crc verif");
		return offset+12+length;
	}
	@Override
	public int save(byte[] data, int offset, int limit) {
		int off = offset;
		Util.cpu_to_be32(length, data, off);
		off += 4;
		Util.copyBytes(data, off, marker, 4, off);
		off += 4;
		Util.cpu_to_be32(width, data, off);
		off += 4;
		Util.cpu_to_be32(height, data, off);
		off += 4;
		data[off++] = bit_depth;
		data[off++] = color_type;
		data[off++] = compression;
		data[off++] = filter;
		data[off++] = interlace;
		Util.copyBytes(data, off, crc, 4, off);
		return offset+12+length;
	}
}
class PNG_Chunk_IEND implements PNG_Chunk {
	byte marker[] = new byte[]{'I','E','N','D'};
	int length;
	byte crc[] = new byte[]{(byte) 0xAE, 0x42, 0x60, (byte) 0x82};
	@Override
	public int length() {
		return 0;
	}
	@Override
	public String toString() {
		String result = "IEND";
		return result;
	}
	@Override
	public byte[] getMarker() {
		return marker;
	}
	@Override
	public int getMarkerInt() {
		return (int)Util.be32_to_cpu(marker, 0);
	}
	@Override
	public int load(byte[] data, int offset, int limit) {
		int off = offset;
		length = (int) Util.be32_to_cpu(data, off);
		if (length!=0)
			throw new RuntimeException("Wrong IEND length");
		off += 4;
		byte[] in = new byte[4];
		Util.copyBytes(in, 0, data, 4, off);
		if (!Util.equalBytes(marker, in))
			throw new RuntimeException("Wrong IEND marker");
		off += 4;
		Util.copyBytes(in, 0, data, 4, off);
		if (!Util.equalBytes(crc, in))
			throw new RuntimeException("Wrong IEND crc");
		off += 4;
		if(!CRC.verifyCRC(crc, data, 4+length, offset+4))
			throw new RuntimeException("Wrong IEND crc verif");
		return off;
	}
	public int save(byte[] data, int off, int limit) {
		Util.cpu_to_be32(length, data, off);
		off += 4;
		Util.copyBytes(data, off, marker, 4, off);
		off += 4;
		Util.copyBytes(data, off, crc, 4, off);
		off += 4;
		return off;
	}
	@Override
	public boolean isPublic() {
		return Util.isUpperCase(marker[1]);
	}
	@Override
	public boolean isPrivate() {
		return Util.isLowerCase(marker[1]);
	}
	/**
	 * Not ancillary
	 */
	@Override
	public boolean isCritical() {
		return Util.isUpperCase(marker[0]);
	}
	@Override
	public boolean isSafeToCopy() {
		return Util.isLowerCase(marker[3]);
	}
}
/**
 * CRC Code taken from the ITU standard, but unsigned long and chars
 * were changed to signed ones...
 * @author msilaghi
 *
 */
class CRC {
	   static long crc_table[] = new long[256];
	   static boolean crc_table_computed = false;
	   static void make_crc_table()
	   {
	     long c;
	     int n, k;
	     for (n = 0; n < 256; n++) {
	       c = (long) n;
	       for (k = 0; k < 8; k++) {
	         if ((c & 1) != 0)
	           c = 0xedb88320L ^ (c >> 1);
	         else
	           c = c >> 1;
	       }
	       crc_table[n] = c;
	     }
	     crc_table_computed = true;
	   }
	   static long update_crc(long crc, byte buf[],
	                            int len, int off)
	   {
	     long c = crc;
	     int n, n_off;
	     if (!crc_table_computed)
	       make_crc_table();
	     for (n_off = 0; n_off < len; n_off++) {
	    	 n = n_off + off;
	    	 c = crc_table[(int) ((c ^ Util.byte_to_uint(buf[n])) & 0xff)] ^ (c >> 8);
	     }
	     return c;
	   }
	   static long crc(byte buf[], int len, int off)
	   {
	     return update_crc(0xffffffffL, buf, len, off) ^ 0xffffffffL;
	   }
		public static byte[] getCRC(byte[] data, int len, int off) {
			byte b[] = new byte[4];
			return getCRC(data, len, off, b, 0);
		}
		public static byte[] getCRC(byte[] in, int in_len, int in_off, byte out[], int out_off){
			long crc = CRC.crc(in, in_len, in_off);
			Util.cpu_to_be32(crc, out, out_off);
			return out;
		}
		public static byte[] getCRC(byte[] in1, byte[] in2){
			byte b[] = new byte[4];
			long crc = 0xffffffffL;
			crc = update_crc(crc, in1, in1.length, 0);
			crc = update_crc(crc, in2, in2.length, 0);
			crc = crc ^ 0xffffffffL;
			Util.cpu_to_be32(crc, b, 0);
			return b;
		}
		public static boolean verifyCRC(byte[]_crc, byte[] data, int len, int off){
			return verifyCRC(_crc, 0, data, len, off);
		}
		public static boolean verifyCRC(byte[]_crc, int _crc_off, byte[] data, int len, int off){
			long crc = CRC.crc(data, len, off);
			long old_crc= Util.be32_to_cpu(_crc, _crc_off);
			return crc==old_crc;
		}
}
public 
class PNG {
	public static final int IEND = 
			Util.be16_to_cpu(new byte[]{'I','E','N','D'}, 0); 
	public static final int IHDR =
			Util.be16_to_cpu(new byte[]{'I','H','D','R'}, 0);
	public static final byte[] _ppDd =
			new byte[]{'p','p','D','d'};
	public static final int ppDd = 
			Util.be16_to_cpu(_ppDd, 0);
	public static final byte crc[] =
			new byte[]{(byte) 0xB7, 0x1D, (byte) 0xC1, 0x04, 1};
	private static final boolean DEBUG = false; 
	private static final boolean _DEBUG = true; 
	String filename;
	ArrayList<PNG_Chunk> chunks = new ArrayList<PNG_Chunk>();
	byte header[] = new byte[] {
			(byte) 0x89, 
			0x50, 0x4E, 0x47, 
			0x0D, 0x0A, 
			0x1A, 0x0A}; 
	private byte[] end_data = new byte[0];
	public PNG() {
	}
	public static boolean validMarker(byte[] marker, int off) {
		if (!Util.isAsciiAlpha(marker[off+0])) return false;
		if (!Util.isAsciiAlpha(marker[off+1])) return false;
		if (!Util.isUpperCase(marker[off+2])) return false;
		if (!Util.isAsciiAlpha(marker[off+3])) return false;
		return true;
	}
	/**
	 * 
	 * @param _filename
	 */
	void load(String _filename) {
		filename = _filename;
		int cnt = 0;
		File f = new File(filename);
		byte[] data = Util.readAll(f);
		if (data == null) return;
		cnt = data.length;
		load(data, cnt);
	}
	/**
	 * 
	 * @param data
	 * @param cnt (length of useful data)
	 */
	public void load(byte[] data, int cnt) {
		int off = 0;
		byte header_read[] = new byte[header.length];
		Util.copyBytes(header_read, 0, data, header.length, 0);
		if (! Util.equalBytes(header, header_read))
			throw new RuntimeException("Wrong header");
		off += header.length;
		while (off < cnt) {
			if (PNG.DEBUG) System.out.println(" loop="+off);
			if (!validMarker(data, off+4)) {
				throw new RuntimeException("Bad Marker at offset: "+off);
			}
			int marker = Util.be16_to_cpu(data, off+4);
			if (marker == IEND) {
				PNG_Chunk_IEND iend = new PNG_Chunk_IEND();
				off = iend.load(data, off, cnt);
				chunks.add(iend);
				if (PNG.DEBUG) System.out.println(" C="+iend);
				break;
			}
			if (marker == IHDR) {
				PNG_Chunk_IHDR ihdr = new PNG_Chunk_IHDR();
				off = ihdr.load(data, off, cnt);
				chunks.add(ihdr);
				if (PNG.DEBUG) System.out.println(" C="+ihdr);
				continue;
			}
			PNG_Chunk_ANY chunk = new PNG_Chunk_ANY(true);
			off = chunk.load(data, off, cnt);
			chunks.add(chunk);
			if (PNG.DEBUG) System.out.println(" C="+chunk);
			continue;
		}
		end_data = new byte[cnt-off];
		Util.copyBytes(end_data, 0, data, cnt-off, off);
	}
	public String toString() {
		String result = "";
		for (PNG_Chunk c : chunks) {
			result += " \n"+c;
		}
		result += " \nEND_DATA LENGTH="+end_data.length;
		return result;
	}
	long length() {
		long len = header.length;
		for (PNG_Chunk c : this.chunks) {
			len += c.length();
		}
		len += end_data.length;
		return len;
	}
	/**
	 * Load the file in the preallocated buf (of length given by length())
	 * @param buf
	 */
	void save(byte[] buf) {
		int off = 0;
		int limit = buf.length;
		Util.copyBytes(buf, off, header, header.length, 0);
		for (PNG_Chunk c : this.chunks) {
			off = c.save(buf, off, limit);
		}
		Util.copyBytes(buf, off, end_data, end_data.length, 0);
	}
	/**
	 * Used to add payload data in private chunk
	 * @param data
	 * @param len
	 * @param off
	 */
	void addData(byte data[], int len, int off) {
		PNG_Chunk_ANY c = new PNG_Chunk_ANY();
		c.marker = _ppDd;
		c.length = len;
		c.buf = new byte[c.length];
		Util.copyBytes(c.buf, 0, data, c.length, off);
		c.crc = CRC.getCRC(c.marker, c.buf);
		chunks.add(chunks.size()-1, c);
	}
	public static void main (String args[]) {
		try {
			String file = args[0];
			PNG png = new PNG();
			png.load(file);
			System.out.println("PNG="+png);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

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
interface JPEG_Chunk {
	byte getMarker();
	boolean hasMarker();
	int load(byte[] data, int off, int limit);
	int save(byte[] data, int off, int limit);
	long length();
}
class JPEG_Chunk_Marker_Standalone implements JPEG_Chunk {
	byte marker;
	@Override
	public long length() {
		return 2;
	}
	@Override
	public String toString() {
		String result = " ";
		switch (marker) {
		case JPEG.SOI:
			result += "SOI";
			break;
		case JPEG.EOI:
			result += "EOI";
			break;
		default:
			result += "MS="+marker;
		}
		return result;
	}
	@Override
	public byte getMarker() {
		return marker;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		marker = data[off + 1];
		return off+2;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = marker;
		return off+2;
	}
}
class DQT_Table {
	byte _type; 
	boolean _color; 
	byte _color_ID;
	boolean _extended;
	int[][] _dqt_Extended_ZigZag;
	byte[][] _dqt_Bytes_ZigZag;
	int[][] _dqt_Extended_orig;
	byte[][] _dqt_Bytes_orig;	
}
class JPEG_Chunk_Marker_DQT implements JPEG_Chunk {
	/**
	 * The horizontal dimension (0 is left)
	 */
	private static final int[] zigzag_i =
			new int[] {
		0, 1, 0, 
		0, 1, 2, 3, 2, 1, 0, 
		0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0,
		0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0,
		1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 
		3, 4, 5, 6, 7, 7, 6, 5, 4,
		5, 6, 7, 7, 6, 7};
	/**
	 * the vertical dimension (0 is up)
	 */
	private static final int[] zigzag_j =
			new int[] {
		0, 0, 1,
		2, 1, 0, 0, 1, 2, 3,
		4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5,
		6, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7,
		7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7,
		7, 6, 5, 4, 3, 4, 5, 6, 7,
		7, 6, 5, 6, 7, 7};
	int length;
	private ArrayList<DQT_Table> qt = new ArrayList<DQT_Table>();
	@Override
	public long length() {
		return 2+length;
	}
	@Override
	public String toString() {
		String result = " ";
		result += "DQT[ len="+length+" #"+qt.size();
		for (int k = 0; k < qt.size(); k ++) {
			DQT_Table _qt = qt.get(k);
			result += " < color="+_qt._color+" ID("+_qt._color_ID+")"+" ext="+_qt._extended+">";
			result += "]";
		}
		return result;
	}
	@Override
	public byte getMarker() {
		return JPEG.DQT;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if ((limit-off < 4)) throw new RuntimeException("No length!");
		length = Util.be16_to_cpu(data, off + 2);
		if ((limit-off < 2+length)) throw new RuntimeException("No payload!");
		int offset = off + 4;
		int idx = 0;
		for (;;) {
			if (offset+65 > off + 2+length) break;
			DQT_Table _qt = new DQT_Table();
			_qt._type = data[offset++];
			_qt._color = (_qt._type & 0x1) != 0;
			_qt._color_ID = (byte) (_qt._type & (byte)0x0F);
			_qt._extended = (_qt._type & 0x10) != 0;
			if (_qt._extended && offset+128 > off + 2+length) break;
			if (_qt._extended) {
				_qt._dqt_Extended_ZigZag = new int[8][8]; 
				_qt._dqt_Extended_orig = new int[8][8]; 
			} else {
				_qt._dqt_Bytes_ZigZag = new byte[8][8]; 
				_qt._dqt_Bytes_orig = new byte[8][8]; 
			}
			for (int i = 0; i < 8; i ++) {
				for (int j = 0; j < 8; j ++) {
					if (_qt._extended) {
						_qt._dqt_Extended_ZigZag[i][j] = Util.be16_to_cpu(data, offset);
						_qt._dqt_Extended_orig[zigzag_i[idx]][zigzag_j[idx]] = _qt._dqt_Extended_ZigZag[i][j];
						offset += 2;
					} else {
						_qt._dqt_Bytes_ZigZag[i][j] = data[offset++];
						_qt._dqt_Bytes_orig[zigzag_i[idx]][zigzag_j[idx]] = _qt._dqt_Bytes_ZigZag[i][j];
					}
				}
			}
			qt.add(_qt);
		}		
		return off+2+length;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = JPEG.DQT;
		Util.cpu_to_16be(length, data, off + 2);
		int offset = off + 4;
		for (int k = 0; k < qt.size(); k ++) {
			DQT_Table _qt = qt.get(k);
			data[offset ++] = _qt._type;
			for (int i=0; i < 8; i++) {
				for (int j=0; j < 8; j++) {
					if (_qt._extended) {
						Util.cpu_to_16be(_qt._dqt_Extended_ZigZag[i][j], data, offset);
						offset += 2;
					} else {
						Util.cpu_to_16be(_qt._dqt_Bytes_ZigZag[i][j], data, offset++);
					}
				}
			}
		}
		return off + 2 + length;
	}		
}
class JPEG_Chunk_Marker_SOF_Component {
	byte identifier;
	byte sampling;
	byte DQT_ID;
	int sampling_x, sampling_y;
	public String toString() {
		String r = "<";
		r += " id="+componentID(identifier)+"("+identifier+")";
		r += " Sx="+sampling_x;
		r += " Sy="+sampling_y;
		r += " DQT="+DQT_ID;
		return r+">";
	}
	public static String componentID(byte identifier2) {
		switch (identifier2) {
		case JPEG.COMPONENT_Y: return "Y";
		case JPEG.COMPONENT_CB: return "Cb";
		case JPEG.COMPONENT_CR: return "Cr";
		default:
			return null;
		}
	}
}
class JPEG_Chunk_Marker_SOF  implements JPEG_Chunk {
	byte marker;
	int length;
	byte precision;
	int width;
	int height;
	byte components;
	JPEG_Chunk_Marker_SOF_Component[] _components;
	@Override
	public long length() {
		return 2+length;
	}
	@Override
	public String toString() {
		String result = " ";
		result += "SOF"+(marker-JPEG.SOF0)+"[ len="+length;
		result += " prec="+precision;
		result += " width="+width;
		result += " height="+height;
		result += " components="+components;
		for (int k=0; k<components; k++)
			result += " c["+k+"]="+_components[k];
		return result+"]";
	}
	@Override
	public byte getMarker() {
		return marker;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if ((limit-off < 4)) throw new RuntimeException("No length!");
		marker = data[off + 1];
		length = Util.be16_to_cpu(data, off + 2);
		if ((limit-off < 2+length)) throw new RuntimeException("No payload!");
		precision = data[off + 4];
		width = Util.be16_to_cpu(data, off + 5);
		height = Util.be16_to_cpu(data, off + 7);
		components = data[off + 9];
		_components = new JPEG_Chunk_Marker_SOF_Component[components];
		int offset = off + 10;
		for (int k = 0; k < components; k ++) {
			_components[k] = new JPEG_Chunk_Marker_SOF_Component();
			_components[k].identifier = data[offset++];
			_components[k].sampling = data[offset++];
			_components[k].DQT_ID = data[offset++];
			_components[k].sampling_x = (_components[k].sampling>>4) & 0x0F;
			_components[k].sampling_y = (_components[k].sampling) & 0x0F;
		}
		return off+2+length;
	}	
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = marker;
		Util.cpu_to_16be(length, data, off+2);
		data[off+4] = precision;
		Util.cpu_to_16be(width, data, off+5);
		Util.cpu_to_16be(height, data, off+7);
		data[off+9] = components;
		int offset = off + 10;
		for (int k = 0; k < components; k ++) {
			data[offset++] = _components[k].identifier;
			data[offset++] = _components[k].sampling;
			data[offset++] = _components[k].DQT_ID;
		}
		return off+2+length;
	}
}
class DHT_Table {
	byte id;
	int DHT_class;
	int DHT_component;
	byte codes[] =  new byte[16];
	byte symbols[];
	public String toString() {
		String result = " Huf_T[";
		result += " "+JPEG_Chunk_Marker_DHT.getDHTclass(DHT_class);
		result += " componentID="+DHT_component;
		result += " symb="+Util.concat(codes, "|", "NULL");
		return result+"]";
	}
	int load (byte[]data, int off, int limit) {
		id = data[off];
		this.DHT_class = (id>>4) & 0x0F; 
		this.DHT_component = (id) & 0x0F; 
		int offset = off+1;
		int nb_symbols = 0;
		for (int k=0; k<16; k++) {
			this.codes[k] = data[offset++];
			nb_symbols += Util.byte_to_uint(this.codes[k]);
		}
		this.symbols = new byte[nb_symbols];
		Util.copyBytes(symbols, 0, data, nb_symbols, offset);
		return offset+nb_symbols;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = id;
		int offset = off+1;
		for (int k=0; k<16; k++) {
			data[offset++] = this.codes[k];
		}
		Util.copyBytes(data, offset, symbols, symbols.length, 0);
		return offset + symbols.length;
	}
}
class JPEG_Chunk_Marker_DHT  implements JPEG_Chunk {
	byte marker;
	int length;
	ArrayList<DHT_Table> tables = new ArrayList<DHT_Table>();
	@Override
	public long length() {
		return 2+length;
	}
	@Override
	public String toString() {
		String result = " DHT[";
		result += "len="+length;
		result += " "+Util.concat(tables, " \n", "NULL");
		return result+"]";
	}
	static String getDHTclass(int dHT_class2) {
		switch (dHT_class2) {
		case JPEG.DHT_DC: return "DC";
		case JPEG.DHT_AC: return "AC";
		default:
			return null;
		}
	}
	@Override
	public byte getMarker() {
		return marker;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if ((limit-off < 4)) throw new RuntimeException("No length!");
		marker = data[off + 1];
		length = Util.be16_to_cpu(data, off + 2);
		if ((limit-off < 2+length)) throw new RuntimeException("No payload!");
		int offset = off+4;
		while (offset < off + 2 + length) {
			DHT_Table table = new DHT_Table();
			offset = table.load(data, offset, limit);
			this.tables.add(table);
		}
		if (offset != off + 2 + length) {
			System.out.println("JPEG:DTH:Error reading table "+offset);
		}
		return off + 2 + length;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = marker;
		Util.cpu_to_16be(length, data, off+2);
		int offset = off+4;
		for (DHT_Table table : tables) {
			offset = table.save(data, offset, limit);
		}
		if (offset != off + 2 + length) {
			System.out.println("JPEG:DTH:Error reading table "+offset);
		}
		return off+2+length;
	}
}
class JPEG_Chunk_Marker_SOS_COMPONENT_DESC {
	byte component_ID;
	byte DC_AC_DHT;
	int ac, dc;
	public String toString() {
		String r = "<";
		r += " ID="+component_ID;
		r += " AC_DHT="+ac;
		r += " DC_DHT="+dc;
		return r+">";
	}
}
class JPEG_Chunk_Marker_SOS implements JPEG_Chunk {
	byte marker;
	int length;
	byte component_count;
	JPEG_Chunk_Marker_SOS_COMPONENT_DESC[] descriptors;
	byte spectral_selection_start;
	byte spectral_selection_end;
	byte approximations;
	@Override
	public long length() {
		return 2+length;
	}
	@Override
	public String toString() {
		String result = " SOS[";
		result += " len="+length;
		result += " #c="+component_count;
		for (int k=0; k<component_count; k++) {
			result += " D"+k+"="+descriptors[k];
		}
		result += " "+spectral_selection_start+"/"+spectral_selection_end+"/"+approximations;
		return result+"]";
	}
	@Override
	public byte getMarker() {
		return marker;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if ((limit-off < 4)) throw new RuntimeException("No length!");
		marker = data[off + 1];
		length = Util.be16_to_cpu(data, off + 2);
		if ((limit-off < 2+length)) throw new RuntimeException("No payload!");
		this.component_count = data[off+4];
		this.descriptors = new JPEG_Chunk_Marker_SOS_COMPONENT_DESC[component_count];
		int offset = off+5;
		for (int k=0; k<descriptors.length; k++) {
			this.descriptors[k] = new JPEG_Chunk_Marker_SOS_COMPONENT_DESC();
			this.descriptors[k].component_ID = data[offset++];
			this.descriptors[k].DC_AC_DHT = data[offset++];
			this.descriptors[k].dc = (this.descriptors[k].DC_AC_DHT>>4) & 0x00F;
			this.descriptors[k].ac = (this.descriptors[k].DC_AC_DHT) & 0x00F;
		}
		this.spectral_selection_start = data[offset++];
		this.spectral_selection_end = data[offset++];
		this.approximations = data[offset++];
		return off+2+length;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = marker;
		Util.cpu_to_16be(length, data, off+2);
		data[off+4] = component_count;
		int offset = off+5;
		for (int k=0; k<descriptors.length; k++) {
			data[offset++] = this.descriptors[k].component_ID;
			data[offset++] = this.descriptors[k].DC_AC_DHT;
		}
		data[offset++] = this.spectral_selection_start;
		data[offset++] = this.spectral_selection_end;
		data[offset++] = this.approximations;
		return off+2+length;
	}
}
class JPEG_Chunk_Marker_Payload implements JPEG_Chunk {
	byte marker;
	int length;
	byte buf[];
	@Override
	public String toString() {
		String result = " ";
		result += marker+"/"+length;
		return result;
	}
	@Override
	public byte getMarker() {
		return marker;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if ((limit-off < 4)) throw new RuntimeException("No length!");
		marker = data[off + 1];
		length = Util.be16_to_cpu(data, off + 2);
		if ((limit-off < 2+length)) throw new RuntimeException("No payload!");
		buf = new byte[length - 2];
		Util.copyBytes(buf, 0, data, buf.length, off+4);
		return off+2+length;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = marker;
		Util.cpu_to_16be(length, data, off+2);
		Util.copyBytes(data, off+4, buf, buf.length, 0);
		return off+2+length;
	}
	@Override
	public long length() {
		return 2+length;
	}
}
class JPEG_Chunk_Marker_APP0 implements JPEG_Chunk {
	private int length, x_density, y_density;
	byte version_major, version_minor, density;
	byte INCH = 1;
	byte CM = 2;
	byte thumbnail_width, thumbnail_height;
	byte[] thumbnail_bitmap;
	byte[] id = new byte[5];
	private byte th_format;
	@Override
	public long length() {
		return 2+length;
	}
	@Override
	public String toString() {
		String result = "";
		result += " APP0["+length;
		result += " ID="+Util.byteToString(id);
		result += " M="+version_major;
		result += " m="+version_minor;
		result += " d="+density;
		result += " tw="+thumbnail_width;
		result += " th="+thumbnail_height;
		return result +"]";
	}
	@Override
	public byte getMarker() {
		return JPEG.APP0;
	}
	@Override
	public boolean hasMarker() {
		return true;
	}
	public int save(byte[] data, int off, int limit) {
		data[off] = JPEG.MARKER;
		data[off+1] = JPEG.APP0;
		Util.cpu_to_16be(length, data, off+2);
		Util.copyBytes(data, off+4, id, id.length, 0);
		if (Util.equalBytes(id, new byte[]{'J','F','I','F',0})) {
			data[off+9] = this.version_major;
			data[off+10] = this.version_minor;
			data[off+11] = this.density;
			Util.cpu_to_16be(x_density, data, off+12);
			Util.cpu_to_16be(y_density, data, off+14);
			data[off+16] = this.thumbnail_width;
			data[off+17] = this.thumbnail_height;
			Util.copyBytes(data, off+18, thumbnail_bitmap, thumbnail_bitmap.length, 0);
		} else
			if (Util.equalBytes(id, new byte[]{'J','F','X','X',0})) {
				data[off+9] = th_format;
				switch (th_format) {
				case JPEG.THUMBNAIL_JPEG:
					break;
				case JPEG.THUMBNAIL_RGB1:
				case JPEG.THUMBNAIL_RGB3:
					data[off+10] = this.thumbnail_width;
					data[off+11] = this.thumbnail_height;
				}
			}
		return off+2+length;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		if (JPEG.DEBUG) System.out.println(" load APP0 off="+off);
		if ((limit-off<4)) throw new RuntimeException("No length!");
		length = Util.be16_to_cpu(data, off+2);
		if (JPEG.DEBUG) System.out.println(" load APP0 len="+length);
		if ((length<16) || (limit-off<18)) throw new RuntimeException("Bad!");
		Util.copyBytes(id, 0, data, id.length, off+4);
		if (Util.equalBytes(id, new byte[]{'J','F','I','F',0})) {
			if (JPEG.DEBUG) System.out.println(" load APP0 JFIF");
			this.version_major = data[off+9];
			this.version_minor = data[off+10];
			this.density = data[off+11];
			this.x_density = Util.be16_to_cpu(data, off+12);
			this.y_density = Util.be16_to_cpu(data, off+14);
			this.thumbnail_width = data[off+16];
			if (JPEG.DEBUG) System.out.println(" load APP0 tw="+thumbnail_width);
			this.thumbnail_height = data[off+17];
			if (JPEG.DEBUG) System.out.println(" load APP0 tw="+thumbnail_height);
			int thumbnail_len = 3*thumbnail_width*thumbnail_height;
			if (JPEG.DEBUG) System.out.println(" load APP0 tl="+thumbnail_len);
			if (limit-off < 18 + thumbnail_len) throw new RuntimeException("Bad thumbnail!");
			thumbnail_bitmap = new byte[thumbnail_len];
			Util.copyBytes(thumbnail_bitmap, 0, data, thumbnail_len, off+18);
		} else
			if (Util.equalBytes(id, new byte[]{'J','F','X','X',0})) {
				System.out.println(" load APP0 JFXX");
				th_format = data[off+9];
				switch (th_format) {
				case JPEG.THUMBNAIL_JPEG:
					break;
				case JPEG.THUMBNAIL_RGB1:
				case JPEG.THUMBNAIL_RGB3:
					this.thumbnail_width = data[off+10];
					System.out.println(" load APP0 tw="+thumbnail_width);
					this.thumbnail_height = data[off+11];
					System.out.println(" load APP0 tw="+thumbnail_height);
				}
			}else
				System.out.println(" load unknown APP0");
		return off + 2 + length;
	}
}
class JPEG_Chunk_Bulk implements JPEG_Chunk {
	public String toString() {
		String result = "";
		result += " ["+buf.length+"]";
		return result;
	}
	private byte[] buf;
	@Override
	public byte getMarker() {
		return 0;
	}
	@Override
	public boolean hasMarker() {
		return false;
	}
	@Override
	public int load(byte[] data, int off, int limit) {
		int pos = scanForMarker(data, off, limit);
		int len = pos - off;
		buf = new byte[len];
		Util.copyBytes(buf, 0, data, len, off);
		return pos;
	}
	public int save(byte[] data, int off, int limit) {
		Util.copyBytes(data, off, buf, buf.length, 0);
		return off+buf.length;
	}
	private int scanForMarker(byte[] data, int off, int limit) {
		for (int k = off; k < limit; k++) {
			if (JPEG.isMarker(data, k)) return k;
		}
		return limit;
	}
	@Override
	public long length() {
		return buf.length;
	}
}
public class JPEG {
	public static final boolean DEBUG = false;
	public static final int DHT_AC = 1;
	public static final int DHT_DC = 0;
	public static final byte COMPONENT_CR = 3;
	public static final byte COMPONENT_CB = 2;
	public static final byte COMPONENT_Y = 1;
	public static final byte THUMBNAIL_RGB3 = 0x13;
	public static final byte THUMBNAIL_RGB1 = 0x11;
	public static final byte THUMBNAIL_JPEG = 0x10;
	public static final byte MARKER = (byte) 0xFF;
	final public static byte TEM = (byte) 0x01;
	final public static byte SOF0 = (byte) 0xC0;
	final public static byte SOF1 = (byte) 0xC1;
	final public static byte SOF2 = (byte) 0xC2;
	final public static byte SOF3 = (byte) 0xC3;
	final public static byte SOF4 = (byte) 0xC5;
	final public static byte SOF5 = (byte) 0xC6;
	final public static byte SOF6 = (byte) 0xC7;
	final public static byte SOF7 = (byte) 0xC9;
	final public static byte SOF8 = (byte) 0xCA;
	final public static byte SOF9 = (byte) 0xCB;
	final public static byte SOF10 = (byte) 0xCD;
	final public static byte SOF11 = (byte) 0xCE;
	final public static byte SOF12 = (byte) 0xCF;
	final public static byte DHT = (byte) 0xC4;
	final public static byte RST0 = (byte) 0xD0; 
	final public static byte SOI = (byte) 0xD8;
	final public static byte EOI = (byte) 0xD9;
	final public static byte SOS = (byte) 0xDA;
	final public static byte DQT = (byte) 0xDB;
	final public static byte DDI = (byte) 0xDD;
	final public static byte APP0 = (byte) 0xE0; 
	final public static byte COM = (byte) 0xFE;
	ArrayList<JPEG_Chunk> chunks = new ArrayList<JPEG_Chunk>();
	String filename;
	public JPEG() {
	}
	public String toString() {
		String result = "";
		for (JPEG_Chunk c : chunks) {
			result += " \n"+c;
		}
		return result;
	}
	long length() {
		long len = 0;
		for (JPEG_Chunk c : this.chunks) {
			len += c.length();
		}
		return len;
	}
	/**
	 * Load the file in the preallocated buf (of length given by length())
	 * @param buf
	 */
	void save(byte[] buf) {
		int off = 0;
		int limit = buf.length;
		for (JPEG_Chunk c : this.chunks) {
			off = c.save(buf, off, limit);
		}
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
	public void load(byte[] data, int cnt) {
		int off = 0;
		while (off < cnt) {
			if (JPEG.DEBUG) System.out.println(" loop="+off);
			if ( ! isMarker(data, off) ) {
				JPEG_Chunk_Bulk bulk = new JPEG_Chunk_Bulk();
				off = bulk.load(data, off, cnt);
				chunks.add(bulk);
				continue;
			}
			if (off + 1 >= cnt) break;
			byte m = data[off + 1];
			if (JPEG.DEBUG) System.out.println(" load marker:"+m);
			switch (m) {
			case APP0:
				if (JPEG.DEBUG) System.out.println(" load APP0");
				JPEG_Chunk_Marker_APP0 mark = new JPEG_Chunk_Marker_APP0();
				off = mark.load(data, off, cnt);
				chunks.add(mark);
				break;
			case SOI:
				if (JPEG.DEBUG) System.out.println(" load SOI");
				JPEG_Chunk_Marker_Standalone soi = new JPEG_Chunk_Marker_Standalone();
				off = soi.load(data, off, cnt);
				chunks.add(soi);
				break;
			case TEM:
				if (JPEG.DEBUG) System.out.println(" load TEM");
				JPEG_Chunk_Marker_Standalone tem = new JPEG_Chunk_Marker_Standalone();
				off = tem.load(data, off, cnt);
				chunks.add(tem);
				break;
			case EOI:
				if (JPEG.DEBUG) System.out.println(" load EOI");
				JPEG_Chunk_Marker_Standalone eoi = new JPEG_Chunk_Marker_Standalone();
				off = eoi.load(data, off, cnt);
				chunks.add(eoi);
				return;
			case DQT:
				if (JPEG.DEBUG) System.out.println(" load DQT");
				JPEG_Chunk_Marker_DQT dqt = new JPEG_Chunk_Marker_DQT();
				off = dqt.load(data, off, cnt);
				chunks.add(dqt);
				break;
			case SOF0:
			case SOF1:
			case SOF2:
			case SOF3:
			case SOF4:
			case SOF5:
			case SOF6:
			case SOF7:
			case SOF8:
			case SOF9:
			case SOF10:
			case SOF11:
			case SOF12:
				if (JPEG.DEBUG) System.out.println(" load SOF");
				JPEG_Chunk_Marker_SOF sof = new JPEG_Chunk_Marker_SOF();
				off = sof.load(data, off, cnt);
				chunks.add(sof);
				break;
			case DHT:
				if (JPEG.DEBUG) System.out.println(" load DHT");
				JPEG_Chunk_Marker_DHT dht = new JPEG_Chunk_Marker_DHT();
				off = dht.load(data, off, cnt);
				chunks.add(dht);
				break;
			case SOS:
				if (JPEG.DEBUG) System.out.println(" load SOS");
				JPEG_Chunk_Marker_SOS sos = new JPEG_Chunk_Marker_SOS();
				off = sos.load(data, off, cnt);
				chunks.add(sos);
				break;
			case DDI:
				if (JPEG.DEBUG) System.out.println(" load DDI");
			case COM:
				if (JPEG.DEBUG) System.out.println(" load COM");
				JPEG_Chunk_Marker_Payload pay = new JPEG_Chunk_Marker_Payload();
				off = pay.load(data, off, cnt);
				chunks.add(pay);
				break;
			default:
				if ((m >= RST0) && (m < RST0 + 8)) {
					if (JPEG.DEBUG) System.out.println(" load RST");
					JPEG_Chunk_Marker_Standalone rst = new JPEG_Chunk_Marker_Standalone();
					off = rst.load(data, off, cnt);
					chunks.add(rst);
					break;
				}
				if (JPEG.DEBUG) System.out.println(" load UNKNOWN");
				JPEG_Chunk_Marker_Payload pay2 = new JPEG_Chunk_Marker_Payload();
				off = pay2.load(data, off, cnt);
				chunks.add(pay2);
				break;
			}
		};
	}
	public static boolean isMarker(byte data[], int off) {
		if (data[off] != MARKER) return false;
		if (data[off+1] == MARKER) return false;
		if (data[off+1] == 0) return false;
		return true;
	}
	public static void main (String args[]) {
		try {
			String file = args[0];
			JPEG jpeg = new JPEG();
			jpeg.load(file);
			System.out.println("JPEG="+jpeg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

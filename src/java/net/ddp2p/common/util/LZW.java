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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Pattern;
public
class LZW {
	/**
	 * LZW functions with both hashtables and arrays (to be used for both decompressing and compressing).
	 * The values is stored here as strings and hashtable String->LZW_Code.
	 * No specialized dictionary class is used (some code duplication).
	 * @author msilaghi
	 *
	 * DICT_SIZE=511
	 * MAX_CODE_SIZE=12
	 * @param compressed
	 * @param initial_code_size
	 * @return
	 */
	public static byte[] LZW_Decompress(byte[] compressed, int initial_code_size) {
		byte output[] = new byte[compressed.length*4];
		int result = _LZW_Decompress(output, 0, compressed, initial_code_size, 511, 12);
		byte uncompressed[] = new byte[result];
		Util.copyBytes(uncompressed, 0, output, result, 0);
		return uncompressed;
	}
	/**
	 * 
	 * @param output
	 * @param compressed
	 * @param initial_code_size
	 * @param DICT max size dict
	 * @param MAX_CODE_SIZE
	 * @return : offset of next empty after result
	 */
	public static int _LZW_Decompress(byte[] output, int off, byte[] compressed, int initial_code_size, int DICT, int MAX_CODE_SIZE) {
		int code_end, code_clr;
		if((compressed == null)||(compressed.length==0)){
			if(LZW_Code.DEBUG_LZW) System.out.println("Decompress null");
			return 0;
		}
		if(LZW_Code.DEBUG_LZW) System.out.println("LZW_Decompress: in_len="+compressed.length+" out_len="+output.length);
		String[] rdict = new String[DICT];
		int nextcode = 1<<initial_code_size;
		long idx = 0;
		int result = off;
		int crt_code_size = initial_code_size;
		for(int i = 0; i < nextcode; i++) { rdict[i] = ":"+i;}
		crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
		 rdict[code_clr = nextcode] = "CLR"; nextcode ++;
		 rdict[code_end = nextcode] = "END"; nextcode ++;
		LZW_Code last_code = new LZW_Code();
		idx = last_code.bit_extract(compressed, idx, crt_code_size); 
		if(LZW_Code.DEBUG_LZW) System.out.println("Extracted initial: "+last_code);
		if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+rdict[last_code.code]);
		if(last_code.code == code_clr){
			nextcode = 1<<initial_code_size;
			crt_code_size = initial_code_size;
			rdict = new String[DICT];
			for(int i = 0; i < nextcode; i++) { rdict[i] = ":"+i;}
			crt_code_size++;
			 rdict[code_clr=nextcode] = "CLR"; nextcode ++;
			 rdict[code_end=nextcode] = "END"; nextcode ++;
			last_code = new LZW_Code();
			idx = last_code.bit_extract(compressed, idx, crt_code_size); 
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted after initial CLR: "+last_code);
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+rdict[last_code.code]);
			String[] v = rdict[last_code.code].split(Pattern.quote(":"));
			for(int k=1; k<v.length; k++){
				output[result++] = (byte)Integer.parseInt(v[k]);
				if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
			}
		}
		else if(last_code.code == code_end){
			idx = -1;
		}
		else {
			String[] v = rdict[last_code.code].split(Pattern.quote(":"));
			for(int k=1; k<v.length; k++){
				output[result++] = (byte)Integer.parseInt(v[k]);
				if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
			}
		}
		while(idx>=0) {
			LZW_Code code = new LZW_Code();
			idx = code.bit_extract(compressed, idx, crt_code_size); 
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted: "+code);
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+rdict[code.code]);
			if(code.code == code_clr){
				nextcode = 1<<initial_code_size;
				crt_code_size = initial_code_size;
				rdict = new String[DICT];
				for(int i = 0; i < nextcode; i++) { rdict[i] = ":"+i;}
				crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				 rdict[nextcode] = "CLR"; nextcode ++;
				 rdict[nextcode] = "END"; nextcode ++;
				last_code = new LZW_Code();
				idx = last_code.bit_extract(compressed, idx, crt_code_size); 
				if(LZW_Code.DEBUG_LZW) System.out.println("Extracted after CLR: "+last_code);
				if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+rdict[last_code.code]);
				String[] v = rdict[last_code.code].split(Pattern.quote(":"));
				for(int k=1; k<v.length; k++){
					output[result++] = (byte)Integer.parseInt(v[k]); 
					if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
				}
				continue;
			}
			else if(code.code == code_end){
				idx = -1;
				continue;
			}
			if (code.code<nextcode) {
				String[] _v = rdict[code.code].split(Pattern.quote(":"));
				for(int k=1; k<_v.length; k++) {
					output[result++] = (byte)Integer.parseInt(_v[k]); 
					if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
				}
				if(LZW_Code.DEBUG_LZW) System.out.println("Code="+code+"="+rdict[code.code]);
				String ov = rdict[code.code].split(Pattern.quote(":"))[1];
				String ad = rdict[last_code.code]+":"+ov;
				rdict[nextcode] = ad;
				if(LZW_Code.DEBUG_LZW) System.out.println("AddToDict_1 "+nextcode+":\""+ad+"\"");
				nextcode++;
				if(!LZW_Code.OLD_CODE) if(nextcode >= (1<<crt_code_size))
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				if(LZW_Code.OLD_CODE) if(nextcode+1 >= (1<<crt_code_size))
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				last_code = code;
			}else{
				if(code.code>nextcode){
					if(LZW_Code.DEBUG_LZW) System.out.println("Unknown "+nextcode+":\""+code+"\"");
					return GIF.ERROR;
				}
				if(LZW_Code.DEBUG_LZW) System.out.println("Code="+last_code+"="+rdict[last_code.code]);
				String ov = rdict[last_code.code].split(Pattern.quote(":"))[1];
				String ad = rdict[last_code.code]+":"+ov;
				rdict[nextcode] = ad;
				if(LZW_Code.DEBUG_LZW) System.out.println("AddToDict_2 "+nextcode+":\""+ad+"\"");
				nextcode++;
				if(!LZW_Code.OLD_CODE) if(nextcode >= (1<<crt_code_size))
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				if(LZW_Code.OLD_CODE) if(nextcode+1 >= (1<<crt_code_size))
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				String[] _v = rdict[code.code].split(Pattern.quote(":"));
				for(int k=1; k<_v.length; k++){
					output[result++] = (byte)Integer.parseInt(_v[k]);
					if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
				}
				last_code = code;
			}
		}
		return result;
	}
	/**
	 * 
	 * @param uncompressed
	 * @param initial_code_size
	 * @DICT size of dictionary
	 * @return
	 */
	public static byte[] LZW_Compress(byte[] _uncompressed, int initial_code_size, int bits_last_byte, int DICT, int MAX_CODE_SIZE) {
		if ((_uncompressed==null) || (_uncompressed.length == 0)) return new byte[0];
		byte uncompressed[] = new byte[((_uncompressed.length-1)*8+bits_last_byte)/initial_code_size];
		for (int k=0; k<_uncompressed.length; k++) {
			int M = 8/initial_code_size;
			if(k ==_uncompressed.length-1) M = bits_last_byte/initial_code_size;
			for (int i = 0; i < M; i++) {
				uncompressed[k*M + i] = (byte) ((byte)(_uncompressed[k]>>(i*initial_code_size))&((1<<initial_code_size) - 1));
			}
		}
		if(LZW_Code.DEBUG_LZW) System.out.println("Uncompressed: "+Util.byteToHex(uncompressed, ";"));
		String rdict[] = new String[DICT];
		Hashtable<String, LZW_Code> dict = new Hashtable<String, LZW_Code>();
		int nextcode = 1<<initial_code_size;
		byte output[] = new byte[uncompressed.length*2];
		long result = 0;
		int crt_code_size = initial_code_size;
		for(int i = 0; i < nextcode; i++) {dict.put(":"+i, new LZW_Code(crt_code_size, i)); rdict[i] = ":"+i;}
		crt_code_size ++;
		dict.put("CLR", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "CLR"; nextcode ++;
		dict.put("END", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "END"; nextcode ++;
		result = dict.get("CLR").bit_append(output, result, crt_code_size);
		String laststring = "";
		for(int i = 0; i<uncompressed.length; i++) {
			if(LZW_Code.DEBUG_LZW) System.out.println("LZW "+i+": "+uncompressed[i]);
			String currentstring = laststring + ":" + uncompressed[i];
			LZW_Code code = dict.get(currentstring);
			if(LZW_Code.OLD_CODE)
				if(nextcode >= (1 << crt_code_size)){
					if(crt_code_size >= MAX_CODE_SIZE) {
						result = dict.get("CLR").bit_append(output, result, crt_code_size);
						dict = new Hashtable<String, LZW_Code>();
						nextcode = 1<<initial_code_size;
						crt_code_size = initial_code_size;
						rdict = new String[DICT];
						for(int j = 0; j < nextcode; j++) {dict.put(":"+j, new LZW_Code(crt_code_size, j)); rdict[j] = ":"+j;}
						crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
						dict.put("CLR", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "CLR"; nextcode ++;
						dict.put("END", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "END"; nextcode ++;
					} else
						crt_code_size++;
				}
			if(code == null) {
				LZW_Code _code = dict.get(laststring);
				if(_code != null)
					result = _code.bit_append(output, result, crt_code_size);
				if(!LZW_Code.OLD_CODE)
					if(nextcode >= (1 << crt_code_size)){
						if(crt_code_size >= MAX_CODE_SIZE) {
							result = dict.get("CLR").bit_append(output, result, crt_code_size);
							dict = new Hashtable<String, LZW_Code>();
							nextcode = 1<<initial_code_size;
							crt_code_size = initial_code_size;
							rdict = new String[DICT];
							for(int j = 0; j < nextcode; j++) {dict.put(":"+j, new LZW_Code(crt_code_size, j)); rdict[j] = ":"+j;}
							crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
							dict.put("CLR", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "CLR"; nextcode ++;
							dict.put("END", new LZW_Code(crt_code_size, nextcode)); rdict[nextcode] = "END"; nextcode ++;
						}else
							crt_code_size++;
					}
				dict.put(currentstring, new LZW_Code(crt_code_size, nextcode));
				rdict[nextcode] = currentstring;
				if(LZW_Code.DEBUG_LZW) System.out.println("AddToDict "+nextcode+":\""+currentstring+"\"");
				nextcode ++;
				laststring = ":"+uncompressed[i];
			}else{
				laststring = currentstring;
			}
		}
		LZW_Code code = dict.get(laststring);
		if(code != null)
			result = code.bit_append(output, result, crt_code_size);
		result = dict.get("END").bit_append(output, result, crt_code_size);
		int bytes = (int) Math.ceil(result/8.0);
		byte compressed[] = new byte[bytes];
		Util.copyBytes(compressed, 0, output, bytes, 0);
		return compressed;
	}
	/**
	 * LZW functions with specialized dictionary classes.
	 * The values are stored there as Object->Object.
	 * 
	 * @param uncompressed
	 * @param initial_code_size
	 * @DICT size of dictionary
	 * @return
	 */
	public static byte[] LZW_Compress_Dict(byte[] _uncompressed, int initial_code_size, int bits_last_byte, int DICT, int MAX_CODE_SIZE) {
		if ((_uncompressed==null) || (_uncompressed.length == 0)) return new byte[0];
		byte uncompressed[] = new byte[((_uncompressed.length-1)*8+bits_last_byte)/initial_code_size];
		for (int k=0; k<_uncompressed.length; k++) {
			int M = 8/initial_code_size;
			if(k ==_uncompressed.length-1) M = bits_last_byte/initial_code_size;
			for (int i = 0; i < M; i++) {
				uncompressed[k*M + i] = (byte) ((byte)(_uncompressed[k]>>(i*initial_code_size))&((1<<initial_code_size) - 1));
			}
		}
		if(LZW_Code.DEBUG_LZW) System.out.println("Uncompressed: "+Util.byteToHex(uncompressed, ";"));
		LZW_Dictionary_Code_Bytes_Compress dict = new LZW_Dictionary_Code_Bytes_Compress(initial_code_size, DICT); 
		byte output[] = new byte[uncompressed.length*2];
		long result = 0;
		int crt_code_size = initial_code_size;
		crt_code_size ++;
		result = LZW.bit_append(dict.getCLR(), output, result, crt_code_size); 
		String laststring = "";
		for(int i = 0; i<uncompressed.length; i++) {
			if(LZW_Code.DEBUG_LZW) System.out.println("LZW "+i+": "+uncompressed[i]);
			String currentstring = laststring + ":" + uncompressed[i];
			int code = dict.search(currentstring);
			if(LZW_Code.OLD_CODE)
				if(dict.nextcode >= (1 << crt_code_size)){
					if(crt_code_size >= MAX_CODE_SIZE) {
						result = LZW.bit_append(dict.getCLR(), output, result, crt_code_size);
						dict.init();
						crt_code_size = initial_code_size;
						crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
					} else
						crt_code_size++;
				}
			if(code < 0) {
				int _code = dict.search(laststring);
				if(_code >= 0)
					result = LZW.bit_append(_code, output, result, crt_code_size);
				if(!LZW_Code.OLD_CODE)
					if(dict.nextcode >= (1 << crt_code_size)){
						if(crt_code_size >= MAX_CODE_SIZE) {
							result = LZW.bit_append(dict.getCLR(), output, result, crt_code_size);
							crt_code_size = initial_code_size;
							crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
						}else
							crt_code_size++;
					}
				dict.add(currentstring);
				if(LZW_Code.DEBUG_LZW) System.out.println("AddToDict "+(dict.nextcode-1)+":\""+currentstring+"\"");
				laststring = ":"+uncompressed[i];
			}else{
				laststring = currentstring;
			}
		}
		int code = dict.search(laststring);
		if(code >= 0)
			result = LZW.bit_append(code, output, result, crt_code_size);
		result = LZW.bit_append(dict.getEND(), output, result, crt_code_size);
		int bytes = (int) Math.ceil(result/8.0);
		byte compressed[] = new byte[bytes];
		Util.copyBytes(compressed, 0, output, bytes, 0);
		return compressed;
	}
	/**
	 * LZW functions with specialized dictionary classes.
	 * The values are stored there as Object->Object.
	 * 
	 * @param output
	 * @param compressed
	 * @param initial_code_size
	 * @param DICT max size dict
	 * @param MAX_CODE_SIZE
	 * @return : offset of next empty after result
	 */
	public static int LZW_Decompress_Dict(byte[] output, int off, byte[] compressed, int initial_code_size, int DICT, int MAX_CODE_SIZE) {
		if((compressed == null)||(compressed.length==0)){
			if(LZW_Code.DEBUG_LZW) System.out.println("Decompress null");
			return 0;
		}
		if(LZW_Code.DEBUG_LZW) System.out.println("LZW_Decompress: in_len="+compressed.length+" out_len="+output.length);
		LZW_Dictionary_Code_Bytes_Decompress dict = new LZW_Dictionary_Code_Bytes_Decompress(initial_code_size, DICT);
		long idx = 0;
		int result = off;
		int crt_code_size = initial_code_size;
		crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
		int last_code[] = new int[]{-1};
		idx = LZW.bit_extract(last_code, compressed, idx, crt_code_size); 
		if(LZW_Code.DEBUG_LZW) System.out.println("Extracted initial: "+last_code[0]);
		if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+dict.get(last_code[0]));
		if(last_code[0] == dict.searchCLR()){
			dict.init();
			crt_code_size = initial_code_size;
			crt_code_size++;
			last_code = new int[]{-1};
			idx = LZW.bit_extract(last_code, compressed, idx, crt_code_size); 
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted after initial CLR: "+last_code[0]);
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+dict.get(last_code[0]));
			result = dict.output(last_code[0], output, result);
		}
		else if(last_code[0] == dict.searchEND()){
			idx = -1;
		}
		else {
			result = dict.output(last_code[0], output, result);
		}
		int crt_code_size_max = 1<<crt_code_size;
		while(idx>=0) {
			int[] code = new int[]{-1};
			idx = LZW.bit_extract(code, compressed, idx, crt_code_size); 
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted: "+code[0]);
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+dict.get(code[0]));
			if(code[0] == dict.searchCLR()){
				dict.init();
				crt_code_size = initial_code_size;
				crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
				crt_code_size_max = 1<<crt_code_size;
				last_code = new int[]{-1};
				idx = LZW.bit_extract(last_code, compressed, idx, crt_code_size); 
				if(LZW_Code.DEBUG_LZW) System.out.println("Extracted after CLR: "+last_code[0]);
				if(LZW_Code.DEBUG_LZW) System.out.println("Extracted dict=: "+dict.get(last_code[0]));
				result = dict.output(last_code[0], output, result);
				continue;
			}
			else if(code[0] == dict.searchEND()){
				idx = -1;
				continue;
			}
			if (code[0]<dict.nextcode) {
				result = dict.output(code[0], output, result);
				if(LZW_Code.DEBUG_LZW) System.out.println("Code="+code+"="+dict.get(code[0]));
				Object ad = dict.append(dict.get(last_code[0]), dict.first(code[0]));
				dict.add(ad);
				if(!LZW_Code.OLD_CODE) if(dict.nextcode >= (crt_code_size_max)) {
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
					crt_code_size_max = 1<<crt_code_size;
				}
				if(LZW_Code.OLD_CODE) if(dict.nextcode+1 >= (crt_code_size_max)) {
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
					crt_code_size_max = 1<<crt_code_size;				}
				last_code[0] = code[0];
			}else{
				if(code[0]>dict.nextcode){
					if(LZW_Code.DEBUG_LZW) System.out.println("Unknown "+dict.nextcode+":\""+code+"\"");
					return GIF.ERROR;
				}
				if(LZW_Code.DEBUG_LZW) System.out.println("Code="+last_code+"="+dict.get(last_code[0]));
				Object ad = dict.appendFirst(dict.get(last_code[0]));
				dict.add(ad);
				if(LZW_Code.DEBUG_LZW) System.out.println("AddToDict_2 "+(dict.nextcode-1)+":\""+ad+"\"");
				if(!LZW_Code.OLD_CODE) if(dict.nextcode >= (crt_code_size_max)) {
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
					crt_code_size_max = 1<<crt_code_size;
				}
				if(LZW_Code.OLD_CODE) if(dict.nextcode+1 >= (crt_code_size_max)){
					crt_code_size = Math.min(crt_code_size + 1, MAX_CODE_SIZE);
					crt_code_size_max = 1<<crt_code_size;
				}
				result = dict.output(code[0], output, result);				
				last_code[0] = code[0];
			}
		}
		return result;
	}
	/**
	 * These variables are needed for the bit_extract_2 function
	 */
	static int crtbyte_idx = 0;
	static int crtbyte = 0;
	static int crtbyte_bits = 8;
	/**
	 * 
	 * Two equivalent version were implemented. They seem to be equally fast/slow ...
	 * This version is less general (assuming that the code size is less than 16, and that the function is not 
	 * running in parallel.
	 * 
	 * LSB-mode unpacking alignment
	 * @param compressed : input
	 * @param bit_idx : index next code
	 * @param crt_code_size : bits per code
	 * @return : index_next_code
	 */
	public static long bit_extract_2(int[] __code, byte[] compressed, long bit_idx, int crt_code_size) {
		if(bit_idx==0) {
			LZW.crtbyte = compressed[0]&0x000000ff;
			LZW.crtbyte_idx = 0;
			LZW.crtbyte_bits = 8;
		}
		int code = LZW.crtbyte; 
		int rem = crt_code_size - LZW.crtbyte_bits;
		LZW.crtbyte_idx ++;
		LZW.crtbyte = (compressed[LZW.crtbyte_idx])&0x000000ff;
		if(rem<=8){
			code |= (LZW.crtbyte<<LZW.crtbyte_bits);
			code &= ((1<<crt_code_size)-1);
			LZW.crtbyte >>= rem;
			LZW.crtbyte_bits = 8 - rem;
		}else{
			code |= (LZW.crtbyte<<LZW.crtbyte_bits);
			LZW.crtbyte_idx ++;
			LZW.crtbyte = (compressed[LZW.crtbyte_idx])&0x000000ff;
			code |= (LZW.crtbyte<<(LZW.crtbyte_bits+8));
			code &= ((1<<crt_code_size)-1);
			rem -= 8;
			LZW.crtbyte >>= rem;
			LZW.crtbyte_bits = 8 - rem;
		}
		__code[0] = code;
		return bit_idx + crt_code_size;
	}
	/**
	 * 
	 * Two equivalent version were implemented. They seem to be equally fast/slow ...
	 * This function is the more general version...
	 * 
	 * LSB-mode unpacking alignment
	 * @param compressed : input
	 * @param bit_idx : index next code
	 * @param crt_code_size : bits per code
	 * @return : index_next_code
	 */
	public static long bit_extract(int[] _code, byte[] compressed, long bit_idx, int crt_code_size) {
		int byte_idx = (int) (bit_idx/8);
		int bits_processed_in_this_byte = (int) (bit_idx%8);
		int bits_total_to_add_to_remaining_bytes_in_function = crt_code_size;
		int code = 0;
		int size = 0;
		while (bits_total_to_add_to_remaining_bytes_in_function > 0) {
			if(byte_idx >= compressed.length) {
				code = -1;
				_code[0] = code;
				if(LZW_Code.DEBUG_LZW) System.out.println("Extracted "+code+" from "+bit_idx+"("+crt_code_size+") : "+Util.byteToHexDump(compressed, byte_idx));
				return -1;
			}
			int crt_bits_available = 8-bits_processed_in_this_byte;
			int crt_bits_to_be_extracted_from_this_byte = Math.min(crt_bits_available, bits_total_to_add_to_remaining_bytes_in_function);
			int val_to_be_extracted_from_this_byte = (compressed[byte_idx]>>bits_processed_in_this_byte) & ((1<<crt_bits_to_be_extracted_from_this_byte) -1);
			code |= val_to_be_extracted_from_this_byte<<size;
			if(LZW_Code.DEBUG_LZW) System.out.println("Do: crt_bits_in_this_byte="+crt_bits_to_be_extracted_from_this_byte+
					" val_extract_from_byte="+val_to_be_extracted_from_this_byte+
					" occupied_in_byte="+bits_processed_in_this_byte+
					" byte_idx="+byte_idx+
					" remain_bits2add="+bits_total_to_add_to_remaining_bytes_in_function+
					" code="+code);
			size += crt_bits_to_be_extracted_from_this_byte;
			bits_total_to_add_to_remaining_bytes_in_function -= crt_bits_to_be_extracted_from_this_byte;
			byte_idx ++;
			bits_processed_in_this_byte = 0;
		}
		_code[0] = code;
		if(byte_idx >= compressed.length) return -1;
		if(LZW_Code.DEBUG_LZW) System.out.println("Extracted "+code+" from "+bit_idx+"("+crt_code_size+") : "+Util.byteToHexDump(compressed, byte_idx));
		return bit_idx + crt_code_size;
	}
	/**
	 * LSB-mode packing alignment
	 * @param output
	 * @param result
	 * @param crt_code_size
	 * @return
	 */
	public static long bit_append(int code, byte[] output, long result, int crt_code_size) {
		if(LZW_Code.DEBUG_LZW) System.out.println("Util:ap "+code+"/"+crt_code_size+" to "+result+"/"+Util.byteToHexDump(output, (int) result/8+1));
		int crt_byte = (int)Math.floor(result/8.0);
		int rem = (int)(result % 8);
		int available = crt_code_size; 
		int val = code;
		while(available > 0) {
			int available_here_bits = 8-rem;
			int here = val & ((1<<available_here_bits) - 1);
			if(LZW_Code.DEBUG_LZW) System.out.println("Util:av "+available+" av_h="+available_here_bits+" c="+crt_byte+" rem="+rem+" val="+val+" h="+here);
			here <<= rem;
			output[crt_byte] |= here;
			val >>= available_here_bits;
			rem = 0;
			crt_byte++;
			result += Math.min(available, available_here_bits);
			available = Math.max(available - available_here_bits, 0);
		}
		if(LZW_Code.DEBUG_LZW) System.out.println("Util:ap got "+result+"/"+Util.byteToHexDump(output, (int) result/8+1));
		return result;
	}
}
/**
 * Structure to store the code for the functions without specialized dictionary.
 * the "size" member is redundant (is commented out);	
 * 
 * @author msilaghi
 *
 */
class LZW_Code{
	public final static boolean DEBUG_LZW = false;
	public static final boolean OLD_CODE = false;
	int code;
	LZW_Code(int s, int c){
			code = c;
	}
	public LZW_Code() {
	}
	public String toString(){
		return "["+code+"]";
	}
	/**
		 * LSB-mode packing alignment
		 * @param output
		 * @param result
		 * @param crt_code_size
		 * @return
		 */
	public long bit_append(byte[] output, long result, int crt_code_size) {
			if(LZW_Code.DEBUG_LZW) System.out.println("Util:ap "+this+"/"+crt_code_size+" to "+result+"/"+Util.byteToHexDump(output, (int) result/8+1));
			int crt_byte = (int)Math.floor(result/8.0);
			int rem = (int)(result % 8);
			int available = crt_code_size; 
			int val = code;
			while(available > 0) {
				int available_here_bits = 8-rem;
				int here = val & ((1<<available_here_bits) - 1);
				if(LZW_Code.DEBUG_LZW) System.out.println("Util:av "+available+" av_h="+available_here_bits+" c="+crt_byte+" rem="+rem+" val="+val+" h="+here);
				here <<= rem;
				output[crt_byte] |= here;
				val >>= available_here_bits;
				rem = 0;
				crt_byte++;
				result += Math.min(available, available_here_bits);
				available = Math.max(available - available_here_bits, 0);
			}
			if(LZW_Code.DEBUG_LZW) System.out.println("Util:ap got "+result+"/"+Util.byteToHexDump(output, (int) result/8+1));
			return result;
	}
		/**
		 * 
		 * LSB-mode unpacking alignment
		 * @param compressed : input
		 * @param bit_idx : index next code
		 * @param crt_code_size : bits per code
		 * @return : index_next_code
		 */
	public long bit_extract(byte[] compressed, long bit_idx, int crt_code_size) {
			int byte_idx = (int) (bit_idx/8);
			int bits_processed_in_this_byte = (int) (bit_idx%8);
			int bits_total_to_add_to_remaining_bytes_in_function = crt_code_size;
			code = 0;
			int size = 0;
			while (bits_total_to_add_to_remaining_bytes_in_function > 0) {
				if(byte_idx >= compressed.length) {
					code = -1;
					size = -1;
					if(LZW_Code.DEBUG_LZW) System.out.println("Extracted "+this+" from "+bit_idx+"("+crt_code_size+") : "+Util.byteToHexDump(compressed, byte_idx));
					return -1;
				}
				int crt_bits_available = 8-bits_processed_in_this_byte;
				int crt_bits_to_be_extracted_from_this_byte = Math.min(crt_bits_available, bits_total_to_add_to_remaining_bytes_in_function);
				int val_to_be_extracted_from_this_byte = (compressed[byte_idx]>>bits_processed_in_this_byte) & ((1<<crt_bits_to_be_extracted_from_this_byte) -1);
				code |= val_to_be_extracted_from_this_byte<<size;
				if(LZW_Code.DEBUG_LZW) System.out.println("Do: crt_bits_in_this_byte="+crt_bits_to_be_extracted_from_this_byte+
						" val_extract_from_byte="+val_to_be_extracted_from_this_byte+
						" occupied_in_byte="+bits_processed_in_this_byte+
						" byte_idx="+byte_idx+
						" remain_bits2add="+bits_total_to_add_to_remaining_bytes_in_function+
						" code="+code);
				size += crt_bits_to_be_extracted_from_this_byte;
				bits_total_to_add_to_remaining_bytes_in_function -= crt_bits_to_be_extracted_from_this_byte;
				byte_idx ++;
				bits_processed_in_this_byte = 0;
			}
			if(byte_idx >= compressed.length) return -1;
			if(LZW_Code.DEBUG_LZW) System.out.println("Extracted "+this+" from "+bit_idx+"("+crt_code_size+") : "+Util.byteToHexDump(compressed, byte_idx));
			return bit_idx + crt_code_size;
	}
}
/**
 * Dictionary class with both hashtables and arrays (to be used for both decompressing and compressing).
 * The values is stored here as strings and hashtable String->Integer.
 * @author msilaghi
 *
 */
class LZW_Dictionary_String {
	public String rdict[];
	public Hashtable<String, Integer> dict;
	public int nextcode;
	public int initial_code_size;
	LZW_Dictionary_String(int initial_code_size, int DICT) {
		init(initial_code_size, DICT);
	}
	void init(int initial_code_size, int DICT) {
		rdict = new String[DICT];
		dict = new Hashtable<String, Integer>();
		this.initial_code_size = initial_code_size;
		nextcode = 1<<initial_code_size;
		for(int i = 0; i < nextcode; i++) {dict.put(":"+i, new Integer(i)); rdict[i] = ":"+i;}
		dict.put("CLR", new Integer(nextcode)); rdict[nextcode] = "CLR"; nextcode ++;
		dict.put("END", new Integer(nextcode)); rdict[nextcode] = "END"; nextcode ++;
	}
	boolean init() {
		if(rdict == null) return false;
		init(initial_code_size, rdict.length);
		return true;
	}
	boolean add(Object val, int code) {
		if(code != nextcode) return false;
		dict.put((String)val, new Integer(code));
		rdict[code] = (String)val;
		nextcode++;
		return true;
	}
	boolean add(Object val) {
		dict.put((String)val, new Integer(nextcode));
		rdict[nextcode] = (String)val;
		nextcode++;
		return true;
	}
	Object get(int code) {
		return rdict[code];
	}
	int search(Object val){
		return dict.get((String)val).intValue();
	}
	String first(Object val){
		return ((String)val).split(Pattern.quote(":"))[1];
	}
	String first(int code) {
		return first(get(code));
	}
	public int searchCLR() {
		return search("CLR");
	}
	public int searchEND() {
		return search("END");
	}
	private String[] getStrings(int i) {
		return ((String)get(i)).split(Pattern.quote(":"));
	}
	private byte[] getBytes(int i) {
		String[] v = getStrings(i);
		byte[]r = new byte[v.length-1];
		for(int k=1; k<v.length; k++)
			r[k-1] = (byte)Integer.parseInt(v[k]);
		return r;
	}
	public int output(int code, byte[] output, int result) {
		byte[] v = getBytes(code);
		for(int k=0; k<v.length; k++){
			output[result++] = v[k]; 
			if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
		}
		return result;
	}
	public Object append(Object head, Object tail) {
		return (String)head +":"+(String)tail;
	}
	public Object appendFirst(Object head) {
		return (String)head +":"+first(head);
	}
	public int getCLR() {
		return dict.get("CLR").intValue();
	}
	public int getEND() {
		return dict.get("END").intValue();
	}
}
/**
 * Decoding class with both hashtables and arrays (to be used for both decompressing and compressing).
 * The values is stored here as strings and an arraylist of Byte and hashtable String->Code
 * @author msilaghi
 *
 */
class LZW_Dictionary_Code_Bytes_String {
	class Code{
		ArrayList<Byte> a_val;
		String s_val;
		int code;
		Code(String _val, int _code){
			s_val = _val;
			code = _code;
			a_val = new ArrayList<Byte>();
		}
		Code(String _val, int _code, byte b){
			s_val = _val;
			code = _code;
			a_val = new ArrayList<Byte>();
			a_val.add(new Byte(b));
		}
		public Code(String _val, int _code, ArrayList<Byte> bytes) {
			s_val = _val;
			code = _code;
			a_val =  bytes;
		}
		public Code(String _val, ArrayList<Byte> bytes) {
			s_val = _val;
			a_val =  bytes;
		}
		public Code setCode(int _code) {
			code = _code;
			return this;
		}
	}
	public Code rdict[];
	public Hashtable<String, Code> dict;
	public int nextcode;
	public int initial_code_size;
	LZW_Dictionary_Code_Bytes_String(int initial_code_size, int DICT) {
		init(initial_code_size, DICT);
	}
	void init(int initial_code_size, int DICT) {
		Code cod;
		if(rdict==null) 
			rdict = new Code[DICT];
			dict = new Hashtable<String, Code>(DICT*4/3+10);
		this.initial_code_size = initial_code_size;
		nextcode = 1<<initial_code_size;
		for(int i = 0; i < nextcode; i++) {cod =  new Code(":"+i, i, (byte)i); dict.put(cod.s_val, cod); rdict[i] = cod;}
		cod =  new Code("CLR", nextcode); dict.put("CLR", cod); rdict[nextcode] = cod; nextcode ++;
		cod =  new Code("END", nextcode); dict.put("END", cod); rdict[nextcode] = cod; nextcode ++;
	}
	boolean init() {
		if(rdict == null) return false;
		init(initial_code_size, rdict.length);
		return true;
	}
	boolean add(Object val, int code) {
		if(code != nextcode) return false;
		dict.put(((Code)val).s_val, ((Code)val).setCode(code));
		rdict[code] = (Code)val;
		nextcode++;
		return true;
	}
	boolean add(Object val) {
		dict.put(((Code)val).s_val, ((Code)val).setCode(nextcode));
		rdict[nextcode] = (Code)val;
		nextcode++;
		return true;
	}
	Code get(int code) {
		return rdict[code];
	}
	int search(Code val){
		return dict.get(val.s_val).code;
	}
	int search(String val){
		return dict.get(val).code;
	}
	Byte first(Code val){
		return ((Code)val).a_val.get(0);
	}
	Byte first(int code) {
		return first(get(code));
	}
	public int searchCLR() {
		return search("CLR");
	}
	public int searchEND() {
		return search("END");
	}
	public int output(int code, byte[] output, int result) {
		ArrayList<Byte> A = rdict[code].a_val;
		for(Byte a : A){
			output[result++] = a; 
			if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
		}
		return result;
	}
	public Code append(Code head, Byte tail) {
		ArrayList<Byte> bytes = new ArrayList<Byte>(((Code)head).a_val);
		bytes.add((Byte)tail);
		Code cod = new Code(((Code)head).s_val +":"+((Byte)tail), bytes);
		return cod;
	}
	public Code appendFirst(Code head) {
		ArrayList<Byte> a = new ArrayList<Byte>(head.a_val);
		Byte f = a.get(0);
		a.add(f);
		return new Code(head.s_val +":"+ head.a_val.get(0),  a);
	}
	public int getCLR() {
		return dict.get("CLR").code;
	}
	public int getEND() {
		return dict.get("END").code;
	}
}
/**
 * Decoding class with both hashtables and arrays (to be used for both decompressing and compressing).
 * The values is stored here as an arraylist of Byte and hashtable ArrayList<Byte>->Code
 * @author msilaghi
 *
 */
class LZW_Dictionary_Code_Bytes {
	class Code{
		ArrayList<Byte> a_val;
		int code;
		Code(int _code){
			code = _code;
			a_val = new ArrayList<Byte>();
		}
		Code(int _code, byte b){
			code = _code;
			a_val = new ArrayList<Byte>();
			a_val.add(new Byte(b));
		}
		public Code(int _code, ArrayList<Byte> bytes) {
			code = _code;
			a_val =  bytes;
		}
		public Code(ArrayList<Byte> bytes) {
			a_val =  bytes;
		}
		public Code setCode(int _code) {
			code = _code;
			return this;
		}
	}
	public Code rdict[];
	public Hashtable<Object, Code> dict;
	public int nextcode;
	public int initial_code_size;
	public int code_clr;
	public int code_end;
	LZW_Dictionary_Code_Bytes(int initial_code_size, int DICT) {
		init(initial_code_size, DICT);
	}
	void init(int initial_code_size, int DICT) {
		Code cod;
		if(rdict==null) rdict = new Code[DICT];
			dict = new Hashtable<Object, Code>(DICT*4/3+10);
		this.initial_code_size = initial_code_size;
		nextcode = 1<<initial_code_size;
		for(int i = 0; i < nextcode; i++) {cod =  new Code(i, (byte)i); dict.put(cod.a_val, cod); rdict[i] = cod;}
		cod =  new Code(code_clr=nextcode); dict.put("CLR", cod); rdict[nextcode] = cod; nextcode ++;
		cod =  new Code(code_end=nextcode); dict.put("END", cod); rdict[nextcode] = cod; nextcode ++;
	}
	boolean init() {
		if(rdict == null) return false;
		init(initial_code_size, rdict.length);
		return true;
	}
	boolean add(Object val, int code) {
		if(code != nextcode) return false;
		dict.put(((Code)val).a_val, ((Code)val).setCode(code));
		rdict[code] = (Code)val;
		nextcode++;
		return true;
	}
	boolean add(Object val) {
		dict.put(((Code)val).a_val, ((Code)val).setCode(nextcode));
		rdict[nextcode] = (Code)val;
		nextcode++;
		return true;
	}
	Code get(int code) {
		return rdict[code];
	}
	int search(Code val){
		return dict.get(val.a_val).code;
	}
	private int search(String val){
		return dict.get(val).code;
	}
	Byte first(Code val){
		return ((Code)val).a_val.get(0);
	}
	Byte first(int code) {
		return first(get(code));
	}
	public int searchCLR() {
		return code_clr; 
	}
	public int searchEND() {
		return code_end; 
	}
	public int output(int code, byte[] output, int result) {
		ArrayList<Byte> A = rdict[code].a_val;
		for(Byte a : A){
			output[result++] = a; 
			if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
		}
		return result;
	}
	public Code append(Code head, Byte tail) {
		ArrayList<Byte> bytes = new ArrayList<Byte>(((Code)head).a_val);
		bytes.add((Byte)tail);
		Code cod = new Code(bytes);
		return cod;
	}
	public Code appendFirst(Code head) {
		ArrayList<Byte> a = new ArrayList<Byte>(head.a_val);
		Byte f = a.get(0);
		a.add(f);
		return new Code(a);
	}
	public int getCLR() {
		return dict.get("CLR").code;
	}
	public int getEND() {
		return dict.get("END").code;
	}
}
/**
 * Decoding class without hashtables (to be used only for decompressing).
 * The values is stored here as an arraylist of Byte
 * @author msilaghi
 *
 */
class LZW_Dictionary_Code_Bytes_Decompress {
	class Code{
		ArrayList<Byte> a_val;
		int code;
		Code(int _code){
			code = _code;
			a_val = new ArrayList<Byte>();
		}
		Code(int _code, byte b){
			code = _code;
			a_val = new ArrayList<Byte>();
			a_val.add(new Byte(b));
		}
		public Code(int _code, ArrayList<Byte> bytes) {
			code = _code;
			a_val =  bytes;
		}
		public Code(ArrayList<Byte> bytes) {
			a_val =  bytes;
		}
		public Code setCode(int _code) {
			code = _code;
			return this;
		}
	}
	public Code rdict[];
	public int nextcode;
	public int initial_code_size;
	public int code_clr;
	public int code_end;
	LZW_Dictionary_Code_Bytes_Decompress(int initial_code_size, int DICT) {
		init(initial_code_size, DICT);
	}
	void init(int initial_code_size, int DICT) {
		Code cod;
		if(rdict==null) rdict = new Code[DICT];
		this.initial_code_size = initial_code_size;
		nextcode = 1<<initial_code_size;
		for(int i = 0; i < nextcode; i++) {cod =  new Code(i, (byte)i); rdict[i] = cod;}
		cod =  new Code(code_clr=nextcode); rdict[nextcode] = cod; nextcode ++;
		cod =  new Code(code_end=nextcode); rdict[nextcode] = cod; nextcode ++;
	}
	boolean init() {
		if(rdict == null) return false;
		init(initial_code_size, rdict.length);
		return true;
	}
	boolean add(Object val, int code) {
		if(code != nextcode) return false;
		rdict[code] = (Code)val;
		nextcode++;
		return true;
	}
	boolean add(Object val) {
		rdict[nextcode] = (Code)val;
		nextcode++;
		return true;
	}
	Code get(int code) {
		return rdict[code];
	}
	Byte first(Code val){
		return ((Code)val).a_val.get(0);
	}
	Byte first(int code) {
		return first(get(code));
	}
	public int searchCLR() {
		return code_clr; 
	}
	public int searchEND() {
		return code_end; 
	}
	public int output(int code, byte[] output, int result) {
		ArrayList<Byte> A = rdict[code].a_val;
		for(Byte a : A){
			output[result++] = a; 
			if(LZW_Code.DEBUG_LZW) System.out.println("OUT["+(result-1)+"] = "+Util.byteToHex(output[result-1]));
		}
		return result;
	}
	public Code append(Code head, Byte tail) {
		ArrayList<Byte> bytes = new ArrayList<Byte>(((Code)head).a_val);
		bytes.add((Byte)tail);
		Code cod = new Code(bytes);
		return cod;
	}
	public Code appendFirst(Code head) {
		ArrayList<Byte> a = new ArrayList<Byte>(head.a_val);
		Byte f = a.get(0);
		a.add(f);
		return new Code(a);
	}
	public int getCLR() {
		return code_clr;
	}
	public int getEND() {
		return code_end;
	}
}
/**
 * Decoding class without array (to be used only for compressing).
 * The values is stored here as an hashtable from ArrayList<Byte> -> Code
 * @author msilaghi
 *
 */
class LZW_Dictionary_Code_Bytes_Compress {
	class Code{
		ArrayList<Byte> a_val;
		int code;
		Code(int _code){
			code = _code;
			a_val = new ArrayList<Byte>();
		}
		Code(int _code, byte b){
			code = _code;
			a_val = new ArrayList<Byte>();
			a_val.add(new Byte(b));
		}
		public Code(int _code, ArrayList<Byte> bytes) {
			code = _code;
			a_val =  bytes;
		}
		public Code(ArrayList<Byte> bytes) {
			a_val =  bytes;
		}
		public Code setCode(int _code) {
			code = _code;
			return this;
		}
	}
	public Hashtable<Object, Code> dict;
	public int nextcode;
	public int initial_code_size;
	public int code_clr;
	public int code_end;
	static int DICT;
	LZW_Dictionary_Code_Bytes_Compress(int initial_code_size, int DICT) {
		LZW_Dictionary_Code_Bytes_Compress.DICT = DICT;
		init(initial_code_size, DICT);
	}
	void init(int initial_code_size, int DICT) {
		Code cod;
		dict = new Hashtable<Object, Code>(DICT*4/3+10);
		this.initial_code_size = initial_code_size;
		nextcode = 1<<initial_code_size;
		for(int i = 0; i < nextcode; i++) {cod =  new Code(i, (byte)i); dict.put(cod.a_val, cod); }
		cod =  new Code(code_clr=nextcode); dict.put("CLR", cod);  nextcode ++;
		cod =  new Code(code_end=nextcode); dict.put("END", cod);  nextcode ++;
	}
	boolean init() {
		init(initial_code_size, LZW_Dictionary_Code_Bytes_Compress.DICT);
		return true;
	}
	boolean add(Object val, int code) {
		if(code != nextcode) return false;
		dict.put(((Code)val).a_val, ((Code)val).setCode(code));
		nextcode++;
		return true;
	}
	boolean add(Object val) {
		dict.put(((Code)val).a_val, ((Code)val).setCode(nextcode));
		nextcode++;
		return true;
	}
	int search(Code val){
		return dict.get(val.a_val).code;
	}
	int search(String val){
		return dict.get(val).code;
	}
	Byte first(Code val){
		return ((Code)val).a_val.get(0);
	}
	public int searchCLR() {
		return code_clr; 
	}
	public int searchEND() {
		return code_end; 
	}
	public Code append(Code head, Byte tail) {
		ArrayList<Byte> bytes = new ArrayList<Byte>(((Code)head).a_val);
		bytes.add((Byte)tail);
		Code cod = new Code(bytes);
		return cod;
	}
	public Code appendFirst(Code head) {
		ArrayList<Byte> a = new ArrayList<Byte>(head.a_val);
		Byte f = a.get(0);
		a.add(f);
		return new Code(a);
	}
	public int getCLR() {
		return dict.get("CLR").code;
	}
	public int getEND() {
		return dict.get("END").code;
	}
}

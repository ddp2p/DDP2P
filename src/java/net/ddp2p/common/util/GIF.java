/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package net.ddp2p.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;


class LogicalScreenDescriptor implements GIFBlock {
	byte width_16le[] = {16,0};
	byte height_16le[] = {16,0};
	/**
	 *  0-2: log_2(GCTsize)-1, 3: GCTsort, 4-6: bpp-1, 7:GCT
	 *  Default 0x70 (8bppc)
	 */
	byte mBitField = 0x70; 
	/**
	 * Default 0
	 */
	byte mBackgroundColor = 0;
	/**
	 * Default 0
	 */
	byte mDefaultPixelAspectRatio = 0;

	public String toString() {
		String r = "";
		r += "\tWidth : "+ Util.le16_to_cpu(width_16le)+"\n";
		r += "\tHeight: "+ Util.le16_to_cpu(height_16le)+"\n";
		r += "\tFlags : "+ Util.byteToHex(new byte[]{mBitField})+"h 0-2:GCTlog="+getLog2GCTSize()+" GCTsz="+getGCTSize()+" 3:GCTsort="+isGCTSorted()+" 4-6:bpp="+getGCTbppc()+" 7:GCT="+hasGCT()+"\n";
		r += "\tBackgr: "+ mBackgroundColor+"\n";
		r += "\tPixRatio: "+ mDefaultPixelAspectRatio+" i.e. "+this.getRatio();
		return r;
	}
	
	boolean isBit(int bit){
		return (mBitField&(1<<bit))>0;
	}
	void setBit(boolean val, int bit){
		mBitField &= ~(1<<bit);
		if(val) mBitField |= (1<<bit);
	}
	
	boolean isGCTSorted(){
		return (mBitField & 8)>0;
	}
	void setGCTSorted(boolean sorted){
		mBitField &= ~8;
		if(sorted) mBitField |= 8;
	}
	
	boolean hasGCT(){
		return (mBitField&128)>0;
	}
	void setGCT(boolean gct){
		mBitField &= ~128;
		if(gct) mBitField |= 128;
	}
	/**
	 * 
	 * @return N+1
	 */
	int getLog2GCTSize() {
		return 1+(mBitField & 7);
	}
	/**
	 * 
	 * @return 1 << (N+1)
	 */
	int getGCTSize() {
		return 1<<(1+(mBitField & 7));
	}
	/**
	 * Must be power of 2^exponent
	 * @param exponent
	 * @return
	 */
	void setLog2GCTSize(int exponent){
		if(exponent>8) exponent = 7;
		mBitField &= ~7;
		mBitField |= exponent-1;
	}
	/**
	 * GlobalColorTable bits per pixel color
	 * @return
	 */
	int getGCTbppc(){
		return 1+((mBitField>>4) & 7);
	}
	void setGCTbppc(int bppc){
		if(bppc>8) bppc = 7;
		mBitField &= ~(7<<4);
		mBitField |= (bppc-1)<<4;
	}
	/**
	 * Length in bytes of neded buffer
	 */
	public int getSize() {
		return 7;
	}
	/**
	 * width/height
	 * @return
	 */
	public double getRatio() {
		if(mDefaultPixelAspectRatio==0) return 1.0;
		return (this.mDefaultPixelAspectRatio+15)/64.0;
	}
	public void setRatio(double r) {
		if(r==1.0) mDefaultPixelAspectRatio=0;
		this.mDefaultPixelAspectRatio = (byte)Math.round(Math.floor((r*64.0)-15.0));
	}
	public int fillBuffer(byte[]buf, int start){
		Util.copyBytes(buf, start, width_16le, 2, 0);
		Util.copyBytes(buf, start+2, height_16le, 2, 0);
		buf[start+4] = mBitField;
		buf[start+5] = mBackgroundColor;
		buf[start+6] = mDefaultPixelAspectRatio;
		return start+7;
	}
	public int fillStructure(byte[]buf, int start){
		Util.copyBytes(width_16le, 0, buf, 2, start);
		Util.copyBytes(height_16le, 0, buf, 2, start+2);
		mBitField = buf[start+4];
		mBackgroundColor = buf[start+5];
		mDefaultPixelAspectRatio = buf[start+6];
		return start+7;
	}
}

class ColorTable implements GIFBlock{
	/**
	 * R, G, B
	 */
	byte colors[][] = new byte[0][3];
	/**
	 * 
	 * @param size : length of the table
	 */
	ColorTable(int size) {
		init(size);
	}
	public String toString(){
		String r = "";
		if(colors==null) return r;
		for(int k=0; k< colors.length; k++)
			r += "\t"+ k+": " + Util.byteToHex(colors[k], ", ")+"\n";
		return r;
	}
	void init(int size) {
		colors = new byte[size][3];
	}
	public int getSize() {
		return colors.length*3;
	}
	public int fillBuffer(byte[]buf, int start){
		for(int k=0; k<colors.length; k++){
			Util.copyBytes(buf, start, colors[k], 3, 0);
			start += 3;
		}
		return start;
	}
	public int fillStructure(byte[]buf, int start){
		for(int k=0; k<colors.length; k++){
			Util.copyBytes(colors[k], 0, buf, 3, start);
			start += 3;
		}
		return start;
	}
}
interface GIFBlock{
	int getSize();
	public int fillBuffer(byte[]buf, int start);
	public int fillStructure(byte[]buf, int start);	
}
interface GIFComponent extends GIFBlock{
	byte getType();
}
class GIFCommentFrag implements GIFBlock {
	byte data[] = new byte[0];

	public String toString() {
		String r = "";
		r += "\t\""+Util.byteToString(data)+"\"";
		return r;
	}
	
	@Override
	public int getSize() {
		return 1+data.length;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = (byte)data.length;
		Util.copyBytes(buf, start, data, data.length, 0);
		start+=data.length;
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		int size = Util.byte_to_uint(buf[start++]);
		data = new byte[size];
		Util.copyBytes(data, 0, buf, data.length, start);
		start+=data.length;
		return start;
	}
}
/**
 Comment Extension Block

Offset   Length   Contents
  0      1 byte   Extension Introducer (0x21)
  1      1 byte   Comment Label (0xfe)
[
         1 byte   Block Size (s)
        (s)bytes  Comment Data
]*
         1 byte   Block Terminator(0x00)
         
 * @author msilaghi
 *
 */
class GIFCommentBlock implements GIFComponent {
	ArrayList<GIFCommentFrag> data = new ArrayList<GIFCommentFrag>();
	
	/**
	 * 
	 * @param msg
	 * @param off : the first position copied into the buffers
	 * @param end : the position past the last to be copied into the buffers (off + length)
	 */
	void setMessage(byte msg[], int off, int end) {
		data = new ArrayList<GIFCommentFrag>();
		if (msg == null){
			data.add(new GIFCommentFrag());
			return;
		}
		// int blocks = (int) Math.ceil(m.length/255.0);
		for (; off < end; off += GIF.MAX_FRAG) {
			GIFCommentFrag gcf = new GIFCommentFrag();
			int crt_len = Math.min(GIF.MAX_FRAG, end-off);
			gcf.data = new byte[crt_len];
			Util.copyBytes(gcf.data, 0, msg, crt_len, off);
			data.add(gcf);
		}
		data.add(new GIFCommentFrag());
	}
	public int getMessageSize() {
		int sz = 0;
		for(GIFCommentFrag f : data){
			sz += f.data.length;
		}
		return sz;
	}
	/**
	 * Should have at least getMessageSize() bytes available after "offset".
	 * @param msg
	 * @param offset
	 * @return : final offset
	 */
	int getMessage(byte msg[], int off) {
		for(GIFCommentFrag f : data){
			Util.copyBytes(msg, off, f.data, f.data.length, 0); off += f.data.length;
		}
		return off;
	}
	
	public String toString() {
		String r = "";

		byte[] msg = new byte[getMessageSize()];
		getMessage(msg, 0);
		r += "Comment Fragment:\n\t\""+Util.byteToString(msg)+"\"";
		/*
		for (GIFCommentFrag f : data) {
			r += "Comment Fragment:\n"+f+"\n";
		}
		*/
		return r;
	}
	
	@Override
	public int getSize() {
		int sz = 2;
		for(GIFCommentFrag f : data){
			sz += f.getSize();
		}
		return sz;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = GIF.BLOCK_EXTENSION;
		buf[start++] = GIF.EXTENSION_COMMENT;
		for(GIFCommentFrag f : data){
			start = f.fillBuffer(buf, start);
		}
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start++] != GIF.BLOCK_EXTENSION) return GIF.ERROR;
		if(buf[start++] != GIF.EXTENSION_COMMENT) return GIF.ERROR;
		GIFCommentFrag f;
		do {
			if (GIF.DEBUG) System.out.println("CommentBlock:fillStructure frag start="+start);
			f = new GIFCommentFrag();
			start = f.fillStructure(buf, start);
			data.add(f);
		}while(f.getSize() > 1);
		if (GIF.DEBUG) System.out.println("CommentBlock:fillStructure end start="+start);
		return start;
	}

	@Override
	public byte getType() {
		return GIF.BLOCK_EXTENSION;
	}
	
}
class GIFAnimationApplicationBlock implements GIFBlock {
	final static byte mBlockSize = 3;
	final static byte mExtensionType = 1;
	/**
	 * infinite uses 0 as repeat count
	 */
	byte[] mRepeatCount = new byte[]{0,0};
	
	public String toString() {
		String r = "";
		r += "\tRepeat Count: "+Util.le16_to_cpu(mRepeatCount)+" (0 stands for infinity)";
		return r;
	}
	
	public GIFAnimationApplicationBlock() {
	}
	
	public GIFAnimationApplicationBlock(GIFBlock gifBlock) {
		if(gifBlock instanceof GIFAnimationApplicationBlock) {
			GIFAnimationApplicationBlock gaab = (GIFAnimationApplicationBlock)gifBlock;
			Util.copyBytes(mRepeatCount, 0, gaab.mRepeatCount, 2, 0);
			return;
		}
		if(gifBlock instanceof GIFCommentFrag) {
			GIFCommentFrag f = (GIFCommentFrag) gifBlock;
			if(f.data.length != mBlockSize) throw new RuntimeException("Wrong Animation Application Data length: "+f.data.length);
			if(f.data[0] != mExtensionType) throw new RuntimeException("Wrong Animation Extension Type: "+f.data[0]);
			Util.copyBytes(mRepeatCount, 0, f.data, 2, 1);
			return;
		}
		throw new RuntimeException("Unsupported Animation Extension Data type");
	}

	@Override
	public int getSize() {
		return 4;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = mBlockSize;
		buf[start++] = mExtensionType;
		Util.copyBytes(buf, start, mRepeatCount, 2, 0); start +=2;
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start++] != mBlockSize) return GIF.ERROR;
		if(buf[start++] != mExtensionType) return GIF.ERROR;
		Util.copyBytes(mRepeatCount, 0, buf, 2, start); start +=2;
		return start;
	}
	
}
class GIFApplicationHeader implements GIFBlock {
	final static byte mBlockSize = 11;
	byte [] mApplicationID = new byte[8];
	byte [] mAuthenticationCode = new byte[3];
	
	public String toString() {
		String r = "";
		r += "ApplicationID: \""+Util.byteToString(mApplicationID)+"\"\n";
		r += "Authentication Code: \""+Util.byteToString(this.mAuthenticationCode)+"\"\n";
		return r;
	}
	
	public boolean isAnimatedGIF() {
		if(!Util.equalBytes(mApplicationID, new byte[]{'N','E','T','S','C','A','P','E'})) return false;
		if(!Util.equalBytes(mAuthenticationCode, new byte[]{'2','.','0'})) return false;
		return true;
	}
	
	@Override
	public int getSize() {
		return 12;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = mBlockSize;
		Util.copyBytes(buf, start, mApplicationID, 8, 0); start += 8;
		Util.copyBytes(buf, start, mAuthenticationCode, 3, 0); start += 3;
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start++] != mBlockSize) return GIF.ERROR;
		Util.copyBytes(mApplicationID, 0, buf, 8, start); start += 8;
		Util.copyBytes(mAuthenticationCode, 0, buf, 3, start); start += 3;
		return start;
	}
	
}
class GIFApplicationBlock implements GIFComponent {
	GIFApplicationHeader header = new GIFApplicationHeader();
	ArrayList<GIFBlock> data = new ArrayList<GIFBlock>();
	
	public String toString() {
		String r = "";
		r += "GIFApplicationHeader:\n"+header+"\n";
		try {
			if(header.isAnimatedGIF() && (data.size()==2)) {
				GIFAnimationApplicationBlock gaab = new GIFAnimationApplicationBlock(data.get(0)); 
				r += "Animation Parameters: \n"+gaab;
				return r;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		for(GIFBlock d : data) {
			r += "\t"+d+"\n";
		}
		return r;
	}
	
	@Override
	public int getSize() {
		int sz = 2;
		sz += header.getSize();
		for(GIFBlock f : data){
			sz += f.getSize();
		}
		sz += 1; //for termination byte //Rakesh
		return sz;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = GIF.BLOCK_EXTENSION;
		buf[start++] = GIF.EXTENSION_APPLICATION;
		start = header.fillBuffer(buf, start);
		for(GIFBlock f : data){
			start = f.fillBuffer(buf, start);
		}
		buf[start++] = GIF.EXTENSION_DATA_SUB_BLOCK_TERMINATOR; // Rakesh
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start++] != GIF.BLOCK_EXTENSION) return GIF.ERROR;
		if(buf[start++] != GIF.EXTENSION_APPLICATION) return GIF.ERROR;
		//System.out.println("b."+start); //Rakesh
		start = header.fillStructure(buf, start);
		GIFCommentFrag f;
		do {
			f = new GIFCommentFrag();
			start = f.fillStructure(buf, start);
			data.add(f);
		}while(f.getSize() > 1);
		return start;
	}

	@Override
	public byte getType() {
		return GIF.BLOCK_EXTENSION;
	}
}
/**
Plain Text Extension Block

Offset   Length   Contents
  0      1 byte   Extension Introducer (0x21)
  1      1 byte   Plain Text Label (0x01)
  2      1 byte   Block Size (0x0c)
  3      2 bytes  Text Grid Left Position
  5      2 bytes  Text Grid Top Position
  7      2 bytes  Text Grid Width
  9      2 bytes  Text Grid Height
 10      1 byte   Character Cell Width(
 11      1 byte   Character Cell Height
 12      1 byte   Text Foreground Color Index(
 13      1 byte   Text Background Color Index(
[
         1 byte   Block Size (s)
        (s)bytes  Plain Text Data
]*
         1 byte   Block Terminator(0x00)
  
 * @author msilaghi
 *
 */
class PlaintextExtensionHeader implements GIFBlock{
	final static byte block_size = 12;
	byte mTextGridLeft[] = {0,0};
	byte mTextGridRight[] = {0,0};
	byte mTextGridWidth[] = {0,1};
	byte mTextGridHeight[] = {10,0};
	byte mCharacterCellWidth = 8;
	byte mCharacterCellHeight = 8;
	byte mTextForegroungColor = 0;
	byte mTextBackgroungColor = 1;	
	
	@Override
	public int getSize() {
		return block_size+1;
	}
	

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start] = block_size;
		Util.copyBytes(buf, start+1, mTextGridLeft, 2, 0);
		Util.copyBytes(buf, start+3, mTextGridRight, 2, 0);
		Util.copyBytes(buf, start+5, mTextGridWidth, 2, 0);
		Util.copyBytes(buf, start+7, mTextGridHeight, 2, 0);
		buf[start+9] = mCharacterCellWidth;
		buf[start+10] = mCharacterCellHeight;
		buf[start+11] = mTextForegroungColor;
		buf[start+12] = mTextBackgroungColor;
		return start+13;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start]!=block_size) return GIF.ERROR;
		Util.copyBytes(mTextGridLeft, 0, buf, 2, start+1);
		Util.copyBytes(mTextGridRight, 0, buf, 2, start+3);
		Util.copyBytes(mTextGridWidth, 0, buf, 2, start+5);
		Util.copyBytes(mTextGridHeight, 0, buf, 2, start+7);
		mCharacterCellWidth = buf[start+9];
		mCharacterCellHeight = buf[start+10];
		mTextForegroungColor = buf[start+11];
		mTextBackgroungColor = buf[start+12];		
		return start+13;
	}
}
class GraphicsControlExtension implements GIFBlock{
	final byte mBlockSize = 4;
	byte mBitFields; // 0: transparent_index_used, 1: wait user; 2-4: disposalMethod 
	byte mDelayTime[]={0,0}; // 1/100th of a second
	byte mTransparentColorIndex = 0;
	
	public String toString() {
		String r = "";
		r += "\tFlags : "+Util.byteToHex(new byte[]{mBitFields})+"h"+
				" 0:Transp="+this.getTransparentUsed()+
				" 1:Wait_User="+this.getWaitUserInput()+
				" Disp="+this.getDisposalMethodName()+" ("+this.getDisposalMethod()+")\n";
		r += "\tDelay : "+Util.le16_to_cpu(mDelayTime)+" *10ms\n";
		r += "\tTransp: "+mTransparentColorIndex+" (index)";
		return r;
	}
	
	private String getDisposalMethodName() {
		switch(getDisposalMethod()){
		case GIF.DISPOSAL_LEAVE_INPLACE: return "LEAVE";
		case GIF.DISPOSAL_NOACTION: return "NOACTION";
		case GIF.DISPOSAL_RESTORE_BACKGROUND: return "BACKGROUND";
		case GIF.DISPOSAL_RESTORE_PREVIOUS: return "PREVIOUS";
		default: return "UNKNOWN";
		}
	}

	boolean getTransparentUsed(){
		return (mBitFields & 1) > 0;
	}
	void setTransparentUsed(boolean val){
		mBitFields &= ~1;
		if(val) mBitFields |= 1;
	}
	boolean getWaitUserInput(){
		return (mBitFields & 2) > 0;
	}
	void setWaitUserInput(boolean val){
		mBitFields &= ~2;
		if(val) mBitFields |= 2;
	}
	void setDisposalMethod(int method){
		if(method>7) method = 7;
		mBitFields &= ~(7<<2);
		mBitFields |= (method)<<2;
	}
	int getDisposalMethod(){
		return (mBitFields>>2) & 7;
	}
	
	@Override
	public int getSize() {
		return 3+mBlockSize+1;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = GIF.BLOCK_EXTENSION;
		buf[start++] = GIF.EXTENSION_GCE_IMAGE;
		
		buf[start++] = mBlockSize;
		buf[start++] = mBitFields;
		Util.copyBytes(buf, start, mDelayTime, 2, 0); start +=2;
		buf[start++] = mTransparentColorIndex;
		buf[start++] = 0; // terminator
		
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if (GIF.BLOCK_EXTENSION != buf[start++]) {
			if (GIF.DEBUG) System.out.println("GCE:fillStructure: missing extension marker");
			return GIF.ERROR;
		}
		if (GIF.EXTENSION_GCE_IMAGE != buf[start++]) {
			if (GIF.DEBUG) System.out.println("GCE:fillStructure: missing extension type marker");
			return GIF.ERROR;
		}
		if (mBlockSize != buf[start++]){
			if (GIF.DEBUG) System.out.println("GCE:fillStructure: wrong extension block size: "+buf[start-1]);
			return GIF.ERROR;
		}
		mBitFields = buf[start++];
		Util.copyBytes(mDelayTime, 0, buf, 2, start); start += 2;
		mTransparentColorIndex = buf[start++];
		if (0 != buf[start++]) {
			if (GIF.DEBUG) System.out.println("GCE:fillStructure: missing extension block terminator");
			return GIF.ERROR;
		}
		return start;
	}
}
class PlaintextData implements GIFBlock{
	byte data[] = "Silaghi".getBytes();
	@Override
	public int getSize() {
		return 1+data.length;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start] = (byte)data.length;
		Util.copyBytes(buf, start+1, data, data.length, 0);
		return start+data.length+1;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		data = new byte[buf[start]];
		Util.copyBytes(data, 0, buf, data.length, start+1);
		return start+data.length+1;
	}
	
}
interface GIFContentBody extends GIFBlock{
}
class GIFImageData implements GIFContentBody  {
	byte mLeftPosition[] = {0,0};
	byte mTopPosition[] = {0,0};
	byte mImageWidth[] = {3,0};
	byte mImageHeight[] = {4,0};
	/**
	 * 0-2: LCT size; 5: sort; 6: Interlaced; 7: LCT flag
	 */
	byte mBitField = 0;
	
	ColorTable mLocalColorTable = null;
	
	byte mInitialCodeSize = 8;
	
	ArrayList<ImageData> data = new ArrayList<ImageData>();
	byte uncompressed[] = null;
	byte compressed[] = null;
	

	void setMessage(byte msg[]) {
		setMessage(msg, 8, 8);
	}
	void setMessage(byte msg[], int initial_code_size, int bits_last_byte) {
		data = new ArrayList<ImageData>();
		uncompressed = null;
		compressed = null;
		if (msg == null){
			data.add(new ImageData());
			return;
		}
		uncompressed = Arrays.copyOf(msg, msg.length);
		compressed = LZW.LZW_Compress(uncompressed, initial_code_size, bits_last_byte, 1<<GIF.MAX_CODE_SIZE, GIF.MAX_CODE_SIZE);
		// int blocks = (int) Math.ceil(m.length/255.0);
		for (int off = 0; off < compressed.length; off += GIF.MAX_FRAG) {
			ImageData gcf = new ImageData();
			int crt_len = Math.min(GIF.MAX_FRAG, compressed.length-off);
			gcf.setCompressed(compressed, off, crt_len);
			data.add(gcf);
		}
		data.add(new ImageData());
	}
	public int getCompressedMessageSize() {
		int sz = 0;
		for(ImageData f : data){
			sz += f.getSize()-1;
		}
		return sz;
	}
	/**
	 * Should have at least getCompressedMessageSize() bytes available after "offset".
	 * @param m
	 * @param offset
	 * @return : new offset
	 */
	int getCompressedMessage(byte m[], int off) {
		for(ImageData f : data){
			Util.copyBytes(m, off, f.getCompressed(), f.getCompressed().length, 0); off += f.getCompressed().length;
		}
		return off;
	}
	int getUncompressedMessage(byte m[], int off, int initial_code_size) {
		byte compressed[] = new byte[getCompressedMessageSize()];
		getCompressedMessage(compressed, 0);
		/*
		byte uncompressed[] = Util.LZW_Decompress(compressed, initial_code_size);
		Util.copyBytes(m, off, uncompressed, uncompressed.length, 0);
		return off+uncompressed.length;
		*/
		int r = LZW._LZW_Decompress(m, 0, compressed, initial_code_size, 1<<GIF.MAX_CODE_SIZE, GIF.MAX_CODE_SIZE);
		return off+r;
	}
	
	public String toString() {
		int width, height;
		String r = "";
		r += "\tLeft Position : "+ Util.le16_to_cpu(mLeftPosition)+"\n";
		r += "\tTop Position  : "+ Util.le16_to_cpu(mTopPosition)+"\n";
		r += "\tImage Width   : "+ (width=Util.le16_to_cpu(mImageWidth))+"\n";
		r += "\tImage Height  : "+ (height=Util.le16_to_cpu(mImageHeight))+"\n";
		r += "\tFlags : "+Util.byteToHex(new byte[]{mBitField})+ "h 0-2:LCT"+this.getLog2LCTSize()+" ("+this.getLCTSize()+") 5:LCTsort="+this.isLCTSorted()+" 6:interlaced="+this.isLCTInterlaced()+" 7:LCT="+this.hasLCT()+"\n";

		if(mLocalColorTable != null) r+="Local Color Table:\n"+mLocalColorTable+"\n";
		r +=  "\tInitial Code Size: "+mInitialCodeSize+"\n";
		if(uncompressed != null) {
			for(int off = 0; off < uncompressed.length; off += width)
				r += "\t"+Util.byteToHex(uncompressed, off, Math.min(width,uncompressed.length-off), " ")+"\n";
			return r;
		}
		try{
			byte msg[] = new byte[(int) Math.ceil(width*height*mInitialCodeSize/8.0)];
			int off = this.getUncompressedMessage(msg, 0, mInitialCodeSize);
			if (GIF.DEBUG) System.out.println("Size w="+width+" h="+height+" codesz="+mInitialCodeSize+" in="+this.getCompressedMessageSize()+" off->"+off);
			String MSG = Util.byteToHex(msg, 0, off, "");
			r+= Util.trimmed("Data:->"+off+"\n\t"+MSG, 10);
		}catch(Exception e){
			if (GIF.DEBUG) System.out.println("Fail to decompress");
			e.printStackTrace();
			byte msg[] = new byte[this.getCompressedMessageSize()];
			int off = this.getCompressedMessage(msg, 0);
			String MSG = Util.byteToHex(msg, 0, off, "");
			r+= Util.trimmed("Compressed Data:\n\t"+MSG, 10);
		}

		/*
		for(ImageData d : data) {
			r += "ImageDataFragment:\n"+d+"\n";
		}
		*/
		return r;
	}
	
	boolean isLCTInterlaced(){
		return (mBitField & 64)>0;
	}
	void setLCTInterlaced(boolean interlaced){
		mBitField &= ~64;
		if(interlaced) mBitField |= 64;
	}
	boolean isLCTSorted(){
		return (mBitField & 32)>0;
	}
	void setLCTSorted(boolean sorted){
		mBitField &= ~32;
		if(sorted) mBitField |= 32;
	}
	boolean hasLCT(){
		return (mBitField&128)>0;
	}
	void setLCT(boolean lct){
		mBitField &= ~128;
		if(lct) mBitField |= 128;
	}
	/**
	 * 
	 * @return N+1
	 */
	int getLog2LCTSize() {
		return 1+(mBitField & 7);
	}
	/**
	 * 
	 * @return 1 << (N+1)
	 */
	int getLCTSize() {
		return 1<<(1+(mBitField & 7));
	}
	/**
	 * Must be power of 2^exponent
	 * @param exponent
	 * @return
	 */
	void setLog2LCTSize(int exponent){
		if (exponent > 8) exponent = 7;
		mBitField &= ~7;
		mBitField |= exponent-1;
	}

	
	@Override
	public int getSize() {
		int sz = 1+9;
		sz += getLocalColorTableSize();
		sz += 1; // initial compression code size
		for(ImageData pd : data) {
			sz += pd.getSize();
		}		
		return sz;
	}

	private int getLocalColorTableSize() {
		if(this.mLocalColorTable == null) return 0;
		return this.mLocalColorTable.getSize();
	}
	@Override
	public int fillBuffer(byte[] buf, int start) {
		buf[start++] = GIF.BLOCK_IMAGE;
		Util.copyBytes(buf, start, mLeftPosition, 2, 0); start+=2;
		Util.copyBytes(buf, start, mTopPosition, 2, 0); start+=2;
		Util.copyBytes(buf, start, mImageWidth, 2, 0); start+=2;
		Util.copyBytes(buf, start, mImageHeight, 2, 0); start+=2;
		buf[start++] = this.mBitField;
		if(this.mLocalColorTable != null)
			start = this.mLocalColorTable.fillBuffer(buf, start);
		buf[start++] = mInitialCodeSize;
		for(ImageData pd : data) {
			start = pd.fillBuffer(buf, start);
		}		
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		if(buf[start++] != GIF.BLOCK_IMAGE) return GIF.ERROR;
		Util.copyBytes(mLeftPosition, 0, buf, 2, start); start+=2;
		Util.copyBytes(mTopPosition, 0, buf, 2, start); start+=2;
		Util.copyBytes(mImageWidth, 0, buf, 2, start); start+=2;
		Util.copyBytes(mImageHeight, 0, buf, 2, start); start+=2;
		this.mBitField = buf[start++];
		if(this.hasLCT()) {
			mLocalColorTable = new ColorTable(getLCTSize());
			start = mLocalColorTable.fillStructure(buf, start);
		}
		mInitialCodeSize = buf[start++];
		ImageData pd;
		do{
			//if (GIF.DEBUG) System.out.println("GIFImageData:fillStructure  loop at: "+start);
			pd = new ImageData(); 
			start = pd.fillStructure(buf, start);
			pd.initial_code_size = this.mInitialCodeSize;
			data.add(pd);
		}while(pd.getSize() > 1);		
		if (GIF.DEBUG) System.out.println("GIFImageData end at: "+start+" loops="+data.size()+" len="+this.getCompressedMessageSize());
		return start;
	}
	
}
class ImageData implements GIFBlock{
	private byte compressed[] = null;
	private byte uncompressed[] = new byte[0];
	public int initial_code_size = 8;
	int bits_last_byte;
	ImageData(){
		
	}
	public byte[] getCompressed() {
		return compressed;
	}
	public void setCompressed(byte[] _compressed, int off, int crt_len) {
		setCompressed(_compressed, off, crt_len, 8, 8);
	}
	public void setCompressed(byte[] _compressed, int off, int crt_len,
			int _initial_code_size, int _bits_last_byte) {
		uncompressed = null;
		compressed = new byte[crt_len];
		Util.copyBytes(compressed, 0, _compressed, crt_len, off);
		initial_code_size = _initial_code_size;
		bits_last_byte = _bits_last_byte;
	}
	public String toString() {
		String r = "";
		if(uncompressed!=null)
			r += "\tU: "+Util.byteToHex(uncompressed, " ") + "/"+bits_last_byte+"."+initial_code_size;
		else
			r+="\tC: "+Util.byteToHex(compressed, " ");
		return r;
	}
	@Override
	public int getSize() {
		if ( (uncompressed == null) && (compressed == null) ) return 0;
		if (compressed == null) compress();
		return 1 + compressed.length;
	}

	private void compress() {
		if(compressed != null) return;
		if(uncompressed == null) compressed = new byte[0];
		compressed = Util.LZW_Compress(uncompressed, initial_code_size, bits_last_byte, 511, GIF.MAX_CODE_SIZE);
	}
	private void uncompress() {
		if (uncompressed != null) return;
		if (compressed == null) uncompressed = new byte[0];
		try {
			byte[] _uncompressed = Util.LZW_Decompress(compressed, initial_code_size);
			bits_last_byte = Util.bytePack(_uncompressed, initial_code_size, uncompressed = new byte[(int)Math.ceil(_uncompressed.length*initial_code_size/8.0)]);
		} catch(Exception e) {
			if (GIF.DEBUG) System.out.println("Error uncompressing image");
		}
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		compress();
		buf[start++] = (byte)compressed.length;
		Util.copyBytes(buf, start, compressed, compressed.length, 0);
		return start+compressed.length;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		int size = Util.byte_to_uint(buf[start++]);
		compressed = new byte[size];
		Util.copyBytes(compressed, 0, buf, compressed.length, start);
		uncompressed = null;
		uncompress();
		return start + compressed.length;
	}

	public void setUncompressed(byte[] _uncompressed, int initial_code_size, int bits_last_byte) {
		this.uncompressed = _uncompressed;
		compressed = null;
		this.bits_last_byte = bits_last_byte;
		this.initial_code_size = initial_code_size;
	}
}
class PlainTextExtention implements GIFContentBody {
	PlaintextExtensionHeader mPlaintextExtensionHeader
		= new PlaintextExtensionHeader();
	ArrayList<PlaintextData> data = new ArrayList<PlaintextData>();
	@Override
	public int getSize() {
		int sz = 0;
		sz += mPlaintextExtensionHeader.getSize();
		for(PlaintextData pd : data) {
			sz += pd.getSize();
		}
		return sz;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		start = mPlaintextExtensionHeader.fillBuffer(buf, start);
		//start += mPlaintextExtensionHeader.getSize();
		for(PlaintextData pd : data) {
			start = pd.fillBuffer(buf, start);
			//start += pd.getSize();
		}
		return start;
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		start = mPlaintextExtensionHeader.fillStructure(buf, start);
		//start += mPlaintextExtensionHeader.getSize();
		/*
		if(buf[start]!=0){
			PlaintextData pd = new PlaintextData();
			start = pd.fillStructure(buf, start);
			//start += pd.getSize();
			data.add(pd);
		}
		*/
		PlaintextData pd;
		do{
			pd = new PlaintextData();
			start = pd.fillStructure(buf, start);
			data.add(pd);
		}while(pd.getSize() > 1);
		return start;
	}
	
}
class GIFContent implements GIFComponent{
	GraphicsControlExtension mGraphicsControlExtension; //= new GraphicsControlExtension();
	GIFContentBody mGIFContentBody; // = new GIFContentBody();
	public String toString() {
		String r = "";
		if(mGraphicsControlExtension != null)
			r += "GraphicsControlExtension:\n"+mGraphicsControlExtension+"\n";
		r += "ImageData:\n"+mGIFContentBody;
		return r;
	}
	public int fillBuffer(byte[]buf, int start){
		if(mGraphicsControlExtension != null) {
			start = mGraphicsControlExtension.fillBuffer(buf, start);
			//start += mGraphicsControlExtension.getSize();
		}
		start = mGIFContentBody.fillBuffer(buf, start);
		return start;
	}
	public int fillStructure(byte[]buf, int start){
		if (GIF.DEBUG) System.out.println("GIFContent:fillStructure at: "+start);
		if(buf[start] == GIF.BLOCK_EXTENSION) {
			mGraphicsControlExtension = new GraphicsControlExtension();
			if(mGraphicsControlExtension != null) {
				start = mGraphicsControlExtension.fillStructure(buf, start);
				//start += mGraphicsControlExtension.getSize();
			}
		}
		if (GIF.DEBUG) System.out.println("GIFContent:fillStructure Image Content at: "+start);
		mGIFContentBody = new GIFImageData();
		start = mGIFContentBody.fillStructure(buf, start);
		return start;
	}
	@Override
	public int getSize() {
		int sz = 0;
		if(mGraphicsControlExtension != null)
			sz += mGraphicsControlExtension.getSize();
		return sz+this.mGIFContentBody.getSize();
	}
	@Override
	public byte getType() {
		if(mGraphicsControlExtension != null)
			return GIF.BLOCK_EXTENSION;
		return GIF.BLOCK_IMAGE;
	}
	
}
class GIFComponentUnknown implements GIFComponent {
	byte data[] = new byte[0];

	@Override
	public int getSize() {
		return data.length;
	}

	@Override
	public int fillBuffer(byte[] buf, int start) {
		Util.copyBytes(buf, start, data, getSize(), 0);
		return start+getSize();
	}

	@Override
	public int fillStructure(byte[] buf, int start) {
		int sz = size(buf, start);
		data = new byte[sz];
		Util.copyBytes(data, 0, buf, sz, start);
		return start+data.length;
	}

	private int size(byte[] buf, int start) {
		int sz = 2;
		int cnt = start + 2;
		while(buf[cnt] != 0) {
			int inc = buf[cnt] + 1;
			sz  += inc;
			cnt += inc;
		}
		return sz+1;
	}

	@Override
	public byte getType() {
		return data[0];
	}
	
}

public class GIF {
	public static final int MAX_CODE_SIZE = 12;
	public static final int MAX_FRAG = 0xFF;
	public static final int ERROR = -2;
	final static int DISPOSAL_NOACTION = 0;
	final static int DISPOSAL_LEAVE_INPLACE = 1;
	final static int DISPOSAL_RESTORE_BACKGROUND = 2;
	final static int DISPOSAL_RESTORE_PREVIOUS = 3;

	static final byte BLOCK_EXTENSION = 0x21;
	static final byte BLOCK_IMAGE = 0x2C;
	static final byte BLOCK_TERMINATOR = 0x3B;

	static final byte EXTENSION_PLAINTEXT = 0x1;
	static final byte EXTENSION_GCE_IMAGE = (byte) 0xF9;
	static final byte EXTENSION_COMMENT = (byte) 0xFE;
	static final byte EXTENSION_APPLICATION = (byte) 0xFF;
	static final byte  EXTENSION_DATA_SUB_BLOCK_TERMINATOR = 0x00; // Rakesh
	static final boolean DEBUG = false;

	final byte header[]={'G','I','F'};
	byte version[]={'8','9','a'};
	private LogicalScreenDescriptor mLogicalScreenDescriptor = new LogicalScreenDescriptor();
	private ColorTable mGlobalColorTable = null;
	private ArrayList<GIFComponent> content = new ArrayList<GIFComponent>();
	GIF(){
	}
	GIF(boolean gif87){
		version[1] = '7';
	}
	GIF GIF_Embed (byte[] data, byte[] file){
		GIF gif = new GIF();
		gif.fillStructure(file, 0);
		GIFCommentBlock gcb = new GIFCommentBlock();
		gcb.setMessage(data, 0, data.length);
		gif.getContent().add(0, gcb);
		return gif;
	}
	public
	boolean GIF_Embed(byte[] data, File in, File out) {
		long MAX_FILE = 2000000;
		if(in.length() > MAX_FILE ) return false;
		int in_len = (int)Math.min(MAX_FILE, in.length());
		byte buf[] = new byte[in_len];
		try {
			Util.readAll(new FileInputStream(in), buf);
			GIF gif = GIF_Embed(data, buf);
			int out_len = gif.getContentSize();
			buf = new byte[out_len];
			gif.fillBuffer(buf, 0);
			FileOutputStream fos = new FileOutputStream(out);
			//BufferedOutputStream bos = new BufferedOutputStream(fos);
			fos.write(buf);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public
	byte[] GIF_Extract(File in) {
		long MAX_FILE = 2000000;
		//if(in.length() > MAX_FILE ) return null;
		int in_len = (int)Math.min(MAX_FILE, in.length());
		byte buf[] = new byte[in_len];
		try {
			Util.readAll(new FileInputStream(in), buf);
			GIF gif = new GIF();
			gif.fillStructure(buf, 0, 1);
			GIFComponent e = gif.getContent().get(0);
			if(! (e instanceof GIFCommentBlock)) return null;
			GIFCommentBlock gcb = (GIFCommentBlock) e;
			
			int out_len = gcb.getMessageSize();
			buf = new byte[out_len];
			gcb.getMessage(buf, 0);
			return buf;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	byte[] content(){
		int size = getContentSize();
		byte[] result = new byte[size];
		fillBuffer(result, 0);
		return result;
	}
	public int fillStructure(byte []buf, int start) {
		return fillStructure(buf, start, -1);
	}
	public int fillStructure(byte []buf, int start, int max_blocks) {
		Util.copyBytes(header, 0, buf, header.length, start); start += header.length;
		Util.copyBytes(version, 0, buf, version.length, start); start += version.length;
		start = getmLogicalScreenDescriptor().fillStructure(buf, start); //start += mLogicalScreenDescriptor.getSize();
		if(getmLogicalScreenDescriptor().hasGCT()) {	
			this.setmGlobalColorTable(new ColorTable(this.getmLogicalScreenDescriptor().getGCTSize()));
			start = getmGlobalColorTable().fillStructure(buf, start);
		}
		if (GIF.DEBUG) System.out.println("GIF read header "+start+" -> \n"); //+ this);
		while(start < buf.length) {
			if((max_blocks >= 0) && (getContent().size() >= max_blocks)) return start;
			if (GIF.DEBUG) System.out.println("GIF read content "+start);
			switch(buf[start]) {
			case GIF.BLOCK_TERMINATOR:
				if (GIF.DEBUG) System.out.println("GIF read content terminator");
				return start+1;
			case GIF.BLOCK_EXTENSION:
				if (GIF.DEBUG) System.out.println("GIF read content extension");
				switch(buf[start+1]) {
				case GIF.EXTENSION_GCE_IMAGE:
					if (GIF.DEBUG) System.out.println("GIF read content extension gce image");
					break; // go to image
				case GIF.EXTENSION_COMMENT:
					if (GIF.DEBUG) System.out.println("GIF read content extension comment");
					GIFCommentBlock commentBlock = new GIFCommentBlock();
					start = commentBlock.fillStructure(buf, start);
					getContent().add(commentBlock);
					continue;
				case GIF.EXTENSION_APPLICATION:
					if (GIF.DEBUG) System.out.println("GIF read content extension application");
					GIFApplicationBlock applicationBlock = new GIFApplicationBlock();
					start = applicationBlock.fillStructure(buf, start);
					getContent().add(applicationBlock);
					continue;
				case GIF.EXTENSION_PLAINTEXT:
					if (GIF.DEBUG) System.out.println("GIF read content extension plaintext");
					// break; // not implemented
				default:
					if (GIF.DEBUG) System.out.println("GIF read content extension unknown");
					GIFComponentUnknown unknown = new GIFComponentUnknown();
					start = unknown.fillStructure(buf, start); //start += unknown.getSize();
					getContent().add(unknown);
					continue;
				}
			case GIF.BLOCK_IMAGE:
				if (GIF.DEBUG) System.out.println("GIF read content image block");
				GIFContent content_block = new GIFContent();
				start = content_block.fillStructure(buf, start); //start += content_block.getSize();
				getContent().add(content_block);
				break;
			default:
				if (GIF.DEBUG) System.out.println("GIF read content unknown block id: "+Util.byteToHex(buf[start]));
				GIFComponentUnknown unknown = new GIFComponentUnknown();
				start = unknown.fillStructure(buf, start); //start += unknown.getSize();
				getContent().add(unknown);
				continue;
			}
		}
		return start;
	}
	public int fillBuffer(byte[]buf, int start) {
		Util.copyBytes(buf, start, header, header.length, 0); start += header.length;
		Util.copyBytes(buf, start, version, version.length, 0); start += version.length;
		start = getmLogicalScreenDescriptor().fillBuffer(buf, start); //start += mLogicalScreenDescriptor.getSize();
		if(this.getmGlobalColorTable()!=null) start = getmGlobalColorTable().fillBuffer(buf, start); //start += mGlobalColorTable.getSize();
		
		for(GIFComponent c : getContent()) {
			start = c.fillBuffer(buf, start); //start +=  c.getSize();
		}
		
		buf[start] = BLOCK_TERMINATOR;
		
		return start+1;
	}
	public String toString() {
		String result = "";
		result += "Header: \""+Util.byteToString(header)+"\"\n";
		result += "Version: \""+Util.byteToString(version)+"\"\n";
		result += "Logical Screen Descriptor: \n"+this.getmLogicalScreenDescriptor()+"\n";
		if(getmGlobalColorTable() != null) result += "Global Color Table: \n"+getmGlobalColorTable()+"\n";
		for(GIFComponent c : getContent()) {
			result += "Component: \n"+c+"\n";
		}
		if (GIF.DEBUG) System.out.println("EOF");
		return result;
	}
	private int getContentSize() {
		int sz =
				6 //header + version
				+ getLSDSize()
				+ getGCDSize()
				+ 1; //terminator
		for(GIFComponent c:getContent()){
			sz += c.getSize();
		}
		return sz;
	}
	private int getGCDSize() {
		if(getmGlobalColorTable()!=null) return getmGlobalColorTable().getSize();
		return 0;
	}
	private int getLSDSize() {
		if(getmLogicalScreenDescriptor()!=null) return getmLogicalScreenDescriptor().getSize();
		return 0;
	}
	/**
	 * Test LZW encoding
	 * @param args
	 */
	public static void main_1(String args[]) {
		try{
			int initial_code_size = Integer.parseInt(args[1]);
			int bits_last_byte = Integer.parseInt(args[2]);
			String d[] = args[3].split(Pattern.quote(":"));
			if(d!=null)if (GIF.DEBUG) System.out.println("In: "+args[3]+" --> "+d.length);
			byte data[] = Util.hexToBytes(d);
			if (GIF.DEBUG) System.out.println("Data: "+Util.byteToHex(data));
			if("c".equals(args[0])){
				if (GIF.DEBUG) System.out.println(Util.byteToHex(LZW.LZW_Compress(data, initial_code_size, bits_last_byte, 511, 12), ":"));
			}else{
				if (GIF.DEBUG) System.out.println(Util.byteToHex(LZW.LZW_Decompress(data, initial_code_size), ":"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * Generate an GIF
	 * @param args
	 */
	public static void main_2(String args[]) {
		try{
			genmain(args);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void genmain(String args[]) {
		GIF gif = new GIF();
		gif.getmLogicalScreenDescriptor().width_16le = Util.cpu_to_16le(30);
		gif.getmLogicalScreenDescriptor().height_16le = Util.cpu_to_16le(20);
		//gif.mLogicalScreenDescriptor.mBackgroundColor = 0;
		//gif.mLogicalScreenDescriptor.setGCTSorted(false);
		gif.getmLogicalScreenDescriptor().setLog2GCTSize(8);
		//gif.mLogicalScreenDescriptor.setRatio(0);

		gif.getmLogicalScreenDescriptor().setGCTbppc(8);
		gif.getmLogicalScreenDescriptor().setGCT(true);
		
		gif.setmGlobalColorTable(new ColorTable(gif.getmLogicalScreenDescriptor().getGCTSize()));
		gif.getmGlobalColorTable().colors[0][2] = (byte) 0xFF; // blue
		
		GIFContent gc = new GIFContent();
		GIFImageData gid = new GIFImageData();
		gid.mLeftPosition = new byte[]{0,0};
		gid.mTopPosition = new byte[]{0,0};
		gid.mImageWidth = new byte[]{4,0};
		gid.mImageHeight = new byte[]{4,0};
		gid.setLCT(false);
		ImageData id = new ImageData();
		byte[] uncompressed=new byte[]{0,1,0,1, 0,1,0,1, 0,1,0,1, 0,1,0,1};
		id.setUncompressed(uncompressed, 8, 8);
		gid.data.add(id);
		gid.data.add(new ImageData());
		
		gc.mGIFContentBody = gid;
		gif.getContent().add(gc);
		
		byte[] buf = gif.content();
		String fname = "test.gif";
		if(args.length>0) fname = args[0];
		if (GIF.DEBUG) System.out.println("Gen: "+fname);
		Util.writeFile(buf, new File(fname));
	}
	public static void main(String args[]) {
		if(args.length < 1) {
			if (GIF.DEBUG) System.out.println("GIF name absent");
			return;
		}
		String fname = args[0];
		if (GIF.DEBUG) System.out.println("GIF: "+fname);
		File file = new File(args[0]);
		if(! file.isFile()) {
			if (GIF.DEBUG) System.out.println("Not a regular file");
			return;
		}
		long f_length = file.length();
		final long MAX_FILE_READ = 2000000;
		int length = (int) Math.min(f_length, MAX_FILE_READ );
		if (GIF.DEBUG) System.out.println("GIF length: "+length);
		byte buf[] = new byte[length];
		try {
			int available = Util.readAll(new FileInputStream(file), buf, length);
			if (GIF.DEBUG) System.out.println("GIF read: "+available);
			GIF gif = new GIF();
			int sz = gif.fillStructure(buf, 0);
			if(sz > available) if (GIF.DEBUG) System.out.println("Gif incomplete: "+sz+">"+available);
			//if (GIF.DEBUG) 
				System.out.println("GIF: "+gif);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public LogicalScreenDescriptor getmLogicalScreenDescriptor() {
		return mLogicalScreenDescriptor;
	}
	void setmLogicalScreenDescriptor(LogicalScreenDescriptor mLogicalScreenDescriptor) {
		this.mLogicalScreenDescriptor = mLogicalScreenDescriptor;
	}
	ColorTable getmGlobalColorTable() {
		return mGlobalColorTable;
	}
	void setmGlobalColorTable(ColorTable mGlobalColorTable) {
		this.mGlobalColorTable = mGlobalColorTable;
	}
	ArrayList<GIFComponent> getContent() {
		return content;
	}
	void setContent(ArrayList<GIFComponent> content) {
		this.content = content;
	}
}

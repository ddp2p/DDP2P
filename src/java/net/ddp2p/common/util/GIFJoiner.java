/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Rakesh Banatho
		Author: Rakesh Banatho: msilaghi@fit.edu
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
import java.io.IOException;



public class GIFJoiner{

	static byte[] width;
	static byte[] height;
	
	public static void main(String args[]){
		String[] inputGif;
		if(args.length>0){
			inputGif = new String[args.length];
			for(int i = 0; i<inputGif.length;i++){
				inputGif[i] = args[i];
			}
		}
		else inputGif = new String[]{"c:\\a.gif","c:\\b.gif", "c:\\c.gif"};
		GIF gif = initgif();
		for(int i = 0; i<inputGif.length;i++){
			gif = addImage(gif, inputGif[i]);	
		}
		
		
//		GIFAnimationApplicationBlock gaa = new GIFAnimationApplicationBlock();
//		GIFApplicationBlock gab = new GIFApplicationBlock();
//		gaa.mRepeatCount = new byte[]{0,0};
//		gab.data.add(gaa);
//		gif.content.add(gab);
		
		byte[] buf = gif.content();
		String fname = "out.gif";
		if (GIF.DEBUG) System.out.println("Gen: "+fname);
		Util.writeFile(buf, new File(fname));
		System.out.println("Created animated gif file with name: "+fname);
		System.out.println("Final Size: "+buf.length);
		System.out.println(gif.getmLogicalScreenDescriptor());
			
	}
	
	public static GIF initgif(){
		GIF gif = new GIF();
		System.out.println("Creating initial GIF file");
		System.out.println("Creating LogicalScreenDescriptor");
		
		gif.getmLogicalScreenDescriptor().width_16le = Util.cpu_to_16le(0);
		gif.getmLogicalScreenDescriptor().height_16le = Util.cpu_to_16le(0);
		
		gif.getmLogicalScreenDescriptor().setGCT(false); //no gct
		gif.getmLogicalScreenDescriptor().setGCTbppc(8); //res bit depth (8bit)
		gif.getmLogicalScreenDescriptor().setGCTSorted(false);
		gif.getmLogicalScreenDescriptor().setLog2GCTSize(8); //2^8 256colors for color table
		gif.setmGlobalColorTable(new ColorTable(gif.getmLogicalScreenDescriptor().getGCTSize()));
//		gif.mGlobalColorTable.colors[0][0] = (byte) 0xFF; // Red
//		gif.mGlobalColorTable.colors[1][1] = (byte) 0xFF; // Green
//		//gif.mGlobalColorTable.colors[2][2] = (byte) 0xFF; // Blue
//		
		GIFContent gc = new GIFContent();
		System.out.println("Creating animated application block");
		GIFApplicationBlock gab = new GIFApplicationBlock();
		GIFApplicationHeader gah = new GIFApplicationHeader();
		gah.mApplicationID = new byte[]{'N','E','T','S','C','A','P','E'};
		gah.mAuthenticationCode = new byte[]{'2','.','0'};
		gab.header = gah; 
		GIFAnimationApplicationBlock gaa = new GIFAnimationApplicationBlock();
		gaa.mRepeatCount = new byte[]{0,0};
		gab.data.add(gaa);
		gif.getContent().add(gab);
		
		return gif;
	}
	
	public static GIF getGIF(String image_path){
		File file = new File(image_path);
		long f_length = file.length();
		final long MAX_FILE_READ = 2000000; //why??
		int len = (int) Math.min(f_length, MAX_FILE_READ ); 
		GIF gif = new GIF();
		byte buf[] = new byte[len];
		try {
			int available = Util.readAll(new FileInputStream(file), buf, len);
			int sz = gif.fillStructure(buf, 0);
			System.out.println("size: "+sz);
//			System.out.println(gif.content);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return gif;
	}
	
	
	private static GIF addImage(GIF gif, String image){
		GIF in_gif = getGIF(image);
		System.out.println("->Adding image from "+image);
		GIF out_gif = gif;
		
		
		GIFContent gc = new GIFContent();
		GIFContent in_gc = new GIFContent();
		for(GIFComponent gcom : in_gif.getContent()){
			if(gcom instanceof GIFContent){
				in_gc = (GIFContent) gcom;	
			}
		}

		GraphicsControlExtension gce = new GraphicsControlExtension();
		gce.mDelayTime = new byte[]{100,0};
		
		GIFImageData in_gid = (GIFImageData) in_gc.mGIFContentBody;
		ImageData in_id = in_gid.data.get(0);
		
		//width height
		int in_width = Util.le16_to_cpu(in_gid.mImageWidth);
		int in_height = Util.le16_to_cpu(in_gid.mImageHeight);
		
		int out_width = Util.le16_to_cpu(gif.getmLogicalScreenDescriptor().width_16le);
		int out_height = Util.le16_to_cpu(gif.getmLogicalScreenDescriptor().height_16le);
		
		if(in_width > out_width) gif.getmLogicalScreenDescriptor().width_16le = Util.cpu_to_16le(in_width);
		if(in_height > out_height) gif.getmLogicalScreenDescriptor().height_16le = Util.cpu_to_16le(in_height);
		
		
		GIFImageData gid = new GIFImageData();
		gid.mLeftPosition = new byte[]{0,0};
		gid.mTopPosition = new byte[]{0,0};
		gid.mImageWidth = Util.cpu_to_16le(in_width);
		gid.mImageHeight = Util.cpu_to_16le(in_height);

		if(out_gif.getmLogicalScreenDescriptor().hasGCT()){
			System.out.println(image);
			System.out.println("GCT is TRUE already so put in lct");
			gid.setLCT(true);
			
			if(in_gif.getmGlobalColorTable() != null){
				System.out.println("copying from gct");
				int size = in_gif.getmGlobalColorTable().getSize();
				int size2 = in_gif.getmLogicalScreenDescriptor().getLog2GCTSize();
				gid.setLog2LCTSize(size2);	
				gid.mLocalColorTable = in_gif.getmGlobalColorTable();
			}
			else if(in_gid.mLocalColorTable != null){
				System.out.println("copying from lct");
				int size = in_gid.mLocalColorTable.getSize();
				gid.setLog2LCTSize(size);
				gid.mLocalColorTable = in_gid.mLocalColorTable;
			}
				
		}
		else {
			System.out.println(image);
			System.out.println("GCT is false so put in gct");
			gif.getmLogicalScreenDescriptor().setGCT(true); //add gct
			if(in_gif.getmGlobalColorTable() != null){
				System.out.println("copying from gct");
				out_gif.getmLogicalScreenDescriptor().setLog2GCTSize(in_gif.getmLogicalScreenDescriptor().getLog2GCTSize());	
				out_gif.setmGlobalColorTable(in_gif.getmGlobalColorTable());
			}
			else if(in_gid.mLocalColorTable != null){
				System.out.println("copying from lct");
				out_gif.getmLogicalScreenDescriptor().setLog2GCTSize(in_gid.mLocalColorTable.getSize());
				out_gif.setmGlobalColorTable(in_gid.mLocalColorTable);
			}
		}
		
		gid.data.add(in_id);
		gid.data.add(new ImageData());

		gc.mGraphicsControlExtension = gce;
		gc.mGIFContentBody = gid;

		out_gif.getContent().add(gc);
		//System.out.println(out_gif);
		return out_gif;
		
	}
}

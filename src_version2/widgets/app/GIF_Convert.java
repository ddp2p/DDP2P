//package GIF2Bitmap;
package widgets.app;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.*;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.stream.*;

import util.BMP;
import util.DD_Address;
import util.EmbedInMedia;
import util.StegoStructure;
import static util.Util.__;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.HandlingMyself_Peer;
import util.DBInterface;

public class GIF_Convert {
	/**
	 * @param args
	 */
	public static void copyImage(File source, File des, String type){

       int typeBI = BufferedImage.TYPE_3BYTE_BGR;
       try{BufferedImage bi = ImageIO.read (source);
          if (type.equals("gif")) typeBI = BufferedImage.TYPE_BYTE_GRAY;
	      BufferedImage ob = new BufferedImage (bi.getWidth(), bi.getHeight(), typeBI);
	      for(int x=0; x< bi.getWidth() ; x++)
	      	 for(int y= 0; y< bi.getHeight() ; y++)
	      	 { int pixel = bi.getRGB (x,y);  // get the value at col,row
	            ob.setRGB (x,y,pixel); }       // set the value at col,row 
	     
	      	ImageIO.write (ob, type, des);
      }catch(IOException e){
      	 System.out.println(e);
      }
	}
	public static String convertFromGIF_to_BMP(String gifPath, String bmpPath){
			try{
			File input = new File(gifPath);
			// or Object input = new FileInputStream("animated.gif");
			ImageInputStream stream = ImageIO.createImageInputStream(input);
			Iterator readers = (Iterator) ImageIO.getImageReaders(stream);
			if (!readers.hasNext())
			throw new RuntimeException("no image reader found");
			ImageReader reader = (ImageReader) readers.next();
			reader.setInput(stream); // don't omit this line!
			int n = reader.getNumImages(true); // don't use false!
			System.out.println("numImages = " + n);
			String filename=null;
			for (int i = 0; i < n; i++) {
			BufferedImage image = reader.read(i);
				//new BufferedImage(reader.read(i).getWidth(), reader.read(i).getHeight(), BufferedImage.TYPE_3BYTE_BGR );
			 //System.out.println("Orginal image[" + i + "] = " + image);
			//AffineTransformOp identityOp = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR);
            //image = identityOp.filter(image, null);
            
           	//image = reader.read(i);
			System.out.println("image[" + i + "] = " + image.getData().getDataBuffer().getSize());
			filename= bmpPath.substring(0,bmpPath.lastIndexOf(".bmp"));
			filename+=i;
			filename+=".bmp";
			File output = new File(filename);
			BufferedImage buffImg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_3BYTE_BGR); 
			final Graphics2D g2 = buffImg.createGraphics();   
			g2.drawImage(image, null, null);   
			g2.dispose();   	  
			//image.setRGB(0,0,0);
			//image.setData(Raster)  
			//Write the image to the destination as a BMP  
			System.out.println(ImageIO.write(buffImg, "bmp", output));  
			System.out.println("Length: "+output.length());
			
			/////// convert back to gif //////
//			image = ImageIO.read(output);
//			File output2 = new File("c:/gif/imageWithAdd.gif");
//			 System.out.println(ImageIO.write(image, "gif", output2));
			//stream = ImageIO.createImageInputStream(output); // output become input streem
			}
			stream.close();
			if(filename!=null) return bmpPath.substring(0,bmpPath.lastIndexOf(".bmp")) + 0 +".bmp";
			
		}catch (IOException e) {
			// e.printStackTrace();
		}
		return null;	
	}
	public static void convertFromBMP_to_GIF(String bmpPath, String gifPath){
		try{
			File input = new File(bmpPath);
			BufferedImage image = ImageIO.read(input);
			File output = new File(gifPath);
			ImageIO.write(image, "gif", output);
		}catch (IOException e) {
			// e.printStackTrace();
		}	
	}
	public static void main(String[] args) throws IOException, P2PDDSQLException {
		Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
//		// TODO Auto-generated method stub
//	  String bmpPath;// = convertFromGIF_to_BMP("c:/GIF/GifSample2.gif", "c:/GIF/ConvertedGifSample2.bmp");
//      bmpPath= "c:/GIF/ConvertedGifSample20.bmp";
//	  if(bmpPath!= null){
//	 // 	 insertData(bmpPath);
//	  	 convertFromBMP_to_GIF(bmpPath, "c:/GIF/GifSample2withData.gif" );
//	  	 
//	  String bmpPath = convertFromGIF_to_BMP("c:/GIF/GifSample2withData.gif", "c:/GIF/ConvertedGifLast.bmp");

//	 File s = new File( "c:/GIF/GifSample1.gif" );
//   	  File d = new File( "c:/GIF/GifSample1Copy.gif" );
//   	  copyImage(s,d,"gif");
//   	  if(compareImages(s,d)) System.out.println("s!=d");
      File s = new File( "c:/GIF/GifSample1.gif" );
   	  File d = new File( "c:/GIF/test.bmp" );
   	 // d.deleteOnExit();
   	 // copyImage(s,d,"bmp");
   	 // insert data in bmp image
   	//  insertData(d);
   	  s = d;
   	  d = new File( "c:/GIF/test.gif" );
   	  d.deleteOnExit();
   	  copyImage(s,d,"gif");
   	  
   	  s=d;
   	  d = new File( "c:/GIF/test2.bmp" );
   	  d.deleteOnExit();
   	  copyImage(s,d,"bmp");
   	  if(compareImages(s,d)) System.out.println("s!=d  s length= "+ s.length() +" d length="+ d.length() );
   	   else System.out.println("s=d   s length= "+ s.length() +" d length= "+ d.length()) ;
   	  // extract data 
	  	 	 
	}
	public static void insertData(File file){
		DD_Address myAddress;
		try {
			myAddress = HandlingMyself_Peer.getMyDDAddress();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		} 
		short content_type = myAddress.getSignShort();
//		if(DEBUG) System.out.println("Got to write: "+myAddress);
		BMP data=null;
		byte[] b=null; // old .bmp file 
		byte[] adr_bytes = myAddress.getBytes();
		
		FileInputStream fis;
		boolean fail= false;
		String explain="";
			try {
				fis = new FileInputStream(file);
				b = new byte[(int) file.length()];  
				fis.read(b);
				fis.close();
				data = new BMP(b, 0);
				if((data.compression!=BMP.BI_RGB)||(data.bpp<24)){
					explain = __("Not supported compression: "+data.compression+" "+data.bpp);
					fail = true;
				}
				if(data.width*data.height*3<(adr_bytes.length*8/EmbedInMedia.STEGO_BITS)+EmbedInMedia.STEGO_BYTE_HEADER){
					explain = __("File too short: "+data.width*data.height*3+" need: "+adr_bytes.length);
					fail = true;
				}
			
//					
//    		n = JOptionPane.showConfirmDialog(this, _("Embed address in: ")+file+"?",
//        			_("Overwrite prior details?"), JOptionPane.YES_NO_OPTION,
//        			JOptionPane.QUESTION_MESSAGE);
        	FileOutputStream fo=new FileOutputStream(file);
			int offset = data.startdata;
			int word_bytes=1;
			int bits = 4;
			////Util.copyBytes(b, BMP.CREATOR, adr_bytes.length);
			fo.write(EmbedInMedia.getSteganoBytes(adr_bytes, b, offset, word_bytes, bits, content_type));
			fo.close();
			} catch (FileNotFoundException e1) {
				fail = true;
			} catch (IOException e2) {
				fail = true;
			}
			if(fail)
				Application_GUI.warning(
					__("Cannot Embed address in: ")+file+" - "+explain,
					__("Inappropriate GIF File"));
	            			
	}
    public static void extractData(File file){
    	StegoStructure[]adr = DD.getAvailableStegoStructureInstances();
    	int[] selected = new int[1];
		String explain="";
		boolean fail= false;
		FileInputStream fis=null;
		byte[] b = null;
		BMP data = null;
		try{
			fis=new FileInputStream(file);
			b = new byte[(int) file.length()];
			fis.read(b);
			fis.close();	
		    data = new BMP(b, 0);
		}catch(Exception e){System.out.println(e);}
		//System.out.println("BMP Header: "+data);

		if((data.compression!=BMP.BI_RGB) || (data.bpp<24)){
			explain = " - "+__("Not supported compression: "+data.compression+" "+data.bpp );
			fail = true;
		}else{
			int offset = data.startdata;
			int word_bytes=1;
			int bits = 4;
			try {
				EmbedInMedia.setSteganoBytes(adr, selected, b, offset, word_bytes, bits);
			} catch (Exception e1) {
				explain = " - "+ __("No valid data in picture!");
				fail = true;
			}
		}
		if(fail){
//				JOptionPane.showMessageDialog(this,
//					_("Cannot Extract address in: ")+file+explain,
//					_("Inappropriate File"), JOptionPane.WARNING_MESSAGE);
				return;
		}
   }	
   public static boolean compareImages(File img1, File img2) throws IOException {
      BufferedImage bi1 = ImageIO.read (img1);
      BufferedImage bi2 = ImageIO.read (img2);
      boolean diff = false;
      for(int x=0; x< bi1.getWidth() ; x++)
      	 for(int y= 0; y< bi1.getHeight() ; y++)
      	 { int pixel1 = bi1.getRGB (x,y);  // get the value at col,row
      	   int pixel2 = bi2.getRGB (x,y);
      	   if (pixel1!=pixel2){
      	   	diff = true; // break;
      	    System.out.println("X: "+x+" Y: "+y+" pixel:"+pixel1+" pixel2:"+pixel2);
      	   }
      	  }     
      return diff;
   	  
  
   }
}

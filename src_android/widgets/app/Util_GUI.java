package widgets.app;

import static util.Util._;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import util.DDP2P_ServiceThread;
import util.EmbedInMedia;
import util.P2PDDSQLException;
import util.StegoStructure;
import util.Util;

import ASN1.ASN1DecoderFail;
import config.Application_GUI;
import config.DD;
public class Util_GUI {

	/*
	@Deprecated
	public static byte[] _setSteganoImage(BufferedImage bi) throws ASN1DecoderFail, P2PDDSQLException{
		return _setSteganoImage(bi,
				DD.getAvailableStegoStructureISignatures(),
				//new short[]{DD.STEGO_SIGN_PEER, DD.STEGO_SIGN_CONSTITUENT_VERIF_REQUEST},
				null);
	}
	*/
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
		byte[] sign= Util_GUI.getBytes(bi,EmbedInMedia.STEGO_SIGN_OFFSET,
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
		byte[] signature = EmbedInMedia.extractSteganoBytes(sign, 0, 1,
				EmbedInMedia.STEGO_BITS, 2);
		short signature_val = 0;
		signature_val=Util.extBytes(signature, 0, signature_val);
		if((content_type!=null)&&(content_type.length>0)) content_type[0] = signature_val;
		if(Util.contains(content_types, signature_val) == -1){
			Application_GUI.warning(
	    			_("When trying to use locally saved image got Wrong Signature: ")+signature_val+
	    			"\n"+_("The source of the drag might have changed the image content (like Safari / use Firefox!).") +
	    			"\n"+_("We will try other methods: test your Internet connection")+
	    			"\n"+_("You can also save the file and drag/load it as a file."),
	    			_("Wrong signature"));
			return null;
		}
		
		byte[] off= Util_GUI.getBytes(bi,EmbedInMedia.STEGO_OFF_OFFSET,
				Util.ceil(2*8/EmbedInMedia.STEGO_BITS));
		byte[] offset = EmbedInMedia.extractSteganoBytes(off, 0, 1,
				EmbedInMedia.STEGO_BITS, 2);
		short offset_val = 0;
		offset_val=Util.extBytes(offset, 0, offset_val);
		if(offset_val!=EmbedInMedia.STEGO_BYTE_HEADER){
			int n = Application_GUI.ask(_("Accept code: ")+offset_val+"!="+EmbedInMedia.STEGO_BYTE_HEADER, _("Accept old file?"), Application_GUI.YES_NO_OPTION);
			if (n != Application_GUI.YES_OPTION)
				return null;
		}
		
		byte[] len= Util_GUI.getBytes(bi,EmbedInMedia.STEGO_LEN_OFFSET,
				Util.ceil(4*8/EmbedInMedia.STEGO_BITS));
		byte[] length = EmbedInMedia.extractSteganoBytes(len, 0, 1,
				EmbedInMedia.STEGO_BITS, 4);
		int bytes_len = 0;
		bytes_len=Util.extBytes(length, 0, bytes_len);
		
		//System.err.println("Imglen:"+Util.byteToHex(len, " ")+" l="+bytes_len);
		int stegoBytes = Util.ceil((bytes_len*8)/EmbedInMedia.STEGO_BITS);
		byte[] useful= Util_GUI.getBytes(bi, offset_val, stegoBytes);
		if(EmbedInMedia.DEBUG)System.out.println("StegData:"+Util.byteToHex(useful, " "));
		byte datab[] = EmbedInMedia.extractSteganoBytes(useful, 0, 1, EmbedInMedia.STEGO_BITS, bytes_len);
		return datab;
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
		if(EmbedInMedia.DEBUG)System.out.println(data.toString());
		//data[k].save();
		new util.DDP2P_ServiceThread("Stego Saver BufImage", false, data[k]) {
			public void _run() {
				try {
					((StegoStructure)ctx).save();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}.start();
		if(EmbedInMedia.DEBUG) System.out.println("setSteganoImage: Done");
		return data[k];
	}
	/**
	 * Used to find the main window of a component, for showing alerts
	 * @param c
	 * @return
	 */
	public static Window findWindow(Component c) {
		    if (c == null) {
		        return JOptionPane.getRootFrame();
		    } else if (c instanceof Window) {
		        return (Window) c;
		    } else {
		        return findWindow(c.getParent());
		    }
	}
	public static byte[] getPixBytes(BufferedImage bi, int x, int y){
		//System.err.println("will get pix "+x);
		int pix=bi.getRGB(x,y);
		byte[]useful = new byte[4];
		/*
		System.err.println("will get pix "+1);
		ColorModel cm = bi.getColorModel();
		System.err.println("will get pix "+2);
		System.err.println("will get pix 3 "+pix+" cm="+cm);
		useful[0]=(byte)cm.getRed(pix);
		System.err.println("will get pix 3b ");
		useful[1]=(byte)cm.getGreen(pix);
		System.err.println("will get pix "+4);
		useful[2]=(byte)cm.getBlue(pix);
		System.err.println("will get pix "+5);
		useful[3]=(byte)cm.getAlpha(pix);
		System.err.println("will copy Bytes "+y);
		//Util.copyBytes(useful, 4, pix);
		 */
		Util.copyBytes(useful, 0, pix);
		//System.err.println("Extracted: "+pix+" ... "+Util.byteToHex(useful, " "));
		return useful;
	}
	public static byte[] getBytes(BufferedImage bi, int start, int length){
		byte[]useful = new byte[length];
		//ColorModel cm = bi.getColorModel();
		int height=bi.getHeight();
		int width=bi.getWidth();
		int stegoBytes = length;
		int lastbyte = start+length;
	    for(int d=0,k=start; k<lastbyte; ){
	    	//System.err.println(d+"/"+stegoBytes+" of max "+length);
	    	int p = k/3;
	    	int w = p % width; //FIREFOX
	    	//int w = width-1 - (p % width);
	    	int h = height - 1 - ((p-w) / height);// FIREFOX
	    	//int h = height-1 - ((p-(p%width)) / height);
	    	int pix=bi.getRGB(w,h);
	    	//System.err.println("getBytes: "+pix);
	    	if(k%3 == 0){
	    		useful[d++]= (byte)((pix>>16) & 0xff);k++; //(byte)cm.getBlue(pix);k++;
	    	}else if(k%3 == 1){
	    		useful[d++]= (byte)((pix>>8) & 0xff);k++; //(byte)cm.getGreen(pix);k++;
	    	}else if(k%3 == 2){
	    		useful[d++]= (byte)((pix) & 0xff);k++; //(byte)cm.getRed(pix);k++;
	    	}
	    	//System.err.println(d+"/"+stegoBytes);
	    	if(d>stegoBytes) break;
	    	/*
	        			useful[d++]=(byte)cm.getGreen(pix);k++;
	           			if(d>stegoBytes) break;
	        			useful[d++]=(byte)cm.getRed(pix);k++;
	           			if(d>stegoBytes) break;
	          */
	    }
		//System.err.println("Done getBytes");
		return useful;
	}
	/**
	     * This function is supposed to read an image from a filename pictureImage and convert it to a byte[]
	     * Unfortunately not portable ..., based on JPEGImageEncoder
	     * 
	     * see
	     * http://mindprod.com/jgloss/imageio.html#TOBYTES
	     * @param pictureImage
	     * @return
	     */
	    public static byte[] getImage(String pictureImage){
	    	
	    	byte[] byteArray=null;
	    	ImageIcon imageIcon;
	    	try{
	    		// iconData is the original array of bytes
	    		imageIcon = new ImageIcon(pictureImage);
	    		Image img = imageIcon.getImage();
	    		Image imageResize = img.getScaledInstance(100, 100, 0);
	    		ImageIcon imageIconResize = new ImageIcon (imageResize);
	    		int resizeWidth = imageIconResize.getIconWidth();
	    		int resizeHeight = imageIconResize.getIconHeight();
	    		Panel p = new Panel();
	    		BufferedImage bi = new BufferedImage(resizeWidth, resizeHeight,
	    				BufferedImage.TYPE_INT_RGB);
	//    		Graphics2D big = bi.createGraphics();
	//    		big.drawImage(imageResize, 0, 0, p);
	//    		ByteArrayOutputStream os = new ByteArrayOutputStream();
	//    		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
	//    		encoder.encode(bi);
	//    		byteArray = os.toByteArray();
	    		
	    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	     		boolean success = ImageIO.write(bi, DD.CONSTITUENT_PICTURE_FORMAT, baos);
	    		if(success) byteArray = baos.toByteArray();
	    		else System.err.println("ConstituentAction:getImage: appropriate picture writter missing");
	    		
	    	}catch(RuntimeException ev){ev.printStackTrace();}
	    	catch (Exception e) {e.printStackTrace();}
	    	return byteArray;
	    	//throw new RuntimeException("Not implemented JPEG!");
	    }
	static final JLabel resizer = new JLabel();
	public static java.awt.Dimension getPreferredSize(String html, boolean width, int prefSize) {
		resizer.setText(html);
		View view = (View) resizer.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
		view.setSize(width?prefSize:0, width?0:prefSize);
		float w = view.getPreferredSpan(View.X_AXIS);
		float h = view.getPreferredSpan(View.Y_AXIS);
		return new java.awt.Dimension((int) Math.ceil(w),(int) Math.ceil(h));
	}
	/** Returns an ImageIcon, or null if the path was invalid. */
	public static ImageIcon createImageIcon(String path,
				     String description) {
	return new ImageIcon(path, description);
	/*
	java.net.URL imgURL = getClass().getResource(path);
	if (imgURL != null) {
	    return new ImageIcon(imgURL, description);
	} else {
	    System.err.println("Couldn't find file: " + path);
	    return null;
	}
	*/
	}
	public static String getJFieldText(JComponent com) {
		if(Util.DEBUG)System.out.println("ConstituentActions:getText:"+com);
		String value;
		if(com instanceof JTextField){
			value = ((JTextField)com).getText();
	    	if(Util.DEBUG)System.out.println("ConstituentActions:getText: got textfield"+value);
		}else{
			value = ((JComboBox)com).getSelectedItem().toString();
	    	if(Util.DEBUG)System.out.println("ConstituentActions:getText: got "+value);
		}
		return value;
	}
	public static void cleanFileSelector(JFileChooser filterUpdates){
		File f = filterUpdates.getCurrentDirectory();
		filterUpdates.setSelectedFile(new File(" "));
		filterUpdates.setCurrentDirectory(f);
	}
	public static final Object crtProcessLabel = new JLabel("",SwingConstants.LEFT);
	
}
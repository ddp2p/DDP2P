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
 package net.ddp2p.widgets.app;
import static net.ddp2p.common.util.Util.__;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.event.InternalFrameAdapter;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.BMP;
import net.ddp2p.common.util.DD_IdentityVerification_Request;
import net.ddp2p.common.util.EmbedInMedia;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.StegoStructure;
public class JFrameDropCatch extends JFrame {
	protected static final boolean DEBUG = false;
	protected static final boolean _DEBUG = true;
	public static JFrame mframe;
    DataFlavor urlFlavor;
	public JFrameDropCatch(){
		mframe = this;
		this.setTransferHandler(handler);
		try {
			urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
		} catch (ClassNotFoundException e) {
		}
	}
	public JFrameDropCatch(String title) {
		super(title);
		mframe = this;
		this.setTransferHandler(handler);
	}
    private TransferHandler handler = new TransferHandler() {
        public boolean canImport(TransferHandler.TransferSupport support) {
           	DataFlavor[] df = null;
           	try {
           		df = support.getDataFlavors();
           	}catch (Exception e) {
           		e.printStackTrace();
           		return false;
           	}
           	if(DEBUG) for(int k =0; k<df.length; k++) System.err.println("df...: "+df[k]+" adica "+df[k].getMimeType());
           	DataFlavor urlFlavor=null, textURIList=null, mozFilePromise=null, mozFilePromiseURL=null,
           	imageBMP=null, chromeNamed=null, textHTML=null;
           	try {
           		textURIList = new DataFlavor("text/uri-list; class=java.lang.String");
             	urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
           		mozFilePromiseURL = new DataFlavor("application/x-moz-file-promise-url; class=java.net.URL");
           		mozFilePromise = new DataFlavor("application/x-moz-file-promise; class=java.io.InputStream");
           		imageBMP = new DataFlavor("image/bmp; class=java.io.InputStream");
           		chromeNamed = new DataFlavor("application/x-chrome-named-url; class=java.io.InputStream");
           		textHTML = new DataFlavor("text/html; class=java.lang.String");
           	} catch(Exception e){e.printStackTrace();}
            if ((!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))&&
            		(!support.isDataFlavorSupported(DataFlavor.imageFlavor))&&
            		(!support.isDataFlavorSupported(DataFlavor.stringFlavor))&&
            		((urlFlavor==null)||!support.isDataFlavorSupported(urlFlavor))&&
            		((textURIList==null)||!support.isDataFlavorSupported(textURIList))&&
            		((mozFilePromise==null)||!support.isDataFlavorSupported(mozFilePromise))&&
            		((mozFilePromiseURL==null)||!support.isDataFlavorSupported(mozFilePromiseURL))&&
            		((imageBMP==null)||!support.isDataFlavorSupported(imageBMP))&&
            		((chromeNamed==null)||!support.isDataFlavorSupported(chromeNamed))&&
            		((textHTML==null)||!support.isDataFlavorSupported(textHTML))
            ){
            	if(DEBUG)System.err.println("JFrameDropCatch: Unaccepted flavor: "+support);
            	Application_GUI.warning("Unaccepted flavors ["+df.length+"]: "+support+"\n You may drop to a file and try to drag from that file!", "DnD failure");
                return false;
            }
            if (true) {
                boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
                if (!copySupported) {
                	if(DEBUG)System.err.println("JFrameDropCatch: Unaccepted copy: "+support);
                    return false;
                }
                support.setDropAction(COPY);
            }
            if(DEBUG)System.out.println("JFrameDropCatch: CanDrop");
            return true;
        }
        boolean getFromInputStream(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromInputStream: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
               	if(DEBUG) System.out.println("getFromInputStream: not supported "+df);
               	return false;
        	}
        	InputStream in;
       		boolean result;
			try {
               	if(DEBUG) System.out.println("getFromInputStream: will get input");
               	Object o = t.getTransferData(df);
               	if((o instanceof String)||(! (o instanceof InputStream))){
               		if(DEBUG) System.out.println("getFromInputStream: STRING: "+o);
               		return false;
               	}
				in = (InputStream)o;
               	if(DEBUG) System.out.println("getFromInputStream: starting input");
				result = EmbedInMedia.fromBMPStreamSave(in, data, selected);
			} catch (UnsupportedFlavorException e) {
               	if(DEBUG) System.out.println("getFromInputStream: not supported flavor "+e);
				return false;
			} catch (ASN1DecoderFail e) {
               	if(DEBUG) System.out.println("getFromInputStream: error decoding "+e);
				return false;
			} catch (IOException e) {
               	if(DEBUG) System.out.println("getFromInputStream: IO error "+e);
				return false;
			} catch (Exception e) {
				if(DEBUG) System.out.println("getFromInputStream: error "+e);
				return false;
			}
           	if(DEBUG) System.out.println("getFromInputStream: result "+result);
       		return result;
        }
        boolean getFromBMPStream(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromBMPStream: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
              	if(DEBUG) System.out.println("getFromBMPStream: not supported "+df);
              	return false;
        	}
			java.net.URL url=null;
        	InputStream in;
        	boolean result;
			try {
				url = (java.net.URL) t.getTransferData (df);
				if(DEBUG) System.out.println("getFromBMPStream: URL= "+url);
				in = url.openStream();
				result = EmbedInMedia.fromBMPStreamSave(in, data, selected);
			} catch (UnsupportedFlavorException e) {
               	if(DEBUG) System.out.println("getFromBMPStream: not supported flavor "+e);
				return false;
			} catch (IOException e) {
              	if(DEBUG) System.out.println("getFromBMPStream: IO error "+e);
				return false;
			} catch (ASN1DecoderFail e) {
               	if(DEBUG) System.out.println("getFromBMPStream: error decoding "+e);
				return false;
			}
          	if(DEBUG) System.out.println("getFromBMPStream: result "+result);
          	return result;
        }
        boolean getFromURIString(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromURIString: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
              	if(DEBUG) System.out.println("getFromURIString: not supported "+df);
        		return false;
        	}
			java.net.URL url=null;
        	InputStream in;
        	boolean result;
        	String i;
			try {
				i = (String) t.getTransferData (df);
            	if(DEBUG) System.err.println("getFromURIString: Got string= "+i);
				url = new URL(i);
				in = url.openStream();
				result = EmbedInMedia.fromBMPStreamSave(in, data, selected);
			} catch (UnsupportedFlavorException e) {
               	if(DEBUG) System.out.println("getFromURIString: not supported flavor "+e);
				return false;
			} catch (IOException e) {
              	if(DEBUG) System.out.println("getFromURIString: IO error "+e);
				return false;
			} catch (ASN1DecoderFail e) {
               	if(DEBUG) System.out.println("getFromURIString: error decoding "+e);
				return false;
			}
          	if(DEBUG) System.out.println("getFromURIString: result "+result);
          	return result;
        }
        boolean getFromURIHTML(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromURIHTML: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
              	if(DEBUG) System.out.println("getFromURIHTML: not supported "+df);
        		return false;
        	}
			java.net.URL url=null;
        	InputStream in;
        	boolean result;
        	String i;
			try {
				i = (String) t.getTransferData (df);
            	if(DEBUG) System.err.println("getFromURIHTML: Got URL string= "+i);
       			String url1[]=i.split(Pattern.quote("img src="));
       			if((url1==null) || (url1.length<2)) url1 = i.split(Pattern.quote("src="));
       			if((url1==null) || (url1.length<2)) return false;
       			String url2[]=url1[1].split(Pattern.quote("\""));
       			i=url2[1];
            	if(DEBUG) System.err.println("getFromURIHTML: Got string= "+i);
				url = new URL(i);
				in = url.openStream();
				result = EmbedInMedia.fromBMPStreamSave(in, data, selected);
			} catch (UnsupportedFlavorException e) {
               	if(_DEBUG) System.out.println("getFromURIHTML: not supported flavor "+e);
				return false;
			} catch (IOException e) {
              	if(_DEBUG) System.out.println("getFromURIHTML: IO error "+e);
              	e.printStackTrace();
				return false;
			} catch (ASN1DecoderFail e) {
              	if(DEBUG) System.out.println("getFromURIHTML: error decoding "+e);
				return false;
			}
          	if(DEBUG) System.out.println("getFromURIHTML: result "+result);
       		return result;
        }
        boolean getFromImage(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromImage: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
              	if(DEBUG) System.out.println("getFromImage: not supported "+df);
        		return false;
        	}
            try {
            	java.awt.Image i = null;
            	i = (java.awt.Image)t.getTransferData(df);
            	if(DEBUG) System.err.println("importData: Got awt image: "+i);
            	if((i!=null) && (i instanceof BufferedImage)) {
            		if(DEBUG) System.err.println("importData: Got BI image: "+i);
                	BufferedImage bi = (BufferedImage)i;
                	if(DEBUG) System.err.println("importData: will get pixels image: ");
              		Util_GUI.getPixBytes(bi, 0, 0);
              		Util_GUI.getPixBytes(bi, bi.getWidth()-1, 0);
              		Util_GUI.getPixBytes(bi, 0, bi.getHeight()-1);
              		Util_GUI.getPixBytes(bi, bi.getWidth()-1, bi.getHeight()-1);
              		if(DEBUG) System.err.println("importData: will Stegano image: "+i);
              		StegoStructure d= Util_GUI.setSteganoImage(bi, data, selected);
                    if(DEBUG) System.err.println("importData: did Stegano image: "+data);
            		if(d!=null){
            			if(DEBUG) System.err.println("importData: Got image successfully");
                        return true;
            		}else{
                      	if(DEBUG) System.err.println("importData: Got image insuccessfully");
            			return false;
            		}
            	}else{
                  	if(DEBUG) System.err.println("importData: Got no image successfully");
            		return false;
            	}
            } catch (UnsupportedFlavorException e) {
               	if(DEBUG) System.out.println("getFromImage: not supported flavor "+e);
            	return false; 
            } catch (IOException e) {
              	if(DEBUG) System.out.println("getFromImage: IO error "+e);
            	return false; 
            } catch (ASN1DecoderFail e) {
              	if(DEBUG) System.out.println("getFromImage: error decoding "+e);
				return false; 
			} catch(Exception e){
        		if(DEBUG) e.printStackTrace();
        		return false;
        	}
        }
        boolean getFromFileList(Transferable t, DataFlavor df, StegoStructure[] data, int[] selected) throws P2PDDSQLException{
        	if(DEBUG) System.out.println("getFromFileList: "+df);
        	if(df==null) return false;
        	if(!t.isDataFlavorSupported (df)){
              	if(DEBUG) System.out.println("getFromFileList: not supported "+df);
        		return false;
        	}
        	boolean result=false;
            try {
            	@SuppressWarnings( "unchecked" )
                java.util.List<File> l =
                    (java.util.List<File>)t.getTransferData(df);
                if(DEBUG) System.err.println("getFromFileList: Got files: "+l);
                for (File f : l) {
                	if(DEBUG) System.err.println("getFromFileList: Got file: "+f);
                	result |= EmbedInMedia.fromBMPFileSave(f, data, selected);
                }
            } catch (UnsupportedFlavorException e) {
            	if(DEBUG) System.err.println("getFromFileList: Not supported: File. "+e);
            } catch (IOException e) {
              	if(DEBUG) System.out.println("getFromFileList: IO error "+e);
            }
          	if(DEBUG) System.out.println("getFromFileList: result "+result);
            return result;
        }
        public boolean importData(TransferHandler.TransferSupport support) {
		        	try {
		        		return 
		        				_importData(support);
		        	} catch(Exception e) {
		        		e.printStackTrace();
		        		Application_GUI.warning(__("Error saving data:")+e.getLocalizedMessage(), __("Error saving data"));
		        		return false;
		        	}
        }
        public boolean _importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
            	if(DEBUG)System.err.println("Cannot Import!");
            	Application_GUI.warning("Cannot import from: ["+support.getDataFlavors().length+"]: "+support+"\tTry saving to a file", "Import Fail!");
                return false;
            }
            Transferable t = support.getTransferable();
            DataFlavor[] df = t.getTransferDataFlavors();
            if(DEBUG) {
            	for(int k =0; k<df.length; k++){
            		System.err.println("DF...: "+k+"/"+df.length+"="+df[k]+" adica "+df[k].getMimeType());
            	}
            }
           	DataFlavor urlFlavor=null, textURIList=null, mozFilePromise=null, mozFilePromiseURL=null,
           	imageBMP=null, chromeNamed=null, textHTML=null;
           	try{
           		textURIList = new DataFlavor("text/uri-list; class=java.lang.String");
             	urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
           		mozFilePromiseURL = new DataFlavor("application/x-moz-file-promise-url; class=java.net.URL");
           		mozFilePromise = new DataFlavor("application/x-moz-file-promise; class=java.io.InputStream");
           		imageBMP = new DataFlavor("image/bmp; class=java.io.InputStream");
           		chromeNamed = new DataFlavor("application/x-chrome-named-url; class=java.io.InputStream");
           		textHTML = new DataFlavor("text/html; class=java.lang.String; charset=Unicode");
           	}catch(Exception e){e.printStackTrace();}
           	boolean result = false;
           	int selected[] = new int[1];
           	StegoStructure[] data = DD.getAvailableStegoStructureInstances();
           	try {
            	if(DEBUG)System.err.println("JFrameDropCatch: try mozFilePromise! ");
				result=getFromInputStream(t, mozFilePromise, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try inputStream!");
				if(!result)result=getFromInputStream(t, chromeNamed, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try urlFlavor!");
				if(!result)result=getFromBMPStream(t, urlFlavor, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try mozFilePromise!");
				if(!result)result=getFromBMPStream(t, mozFilePromiseURL, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try imageFlavor!");
				if(!result)result=getFromImage(t, DataFlavor.imageFlavor, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try javaFileListFlavor!");
				if(!result)result=getFromFileList(t, DataFlavor.javaFileListFlavor, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try stringFlavor!");
				if(!result)result=getFromURIString(t, DataFlavor.stringFlavor, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try textURIList!");
				if(!result)result=getFromURIString(t, textURIList, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: try textHTML!");
				if(!result)result=getFromURIHTML(t, textHTML, data, selected);
				if(!result)if(DEBUG)System.err.println("JFrameDropCatch: no try worked!");
			} catch (P2PDDSQLException e1) {
				Application_GUI.warning(__("Database Error: ")+e1, __("Importing Address From Image"));
			}
			if(DEBUG)System.err.println("getDrop: pre result: "+result);
			if(!result)
				Application_GUI.warning(__("Abandoning drag and drop import attempt. Try to save file and load directly."), __("Importing address"));
			if(DEBUG)System.err.println("getDrop: result: "+result);
			return result;
        }
    };
}

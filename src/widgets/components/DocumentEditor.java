/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
package widgets.components;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.views.DocumentViewController;

import com.metaphaseeditor.MetaphaseEditor;

import data.D_Document;

import util.Util;

public class DocumentEditor implements DocumentListener{
	public static final int TEXTAREA = 0;
	public static final String TXT_BODY_FORMAT = "TXT";
	
	static final int DJNative = 1;
	public static final String PDF_BODY_FORMAT = "PDF";
	
	
	public static final int RTEDIT = 2;
	public static final String HTM_BODY_FORMAT = "HTM";

	public static final int PDFVIEW = 3;

	public static final String DEFAULT_FORMAT = HTM_BODY_FORMAT;
	public static final int DEFAULT_EDITOR = RTEDIT;
	
	
	public static final int  MAX_PDF = 1000000;
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	
	int type_editor = TEXTAREA;
	String type_document = TXT_BODY_FORMAT;
	JTextArea textArea = new JTextArea();
	//DJNativeBrowser djNB;
	SwingController controller = new SwingController();
	DocumentViewController viewController;
	// HTML editor, RichTextEditor
	MetaphaseEditor rtEditor;
	public String name;
	
	public DocumentEditor(){
		controller.setIsEmbeddedComponent(true);
		viewController = controller.getDocumentViewController();
		rtEditor = new MetaphaseEditor();
		addListener(this);
	}

	public Component getComponent(){
		switch(type_editor){
		case TEXTAREA:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: TXT");
			return textArea;
		case PDFVIEW:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: PDF");
			return viewController.getViewContainer();
		case RTEDIT:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: RTE");
			return rtEditor;
		}
		if(DEBUG) System.out.println("DocumentEditor: getComponent:: No component of type: "+type_editor);
		return null;
	}
	public Component getComponent(int type_editor){
		switch(type_editor){
		case TEXTAREA: return textArea;
		case PDFVIEW: return viewController.getViewContainer();
		case RTEDIT: return rtEditor;
		}
		return null;
	}
	
	public void setEnabled(boolean b) {
		//System.out.println("DocumentEditor: setEnabled: bool="+b);
		switch(type_editor){
		case TEXTAREA:
    		if(DEBUG) System.out.println("DocumentEditor: setEnabled: TXT: "+b);
			textArea.setEnabled(b); break;
		case RTEDIT:
			if(DEBUG) System.out.println("DocumentEditor: setEnabled: RTE: "+b);
			//rtEditor.setToolbarComponentEnable(b);
			rtEditor.setToolbarComponentVisibleAll(b);
            rtEditor.getHtmlTextArea().setEditable(b); //.setEnabled(b);
            rtEditor.getHtmlTextAreaPane().setEditable(b); //.setEnabled(b);
            break;
            default:
            	if(_DEBUG) System.out.println("DocumentEditor: setEnabled: unknown type: "+type_editor+" :"+b);
		}
	}
	public void removeListener(DocumentListener obj) {
		if(DEBUG) System.out.println("DocumentEditor: removeListener: "+name);		
		textArea.getDocument().removeDocumentListener(obj);
		rtEditor.getHtmlTextAreaPane().getDocument().removeDocumentListener(obj);
		rtEditor.getHtmlTextArea().getDocument().removeDocumentListener(obj);
		/*
		switch(type_editor){
		case TEXTAREA: textArea.getDocument().removeDocumentListener(obj); break;
//		case RTEDIT:   rtEditor.getHtmlTextArea().getDocument().removeDocumentListener(obj); break;
		case RTEDIT:   rtEditor.getHtmlTextAreaPane().getDocument().removeDocumentListener(obj); break;
		}
		*/
	}
	public void addListener(DocumentListener obj){
		if(DEBUG) System.out.println("DocumentEditor: addListener: name="+name+" : o="+obj);
		textArea.getDocument().addDocumentListener(obj);
		rtEditor.getHtmlTextArea().getDocument().addDocumentListener(obj);
		rtEditor.getHtmlTextAreaPane().getDocument().addDocumentListener(obj);
		/*
		switch(type_editor){
		case TEXTAREA: textArea.getDocument().addDocumentListener(obj); break;
//		case RTEDIT:   rtEditor.getHtmlTextArea().getDocument().addDocumentListener(obj); break;
		case RTEDIT:   rtEditor.getHtmlTextAreaPane().getDocument().addDocumentListener(obj); break;
		}
		*/
	}
	public void setText(String documentString) {
		switch(type_editor){
		case TEXTAREA:
			if(documentString==null){
				textArea.setText(""); //ADDED
				if(DEBUG) System.out.println("DocumentEditor: setText: txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: txt len="+documentString.length());
			textArea.setText(documentString); break;
		case RTEDIT:
			if(documentString==null){
				rtEditor.setDocument("") ; //ADDED
				if(DEBUG) System.out.println("DocumentEditor: setText: txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: txt len="+documentString.length());
			rtEditor.setDocument(documentString); break;
		case PDFVIEW:
			if(documentString==null){
				if(DEBUG) System.out.println("DocumentEditor: setText: pdf txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: pdf txt len="+documentString.length());
			byte[] pdf = Util.byteSignatureFromString(documentString);
			if(DEBUG) System.out.println("DocumentEditor: setText: pdf bin len="+pdf.length);
			if(pdf!=null)
				try{
					controller.openDocument(pdf, 0, pdf.length, null, null);
				}catch(Exception e){
					e.printStackTrace();
				}
		}
	}
	public void init(int TEXT_LEN_ROWS){
		switch(type_editor){
		case TEXTAREA:
			textArea.setEditable(true);
			textArea.setSize(500, 200);
			//.setEditorKit(RTFEditorKit);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setRows(TEXT_LEN_ROWS);
			textArea.setAutoscrolls(true);
			//result.setMaximumSize(new java.awt.Dimension(300,100));
			break;
		case PDFVIEW:
			
		}
	}
	public void init(int TEXT_LEN_ROWS, int TEXT_LEN_COLS){
		switch(type_editor){
		case TEXTAREA:
			textArea.setEditable(true);
			textArea.setSize(500, 200);
			//.setEditorKit(RTFEditorKit);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setColumns(TEXT_LEN_COLS);
			textArea.setRows(TEXT_LEN_ROWS);
			textArea.setAutoscrolls(true);
			//result.setMaximumSize(new java.awt.Dimension(300,100));
			break;
		case PDFVIEW:
			
		}
	}
	public String getText() {
		/*
		String data = null;
		try {
			File f = new File("/home/msilaghi/CS_seminar_flyer.pdf");
			InputStream in = new FileInputStream(f); // "/home/msilaghi/CS_seminar_flyer.pdf");
			if(f.length() > MAX_PDF) {
				if(DEBUG) System.out.println("DocumentEditor: getText: bin size="+f.length()+" vs "+MAX_PDF);
				return null;
			}
			byte bin[] = new byte[(int)f.length()];
			int off = 0;
			do{
				int cnt = in.read(bin, off, bin.length-off);
				if(cnt == -1) {
					if(DEBUG) System.out.println("DocumentEditor: getText: crt="+cnt+" off="+off);
					break;
				}
			}while(off < bin.length);
			if(DEBUG) System.out.println("DocumentEditor: getText: bin size="+bin.length);
			data = Util.stringSignatureFromByte(bin);
			if(DEBUG) System.out.println("DocumentEditor: getText: txt size="+data.length());
			return data;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		switch(type_editor){
		case TEXTAREA: return textArea.getText();// break;
		case RTEDIT: return rtEditor.getDocument();// break;
		case PDFVIEW: break; // return controller.getDocument();
		}
		return null;
	}
	// Here I need help
	public Object getDocumentSource() {
		switch(type_editor){
		case TEXTAREA: return textArea.getDocument(); //break;
//		case RTEDIT: return rtEditor.getHtmlTextArea().getDocument(); //break;
		case RTEDIT: return rtEditor.getHtmlTextAreaPane().getDocument(); //break;
		case PDFVIEW: break;
		}
		return null;
	}

	public String getFormatString() {
		//if(true)return PDF_BODY_FORMAT; //break;
		
		switch(type_editor){
		case TEXTAREA: return TXT_BODY_FORMAT; //break;
		case RTEDIT: return HTM_BODY_FORMAT; //break;
		case DJNative: return PDF_BODY_FORMAT; //break;
		case PDFVIEW: return PDF_BODY_FORMAT; //break;
		}
		return null;
	}
	public void updateDoc(D_Document data) {
		data.setFormatString(this.getFormatString());
		data.setDocumentString(this.getText());
	}

	public void setType(String formatString) {
		if(DEBUG) System.out.println("DocumentEditor: setType: type="+formatString);
		if(formatString==null) type_editor = RTEDIT;//TEXTAREA; // default set to RTEDIT
		if(PDF_BODY_FORMAT.equals(formatString)){
			type_editor = PDFVIEW;
			if(DEBUG) System.out.println("DocumentEditor: setType: type=PDF!");
			return;
		}
		if(TXT_BODY_FORMAT.equals(formatString)){
			type_editor = TEXTAREA;
			if(DEBUG) System.out.println("DocumentEditor: setType: type=txt!");
			return;
		}
		type_editor = RTEDIT; // default set to RTEDIT
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		if(DEBUG) System.out.println("DocumentEditor: changeUpdate: "+name);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		if(DEBUG) System.out.println("DocumentEditor: insertUpdate: "+name);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		if(DEBUG) System.out.println("DocumentEditor: removeUpdate: "+name);
	}

	
}
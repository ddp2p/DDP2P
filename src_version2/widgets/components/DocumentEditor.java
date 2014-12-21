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
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//
//import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.views.DocumentViewController;

import com.metaphaseeditor.MetaphaseEditor;

import data.D_Document;

import util.Util;

public class DocumentEditor implements DocumentListener{
	public static final int  MAX_PDF = 1000000;
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int TEXTAREA_WIDTH = 1000;
	
	int type_editor = D_Document.TEXTAREA;
	String type_document = D_Document.TXT_BODY_FORMAT;
	JTextArea textArea = new JTextArea();
	//DJNativeBrowser djNB;
	SwingController controller = new SwingController();
	DocumentViewController viewController;
	// HTML editor, RichTextEditor
	MetaphaseEditor rtEditor;
	public String name;
	
	public DocumentEditor(){
		//System.out.println("**************************");
		controller.setIsEmbeddedComponent(true);
		viewController = controller.getDocumentViewController();
		rtEditor = new MetaphaseEditor();
		addListener(this);
	}

	public Component getComponent(){
		//boolean DEBUG=true;
		switch(type_editor){
		case D_Document.TEXTAREA:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: TXT");
			return textArea;
		case D_Document.PDFVIEW:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: PDF");
			return viewController.getViewContainer();
		case D_Document.RTEDIT:
			if(DEBUG) System.out.println("DocumentEditor: getComponent:: RTE");
			return rtEditor;
		}
		if(DEBUG) System.out.println("DocumentEditor: getComponent:: No component of type: "+type_editor);
		return null;
	}
	public Component getComponent(int type_editor){
		switch(type_editor){
		case D_Document.TEXTAREA: return textArea;
		case D_Document.PDFVIEW:{
			if(DEBUG) System.out.println("DocumentEditor: getComponent():: PDF");
			return viewController.getViewContainer();
		}
		case D_Document.RTEDIT: return rtEditor;
		}
		return null;
	}
	/**
	 * Makes editor (not) editable.
	 * Hides/Shoes toolbars for RTEditor.
	 * 
	 * Applies only to the currently set type of editor format
	 * @param b
	 */
	public void setEnabled(boolean b) {
		//boolean DEBUG = true;
		//System.out.println("DocumentEditor: setEnabled: bool="+b);
		switch(type_editor){
		case D_Document.TEXTAREA:
    		if(DEBUG) System.out.println("DocumentEditor: setEnabled: TXT: "+b);
			//textArea.setEnabled(b); break;
			textArea.setEditable(b); break;
		case D_Document.RTEDIT:
			if(DEBUG) System.out.println("DocumentEditor: setEnabled: RTE: "+b);
			//rtEditor.setToolbarComponentEnable(b);
			rtEditor.setToolbarComponentVisibleAll(b);
            rtEditor.getHtmlTextArea().setEditable(b); //.setEnabled(b);
            rtEditor.getHtmlTextAreaPane().setEditable(b); //.setEnabled(b);
            break;
		case D_Document.PDFVIEW:
			if(DEBUG) System.out.println("DocumentEditor: setEnabled: PDF type ("+type_editor+") :"+b);
			//this.controller;
			break;
		default:
			if(_DEBUG) System.out.println("DocumentEditor: setEnabled: unknown type: "+type_editor+" :"+b);
		}
		//Util.printCallPath("");
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
		case D_Document.TEXTAREA:
			if(documentString==null){
				textArea.setText(""); //ADDED
				if(DEBUG) System.out.println("DocumentEditor: setText: txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: txt len="+documentString.length());
			textArea.setText(documentString); break;
		case D_Document.RTEDIT:
			if(documentString==null){
				rtEditor.setDocument("") ; //ADDED
				if(DEBUG) System.out.println("DocumentEditor: setText: txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: txt len="+documentString.length());
			rtEditor.setDocument(documentString); break;
		case D_Document.PDFVIEW:
			if(documentString==null){
				if(DEBUG) System.out.println("DocumentEditor: setText: pdf txt=NULL");
				return;
			}
			if(DEBUG) System.out.println("DocumentEditor: setText: pdf txt len="+documentString.length());
			byte[] pdf = Util.byteSignatureFromString(documentString);
			if(pdf!=null){
				if(DEBUG) System.out.println("DocumentEditor: setText: pdf bin len="+pdf.length);
				try{
					controller.openDocument(pdf, 0, pdf.length, null, null);
				}catch(Exception e){
					e.printStackTrace();
				}
			}else{
				if(DEBUG) System.out.println("DocumentEditor: setText: pdf bin null");
			}
		}
	}
	public void init(int TEXT_LEN_ROWS){
		switch(type_editor){
		case D_Document.TEXTAREA:
			textArea.setEditable(true);
			textArea.setSize(TEXTAREA_WIDTH, 200);
			//.setEditorKit(RTFEditorKit);
			//textArea.setLineWrap(true);
			//textArea.setWrapStyleWord(true);
			textArea.setRows(TEXT_LEN_ROWS);
			textArea.setAutoscrolls(true);
			//result.setMaximumSize(new java.awt.Dimension(300,100));
			break;
		case D_Document.PDFVIEW:
			
		}
	}
	public void init(int TEXT_LEN_ROWS, int TEXT_LEN_COLS){
		switch(type_editor){
		case D_Document.TEXTAREA:
			textArea.setEditable(true);
			textArea.setSize(TEXTAREA_WIDTH, 200);
			//.setEditorKit(RTFEditorKit);
			//textArea.setLineWrap(true);
			//textArea.setWrapStyleWord(true);
			//textArea.setColumns(TEXT_LEN_COLS);
			textArea.setRows(TEXT_LEN_ROWS);
			textArea.setAutoscrolls(true);
			//result.setMaximumSize(new java.awt.Dimension(300,100));
			break;
		case D_Document.PDFVIEW:
			
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
		case D_Document.TEXTAREA: return textArea.getText();// break;
		case D_Document.RTEDIT: return rtEditor.getDocument();// break;
		case D_Document.PDFVIEW: break; // return controller.getDocument();
		}
		return null;
	}
	// Here I need help
	public Object getDocumentSource() {
		switch(type_editor){
		case D_Document.TEXTAREA: return textArea.getDocument(); //break;
//		case RTEDIT: return rtEditor.getHtmlTextArea().getDocument(); //break;
		case D_Document.RTEDIT: return rtEditor.getHtmlTextAreaPane().getDocument(); //break;
		case D_Document.PDFVIEW: break;
		}
		return null;
	}

	public String getFormatString() {
		//if(true)return PDF_BODY_FORMAT; //break;
		
		switch(type_editor) {
		case D_Document.TEXTAREA: return D_Document.TXT_BODY_FORMAT; //break;
		case D_Document.RTEDIT: return D_Document.HTM_BODY_FORMAT; //break;
		case D_Document.DJNative: return D_Document.PDF_BODY_FORMAT; //break;
		case D_Document.PDFVIEW: return D_Document.PDF_BODY_FORMAT; //break;
		}
		return null;
	}
	public void updateDoc(D_Document data) {
		data.setFormatString(this.getFormatString());
		data.setDocumentString(this.getText());
	}
	/**
	 * Set the editor type
	 * @param formatStrings
	 */
	public void setType(String formatString) {
		//boolean DEBUG=true;
		if(DEBUG) System.out.println("DocumentEditor: setType: type="+formatString);
		if(formatString==null) type_editor = D_Document.RTEDIT;//TEXTAREA; // default set to RTEDIT
		if(D_Document.PDF_BODY_FORMAT.equals(formatString)){
			type_editor = D_Document.PDFVIEW;
			if(DEBUG) System.out.println("DocumentEditor: setType: type=PDF!");
			return;
		}
		if(DEBUG) System.out.println("DocumentEditor: setType: type=!PDF!");
		if(D_Document.TXT_BODY_FORMAT.equals(formatString)){
			type_editor = D_Document.TEXTAREA;
			if(DEBUG) System.out.println("DocumentEditor: setType: type=txt!");
			return;
		}
		type_editor = D_Document.RTEDIT; // default set to RTEDIT
		if(DEBUG) System.out.println("DocumentEditor: setType: type=RTE!");
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
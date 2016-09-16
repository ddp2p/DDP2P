package net.ddp2p.widgets.components;

import static net.ddp2p.common.util.Util.__;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.Document;
import javax.swing.text.Element;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.motions.MotionEditor;

@SuppressWarnings("serial")
public class MultiformatDocumentEditor extends JSplitPane implements ActionListener, DocumentListener{
	private static final boolean DEBUG = false; //false;
	D_Document data = new D_Document();
	JPanel p1,p2,p;
	private JButton setTxtMode;
	private JButton setHTMMode;
	private JButton load_field;
	private JButton sync_changes;
	public DocumentEditor editor;
	private boolean enabled;
	static int cnt = 0;
	public MultiformatDocumentEditor(int TEXT_LEN_ROWS, int TEXT_LEN_COLS){
		super(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), new JPanel());
		init(TEXT_LEN_ROWS, TEXT_LEN_COLS);
	}
	void init(int TEXT_LEN_ROWS, int TEXT_LEN_COLS) {
		try{
			p = (JPanel) this.getLeftComponent();
			p2 = (JPanel) this.getRightComponent();
		}catch(Exception e){
			return;
		}
		p1 = new JPanel();
    	p.setLayout(new BorderLayout());
    	p.add(p1,BorderLayout.NORTH);
		
    	editor = new DocumentEditor();
    	cnt++;
    	editor.name = "MF"+cnt;
    	editor.init(TEXT_LEN_ROWS, TEXT_LEN_COLS);
    	editor.addListener(this);
		
		editor.getComponent(D_Document.RTEDIT).setVisible(false);
		editor.getComponent(D_Document.TEXTAREA).setVisible(false);
		editor.getComponent(D_Document.PDFVIEW).setVisible(false);
		
		editor.setType(D_Document.DEFAULT_FORMAT);
		editor.getComponent(D_Document.DEFAULT_EDITOR).setVisible(true);
		
		p2.add(editor.getComponent(D_Document.TEXTAREA));
		p2.add(editor.getComponent(D_Document.RTEDIT));
		p2.add(editor.getComponent(D_Document.PDFVIEW));
		
    	GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0; c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		p1.setLayout(new GridBagLayout());
		
		c.gridx = 0; c.gridy++;		
		c.anchor = GridBagConstraints.EAST;
		p1.add(load_field = new JButton(__("Load PDF/HTM/TXT")),c);
		load_field.addActionListener(this);
		
		c.gridx = 0; c.gridy ++;		
		c.anchor = GridBagConstraints.EAST;
		p1.add(this.setTxtMode = new JButton(__("Set TXT Mode")),c);
		setTxtMode.addActionListener(this);
		
		c.gridx = 0; c.gridy ++;		
		c.anchor = GridBagConstraints.EAST;
		p1.add(this.setHTMMode = new JButton(__("Set HTML Mode")),c);
		setHTMMode.addActionListener(this);
		
		c.gridx = 0; c.gridy ++;		
		c.anchor = GridBagConstraints.EAST;
		p1.add(this.sync_changes = new JButton(__("Sync Changes")),c);
		this.sync_changes.addActionListener(this);
		
	}
	public void setEnabled(boolean enable){
		super.setEnabled(enable);
		this.enabled = enable;
		if(enable) enable_it(); else disable_it();
	}
	private void disable_it() {
		boolean enable = false;
		setTxtMode.setEnabled(enable);
		setHTMMode.setEnabled(enable);
		load_field.setEnabled(enable);
		this.sync_changes.setEnabled(enable);
		editor.setEnabled(enable);
	}
	private void enable_it() {
		boolean enable = true;
		setTxtMode.setEnabled(enable);
		setHTMMode.setEnabled(enable);
		load_field.setEnabled(enable);
		this.sync_changes.setEnabled(enable);
		editor.setEnabled(enable);
	}
	ArrayList<MultiformatDocumentListener> listeners = new ArrayList<MultiformatDocumentListener>();
	public void addListener(MultiformatDocumentListener l){
		if(!listeners.contains(l)){
			listeners.add(l);
			if(DEBUG) System.out.println("MFEditor:addListener: from="+editor.name+" : add listener="+l);
		}else{
			if(DEBUG) System.out.println("MFEditor:addListener: from="+editor.name+" : already listener="+l);
		}
	}
	public void addDocumentListener(MultiformatDocumentListener l){
		addListener(l);
	}
	public void removeListener(MultiformatDocumentListener l){
		if(! listeners.contains(l)) {
			if(DEBUG) System.out.println("MFEditor:removeListener: from="+editor.name+" : no listener="+l);
			return;
		}
		if(DEBUG) System.out.println("MFEditor:removeListener: from="+editor.name+" : listener="+l);
		listeners.remove(l);
	}
	public void removeDocumentListener(MultiformatDocumentListener l){
		removeListener(l);
	}
	void notifyChange(DocumentEvent e){
		System.out.println("MFEditor:notifyChange: editors="+listeners.size());
		for (MultiformatDocumentListener l : listeners) {
			l.changeUpdate(this);
		}
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and load it with "data"
	 * @param _NEW_FORMAT
	 * @param data
	 */
	public void setMode(String _NEW_FORMAT, String data){
		
		this.data.setDocumentString(data);
		this.data.setFormatString(_NEW_FORMAT);
		
		this.editor.removeListener(this);
		editor.getComponent().setVisible(false);
		System.out.println("MFEditor: setMode: setVisCrtTo false");
		this.editor.setType(this.data.getFormatString());
		this.editor.setText(this.data.getDocumentString());
		
		editor.getComponent().setVisible(true);
		System.out.println("MFEditor: setMode: setCrtVis=true, enabled="+enabled+" data= "+data);
	    //this.panel_body.removeAll();
	    //this.panel_body.add(motion_body_field.getComponent());
		this.editor.setEnabled(enabled);
		this.editor.addListener(this);
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and convert current data to it
	 * @param _NEW_FORMAT
	 */
	public void switchMode(String _NEW_FORMAT){
		String data;
		editor.updateDoc(this.data);
		data = this.data.convertTo(_NEW_FORMAT);
		if(data==null){
			//if(DEBUG)System.out.println("MFEditor:switchMode: Nothing in doc");
			//if(_NEW_FORMAT.equals(DocumentEditor.PDF_BODY_FORMAT)) return;
			//data = "null fost";
			return;
		}
		setMode(_NEW_FORMAT, data);
	}
	
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:changedUpdate:Action: "+evt);
		editor.updateDoc(this.data);
		this.handleFieldEvent(evt.getDocument(), evt);
	}
	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:insertUpdate:Action: "+evt);
		editor.updateDoc(this.data);
		this.handleFieldEvent(evt.getDocument(), evt);
	}
	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:removeUpdate:Action "+evt);
		editor.updateDoc(this.data);
		this.handleFieldEvent(evt.getDocument(), evt);
	}
	@Override
	public void actionPerformed(ActionEvent act) {
		if(DEBUG)System.out.println("JustEditor:actionPerformed:Action: "+act);
		this.handleFieldEvent(act.getSource(), null);
	}
	@SuppressWarnings("unchecked")
	public void handleFieldEvent(Object source, DocumentEvent evt) {
		
		if(evt!=null){
			this.notifyChange(evt);
			return;
		}else{
			/*
			evt = new DocumentEvent(){ //placeholder for null
				public Document mySource = null;
				@Override
				public ElementChange getChange(Element arg0) {
					return null;
				}
				@Override
				public Document getDocument() {
					return mySource;
				}
				@Override
				public int getLength() {
					return 0;
				}
				@Override
				public int getOffset() {
					return 0;
				}
				@Override
				public EventType getType() {
					return null;
				}};
				*/
		}
		
		//boolean DEBUG = true;
		if(this.setTxtMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setText");
			switchMode(D_Document.TXT_BODY_FORMAT);
			this.notifyChange(evt);
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");
		}
		if(this.setHTMMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setHTM");
			switchMode(D_Document.HTM_BODY_FORMAT);
			this.notifyChange(evt);
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");
		}
		if(this.sync_changes==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: sync");
			editor.updateDoc(data);
			this.notifyChange(evt);
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");
		}
		if(this.load_field==source) {
			if(DEBUG)System.err.println("ControlPane:actionImport: import file");
			int returnVal = MotionEditor.fd.showOpenDialog(this);
			if(DEBUG)System.err.println("ControlPane:actionImport: Got: selected");
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	        	if(DEBUG)System.err.println("ControlPane:actionImport: Got: ok");
	            File file = MotionEditor.fd.getSelectedFile();
	            if(!file.exists()){
	            	Application_GUI.warning(__("The file does not exists: "+file),__("Importing Address")); return;
	            }
	            String ext = Util.getExtension(file);
	            if(ext!=null) ext = ext.toLowerCase();
	            try {
	            	boolean _DEBUG = true;
					if("pdf".equals(ext)) {
	            		if(DEBUG)System.err.println("ControlPane:actionImport: Got: pdf");
	            		try {
	            			// File f = new File("/home/msilaghi/CS_seminar_flyer.pdf");
	            			InputStream in = new FileInputStream(file); // "/home/msilaghi/CS_seminar_flyer.pdf");
	            			if(file.length() > DocumentEditor.MAX_PDF) {
	            				if(_DEBUG) System.out.println("OrgEditor: getText: bin size="+file.length()+" vs "+DocumentEditor.MAX_PDF);
	            				Application_GUI.warning(__("File too large! Current Limit:"+" "+file.length()+"/"+DocumentEditor.MAX_PDF),
	            						__("Document too large for import!"));
	            				in.close();
	            				return;
	            			}
	            			byte bin[] = new byte[(int)file.length()];
	            			int off = 0;
	            			do{
	            				int cnt = in.read(bin, off, bin.length-off);
	            				if(cnt == -1) {
	            					if(_DEBUG) System.out.println("OrgEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            					break;
	            				}
	            				off +=cnt;
            					if(_DEBUG) System.out.println("OrgEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            			}while(off < bin.length);
	            			in.close();
	            			if(DEBUG) System.out.println("DocumentEditor: handle: bin size="+bin.length);
	            			String data = Util.stringSignatureFromByte(bin);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: txt size="+data.length());
	            			
	            			setMode(D_Document.PDF_BODY_FORMAT, data);
	            			
							this.notifyChange(evt);
	            		} catch (FileNotFoundException e) {
	            			e.printStackTrace();
	            		} catch (IOException e) {
	            			e.printStackTrace();
	            		}
	            	}else
	            		if(("html".equals(ext)) || ("htm".equals(ext))){
	            			if(_DEBUG)System.err.println("ControlPane:actionImport: Got: html: implement!");
	            			try{
	            				BufferedReader bri = new BufferedReader(new FileReader(file));
								String data = Util.readAll(bri);
								
								setMode(D_Document.HTM_BODY_FORMAT, data);
								
								this.notifyChange(evt);
	            			}catch(Exception e){
	            				e.printStackTrace();
	            			}
	            		}else
	            			if(("txt".equals(ext))){
	            				try{
		            				BufferedReader bri = new BufferedReader(new FileReader(file));
									String data = Util.readAll(bri);
									
									setMode(D_Document.TXT_BODY_FORMAT, data);
									
									this.notifyChange(evt);
		            			}catch(Exception e){
		            				e.printStackTrace();
		            			}
	            			}else
	            				if(_DEBUG)System.err.println("ControlPane:actionImport: Got: "+ext+": implement!");
	            }catch(Exception e){
	            	e.printStackTrace();
	            }
	        }
		}
	}
	public DocumentEditor getDocument() {
		return editor;
	}
	public MultiformatDocumentEditor getMFDocument() {
		return this;
	}
	/*
	public String getText() {
		editor.updateDoc(data);
		return editor.getText();
	}
	public void setText(String string) {
		editor.setText(string);
	}
	*/
	public String getDBDoc() {
		editor.updateDoc(data);
		return data.getDBDoc();
	}
	public void setDBDoc(String string) {
		if(string != null) {
			data.setDBDoc(string);
			editor.setType(data.getFormatString());
			editor.setText(data.getDocumentString());
		}else
			editor.setText(string);
	}
}
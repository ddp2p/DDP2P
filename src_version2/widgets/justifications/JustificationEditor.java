/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package widgets.justifications;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import ASN1.Encoder;
import util.P2PDDSQLException;
import config.Application_GUI;
import config.DD;
import config.JustificationsListener;
import data.D_Document;
import data.D_Justification;
import data.D_Motion;
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
import data.D_Vote;
import data.JustGIDItem;
import util.Util;
import widgets.components.DocumentEditor;
import widgets.components.GUI_Swing;
import widgets.components.TranslatedLabel;
import widgets.motions.MotionEditor;
import widgets.motions.VoteEditor;
//import widgets.org.CreatorGIDItem;
import static java.lang.System.out;
import static util.Util.__;
@SuppressWarnings("serial")
public class JustificationEditor extends JPanel  implements JustificationsListener, DocumentListener, ItemListener, ActionListener {
	
	private static final int TITLE_LEN = 20;
	private static final int TEXT_LEN_ROWS = 10;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String BODY_FORMAT = "TXT";
	private static final String TITLE_FORMAT = "TXT";
	//modes
	private static final int ALL = 0;
	public static final int ONLY = 1;
	public static final int CHOICE = 2;
	
	
	public JTextField just_title_field;
	public JComboBox just_answer_field;
	public DocumentEditor just_body_field;
	public JTextField date_field;
	public JButton dategen_field;
	public JButton load_field;
	public JButton setTxtMode;
	public JButton setHTMMode;
	public JCheckBox requested;
	public JCheckBox broadcasted;
	public JCheckBox blocked;

	JustGIDItem[] combo_answerTo = new JustGIDItem[]{};
	private boolean enabled = false;
	private String justID = null;
	
	boolean m_editable = false;
	//private int max_general_fields;
	JTabbedPane tabbedPane = new JTabbedPane();
	public D_Justification just; // data about the current organization
	//public D_Vote signature;
	private int mode = ALL;
	private boolean SUBMIT = true;
	private MotionEditor motionEditor;
	private VoteEditor vEditor;
	JPanel panel_body = new JPanel();

	
	public void disable_it() {
		enabled  = false;
		if(DEBUG)System.out.println("JustificationEditor:Disabling");
		disable_just();
	}
	public void disable_just() {
		enabled = false;
		if(this.just_title_field!=null) this.just_title_field.setEnabled(false);
		if(this.just_answer_field!=null) this.just_answer_field.setEnabled(false);
		if(this.just_body_field!=null) this.just_body_field.setEnabled(false);
		if(this.date_field!=null) this.date_field.setEnabled(false);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(false);	
		if(this.load_field!=null) this.load_field.setEnabled(false);
		if(this.setTxtMode!=null) this.setTxtMode.setEnabled(false);
		if(this.setHTMMode!=null) this.setHTMMode.setEnabled(false);
	}
	public void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("JustificationEditor:Enabling");
		enable_just();
	}
	public void enable_just() {
		enabled = true;
		if(this.just_title_field!=null) this.just_title_field.setEnabled(true);
		if(this.just_answer_field!=null) this.just_answer_field.setEnabled(true);
		if(this.just_body_field!=null) this.just_body_field.setEnabled(true);
		if(this.date_field!=null) this.date_field.setEnabled(true);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(true);	
		if(this.load_field!=null) this.load_field.setEnabled(true);
		if(this.setTxtMode!=null) this.setTxtMode.setEnabled(true);
		if(this.setHTMMode!=null) this.setHTMMode.setEnabled(true);
	}
	public JustificationEditor(int _mode){
		mode  = _mode;
		//this.setLayout(new GridBagLayout());
		int y[] = new int[]{0};
		switch(mode){
		case ONLY:
			SUBMIT = false;
			this.add(makeGeneralPanel(y));//this
			 break;
			 /*
		case CHOICE:
			SUBMIT = false;
			makeChoicePanel(this,y);
			break;
			*/
		}
		disable_it();
	}
	public JustificationEditor(){
		tabbedPane.setTabPlacement(JTabbedPane.TOP);
		ImageIcon icon = widgets.app.DDIcons.getJusImageIcon("General Justification");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_sig = widgets.app.DDIcons.getSigImageIcon("General Signature");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_conf = widgets.app.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		int y[] = new int[]{0};

		JPanel cp = new JPanel();
		cp.setLayout(new GridBagLayout()); y[0] = 0;
		vEditor = new VoteEditor(null, this, VoteEditor.CHOICE);//makeChoicePanel(cp,y);
		
		//JPanel gp = new JPanel();
		//gp.setLayout(new GridBagLayout()); y[0] = 0;
		JComponent generalPane = makeGeneralPanel(y);//gp
		
		tabbedPane.addTab(__("Justification Body"), icon, generalPane, __("Generic fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_G);
		
		tabbedPane.addTab(__("Choice"), icon_sig, vEditor, __("Voting choice"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);

		JPanel hp = new JPanel();
		hp.setLayout(new GridBagLayout()); y[0] = 0;
		JComponent handlingPane = makeHandlingPanel(hp, y);
		tabbedPane.addTab(__("Handling"), icon_conf, handlingPane, __("Handling fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_H);
        this.setLayout(new BorderLayout());
		this.add(tabbedPane);
		disable_it();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
	public void setJust(String _justID, boolean force){
		if(DEBUG) out.println("JustEditor:setJust: force="+force);
		if((justID !=null) && (justID.equals(_justID)) && !force){
			if(DEBUG) out.println("JustEditor:setJust: justID="+justID);
			return;
		}
		if((justID==null) && (_justID==null)){
			if(DEBUG) out.println("JustEditor:setJust: _justID="+justID);
			return;
		}
		if(_justID==null) {
			if(DEBUG) out.println("JustEditor:setJust: _ justID null "+force);
			disable_it(); return;
		}
		justID = _justID;
		if(!force && enabled){
			if(DEBUG) out.println("JustEditor:setJust: !force="+force);
			disable_it();
		}
		update_it(force);
		//if(this.extraFields!=null)	this.extraFields.setCurrent(_justID);
		if(DEBUG) out.println("JustificationEditor:setJust: exit");
	}
	/**
	 * Is this org editable?
	 * @return
	 */
	private boolean editable() {
		if(DEBUG) out.println("JustEditor:editable: start");
		if(just == null){
			if(DEBUG) out.println("JustEditor:editable: no just");
			return false;
		}
		if(just.isEditable()) {
			if(DEBUG) out.println("JustEditor:editable");
			return true;			
		}
		if(DEBUG) out.println("JustEditor:editable: exit "+just);
		return false;
	}
	/**
	 * Will reload org from database
	 * returns whether to edit
	 */
	private boolean reloadJust(boolean force){
		if(DEBUG) out.println("JustEditor:reloadJust: start force="+force+" justID="+justID);
		if(justID==null) {
			just = null;
			return true;
		}
		try {
			just = D_Justification.getJustByLID(justID, true, false);
			if (just.getMotionLIDstr() != null) {
				long _jID = Util.lval(just.getMotionLIDstr(), -1);
				if (_jID > 0) just.setMotion(D_Motion.getMotiByLID(_jID, true, false));
			}
			if(DEBUG) out.println("JustEditor:reloadJust: got just ="+just);
		} catch (Exception e) {
			e.printStackTrace();
			disable_it();
			return false;
		}
		if (!editable()) {
			if(DEBUG) out.println("JustEditor:reloadJust: quit not editable");
			return force || DD.EDIT_RELEASED_JUST;
		}
		if (DEBUG) out.println("JustEditor:reloadJust: exit");
		return true;
	}
	private boolean update_it(boolean force) {
		if(DEBUG) out.println("JustEditor:updateit: start");
		if(reloadJust(force))
			enable_it();
		//else return false; // further processing changes arrival_date by handling creator and default_scoring fields
		if(just == null){
			if(DEBUG) out.println("JustEditor:updateit: quit null just");
			return false;
		}
		m_editable = editable(); // editable?
		
		Calendar creation_date = Util.CalendargetInstance();
		String constituent_ID = Util.getStringID(this.getConstituentIDMyself());
		String constituent_GID = this.getConstituentGIDMyself();
		String signature_ID = MotionEditor.findMyVote(just.getMotionLIDstr(), constituent_ID);
		D_Vote _sign;
		try {
			long _s_ID = Util.lval(signature_ID,-1);
			if(_s_ID > 0) {
				_sign = new D_Vote(_s_ID);
			}else{
				_sign = new D_Vote();
				//_sign.motion = moti;
				_sign.motion_ID = just.getMotionLIDstr();
				_sign.global_motion_ID = just.getMotionGID();
				_sign.justification_ID = null;
				_sign.constituent_ID = constituent_ID;
				_sign.global_constituent_ID = constituent_GID;
				_sign.organization_ID = this.just.getOrganizationLIDstr();
				_sign.global_organization_ID = this.just.getOrgGID();
				_sign.creation_date = creation_date;
			}
			vEditor.setSignature(_sign, motionEditor);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return update_fields();
	}
	private long getConstituentIDMyself() {
		return GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
	}
	private String getConstituentGIDMyself() {
		return GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
	}
	@SuppressWarnings("unchecked")
	private boolean update_fields(){
		if(requested!=null) {
			requested.removeItemListener(this);
			requested.setSelected(just.isRequested());
			requested.addItemListener(this);
		}
		if(blocked!=null) {
			blocked.removeItemListener(this);
			blocked.setSelected(just.isBlocked());
			blocked.addItemListener(this);
		}
		if(broadcasted!=null) {
			broadcasted.removeItemListener(this);
			broadcasted.setSelected(just.isBroadcasted());
			broadcasted.addItemListener(this);
		}
		if(date_field!=null) {
			date_field.getDocument().removeDocumentListener(this);
			date_field.setText(Encoder.getGeneralizedTime(just.getCreationDate()));
			date_field.getDocument().addDocumentListener(this);
		}
		vEditor.loadJustificationChoices(just.getMotionLIDstr());
		
		if(just_answer_field != null) {
			//just_answer_field.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_REQUEST))));
			just_answer_field.removeItemListener(this);
			just_answer_field.removeAllItems();
			JustGIDItem sel=null;
			for(JustGIDItem i : vEditor.combo_answerTo){
				just_answer_field.addItem(i);
				if((i.id!=null)&&i.id.equals(just.getAnswerToLIDstr())){ sel = i;}
			}
			if(sel!=null)just_answer_field.setSelectedItem(sel);
			just_answer_field.addItemListener(this);
		}
		
		/*
		if(just.answerTo_ID!=null) {
			//String answer_title;
			if(just.answerTo == null) {
				try {
					just.answerTo = new D_Justification(new Integer(just.answerTo_ID).longValue());
				} catch (NumberFormatException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				}
			}
			if(just.answerTo == null) {
				//answer_title = just.answerTo.justification_title.title_document.getDocumentString();
				//just_answer_field = new JComboBox(combo_answerTo);
				//just_answer_field.addItem(new JustGIDItem(just.answerTo.global_answerTo_ID,just.answerTo.answerTo_ID,answer_title));
			}
		}
		*/
		if(just_body_field != null) {
			this.just_body_field.removeListener(this);
			this.just_body_field.setText(just.getJustificationText().getDocumentString());
			this.just_body_field.addListener(this);
		}
		if(just_title_field != null) {
			this.just_title_field.getDocument().removeDocumentListener(this);
			this.just_title_field.setText(just.getJustificationTitle().title_document.getDocumentString());
			this.just_title_field.getDocument().addDocumentListener(this);
		}
		//update_signature(false);
		return true;
	}			    
    @SuppressWarnings("unchecked")
	private JPanel _makeGeneralPanel(JPanel p, int []_y) {
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p.add(label_just_title, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(just_title_field = new JTextField(TITLE_LEN),c);
		//title_field.addActionListener(this); //name_field.addFocusListener(this);
		just_title_field.getDocument().addDocumentListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_body = new TranslatedLabel("Justification");
		p.add(label_just_body, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		just_body_field = new DocumentEditor();
		//just_body_field.setLineWrap(true);
		//just_body_field.setWrapStyleWord(true);
		//just_body_field.setAutoscrolls(true);
		//just_body_field.setRows(TEXT_LEN_ROWS);
		just_body_field.init(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		just_body_field.addListener(this);
		//p.add(just_body_field, c);
		p.add(panel_body, c);
		panel_body.add(just_body_field.getComponent());
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		p.add(label_answer_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(just_answer_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		just_answer_field.addItemListener(this);
		
		String creation_date = Util.getGeneralizedTime();
		date_field = new JTextField(creation_date);
		date_field.setColumns(creation_date.length());
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		//hash_org.creation_date = creation_date;
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		date_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(dategen_field = new JButton(__("Set Current Date")),c);
		dategen_field.addActionListener(this);
		
		/*
		if(SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			//TranslatedLabel label_submit_just = new TranslatedLabel("Submit");
			//p.add(label_submit_just, c);
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(just_submit_field = new JButton(_("Submit Justification")),c);
			//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
			//just_answer_field.getDocument().addDocumentListener(this);
			just_submit_field.addActionListener(this);
		}
		
		c.gridx = 0; c.gridy = 4;
		//CheckboxGroup cg = null;//new CheckboxGroup();
		requested = new JCheckBox(_("Requested"), false);
		requested.addItemListener(this);
		
		broadcasted = new JCheckBox(_("Broadcasted"), false);
		broadcasted.addItemListener(this);
		
		blocked = new JCheckBox(_("Blocked"), false);
		blocked.addItemListener(this);
		p.add(broadcasted,c);
		c.gridx = 0; c.gridy = 5;
		p.add(requested,c);
		c.gridx = 0; c.gridy = 6;
		p.add(blocked,c);
		*/
		
		//max_general_fields = 5;
		_y[0] = y;
		return p;
	}
    @SuppressWarnings("unchecked")
	private JSplitPane makeGeneralPanel(int []_y) {
    	JPanel p=new JPanel(), p1=new JPanel(), p2=new JPanel();
    	JSplitPane sp= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p1, p2);
    	//_p.setLayout(new FlowLayout());
    	//_p.add(sp);
    	p1.setLayout(new BorderLayout());
    	p1.add(p,BorderLayout.NORTH);
    	p2.setLayout(new GridBagLayout());
    	GridBagConstraints t = new GridBagConstraints();
		t.fill = GridBagConstraints.NONE;
		t.gridx = 0; t.gridy = 0;
		t.anchor = GridBagConstraints.WEST;
    	
		int y = _y[0];
    	p.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		JPanel p_t = new JPanel();
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p_t.add(label_just_title);
		p_t.add(just_title_field = new JTextField(TITLE_LEN));
		just_title_field.getDocument().addDocumentListener(this);
		p2.add(p_t,t);
/*
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p.add(label_just_title, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(just_title_field = new JTextField(TITLE_LEN),c);
		//title_field.addActionListener(this); //name_field.addFocusListener(this);
*/
		
		//c.anchor = GridBagConstraints.EAST;
		//c.gridx = 0; c.gridy = y++;		
		//TranslatedLabel label_just_body = new TranslatedLabel("Justification");
		//p.add(label_just_body, c);
		//c.gridx = 1;
		//c.anchor = GridBagConstraints.WEST;
		just_body_field = new DocumentEditor();
		//just_body_field.setLineWrap(true);
		//just_body_field.setWrapStyleWord(true);
		//just_body_field.setAutoscrolls(true);
		//just_body_field.setRows(TEXT_LEN_ROWS);
		just_body_field.init(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		just_body_field.addListener(this);
		//p.add(just_body_field, c);
		t.gridx = 0; t.gridy = 1;
		t.anchor = GridBagConstraints.WEST;
		t.fill = GridBagConstraints.BOTH;
		p2.add(panel_body,t);//, c);
		//p.add(panel_body, c);
		//panel_body.add(just_body_field.getComponent());

		just_body_field.getComponent(D_Document.RTEDIT).setVisible(false);
		just_body_field.getComponent(D_Document.TEXTAREA).setVisible(false);
		just_body_field.getComponent(D_Document.PDFVIEW).setVisible(false);
		panel_body.add(just_body_field.getComponent(D_Document.TEXTAREA));
		panel_body.add(just_body_field.getComponent(D_Document.RTEDIT));
		panel_body.add(just_body_field.getComponent(D_Document.PDFVIEW));
		
		just_body_field.setType(D_Document.DEFAULT_FORMAT);
		just_body_field.getComponent(D_Document.DEFAULT_EDITOR).setVisible(true);
		
		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		p.add(label_answer_just, c);
		//c.gridx = 1;
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		p.add(just_answer_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		just_answer_field.addItemListener(this);
		
		String creation_date = Util.getGeneralizedTime();
		date_field = new JTextField(creation_date);
		date_field.setColumns(creation_date.length());
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		TranslatedLabel label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
		//c.gridx = 1;
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		//hash_org.creation_date = creation_date;
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		date_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		//c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.anchor = GridBagConstraints.EAST;
		p.add(dategen_field = new JButton(__("Set Current Date")),c);
		dategen_field.addActionListener(this);

		
		c.gridx = 0; c.gridy = y++;		
		//c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		//c.anchor = GridBagConstraints.WEST;
		p.add(load_field = new JButton(__("Load PDF/HTM/TXT")),c);
		load_field.addActionListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		//c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		//c.anchor = GridBagConstraints.WEST;
		p.add(this.setTxtMode = new JButton(__("Set TXT Mode")),c);
		setTxtMode.addActionListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		//c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		//c.anchor = GridBagConstraints.WEST;
		p.add(this.setHTMMode = new JButton(__("Set HTML Mode")),c);
		setHTMMode.addActionListener(this);

		
		/*
		if(SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			//TranslatedLabel label_submit_just = new TranslatedLabel("Submit");
			//p.add(label_submit_just, c);
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(just_submit_field = new JButton(_("Submit Justification")),c);
			//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
			//just_answer_field.getDocument().addDocumentListener(this);
			just_submit_field.addActionListener(this);
		}
		
		c.gridx = 0; c.gridy = 4;
		//CheckboxGroup cg = null;//new CheckboxGroup();
		requested = new JCheckBox(_("Requested"), false);
		requested.addItemListener(this);
		
		broadcasted = new JCheckBox(_("Broadcasted"), false);
		broadcasted.addItemListener(this);
		
		blocked = new JCheckBox(_("Blocked"), false);
		blocked.addItemListener(this);
		p.add(broadcasted,c);
		c.gridx = 0; c.gridy = 5;
		p.add(requested,c);
		c.gridx = 0; c.gridy = 6;
		p.add(blocked,c);
		*/
		
		//max_general_fields = 5;
		_y[0] = y;
		return sp;
	}
	private JComponent makeHandlingPanel(JPanel p, int[] _y) {
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;
		//CheckboxGroup cg = null;//new CheckboxGroup();
		requested = new JCheckBox(__("Requested"), false);
		requested.addItemListener(this);
		
		broadcasted = new JCheckBox(__("Broadcasted"), false);
		broadcasted.addItemListener(this);
		
		blocked = new JCheckBox(__("Blocked"), false);
		blocked.addItemListener(this);
		p.add(broadcasted,c);
		c.gridx = 0; c.gridy = y++;
		p.add(requested,c);
		c.gridx = 0; c.gridy = y++;
		p.add(blocked,c);
		_y[0] = y;
		return p;
	}

	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and load it with "data"
	 * @param _NEW_FORMAT
	 * @param data
	 */
	public void setMode(String _NEW_FORMAT, String data){
		this.just.getJustificationText().setDocumentString(data);
		this.just.getJustificationText().setFormatString(_NEW_FORMAT);
		
		this.just_body_field.removeListener(this);
		just_body_field.getComponent().setVisible(false);
		this.just_body_field.setType(just.getJustificationText().getFormatString());
		this.just_body_field.setText(just.getJustificationText().getDocumentString());
		
		just_body_field.getComponent().setVisible(true);
	    //this.panel_body.removeAll();
	    //this.panel_body.add(motion_body_field.getComponent());
		this.just_body_field.setEnabled(enabled);
		this.just_body_field.addListener(this);
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and convert current data to it
	 * @param _NEW_FORMAT
	 */
	public void switchMode(String _NEW_FORMAT){
		String data = "";
		data = this.just.getJustificationText().convertTo(_NEW_FORMAT);
		if(data==null) return;
		setMode(_NEW_FORMAT, data);
	}

	@Override
	public void justUpdate(String justID, int col, boolean db_sync, D_Justification just) {
		if(DEBUG) System.out.println("JustEditor: justUpdate: id="+justID+" col: "+col+" set_sync="+db_sync);
		if(db_sync) return;
		this.setJust(justID, false);
	}
	@Override
	public void forceJustificationEdit(String justID) {
		if(DEBUG)System.out.println("JustEditor: forceEdit");
		this.setJust(justID, true);
	}
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:changedUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:insertUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("JustEditor:removeUpdate:Action "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if(DEBUG)System.out.println("JustEditor:itemStateChanged:Action "+evt);
		// if(evt.getStateChange())
		this.handleFieldEvent(evt.getSource());		
	}
	@Override
	public void actionPerformed(ActionEvent act) {
		if(DEBUG)System.out.println("JustEditor:actionPerformed:Action: "+act);
		this.handleFieldEvent(act.getSource());
	}
	
	@SuppressWarnings("unchecked")
	public void handleFieldEvent(Object source) {
		if(DEBUG)System.out.println("JustEditor: handleFieldEvent: enter enabled="+enabled);
		
		if(this.broadcasted == source) {
			boolean val = broadcasted.isSelected();
			D_Justification _m = D_Justification.getJustByLID(this.justID, true, true);
			_m.setBroadcasted(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			D_Justification _m = D_Justification.getJustByLID(this.justID, true, true);
			_m.setBlocked(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			D_Justification _m = D_Justification.getJustByLID(this.justID, true, true);
			_m.setRequested(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(!enabled) return;
		//String currentTime = Util.getGeneralizedTime();
		/*
		if(mode == CHOICE) {
			handleChoiceEvent(source);
			return;
		}
		*/
		String creationTime;
		if(date_field != null) {
			creationTime = date_field.getText();
		}else{
			creationTime = Util.getGeneralizedTime();
		}
		if((this.just_body_field==source)||(this.just_body_field.getDocumentSource()==source)) {
			if(DEBUG) out.println("JustEditor:handleFieldEvent: just body");
			String new_text = this.just_body_field.getText();
			
			this.just = D_Justification.getJustByJust_Keep(just);
			this.just.getJustificationText().setDocumentString(new_text);
			this.just.getJustificationText().setFormatString(BODY_FORMAT);
			this.just.setCreationDate(Util.getCalendar(creationTime));
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			return;			
		}
		if (this.setTxtMode == source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setText");
			
			this.just = D_Justification.getJustByJust_Keep(just);
			switchMode(D_Document.TXT_BODY_FORMAT);
			this.just.setCreationDate(Util.getCalendar(creationTime));
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");

		}
		if(this.setHTMMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setHTM");
			this.just = D_Justification.getJustByJust_Keep(just);
			switchMode(D_Document.HTM_BODY_FORMAT);
			this.just.setCreationDate(Util.getCalendar(creationTime));
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			
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
	            	if("pdf".equals(ext)) {
	            		if(DEBUG)System.err.println("ControlPane:actionImport: Got: pdf");
	            		try {
	            			//File f = new File("/home/msilaghi/CS_seminar_flyer.pdf");
	            			InputStream in = new FileInputStream(file); // "/home/msilaghi/CS_seminar_flyer.pdf");
	            			if(file.length() > DocumentEditor.MAX_PDF) {
	            				if(_DEBUG) System.out.println("JustEditor: getText: bin size="+file.length()+" vs "+DocumentEditor.MAX_PDF);
	            				Application_GUI.warning(__("File too large! Current Limit:"+" "+file.length()+"/"+DocumentEditor.MAX_PDF),
	            						__("Document too large for import!"));
	            				return;
	            			}
	            			byte bin[] = new byte[(int)file.length()];
	            			int off = 0;
	            			do{
	            				int cnt = in.read(bin, off, bin.length-off);
	            				if(cnt == -1) {
	            					if(_DEBUG) System.out.println("JustEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            					break;
	            				}
	            				off +=cnt;
            					if(_DEBUG) System.out.println("JustEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            			} while(off < bin.length);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: bin size="+bin.length);
	            			String data = Util.stringSignatureFromByte(bin);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: txt size="+data.length());
	            			
	            			this.just = D_Justification.getJustByJust_Keep(just);
	            			setMode(D_Document.PDF_BODY_FORMAT, data);
	            			this.just.setCreationDate(Util.getCalendar(creationTime));
	            			this.just.setEditable();
	            			this.just.storeRequest();
	            			this.just.releaseReference();
	            			
	            			if(DEBUG) System.out.println("DocumentEditor: handle: done");

	            			
	            		} catch (FileNotFoundException e) {
	            			e.printStackTrace();
	            		} catch (IOException e) {
	            			e.printStackTrace();
	            		}
	            	}else
	            		if(("html".equals(ext)) || ("htm".equals(ext))){
	            			if(_DEBUG)System.err.println("JustEditor: actionImport: Got: html: implement!");
	            			try{
	            				BufferedReader bri = new BufferedReader(new FileReader(file));
								String data = Util.readAll(bri);
								
		            			this.just = D_Justification.getJustByJust_Keep(just);
								setMode(D_Document.HTM_BODY_FORMAT, data);
		            			this.just.setCreationDate(Util.getCalendar(creationTime));
		            			this.just.setEditable();
		            			this.just.storeRequest();
		            			this.just.releaseReference();
								
								if(DEBUG) System.out.println("DocumentEditor: handle: done");


	            			} catch(Exception e){
	            				e.printStackTrace();
	            			}
	            		} else
	            			if(("txt".equals(ext))){
	            				try{
		            				BufferedReader bri = new BufferedReader(new FileReader(file));
									String data = Util.readAll(bri);
									
			            			this.just = D_Justification.getJustByJust_Keep(just);
									setMode(D_Document.TXT_BODY_FORMAT, data);
			            			this.just.setCreationDate(Util.getCalendar(creationTime));
			            			this.just.setEditable();
			            			this.just.storeRequest();
			            			this.just.releaseReference();
									
			            			if(DEBUG) System.out.println("DocumentEditor: handle: done");
	
		            			} catch(Exception e){
		            				e.printStackTrace();
		            			}
	            			} else
	            				if (_DEBUG) System.err.println("JustEditor: actionImport: Got: "+ext+": implement!");
	            } catch(Exception e) {
	            	e.printStackTrace();
	            }
	        }
		}

		if((this.just_title_field==source)||(this.just_title_field.getDocument()==source)) {
			if(DEBUG) out.println("JustificationEditor:handleFieldEvent: just title");
			String new_text = this.just_title_field.getText();
			
			this.just = D_Justification.getJustByJust_Keep(just);
			this.just.getJustificationTitle().title_document.setDocumentString(new_text);
			this.just.getJustificationTitle().title_document.setFormatString(TITLE_FORMAT);
			this.just.setCreationDate(Util.getCalendar(creationTime));
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			return;			
		}

		if((this.date_field==source)||(this.date_field.getDocument()==source)) {
			if(DEBUG) out.println("JustificationEditor:handleFieldEvent: date title");
			String new_text = this.date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			
			this.just = D_Justification.getJustByJust_Keep(just);
			this.just.setCreationDate(cal);
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			return;			
		}
		if(this.dategen_field==source) {
			
			this.just = D_Justification.getJustByJust_Keep(just);
			this.just.setCreationDate(Util.CalendargetInstance());
			this.date_field.setText(Encoder.getGeneralizedTime(this.just.getCreationDate()));
			this.just.setEditable();
			this.just.storeRequest();
			this.just.releaseReference();
			return;			
		}
		if(just_answer_field==source) {
			if(DEBUG) out.println("JustEditor:handleFieldEvent: creator");
			if(DEBUG) Util.printCallPath("Linux tracing");
			JustGIDItem selected = (JustGIDItem) just_answer_field.getSelectedItem();
			String id = null;
			try {
				if(selected == null){
					id = null;
				}else{
					if(DEBUG) out.println("JustEditor:handleFieldEvent: selected just answer="+selected.toStringDump());
					if(selected.id == null) id = null;
					else id = Util.getStringID(Util.lval(selected.id));
				}
				
				this.just = D_Justification.getJustByJust_Keep(just);
				just.setAnswerToLIDstr(id);
				this.just.setCreationDate(Util.getCalendar(creationTime));
				this.just.setEditable();
				this.just.storeRequest();
				this.just.releaseReference();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		if((just.getLIDstr()!=null)&&(motionEditor!=null)) motionEditor.setNewJustificationID(just.getLIDstr());
		if(DEBUG) out.println("JustEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	/**
	 * Called by Motion Editor
	 * @param justification
	 * @param force
	 * @param _motionEditor
	 */
	public void setJustification(D_Justification justification, boolean force, MotionEditor _motionEditor) {
		if(DEBUG) out.println("JustEditor:setJustification: got ="+justification);
		motionEditor = _motionEditor;
		if(justification.getLIDstr()==null){
			this.just = justification;
			this.justID = null;
			update_fields();
			if(force||just.isEditable()) enable_it(); else disable_it();
		}
		else this.setJust(justification.getLIDstr(), force);
	}
	public void setVoteEditor(VoteEditor _vEditor) {
		vEditor = _vEditor;
	}
	public D_Motion getMotion() {
		if (motionEditor != null) return motionEditor.getMotion();
		if (just != null)return just.getMotion();
		return null;
	}
	public void make_new() {
		if (just==null) just = D_Justification.getEmpty();
		if (just.isEditable()) return;
		just.setEditable();
		just.setLIDstr(null);
	}
}

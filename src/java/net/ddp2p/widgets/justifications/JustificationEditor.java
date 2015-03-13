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
package net.ddp2p.widgets.justifications;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.ConstituentListener;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.JustificationsListener;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.JustGIDItem;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
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
import net.ddp2p.widgets.components.DocumentEditor;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.TranslatedLabel;
import net.ddp2p.widgets.motions.MotionEditor;
import net.ddp2p.widgets.motions.VoteEditor;
//import widgets.org.CreatorGIDItem;
import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;
@SuppressWarnings("serial")
public class JustificationEditor extends JPanel  implements JustificationsListener, DocumentListener, ItemListener, ActionListener, MotionsListener, ConstituentListener {
	
	private static final int TITLE_LEN = 20;
	static final int TEXT_LEN_ROWS = 10;
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
	//private String justID = null;
	
	boolean m_editable = false;
	//private int max_general_fields;
	JTabbedPane tabbedPane = new JTabbedPane();
	public D_Justification justificationEdited; // data about the current organization
	public D_Motion motionCurrent = null; // data about the current organization
	//public D_Vote signature;
	private int mode = ALL;
	private boolean SUBMIT = true;
	private MotionEditor motionEditor;
	private VoteEditor vEditor;
	JPanel panel_body = new JPanel();
	private D_Constituent constituentCurrent;

	/**
	 * Disables all GUI fields. sets the flag enabled=false
	 */
	public void disable_it() {
		enabled  = false;
		if(DEBUG)System.out.println("JustificationEditor:Disabling");
		disable_just();
	}
	/**
	 * Disables all GUI fields. sets the flag enabled=false
	 */
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
	/**
	 * Enables all GUI fields. sets the flag enabled=true
	 * 
	 */
	public void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("JustificationEditor:Enabling");
		enable_just();
	}
	/**
	 * Enables all GUI fields. sets the flag enabled=true
	 * 
	 */
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
	/**
	 * If _mode is ONLY then adds a generakPanel([0]), else nothing.
	 * @param _mode
	 */
	public JustificationEditor(int _mode) {
		mode  = _mode;
		//this.setLayout(new GridBagLayout());
		int y[] = new int[]{Util.ival(D_Vote.DEFAULT_YES_COUNTED_LABEL, 0)};
		switch(mode) {
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
		ImageIcon icon = net.ddp2p.widgets.app.DDIcons.getJusImageIcon("General Justification");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_sig = net.ddp2p.widgets.app.DDIcons.getSigImageIcon("General Signature");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_conf = net.ddp2p.widgets.app.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
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
	/**
	 * Sets the justification, and if different from the current one, then it updates the GUI
	 * 
	 * @param _justID
	 * @param force
	 */
	public void setJustification(String _justID, boolean force) {
		D_Justification justification_new = D_Justification.getJustByLID(_justID, true, false);
		if (justification_new == null && _justID != null) {
			if (_DEBUG) out.println("JustEditor: setJust: force="+force+" inexisting jID="+_justID);
			//update_it(false);
			return;
		}
		setJustification(justification_new, force);
	}
	/**
	 * Sets the justification, and if different from the current one, then it updates the GUI
	 * @param justification_new
	 * @param force
	 */
	public void setJustification(D_Justification justification_new, boolean force) {
		String justID = this.getJustificationLIDstr();
		String _justID = null;
		if (justification_new != null) _justID = justification_new.getLIDstr();
		
		if (DEBUG) out.println("JustEditor: setJustification: force="+force);
		if ((justID != null) && (justID.equals(_justID)) && !force){
			if (DEBUG) out.println("JustEditor:setJust: same justID="+justID);
			return;
		}
		if ((justID == null) && (_justID == null)) {
			this.justificationEdited = justification_new;
			if (DEBUG) out.println("JustEditor: setJustification: _justID="+justID);
			update_it(false);
			return;
		}
		if (_justID == null) {
			if (DEBUG) out.println("JustEditor: setJustification: _ justID null "+force);
			this.justificationEdited = justification_new;
			update_it(false);
			disable_it(); return;
		}
		//justID = _justID;
		//this.setJustification(justification, force, _motionEditor);
		this.justificationEdited = justification_new;
		if (! force && enabled) {
			if (DEBUG) out.println("JustEditor: setJustification: !force="+force);
			disable_it();
		}
		update_it(force);
		//if(this.extraFields!=null)	this.extraFields.setCurrent(_justID);
		if (DEBUG) out.println("JustificationEditor: setJustification: exit");
	}
	public String getJustificationLIDstr() {
		if (this.justificationEdited == null) {
			if (DEBUG) out.println("JustificationEditor: getJustificationLIDstr: exit null");
			return null;
		}
		return this.justificationEdited.getLIDstr();
	}
	/**
	 * Is this org editable?
	 * @return
	 */
	private boolean editable() {
		if (DEBUG) out.println("JustEditor:editable: start");
		if (justificationEdited == null) {
			if (DEBUG) out.println("JustEditor:editable: no just");
			return false;
		}
		if (justificationEdited.isEditable()) {
			if(DEBUG) out.println("JustEditor:editable");
			return true;			
		}
		if (DEBUG) out.println("JustEditor:editable: exit "+justificationEdited);
		return false;
	}
	/**
	 * Will reload justification from database.
	 * Fails when setting a volatile one!
	 * 
	 * After reloading, a motion object is piggy-backed in justificationEdited
	 * 
	 * returns whether to edit (based on DD.EDIT_RELEASED_JUST, editable, and force)
	 */
	private boolean reloadJust(boolean force) {
		String justID = this.getJustificationLIDstr();
		if (justID == null) {
			if (DEBUG) out.println("JustEditor: reloadJust: got unsaved just ="+justificationEdited);
//			Util.printCallPath("Why doing this?");
//			justificationEdited = null;
			return true;
		}
		if(DEBUG) out.println("JustEditor: reloadJust: start force="+force+" justID="+justID);
		try {
			justificationEdited = D_Justification.getJustByLID(justID, true, false);
			if (justificationEdited.getMotionLIDstr() != null) {
				long _mID = Util.lval(justificationEdited.getMotionLIDstr(), -1);
				/**
				 * Here do not set motionAll to not disturb signature
				 */
				if (_mID > 0) justificationEdited.setMotionObj(D_Motion.getMotiByLID(_mID, true, false));
			}
			if(DEBUG) out.println("JustEditor:reloadJust: got just ="+justificationEdited);
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
	/**
	 * Calls the update of the justification GUI fields.
	 * Gets the last signature for the motion in this justification 
	 * (or creates an empty one with motion/org from the crt justification, constituent as crt constit),
	 * and sets it in the signature editor;
	 * 
	 * parameter force tells whether to force the reloading of the justification (based on justification LID).
	 * Not used
	 * @param force
	 * @return
	 */
	private boolean update_it(boolean force) {
		if (DEBUG) out.println("JustEditor:updateit: start");
		if (reloadJust(force))
			enable_it();
		//else return false; // further processing changes arrival_date by handling creator and default_scoring fields
		if (justificationEdited == null) {
			if (_DEBUG) out.println("JustEditor:updateit: quit null just");
			return false;
		}
		m_editable = editable(); // editable?
		
		setSignatureForConstituentAndMotion();
		
		return update_justification_GUI_fields();
	}
	public void setSignatureForConstituentAndMotion() {
		Calendar creation_date = Util.CalendargetInstance();
		String constituent_ID = Util.getStringID(this.getConstituentIDMyself());
		String constituent_GID = this.getConstituentGIDMyself();
		if (justificationEdited == null) return;
		String signature_ID = MotionEditor.findMyVote(justificationEdited.getMotionLIDstr(), constituent_ID);
		D_Vote _sign;
		try {
			long _s_ID = Util.lval(signature_ID, -1);
			if (_s_ID > 0) {
				_sign = new D_Vote(_s_ID);
			} else {
				_sign = new D_Vote();
				//_sign.motion = moti;
				_sign.setMotionLID(justificationEdited.getMotionLIDstr());
				_sign.setMotionGID(justificationEdited.getMotionGID());
				_sign.setJustificationLID(null);
				_sign.setConstituentLID(constituent_ID);
				_sign.setConstituentGID(constituent_GID);
				_sign.setOrganizationLID(this.justificationEdited.getOrganizationLIDstr());
				_sign.setOrganizationGID(this.justificationEdited.getOrgGID());
				_sign.setCreationDate(creation_date);
			}
			vEditor.setSignature(_sign, motionEditor, justificationEdited);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Try first to extract it from the constituents widget.
	 * If not , try to extract the answer from the field constituentCurrent
	 * @return
	 */
	private long getConstituentIDMyself() {
		if (GUI_Swing.constituents != null && GUI_Swing.constituents.tree != null)
			return GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
		if (constituentCurrent != null) return constituentCurrent.getLID();
		return -1;
	}
	/**
	 * Try first to extract it from the constituents widget.
	 * If not , try to extract the answer from the field constituentCurrent
	 * @return
	 */
	private String getConstituentGIDMyself() {
		if (GUI_Swing.constituents != null && GUI_Swing.constituents.tree != null)
			return GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
		if (constituentCurrent != null) return constituentCurrent.getGID();
		return null;
	}
	/**
	 * Used to load a justification in the GUI
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean update_justification_GUI_fields(){
		if (requested != null) {
			requested.removeItemListener(this);
			requested.setSelected(justificationEdited.isRequested());
			requested.addItemListener(this);
		}
		if (blocked != null) {
			blocked.removeItemListener(this);
			blocked.setSelected(justificationEdited.isBlocked());
			blocked.addItemListener(this);
		}
		if (broadcasted != null) {
			broadcasted.removeItemListener(this);
			broadcasted.setSelected(justificationEdited.isBroadcasted());
			broadcasted.addItemListener(this);
		}
		if (date_field != null) {
			date_field.getDocument().removeDocumentListener(this);
			date_field.setText(Encoder.getGeneralizedTime(justificationEdited.getCreationDate()));
			date_field.getDocument().addDocumentListener(this);
		}
		vEditor.loadJustificationChoices(justificationEdited.getMotionLIDstr());
		
		if(just_answer_field != null) {
			//just_answer_field.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_REQUEST))));
			just_answer_field.removeItemListener(this);
			just_answer_field.removeAllItems();
			JustGIDItem sel=null;
			for(JustGIDItem i : vEditor.combo_answerTo){
				just_answer_field.addItem(i);
				if((i.id!=null)&&i.id.equals(justificationEdited.getAnswerToLIDstr())){ sel = i;}
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
			this.just_body_field.setText(justificationEdited.getJustificationBody().getDocumentString());
			this.just_body_field.addListener(this);
		}
		if(just_title_field != null) {
			this.just_title_field.getDocument().removeDocumentListener(this);
			this.just_title_field.setText(justificationEdited.getJustificationTitle().title_document.getDocumentString());
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
		just_body_field.init(TEXT_LEN_ROWS);
		just_body_field.addListener(this);
		p.add(panel_body, c);
		panel_body.add(just_body_field.getComponent());
		//just_body_field.setLineWrap(true);
		//just_body_field.setWrapStyleWord(true);
		//just_body_field.setAutoscrolls(true);
		//just_body_field.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		//p.add(just_body_field, c);
		
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
		int yt = 0;
		t.fill = GridBagConstraints.NONE;
		t.gridx = 0; t.gridy = yt++;
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
		
		JPanel p_ans = new JPanel();
		//c.anchor = GridBagConstraints.EAST;
		//c.anchor = GridBagConstraints.WEST;
		//c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		//p.add(label_answer_just, c);
		p_ans.add(label_answer_just);
		
		//c.gridx = 0; c.gridy = y++;		
		//c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.BOTH;
		//p.add(just_answer_field = new JComboBox(combo_answerTo),c);
		p_ans.add(just_answer_field = new JComboBox(combo_answerTo));
		just_answer_field.addItemListener(this);
		t.gridx = 0; t.gridy = yt++;
		p2.add(p_ans,t);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		
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
		t.gridx = 0; t.gridy = yt++;
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

		/**
		 * Here was the answer field
		 */
		
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
		this.justificationEdited.getJustificationBody().setDocumentString(data);
		this.justificationEdited.getJustificationBody().setFormatString(_NEW_FORMAT);
		
		this.just_body_field.removeListener(this);
		just_body_field.getComponent().setVisible(false);
		this.just_body_field.setType(justificationEdited.getJustificationBody().getFormatString());
		this.just_body_field.setText(justificationEdited.getJustificationBody().getDocumentString());
		
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
		data = this.justificationEdited.getJustificationBody().convertTo(_NEW_FORMAT);
		if (data == null) return;
		setMode(_NEW_FORMAT, data);
	}
	@Override
	public void constituentUpdate(D_Constituent c, boolean me, boolean selected) {
		if (c != null && constituentCurrent != null)
			if (c.getLID() == constituentCurrent.getLID()) return;
		this.constituentCurrent = c;
		setSignatureForConstituentAndMotion();
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if(DEBUG) System.out.println("JustEditor: motion_update: id="+motID+" col: "+col+" motObj="+d_motion);
		if (motID != null && d_motion == null)
			d_motion = D_Motion.getMotiByLID(motID, true, false);
		this.motionCurrent = d_motion;
		if (justificationEdited != null && d_motion != null && justificationEdited.getMotionLID() == d_motion.getLID()) {
			if (DEBUG) System.out.println("JustEditor: motion_update: already had same motion!");
			return;
		}
		this.justificationEdited = null;
		if (this.motionCurrent == null) {
			this.disable_it();
			if (_DEBUG) System.out.println("JustEditor: motion_update: null motion!");
			return;
		}
		// prepares an empty justification
		this.make_new(motionCurrent);
		// updates GUI and sets signature in VoteEditor
		update_it(false);
	}
	@Override
	public void motion_forceEdit(String motID) {
		if(DEBUG) System.out.println("JustEditor: motion_update: id="+motID+" force");
		D_Motion d_motion = null;
		if (motID != null)
			d_motion = D_Motion.getMotiByLID(motID, true, false);
		this.motionCurrent = d_motion;
		if (justificationEdited != null && d_motion != null && justificationEdited.getMotionLID() == d_motion.getLID()) {
			if (_DEBUG) System.out.println("JustEditor: motion_update: already had same motion!");
			return;
		}
		this.justificationEdited = null;
		if (this.motionCurrent == null) {
			this.disable_it();
			if (_DEBUG) System.out.println("JustEditor: motion_update: null motion!");
			return;
		}
		// prepares an empty justification
		this.make_new(motionCurrent);
		// updates GUI and sets signature in VoteEditor
		update_it(false);
	}

	@Override
	public void justUpdate(String justID, int col, boolean db_sync, D_Justification just) {
		if (DEBUG) System.out.println("JustEditor: justUpdate: id="+justID+" col: "+col+" set_sync="+db_sync);
		if (db_sync) return;
		this.setJustification(justID, false);
	}
	@Override
	public void forceJustificationEdit(String justID) {
		if (DEBUG) System.out.println("JustEditor: forceEdit");
		this.setJustification(justID, true);
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
			D_Justification _m = this.getJustificationKeep(); //D_Justification.getJustByLID(this.justID, true, true);
			_m.setBroadcasted(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			D_Justification _m = this.getJustificationKeep(); //D_Justification.getJustByLID(this.justID, true, true);
			_m.setBlocked(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			D_Justification _m = this.getJustificationKeep(); //D_Justification.getJustByLID(this.justID, true, true);
			_m.setRequested(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if (!enabled) return;
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
		if ((this.just_body_field == source) || (this.just_body_field.getDocumentSource() == source)) {
			if (DEBUG) out.println("JustEditor:handleFieldEvent: just body");
			String new_text = this.just_body_field.getText();
			String old_text = this.justificationEdited.getJustificationBody().getDocumentString();
			
			String editor_format = this.just_body_field.getFormatString();
			String old_old_text = this.justificationEdited.getJustificationBody().getFormatString();
			
			if (Util.equalStrings_null_or_not(new_text, old_text)
					&& Util.equalStrings_null_or_not(editor_format, old_old_text)) {
				return;
			}
			
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			this.justificationEdited.setJustificationBodyTextAndFormat(new_text, editor_format);
			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
			this.justificationEdited.setEditable();
			if (this.justificationEdited.dirty_any()) this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
			return;			
		}
		if (this.setTxtMode == source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setText");
			
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			switchMode(D_Document.TXT_BODY_FORMAT);
			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
			this.justificationEdited.setEditable();
			this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
			
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");

		}
		if(this.setHTMMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setHTM");
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			switchMode(D_Document.HTM_BODY_FORMAT);
			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
			this.justificationEdited.setEditable();
			this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
			
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
	            			
	            			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
	            			setMode(D_Document.PDF_BODY_FORMAT, data);
	            			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
	            			this.justificationEdited.setEditable();
	            			this.justificationEdited.storeRequest();
	            			this.justificationEdited.releaseReference();
	            			
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
								
		            			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
								setMode(D_Document.HTM_BODY_FORMAT, data);
		            			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
		            			this.justificationEdited.setEditable();
		            			this.justificationEdited.storeRequest();
		            			this.justificationEdited.releaseReference();
								
								if(DEBUG) System.out.println("DocumentEditor: handle: done");


	            			} catch(Exception e){
	            				e.printStackTrace();
	            			}
	            		} else
	            			if(("txt".equals(ext))){
	            				try{
		            				BufferedReader bri = new BufferedReader(new FileReader(file));
									String data = Util.readAll(bri);
									
			            			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
									setMode(D_Document.TXT_BODY_FORMAT, data);
			            			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
			            			this.justificationEdited.setEditable();
			            			this.justificationEdited.storeRequest();
			            			this.justificationEdited.releaseReference();
									
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
			
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			this.justificationEdited.setJustificationTitleText(new_text);
			this.justificationEdited.setJustificationTitleFormat(TITLE_FORMAT);
			this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
			this.justificationEdited.setEditable();
			if (this.justificationEdited.dirty_any()) this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
			return;			
		}

		if((this.date_field==source)||(this.date_field.getDocument()==source)) {
			if(DEBUG) out.println("JustificationEditor:handleFieldEvent: date title");
			String new_text = this.date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			this.justificationEdited.setCreationDate(cal);
			this.justificationEdited.setEditable();
			this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
			return;			
		}
		if(this.dategen_field==source) {
			
			this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
			this.justificationEdited.setCreationDate(Util.CalendargetInstance());
			this.date_field.setText(Encoder.getGeneralizedTime(this.justificationEdited.getCreationDate()));
			this.justificationEdited.setEditable();
			this.justificationEdited.storeRequest();
			this.justificationEdited.releaseReference();
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
				
				this.justificationEdited = D_Justification.getJustByJust_Keep(justificationEdited);
				justificationEdited.setAnswerToLIDstr(id);
				this.justificationEdited.setCreationDate(Util.getCalendar(creationTime));
				this.justificationEdited.setEditable();
				this.justificationEdited.storeRequest();
				this.justificationEdited.releaseReference();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		if((justificationEdited.getLIDstr()!=null)&&(motionEditor!=null)) motionEditor.setNewJustificationID(justificationEdited.getLIDstr());
		if(DEBUG) out.println("JustEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	/**
	 * If GID nut no LID (remote temporary not saved, exit).
	 * If no LID, save that get kept
	 * @return
	 */
	private D_Justification getJustificationKeep() {
		if (justificationEdited == null) {
			if (_DEBUG) out.println("JustEditor: getJustificationKeep: exit null edited object");
			return null;
		}
		if (justificationEdited.getLIDstr() == null) {
			if (justificationEdited.getGID() != null) {
				if (_DEBUG) out.println("JustEditor: getJustificationKeep: exit no LID but LID");
				return null;
			}
			//long jID = 
			justificationEdited.storeSynchronouslyNoException();
		}
		D_Justification j = //D_Justification.getJustByLID(this.justID, true, true);
				D_Justification.getJustByJust_Keep(justificationEdited);
		justificationEdited = j;
		return justificationEdited;
	}
	/**
	 * Called by Motion Editor
	 * @param justification
	 * @param force
	 * @param _motionEditor
	 */
	public void setJustificationAndMotionEditor(D_Justification justification, boolean force, MotionEditor _motionEditor) {
		if (DEBUG) out.println("JustEditor: setJustificationAndMotionEditor: got ="+justification);
		motionEditor = _motionEditor;
		if (justification.getLIDstr() == null) {
			if (DEBUG) out.println("JustEditor: setJustificationAndMotionEditor: set empty just ="+justification);
			this.justificationEdited = justification;
			//this.justID = null;
			update_justification_GUI_fields();
			if (force||justificationEdited.isEditable()) enable_it(); else disable_it();
		} else {
			if (DEBUG) out.println("JustEditor: setJustificationAndMotionEditor: got ="+justification);
			this.setJustification(justification.getLIDstr(), force);
		}
	}
	public void setVoteEditor(VoteEditor _vEditor) {
		if (DEBUG) out.println("JustEditor: setVoteEditor: got ="+  _vEditor);
		vEditor = _vEditor;
	}
	public D_Motion getMotion() {
		if (motionEditor != null) return motionEditor.getMotion();
		if (justificationEdited != null)return justificationEdited.getMotion();
		return null;
	}
	/**
	 * Old: Takes the existing justification, setting its LID to null and its date to current
	 * New: Sets an empty justification, but does not store it yet
	 * 
	 * If an old non-empty justification existed, asks the user on whether to copy its data with loadDuplicatingInTemporary().
	 * 
	 * @param motion
	 */
	public void make_new(D_Motion motion) {
		//boolean DEBUG = true;
		if (DEBUG) out.println("JustEditor: make_new: got motion =" + motion);
		D_Justification old_just = justificationEdited; //if (just == null) 
		D_Justification proposed_just = D_Justification.getEmpty();
		
		boolean load_existing = false;
		if (old_just != null && old_just.getLIDstr() == null) load_existing = true;
		else {
			if (justificationEdited == null ||
					0 == Application_GUI.ask(
					__("Do you want a new justification rather than to reuse the old one?"),
					__("New Justification"), 
					JOptionPane.YES_NO_OPTION)) {
				if (DEBUG) out.println("JustEditor: make_new: keep empty");
			} else {
				if (DEBUG) out.println("JustEditor: make_new: loadDuplicate");
				if (old_just != null) load_existing = true;
			}
		}
		if (load_existing)
			proposed_just.loadDuplicatingInTemporary(old_just);
		proposed_just.setMotionAndOrganizationAll(motion);
		//justificationEdited.setOrganizationLIDstr(motion.getOrganizationLIDstr());
		proposed_just.setConstituentAll(null);
		//justificationEdited.setLIDstr(null);
		//justificationEdited.setGID(null);
		proposed_just.setCreationDate(Util.CalendargetInstance());
		if ( ! proposed_just.isEditable()) {
			if (DEBUG) out.println("JustEditor: make_new: setting editable");
			proposed_just.setEditable();
		}
		justificationEdited = proposed_just;
		if (DEBUG) out.println("JustEditor: make_new: exit getting: "+justificationEdited);
		update_justification_GUI_fields();
		//this.update_it(false);
	}
}

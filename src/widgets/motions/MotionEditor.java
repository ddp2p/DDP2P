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
package widgets.motions;

import static java.lang.System.out;
import static util.Util._;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import table.motion;
import table.signature;
import util.Util;
import widgets.components.DocumentEditor;
import widgets.components.LVComboBox;
import widgets.components.LVListener;
import widgets.components.TranslatedLabel;
import widgets.justifications.JustificationEditor;
import widgets.motions.MotionsListener;
import widgets.motions.MotionsModel;
import widgets.org.OrgEditor;
import ASN1.Encoder;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import data.D_Justification;
import data.D_Motion;
import data.D_MotionChoice;
//import data.D_OrgConcepts;
import data.D_Vote;

class MotionsGIDItem{
	String gid;
	String id;
	String name;
	/**
	 *  create from gid, id , name
	 * @param _gid
	 * @param _id
	 * @param _name
	 */
	public MotionsGIDItem(String _gid, String _id, String _name) {set(_gid, _id, _name);}
	public void set(String _gid, String _id, String _name){
		gid = _gid;
		id = _id;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name+((id!=null)?(" #"+id):"");
		if(id == null) return "JUST:"+super.toString();
		return "JUST:"+id;
	}
	public String toStringDump() {
		return "JUST:"+name+" id="+id+" gid="+gid;
	}
}



@SuppressWarnings("serial")
public class MotionEditor extends JPanel  implements MotionsListener, DocumentListener, ItemListener, ActionListener, LVListener {
	
	private static final int TITLE_LEN = 20;
	private static final int TEXT_LEN_ROWS = 10;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String BODY_FORMAT = "TXT";
	private static final String TITLE_FORMAT = "TXT";
	public boolean SUBMIT = true;
	
	public JTextField motion_title_field;
	public JComboBox motion_answer_field;
	public DocumentEditor motion_body_field;
	public JButton motion_submit_field;
	public JTextField date_field;
	public JButton dategen_field;
	public JCheckBox requested;
	public JCheckBox broadcasted;
	public JCheckBox blocked;
	public JComboBox category_field;
	public JTextField category_field_editor;
	JTabbedPane tabbedPane = new JTabbedPane();

	MotionsGIDItem[] combo_answerTo = new MotionsGIDItem[]{};
	private boolean enabled = false;
	private String motionID = null;
	
	public D_Motion moti; // data about the current organization
	boolean m_editable = false;
	private Object signature_ID;
	//private int max_general_fields;
	private D_Vote signature;
	private D_Justification justification;
	JustificationEditor jEditor;
	VoteEditor vEditor;
	private LVComboBox scoring_options_field;
	
	private void disable_handling() {
		if(this.requested!=null) this.requested.setEnabled(false);
		if(this.broadcasted!=null) this.broadcasted.setEnabled(false);
		if(this.blocked!=null) this.blocked.setEnabled(false);
	}
	private void disable_it() {
		enabled  = false;
		if(this.motion_title_field!=null) this.motion_title_field.setEnabled(false);
		if(this.motion_answer_field!=null) this.motion_answer_field.setEnabled(false);
		if(this.motion_body_field!=null) this.motion_body_field.setEnabled(false);
		if(this.motion_submit_field!=null) this.motion_submit_field.setEnabled(false);
		if(this.date_field!=null) this.date_field.setEnabled(false);
		if(this.scoring_options_field!=null) this.scoring_options_field.setEnabled(false);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(false);
		if(this.category_field!=null) this.category_field.setEnabled(false);
		if(this.category_field_editor!=null) this.category_field_editor.setEnabled(false);
		vEditor.disable_it();
		jEditor.disable_it();
	}
	private void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("MotionEditor:Enabling");
		if(this.motion_title_field!=null) this.motion_title_field.setEnabled(true);
		if(this.motion_answer_field!=null) this.motion_answer_field.setEnabled(true);
		if(this.motion_body_field!=null) this.motion_body_field.setEnabled(true);
		if(this.motion_submit_field!=null) this.motion_submit_field.setEnabled(true);
		if(this.date_field!=null) this.date_field.setEnabled(true);
		if(this.scoring_options_field!=null) this.scoring_options_field.setEnabled(true);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(true);
		if(this.category_field!=null) this.category_field.setEnabled(true);
		if(this.category_field_editor!=null) this.category_field_editor.setEnabled(true);
		if(this.requested!=null) this.requested.setEnabled(true);
		if(this.broadcasted!=null) this.broadcasted.setEnabled(true);
		if(this.blocked!=null) this.blocked.setEnabled(true);
		vEditor.enable_it();
		if((signature!=null) && (signature.justification_ID!=null)) jEditor.enable_it();
		else jEditor.disable_it();
	}
	public MotionEditor () {
		init();
	}
	public MotionEditor (boolean _SUBMIT) {
		SUBMIT = _SUBMIT;
		init();
	}
	void init() {
		//this.setLayout(new GridBagLayout());
		tabbedPane.setTabPlacement(JTabbedPane.TOP);
		ImageIcon icon = config.DDIcons.getOrgImageIcon("General Org");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		int y[] = new int[]{0};
		
		JPanel gp = new JPanel();
		gp.setLayout(new GridBagLayout()); y[0] = 0;
		JComponent generalPane = makeGeneralPanel(gp,y);
		tabbedPane.addTab(_("Motion Body"), icon, generalPane, _("Generic fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_G);
		
		jEditor = new JustificationEditor(JustificationEditor.ONLY);
		
		vEditor = new VoteEditor(this, jEditor, VoteEditor.CHOICE);
		jEditor.setVoteEditor(vEditor);
		tabbedPane.addTab(_("Choice"), icon,  vEditor, _("My Choice"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);
		
		tabbedPane.addTab(_("Justification"), icon, jEditor, _("Explain Motion"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);

		JPanel hp = new JPanel();
		hp.setLayout(new GridBagLayout()); y[0] = 0;
		JComponent handlingPane = makeHandlingPanel(hp, y);
		tabbedPane.addTab(_("Handling"), icon, handlingPane, _("Handling fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_H);

		this.setLayout(new BorderLayout());
		this.add(tabbedPane);
		disable_it();
		this.disable_handling();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
        //scrollPane.setSize(1000, this.getHeight());
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
	public void setMotion(String _motionID, boolean force){
		if(DEBUG) out.println("MotionEditor:setMotion: force="+force);
		if((motionID !=null) && (motionID.equals(_motionID)) && !force){
			if(DEBUG) out.println("MotionEditor:setMotion: motID="+motionID);
			return;
		}
		if((motionID==null) && (_motionID==null)){
			if(DEBUG) out.println("MotionEditor:setMotion: _motID="+motionID);
			return;
		}
		if(_motionID==null) {
			if(DEBUG) out.println("MotionEditor:setMotion: _motID=null force="+force);
			motionID=null;
			disable_it();
			this.disable_handling();
			return;
		}
		motionID = _motionID;
		if(!force && enabled){
			if(DEBUG) out.println("MotionEditor:setMotion: !force="+force);
			disable_it();
		}
		update_it(force);
		//if(this.extraFields!=null)	this.extraFields.setCurrent(_justID);
		if(DEBUG) out.println("MotionEditor:setMotion: exit");
	}
	public static String findMyVote(String motionID, String constituentID) {
		if(motionID==null) return null;
		String sql = 
			"SELECT "+table.signature.signature_ID+
			" FROM "+table.signature.TNAME+
			" WHERE "+table.signature.motion_ID+"=?" +
					" AND "+table.signature.constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> s = null;
		try {
			s = Application.db.select(sql, new String[]{motionID,constituentID});
		} catch (SQLiteException e) {
			e.printStackTrace();
			return null;
		}
		if(s.size()==0) return null;
		String signature_ID = Util.getString(s.get(0).get(0));
		return signature_ID;
	}
	/**
	 * Is this org editable?
	 * @return
	 */
	private boolean editable() {
		if(DEBUG) out.println("MotionEditor:editable: start");
		if(moti == null){
			if(DEBUG) out.println("MotionEditor:editable: no just");
			return false;
		}
		if(moti.isEditable()){
			if(DEBUG) out.println("MotionEditor:editable");
			return true;
		}
		if(DEBUG) out.println("MotionEditor:editable: exit "+moti);
		return false;
	}
	/**
	 * Will reload org from database
	 * returns whether to edit
	 */
	private boolean reloadMotion(boolean force){
		if(DEBUG) out.println("MotionEditor:reloadMotion: start force="+force+" justID="+motionID);
		if(motionID==null) {
			moti = null;
			return false;
		}
		try {
			long _mID = new Integer(motionID).longValue();
			moti = new D_Motion(_mID);
			if(DEBUG) out.println("MotionEditor:reloadMotion: got="+moti);
		} catch (Exception e) {
			e.printStackTrace();
			disable_it();
			return false;
		}
		if (!editable()){
			if(DEBUG) out.println("MotionEditor:reloadMotion: quit not editable");
			return force || DD.EDIT_RELEASED_JUST;
		}
		if(DEBUG) out.println("MotionEditor:reloadMotion: exit");
		return true;
	}
	private long getConstituentIDMyself() {
		return Application.constituents.tree.getModel().getConstituentIDMyself();
	}
	private String getConstituentGIDMyself() {
		return Application.constituents.tree.getModel().getConstituentGIDMyself();
	}
	@SuppressWarnings("unchecked")
	private boolean update_it(boolean force) {
		if(DEBUG) out.println("MotionEditor:updateit: start");
		if(reloadMotion(force)){
			if(DEBUG) out.println("MotionEditor:updateit: enable");
			enable_it();
		}else{
			if(DEBUG) out.println("MotionEditor:updateit: disable");
			disable_it();
		}
		//else return false; // further processing changes arrival_date by handling creator and default_scoring fields
		if(moti == null){
			if(DEBUG) out.println("MotionEditor:updateit: quit null just");
			return false;
		}
		/*
		try {
			this.moti = new D_Motion(Util.lval(motionID, -1));
		} catch (SQLiteException e1) {
			e1.printStackTrace();
			return;
		}
		*/
		Calendar creation_date = Util.CalendargetInstance();
		String constituent_ID = Util.getStringID(this.getConstituentIDMyself());
		String constituent_GID = this.getConstituentGIDMyself();
		signature_ID = findMyVote(motionID, constituent_ID);
		try {
			long _s_ID = Util.lval(signature_ID,-1);
			if(_s_ID > 0) {
				signature = new D_Vote(_s_ID);
				signature.motion = moti;
				long _jID = Util.lval(signature.justification_ID,-1);
				if((_jID > 0) && (signature.justification == null)) {
					justification = signature.justification = new D_Justification(_jID);
					vEditor.setSignature(signature, this);
					jEditor.setJustification(justification,false, this);
				}else{
					vEditor.setSignature(signature, this);
					D_Justification _just = new D_Justification();
					_just.motion = moti;
					_just.constituent_ID = constituent_ID;
					_just.global_constituent_ID = constituent_GID;
					_just.motion_ID = moti.motionID;
					_just.global_motionID = moti.global_motionID;
					_just.organization_ID = this.moti.organization_ID;
					_just.global_organization_ID = this.moti.global_organization_ID;
					_just.creation_date = creation_date;
					jEditor.setJustification(_just, false, this);					
				}
			}else{
				D_Vote _sign = new D_Vote();
				_sign.motion = moti;
				_sign.choice = moti.getDefaultChoice();
				_sign.motion_ID = moti.motionID;
				_sign.global_motion_ID = moti.global_motionID;
				_sign.justification_ID = null;
				_sign.constituent_ID = constituent_ID;
				_sign.global_constituent_ID = constituent_GID;
				_sign.organization_ID = this.moti.organization_ID;
				_sign.global_organization_ID = this.moti.global_organization_ID;
				_sign.creation_date = creation_date;
				vEditor.setSignature(_sign, this);
				signature = _sign;
				
				D_Justification _just = new D_Justification();
				_just.motion = moti;
				_just.constituent_ID = constituent_ID;
				_just.global_constituent_ID = constituent_GID;
				_just.motion_ID = moti.motionID;
				_just.global_motionID = moti.global_motionID;
				_just.organization_ID = this.moti.organization_ID;
				_just.global_organization_ID = this.moti.global_organization_ID;
				_just.creation_date = creation_date;
				jEditor.setJustification(_just, false, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		m_editable = editable(); // editable?

		requested.removeItemListener(this);
		requested.setSelected(moti.requested);
		requested.addItemListener(this);

		blocked.removeItemListener(this);
		blocked.setSelected(moti.blocked);
		blocked.addItemListener(this);

		broadcasted.removeItemListener(this);
		broadcasted.setSelected(moti.broadcasted);
		broadcasted.addItemListener(this);

		date_field.getDocument().removeDocumentListener(this);
		date_field.setText(Encoder.getGeneralizedTime(moti.creation_date));
		date_field.getDocument().addDocumentListener(this);
		
		//String[] scores_array = D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_SCORES)));
		
		//if(DEBUG) System.out.println("MotionEditor: update_it: scorings:"+moti);
		this.scoring_options_field.removeLVListener(this);
		this.scoring_options_field.setArrayValue(D_MotionChoice.getNames(moti.choices));//, table.organization.ORG_LANG_SEP);//Util.getString(org.get(table.organization.ORG_COL_SCORES)));
		this.scoring_options_field.addLVListener(this);


		String sql =
			"SELECT "+table.motion.motion_title+
			","+table.motion.global_motion_ID+
			","+table.motion.motion_ID+
			" FROM "+table.motion.TNAME+
			" WHERE "+table.motion.organization_ID+"=?;";
		try {
			ArrayList<ArrayList<Object>> j = Application.db.select(sql, new String[]{moti.organization_ID}, DEBUG);
			combo_answerTo = new MotionsGIDItem[j.size()+1];
			int k=0;
			combo_answerTo[k++] = new MotionsGIDItem(null, null, _("None"));
			for (ArrayList<Object> _j :j){
				String gid = Util.getString(_j.get(1));
				String id = Util.getString(_j.get(2));
				String name = Util.getString(_j.get(0));
				combo_answerTo[k++] = new MotionsGIDItem(gid, id, name);
			}
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}
		

		//just_answer_field.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_REQUEST))));
		motion_answer_field.removeItemListener(this);
		motion_answer_field.removeAllItems();
		MotionsGIDItem sel=null;
		/*
		if(just.answerTo_ID!=null) {
			//String answer_title;
			if(just.answerTo == null) {
				try {
					just.answerTo = new D_Motion(new Integer(just.answerTo_ID).longValue());
				} catch (NumberFormatException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				} catch (SQLiteException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				}
			}
			if(just.answerTo == null) {
				//answer_title = just.answerTo.motion_title.title_document.getDocumentString();
				//just_answer_field = new JComboBox(combo_answerTo);
				//just_answer_field.addItem(new JustGIDItem(just.answerTo.global_answerTo_ID,just.answerTo.answerTo_ID,answer_title));
			}
		}
		*/
		for(MotionsGIDItem i : combo_answerTo){
			motion_answer_field.addItem(i);
			if(i.id==null) continue;
			if(i.id.equals(moti.enhanced_motionID)){ sel = i;}
		}
		if(sel!=null)motion_answer_field.setSelectedItem(sel);
		motion_answer_field.addItemListener(this);
		
		this.motion_body_field.getDocument().removeDocumentListener(this);
		this.motion_body_field.setText(moti.motion_text.getDocumentString());
		this.motion_body_field.getDocument().addDocumentListener(this);
		
		this.motion_title_field.getDocument().removeDocumentListener(this);
		this.motion_title_field.setText(moti.motion_title.title_document.getDocumentString());
		this.motion_title_field.getDocument().addDocumentListener(this);
		
		
		category_field.removeItemListener(this);
		category_field_editor.getDocument().removeDocumentListener(this);
		category_field.removeAllItems(); category_field.getEditor().setItem("");
		String g_category_others = _("Others");
		boolean g_category_others_added = false;
		String category = moti.category;
		if(true){ // if my org
			String sql_cat = "SELECT "+table.motion.category+
			" FROM "+table.motion.TNAME+
			" WHERE "+table.motion.organization_ID+"=?"+
			" GROUP BY "+table.motion.category;
			ArrayList<ArrayList<Object>> c;
			try {
				c = Application.db.select(sql_cat, new String[]{moti.organization_ID});
				for(ArrayList<Object> i: c){
					String crt = Util.getString(i.get(0));
					if(g_category_others.equals(crt)) g_category_others_added = true;
					category_field.addItem(crt);
					if((category != null)&&(category.equals(crt))){
						category_field.setSelectedItem(crt);
					}
				}
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}else{
			if(category != null){category_field.addItem(category);category_field.setSelectedItem(category);}
		}
		if(!g_category_others_added) category_field.addItem(g_category_others);
		category_field.addItemListener(this);
		category_field_editor.getDocument().addDocumentListener(this);
		
		return true;
	}
	
	private String getConstituentID() {
		// TODO Auto-generated method stub
		return null;
	}
        @SuppressWarnings("unchecked")
	private JPanel makeGeneralPanel(JPanel p, int _y[]) {
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p.add(label_just_title, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(motion_title_field = new JTextField(TITLE_LEN),c);
		//title_field.addActionListener(this); //name_field.addFocusListener(this);
		motion_title_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_choices = new TranslatedLabel("Default Choices");
		p.add(label_choices, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		scoring_options_field = new LVComboBox();
		p.add(scoring_options_field,c);
		scoring_options_field.addLVListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		p.add(label_answer_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(motion_answer_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		motion_answer_field.addItemListener(this);
		
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_category = new TranslatedLabel("Category");
		p.add(label_category, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(category_field = new JComboBox(new String[]{}),c);
		category_field.addItemListener(this);
		category_field_editor = ((JTextField)category_field.getEditor().getEditorComponent());
		category_field_editor.getDocument().addDocumentListener(this);
		category_field.setEditable(true);
		
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
		p.add(dategen_field = new JButton(_("Set Current Date")),c);
		dategen_field.addActionListener(this);

		if(SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(motion_submit_field = new JButton(_("Submit Motion")),c);
			motion_submit_field.addActionListener(this);
		}
		
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0; c.gridy = y-1;		
		TranslatedLabel label_just_body = new TranslatedLabel("Motion");
		p.add(label_just_body, c);
		c.gridx = 0; c.gridy = y++;	 c.gridwidth=2; c.fill = GridBagConstraints.BOTH; //c.ipadx=c.ipady=2;
		//c.anchor = GridBagConstraints.WEST;
		motion_body_field = new DocumentEditor();
		//javax.swing.text.rtf.
		//motion_body_field.setContentType("text/html");
		//motion_body_field.setText("<html><body>This is <em>emphasized</em>.</body></html>");
		motion_body_field.setEditable(true);
		motion_body_field.setSize(500, 200);
		//.setEditorKit(RTFEditorKit);
		motion_body_field.setLineWrap(true);
		motion_body_field.setWrapStyleWord(true);
		motion_body_field.setRows(TEXT_LEN_ROWS);
		motion_body_field.setAutoscrolls(true);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		motion_body_field.getDocument().addDocumentListener(this);
		p.add(motion_body_field, c);
		
		/*
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
		
		//max_general_fields = 6;
		_y[0]=y;
		return p;
	}
	private JComponent makeHandlingPanel(JPanel p, int _y[]) {
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;
		//CheckboxGroup cg = null;//new CheckboxGroup();
		requested = new JCheckBox(_("Requested"), false);
		requested.addItemListener(this);
		
		broadcasted = new JCheckBox(_("Broadcasted"), false);
		broadcasted.addItemListener(this);
		
		blocked = new JCheckBox(_("Blocked"), false);
		blocked.addItemListener(this);
		p.add(broadcasted,c);
		c.gridx = 0; c.gridy = y++;
		p.add(requested,c);
		c.gridx = 0; c.gridy = y++;
		p.add(blocked,c);
		_y[0]=y;
		return p;
	}

	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if(DEBUG)System.out.println("MotionEditor: motion_update: col: "+col+" motID="+motID);
		this.setMotion(motID, false);
	}
	@Override
	public void news_forceEdit(String motID) {
		if(DEBUG)System.out.println("MotionEditor: forceEdit motID="+motID);
		this.setMotion(motID, true);
	}
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("MotionEditor:changedUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("MotionEditor:insertUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("MotionEditor:removeUpdate:Action "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if(DEBUG)System.out.println("MotionEditor:itemStateChanged:Action "+evt);
		// if(evt.getStateChange())
		this.handleFieldEvent(evt.getSource());		
	}
	@Override
	public void actionPerformed(ActionEvent act) {
		if(DEBUG)System.out.println("MotionEditor:actionPerformed:Action: "+act);
		this.handleFieldEvent(act.getSource());
	}
	
	@SuppressWarnings("unchecked")
	public void handleFieldEvent(Object source) {
		if(DEBUG)System.out.println("MotionEditor: handleFieldEvent: enter enabled="+enabled);
		
		if(this.broadcasted == source) {
			boolean val = broadcasted.isSelected();
			MotionsModel.setBroadcasting(this.motionID, val);
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			MotionsModel.setBlocking(this.motionID, val);
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			MotionsModel.setRequested(this.motionID, val);
		}
		if(!enabled) return;
		//String currentTime = Util.getGeneralizedTime();
		String creationTime = date_field.getText();

		
		if(this.scoring_options_field == source) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: default scoring"); 
			//String[] editable_scores = this.scoring_options_field.getVal();
			//String new_text = D_OrgConcepts.stringFromStringArray(editable_scores);
			this.moti.choices = D_MotionChoice.getChoices(this.scoring_options_field.getVal());
			//if(_DEBUG)for (D_MotionChoice _t : moti.choices) System.out.println("Choices-: "+_t);
			this.moti.creation_date = Util.getCalendar(creationTime);
			this.moti.setEditable();
			try {
				this.moti.storeVerified(DEBUG);
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			this.vEditor.setSignature(vEditor.signature, this);
			return;						
		}

		
		if((this.motion_body_field==source)||(this.motion_body_field.getDocument()==source)) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: just body");
			String new_text = this.motion_body_field.getText();
			this.moti.motion_text.setDocumentString(new_text);
			this.moti.motion_text.setFormatString(BODY_FORMAT);
			this.moti.creation_date = Util.getCalendar(creationTime);
			this.moti.setEditable();
			try {
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}

		if((this.motion_title_field==source)||(this.motion_title_field.getDocument()==source)) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: motion title");
			String new_text = this.motion_title_field.getText();
			this.moti.motion_title.title_document.setDocumentString(new_text);
			this.moti.motion_title.title_document.setFormatString(TITLE_FORMAT);
			this.moti.creation_date = Util.getCalendar(creationTime);
			this.moti.setEditable();
			try {
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}

		if((this.date_field==source)||(this.date_field.getDocument()==source)) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: date title");
			String new_text = this.date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			this.moti.creation_date = cal;
			this.moti.setEditable();
			try {
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(this.dategen_field==source) {
			this.moti.creation_date = Util.CalendargetInstance();
			this.date_field.setText(Encoder.getGeneralizedTime(this.moti.creation_date));
			this.moti.setEditable();
			try {
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(motion_answer_field==source) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: creator");
			if(DEBUG) Util.printCallPath("Linux tracing");
			MotionsGIDItem selected = (MotionsGIDItem) motion_answer_field.getSelectedItem();
			String id = null;
			try {
				if(selected == null){
					id = null;
				}else{
					if(DEBUG) out.println("MotionEditor:handleFieldEvent: selected motion answer="+selected.toStringDump());
					if(selected.id == null) id = null;
					else id = ""+(new Integer(selected.id).longValue());
				}
				
				moti.enhanced_motionID = id;
				this.moti.creation_date = Util.getCalendar(creationTime);
				this.moti.setEditable();
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		if((category_field==source) || (category_field_editor==source)||(category_field_editor.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: category");
			String new_category = Util.getString(category_field.getSelectedItem());
			try {
				if((new_category!=null) && "".equals(new_category.trim())) new_category = null;
				//if(new_category == null) return;
				this.moti.category = new_category;
				this.moti.creation_date = Util.getCalendar(creationTime);
				this.moti.setEditable();
				this.moti.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if((motion_submit_field == source)) {
			try {
				long j_id = -1;
				if(this.moti.global_constituent_ID==null){
					this.moti.global_constituent_ID = Application.constituents.tree.getModel().getConstituentGIDMyself();
					this.moti.constituent_ID = Util.getStringID(Application.constituents.tree.getModel().getConstituentIDMyself());
					if(this.moti.global_constituent_ID==null) {
						Application.warning(_("You should first select a constituent identity for this organization!"), _("No Constituent ID for this org!"));
						return;
					}
				}
				this.moti.global_motionID = this.moti.make_ID();
				this.moti.sign();
				long m_id = this.moti.storeVerified(DEBUG);
				if(m_id<=0) return;
				
				if(vEditor.vote_newjust_field.isSelected()) {
					this.justification = jEditor.just;
					this.justification.global_motionID = this.moti.global_motionID;
					this.justification.global_constituent_ID = this.moti.global_constituent_ID;
					this.justification.constituent_ID = this.moti.constituent_ID;
					this.justification.global_organization_ID = this.moti.global_organization_ID;
					this.justification.organization_ID = this.moti.organization_ID;
					this.justification.global_justificationID = this.justification.make_ID();
					justification.sign();
					j_id = this.justification.storeVerified(DEBUG);
					if(j_id<=0) return;					
				}
				
				D_Vote _signature = vEditor.signature;
				_signature.global_motion_ID = this.moti.global_motionID;
				_signature.motion_ID = Util.getStringID(m_id);
				_signature.global_constituent_ID = this.moti.global_constituent_ID;
				_signature.constituent_ID = this.moti.constituent_ID;
				_signature.global_organization_ID = this.moti.global_organization_ID;
				_signature.organization_ID = this.moti.organization_ID;
				if(vEditor.vote_newjust_field.isSelected()) {
					_signature.global_justification_ID = justification.global_justificationID;
					_signature.justification_ID = Util.getStringID(j_id);
				}
				_signature.global_vote_ID = _signature.make_ID();
				_signature.sign();
				long v_id = _signature.storeVerified();
				signature = _signature;
				if(v_id<=0) return;
				
				disable_it();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}
		if(DEBUG) out.println("MotionEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	public void enableNewJustification(boolean b) {
		if(b){
			jEditor.make_new();
			jEditor.enable_just();
		}else jEditor.disable_just();
	}
	public String getNewJustificationID() {
		return jEditor.just.justification_ID;
	}
	/**
	 * Lets justificator pane communicate crt jID to the vote pane
	 * @param jID
	 */
	public void setNewJustificationID(String jID) {
		if(jID.equals(vEditor.signature.justification_ID)) return;
		vEditor.setNewJustificationID(jID);
	}
	@Override
	public void listenLV(Object source) {
		this.handleFieldEvent(source);
	}
	public D_Justification getNewJustification() {
		return jEditor.just;
	}
}

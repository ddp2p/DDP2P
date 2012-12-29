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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import table.justification;
import table.motion;
import util.Util;
import widgets.components.TranslatedLabel;
import widgets.justifications.JustGIDItem;
import widgets.justifications.JustificationEditor;
//import widgets.justifications.JustificationsListener;
//import widgets.justifications.JustificationsModel;
import ASN1.Encoder;
import data.D_Justification;
import data.D_Motion;
import data.D_MotionChoice;
import data.D_Organization;
import data.D_Vote;

@SuppressWarnings("serial")
public class VoteEditor  extends JPanel  implements DocumentListener, ItemListener, ActionListener{

	public JButton just_submit_field;
	public JComboBox vote_choice_field;
	public JComboBox just_old_just_field;
	public JRadioButton vote_nojust_field;
	public JRadioButton vote_oldjust_field;
	public JRadioButton vote_newjust_field;
	public JTextField vote_date_field;
	public JButton vote_dategen_field;
	D_MotionChoice[] choices;
	private static final int ALL = 0;
	public static final int ONLY = 1;
	public static final int CHOICE = 2;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public D_Vote signature;
	private int mode = ALL;
	private boolean SUBMIT = true;
	private MotionEditor motionEditor;
	public JustGIDItem[] combo_answerTo = new JustGIDItem[]{};
	private boolean enabled = true;
	private JustificationEditor justificationEditor;

	public void disable_it() {
		if ((motionEditor!=null) && (motionEditor.moti!=null) && (motionEditor.moti.isEditable())) {
			if(this.just_submit_field!=null){
				this.just_submit_field.setEnabled(false);
				this.just_submit_field.setVisible(false);
			}
		}else{
			this.just_submit_field.setEnabled(true);
			this.just_submit_field.setVisible(true);			
		}
		if(true) return;
		enabled  = false;
		if(this.vote_choice_field!=null) this.vote_choice_field.setEnabled(false);
		if(this.just_old_just_field!=null) this.just_old_just_field.setEnabled(false);
		if(this.vote_nojust_field!=null) this.vote_nojust_field.setEnabled(false);
		if(this.vote_oldjust_field!=null) this.vote_oldjust_field.setEnabled(false);
		if(this.vote_newjust_field!=null) this.vote_newjust_field.setEnabled(false);
		if(this.vote_date_field!=null) this.vote_date_field.setEnabled(false);
		if(this.vote_dategen_field!=null) this.vote_dategen_field.setEnabled(false);
		if(this.just_submit_field!=null) this.just_submit_field.setEnabled(false);
	}
	public void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("JustificationEditor:Enabling");
		if(this.vote_choice_field!=null) this.vote_choice_field.setEnabled(true);
		if(this.just_old_just_field!=null) this.just_old_just_field.setEnabled(true);
		if(this.vote_nojust_field!=null) this.vote_nojust_field.setEnabled(true);
		if(this.vote_oldjust_field!=null) this.vote_oldjust_field.setEnabled(true);
		if(this.vote_newjust_field!=null) this.vote_newjust_field.setEnabled(true);
		if(this.vote_date_field!=null) this.vote_date_field.setEnabled(true);
		if(this.vote_dategen_field!=null) this.vote_dategen_field.setEnabled(true);
		if(this.just_submit_field!=null){ this.just_submit_field.setEnabled(true); this.just_submit_field.setVisible(true);}
		if ((motionEditor!=null) && (motionEditor.moti!=null) && (motionEditor.moti.isEditable())) 
			if(this.just_submit_field!=null){
				this.just_submit_field.setEnabled(false);
				this.just_submit_field.setVisible(false);
			}
	}
	public VoteEditor(MotionEditor _motionEditor, JustificationEditor _justificationEditor, int _mode){
		motionEditor = _motionEditor;
		justificationEditor = _justificationEditor;
		mode  = _mode;
		this.setLayout(new GridBagLayout());
		int y[] = new int[]{0};
		switch(mode){
		case CHOICE:
			//if ((_motionEditor!=null) && (!_motionEditor.moti.isEditable())) SUBMIT = true;
			//else SUBMIT = false;
			SUBMIT = true;
			makeChoicePanel(this,y);
			break;
		case ALL:
			makeChoicePanel(this,y);
			break;			
		}
		disable_it();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
	private long getConstituentIDMyself() {
		return Application.constituents.tree.getModel().getConstituentIDMyself();
	}
	private String getConstituentGIDMyself() {
		return Application.constituents.tree.getModel().getConstituentGIDMyself();
	}
	
	public boolean initMotionOrg(){
		D_Motion mot = getMotion();
		if(mot == null){
			if(DEBUG)System.out.println("VEditor:initMotionOrg: null motion");
			return false;
		}
		if((mot.organization == null) && (mot.global_organization_ID == null) && (mot.organization_ID == null)){
			if(DEBUG)System.out.println("VEditor:initMotionOrg: null org motion: "+mot);
			return false;
		}
		try {
			long _oid = Util.lval(mot.organization_ID,-1);
			if(_oid>0) mot.organization = new D_Organization(_oid);
			else mot.organization = new D_Organization(mot.global_organization_ID, null);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
		/*
		//if(just == null) return false;
		if((getMotion() == null) && (just.global_motionID == null)) return false;
		try {
			just.motion = new D_Motion(just.global_motionID);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		*/
	
	
	D_Motion getMotion(){
		if(motionEditor!=null) return motionEditor.moti;
		if(justificationEditor!=null) return justificationEditor.getMotion();
		return null;
	}
	public D_MotionChoice[] getChoices() {
		if(DEBUG) System.out.println("VoteEditor: getChoices: start");
		D_MotionChoice[] choices = null;
		if(!initMotionOrg()){
			choices =  D_Organization.getDefaultMotionChoices(D_Organization.get_DEFAULT_OPTIONS());
			if(DEBUG)if(choices!=null)for(D_MotionChoice m : choices) System.out.println("VEditor:getChoices:1:"+m);
		}else{
			choices = getMotion().choices;
			if( (choices == null) || (choices.length==0)){
				choices = getMotion().organization.getDefaultMotionChoices();
				if(DEBUG)if(choices!=null)for(D_MotionChoice m : choices) System.out.println("VEditor:getChoices:2:"+m);
			}
			/*
			if( (choices == null) || (choices.length==0)) {
				choices = D_Organization.getDefaultMotionChoices(D_Organization.get_DEFAULT_OPTIONS());
				if(DEBUG)if(choices!=null)for(D_MotionChoice m : choices) System.out.println("VEditor:getChoices:3:"+m);
			}
			*/
		}
		ArrayList<D_MotionChoice> a =  new ArrayList<D_MotionChoice>();
		Collections.addAll(a,choices);//new ArrayList<D_MotionChoice>(Arrays.asList(choices));
		a.add(0, new D_MotionChoice("", null));
		choices = a.toArray(new D_MotionChoice[0]);
		if(DEBUG)if(choices!=null)for(D_MotionChoice m : choices) System.out.println("VEditor:getChoices:4:"+m);
		return choices;
	}
        @SuppressWarnings("unchecked")
	public JPanel makeChoicePanel(JPanel p, int []_y) {
		int y=_y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_choice = new TranslatedLabel("Vote");
		p.add(label_choice, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		choices = getChoices();
		p.add(vote_choice_field = new JComboBox(choices),c);
		vote_choice_field.addItemListener(this);
		
		vote_nojust_field = new JRadioButton(_("No Justification"));
		vote_oldjust_field = new JRadioButton(_("Old Justification"));
		vote_newjust_field = new JRadioButton(_("New Justification"));
		vote_nojust_field.setMnemonic(KeyEvent.VK_N);
		vote_oldjust_field.setMnemonic(KeyEvent.VK_O);
		vote_newjust_field.setMnemonic(KeyEvent.VK_W);
		vote_nojust_field.setActionCommand("j_none");
		vote_oldjust_field.setActionCommand("j_old");
		vote_newjust_field.setActionCommand("j_new");
		vote_nojust_field.setSelected(true);
		vote_oldjust_field.setSelected(false);
		vote_newjust_field.setSelected(false);
		ButtonGroup vote_j_group = new ButtonGroup();
		vote_j_group.add(vote_nojust_field);
		vote_j_group.add(vote_oldjust_field);
		vote_j_group.add(vote_newjust_field);		
		JPanel vj_panel=new JPanel();
		vj_panel.setLayout(new BoxLayout(vj_panel, BoxLayout.Y_AXIS));
		vj_panel.add(vote_nojust_field);
		vj_panel.add(vote_oldjust_field);
		vj_panel.add(vote_newjust_field);		
		vote_nojust_field.addActionListener(this);
		vote_oldjust_field.addActionListener(this);
		vote_newjust_field.addActionListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;
		TranslatedLabel label_type = new TranslatedLabel("Type");
		p.add(label_type, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(vj_panel,c);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_old_just = new TranslatedLabel("Old Justification");
		p.add(label_old_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(just_old_just_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		just_old_just_field.setEnabled(false);
		just_old_just_field.addItemListener(this);

		String creation_date = Util.getGeneralizedTime();
		vote_date_field = new JTextField(creation_date);
		vote_date_field.setColumns(creation_date.length());
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		//hash_org.creation_date = creation_date;
		p.add(vote_date_field,c);
		vote_date_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		vote_date_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = y++;		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(vote_dategen_field = new JButton(_("Set Current Date")),c);
		vote_dategen_field.addActionListener(this);

		if (SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(just_submit_field = new JButton(_("Submit Vote")),c);
			just_submit_field.addActionListener(this);
		}
		_y[0] = y;
		return p;
	}	
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("VoteEditor:changedUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("VoteEditor:insertUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("VoteEditor:removeUpdate:Action "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if(DEBUG)System.out.println("VoteEditor:itemStateChanged:Action "+evt);
		// if(evt.getStateChange())
		this.handleFieldEvent(evt.getSource());		
	}
	@Override
	public void actionPerformed(ActionEvent act) {
		if(DEBUG)System.out.println("VoteEditor:actionPerformed:Action: "+act);
		this.handleFieldEvent(act.getSource());
	}
	
	@SuppressWarnings("unchecked")
	public void handleFieldEvent(Object source) {
		if(DEBUG)System.out.println("VoteEditor: handleFieldEvent: enter enabled="+enabled);
		//if(!enabled) return;
		//String currentTime = Util.getGeneralizedTime();
		if(mode == CHOICE) {
			handleChoiceEvent(source);
			return;
		}else{
			handleChoiceEvent(source);
			return;			
		}
	}
	private void handleChoiceEvent(Object source) {
		//boolean DEBUG = true;
		if(DEBUG) out.println("JustificationEditor:handleFieldEvent: start");
		/*
		String creationTime;
		if(vote_date_field != null) {
			creationTime = vote_date_field.getText();
		}else{
			creationTime = Util.getGeneralizedTime();
		}
		*/
		if((this.vote_date_field==source)||(this.vote_date_field.getDocument()==source)) {
			if(DEBUG) out.println("JustificationEditor:handleFieldEvent: date title");
			String new_text = this.vote_date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			this.signature.creation_date = cal;
			this.signature.setEditable();
			try {
				this.signature.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(this.vote_dategen_field==source) {
			this.signature.creation_date = Util.CalendargetInstance();
			this.vote_date_field.setText(Encoder.getGeneralizedTime(this.signature.creation_date));
			this.signature.setEditable();
			try {
				this.signature.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(this.vote_newjust_field==source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio");
			this.just_old_just_field.setEnabled(false);
			//setChoiceOldJustification(creationTime);
			//enable_just();
			if(motionEditor!=null){
				if(_DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio, in motioneditor");
				motionEditor.enableNewJustification(true);
				signature.justification_ID = motionEditor.getNewJustificationID();
				signature.global_justification_ID = null;
			}else{
				if(_DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio, in justeditor");
				this.justificationEditor.make_new();
				this.enable_just();
				signature.justification_ID = justification.justification_ID;
				signature.global_justification_ID = null;
			}
			return;
		}
		if(this.vote_oldjust_field==source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice oldjust_radio");
			this.just_old_just_field.setEnabled(true);
			setChoiceOldJustification();
			if(motionEditor!=null){
				motionEditor.enableNewJustification(false);
			}else{
				this.disable_just();
			}
			return;
		}
		if(this.vote_nojust_field==source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice nojust_radio");
			this.just_old_just_field.setEnabled(false);
			if(motionEditor!=null){
				motionEditor.enableNewJustification(false);
			}else{
				this.disable_just();
			}
			try{
				signature.justification_ID = null;
				signature.global_justification_ID = null;
				//this.signature.creation_date = Util.getCalendar(creationTime);
				this.signature.setEditable();
				this.signature.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}	
		}
		if(just_old_just_field==source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice old_just_combo");
			//if(DEBUG) Util.printCallPath("Linux tracing");
			setChoiceOldJustification();
			return;
		}
		if(this.vote_choice_field == source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice vote_choice_field combo");
			setChoice();
			return;
		}
		if((just_submit_field == source)) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: submit");
			try {
				long j_id=-1;
				D_Justification justification =null;
				if(signature.choice==null){
					Application.warning(_("No voting choice made.\nPlease return and make a voting choice."), _("No Choice Made"));
					return;
				}
				if(vote_newjust_field.isSelected()) {
					if(DEBUG) out.println("VoteEditor:handleFieldEvent: new justification");
					justification = justificationEditor.just;
					if(motionEditor!=null){
						justification = motionEditor.getNewJustification();
						if(DEBUG) out.println("VoteEditor:handleFieldEvent: new justification from motionEditor="+justification);
					}else{
						if(DEBUG) out.println("VoteEditor:handleFieldEvent: new justification from justEditor="+justification);						
					}
					
					if(justification.global_constituent_ID==null){
						if(DEBUG) out.println("VoteEditor:handleFieldEvent: reget justification");
						justification.global_constituent_ID = Application.constituents.tree.getModel().getConstituentGIDMyself();
						justification.constituent_ID = Util.getStringID(Application.constituents.tree.getModel().getConstituentIDMyself());
						if(justification.global_constituent_ID==null) {
							Application.warning(_("You should first select a constituent identity for this organization!"), _("No Constituent ID for this org!"));
							return;
						}
					}

					
					justification.global_justificationID = justification.make_ID();
					justification.sign();
					j_id = justification.storeVerified(DEBUG);
					if(j_id<=0){
						if(_DEBUG) out.println("VoteEditor:handleFieldEvent: failed saving no new justif");
						return;					
					}
				}else{
					if(DEBUG) out.println("VoteEditor:handleFieldEvent: no new justification selected");
				}
				
				//signature = signature;
				//signature.global_motion_ID = null;
				//signature.motion_ID = justificationEditor.just.motion_ID;
				if(vote_newjust_field.isSelected()) {
					signature.global_justification_ID = justification.global_justificationID;
					signature.justification_ID = Util.getStringID(j_id);//justificationEditor.just.justification_ID;
				}
				signature.global_vote_ID = signature.make_ID();
				signature.sign();
				long v_id = this.signature.storeVerified();
				if(v_id<=0){
					if(_DEBUG) out.println("VoteEditor:handleFieldEvent: submit sign got no local ID");
					return;
				}
				
				/*
				justificationEditor.just.global_justificationID = justificationEditor.just.make_ID();
				justificationEditor.just.sign();
				long id = justificationEditor.just.storeVerified();
				if(id<=0) return;
				*/
				disable_it();
				justificationEditor.disable_it();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
		}
	}
	private void disable_just() {
		justificationEditor.disable_it();
	}
	private void enable_just() {
		justificationEditor.enable_it();
	}
	/*
			//	this.signature.creation_date = Util.getCalendar(creationTime);
			 try {
				this.signature.setEditable();
				this.signature.storeVerified();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
	 */
	public void setChoiceOldJustification(/*String creationTime*/){
		if(DEBUG) System.out.println("VoteEditor: setChoiceOldJustification start");
		JustGIDItem selected = (JustGIDItem) just_old_just_field.getSelectedItem();
		String id = null, gid=null;
		try {
			if(selected == null){
				if(DEBUG) System.out.println("VoteEditor: setChoiceOldJustification no selection null");
				id = null;
			}else{
				if(DEBUG) out.println("VoteEditor:handleFieldEvent: selected old just ="+selected.toStringDump());
				if(selected.id == null){
					if(DEBUG) out.println("VoteEditor:handleFieldEvent: selected id=null");
					id = null;
				}
				else{
					id = ""+(new Integer(selected.id).longValue());
					gid = selected.gid;
				}
			}

			signature.justification_ID = id;
			signature.global_justification_ID = gid;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}	
	}
	public void setChoice(){
		if(DEBUG) System.out.println("VoteEditor: setChoice start");
		D_MotionChoice selected = (D_MotionChoice) this.vote_choice_field.getSelectedItem();
		String name = null, short_name=null;
		try {
			if(selected == null){
				if(DEBUG) System.out.println("VoteEditor: setChoiceOldJustification no selection null");
				short_name = null;
			}else{
				if(DEBUG) out.println("VoteEditor:handleFieldEvent: selected old just ="+selected.toStringDump());
				if(selected.short_name == null){
					if(DEBUG) out.println("VoteEditor:handleFieldEvent: selected choice = null");
					short_name = null;
				}
				else {
					short_name = selected.short_name;
					name = selected.name;
				}
			}

			signature.choice = short_name;
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: selected choice = "+name);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}		
	}
	private static Object getChoiceSelection(String choice, D_MotionChoice[] choices) {
		if(choice == null) return null;
		if(choices == null) return null;
		for(D_MotionChoice c: choices) {
			if(choice.equals(c.short_name)) return c;
		}
		return null;
	}
	public void loadJustificationChoices(String motionID){
		String sql =
			"SELECT "+table.justification.justification_title+
			","+table.justification.global_justification_ID+
			","+table.justification.justification_ID+
			" FROM "+table.justification.TNAME+
			" WHERE "+table.justification.motion_ID+"=?;";
		try {
			ArrayList<ArrayList<Object>> j = Application.db.select(sql, new String[]{motionID}, DEBUG);
			combo_answerTo = new JustGIDItem[j.size()+1];
			int k=0;
			combo_answerTo[k++] = new JustGIDItem(null, null, "");
			for (ArrayList<Object> _j :j){
				String gid = Util.getString(_j.get(1));
				String id = Util.getString(_j.get(2));
				String name = Util.getString(_j.get(0));
				combo_answerTo[k++] = new JustGIDItem(gid, id, name);
			}
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}	
}
	public void setSignature(D_Vote _signature, MotionEditor _motionEditor) {
		if(DEBUG)System.out.println("VoteEditor: setSignature: start");
		motionEditor = _motionEditor;
		signature = _signature;
		update_signature(true);
		if(DEBUG)System.out.println("VoteEditor: setSignature: done");
	}
        @SuppressWarnings("unchecked")
	private void update_signature(boolean _choices) {
		//boolean DEBUG = true;
		if(DEBUG)System.out.println("VoteEditor: update_signature: start");
		if(signature == null){
			if(DEBUG)System.out.println("VoteEditor: update_signature: null sign="+signature);
			return;
		}
		if(mode == JustificationEditor.ONLY){
			if(DEBUG)System.out.println("VoteEditor: update_signature: mode justification only");
			return;
		}
		//if(mode != CHOICE) throw new RuntimeException("Mode="+mode);
		
		vote_choice_field.removeItemListener(this);
		this.choices = getChoices();
		vote_choice_field.removeAllItems();
		for(D_MotionChoice c: this.choices) vote_choice_field.addItem(c);
		vote_choice_field.addItemListener(this);
		
		Object sel = getChoiceSelection(signature.choice, this.choices);
		if(sel!=null) {
			vote_choice_field.removeItemListener(this);
			vote_choice_field.setSelectedItem(sel);
			vote_choice_field.addItemListener(this);
		}else{
			vote_choice_field.setSelectedIndex(-1);
		}
		long j_ID = Util.lval(signature.justification_ID, -1);
		if(j_ID <= 0) {
			this.vote_nojust_field.setSelected(true);
			this.just_old_just_field.setEnabled(false);
		}
		if(j_ID > 0) {
			D_Justification _just = getJustification();
			if(DEBUG)System.out.println("VoteEditor: update_signature: just="+_just);
			if ((_just!=null)&&(_just.isEditable())&&(signature.constituent_ID.equals(_just.constituent_ID))) {
				this.vote_newjust_field.setSelected(true);
				this.just_old_just_field.setEnabled(false);
			}else{
				this.vote_oldjust_field.setSelected(true);
				this.just_old_just_field.setEnabled(true);
			}
		}

		vote_date_field.getDocument().removeDocumentListener(this);
		vote_date_field.setText(Encoder.getGeneralizedTime(signature.creation_date));
		vote_date_field.getDocument().addDocumentListener(this);

		if(_choices) {
			loadJustificationChoices(signature.motion_ID);
		}

		if(just_old_just_field != null) {
			this.just_old_just_field.removeItemListener(this);
			just_old_just_field.removeAllItems();
			JustGIDItem osel=null;
			for(JustGIDItem i : combo_answerTo){
				just_old_just_field.addItem(i);
				if((signature !=null) && (signature.justification_ID != null)) {
					if((i.id!=null)&&i.id.equals(signature.justification_ID)){ osel = i;}
				}
			}
			if(osel!=null)just_old_just_field.setSelectedItem(osel);
			just_old_just_field.addItemListener(this);
		}
	}
	private D_Justification getJustification() {
		return justificationEditor.just;
	}
	public void setNewJustificationID(String jID) {
		try{
			signature.justification_ID = jID;
			signature.global_justification_ID = null;
			//this.signature.creation_date = Util.getCalendar(creationTime);
			this.signature.setEditable();
			this.signature.storeVerified();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}			
	}
}

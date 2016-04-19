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
package net.ddp2p.widgets.motions;
import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_MotionChoice;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.JustGIDItem;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.TranslatedLabel;
import net.ddp2p.widgets.justifications.JustificationEditor;
@SuppressWarnings("serial")
public class VoteEditor  extends JPanel  implements DocumentListener, ItemListener, ActionListener{
	public JButton just_submit_field;
	public JButton just_submit_anonymous_field;
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
	private D_Justification crt_selected_justification;
	boolean haveMotionSubmit() {
		if (DEBUG) {
			System.out.println("VoteEditor: haveMotionSubmit: edit = "+(motionEditor != null));
			if (motionEditor != null) {
				System.out.println("VoteEditor: haveMotionSubmit: mot = "+(motionEditor.getMotion() != null));
				if (motionEditor.getMotion() != null) 
					System.out.println("VoteEditor: haveMotionSubmit: mot_edit = "+(motionEditor.getMotion().isEditable()));
			}
		}
		return
				(motionEditor != null)
				&& (motionEditor.getMotion() != null)
				&& (motionEditor.getMotion().isEditable());
	}
	public void disable_it() {
		if (haveMotionSubmit()) {
			if (this.just_submit_field != null) {
				this.just_submit_field.setEnabled(false);
				this.just_submit_field.setVisible(false);
			}
			if (this.just_submit_anonymous_field != null) {
				this.just_submit_anonymous_field.setEnabled(false);
				this.just_submit_anonymous_field.setVisible(false);
			}
		} else {
			this.just_submit_field.setEnabled(true);
			this.just_submit_field.setVisible(true);			
			this.just_submit_anonymous_field.setEnabled(true);
			this.just_submit_anonymous_field.setVisible(true);			
		}
		if (true) return;
		enabled  = false;
		if(this.vote_choice_field!=null) this.vote_choice_field.setEnabled(false);
		if(this.just_old_just_field!=null) this.just_old_just_field.setEnabled(false);
		if(this.vote_nojust_field!=null) this.vote_nojust_field.setEnabled(false);
		if(this.vote_oldjust_field!=null) this.vote_oldjust_field.setEnabled(false);
		if(this.vote_newjust_field!=null) this.vote_newjust_field.setEnabled(false);
		if(this.vote_date_field!=null) this.vote_date_field.setEnabled(false);
		if(this.vote_dategen_field!=null) this.vote_dategen_field.setEnabled(false);
		if(this.just_submit_field!=null) this.just_submit_field.setEnabled(false);
		if(this.just_submit_anonymous_field!=null) this.just_submit_anonymous_field.setEnabled(false);
	}
	public void enable_it() {
		enabled  = true;
		if (DEBUG)System.out.println("JustificationEditor:Enabling");
		if (this.vote_choice_field != null) this.vote_choice_field.setEnabled(true);
		if (this.just_old_just_field != null) this.just_old_just_field.setEnabled(true);
		if (this.vote_nojust_field != null) this.vote_nojust_field.setEnabled(true);
		if (this.vote_oldjust_field != null) this.vote_oldjust_field.setEnabled(true);
		if (this.vote_newjust_field != null) this.vote_newjust_field.setEnabled(true);
		if (this.vote_date_field != null) this.vote_date_field.setEnabled(true);
		if (this.vote_dategen_field != null) this.vote_dategen_field.setEnabled(true);
		if (this.just_submit_field != null) { this.just_submit_field.setEnabled(true); this.just_submit_field.setVisible(true);}
		if (this.just_submit_anonymous_field != null) { this.just_submit_anonymous_field.setEnabled(true); this.just_submit_anonymous_field.setVisible(true);}
		if (haveMotionSubmit()) {
			if (this.just_submit_field != null) {
				this.just_submit_field.setEnabled(false);
				this.just_submit_field.setVisible(false);
			}
			if (this.just_submit_anonymous_field != null) {
				this.just_submit_anonymous_field.setEnabled(false);
				this.just_submit_anonymous_field.setVisible(false);
			}
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
		return scrollPane;
	}
	private long getConstituentIDMyself() {
		return GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
	}
	private String getConstituentGIDMyself() {
		return GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
	}
	public boolean initMotionOrg(){
		D_Motion mot = getMotion();
		if(mot == null){
			if(DEBUG)System.out.println("VEditor:initMotionOrg: null motion");
			return false;
		}
		if((mot.getOrganization() == null) && (mot.getOrganizationGID_force() == null) && (mot.getOrganizationLIDstr() == null)){
			if(DEBUG)System.out.println("VEditor:initMotionOrg: null org motion: "+mot);
			return false;
		}
		try {
			long _oid = Util.lval(mot.getOrganizationLIDstr(),-1);
			if(_oid>0) mot.setOrganization(D_Organization.getOrgByLID_NoKeep(_oid, true));
			else mot.setOrganization(D_Organization.getOrgByGID_or_GIDhash_NoCreate(mot.getOrganizationGID_force(), null, true, false));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * 
	 * @return
	 */
	D_Motion getMotion() {
		if (motionEditor != null) return motionEditor.getMotion();
		if (justificationEditor != null) return justificationEditor.getMotion();
		return null;
	}
	public D_MotionChoice[] getChoices() {
		if(DEBUG) System.out.println("VoteEditor: getChoices: start");
		D_MotionChoice[] choices = null;
		if (! initMotionOrg()) {
			choices =  D_Organization.getDefaultMotionChoices(D_Organization.get_DEFAULT_OPTIONS());
			if (DEBUG) if (choices != null) for (D_MotionChoice m : choices) System.out.println("VEditor:getChoices:1:"+m);
		} else {
			choices = getMotion().getChoices();
			if( (choices == null) || (choices.length==0)){
				choices = getMotion().getOrganization().getDefaultMotionChoices();
				if(DEBUG)if(choices!=null)for(D_MotionChoice m : choices) System.out.println("VEditor:getChoices:2:"+m);
			}
		}
		ArrayList<D_MotionChoice> a =  new ArrayList<D_MotionChoice>();
		Collections.addAll(a,choices);
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
		vote_nojust_field = new JRadioButton(__("No Justification"));
		vote_oldjust_field = new JRadioButton(__("Old Justification"));
		vote_newjust_field = new JRadioButton(__("New Justification"));
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
		p.add(vote_date_field,c);
		vote_date_field.setForeground(Color.GREEN);
		vote_date_field.getDocument().addDocumentListener(this);
		c.gridx = 0; c.gridy = y++;		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(vote_dategen_field = new JButton(__("Set Current Date")),c);
		vote_dategen_field.addActionListener(this);
		if (SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(just_submit_field = new JButton(__("Submit Vote")),c);
			just_submit_field.addActionListener(this);
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(just_submit_anonymous_field = new JButton(__("Submit Anonymously")),c);
			just_submit_anonymous_field.addActionListener(this);
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
		if (mode == CHOICE) {
			handleChoiceEvent(source);
			return;
		}else{
			handleChoiceEvent(source);
			return;			
		}
	}
	private void handleChoiceEvent(Object source) {
		if(DEBUG) out.println("JustificationEditor:handleFieldEvent: start");
		if ((this.vote_date_field == source) || (this.vote_date_field.getDocument() == source)) {
			if(DEBUG) out.println("JustificationEditor:handleFieldEvent: date title");
			String new_text = this.vote_date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			this.signature.setCreationDate(cal);
			this.signature.setEditable();
			try {
				this.signature.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(this.vote_dategen_field==source) {
			this.signature.setCreationDate(Util.CalendargetInstance());
			this.vote_date_field.setText(Encoder.getGeneralizedTime(this.signature.getCreationDate()));
			this.signature.setEditable();
			try {
				this.signature.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if (this.vote_newjust_field == source) {
			if (DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio");
			this.just_old_just_field.setEnabled(false);
			if (motionEditor != null) {
				if (DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio, in motioneditor");
				motionEditor.enableNewJustification(true);
			} else {
				if (DEBUG) out.println("VoteEditor:handleFieldEvent: choice newjust_radio, in justeditor");
				this.justificationEditor.make_new(getMotion());
				this.enable_just();
			}
			return;
		}
		if (this.vote_oldjust_field == source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice oldjust_radio");
			this.just_old_just_field.setEnabled(true);
			setChoiceOldJustification();
			if (motionEditor != null) {
				motionEditor.enableNewJustification(false);
			}else{
				this.disable_just();
			}
			return;
		}
		if (this.vote_nojust_field == source) {
			if (DEBUG) out.println("VoteEditor:handleFieldEvent: choice nojust_radio");
			this.just_old_just_field.setEnabled(false);
			if (motionEditor != null) {
				motionEditor.enableNewJustification(false);
			} else {
				this.disable_just();
			}
		}
		if (just_old_just_field == source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice old_just_combo");
			setChoiceOldJustification();
			return;
		}
		if (this.vote_choice_field == source) {
			if(DEBUG) out.println("VoteEditor:handleFieldEvent: choice vote_choice_field combo");
			setChoice();
			return;
		}
		if ((just_submit_anonymous_field == source)) {
			if (DEBUG) out.println("VoteEditor:handleFieldEvent: submit");
			generateVoteAndFinalizeJustificationAndBroadcastMotion(true);
		}
		if ((just_submit_field == source)) {
			generateVoteAndFinalizeJustificationAndBroadcastMotion(false);
		}
	}
	/**
	 * This was the old implementation with signature only. Remove when new submit is debugged.
	 * @param anonymous
	 */
	/**
	 * This is a unified implementation. If anonymous is set to false, then the justification
	 * is signed by myself.
	 * 
	 * A vote is stored, and the motion is set to broadcasted!
	 * @param anonymous
	 */
	public void generateVoteAndFinalizeJustificationAndBroadcastMotion(boolean anonymous) {
		D_Constituent c_myself = GUI_Swing.constituents.tree.getModel().getConstituentMyself();
		String c_myself_GID = GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
		long c_myself_LID = GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
		if (c_myself_GID == null) {
			Application_GUI.warning(__("You should first select a constituent identity for this organization!"), __("No constituent ID for this org!"));
					return;
		}
		if (DEBUG) out.println("VoteEditor: submitAnonymously: myself exists");
		if (! Util.equalStrings_null_or_not(signature.getConstituentGID(), c_myself_GID)) {
			signature = new D_Vote();
			signature.setMotionAndOrganizationAll(getMotion());
			signature.setCreationDate();
			if (_DEBUG) out.println("VoteEditor: generateVoteAndFinalizeJustificationAndBroadcastMotion: new vote: myself = "+signature);
		}
		try {
			D_Justification justification = null;
			if (signature.getChoice() == null) {
				Application_GUI.warning(__("No voting choice made.\nPlease return and make a voting choice."), __("No Choice Made"));
				return;
			}
			if (this.vote_oldjust_field.isSelected()) {
				JustGIDItem oldj = setChoiceOldJustification();
				if (oldj != null) {
					if (DEBUG) out.println("VoteEditor: submitAnonymously: setting oldj="+oldj);
					justification = D_Justification.getJustByLID(oldj.id, true, false);
					if (justification == null) {
						if (_DEBUG) out.println("VoteEditor: submitAnonymously: failed oldj="+oldj);
						return;
					}
				}
				if (justification != null) {
					D_Justification old_justification = justification; 
					if (DEBUG) out.println("VoteEditor: submitAnonymously: setting old j=" + justification);
					if (justification.getGID() == null) {
						if (! anonymous) {
							if (DEBUG) out.println("VoteEditor: submitAnonymously: set justification submitter");
							justification.setConstituentAll(c_myself);
							justification.sign();
						}
						if (DEBUG) out.println("VoteEditor: submitAnonymously: from temp old just");
						justification = D_Justification.createFinalJustificationFromNew(justification);
					} else {
						if (justification.isTemporary())
							if (_DEBUG) out.println("VoteEditor: submitAnonymously: from old temporary just = " + justification);
						if (DEBUG) out.println("VoteEditor: submitAnonymously: from old unknown");
					}
					if (justification == null) {
						if (DEBUG) out.println("VoteEditor: submitAnonymously: old justification trouble: ="+old_justification);	
						return;
					}
					if (DEBUG) out.println("VoteEditor: submitAnonymously: old justification to use: =" + justification);	
				}
			}
			if (vote_newjust_field.isSelected()) {
				if (DEBUG) out.println("VoteEditor: submitAnonymously: new justification");
				justification = justificationEditor.justificationEdited;
				D_Justification old_justification = justification; 
				if (motionEditor != null) {
					justification = motionEditor.getNewJustification();
					if (DEBUG) out.println("VoteEditor: submitAnonymously: new justification from motionEditor="+justification);
				} else {
					if (DEBUG) out.println("VoteEditor: submitAnonymously: new justification from justEditor="+justification);						
				}
				if (justification.getGID() == null) {
					if (! anonymous) { 
						if (DEBUG) out.println("VoteEditor: submitAnonymously: set justification submitter");
						justification.setConstituentAll(c_myself);
						justification.sign();
					}
					if (DEBUG) out.println("VoteEditor: submitAnonymously: from new");
					justification = D_Justification.createFinalJustificationFromNew(justification);
				} else {
					if (justification.isTemporary())
						if (_DEBUG) out.println("VoteEditor: submitAnonymously: from unknown temp justification j="+justification);
					if (_DEBUG) out.println("VoteEditor: submitAnonymously: from unknown justification");
					justification = D_Justification.createFinalJustification_FromUnknown(justification);
				}
				if (justification == null) {
					if (DEBUG) out.println("VoteEditor: submitAnonymously: new justification trouble: ="+old_justification);	
					return;
				}
				if (DEBUG) out.println("VoteEditor: submitAnonymously: new justification to use: =" + justification);	
			} else {
				if (DEBUG) out.println("VoteEditor: submitAnonymously: no new justification selected");
			}
			if (DEBUG) out.println("VoteEditor: submitAnonymously: no new justification selected");
			signature.setJustificationAll(null);
			if (vote_newjust_field.isSelected() || this.vote_oldjust_field.isSelected() || justification != null) {
				String jGID = justification.getGID();
				String jLID = justification.getLIDstr();
				if (DEBUG) out.println("VoteEditor: submitAnonymously: setting justification selected: "+jLID+" GID="+jGID);
				signature.setJustificationAll(justification);
			} else {
				if (_DEBUG) System.out.println("VoteEditor: submitAnonymously: not setting a justification");
			}
			signature.setConstituentGID(c_myself_GID); 
			signature.setConstituentLID(Util.getStringID(c_myself_LID));
			signature.setConstituentObjOnly(c_myself);
			signature.setGID();
			D_Vote old_signature = new D_Vote(signature.getGID());
			if (old_signature.getLIDstr() != null) {
				String old_date = Encoder.getGeneralizedTime(old_signature.getCreationDate());
				String new_date = Encoder.getGeneralizedTime(signature.getCreationDate());
				if (old_date.equals(new_date)) {
					if (DEBUG) System.out.println("\n\nVoteEditor: submitAnonymously: attack: old="+old_signature);
					if (DEBUG) System.out.println("\n***\n\nVoteEditor: submitAnonymously: attack: new="+signature+"\n");
					int attack = Application_GUI.ask (
							__("Update signature creation date?"),
							__("Votes from you on the same issue!"), JOptionPane.YES_NO_OPTION);
					switch (attack) {
					case 0: 
						signature.setCreationDate(Util.CalendargetInstance());
						this.vote_date_field.setText(Encoder.getGeneralizedTime(this.signature.getCreationDate()));
						if (_DEBUG) System.out.println("VoteEditor: submitAnonymously: updated date");
						break;
					case 1: 
						if (_DEBUG) System.out.println("VoteEditor: submitAnonymously: attack date");
						break;
					case 2: 
					default:
						return;
					}
				}
			}
			signature.sign();
			long v_id = this.signature.storeVerified();
			if (v_id <= 0) {
				if(_DEBUG) Util.printCallPath("VoteEditor: handleFieldEvent: submit sign got no local ID");
				return;
			}
			if (DEBUG) System.out.println("VoteEditor: submitAnonymously: obtained = "+signature);
			disable_it();
			justificationEditor.disable_it();
			try {
				D_Motion m = D_Motion.getMotiByLID(signature.getMotionLIDstr(), true, true);
				m.resetCache();
				m.setBroadcasted(true);
				if (m.dirty_any()) {
					if (DEBUG) System.out.println("VoteEditor: submitAnonymously: set motion broadcastable");
					m.storeRequest();
				}
				m.releaseReference();
				m.resetCache(); 
			} catch(Exception e){e.printStackTrace();}
			if (justification != null) justification.resetCache();
			if (old_signature != null) {
				D_Justification old_jus = old_signature.getJustificationFromObjOrLID();
				if (old_jus != null) old_jus.resetCache();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("VoteEditor: submitAnonymously: done");
	}
	private void disable_just() {
		justificationEditor.disable_it();
	}
	private void enable_just() {
		justificationEditor.enable_it();
	}
	public JustGIDItem setChoiceOldJustification(){
		if(DEBUG) System.out.println("VoteEditor: setChoiceOldJustification start");
		JustGIDItem selected = (JustGIDItem) just_old_just_field.getSelectedItem();
		String id = null, gid=null;
		try {
			if (selected == null) {
				if (DEBUG) System.out.println("VoteEditor: setChoiceOldJustification no selection null");
				id = null;
			} else {
				if (DEBUG) out.println("VoteEditor:handleFieldEvent: selected old just ="+selected.toStringDump());
				if (selected.id == null) {
					if (DEBUG) out.println("VoteEditor:handleFieldEvent: selected id=null");
					id = null;
				}
				else {
					id = ""+(new Integer(selected.id).longValue());
					gid = selected.gid;
				}
			}
			signature.setJustificationLID(id);
			signature.setJustificationGID(gid);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}	
		return selected;
	}
	public void setChoice() {
		if (DEBUG) System.out.println("VoteEditor: setChoice start");
		D_MotionChoice selected = (D_MotionChoice) this.vote_choice_field.getSelectedItem();
		String name = null, short_name = null;
		try {
			if (selected == null) {
				if(DEBUG) System.out.println("VoteEditor: setChoiceOldJustification no selection null");
				short_name = null;
			} else {
				if (DEBUG) out.println("VoteEditor:handleFieldEvent: selected old just ="+selected.toStringDump());
				if (selected.short_name == null) {
					if (DEBUG) out.println("VoteEditor:handleFieldEvent: selected choice = null");
					short_name = null;
				}
				else {
					short_name = selected.short_name;
					name = selected.name;
				}
			}
			signature.setChoice(short_name);
			if (DEBUG) out.println("VoteEditor:handleFieldEvent: selected choice = "+name);
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
		D_Motion m = D_Motion.getMotiByLID(motionID, true, false);
		ArrayList<JustGIDItem> j;
		if (m != null)
			j = m.getJustificationsListForMotion();
		else
			j = new ArrayList<JustGIDItem>();
		combo_answerTo = new JustGIDItem[j.size()+1];
		int k = 0;
		combo_answerTo[k++] = new JustGIDItem(null, null, "");
		for (JustGIDItem _j :j){
			String gid = Util.getString(_j.gid);
			String id = Util.getString(_j.id);
			String name = Util.getString(_j.name);
			combo_answerTo[k++] = new JustGIDItem(gid, id, name);
		}
}
	public void setSignature(D_Vote _signature, MotionEditor _motionEditor, D_Justification _justif) {
		if (DEBUG) System.out.println("VoteEditor: setSignature: start "+_signature);
		motionEditor = _motionEditor;
		if (_justif != null) crt_selected_justification = _justif;
		signature = _signature;
		update_signature_GUI_fields(true);
		if (DEBUG) System.out.println("VoteEditor: setSignature: done");
	}
	/**
	 * Used to update a the GUI for a given signature
	 * @param _choices
	 */
	@SuppressWarnings("unchecked")
	private void update_signature_GUI_fields(boolean _choices) {
		if (DEBUG) System.out.println("VoteEditor: update_signature: start");
		if (signature == null) {
			if(DEBUG)System.out.println("VoteEditor: update_signature: null sign="+signature);
			return;
		}
		if (mode == JustificationEditor.ONLY) {
			if (DEBUG) System.out.println("VoteEditor: update_signature: mode justification only");
			return;
		}
		vote_choice_field.removeItemListener(this);
		this.choices = getChoices();
		vote_choice_field.removeAllItems();
		for(D_MotionChoice c: this.choices) vote_choice_field.addItem(c);
		vote_choice_field.addItemListener(this);
		Object sel = getChoiceSelection(signature.getChoice(), this.choices);
		if(sel!=null) {
			vote_choice_field.removeItemListener(this);
			vote_choice_field.setSelectedItem(sel);
			vote_choice_field.addItemListener(this);
		}else{
			vote_choice_field.setSelectedIndex(-1);
		}
		long j_ID = Util.lval(signature.getJustificationLIDstr(), -1);
		if (j_ID <= 0) {
			this.vote_nojust_field.setSelected(true);
			this.just_old_just_field.setEnabled(false);
		}
		if (j_ID > 0) {
			D_Justification _just = getJustification();
			if (DEBUG) System.out.println("VoteEditor: update_signature: just="+_just);
			if ((_just!=null)&&(_just.isEditable())&&(signature.getConstituentLIDstr().equals(_just.getConstituentLIDstr()))) {
				this.vote_newjust_field.setSelected(true);
				this.just_old_just_field.setEnabled(false);
			}else{
				this.vote_oldjust_field.setSelected(true);
				this.just_old_just_field.setEnabled(true);
			}
		}
		vote_date_field.getDocument().removeDocumentListener(this);
		vote_date_field.setText(Encoder.getGeneralizedTime(signature.getCreationDate()));
		vote_date_field.getDocument().addDocumentListener(this);
		if (_choices) {
			loadJustificationChoices(signature.getMotionLIDstr());
		} else {
			if (DEBUG) System.out.println("VoteEditor: update_signature: no choices");
		}
		if (just_old_just_field != null) {
			this.just_old_just_field.removeItemListener(this);
			just_old_just_field.removeAllItems();
			JustGIDItem osel = null;
			for (JustGIDItem i : combo_answerTo) {
				just_old_just_field.addItem(i);
				if ((signature != null) && (signature.getJustificationLIDstr() != null)) {
					if ((i.id != null) && i.id.equals(signature.getJustificationLIDstr())) { osel = i;}
				}
				if (osel == null && this.crt_selected_justification != null) {
					if ((i.id != null) && i.id.equals(crt_selected_justification.getLIDstr())) {
						osel = i;
					}
				}
			}
			if (osel != null) just_old_just_field.setSelectedItem(osel);
			else crt_selected_justification = null;
			just_old_just_field.addItemListener(this);
		}
	}
	private D_Justification getJustification() {
		return justificationEditor.justificationEdited;
	}
	public void setNewJustificationID(String jID) {
		try{
			signature.setJustificationLID(jID);
			signature.setJustificationGID(null);
			this.signature.setEditable();
			this.signature.storeVerified();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}			
	}
}

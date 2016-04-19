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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.ConstituentListener;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_MotionChoice;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.MotionsGIDItem;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.components.DocumentEditor;
import net.ddp2p.widgets.components.DocumentFilter;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.components.LVComboBox;
import net.ddp2p.widgets.components.LVListener;
import net.ddp2p.widgets.components.TranslatedLabel;
import net.ddp2p.widgets.justifications.JustificationEditor;
@SuppressWarnings("serial")
public class MotionEditor extends JPanel  implements MotionsListener, DocumentListener, ItemListener, ActionListener, LVListener, ConstituentListener {
	private static final int TITLE_LEN = 60;
	private static final int TEXT_LEN_ROWS = 10;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String TITLE_FORMAT = "TXT";
	public boolean SUBMIT = true;
	static final public JFileChooser fd = new JFileChooser();
	private static final boolean VISIBILITY_MOTION = false; 
	private static final boolean DISABLE_LABELS = true;
	private static final boolean DISABLING_MOTION_TITLE_LABEL = false;
	public static final int DIMY = 300;
	public JTextField motion_title_field;
	public JComboBox motion_answer_field;
	public DocumentEditor motion_body_field;
	public JButton motion_submit_field;
	public JButton motion_submit_anonymously_field;
	public JTextField date_field;
	public JButton dategen_field;
	public JButton load_field;
	public JButton setTxtMode;
	public JButton setHTMMode;
	public JCheckBox requested;
	public JCheckBox broadcasted;
	public JCheckBox blocked;
	public JComboBox category_field;
	public JTextField category_field_editor;
	JTabbedPane tabbedPane = new JTabbedPane();
	MotionsGIDItem[] combo_answerTo = new MotionsGIDItem[]{};
	private boolean enabled = false;
	private String motionID = null;
	private D_Motion motionEdited; 
	boolean m_editable = false;
	private Object signature_ID;
	private D_Vote signature;
	private D_Justification justification;
	JustificationEditor jEditor;
	VoteEditor vEditor;
	private LVComboBox scoring_options_field;
	JPanel panel_body = new JPanel(new FlowLayout(FlowLayout.LEFT));
	TranslatedLabel label_choices;
	TranslatedLabel label_category;
	TranslatedLabel label_date;
	TranslatedLabel label_motion_answer;
	TranslatedLabel label_motion_title;
	private D_Constituent constituentCurrent;
	private void disable_handling() {
		if(this.requested!=null) this.requested.setEnabled(false);
		if(this.broadcasted!=null) this.broadcasted.setEnabled(false);
		if(this.blocked!=null) this.blocked.setEnabled(false);
	}
	private void disable_it() {
		enabled  = false;
		if(this.motion_title_field!=null){
			this.motion_title_field.setEditable(false);
		}
		if(this.motion_body_field!=null){
			this.motion_body_field.setEnabled(false);
		}
		if(this.motion_answer_field!=null){
			this.motion_answer_field.setEnabled(false);
		}
		if(this.scoring_options_field!=null){
			this.scoring_options_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.scoring_options_field.setVisible(false);
		}
		if(this.category_field!=null){
			this.category_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.category_field.setVisible(false);
		}
		if(this.category_field_editor!=null){
			this.category_field_editor.setEnabled(false);
			if(VISIBILITY_MOTION)this.category_field_editor.setVisible(false);
		}
		if(this.date_field!=null){
			this.date_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.date_field.setVisible(false);
		}
		if(DISABLE_LABELS){
			if(this.label_choices!=null){
				this.label_choices.setEnabled(false);
				if(VISIBILITY_MOTION)this.label_choices.setVisible(false);
			}
			if(this.label_category!=null){
				this.label_category.setEnabled(false);
				if(VISIBILITY_MOTION)this.label_category.setVisible(false);
			}
			if(this.label_date!=null){
				this.label_date.setEnabled(false);
				if(VISIBILITY_MOTION)this.label_date.setVisible(false);
			}
			if(this.label_motion_answer!=null){
				this.label_motion_answer.setEnabled(false);
			}
			if(DISABLING_MOTION_TITLE_LABEL)
				if(this.label_motion_title!=null){
					this.label_motion_title.setEnabled(false);
					if(VISIBILITY_MOTION)this.label_motion_title.setVisible(false);
				}
		}
		if(this.motion_submit_field!=null){
			this.motion_submit_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.motion_submit_field.setVisible(false);
		}
		if(this.motion_submit_anonymously_field!=null){
			this.motion_submit_anonymously_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.motion_submit_anonymously_field.setVisible(false);
		}
		if(this.dategen_field!=null){
			this.dategen_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.dategen_field.setVisible(false);
		}
		if(this.load_field!=null){
			this.load_field.setEnabled(false);
			if(VISIBILITY_MOTION)this.load_field.setVisible(false);
		}
		if(this.setTxtMode!=null){
			this.setTxtMode.setEnabled(false);
			if(VISIBILITY_MOTION)this.setTxtMode.setVisible(false);
		}
		if(this.setHTMMode!=null){
			this.setHTMMode.setEnabled(false);
			if(VISIBILITY_MOTION)this.setHTMMode.setVisible(false);
		}
		vEditor.disable_it();
		jEditor.disable_it();
	}
	private void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("MotionEditor:Enabling");
		if(this.motion_title_field!=null){
			this.motion_title_field.setEditable(true);
		}
		if(this.motion_body_field!=null){
			this.motion_body_field.setEnabled(true);
		}
		if(this.motion_answer_field!=null){
			this.motion_answer_field.setEnabled(true);
		}
		if(this.category_field!=null){
			this.category_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.category_field.setVisible(true);
		}
		if(this.category_field_editor!=null){
			this.category_field_editor.setEnabled(true);
			if(VISIBILITY_MOTION)this.category_field_editor.setVisible(true);
		}
		if(this.date_field!=null){
			this.date_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.date_field.setVisible(true);
		}
		if(this.scoring_options_field!=null){
			this.scoring_options_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.scoring_options_field.setVisible(true);
		}
		if(DISABLE_LABELS){
			if(this.label_choices!=null){
				this.label_choices.setEnabled(true);
				if(VISIBILITY_MOTION)this.label_choices.setVisible(true);
			}
			if(this.label_category!=null){
				this.label_category.setEnabled(true);
				if(VISIBILITY_MOTION)this.label_category.setVisible(true);
			}
			if(this.label_date!=null){
				this.label_date.setEnabled(true);
				if(VISIBILITY_MOTION)this.label_date.setVisible(true);
			}
			if(this.label_motion_answer!=null){
				this.label_motion_answer.setEnabled(true);
			}
			if(DISABLING_MOTION_TITLE_LABEL)
				if(this.label_motion_title!=null){
					this.label_motion_title.setEnabled(true);
					if(VISIBILITY_MOTION)this.label_motion_title.setVisible(true);
				}
		}
		if(this.motion_submit_field!=null){
			this.motion_submit_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.motion_submit_field.setVisible(true);
		}
		if(this.motion_submit_anonymously_field!=null){
			this.motion_submit_anonymously_field.setEnabled(true);
			if (VISIBILITY_MOTION) this.motion_submit_anonymously_field.setVisible(true);
		}
		if(this.dategen_field!=null){
			this.dategen_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.dategen_field.setVisible(true);
		}
		if(this.load_field!=null){
			this.load_field.setEnabled(true);
			if(VISIBILITY_MOTION)this.load_field.setVisible(true);
		}
		if(this.setTxtMode!=null){
			this.setTxtMode.setEnabled(true);
			if(VISIBILITY_MOTION)this.setTxtMode.setVisible(true);
		}
		if(this.setHTMMode!=null){
			this.setHTMMode.setEnabled(true);
			if(VISIBILITY_MOTION)this.setHTMMode.setVisible(true);
		}
		if(this.requested!=null){
			this.requested.setEnabled(true);
		}
		if(this.broadcasted!=null){
			this.broadcasted.setEnabled(true);
		}
		if(this.blocked!=null){
			this.blocked.setEnabled(true);
		}
		vEditor.enable_it();
		if((signature!=null) && (signature.getJustificationLIDstr()!=null)) jEditor.enable_it();
		else jEditor.disable_it();
	}
	public MotionEditor () {
		init();
	}
	public MotionEditor (boolean _SUBMIT) {
		SUBMIT = _SUBMIT;
		init();
	}
	void _init() {
		new DDP2P_ServiceThread("MotionEditor: init", true) {
			public void _run(){
				init();
			}
		}.start();
	}
	void init() {
		if(DEBUG) System.out.println("MotionEditor: start");
		tabbedPane.setTabPlacement(JTabbedPane.TOP);
		ImageIcon icon = net.ddp2p.widgets.app.DDIcons.getMotImageIcon("General Motion");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_sig = net.ddp2p.widgets.app.DDIcons.getSigImageIcon("General Signature");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_jus = net.ddp2p.widgets.app.DDIcons.getJusImageIcon("General Justification");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_conf = net.ddp2p.widgets.app.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		int y[] = new int[]{0};
		JPanel enc;
		if(DEBUG) System.out.println("MotionEditor: icons");
		JComponent generalPane = makeGeneralPanel(y);
		enc = new JPanel(new FlowLayout(FlowLayout.LEFT));
		enc.add(generalPane);
		tabbedPane.addTab(__("Motion Body"), icon, enc, __("Generic fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_G);
		if(DEBUG) System.out.println("MotionEditor: body");
		jEditor = new JustificationEditor(JustificationEditor.ONLY);
		if(DEBUG) System.out.println("MotionEditor: justif");
		vEditor = new VoteEditor(this, jEditor, VoteEditor.CHOICE);
		jEditor.setVoteEditor(vEditor);
		if(DEBUG) System.out.println("MotionEditor: vote");
		enc = new JPanel(new FlowLayout(FlowLayout.LEFT));
		enc.add(vEditor);
		tabbedPane.addTab(__("Choice"), icon_sig,  enc, __("My Choice"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);
		if(DEBUG) System.out.println("MotionEditor: choice");
		enc = new JPanel(new FlowLayout(FlowLayout.LEFT));
		enc.add(jEditor);
		tabbedPane.addTab(__("Justification"), icon_jus, enc, __("Explain Motion"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);
		if(DEBUG) System.out.println("MotionEditor: motions");
		JPanel hp = new JPanel();
		hp.setLayout(new GridBagLayout()); y[0] = 0;
		JComponent handlingPane = makeHandlingPanel(hp, y);
		enc = new JPanel(new FlowLayout(FlowLayout.LEFT));
		enc.add(handlingPane);
		tabbedPane.addTab(__("Handling"), icon_conf, enc, __("Handling fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_H);
		if(DEBUG) System.out.println("MotionEditor: handling");
		this.setLayout(new BorderLayout());
		this.add(tabbedPane);
		disable_it();
		this.disable_handling();
		if(DEBUG) System.out.println("MotionEditor: done");
		SwingUtilities.invokeLater(new Runnable() {
			public void run(){
				fd.setFileFilter(new DocumentFilter());
				try{
					if(DEBUG)System.out.println("ControlPane:<init>: set Dir = "+Application.USER_CURRENT_DIR);
					File userdir = new File(Application.USER_CURRENT_DIR);
					if(DEBUG)System.out.println("ControlPane:<init>: set Dir FILE = "+userdir);
					fd.setCurrentDirectory(userdir);
				}catch(Exception e){if(_DEBUG)e.printStackTrace();}
			}
		});
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		return scrollPane;
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and load it with "data"
	 * @param _NEW_FORMAT
	 * @param data
	 */
	public void setMode(String _NEW_FORMAT, String data){
		if(DEBUG) System.out.println("MotionEditor:setMode: "+_NEW_FORMAT+" "+data);
		this.getMotion().getMotionText().setDocumentString(data);
		this.getMotion().getMotionText().setFormatString(_NEW_FORMAT);
		this.motion_body_field.removeListener(this);
		motion_body_field.getComponent().setVisible(false);
		this.motion_body_field.setType(getMotion().getMotionText().getFormatString());
		this.motion_body_field.setText(getMotion().getMotionText().getDocumentString());
		motion_body_field.getComponent().setVisible(true);
		this.motion_body_field.setEnabled(enabled);
		this.motion_body_field.addListener(this);
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and convert current data to it
	 * @param _NEW_FORMAT
	 */
	public void switchMode(String _NEW_FORMAT){
		String data = "";
		data = this.getMotion().getMotionText().convertTo(_NEW_FORMAT);
		if(data==null) return;
		setMode(_NEW_FORMAT, data);
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
		if(DEBUG) out.println("MotionEditor:setMotion: exit");
	}
	public static String findMyVote(String motionID, String constituentID) {
		if(motionID==null) return null;
		String sql = 
			"SELECT "+net.ddp2p.common.table.signature.signature_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.motion_ID+"=?" +
					" AND "+net.ddp2p.common.table.signature.constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> s = null;
		try {
			s = Application.getDB().select(sql, new String[]{motionID,constituentID});
		} catch (P2PDDSQLException e) {
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
		if(getMotion() == null){
			if(DEBUG) out.println("MotionEditor:editable: no just");
			return false;
		}
		if(getMotion().isEditable()){
			if(DEBUG) out.println("MotionEditor:editable");
			return true;
		}
		if(DEBUG) out.println("MotionEditor:editable: exit "+getMotion());
		return false;
	}
	/**
	 * Will reload org from database
	 * returns whether to edit
	 */
	private boolean reloadMotion(boolean force){
		if(DEBUG) out.println("MotionEditor:reloadMotion: start force="+force+" justID="+motionID);
		if (motionID == null) {
			setNullMotion();
			return false;
		}
		try {
			setMotion(D_Motion.getMotiByLID(motionID, true, false));
			if(DEBUG) out.println("MotionEditor:reloadMotion: got="+getMotion());
		} catch (Exception e) {
			e.printStackTrace();
			disable_it();
			return false;
		}
		if (!editable()){
			if(DEBUG) out.println("MotionEditor:reloadMotion: quit not editable: f="+force);
			return force || DD.EDIT_RELEASED_JUST;
		}
		if(DEBUG) out.println("MotionEditor:reloadMotion: exit");
		return true;
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
	@SuppressWarnings("unchecked")
	private boolean update_it(boolean force) {
		boolean toEnable = false;
		if(DEBUG) out.println("MotionEditor:updateit: start");
		if (reloadMotion(force)) {
			if(DEBUG) out.println("MotionEditor:updateit: enable");
			disable_it();
			toEnable = true;
		} else {
			if (DEBUG) out.println("MotionEditor:updateit: disable");
			disable_it();
		}
		if (getMotion() == null) {
			if(DEBUG) out.println("MotionEditor:updateit: quit null just");
			return false;
		}
		this.setSignatureAndJustificationForConstituentAndMotion();
		m_editable = editable(); 
		return update_motion_GUI_fields(toEnable);
	}
	/**
	 * Used to load a motion in the GUI
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean update_motion_GUI_fields(boolean toEnable) {
		requested.removeItemListener(this);
		requested.setSelected(getMotion().isRequested());
		requested.addItemListener(this);
		blocked.removeItemListener(this);
		blocked.setSelected(getMotion().isBlocked());
		blocked.addItemListener(this);
		broadcasted.removeItemListener(this);
		broadcasted.setSelected(getMotion().isBroadcasted());
		broadcasted.addItemListener(this);
		date_field.getDocument().removeDocumentListener(this);
		date_field.setText(Encoder.getGeneralizedTime(getMotion().getCreationDate()));
		date_field.getDocument().addDocumentListener(this);
		this.scoring_options_field.removeLVListener(this);
		this.scoring_options_field.setArrayValue(D_MotionChoice.getNames(getMotion().getChoices()));
		this.scoring_options_field.addLVListener(this);
		ArrayList<MotionsGIDItem> j = D_Motion.getAnswerToChoice(getMotion().getOrganizationLIDstr()); 
		combo_answerTo = new MotionsGIDItem[j.size()+1];
		int k = 0;
		combo_answerTo[k++] = new MotionsGIDItem(null, null, __("None"));
		for (MotionsGIDItem _j :j){
			String gid = Util.getString(_j.gid);
			String id = Util.getString(_j.id);
			String name = Util.getString(_j.name);
			combo_answerTo[k++] = new MotionsGIDItem(gid, id, name);
		}
		if (motion_answer_field != null) {
			motion_answer_field.removeItemListener(this);
			motion_answer_field.removeAllItems();
		}
		MotionsGIDItem sel = null;
		if (motion_answer_field != null) {
			for (MotionsGIDItem i : combo_answerTo) {
				motion_answer_field.addItem(i);
				if (i.id == null) continue;
				if (i.id.equals(getMotion().getEnhancedMotionLIDstr())){ sel = i;}
			}
			if (sel != null) motion_answer_field.setSelectedItem(sel);
			motion_answer_field.addItemListener(this);
		}
		try {
			if (DEBUG) System.out.println("MotionEditor: update_it: will set="+getMotion().getMotionText().getFormatString());
			this.motion_body_field.removeListener(this);
			motion_body_field.getComponent().setVisible(false);
			this.motion_body_field.setType(getMotion().getMotionText().getFormatString()); 
			this.motion_body_field.setText(getMotion().getMotionText().getDocumentString());
			motion_body_field.getComponent().setVisible(true);
			this.motion_body_field.setEnabled(enabled);
			this.motion_body_field.addListener(this);
			if(DEBUG) System.out.println("MotionEditor: update_it: did set="+getMotion().getMotionText().getFormatString());
		} catch(Exception e) {
			e.printStackTrace();
		}
		this.motion_title_field.getDocument().removeDocumentListener(this);
		this.motion_title_field.setText(getMotion().getMotionTitle().title_document.getDocumentString());
		this.motion_title_field.getDocument().addDocumentListener(this);
		category_field.removeItemListener(this);
		category_field_editor.getDocument().removeDocumentListener(this);
		category_field.removeAllItems(); category_field.getEditor().setItem("");
		String g_category_others = __("Others");
		boolean g_category_others_added = false;
		String category = getMotion().getCategory();
		if (true) { 
			ArrayList<Object> c = D_Motion.getCategories(getMotion().getOrganizationLIDstr());
			for (Object i: c) {
				String crt = Util.getString(i);
				if (g_category_others.equals(crt)) g_category_others_added = true;
				category_field.addItem(crt);
				if ((category != null) && (category.equals(crt))) {
					category_field.setSelectedItem(crt);
				}
			}
		} else {
			if (category != null) {category_field.addItem(category); category_field.setSelectedItem(category);}
		}
		if (! g_category_others_added) category_field.addItem(g_category_others);
		category_field.addItemListener(this);
		category_field_editor.getDocument().addDocumentListener(this);
		if (toEnable) enable_it();
		return true;		
	}
	/**
	 * Finds any prior vote or signature for this constituent and motion, and populates with them the
	 * corresponding editors. If none, then sets empty objects.
	 */
	private void setSignatureAndJustificationForConstituentAndMotion() {
		Calendar creation_date = Util.CalendargetInstance();
		String constituent_ID = Util.getStringID(this.getConstituentIDMyself());
		String constituent_GID = this.getConstituentGIDMyself();
		if (motionID == null) return;
		signature_ID = findMyVote(motionID, constituent_ID);
		try {
			long _s_ID = Util.lval(signature_ID,-1);
			if (_s_ID > 0) {
				signature = new D_Vote(_s_ID);
				signature.setMotionObjOnly(getMotion()); 
				long _jID = Util.lval(signature.getJustificationLIDstr(),-1);
				if ((_jID > 0) && (signature.getJustificationFromObjOrLID() == null)) {
					justification = signature.setJustification(D_Justification.getJustByLID(_jID, true, false));
					jEditor.setJustificationAndMotionEditor(justification, false, this);
					vEditor.setSignature(signature, this, justification);
				} else {
					vEditor.setSignature(signature, this, null);
					D_Justification _just = D_Justification.getEmpty();
					_just.setMotionAndOrganizationAll(getMotion());
					_just.setOrganizationLIDstr(this.getMotion().getOrganizationLIDstr());
					_just.setOrgGID(this.getMotion().getOrganizationGID_force());
					_just.setCreationDate(creation_date);
					_just.setTemporary(true);
					jEditor.setJustificationAndMotionEditor(_just, false, this);					
				}
			} else {
				D_Vote _sign = new D_Vote();
				_sign.setMotionObjOnly(getMotion());
				_sign.setChoice(getMotion().getDefaultChoice());
				_sign.setMotionLID(getMotion().getLIDstr());
				_sign.setMotionGID(getMotion().getGID());
				_sign.setJustificationLID(null);
				_sign.setConstituentLID(constituent_ID);
				_sign.setConstituentGID(constituent_GID);
				_sign.setOrganizationLID(this.getMotion().getOrganizationLIDstr());
				_sign.setOrganizationGID(this.getMotion().getOrganizationGID_force());
				_sign.setCreationDate(creation_date);
				vEditor.setSignature(_sign, this, null);
				signature = _sign;
				D_Justification _just = D_Justification.getEmpty();
				_just.setMotionObj(getMotion());
				_just.setMotionLIDstr(getMotion().getLIDstr());
				_just.setMotionGID(getMotion().getGID());
				_just.setOrganizationLIDstr(this.getMotion().getOrganizationLIDstr());
				_just.setOrgGID(this.getMotion().getOrganizationGID_force());
				_just.setCreationDate(creation_date);
				_just.setTemporary(true);
				jEditor.setJustificationAndMotionEditor(_just, false, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    @SuppressWarnings("unchecked")
    private JComponent makeGeneralPanel(int _y[]) {
     	JPanel p1=new JPanel(), p2=new JPanel();
     	JPanel fill = new JPanel(new FlowLayout(FlowLayout.LEFT));
     	fill.add(p2);
    	JSplitPane sp= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, p1, fill);
    	p2.setLayout(new GridBagLayout());
    	GridBagConstraints t = new GridBagConstraints();
		t.fill = GridBagConstraints.NONE;
		t.gridx = 0; t.gridy = 0;
		t.anchor = GridBagConstraints.WEST;
    	int y = _y[0];
    	GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		JPanel p_t = new JPanel();
		label_motion_title = new TranslatedLabel("Title");
		p_t.add(label_motion_title);
		p_t.add(motion_title_field = new JTextField(TITLE_LEN));
		motion_title_field.getDocument().addDocumentListener(this);
		p2.add(p_t,t);
		JPanel p_a = new JPanel();
		label_motion_answer = new TranslatedLabel("Enhancement Of");
		p_a.add(label_motion_answer);
		p_a.add(motion_answer_field = new JComboBox(combo_answerTo));
		motion_answer_field.addItemListener(this);
		t.gridx = 0; t.gridy = 1;
		t.anchor = GridBagConstraints.WEST;
		p2.add(p_a,t);
		motion_body_field = new DocumentEditor();
		motion_body_field.name = "Motion Editor";
		motion_body_field.init(TEXT_LEN_ROWS);
		motion_body_field.addListener(this);
		t.gridx = 0; t.gridy = 2;
		t.anchor = GridBagConstraints.WEST;
		t.fill = GridBagConstraints.BOTH;
		p2.add(panel_body,t); 
		motion_body_field.getComponent(D_Document.RTEDIT).setVisible(false);
		motion_body_field.getComponent(D_Document.TEXTAREA).setVisible(false);
		motion_body_field.getComponent(D_Document.PDFVIEW).setVisible(false);
		panel_body.add(motion_body_field.getComponent(D_Document.TEXTAREA));
		panel_body.add(motion_body_field.getComponent(D_Document.RTEDIT));
		panel_body.add(motion_body_field.getComponent(D_Document.PDFVIEW));
		motion_body_field.setType(D_Document.DEFAULT_FORMAT);
		motion_body_field.getComponent(D_Document.DEFAULT_EDITOR).setVisible(true);
       	JPanel p=new JPanel();
       	p.setLayout(new GridBagLayout());
    	p1.setLayout(new BorderLayout());
    	p1.add(p,BorderLayout.NORTH);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		label_choices = new TranslatedLabel("Default Choices");
		p.add(label_choices, c);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		scoring_options_field = new LVComboBox();
		p.add(scoring_options_field,c);
		scoring_options_field.addLVListener(this);
		c.fill = GridBagConstraints.NONE;
		if(false){
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = y++;		
		label_motion_answer = new TranslatedLabel("Answer To");
		p.add(label_motion_answer, c);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(motion_answer_field = new JComboBox(combo_answerTo),c);
		motion_answer_field.addItemListener(this);
		}
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		label_category = new TranslatedLabel("Category");
		p.add(label_category, c);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(category_field = new JComboBox(new String[]{}),c);
		category_field.addItemListener(this);
		category_field_editor = ((JTextField)category_field.getEditor().getEditorComponent());
		category_field_editor.getDocument().addDocumentListener(this);
		category_field.setEditable(true);
		c.fill = GridBagConstraints.NONE;
		String creation_date = Util.getGeneralizedTime();
		date_field = new JTextField(creation_date);
		date_field.setColumns(creation_date.length());
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		date_field.getDocument().addDocumentListener(this);
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		p.add(dategen_field = new JButton(__("Set Current Date")),c);
		dategen_field.addActionListener(this);
		if (SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++; 
			c.anchor = GridBagConstraints.EAST;
			p.add(motion_submit_field = new JButton(__("Submit Motion")),c);
			motion_submit_field.addActionListener(this);
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++; 
			c.anchor = GridBagConstraints.EAST;
			p.add(motion_submit_anonymously_field = new JButton(__("Submit Anonymously")),c);
			motion_submit_anonymously_field.addActionListener(this);
		}
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		p.add(load_field = new JButton(__("Load PDF/HTM/TXT")),c);
		load_field.addActionListener(this);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		p.add(this.setTxtMode = new JButton(__("Set TXT Mode")),c);
		setTxtMode.addActionListener(this);
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		p.add(this.setHTMMode = new JButton(__("Set HTML Mode")),c);
		setHTMMode.addActionListener(this);
		c.gridx = 0; c.gridy = y++;	 
		c.fill = GridBagConstraints.BOTH; 
		_y[0]=y;
    	return sp;
    }
    @SuppressWarnings("unchecked")
	private JPanel _makeGeneralPanel(int _y[]) {
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout()); _y[0] = 0;
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
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		date_field.getDocument().addDocumentListener(this);
		c.gridx = 0; c.gridy = y++;		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(dategen_field = new JButton(__("Set Current Date")),c);
		dategen_field.addActionListener(this);
		if(SUBMIT) {
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0; c.gridy = y++;		
			c.gridx = 1;
			c.anchor = GridBagConstraints.WEST;
			p.add(motion_submit_field = new JButton(__("Submit Motion")),c);
			motion_submit_field.addActionListener(this);
			p.add(motion_submit_anonymously_field = new JButton(__("Submit Anonymous")),c);
			motion_submit_anonymously_field.addActionListener(this);
		}
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0; c.gridy = y-1;		
		TranslatedLabel label_just_body = new TranslatedLabel("Motion");
		p.add(label_just_body, c);
		c.gridx = 0; c.gridy = y++;	 c.gridwidth=2; c.fill = GridBagConstraints.BOTH; 
		motion_body_field = new DocumentEditor();
		motion_body_field.init(TEXT_LEN_ROWS);
		motion_body_field.addListener(this);
		p.add(panel_body, c);
		panel_body.add(motion_body_field.getComponent());
		_y[0]=y;
		return p;
	}
	private JComponent makeHandlingPanel(JPanel p, int _y[]) {
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;
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
		_y[0]=y;
		return p;
	}
	@Override
	public void constituentUpdate(D_Constituent c, boolean me, boolean selected) {
		if (c != null && constituentCurrent != null)
			if (c.getLID() == constituentCurrent.getLID()) return;
		this.constituentCurrent = c;
		setSignatureAndJustificationForConstituentAndMotion();
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if (DEBUG) System.out.println("MotionEditor: motion_update: col: "+col+" motID="+motID);
		if (motID == null) {
			if (_DEBUG) System.out.println("MotionEditor: motion_update: null motion col: "+col+" motID="+motID+" ->"+d_motion);
		}
		this.setMotion(motID, false);
	}
	@Override
	public void motion_forceEdit(String motID) {
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
			D_Motion _m = D_Motion.getMotiByLID(motionID, true, true);
			_m.setBroadcasted(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			D_Motion _m = D_Motion.getMotiByLID(motionID, true, true);
			_m.setBlocked(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			D_Motion _m = D_Motion.getMotiByLID(motionID, true, true);
			_m.setRequested(val);
			_m.storeRequest();
			_m.releaseReference();
		}
		if(!enabled) return;
		String creationTime = date_field.getText();
		if(this.scoring_options_field == source) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: default scoring"); 
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final options!");
				return;
			}
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			this.getMotion().setChoices(D_MotionChoice.getChoices(this.scoring_options_field.getVal()));
			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			this.vEditor.setSignature(vEditor.signature, this, null);
			return;						
		}
		if ((this.motion_body_field == source) || (this.motion_body_field.getDocumentSource() == source)) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: just body");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final body! "+this.getMotion().getTitleOrMy());
				return;
			}
			String new_text = this.motion_body_field.getText();
			String old_text = this.getMotion().getMotionText().getDocumentString();
			String editor_format = this.motion_body_field.getFormatString();
			String old_old_text = this.getMotion().getMotionText().getFormatString();
			if (Util.equalStrings_null_or_not(new_text, old_text)
					&& Util.equalStrings_null_or_not(editor_format, old_old_text)) {
				return;
			}
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			this.getMotion().setMotionText(new_text, editor_format);
			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			return;			
		}
		if(this.setTxtMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setText");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final text mode!");
				return;
			}
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			switchMode(D_Document.TXT_BODY_FORMAT);
			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");
		}
		if(this.setHTMMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setHTM");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final html mode!");
				return;
			}
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			switchMode(D_Document.HTM_BODY_FORMAT);
			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");
		}
		if (this.load_field == source) {
			if(DEBUG)System.err.println("ControlPane:actionImport: import file");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final import!");
				return;
			}
			int returnVal = fd.showOpenDialog(this);
			if(DEBUG)System.err.println("ControlPane:actionImport: Got: selected");
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	        	if(DEBUG)System.err.println("ControlPane:actionImport: Got: ok");
	            File file = fd.getSelectedFile();
	            if(!file.exists()){
	            	Application_GUI.warning(__("The file does not exists: "+file),__("Importing Address")); return;
	            }
	            String ext = Util.getExtension(file);
	            if(ext!=null) ext = ext.toLowerCase();
	            try {
	            	if ("pdf".equals(ext)) {
	            		if (DEBUG) System.err.println("ControlPane:actionImport: Got: pdf");
	            		try {
	            			InputStream in = new FileInputStream(file); 
	            			if (file.length() > DocumentEditor.MAX_PDF) {
	            				if (_DEBUG) System.out.println("MotionEditor: getText: bin size="+file.length()+" vs "+DocumentEditor.MAX_PDF);
	            				Application_GUI.warning(__("File too large! Current Limit:"+" "+file.length()+"/"+DocumentEditor.MAX_PDF),
	            						__("Document too large for import!"));
	            				return;
	            			}
	            			byte bin[] = new byte[(int)file.length()];
	            			int off = 0;
	            			do {
	            				int cnt = in.read(bin, off, bin.length-off);
	            				if (cnt == -1) {
	            					if(_DEBUG) System.out.println("MotionEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            					break;
	            				}
	            				off += cnt;
            					if(_DEBUG) System.out.println("MotionEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            			} while(off < bin.length);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: bin size="+bin.length);
	            			String data = Util.stringSignatureFromByte(bin);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: txt size="+data.length());
	            			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
	            			setMode(D_Document.PDF_BODY_FORMAT, data);
	            			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
	            			this.getMotion().setEditable();
	            			this.getMotion().storeRequest();
	            			this.getMotion().releaseReference();
	            			if(DEBUG) System.out.println("DocumentEditor: handle: done");
	            		} catch (FileNotFoundException e) {
	            			e.printStackTrace();
	            		} catch (IOException e) {
	            			e.printStackTrace();
	            		}
	            	}else
	            		if (("html".equals(ext)) || ("htm".equals(ext))) {
	            			if (_DEBUG) System.err.println("ControlPane:actionImport: Got: html: implement!");
	            			try {
	            				BufferedReader bri = new BufferedReader(new FileReader(file));
								String data = Util.readAll(bri);
		            			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
								setMode(D_Document.HTM_BODY_FORMAT, data);
		            			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
		            			this.getMotion().setEditable();
		            			this.getMotion().storeRequest();
		            			this.getMotion().releaseReference();
								if(DEBUG) System.out.println("DocumentEditor: handle: done");
	            			} catch(Exception e) {
	            				e.printStackTrace();
	            			}
	            		} else
	            			if(("txt".equals(ext))){
	            				try{
		            				BufferedReader bri = new BufferedReader(new FileReader(file));
									String data = Util.readAll(bri);
			            			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
									setMode(D_Document.TXT_BODY_FORMAT, data);
			            			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			            			this.getMotion().setEditable();
			            			this.getMotion().storeRequest();
			            			this.getMotion().releaseReference();
			            			if(DEBUG) System.out.println("DocumentEditor: handle: done");
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
		if ((this.motion_title_field==source)||(this.motion_title_field.getDocument() == source)) {
			if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final title!");
				return;
			}
			String new_text = this.motion_title_field.getText();
			if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: "+new_text);
			if (getMotion() == null) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: had null motion changing title_field");
			}
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: "+getMotion());
			if (getMotion() != null) {
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u1");
				this.getMotion().getMotionTitle().title_document.setDocumentString(new_text);
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u2");
				this.getMotion().getMotionTitle().title_document.setFormatString(TITLE_FORMAT);
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u3");
				this.getMotion().setCreationDate(Util.getCalendar(creationTime));
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u4");
				this.getMotion().setEditable();
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u5");
				if (this.getMotion().dirty_any()) this.getMotion().storeRequest();
				if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u6");
				this.getMotion().releaseReference();
			} else {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: got null motion changing title_field");
			}
			if (DEBUG) out.println("MotionEditor:handleFieldEvent: motion title got: u3");
			return;			
		}
		if ((this.date_field==source)||(this.date_field.getDocument()==source)) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: date title");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final date!");
				return;
			}
			String new_text = this.date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			this.getMotion().setCreationDate(cal);
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			return;			
		}
		if(this.dategen_field==source) {
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final dategen!");
				return;
			}
			Calendar now = Util.CalendargetInstance();
			this.date_field.setText(Encoder.getGeneralizedTime(now));
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			this.getMotion().setCreationDate(now);
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			return;			
		}
		if(motion_answer_field==source) {
			if(DEBUG) out.println("MotionEditor:handleFieldEvent: creator");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final answer!");
				return;
			}
			if(DEBUG) Util.printCallPath("Linux tracing");
			MotionsGIDItem selected = (MotionsGIDItem) motion_answer_field.getSelectedItem();
			String id = null;
			try {
				if(selected == null){
					id = null;
				}else{
					if(DEBUG) out.println("MotionEditor:handleFieldEvent: selected motion answer="+selected.toStringDump());
					if(selected.id == null) id = null;
					else id = ""+(new Long(selected.id).longValue());
				}
				this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
				getMotion().setEnhancedMotionLIDstr(id);
				this.getMotion().setCreationDate(Util.getCalendar(creationTime));
				this.getMotion().setEditable();
				this.getMotion().storeRequest();
				this.getMotion().releaseReference();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		if((category_field==source) || (category_field_editor==source)||(category_field_editor.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: category");
			if (! this.getMotion().isTemporary()) {
				if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final category!");
				return;
			}
			String new_category = Util.getString(category_field.getSelectedItem());
			if((new_category!=null) && "".equals(new_category.trim())) new_category = null;
			this.setMotion(D_Motion.getMotiByMoti_Keep(getMotion()));
			this.getMotion().setCategory(new_category);
			this.getMotion().setCreationDate(Util.getCalendar(creationTime));
			this.getMotion().setEditable();
			this.getMotion().storeRequest();
			this.getMotion().releaseReference();
			return;
		}
		if ((motion_submit_anonymously_field == source)) {
			generateMotionAndVoteAndSignature();
		}
		if ((motion_submit_field == source)) {
			generateSignedMotionAndVoteAndSignature();
		}
		if(DEBUG) out.println("MotionEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	public void generateSignedMotionAndVoteAndSignature() {
		D_Motion crt_motion = this.getMotion();
		if (! crt_motion.isTemporary()) {
			if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final subm!");
			return;
		}
		D_Constituent c_myself = GUI_Swing.constituents.tree.getModel().getConstituentMyself();
		String c_myself_GID = GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
		long c_myself_LID = GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
		try {
			long j_id = -1;
			if (crt_motion.getConstituentGID() == null) {
				crt_motion.setConstituentGID(c_myself_GID);
				crt_motion.setConstituentLID(c_myself_LID);
				if (crt_motion.getConstituentGID() == null) {
					crt_motion.storeRequest();
					crt_motion.releaseReference();
					Application_GUI.warning(__("You should first select a constituent identity for this organization!"), __("No Constituent ID for this org!"));
					return;
				}
			}
			crt_motion._setGID(crt_motion.make_ID());
			D_Motion m = D_Motion.getMotiByGID(crt_motion.getGID(), true, true, true, null, crt_motion.getOrganizationLID(), crt_motion);
			if (m != crt_motion) {
				m.loadRemote(getMotion(), null, null, null);						
				this.setMotion(m);
				crt_motion = this.getMotion();
			}
			crt_motion.setTemporary(false);				
			crt_motion.setBroadcasted(true); 
			if (DEBUG) System.out.println("\nMotionEditor: handleFieldEvent: submitting: signing"+crt_motion);
			crt_motion.sign();
			crt_motion.setArrivalDate();
			if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: signed:"+crt_motion);
			long m_id = crt_motion.storeRequest_getID();
			crt_motion.releaseReference();
			net.ddp2p.common.hds.ClientSync.addToPayloadAdvertisements(crt_motion.getGID(), D_Organization.getOrgGIDHashGuess(crt_motion.getOrganizationGID_force()), null, net.ddp2p.common.streaming.RequestData.MOTI);
			if (m_id <= 0) {
				Util.printCallPath("Why Error saving motion!"); 
				return;
			}
			if(vEditor.vote_newjust_field.isSelected()) {
				this.justification = jEditor.justificationEdited;
				this.justification.setMotionGID(crt_motion.getGID());
				this.justification.setMotionLID(crt_motion.getLID());
				this.justification.setConstituentGID(crt_motion.getConstituentGID());
				this.justification.setConstituentLIDstr_Dirty(crt_motion.getConstituentLIDstr());
				this.justification.setOrgGID(crt_motion.getOrganizationGID_force());
				this.justification.setOrganizationLIDstr(crt_motion.getOrganizationLIDstr());
				this.justification.setGID();
				justification.sign();
				D_Justification j = D_Justification.getJustByGID(justification.getGID(), true, true, true, null, justification.getOrganizationLID(), justification.getMotionLID(), justification);
				if (j != justification) {
					if (! j.loadRemote(justification, null, null, null)) {
						if (DEBUG) out.println("MotionEditor:handleFieldEvent: subm: just: set arrival");
						j.setArrivalDate();
					}
				} else {
					j.setTemporary(false);
					j.setArrivalDate();
				}
				j_id = j.storeRequest_getID();
				j.releaseReference();
				if (j_id <= 0) {
					Util.printCallPath("Why Error saving justification!"); 
					return;					
				}
			}
			D_Vote _signature = vEditor.signature;
			_signature.setMotionGID(crt_motion.getGID());
			_signature.setMotionLID(Util.getStringID(m_id));
			_signature.setConstituentGID(crt_motion.getConstituentGID());
			_signature.setConstituentLID(crt_motion.getConstituentLIDstr());
			_signature.setOrganizationGID(crt_motion.getOrganizationGID_force());
			_signature.setOrganizationLID(crt_motion.getOrganizationLIDstr());
			if(vEditor.vote_newjust_field.isSelected()) {
				_signature.setJustificationGID(justification.getGID());
				_signature.setJustificationLID(Util.getStringID(j_id));
			}
			_signature.setGID();
			_signature.sign();
			long v_id = _signature.storeVerified();
			signature = _signature;
			if (v_id <= 0) {
				Util.printCallPath("Why Error saving vote!"); 
				return;
			}
			disable_it();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		crt_motion.resetCache();
	}
	public void generateMotionAndVoteAndSignature() {
		D_Motion crt_motion = this.getMotion();
		if (! crt_motion.isTemporary()) {
			if (_DEBUG) out.println("MotionEditor:handleFieldEvent: abandon modifying final submit anonym!");
			return;
		}
		D_Constituent c_myself = GUI_Swing.constituents.tree.getModel().getConstituentMyself();
		String c_myself_GID = GUI_Swing.constituents.tree.getModel().getConstituentGIDMyself();
		long c_myself_LID = GUI_Swing.constituents.tree.getModel().getConstituentIDMyself();
		if (c_myself_GID == null) {
			Application_GUI.warning(__("You should first select a constituent identity for this organization!"), __("No Constituent ID for this org!"));
					return;
		}
		try {
			long j_id = -1;
			crt_motion.setConstituentGID(null);
			crt_motion._setGID(crt_motion.make_ID());				
			D_Motion m = D_Motion.getMotiByGID(crt_motion.getGID(), true, true, true, null, crt_motion.getOrganizationLID(), crt_motion);
			if (m != crt_motion) {
				m.loadRemote(getMotion(), null, null, null);
				this.setMotion(m);
				crt_motion = this.getMotion();
			}
			crt_motion.setTemporary(false);
			crt_motion.setBroadcasted(true); 
			if (DEBUG) System.out.println("\nMotionEditor: handleFieldEvent: submitting: signing"+crt_motion);
			crt_motion.setArrivalDate();
			if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: signed:"+crt_motion);
			long m_id = crt_motion.storeRequest_getID();
			crt_motion.releaseReference();
			net.ddp2p.common.hds.ClientSync.addToPayloadAdvertisements(crt_motion.getGID(), D_Organization.getOrgGIDHashGuess(crt_motion.getOrganizationGID_force()), null, net.ddp2p.common.streaming.RequestData.MOTI);
			if (m_id <= 0) {
				Util.printCallPath("Why Error saving motion!"); 
				return;
			}
			if (vEditor.vote_newjust_field.isSelected()) {
				this.justification = jEditor.justificationEdited;
				if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: crt just = "+this.justification);
				this.justification.setMotionGID(crt_motion.getGID());
				this.justification.setMotionLID(crt_motion.getLID()); 
				this.justification.setConstituentGID(crt_motion.getConstituentGID());
				this.justification.setConstituentLIDstr_Dirty(crt_motion.getConstituentLIDstr());
				this.justification.setOrgGID(crt_motion.getOrganizationGID_force());
				this.justification.setOrganizationLIDstr(crt_motion.getOrganizationLIDstr());
				this.justification.setGID();
				D_Justification j = D_Justification.getJustByGID(justification.getGID(), true, true, true, null, justification.getOrganizationLID(), justification.getMotionLID(), justification);
				if (j != justification) {
					if (_DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: just new allocation: "+j);
					if (! j.loadRemote(justification, null, null, null))
						j.setArrivalDate();
					if (_DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: just: set arrival");
					jEditor.justificationEdited = justification = j;
				} else {
					this.justification.setTemporary(false);
					this.justification.setArrivalDate();
				}
				j_id = j.storeRequest_getID();
				j.releaseReference();
				if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: final just = " + j);
				if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: final _just = " + justification);
				if (j_id <= 0) {
					Util.printCallPath("Why Error saving justification!"); 
					return;					
				}
			}
			D_Vote _signature = vEditor.signature;
			_signature.setMotionGID(crt_motion.getGID());
			_signature.setMotionLID(Util.getStringID(m_id));
			_signature.setConstituentGID(c_myself_GID); 
			_signature.setConstituentLID(Util.getStringID(c_myself_LID));
			_signature.setConstituentObjOnly(c_myself);
			_signature.setOrganizationGID(crt_motion.getOrganizationGID_force());
			_signature.setOrganizationLID(crt_motion.getOrganizationLIDstr());
			if (vEditor.vote_newjust_field.isSelected()) {
				_signature.setJustificationGID(justification.getGID());
				_signature.setJustificationLID(Util.getStringID(j_id));
			}
			_signature.setGID();
			_signature.sign();
			long v_id = _signature.storeVerified();
			if (DEBUG) System.out.println("MotionEditor: handleFieldEvent: submitting: final sign = " + _signature);
			signature = _signature;
			if (v_id <= 0) {
				Util.printCallPath("Why Error saving vote!"); 
				return;
			}
			disable_it();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		crt_motion.resetCache();
	}
	public void enableNewJustification(boolean b) {
		if (b) {
			jEditor.make_new(getMotion());
			jEditor.enable_just();
		} else jEditor.disable_just();
	}
	public String getNewJustificationID() {
		return jEditor.justificationEdited.getLIDstr();
	}
	/**
	 * Lets justificator pane communicate crt jID to the vote pane
	 * @param jID
	 */
	public void setNewJustificationID(String jID) {
		if (jID.equals(vEditor.signature.getJustificationLIDstr())) return;
		vEditor.setNewJustificationID(jID);
	}
	@Override
	public void listenLV(Object source) {
		this.handleFieldEvent(source);
	}
	public D_Justification getNewJustification() {
		return jEditor.justificationEdited;
	}
	public D_Motion getMotion() {
		return motionEdited;
	}
	public void setMotion(D_Motion moti) {
		if (moti == null) Util.printCallPath("");
		this.motionEdited = moti;
	}
	public void setNullMotion() {
		this.motionEdited = null;
	}
}

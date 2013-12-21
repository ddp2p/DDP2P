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
package widgets.org;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import ASN1.Encoder;

import ciphersuits.Cipher;
import ciphersuits.SK;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_OrgConcepts;
import data.D_Organization;

import streaming.OrgHandling;
import util.Util;
import widgets.components.LVComboBox;
import widgets.components.LVListener;
import widgets.components.MultiformatDocumentEditor;
import widgets.components.MultiformatDocumentListener;
import widgets.components.TranslatedLabel;
import static java.lang.System.out;
import static util.Util._;
class OrgGIDItem{
	String gid;
	String hash;
	String name;
	public OrgGIDItem(String _gid, String _hash, String _name) {set(_gid, _hash, _name);}
	public void set(String _gid, String _hash, String _name){
		gid = _gid;
		hash = _hash;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name;
		if(hash == null) return "ORG:"+super.toString();
		return "ORG:"+hash;
	}
}

@SuppressWarnings("serial")
public class OrgEditor  extends JPanel implements OrgListener, ActionListener, FocusListener, DocumentListener, LVListener, ItemListener, MultiformatDocumentListener {
	//private static final boolean _DEBUG = true;
	private static final String GRASSROOT = _("Grass-Root");
	private static final String AUTHORITARIAN = _("Authoritarian");
	private static final String EXPRESSION = _("Expression");
	private static String []g_methods=new String[]{GRASSROOT,AUTHORITARIAN,EXPRESSION};
	private static final boolean DEBUG = false; //false;
	private static final boolean _DEBUG = true;
	/* JTextArea */ 
	MultiformatDocumentEditor instructionsMotionsPane, instructionsRegistrationsPane, descriptionPane;
	JPanel preapprovedPane;
	
	JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP,JTabbedPane.WRAP_TAB_LAYOUT);
	String orgID;
	JTextField name_field, category_field_editor, date_field;
	JComboBox method_field, creator_field, globID_field, category_field;
	JButton keygen_field;
	JButton commit_field;
	JButton dategen_field;
	int TEXT_LEN = 20;
	int TEXT_LEN_ROWS = 10;
	int TEXT_LEN_COLS = 30;
	table.HashOrg hash_org = new table.HashOrg(); //The hash of the org data (for signature or GID)
	
	ArrayList<Object> org; // data about the current organization
	String sql_org = "SELECT "+table.organization.org_list+
		" FROM "+table.organization.TNAME+
		" WHERE "+table.organization.organization_ID+"=?;";
	boolean m_editable = false;
	private boolean m_setting_Date = false;
	private OrgExtra extraFields;
	private JScrollPane extraFieldsPane;
	private LVComboBox default_scoring_options_field;
	private JTextField name_org_field;
	private JTextField name_forum_field;
	private JTextField name_motion_field;
	private JTextField name_justification_field;
	private JTextArea preapproved;
	private LVComboBox languages_field;
	private boolean enabled;
	JCheckBox requested;
	JCheckBox broadcasted;
	JCheckBox blocked;
	private D_Organization organization;
	private JCheckBox broadcast_rule;

	public OrgEditor() {
		tabbedPane.setTabPlacement(JTabbedPane.TOP);
	//	tabbedPane.setLayout(new BorderLayout());
	    tabbedPane.setAutoscrolls(true);
		ImageIcon icon = config.DDIcons.getOrgImageIcon("General Org"); //Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_conf = config.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_register = config.DDIcons.getRegistrationImageIcon("Register");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
		ImageIcon icon_preapproved = config.DDIcons.getConImageIcon("Preapproved Constituents");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");

		JComponent generalPane = makeGeneralPanel();
		tabbedPane.addTab(_("General Org"), icon, generalPane, _("Generic fields"));
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_O);

		JComponent languagePane = makeLanguagePanel();
		tabbedPane.addTab(_("Language Org"), icon, languagePane, _("Language fields"));
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_L);
		
		descriptionPane = makeDescriptionPanel();
		tabbedPane.addTab(_("Description"), icon, new JScrollPane(descriptionPane), _("Description"));
		tabbedPane.setMnemonicAt(2, KeyEvent.VK_D);
		
		instructionsMotionsPane = makeInstructionsMotionsPanel();
		tabbedPane.addTab(_("Instructions Motions"), icon, new JScrollPane(instructionsMotionsPane), _("Instructions Motions"));
		tabbedPane.setMnemonicAt(2, KeyEvent.VK_M);
		
		instructionsRegistrationsPane = makeInstructionsRegistrationsPanel();
		tabbedPane.addTab(_("Instructions Registrations"), icon_register, new JScrollPane(instructionsRegistrationsPane), _("Instructions Registrations"));
		tabbedPane.setMnemonicAt(3, KeyEvent.VK_R);
		
		preapprovedPane = makePreapprovedPanel();
		tabbedPane.addTab(_("Pre-Approved"), icon_preapproved, new JScrollPane(preapprovedPane), _("Emails of Pre-Approved Receivers"));
		tabbedPane.setMnemonicAt(3, KeyEvent.VK_R);
		
		extraFields = new OrgExtra();
		extraFieldsPane = extraFields.getScrollPane();
		tabbedPane.addTab(_("Extra Fields"), icon, extraFieldsPane, _("Extra Fields"));

		JComponent handlingPane = makeHandlingPanel();
		tabbedPane.addTab(_("Handling"), icon_conf, handlingPane, _("Handling fields"));
		tabbedPane.setMnemonicAt(5, KeyEvent.VK_H);
        this.setLayout(new BorderLayout());
		this.add(tabbedPane);
		disable_it();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
	public void setOrg(String _orgID, boolean force){
		if(DEBUG) out.println("OrgEditor:setOrg: force="+force);
		if((orgID==null) && (_orgID==null)) return;
		if(_orgID==null) {
			disable_it(); return;
		}
		update_handling();
		if((orgID!=null) && (orgID.equals(_orgID)) && !force) return;
		orgID = _orgID;
		if(!force && enabled) disable_it();
		update_it(force);
		boolean editable = isEditable();
		if(editable && ! enabled) enable_it();
		if(this.extraFields!=null)	
			this.extraFields.setCurrent(_orgID);
		if(DEBUG) out.println("OrgEditor:setOrg: exit");
	}
	private void update_handling() {
		if(this.organization==null) return;
		
		requested.removeItemListener(this);
		requested.setSelected(this.organization.requested);
		requested.addItemListener(this);

		blocked.removeItemListener(this);
		blocked.setSelected(this.organization.blocked);
		blocked.addItemListener(this);

		broadcasted.removeItemListener(this);
		broadcasted.setSelected(this.organization.broadcasted);
		broadcasted.addItemListener(this);

	}
	/**
	 * Is this org editable?
	 * @return
	 */
	private boolean isEditable() {
		if(org == null) return false; // no org => no editing
		int method = new Integer(Util.getString(org.get(table.organization.ORG_COL_CERTIF_METHODS))).intValue();
		String GID = Util.getString(org.get(table.organization.ORG_COL_GID));
		String cID = Util.getString(org.get(table.organization.ORG_COL_CREATOR_ID));
		return D_Organization.isEditable(method, GID, cID);
	}
	/**
	 * Load keys for the current organization from available keys
	 * @param method
	 * @param default_gID
	 * @param default_hgID
	 * @param default_dateID
	 */
	@SuppressWarnings("unchecked")
	private void loadOrgIDKeys (int method, String default_gID, String default_hgID, String default_dateID) {
		globID_field.removeItemListener(this);
		if(DEBUG) System.out.println("OrgEditor:loadOrgIDKeys:Removed ORG GID ItemListener");
		globID_field.removeAllItems();globID_field.getEditor().setItem(null);
		if(m_editable && (method==table.organization._AUTHORITARIAN)) { // editing: may set any existing secret key
			if(DEBUG) System.out.println("OrgEditor:loadOrgIDKeys: editable_authoritarian");
			String sql_gid =
				"SELECT k."+table.key.public_key+",k."+table.key.ID_hash+",k."+table.key.name+",p."+table.peer.name+
				" FROM " + table.key.TNAME + " AS k " +
				" LEFT JOIN "+table.peer.TNAME+" AS p ON(k."+table.key.public_key+"=p."+table.peer.global_peer_ID+")"+
				" WHERE "+table.key.secret_key+" IS NOT NULL;";
			ArrayList<ArrayList<Object>> c;
			try {
				c = Application.db.select(sql_gid, new String[]{}, DEBUG);
				for(ArrayList<Object> i: c){
					String gid = Util.getString(i.get(0));
					String hash = Util.getString(i.get(1));
					String k_name = Util.getString(i.get(2));
					String p_name = Util.getString(i.get(3));
					String name;
					if(p_name != null) name = _("PEER:")+" "+p_name;
					else name = k_name;
					OrgGIDItem crt = new OrgGIDItem(gid,hash,name);
					globID_field.addItem(crt);
					if((default_gID != null)&&(default_gID.equals(gid))){
						globID_field.setSelectedItem(crt);
					}
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}else{ // fix: will set GID hash
			if(DEBUG) System.out.println("OrgEditor:loadOrgIDKeys: fix: "+default_hgID);
			if(default_gID != null) {
				if(DEBUG) System.out.println("OrgEditor:loadOrgIDKeys: default");
				//String sel_name = "SELECT "+table.peer.name+" FROM "+table.peer.TNAME+
				//" WHERE "+table.peer.global_peer_ID_hash+" = ?;";
				//Application.db.select(sel_name, new String[]{creatorID});
				OrgGIDItem crt = new OrgGIDItem(default_gID,default_hgID,default_hgID);
				globID_field.addItem(crt);globID_field.setSelectedItem(crt);
			}
		}
		if(DEBUG) System.out.println("OrgEditor:loadOrgIDKeys:ReInstalled ORG GID ItemListener");
		globID_field.addItemListener(this);
	}
	/**
	 * Will reload org from database
	 */
	private boolean reloadOrg(boolean force){
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql_org, new String[]{orgID});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			disable_it(); return false;
		}
		if(a.size()==0){ disable_it(); return false;}
		org = a.get(0);
		//if (Util.getString(org.get(table.organization.ORG_COL_GID))!=null)
		if (!isEditable())
			return force || DD.EDIT_RELEASED_ORGS;
		return true;
	}
	/**
	 * Called to reload editor fields from newly selected org
	 */
	@SuppressWarnings({"unchecked","unused"})
	private boolean update_it(boolean force) {
		if(DEBUG) out.println("OrgEditor:updateit: start");
		if(reloadOrg(force))
			enable_it();
		//else return false; // further processing changes arrival_date by handling creator and default_scoring fields
		if(org==null) return false;
		m_editable = isEditable(); // editable?

		requested.removeItemListener(this);
		requested.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_REQUEST))));
		requested.addItemListener(this);

		blocked.removeItemListener(this);
		blocked.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_BLOCK))));
		blocked.addItemListener(this);

		broadcast_rule.removeItemListener(this);
		broadcast_rule.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_BROADCAST_RULE))));
		broadcast_rule.addItemListener(this);

		broadcasted.removeItemListener(this);
		broadcasted.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_BROADCASTED))));
		broadcasted.addItemListener(this);

		date_field.getDocument().removeDocumentListener(this);
		date_field.setText(Util.getString(org.get(table.organization.ORG_COL_CREATION_DATE)));
		date_field.getDocument().addDocumentListener(this);

		String[] languages_array = D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_LANG)));
		//String editable_languages = editableArray(languages_array);
		this.languages_field.removeLVListener(this);
		this.languages_field.setArrayValue(languages_array);
		this.languages_field.addLVListener(this);
		
		String editable_org_name = editableArray(D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_NAME_ORG))));
		name_org_field.getDocument().removeDocumentListener(this);
		name_org_field.setText(editable_org_name);
		name_org_field.getDocument().addDocumentListener(this);
		
		String editable_forum_name = editableArray(D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_NAME_FORUM))));
		name_forum_field.getDocument().removeDocumentListener(this);
		name_forum_field.setText(editable_forum_name);
		name_forum_field.getDocument().addDocumentListener(this);
		
		String editable_motion_name = editableArray(D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_NAME_MOTION))));
		name_motion_field.getDocument().removeDocumentListener(this);
		name_motion_field.setText(editable_motion_name);
		name_motion_field.getDocument().addDocumentListener(this);
		
		String editable_justification_name = editableArray(D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_NAME_JUST))));
		name_justification_field.getDocument().removeDocumentListener(this);
		name_justification_field.setText(editable_justification_name);
		name_justification_field.getDocument().addDocumentListener(this);

		String[] scores_array = D_OrgConcepts.stringArrayFromString(Util.getString(org.get(table.organization.ORG_COL_SCORES)));
		this.default_scoring_options_field.removeLVListener(this);
		this.default_scoring_options_field.setArrayValue(scores_array);//, table.organization.ORG_LANG_SEP);//Util.getString(org.get(table.organization.ORG_COL_SCORES)));
		this.default_scoring_options_field.addLVListener(this);
		
		this.descriptionPane.getMFDocument().removeDocumentListener(this);
		this.descriptionPane.setDBDoc(Util.getString(org.get(table.organization.ORG_COL_DESCRIPTION)));
		this.descriptionPane.getMFDocument().addDocumentListener(this);
		
		this.instructionsMotionsPane.getMFDocument().removeDocumentListener(this);
		this.instructionsMotionsPane.setDBDoc(Util.getString(org.get(table.organization.ORG_COL_INSTRUC_MOTION)));
		this.instructionsMotionsPane.getMFDocument().addDocumentListener(this);
		
		this.instructionsRegistrationsPane.getMFDocument().removeDocumentListener(this);
		this.instructionsRegistrationsPane.setDBDoc(Util.getString(org.get(table.organization.ORG_COL_INSTRUC_REGIS)));
		this.instructionsRegistrationsPane.getMFDocument().addDocumentListener(this);
		
		preapproved.getDocument().removeDocumentListener(this);
		preapproved.setText(Util.getString(org.get(table.organization.ORG_COL_PREAPPROVED)));
		preapproved.getDocument().addDocumentListener(this);
		
		
		name_field.getDocument().removeDocumentListener(this);
		name_field.setText(Util.getString(org.get(table.organization.ORG_COL_NAME)));
		name_field.getDocument().addDocumentListener(this);
		
		String certif_method = Util.getString(org.get(table.organization.ORG_COL_CERTIF_METHODS));
		int method = 0;
		try{method = Integer.parseInt(certif_method);}catch(Exception e){}
		method_field.removeItemListener(this);
		try{method_field.setSelectedIndex(method);}catch(Exception e){}
		method_field.addItemListener(this);
		//if(DD.ORG_CREATOR_REQUIRED)
		globID_field.setEnabled(enabled && (method != table.organization._GRASSROOT));
		commit_field.setEnabled(enabled && (method != table.organization._GRASSROOT));
			
		
		String gID = Util.getString(org.get(table.organization.ORG_COL_GID));
		String hgID = Util.getString(org.get(table.organization.ORG_COL_GID_HASH));
		String dateID = Util.getString(org.get(table.organization.ORG_COL_CREATION_DATE));
		this.loadOrgIDKeys(method, gID, hgID, dateID);
		
		creator_field.removeItemListener(this);
		creator_field.removeAllItems();creator_field.getEditor().setItem(null);
		boolean creator_selected = false;
		String creatorID = Util.getString(org.get(table.organization.ORG_COL_CREATOR_ID));
		if(DEBUG) System.out.println("widgets:OrgEditor:update_it creatorID="+creatorID);
		//creator_field.addItem(new GIDItem("a","b","c"));
		if(creatorID != null){
			ArrayList<ArrayList<Object>> c_existing;
			String sql_peer =
				"SELECT "+ table.peer.peer_ID+","+table.peer.name+","+table.peer.global_peer_ID_hash+","+table.peer.global_peer_ID+
				" FROM "+table.peer.TNAME+
				" WHERE "+table.peer.peer_ID+"=?;";
			try {
				c_existing=Application.db.select(sql_peer, new String[]{creatorID}, DEBUG);
				if(c_existing.size()>0){
					ArrayList<Object> r=c_existing.get(0);
					String creator_peer_id = Util.getString(r.get(0));
					String hash = Util.getString(r.get(2));
					String name = _("PEER:")+" "+Util.getString(r.get(1));
					if(hash == null) hash = Util.getGIDhash(Util.getString(r.get(3)));
					else hash = hash.substring(2);
					if(DEBUG) System.out.println("widgets:OrgEditor: add"+ creator_peer_id+","+hash+","+name);
					CreatorGIDItem i = new CreatorGIDItem(creator_peer_id,hash,name);
					creator_field.addItem(i);
					creator_field.setSelectedItem(i);
					creator_selected = true;
					if(DEBUG) System.out.println("widgets:OrgEditor:update_it select creator creatorID="+i+" "+
							i.gid+":"+i.hash+" n="+i.name);
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(!creator_selected){
			creator_field.addItem(new CreatorGIDItem(null,"",_("Not Used")));
		}
		if(m_editable) { // if editing
			if(DEBUG) System.out.println("widgets:OrgEditor:update_it creatorID editing");
			ArrayList<ArrayList<Object>> c_available;
			String sql_gid =
				"SELECT" +
				" k."+table.key.public_key+
				",k."+table.key.ID_hash+
				",p."+table.peer.name +
				",p."+table.peer.peer_ID+
				",k."+table.key.name+
				" FROM "+table.key.TNAME+" AS k " +
				" LEFT JOIN "+table.peer.TNAME+" AS p ON(k."+table.key.public_key+"=p."+table.peer.global_peer_ID+")"+
				" WHERE k."+table.key.secret_key+" IS NOT NULL ORDER BY p."+table.peer.name+" DESC;";
			try {
				c_available = Application.db.select(sql_gid, new String[]{}, DEBUG);
				for(ArrayList<Object> i: c_available){
					String creator_peer_id = Util.getString(i.get(3));
					String hash = Util.getString(i.get(1));
					String name = Util.getString(i.get(2));
					String k_name = Util.getString(i.get(4));
					if(k_name==null) k_name = ""; else k_name+=": ";
					// if(name==null) name = (creator_peer_id!=null)?"No peer name available":"Key is not a peer, yet!";
					if(name==null) name = (creator_peer_id!=null)?(k_name+"No peer name available"):(k_name+"Key is not a peer, yet!");
					else name = _("PEER:")+" "+name;
					CreatorGIDItem crt = new CreatorGIDItem(creator_peer_id,hash,name);
					//if(DEBUG) System.out.println("widgets:OrgEditor:update_it adding="+crt);
					if(DEBUG) System.out.println("widgets:OrgEditor:update_it added="+creator_peer_id+" h="+hash+" n="+name);
					// Util.printCallPath("update");
					//globID_field.addItem(crt);
					creator_field.addItem(crt);
					if(!creator_selected && Util.equalStrings_and_not_null(hash,DD.getMyPeerIDhash())) {
						//creator_field.setSelectedItem(crt);
						if(DEBUG) System.out.println("widgets:OrgEditor:update_it selected="+crt);
					}
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		creator_field.addItemListener(this);
		
		category_field.removeItemListener(this);
		category_field_editor.getDocument().removeDocumentListener(this);
		category_field.removeAllItems(); category_field.getEditor().setItem("");
		String g_category_others = _("Others");
		boolean g_category_others_added = false;
		String category = Util.getString(org.get(table.organization.ORG_COL_CATEG));
		if(true){ // if my org
			String sql_cat = "SELECT "+table.organization.category+
			" FROM "+table.organization.TNAME+
			" GROUP BY "+table.organization.category;
			ArrayList<ArrayList<Object>> c;
			try {
				c = Application.db.select(sql_cat, new String[]{});
				for(ArrayList<Object> i: c){
					String crt = Util.getString(i.get(0));
					if(g_category_others.equals(crt)) g_category_others_added = true;
					category_field.addItem(crt);
					if((category != null) && (category.equals(crt))){
						category_field.setSelectedItem(crt);
					}
				}
			} catch (P2PDDSQLException e) {
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
	private void disable_it() {
		enabled = false;
		if(this.broadcast_rule!=null) this.broadcast_rule.setEnabled(false);
		if(this.name_forum_field!=null) this.name_forum_field.setEnabled(false);
		if(this.name_org_field!=null) this.name_org_field.setEnabled(false);
		if(this.name_motion_field!=null) this.name_motion_field.setEnabled(false);
		if(this.name_justification_field!=null) this.name_justification_field.setEnabled(false);
		if(this.languages_field!=null) this.languages_field.setEnabled(false);
		if(this.name_field!=null) this.name_field.setEnabled(false);
		if(this.method_field!=null) this.method_field.setEnabled(false);
		if(this.category_field!=null) this.category_field.setEnabled(false);
		if(this.globID_field!=null) this.globID_field.setEnabled(false);
		if(this.creator_field!=null) this.creator_field.setEnabled(false);
		if(this.keygen_field!=null) this.keygen_field.setEnabled(false);
		if(this.commit_field!=null) this.commit_field.setEnabled(false);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(false);
		if(this.date_field!=null) this.date_field.setEnabled(false);
		if(this.default_scoring_options_field!=null) this.default_scoring_options_field.setEnabled(false);
		if(this.descriptionPane!=null) this.descriptionPane.setEnabled(false);
		if(this.preapproved!=null) this.preapproved.setEnabled(false);
		if(this.instructionsMotionsPane!=null) this.instructionsMotionsPane.setEnabled(false);
		if(this.instructionsRegistrationsPane!=null) this.instructionsRegistrationsPane.setEnabled(false);
		if(this.extraFields!=null) this.extraFields.setEnabled(false);
	}
	private void enable_it() {
		enabled = true;
		if(DEBUG)System.out.println("OrgEditor:Enabling");
		//Util.printCallPath("enabling");
		if(this.broadcast_rule!=null) this.broadcast_rule.setEnabled(true);
		if(this.name_forum_field!=null) this.name_forum_field.setEnabled(true);
		if(this.name_org_field!=null) this.name_org_field.setEnabled(true);
		if(this.name_motion_field!=null) this.name_motion_field.setEnabled(true);
		if(this.name_justification_field!=null) this.name_justification_field.setEnabled(true);
		if(this.languages_field!=null) this.languages_field.setEnabled(true);
		if(this.name_field!=null) this.name_field.setEnabled(true);
		if(this.method_field!=null) this.method_field.setEnabled(true);		
		if(this.category_field!=null) this.category_field.setEnabled(true);
		if(this.globID_field!=null) this.globID_field.setEnabled(true);
		if(this.creator_field!=null) this.creator_field.setEnabled(true);
		if(this.keygen_field!=null) this.keygen_field.setEnabled(true);
		if(this.commit_field!=null) this.commit_field.setEnabled(true);
		if(DD.ORG_CREATOR_REQUIRED)
			try{this.keygen_field.setEnabled(org.get(table.organization.ORG_COL_CREATOR_ID)!=null);}catch(Exception e){}
		if(this.dategen_field!=null) this.dategen_field.setEnabled(true);
		if(this.date_field!=null) this.date_field.setEnabled(true);
		if(this.default_scoring_options_field!=null) this.default_scoring_options_field.setEnabled(true);
		if(this.descriptionPane!=null) this.descriptionPane.setEnabled(true);
		if(this.preapproved!=null) this.preapproved.setEnabled(true);
		if(this.instructionsMotionsPane!=null) this.instructionsMotionsPane.setEnabled(true);
		if(this.instructionsRegistrationsPane!=null) this.instructionsRegistrationsPane.setEnabled(true);
		if(this.extraFields!=null) this.extraFields.setEnabled(true);
	}
	private MultiformatDocumentEditor makeInstructionsMotionsPanel() {
		return new MultiformatDocumentEditor(TEXT_LEN_ROWS,TEXT_LEN_COLS);
/*
		JTextArea result;
		//result = new JTextArea(TEXT_LEN_ROWS,TEXT_LEN_COLS);
		result = new JTextArea();
		result.setLineWrap(true);
		result.setWrapStyleWord(true);
		result.setAutoscrolls(true);
		result.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		result.getDocument().addDocumentListener(this);
		return result;
		*/
	}
	private MultiformatDocumentEditor makeDescriptionPanel() {
		return new MultiformatDocumentEditor(TEXT_LEN_ROWS,TEXT_LEN_COLS);
		/*
		JTextArea result;
		//result = new JTextArea(TEXT_LEN_ROWS,TEXT_LEN_COLS);
		result = new JTextArea();
		result.setLineWrap(true);
		result.setWrapStyleWord(true);
		result.setAutoscrolls(true);
		result.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		result.getDocument().addDocumentListener(this);
		return result;
		*/
	}
	private JPanel makePreapprovedPanel(){
		JPanel p = new JPanel(new BorderLayout());
		JTextArea Instruct;
		p.add(
				Instruct = new JTextArea(_("For controlled broadcasting, add email addresses, separated by any spaces and the separator:")+" \""+table.organization.SEP_PREAPPROVED+"\""),
				BorderLayout.NORTH
				);
		Instruct.setEditable(false);
		p.add(preapproved=new JTextArea(), BorderLayout.CENTER);
		preapproved.getDocument().addDocumentListener(this); //.addKeyListener(this);
		return p;
	}
	private MultiformatDocumentEditor makeInstructionsRegistrationsPanel() {
		MultiformatDocumentEditor ed =
				new MultiformatDocumentEditor(TEXT_LEN_ROWS,TEXT_LEN_COLS);
		ed.editor.name = "Instr Reg";
		return ed;
		/*
		JTextArea result;
		//result = new JTextArea(TEXT_LEN_ROWS,TEXT_LEN_COLS);
		result = new JTextArea();
		result.setLineWrap(true);
		result.setWrapStyleWord(true);
		result.setAutoscrolls(true);
		result.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		result.getDocument().addDocumentListener(this);
		return result;
		*/
	}
	private JComponent makeHandlingPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 0;
		//CheckboxGroup cg = null;//new CheckboxGroup();
		requested = new JCheckBox(_("Requested"), false);
		requested.addItemListener(this);
		
		broadcasted = new JCheckBox(_("Broadcasted"), false);
		broadcasted.addItemListener(this);
		
		broadcast_rule = new JCheckBox(_("Broadcast Indiscriminately"), true);
		broadcast_rule.addItemListener(this);
		
		blocked = new JCheckBox(_("Blocked"), false);
		blocked.addItemListener(this);
		p.add(broadcast_rule,c);
		c.gridx = 0; c.gridy = 1;
		p.add(broadcasted,c);
		c.gridx = 0; c.gridy = 2;
		p.add(requested,c);
		c.gridx = 0; c.gridy = 3;
		p.add(blocked,c);
		JScrollPane scrollPane = new JScrollPane(p);
		return scrollPane;
	}
	private JComponent makeLanguagePanel() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 0;		
		TranslatedLabel label_name_org = new TranslatedLabel("Name Organization");
		p.add(label_name_org, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(name_org_field = new JTextField(TEXT_LEN),c);
		name_org_field.getDocument().addDocumentListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 1;		
		TranslatedLabel label_name_forum = new TranslatedLabel("Name Forum");
		p.add(label_name_forum, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(name_forum_field = new JTextField(TEXT_LEN),c);
		name_forum_field.getDocument().addDocumentListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 2;		
		TranslatedLabel label_name_motion = new TranslatedLabel("Name Motion");
		p.add(label_name_motion, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(name_motion_field = new JTextField(TEXT_LEN),c);
		name_motion_field.getDocument().addDocumentListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 3;		
		TranslatedLabel label_name_justification = new TranslatedLabel("Name Justification");
		p.add(label_name_justification, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(name_justification_field = new JTextField(TEXT_LEN),c);
		name_justification_field.getDocument().addDocumentListener(this);

		c.gridx = 0; c.gridy = 4;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel languages = new TranslatedLabel("Languages");
		p.add(languages, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		hash_org.languages = new String[]{_("en"),_("ro")};
		languages_field = new LVComboBox();
		//p.add(languages_field.setValue(Util.concat(hash_org.languages,table.organization.ORG_LANG_SEP)),c);
		p.add(languages_field.setArrayValue(hash_org.languages),c);
		languages_field.addLVListener(this);
		JScrollPane scrollPane = new JScrollPane(p);
		return scrollPane;
	}
	@SuppressWarnings("unchecked")
	private JComponent makeGeneralPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = 0;		
		TranslatedLabel label_name = new TranslatedLabel("Name");
		p.add(label_name, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(name_field = new JTextField(TEXT_LEN),c);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		name_field.getDocument().addDocumentListener(this);

		c.gridx = 0; c.gridy = 1;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_method = new TranslatedLabel("Method");
		p.add(label_method, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(method_field = new JComboBox(g_methods),c);
		hash_org.certification_methods=new String[]{"0"};
		method_field.addItemListener(this);
		
		c.gridx = 0; c.gridy = 2;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_creator = new TranslatedLabel("Initiator");
		p.add(label_creator, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST; // _("Initiator N/A")
		p.add(creator_field = new JComboBox(new String[]{}),c);
		// add all my peer identities
		creator_field.addItemListener(this);
		
		c.gridx = 0; c.gridy = 3;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_globID = new TranslatedLabel("Identity");
		p.add(label_globID, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST; // _("Identity N/A")
		p.add(globID_field = new JComboBox(new String[]{}),c);
		globID_field.addItemListener(this);
		
		c.gridx = 0; c.gridy = 4;		
		c.anchor = GridBagConstraints.EAST;
		//TranslatedLabel label_globID = new TranslatedLabel("Identity");
		//p.add(label_globID, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		keygen_field = new JButton(_("Generate Key"));
		commit_field = new JButton(_("Commit"));
		GridBagLayout bl = new GridBagLayout();
		JPanel bt = new JPanel(bl);
		GridBagConstraints cb = new GridBagConstraints();
		cb.fill = GridBagConstraints.NONE;
		cb.anchor = GridBagConstraints.EAST;
		cb.gridx = 0; cb.gridy = 0;		
		bt.add(keygen_field,cb);
		cb.gridx = 1; cb.gridy = 0;		
		bt.add(commit_field,cb);
		p.add(bt,c);
		keygen_field.addActionListener(this);
		commit_field.addActionListener(this);
		
		String creation_date = Util.getGeneralizedTime();
		date_field = new JTextField(creation_date);
		date_field.setColumns(creation_date.length());
		
		c.gridx = 0; c.gridy = 5;		
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
		//category_field.setMinimumSize(date_field.getSize());
		
		c.gridx = 0; c.gridy = 6;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		hash_org.creation_date = creation_date;
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		date_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = 7;		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(dategen_field = new JButton(_("Set Current Date")),c);
		dategen_field.addActionListener(this);
		
		c.gridx = 0; c.gridy = 8;		
		c.anchor = GridBagConstraints.EAST;
		TranslatedLabel label_choices = new TranslatedLabel("Default Choices");
		p.add(label_choices, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		hash_org.default_scoring_options = new String[]{_("YES"),_("NO")};
		default_scoring_options_field = new LVComboBox();
		//p.add(default_scoring_options_field.setValue(Util.concat(hash_org.default_scoring_options,table.organization.ORG_LANG_SEP),table.organization.ORG_LANG_SEP),c);
		p.add(default_scoring_options_field.setArrayValue(hash_org.default_scoring_options),c);
		//default_scoring_options_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		//default_scoring_options_field.addContainerListener(this);
		//default_scoring_options_field.addActionListener(this);
		//default_scoring_options_field.addComponentListener(this);
		//default_scoring_options_field.addFocusListener(this);
		default_scoring_options_field.addLVListener(this);
		JScrollPane scrollPane = new JScrollPane(p);
		return scrollPane;
	}
	/**
	 * Force editing ready orgs if col<3
	 */
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if(DEBUG)System.out.println("OrgEditor: orgUpdate: col: "+col);
		//this.setOrg(orgID, col<Orgs.FORCE_THREASHOLD_COL);
		this.organization=org;
		this.setOrg(orgID, false); // false
	}
	public void org_forceEdit(String orgID, D_Organization org) {
		if(DEBUG)System.out.println("OrgEditor: forceEdit");
		this.organization=org;
		//this.setOrg(orgID, col<Orgs.FORCE_THREASHOLD_COL);
		this.setOrg(orgID, true); // false
	}
	
	@SuppressWarnings("unchecked")
	public void handleFieldEvent(Object source) {
		
		//boolean DEBUG = true;
		if(DEBUG)System.out.println("OrgEditor: handleFieldEvent: enter enabled="+enabled);
		
		if(this.broadcasted == source) {
			boolean val = broadcasted.isSelected();
			//OrgsModel.toggleServing(this.orgID, false, val);
			D_Organization.setBroadcasting(this.orgID, val);
			if(this.organization!=null) this.organization.broadcasted = val;
			return;
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			D_Organization.setBlocking(this.orgID, val);
			if(this.organization!=null) this.organization.blocked = val;
			return;
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			D_Organization.setRequested(this.orgID, val);
			if(this.organization!=null) this.organization.requested = val;
			return;
		}
		if(!enabled) return;
		Calendar _currentTime = Util.CalendargetInstance();
		String currentTime = Encoder.getGeneralizedTime(_currentTime);
		String creationTime = date_field.getText();

		
		if(this.broadcast_rule == source) {
			boolean val = broadcast_rule.isSelected();
			if(_DEBUG)System.out.println("OrgEditor:handleFieldEvent: broadcast_rule:val="+val);
			String val_S = Util.bool2StringInt(val);
			if(_DEBUG)System.out.println("OrgEditor:handleFieldEvent: broadcast_rule:val="+val_S);
			try {
				Application.db.updateNoSync(table.organization.TNAME,
						new String[]{table.organization.broadcast_rule, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{val_S, creationTime, currentTime, null, this.orgID}, _DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			if(this.org!=null) org.set(table.organization.ORG_COL_BROADCAST_RULE,val_S);
			if(this.organization!=null) this.organization.broadcast_rule = val;
			return;
		}
		
		if(this.default_scoring_options_field == source) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: default scoring"); 
			//String[] editable_scores = arrayFromEditable(this.default_scoring_options_field.buildVal(table.organization.ORG_LANG_SEP));
			String[] editable_scores = this.default_scoring_options_field.getVal();
			String new_text = D_OrgConcepts.stringFromStringArray(editable_scores);
			//String new_text = this.default_scoring_options_field.buildVal(table.organization.SEP_scoring_options);
			//if(new_text==null) this.hash_org.default_scoring_options = null;
			//else 
				this.hash_org.default_scoring_options = editable_scores;//new_text.split(table.organization.SEP_scoring_options);
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.default_scoring_options, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;						
		}
		if(this.languages_field == source) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: languages");
			//String new_text = this.languages_field.buildVal();
			String[] editable_lang = arrayFromEditable(this.languages_field.buildVal(table.organization.ORG_LANG_SEP));
			String new_text = D_OrgConcepts.stringFromStringArray(editable_lang);
			if(new_text==null) this.hash_org.languages = null;
			else this.hash_org.languages = editable_lang;//new_text.split(table.organization.ORG_LANG_SEP);
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.languages, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;						
		}
		if((this.name_org_field==source)||(this.name_org_field.getDocument()==source)) {
			String new_text = D_OrgConcepts.stringFromStringArray(arrayFromEditable(this.name_org_field.getText()));
			//String new_text = this.name_org_field.getText();
			this.hash_org.name_organization = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.name_organization, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.name_forum_field==source)||(this.name_forum_field.getDocument()==source)) {
			String new_text = D_OrgConcepts.stringFromStringArray(arrayFromEditable(this.name_forum_field.getText()));
			this.hash_org.name_forum = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.name_forum, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.name_motion_field==source)||(this.name_motion_field.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: motion");
			String new_text = D_OrgConcepts.stringFromStringArray(arrayFromEditable(this.name_motion_field.getText()));
			this.hash_org.name_motion = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.name_motion, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.name_justification_field==source)||(this.name_justification_field.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: justif");
			String new_text = D_OrgConcepts.stringFromStringArray(arrayFromEditable(this.name_justification_field.getText()));
			this.hash_org.name_motion = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.name_justification, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.descriptionPane==source)||(this.descriptionPane.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: description");
			String new_text = this.descriptionPane.getDBDoc();
			//this.hash_org.instructions_new_motions = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.description, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.instructionsMotionsPane==source)||(this.instructionsMotionsPane.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: instructions motions");
			String new_text = this.instructionsMotionsPane.getDBDoc();
			this.hash_org.instructions_new_motions = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.instructions_new_motions, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.instructionsRegistrationsPane==source)||(this.instructionsRegistrationsPane.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: instructions registration");
			String new_text = this.instructionsRegistrationsPane.getDBDoc();
			this.hash_org.instructions_registration = new_text;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.instructions_registration, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_text, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if((this.preapproved==source)||(this.preapproved.getDocument()==source)) {
			String _preapproved = this.preapproved.getText();
			this.organization._preapproved = _preapproved;
			this.organization.updatePreapproved();
			this.organization.setPreferencesDate(_currentTime, currentTime);
			try {
				this.organization.storeLocalFlags();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
//			try {
//				Application.db.update(table.organization.TNAME,
//						new String[]{table.organization.preapproved, table.organization.arrival_date},
//						new String[]{table.organization.organization_ID},
//						new String[]{_preapproved, currentTime, this.orgID});
//			} catch (P2PDDSQLException e) {
//				e.printStackTrace();
//			}
			return;
		}
		if((name_field==source)||(name_field.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: name");
			String new_name = name_field.getText();
			this.hash_org.name = new_name;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.name, table.organization.creation_date, table.organization.arrival_date},
						new String[]{table.organization.organization_ID},
						new String[]{new_name, creationTime, currentTime, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;
		}
		if((date_field==source)||(date_field.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: date");
			String new_date = date_field.getText();
			if(!this.m_setting_Date&&"".equals(new_date) && hash_org.creation_date!=null) {
				if(DEBUG) System.out.println("OrgEditor:changedate:Set date to:"+new_date);
				date_field.setText(hash_org.creation_date);
				date_field.setForeground(Color.GREEN);
				return;
			}
			Calendar c = Util.getCalendar(new_date);
			if(c==null) {
				date_field.setForeground(Color.RED);
				return;
			}
			date_field.setForeground(Color.GREEN);
			new_date=Encoder.getGeneralizedTime(c);
			this.hash_org.creation_date = new_date;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_date, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;
		}
		//  authoritarian?keygen<-true ; creatorID?: keygen<-enabled
		if(creator_field==source) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: creator");
			if(DEBUG) Util.printCallPath("Linux tracing");
			CreatorGIDItem selected = (CreatorGIDItem) creator_field.getSelectedItem();
			String id = null;
			try {
				if(selected == null){
					id = null;
				}else{
					if(DEBUG) out.println("OrgEditor:handleFieldEvent: selected creator="+selected.toStringDump());
					if(selected.gid == null) id = null;
					else id = ""+(new Integer(selected.gid).longValue());
				}
				org.set(table.organization.ORG_COL_CREATOR_ID, id);
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.creator_ID, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{id, creationTime, currentTime, null, this.orgID}, DEBUG);
				//this.commit_field.setEnabled(id!=null);
				if(DD.ORG_CREATOR_REQUIRED) {
					int method = method_field.getSelectedIndex();
					if(method==table.organization._AUTHORITARIAN) {
						this.keygen_field.setEnabled(true);
					}
					if(method==table.organization._GRASSROOT) {
						this.keygen_field.setEnabled(id!=null);
					}
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		if((category_field==source) || (category_field_editor==source)||(category_field_editor.getDocument()==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: category");
			String new_category = Util.getString(category_field.getSelectedItem());
			if((new_category!=null) && "".equals(new_category.trim())) new_category = null;
			//if(new_category == null) return;
			hash_org.category = new_category;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.category, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_category, creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if((globID_field==source)) {
			if(_DEBUG) out.println("OrgEditor:handleFieldEvent: globID");
			OrgGIDItem selected = (OrgGIDItem)globID_field.getSelectedItem();
			if(selected == null) {
				hash_org.global_organization_ID = null;
				return;
			}
			String new_ID = Util.getString(selected.gid);
			hash_org.global_organization_ID = new_ID;
			String new_IDhash = D_Organization.getOrgGIDHashAuthoritarian(new_ID);
			if(_DEBUG) out.println("OrgEditor:handleFieldEvent: globIDhash="+new_IDhash);
			
			String signature=null;
			/*
			ciphersuits.SK sk = Util.getStoredSK(new_ID, null);
			byte[] _signature=null;
			try {
				_signature = OrgHandling.getOrgSignature(this.orgID, creationTime, sk);
				signature = Util.stringSignatureFromByte(_signature);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return;
			}
			*/
			
			//if((new_ID!=null) && "".equals(new_ID.trim())) new_ID = null;
			//if(new_category == null) return;
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.global_organization_ID,
							table.organization.global_organization_ID_hash,
							table.organization.creation_date,
							table.organization.arrival_date,
							table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{new_ID, new_IDhash, //selected.hash,
						creationTime, currentTime, signature, this.orgID}, _DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
			// disable_it();
			 
		}
		// store signature and modifs for authoritarian org
		if((commit_field==source)) {
			OrgGIDItem selected = (OrgGIDItem)globID_field.getSelectedItem();
			if((selected == null) || (selected.gid == null)){
				Application.warning(_("No Identity created/selected"), _("Organization not ready"));
				return;
			}
			try{
				String name = Util.getString(org.get(table.organization.ORG_COL_NAME));
				if((name==null) || ("".equals(name.trim()))){
					Application.warning(_("You need to select a name for you organization"), _("Missing Name"));
					return;						
				}
				if(org.get(table.organization.ORG_COL_CREATOR_ID)==null){
					Application.warning(_("You may want to select a peer identity as Initiator"), _("Missing Initiator"));
					//return;					
				}
			}catch(Exception e){}

			/*
			String gID=Util.getString(selected.gid);//hash_org.global_organization_ID;
			String gIDhash = D_Organization.getOrgGIDHashAuthoritarian(gID);
			byte[] _signature = this.signOrg(gID, creationTime);
			String signature = Util.stringSignatureFromByte(_signature);
			*/
			String ID = Util.getString(org.get(table.organization.ORG_COL_ID));
			try {
				D_Organization.readSignSave(Util.lval(ID,-1));
				/*
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.global_organization_ID,
							table.organization.global_organization_ID_hash,
							table.organization.creation_date,
							table.organization.arrival_date,
							table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{gID, gIDhash, //selected.hash,
						creationTime, currentTime, signature, this.orgID});
				*/
			} catch (Exception e) {
				e.printStackTrace();
				Application.warning(_("Error saving organization:")+" "+e.getLocalizedMessage()+"\n"+
						_("Potentially you use the same Identity as another existing org.")+"\n"+
						_("Probably you should generate a new Identity key (for AUTHORITARIAN orgs)\n"+
						"or should change some parameters to avoid conflicts (with GRASSROOT orgs)"),
						_("Error Saving Org"));
				return;
			}
			disable_it();			
		}
		if((method_field==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: method");
			int method = method_field.getSelectedIndex();
			hash_org.certification_methods=new String[]{""+method};
			globID_field.setEnabled(method != 0);
			commit_field.setEnabled(method != 0);
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.certification_methods, table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{method+"", creationTime, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}			
			this.reloadOrg(true); // force since editing already
			m_editable=isEditable();
			String default_gID = Util.getString(org.get(table.organization.ORG_COL_GID));
			String default_hgID = Util.getString(org.get(table.organization.ORG_COL_GID_HASH));
			String default_dateID = Util.getString(org.get(table.organization.ORG_COL_CREATION_DATE));
			this.loadOrgIDKeys(method, default_gID, default_hgID, default_dateID);
		}
		if((dategen_field==source)) {
			if(DEBUG) out.println("OrgEditor:handleFieldEvent: dategen");
			String crt_date = Util.getGeneralizedTime();
			
			try {
				Application.db.update(table.organization.TNAME,
						new String[]{table.organization.creation_date, table.organization.arrival_date, table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{crt_date, currentTime, null, this.orgID});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}			
			
			if(DEBUG) System.out.println("OrgEditor:setDateButton:Set date to:"+crt_date);
			m_setting_Date = true;
			this.date_field.setText(crt_date);
			m_setting_Date = false;
			this.hash_org.creation_date = crt_date;
		}
		if((keygen_field==source)) {
			String name = name_field.getText();
			String creation_date = date_field.getText();
			int method = method_field.getSelectedIndex();
			if(method==table.organization._AUTHORITARIAN) {
				if(DEBUG) out.println("OrgEditor:handleFieldEvent: genKey for AUTHORITARIAN");
				Cipher keys = Util.getKeyedGlobalID(AUTHORITARIAN, ""+name+creation_date);
				keys.genKey(1024);
				String date = Util.getGeneralizedTime();
				try {
					DD.storeSK(keys, "ORG:"+name+":", date);
				} catch (P2PDDSQLException e2) {
					e2.printStackTrace();
				}
				String gID = Util.getKeyedIDPK(keys);
				String sID = Util.getKeyedIDSK(keys);
				//String gIDhash = Util.getHash(keys.getPK().encode());
				String gIDhash = Util.getGIDhash(gID); // for the key table
				String gIDhashAuth = D_Organization.getOrgGIDHashAuthoritarian(gID);
				String type = Util.getKeyedIDType(keys);
				OrgGIDItem gid = new OrgGIDItem(gID, gIDhash, date);
				//String hashorg = Util.getHash(hash_org.encode(), DD.APP_ID_HASH);
				byte[] _signature=null;
				String signature=null;
				D_Organization orgData = null;
				try {
					D_Organization[] org = new D_Organization[1]; 
					_signature = D_Organization.getOrgSignature(this.orgID, creationTime, Cipher.getSK(sID), org);
					if((org[0]==null) || (org[0].name==null) || ("".equals(org[0].name.trim()))){
						Application.warning(_("You need to select a name for you organization"), _("Missing Name"));
						return;						
					}
					if((org[0]==null) || (org[0].params==null) || (org[0].params.creator_global_ID==null)) {
						Application.warning(_("You need to select a peer identity as")+" "+_("Initiator"), _("Missing Initiator"));
						return;
					}
					orgData = org[0];
					signature = Util.stringSignatureFromByte(_signature);
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
					return;
				}
				//ArrayList<GIDItem> gIDs = new ArrayList<GIDItem>();
				//gIDs.add(gid);
				if(Application.ask(
						_("Are you ready to commit?")+" "+
						_("Later changes are possible to parameters other than:")+" "+_("Identity"),
						_("Commit"), JOptionPane.OK_CANCEL_OPTION)>0) return;
				globID_field.addItem(gid);
				//String hash_alg = table.organization.hash_org_alg_crt;
				//String sign = "";//sign hash with secret key
				try {
					//String _date = Util.getGeneralizedTime();
					Application.db.insert(table.key.TNAME, 
							new String[]{table.key.ID_hash,table.key.public_key,table.key.secret_key,table.key.type,table.key.creation_date},
							new String[]{gIDhash, gID, sID, type, date});
					Application.db.update(table.organization.TNAME,
							new String[]{table.organization.global_organization_ID,
							table.organization.global_organization_ID_hash,
							table.organization.signature_initiator,
							//table.organization.hash_orgID,
							table.organization.creation_date,table.organization.arrival_date,table.organization.signature},
							new String[]{table.organization.organization_ID},
							new String[]{gID, gIDhashAuth,
							Util.stringSignatureFromByte(orgData.signature_initiator),
							//hashorg, 
							creationTime, currentTime, signature, this.orgID},DEBUG);
					org.set(table.organization.ORG_COL_GID, gID);
					org.set(table.organization.ORG_COL_GID_HASH, gIDhash);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					return;
				}
			}else
			if(method==table.organization._GRASSROOT) {
				if(DEBUG) out.println("OrgEditor:handleFieldEvent: genKey for GRASSROOT");
				//String hashorg = Util.getHash(hash_org.encode(), DD.APP_ID_HASH);
				//byte[] _gid=null;
				String gid=null;
				SK sk_ini=null;
				D_Organization orgData = null;
				try {
					D_Organization org[] = new D_Organization[1]; 
					gid = D_Organization._getOrgGIDandHashForGrassRoot(this.orgID, creationTime, org);
					if((org[0]==null) || (org[0].name==null) || ("".equals(org[0].name.trim()))){
						Application.warning(_("You need to select a name for you organization"), _("Missing Name"));
						return;						
					}
					if((org[0]==null) || (org[0].params==null) || (org[0].params.creator_global_ID==null)){
						Application.warning(_("You need to select a peer identity as")+" "+ _("Initiator"), _("Missing Initiator"));
						return;
					}
//					if(_gid==null){
//						Util.printCallPath("Failure to compute Hash, incomplete data");
//						return;
//					}
//					gid = Util.stringSignatureFromByte(_gid);
					orgData = org[0];
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
					return;
				}
				if(Application.ask(_("GrassRoot commision is irreversible.")+"\n"+
						_("No parameter can be changed later.")+"\n"+
						_("For later modifications you will have to create a new organization.")+"\n"+
						_("Once this organization is disseminated, you cannot retract it.")+"\n"+
						_("Are you ready to commit?"), _("Commit"), JOptionPane.OK_CANCEL_OPTION)>0) return;
				try {
					Application.db.update(table.organization.TNAME,
						new String[]{table.organization.global_organization_ID,
							table.organization.global_organization_ID_hash,
							table.organization.signature_initiator,
							//table.organization.hash_orgID,
							table.organization.creation_date,table.organization.arrival_date,table.organization.signature},
						new String[]{table.organization.organization_ID},
						new String[]{gid, gid,
							Util.stringSignatureFromByte(orgData.signature_initiator),
							//hashorg, 
							creationTime, currentTime, gid, this.orgID}, DEBUG);
					org.set(table.organization.ORG_COL_GID, gid);
					org.set(table.organization.ORG_COL_GID_HASH, gid);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					return;
				}
			}
			String default_gID = Util.getString(org.get(table.organization.ORG_COL_GID));
			String default_hgID = Util.getString(org.get(table.organization.ORG_COL_GID_HASH));
			String default_dateID = Util.getString(org.get(table.organization.ORG_COL_CREATION_DATE));
			this.loadOrgIDKeys(method, default_gID, default_hgID, default_dateID);
			disable_it();
		}
		if(DEBUG) out.println("OrgEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	/**
	 * concats with table.organization.ORG_LANG_SEP
	 * uses "" for null
	 * @param stringArrayFromString
	 * @return
	 */
	public static String editableArray(String[] stringArrayFromString) {
		return Util.concat(stringArrayFromString, table.organization.ORG_LANG_SEP, "");
	}
	/**
	 * returns null for null or ""
	 * splits by organization.ORG_LANG_SEP = ";"
	 * @param text
	 * @return
	 */
	public static String[] arrayFromEditable(String text) {
		if(text==null) return null;
		if("".equals(text)) return null;
		return text.split(table.organization.ORG_LANG_SEP);
	}
	private byte[] signOrg(String gID, String creationTime){
		String new_ID = Util.getString(gID);
		hash_org.global_organization_ID = new_ID;
		ciphersuits.SK sk = Util.getStoredSK(new_ID, null);

		byte[] _signature=null;
		//String signature=null;
		try {
			D_Organization[] org = new D_Organization[1]; 
			_signature = D_Organization.getOrgSignature(this.orgID, creationTime, sk, org);
			if((org[0]==null) || (org[0].params==null) || (org[0].params.creator_global_ID==null)){
				Application.warning(_("You need to select a peer identity as Initiator"), _("Missing Initiator"));
				return null;
			}
			//signature = Util.stringSignatureFromByte(_signature);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return _signature;
	}
	@Override
	public void actionPerformed(ActionEvent act) {
		if(DEBUG)System.out.println("OrgEditor:actionPerformed:Action: "+act);
		this.handleFieldEvent(act.getSource());
	}
	@Override
	public void focusGained(FocusEvent act) {
		if(DEBUG)System.out.println("OrgEditor:focusGained:Action: "+act);
		//this.handleFieldEvent(act.getSource());
	}
	@Override
	public void focusLost(FocusEvent evt) {
		if(DEBUG)System.out.println("OrgEditor:focusLost:Action: "+evt);
		this.handleFieldEvent(evt.getSource());
	}
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("OrgEditor:changedUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("OrgEditor:insertUpdate:Action: "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(DEBUG)System.out.println("OrgEditor:removeUpdate:Action "+evt);
		this.handleFieldEvent(evt.getDocument());
	}
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if(DEBUG)System.out.println("OrgEditor:itemStateChanged:Action "+evt);
		// if(evt.getStateChange())
		this.handleFieldEvent(evt.getSource());		
	}
	@Override
	public void listenLV(Object source) {
		if(DEBUG)System.out.println("OrgEditor:listenLV:SRC= "+source);
		this.handleFieldEvent(source);		
	}
	@Override
	public void changeUpdate(Object source) {
		if(DEBUG)System.out.println("OrgEditor:changeUpdate:SRC= "+source);
		this.handleFieldEvent(source);		
	}
}

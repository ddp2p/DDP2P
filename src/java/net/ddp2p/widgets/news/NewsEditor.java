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
package net.ddp2p.widgets.news;

import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;
import net.ddp2p.widgets.components.DebateDecideAction;
import net.ddp2p.widgets.components.DocumentEditor;
import net.ddp2p.widgets.components.TranslatedLabel;
import net.ddp2p.widgets.motions.MotionEditor;
import net.ddp2p.widgets.news.NewsListener;
import net.ddp2p.widgets.news.NewsModel;

class NewsGIDItem{
	String gid;
	String id;
	String name;
	/**
	 *  create from gid, id , name
	 * @param _gid
	 * @param _id
	 * @param _name
	 */
	public NewsGIDItem(String _gid, String _id, String _name) {set(_gid, _id, _name);}
	public void set(String _gid, String _id, String _name){
		gid = _gid;
		id = _id;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name;
		if(id == null) return "JUST:"+super.toString();
		return "JUST:"+id;
	}
	public String toStringDump() {
		return "JUST:"+name+" id="+id+" gid="+gid;
	}
}

@SuppressWarnings("serial")
public class NewsEditor  extends JPanel  implements NewsListener, DocumentListener, ItemListener, ActionListener, OrgListener, MotionsListener, MouseListener {
	private static final int TITLE_LEN = 20;
	private static final int TEXT_LEN_ROWS = 10;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String BODY_FORMAT = "TXT";
	private static final String TITLE_FORMAT = "TXT";
	
	public JTextField news_title_field;
	//public JComboBox just_answer_field;
	public DocumentEditor news_body_field;
	public JButton news_submit_field;
	public JTextField date_field;
	public JButton dategen_field;
	public JButton load_field;
	public JButton setTxtMode;
	public JButton setHTMMode;
	public JCheckBox requested;
	public JCheckBox broadcasted;
	public JCheckBox blocked;

	NewsGIDItem[] combo_answerTo = new NewsGIDItem[]{};
	private boolean enabled = false;
	private String news_ID = null;
	
	D_News d_news; // data about the current organization
	boolean m_editable = false;
	//private int max_general_fields;
	JLabel org_label = new JLabel();
	JLabel mot_label = new JLabel();
	long organization_ID = -1;
	long motion_ID = -1;
	boolean forced;
	private D_Organization organization;
	JPanel panel_body = new JPanel();

	
	private void disable_it() {
		enabled  = false;
		if(this.news_title_field!=null) this.news_title_field.setEnabled(false);
		//if(this.just_answer_field!=null) this.just_answer_field.setEnabled(false);
		if(this.news_body_field!=null) this.news_body_field.setEnabled(false);
		if(this.news_submit_field!=null) this.news_submit_field.setEnabled(false);
		if(this.date_field!=null) this.date_field.setEnabled(false);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(false);
	}
	private void enable_it() {
		enabled  = true;
		if(DEBUG)System.out.println("NewsEditor:Enabling");
		if(this.news_title_field!=null) this.news_title_field.setEnabled(true);
		//if(this.just_answer_field!=null) this.just_answer_field.setEnabled(true);
		if(this.news_body_field!=null) this.news_body_field.setEnabled(true);
		if(this.news_submit_field!=null) this.news_submit_field.setEnabled(true);
		if(this.date_field!=null) this.date_field.setEnabled(true);
		if(this.dategen_field!=null) this.dategen_field.setEnabled(true);
	}
	public NewsEditor(){
		org_label.addMouseListener(this);
		mot_label.addMouseListener(this);
		this.setLayout(new GridBagLayout());
		int y[]={0};
		makeGeneralPanel(y);//this
		makeHandlingPanel(this, y);
		disable_it();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		//this.setFillsViewportHeight(true);
		return scrollPane;
	}
	public void setJust(String _justID, boolean force){
		if(DEBUG) out.println("JustEditor:setJust: force="+force);
		if((news_ID !=null) && (news_ID.equals(_justID)) && !force){
			if(DEBUG) out.println("JustEditor:setJust: justID="+news_ID);
			return;
		}
		if((news_ID==null) && (_justID==null)){
			if(DEBUG) out.println("JustEditor:setJust: _justID="+news_ID);
			return;
		}
		if(_justID==null) {
			if(DEBUG) out.println("JustEditor:setJust: _ justID null "+force);
			disable_it(); return;
		}
		news_ID = _justID;
		if(!force && enabled){
			if(DEBUG) out.println("JustEditor:setJust: !force="+force);
			disable_it();
		}
		update_it(force);
		//if(this.extraFields!=null)	this.extraFields.setCurrent(_justID);
		if(DEBUG) out.println("NewsEditor:setJust: exit");
	}
	/**
	 * Is this org editable?
	 * @return
	 */
	private boolean editable() {
		if(DEBUG) out.println("JustEditor:editable: start");
		if(d_news == null){
			if(DEBUG) out.println("JustEditor:editable: no just");
			return false;
		}
		if(d_news.isEditable()){
			if(DEBUG) out.println("JustEditor:editable");
			return true;
		}
		if(DEBUG) out.println("JustEditor:editable: exit "+d_news);
		return false;
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and load it with "data"
	 * @param _NEW_FORMAT
	 * @param data
	 */
	public void setMode(String _NEW_FORMAT, String data){
		this.d_news.news.setDocumentString(data);
		this.d_news.news.setFormatString(_NEW_FORMAT);
		
		this.news_body_field.removeListener(this);
		news_body_field.getComponent().setVisible(false);
		this.news_body_field.setType(d_news.news.getFormatString());
		this.news_body_field.setText(d_news.news.getDocumentString());
		
		news_body_field.getComponent().setVisible(true);
	    //this.panel_body.removeAll();
	    //this.panel_body.add(motion_body_field.getComponent());
		this.news_body_field.setEnabled(enabled);
		this.news_body_field.addListener(this);
	}
	/**
	 * Set the editor in a given mode (HTMLEditor/TXT/PDFViewer)
	 * and convert current data to it
	 * @param _NEW_FORMAT
	 */
	public void switchMode(String _NEW_FORMAT){
		String data = "";
		data = this.d_news.news.convertTo(_NEW_FORMAT);
		if(data==null) return;
		setMode(_NEW_FORMAT, data);
	}
	/**
	 * Will reload org from database
	 * returns whether to edit
	 */
	private boolean reloadNews(boolean force){
		if(DEBUG) out.println("NewsEditor:reloadNews: start force="+force+" newsID="+news_ID);
		if(news_ID==null) {
			d_news = null;
			return true;
		}
		try {
			long _newsID = new Integer(news_ID).longValue();
			if(_newsID>0){
				d_news = new D_News(_newsID);
				organization_ID = Util.lval(d_news.organization_ID, -1);
				motion_ID = Util.lval(d_news.motion_ID, -1);
				if(DEBUG) out.println("NewsEditor:reloadNews: news = "+d_news);
			}
		} catch (Exception e) {
			e.printStackTrace();
			disable_it();
			return false;
		}
		if (!editable()){
			if(DEBUG) out.println("JustEditor:reloadNews: quit not editable");
			return force || DD.EDIT_RELEASED_JUST;
		}
		if(DEBUG) out.println("NewsEditor:reloadNews: exit");
		return true;
	}
	boolean update_it(boolean force) {
		forced = force;
		if(DEBUG) out.println("JustEditor:updateit: start");
		if(reloadNews(force))
			enable_it();
		//else return false; // further processing changes arrival_date by handling creator and default_scoring fields
		if(d_news == null){
			if(DEBUG) out.println("JustEditor:updateit: quit null just");
			return false;
		}
		m_editable = editable(); // editable?

		try {
			if (organization_ID > 0) org_label.setText(D_Organization.getOrgByLID_NoKeep(organization_ID, true).getName());
			else org_label.setText("");
			if (motion_ID > 0) mot_label.setText(D_Motion.getMotiByLID(motion_ID, true, false).getMotionTitle().title_document.getDocumentUTFString());
			else mot_label.setText("");
		}catch(Exception e){e.printStackTrace();}
		
		requested.removeItemListener(this);
		requested.setSelected(d_news.requested);
		requested.addItemListener(this);

		blocked.removeItemListener(this);
		blocked.setSelected(d_news.blocked);
		blocked.addItemListener(this);

		broadcasted.removeItemListener(this);
		broadcasted.setSelected(d_news.broadcasted);
		broadcasted.addItemListener(this);

		date_field.getDocument().removeDocumentListener(this);
		date_field.setText(Encoder.getGeneralizedTime(d_news.creation_date));
		date_field.getDocument().addDocumentListener(this);

		String sql =
			"SELECT "+net.ddp2p.common.table.news.title+
			","+net.ddp2p.common.table.news.global_news_ID+
			","+net.ddp2p.common.table.news.news_ID+
			" FROM "+net.ddp2p.common.table.news.TNAME+
			" WHERE "+net.ddp2p.common.table.news.news_ID+"=?;";
		try {
			ArrayList<ArrayList<Object>> j = Application.db.select(sql, new String[]{d_news.news_ID}, DEBUG);
			combo_answerTo = new NewsGIDItem[j.size()];
			int k=0;
			for (ArrayList<Object> _j :j){
				String gid = Util.getString(_j.get(1));
				String id = Util.getString(_j.get(2));
				String name = Util.getString(_j.get(0));
				combo_answerTo[k++] = new NewsGIDItem(gid, id, name);
			}
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		

		//just_answer_field.setSelected("1".equals(Util.getString(org.get(table.organization.ORG_COL_REQUEST))));
		//just_answer_field.removeItemListener(this);
		//just_answer_field.removeAllItems();
		/*
		NewsGIDItem sel=null;
		if(just.answerTo_ID!=null) {
			//String answer_title;
			if(just.answerTo == null) {
				try {
					just.answerTo = new D_News(new Integer(just.answerTo_ID).longValue());
				} catch (NumberFormatException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					just.answerTo_ID = null;
				}
			}
			if(just.answerTo == null) {
				//answer_title = just.answerTo.news_title.title_document.getDocumentString();
				//just_answer_field = new JComboBox(combo_answerTo);
				//just_answer_field.addItem(new JustGIDItem(just.answerTo.global_answerTo_ID,just.answerTo.answerTo_ID,answer_title));
			}
		}
		
		for(NewsGIDItem i : combo_answerTo){
			just_answer_field.addItem(i);
			if(i.id.equals(d_news.enhanced_newsID)){ sel = i;}
		}
		if(sel!=null)just_answer_field.setSelectedItem(sel);
		just_answer_field.addItemListener(this);
		*/
		this.news_body_field.removeListener(this);
		this.news_body_field.setText(d_news.news.getDocumentString());
		this.news_body_field.addListener(this);
		
		this.news_title_field.getDocument().removeDocumentListener(this);
		this.news_title_field.setText(d_news.title.title_document.getDocumentString());
		this.news_title_field.getDocument().addDocumentListener(this);
		
		
		return true;
	}
	
	private JSplitPane makeGeneralPanel(int _y[]) { 
    	
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
    	
		//NewsEditor p = this; 
		int y = _y[0];
    	p.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_org = new TranslatedLabel("Organization");
		p.add(label_org, c);
		c.gridx = 0; c.gridy = y++;		
//		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(org_label,c);
		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_mot = new TranslatedLabel("Motion");
		p.add(label_mot, c);
		c.gridx = 0; c.gridy = y++;		
//		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(mot_label,c);
		
		JPanel p_t = new JPanel();
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p_t.add(label_just_title);
		p_t.add(news_title_field = new JTextField(TITLE_LEN));
		news_title_field.getDocument().addDocumentListener(this);
		p2.add(p_t,t);

		/*
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p.add(label_just_title, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(news_title_field = new JTextField(TITLE_LEN),c);
		//title_field.addActionListener(this); //name_field.addFocusListener(this);
		news_title_field.getDocument().addDocumentListener(this);
		*/

		/*
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_body = new TranslatedLabel("News");
		p.add(label_just_body, c);
		
//		c.gridx = 1;
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		news_body_field = new DocumentEditor();
		
		news_body_field.init(TEXT_LEN_ROWS);
		news_body_field.addListener(this);
		p.add(panel_body, c);
		panel_body.add(news_body_field.getComponent());
		

		news_body_field.setLineWrap(true);
		news_body_field.setWrapStyleWord(true);
		news_body_field.setAutoscrolls(true);
		news_body_field.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		news_body_field.getDocument().addDocumentListener(this);
		
		p.add(news_body_field, c);
		 */
		
		/*
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		p.add(label_answer_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		*/
		//p.add(just_answer_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		//just_answer_field.addItemListener(this);
		
		String creation_date = Util.getGeneralizedTime();
		date_field = new JTextField(creation_date);
		date_field.setColumns(creation_date.length());
		
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		TranslatedLabel label_date = new TranslatedLabel("Creation Date");
		p.add(label_date, c);
//		c.gridx = 1;
		c.gridx = 0; c.gridy = y++;		
		c.anchor = GridBagConstraints.WEST;
		//hash_org.creation_date = creation_date;
		p.add(date_field,c);
		date_field.setForeground(Color.GREEN);
		//name_field.addActionListener(this); //name_field.addFocusListener(this);
		date_field.getDocument().addDocumentListener(this);
		
		c.gridx = 0; c.gridy = y++;		
//		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(dategen_field = new JButton(__("Set Current Date")),c);
		c.anchor = GridBagConstraints.EAST;
		dategen_field.addActionListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		//TranslatedLabel label_submit_just = new TranslatedLabel("Submit");
		//p.add(label_submit_just, c);
//		c.gridx = 1;
		//c.gridx = 0; c.gridy = y++;		
		//c.anchor = GridBagConstraints.WEST;
		p.add(news_submit_field = new JButton(__("Submit News")),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		news_submit_field.addActionListener(this);

		
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
		c.gridx = 0; c.gridy = y++;	 //c.gridwidth=2;

		
		news_body_field = new DocumentEditor();
		news_body_field.init(TEXT_LEN_ROWS);
		news_body_field.addListener(this);
		t.gridx = 0; t.gridy = 1;
		t.anchor = GridBagConstraints.WEST;
		t.fill = GridBagConstraints.BOTH;
		p2.add(panel_body,t);//, c);

		news_body_field.getComponent(D_Document.RTEDIT).setVisible(false);
		news_body_field.getComponent(D_Document.TEXTAREA).setVisible(false);
		news_body_field.getComponent(D_Document.PDFVIEW).setVisible(false);
		panel_body.add(news_body_field.getComponent(D_Document.TEXTAREA));
		panel_body.add(news_body_field.getComponent(D_Document.RTEDIT));
		panel_body.add(news_body_field.getComponent(D_Document.PDFVIEW));
		
		news_body_field.setType(D_Document.DEFAULT_FORMAT);
		news_body_field.getComponent(D_Document.DEFAULT_EDITOR).setVisible(true);

		
		/*
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
		*/
		_y[0] = y;
		//max_general_fields = 5;
		this.add(sp);
		return sp;
	}
	
	private JPanel _makeGeneralPanel(int _y[]) { 
		NewsEditor p = this; 
		int y = _y[0];
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_org = new TranslatedLabel("Organization");
		p.add(label_org, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(org_label,c);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_mot = new TranslatedLabel("Motion");
		p.add(label_mot, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(mot_label,c);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_title = new TranslatedLabel("Title");
		p.add(label_just_title, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(news_title_field = new JTextField(TITLE_LEN),c);
		//title_field.addActionListener(this); //name_field.addFocusListener(this);
		news_title_field.getDocument().addDocumentListener(this);
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_just_body = new TranslatedLabel("News");
		p.add(label_just_body, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		news_body_field = new DocumentEditor();
		
		news_body_field.init(TEXT_LEN_ROWS);
		news_body_field.addListener(this);
		p.add(panel_body, c);
		panel_body.add(news_body_field.getComponent());
		/*

		news_body_field.setLineWrap(true);
		news_body_field.setWrapStyleWord(true);
		news_body_field.setAutoscrolls(true);
		news_body_field.setRows(TEXT_LEN_ROWS);
		//result.setMaximumSize(new java.awt.Dimension(300,100));
		news_body_field.getDocument().addDocumentListener(this);
		
		p.add(news_body_field, c);
		 */
		
		/*
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		TranslatedLabel label_answer_just = new TranslatedLabel("Answer To");
		p.add(label_answer_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		*/
		//p.add(just_answer_field = new JComboBox(combo_answerTo),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		//just_answer_field.addItemListener(this);
		
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
		
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0; c.gridy = y++;		
		//TranslatedLabel label_submit_just = new TranslatedLabel("Submit");
		//p.add(label_submit_just, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(news_submit_field = new JButton(__("Submit News")),c);
		//p.add(just_answer_field = new JTextField(TITLE_LEN),c);
		//just_answer_field.getDocument().addDocumentListener(this);
		news_submit_field.addActionListener(this);

		/*
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
		*/
		_y[0] = y;
		//max_general_fields = 5;
		return p;
	}
	private JComponent makeHandlingPanel(JPanel p, int _y[]) {
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

	@Override
	public void newsUpdate(String newsID, int col) {
		if(DEBUG)System.out.println("JustEditor: justUpdate: col: "+col);
		this.setJust(newsID, false);
	}
	@Override
	public void org_forceEdit(String justID, D_Organization org) {
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
		//boolean DEBUG = true;
		if(DEBUG)System.out.println("JustEditor: handleFieldEvent: enter enabled="+enabled);
		if(DEBUG)System.out.println("JustEditor: handleFieldEvent: enter news="+d_news);
		
		if(this.d_news==null){
			Util.printCallPath("No news object selected! Will create one.");
			this.d_news = new D_News();
			d_news.organization = this.organization;
			d_news.organization_ID = Util.getStringID(this.organization_ID);
			d_news.motion_ID = Util.getStringID(this.motion_ID);
		}
		
		if(this.broadcasted == source) {
			boolean val = broadcasted.isSelected();
			NewsModel.setBroadcasting(this.news_ID, val);
		}
		if(this.blocked == source) {
			boolean val = blocked.isSelected();
			NewsModel.setBlocking(this.news_ID, val);
		}
		if(this.requested == source) {
			boolean val = requested.isSelected();
			NewsModel.setRequested(this.news_ID, val);
		}
		if(!enabled) return;
		//String currentTime = Util.getGeneralizedTime();
		String creationTime = date_field.getText();

		if((this.news_body_field==source)||(this.news_body_field.getDocumentSource()==source)) {
			if(DEBUG) out.println("JustEditor:handleFieldEvent: just body");
			String new_text = this.news_body_field.getText();
			this.d_news.news.setDocumentString(new_text);
			this.d_news.news.setFormatString(BODY_FORMAT);
			this.d_news.creation_date = Util.getCalendar(creationTime);
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}

		if((this.news_title_field==source)||(this.news_title_field.getDocument()==source)) {
			if(DEBUG) out.println("NewsEditor:handleFieldEvent: news title");
			String new_text = this.news_title_field.getText();
			this.d_news.title.title_document.setDocumentString(new_text);
			this.d_news.title.title_document.setFormatString(TITLE_FORMAT);
			this.d_news.creation_date = Util.getCalendar(creationTime);
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}

		if((this.date_field==source)||(this.date_field.getDocument()==source)) {
			if(DEBUG) out.println("NewsEditor:handleFieldEvent: date title");
			String new_text = this.date_field.getText();
			Calendar cal = Util.getCalendar(new_text);
			if(cal == null) return;
			this.d_news.creation_date = cal;
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		if(this.dategen_field==source) {
			this.d_news.creation_date = Util.CalendargetInstance();
			this.date_field.setText(Encoder.getGeneralizedTime(this.d_news.creation_date));
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return;			
		}
		/*
		if(just_answer_field==source) {
			if(DEBUG) out.println("JustEditor:handleFieldEvent: creator");
			if(DEBUG) Util.printCallPath("Linux tracing");
			NewsGIDItem selected = (NewsGIDItem) just_answer_field.getSelectedItem();
			String id = null;
			try {
				if(selected == null){
					id = null;
				}else{
					if(DEBUG) out.println("JustEditor:handleFieldEvent: selected just answer="+selected.toStringDump());
					if(selected.id == null) id = null;
					else id = ""+(new Integer(selected.id).longValue());
				}
				
				d_news.enhanced_newsID = id;
				this.d_news.creation_date = Util.getCalendar(creationTime);
				this.d_news.setEditable();
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return;
		}
		*/
		if((news_submit_field == source)) {
			try {
				d_news.organization_ID = Util.getStringID(this.organization_ID);
				d_news.motion_ID = Util.getStringID(this.motion_ID);
				d_news.global_organization_ID = null;
				d_news.global_motion_ID = null;
				this.d_news.global_news_ID = this.d_news.make_ID();
				this.d_news.sign();
				long id = this.d_news.storeVerified();
				if(id<=0) return;
				disable_it();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		
		if(this.setTxtMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setText");
			switchMode(D_Document.TXT_BODY_FORMAT);
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");

			this.d_news.creation_date = Util.getCalendar(creationTime);
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(this.setHTMMode==source) {
			if(DEBUG)System.err.println("MotionEditor:handleFieldEvent: setHTM");
			switchMode(D_Document.HTM_BODY_FORMAT);
			if(DEBUG) System.out.println("MotionEditor:handleFieldEvent: done");

			this.d_news.creation_date = Util.getCalendar(creationTime);
			this.d_news.setEditable();
			try {
				this.d_news.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
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
	            				if(_DEBUG) System.out.println("MotionEditor: getText: bin size="+file.length()+" vs "+DocumentEditor.MAX_PDF);
	            				Application_GUI.warning(__("File too large! Current Limit:"+" "+file.length()+"/"+DocumentEditor.MAX_PDF),
	            						__("Document too large for import!"));
	            				return;
	            			}
	            			byte bin[] = new byte[(int)file.length()];
	            			int off = 0;
	            			do{
	            				int cnt = in.read(bin, off, bin.length-off);
	            				if(cnt == -1) {
	            					if(_DEBUG) System.out.println("MotionEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            					break;
	            				}
	            				off +=cnt;
            					if(_DEBUG) System.out.println("MotionEditor: getText: crt="+cnt+" off="+off+"/"+bin.length);
	            			}while(off < bin.length);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: bin size="+bin.length);
	            			String data = Util.stringSignatureFromByte(bin);
	            			if(DEBUG) System.out.println("DocumentEditor: handle: txt size="+data.length());
	            			
	            			setMode(D_Document.PDF_BODY_FORMAT, data);
	            			
	            			if(DEBUG) System.out.println("DocumentEditor: handle: done");

	            			this.d_news.creation_date = Util.getCalendar(creationTime);
	            			this.d_news.setEditable();
	            			try {
	            				this.d_news.storeVerified();
	            			} catch (P2PDDSQLException e) {
	            				e.printStackTrace();
	            			}
	            			
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
								
								if(DEBUG) System.out.println("DocumentEditor: handle: done");

		            			this.d_news.creation_date = Util.getCalendar(creationTime);
		            			this.d_news.setEditable();
		            			try {
		            				this.d_news.storeVerified();
		            			} catch (P2PDDSQLException e) {
		            				e.printStackTrace();
		            			}
	            			}catch(Exception e){
	            				e.printStackTrace();
	            			}
	            		}else
	            			if(("txt".equals(ext))){
	            				try{
		            				BufferedReader bri = new BufferedReader(new FileReader(file));
									String data = Util.readAll(bri);
									
									setMode(D_Document.TXT_BODY_FORMAT, data);
									
			            			if(DEBUG) System.out.println("DocumentEditor: handle: done");
	
			            			this.d_news.creation_date = Util.getCalendar(creationTime);
			            			this.d_news.setEditable();
			            			try {
			            				this.d_news.storeVerified();
			            			} catch (P2PDDSQLException e) {
			            				e.printStackTrace();
			            			}
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


		
		if(DEBUG) out.println("JustEditor:handleFieldEvent: exit");
		if(DEBUG) out.println("*****************");
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		motion_ID = Util.lval(motID, -1);
		update_it(forced);
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		organization_ID = Util.lval(orgID, -1);
		this.organization = org;
		update_it(forced);
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		jtableMouseReleased(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		jtableMouseReleased(e);
	}
	JPopupMenu getPopup(int row, int col){
		JMenuItem menuItem;    	
    	ImageIcon addicon = DDIcons.getAddImageIcon(__("add an item")); 
    	ImageIcon delicon = DDIcons.getDelImageIcon(__("delete an item")); 
    	ImageIcon reseticon = DDIcons.getResImageIcon(__("reset item"));
    	JPopupMenu popup = new JPopupMenu();
    	NewsEditorCustomAction aAction;
    	
    	aAction = new NewsEditorCustomAction(this, __("Delete Organization Restriction!"), delicon,__("Delete organization restriction."), __("Delete Organization Restriction"),KeyEvent.VK_O, NewsEditorCustomAction.M_DELORG);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	aAction = new NewsEditorCustomAction(this, __("Delete Motion Restriction!"), delicon,__("Delete motion restriction."), __("Delete Motion Restriction"),KeyEvent.VK_M, NewsEditorCustomAction.M_DELMOT);
    	aAction.putValue("row", new Integer(row));
    	menuItem = new JMenuItem(aAction);
    	popup.add(menuItem);
    	return popup;
	}
    private void jtableMouseReleased(java.awt.event.MouseEvent evt) {
    	int row = (int) this.organization_ID; //=this.getSelectedRow();
    	int col = (int) this.motion_ID; //=this.getSelectedColumn();
    	if(!evt.isPopupTrigger()) return;
    	//if ( !SwingUtilities.isLeftMouseButton( evt )) return;
    	//Point point = evt.getPoint();
    	JPopupMenu popup = getPopup(row,col);
    	if(popup == null) return;
    	popup.show((Component)evt.getSource(), evt.getX(), evt.getY());
    }
	@Override
	public void mouseClicked(MouseEvent arg0) {
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {
	}
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	@Override
	public void motion_forceEdit(String justID) {
		// TODO Auto-generated method stub
		
	}

}


@SuppressWarnings("serial")
class NewsEditorCustomAction extends DebateDecideAction {
    public static final int M_DELORG = 1;
    public static final int M_DELMOT = 2;
	private static final boolean DEBUG = false;
    private static final boolean _DEBUG = true;
	NewsEditor tree; ImageIcon icon; int cmd;
    public NewsEditorCustomAction(NewsEditor tree,
			     String text, ImageIcon icon,
			     String desc, String whatis,
			     Integer mnemonic, int cmd) {
        super(text, icon, desc, whatis, mnemonic);
        this.tree = tree; this.icon = icon; this.cmd = cmd;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.out.println("NECAction: start");
    	Object src = e.getSource();
    	JMenuItem mnu;
    	int row =-1;
    	String org_id=null;
    	if(src instanceof JMenuItem){
    		mnu = (JMenuItem)src;
    		Action act = mnu.getAction();
    		row = ((Integer)act.getValue("row")).intValue();
    	}
     	
    	if(DEBUG) System.out.println("NewsECAction: row = "+row);
    	//do_cmd(row, cmd);
    	if(cmd == M_DELORG){
        	if(DEBUG) System.out.println("NewsECAction: start DEL ORG");
        	tree.organization_ID = -1;
        	tree.motion_ID = -1;
        	tree.d_news.organization_ID = null;
        	tree.d_news.global_organization_ID = null;
        	tree.d_news.organization = null;
        	tree.d_news.motion_ID = null;
        	tree.d_news.global_motion_ID = null;
        	tree.d_news.motion = null;
        	tree.mot_label.setText("");
        	tree.org_label.setText("");
        	try {
				tree.d_news.storeVerified();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
        	tree.update_it(tree.forced);
     	}
    	if(cmd == M_DELMOT){
        	if(DEBUG) System.out.println("NewsECAction: start DEL MOT");
        	tree.motion_ID = -1;
        	tree.d_news.motion_ID = null;
        	tree.d_news.global_motion_ID = null;
        	tree.d_news.motion = null;
        	tree.mot_label.setText("");
        	try {
				tree.d_news.storeVerified();
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
        	tree.update_it(tree.forced);
     	}
    }
}

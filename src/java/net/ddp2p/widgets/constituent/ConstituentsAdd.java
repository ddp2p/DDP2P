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
 package net.ddp2p.widgets.constituent;
import static net.ddp2p.common.util.Util.__;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.CipherSuit;
import net.ddp2p.ciphersuits.Cipher_Sizes;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.components.CipherSelection;
import net.ddp2p.widgets.components.DocumentEditor;
import net.ddp2p.widgets.components.TranslatedLabel;
/**
 * A class to store data associate with the constituentAdd Dialog
 * @author silaghi
 *
 */
class ConstituentAddData{
	String valueEditor[];
	String lov[];
	String tip[];
	String label[];
	String label_lang[];
	int partNeigh[];
	String gnEditor, snEditor, emailEditor;
	String pictureImage;
	String witness_category;
	long fieldID[];
	int last_neighbor_idx=-1;
	String _subdivisions;
	TreePath tp;
	public boolean sign;
	public String witness_category_trustworthiness;
	public String ciphersuit;
	public int ciphersize;
	public String hash_alg;
	public Object weight;
	
	String subdivisions(){
		String result=":";
		for (int k=valueEditor.length-1;k>=0; k--) {
			if(partNeigh[k]<1) continue;
			if((valueEditor[k]==null)||("".equals(valueEditor[k]))) continue;
			result = result+label[k]+":";//valueEditor[k];
			last_neighbor_idx = k;
		} 
		_subdivisions = result;
		return result;
	}
	/**
	 * Should return a list of languages known by the user, in the order of the preferences.
	 * Currently just returns the language of the first label
	 * @return
	 */
	public String[] getLanguages() {
		if(label_lang==null) return new String[]{"en"};
		if(label_lang.length==0) return new String[]{"en"};
		String result[] = new String[1];
		result[0] = label_lang[0];
		if(null==result[0]) result[0] = "en";
		if("".equals(result[0])) result[0] = "en";
		return result;
	}
	public String getFieldValueLanguage(int k) {
		if(label_lang==null) return null;
		if(label_lang.length<k+1) return null;
		String result = label_lang[k];
		return result;
	}
}
@SuppressWarnings({ "unchecked", "serial" })
public class ConstituentsAdd extends JDialog {
    private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int INDEX_WITNESS_MYSELF = 2;
	public boolean myself;
	private static final int[] witness_categories_sense = new int[]{D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.UNKNOWN,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE};
	private static final String[] witness_categories=new String[]{
		__("Eligibility personally known"),
		__("Hearsay eligibility"),
		__("Myself (eligible)"),
		__("Unknown"),
		__("Myself (ineligible)"),
		__("Hearsay ineligibility"),
		__("Ineligibility personally known")
		};
	private static final int INDEX_WITNESS_TRUSTWORTHINESS_MYSELF = 2;
	private static final int[] witness_categories_trustworthiness_sense = new int[]{D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.UNKNOWN,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE};
	private static final String[] witness_categories_trustworthiness=new String[]{
		__("Trust personally inferred"),
		__("Hearsay trust"),
		__("Myself"),
		__("Unknown"),
		__("Hearsay distrust"),
		__("Distrust personally inferred")
		};
	public static final Hashtable<String,Integer> sense_eligibility = init_sense_eligibility();
	public static final Hashtable<String,Integer> sense_trustworthiness = init_sense_trustworthiness();
	private static Hashtable<String, Integer> init_sense_eligibility() {
		Hashtable<String,Integer> result = new Hashtable<String,Integer>();
		for(int i=0; i<witness_categories.length; i++)result.put(witness_categories[i], witness_categories_sense[i]);
		return result;
	}
	private static Hashtable<String, Integer> init_sense_trustworthiness() {
		Hashtable<String,Integer> result = new Hashtable<String,Integer>();
		for(int i=0; i<witness_categories_trustworthiness.length; i++)
			result.put(witness_categories_trustworthiness[i], witness_categories_trustworthiness_sense[i]);
		return result;
	}
	@SuppressWarnings("unchecked")
	public JComboBox witness_category =
			new JComboBox(witness_categories);

	@SuppressWarnings("unchecked")
	public JComboBox witness_category_trustworthiness =
			new JComboBox(witness_categories_trustworthiness);

	String getText(JComponent com) {
    	String value;
    	if(com==null) return null;
		if(com instanceof JTextField)
			value = ((JTextField)com).getText();
		else{
			Object sel = ((JComboBox)com).getSelectedItem();
			if(sel==null) return null;
			value = sel.toString();
		}
		return value;
    }
	ConstituentAddData getConstituentAddData(){
		ConstituentAddData cad = new ConstituentAddData();
		cad.label=label;
		cad.label_lang=label_lang;
		cad.partNeigh=partNeigh;
		cad.witness_category = Util_GUI.getJFieldText(witness_category);
		if(DD.CONSTITUENTS_ADD_ASK_TRUSTWORTHINESS)
			cad.witness_category_trustworthiness = Util_GUI.getJFieldText(witness_category_trustworthiness);
		else cad.witness_category_trustworthiness = null;
		cad.valueEditor=new String[valueEditor.length]; 
		for(int k=0;k<valueEditor.length;k++)
			cad.valueEditor[k]=getText(valueEditor[k]);
		cad.gnEditor=getText(gnEditor);
		cad.snEditor=getText(snEditor);
		cad.emailEditor=getText(emailEditor);
		cad.fieldID = fieldID;
		cad.tip=tip;
		cad.lov=lov;
		cad.tp=tp;
		cad.pictureImage = pictureImage;
		if(signEditor !=null) cad.sign = signEditor.isSelected();
		else cad.sign = true;
		cad.subdivisions();
		CipherSuit cs = this.cipherSuite.getSelectedCipherSuite();
		cad.ciphersuit = cs.cipher;
		cad.ciphersize = cs.ciphersize;
		cad.hash_alg = cs.hash_alg;
		/*
		cad.ciphersuit = (String) this.crt_cipher.getSelectedItem();
		if(cad.ciphersuit != null) {
			if (this.crt_sizes_int.isVisible()) {
				cad.ciphersize = getCrtCipherSize();
			}
			else if (this.crt_sizes_list.isVisible()) {
				Object val = this.crt_sizes_list.getSelectedItem();
				if (val != null) {
					cad.ciphersize = getCrtCipherSize((String)val);
				}
			}
			
			Object hash = this.crt_hash_algos.getSelectedItem();
			cad.hash_alg = (String) hash;
//			if (hash != null) {
//				cad.ciphersuit = Cipher.buildCiphersuitID(cad.ciphersuit, (String)hash);
//			}
			
		}
		*/
		if (weight != null) { // && org.hasWeights()) {
			cad.weight = "" + Util.ival(weight.getValue(), 0);
		}
		return cad;
	}
	
	private int getCrtCipherSize(String val) {
		int result = 0;
		String txt = val;
		try{
			result = Integer.parseInt(txt);
			return result;
		}catch(Exception e) {
		}
		try{
			int idx = txt.indexOf("-");
			result = Integer.parseInt(txt.substring(idx+1));
			return result;
		}catch(Exception e) {
		}
		return 0;
	}
	/*
	private int getCrtCipherSize() {
		try{
			return Integer.parseInt(this.crt_sizes_int.getText());
		}catch(Exception e) {
			return 0;
		}
	}
	*/
	ConstituentsModel model;
	ConstituentsTree tree;
	ConstituentsAdd dialog=this;
	TreePath tp;
	JButton ok;
	JLabel jpl=new JLabel();
	//String jpl_name = null;
	TranslatedLabel tpl;
	JComponent[] valueEditor = new JComponent[0];  // list of components
	JCheckBox signEditor;
	JTextField gnEditor, snEditor, emailEditor;
	GridBagConstraints c = new GridBagConstraints();
	JPanel panel = new JPanel();
	String pictureImage=null;
	boolean can_be_provided_later[];
	boolean certificated[];
	String default_val[];
	int entry_size[];
	long fieldID[];
	String label[];
	String lov[];
	int partNeigh[];
	boolean required[];
	String tip[];
	String label_lang[];
	TranslatedLabel tl[];
	public boolean accepted = false;
	private DocumentEditor instr_reg;
	/*
	private JComboBox<String> crt_cipher;
	private JComboBox<String> crt_sizes_list;
	private JTextField crt_sizes_int;
	private JComboBox<String> crt_hash_algos;
	*/
	CipherSelection cipherSuite;
	private JSpinner weight;
	
	long ival(Object obj, long _default){
		if(obj == null) return _default;
		return Long.parseLong(obj.toString());
	}
	String sval(Object obj, String _default){
		if(obj == null) return _default;
		return obj.toString();
	}
	int getIndex(String[] items, String val){
		for (int i=0; i<items.length; i++) if(items[i].equals(val)) return i;
		return -1;
	}
	/*
	void setSelectedCipher(String cipher) {
		crt_hash_algos.removeAllItems();
		crt_sizes_list.removeAllItems();
		Cipher_Sizes cs = ciphersuits.Cipher.getAvailableSizes(cipher);
		if (cs == null) {
			System.out.println("ConstituentsAdd:setSelectedCipher No sizes for: "+cipher+" cs="+cs);
			crt_sizes_list.setVisible (false);
			crt_sizes_int.setVisible (false);
		}
		else if (cs.type == Cipher_Sizes.INT_RANGE) {
			System.out.println("ConstituentsAdd:setSelectedCipher int range for: "+cipher+" cs="+cs);
			crt_sizes_list.setVisible (false);
			crt_sizes_int.setVisible (true);
			crt_sizes_int.setText(cs._default+"");
		}
		else if (cs.type == Cipher_Sizes.LIST) {
			System.out.println("ConstituentsAdd:setSelectedCipher list range for: "+cipher+" cs="+cs);
			crt_sizes_int.setVisible (false);
			crt_sizes_list.setVisible (true);
			crt_sizes_list.removeAllItems();
			if (cs.range instanceof String[]) {
				String[] l = (String[]) cs.range;
				for (String k : l) {
					crt_sizes_list.addItem(k);
				}
				if ((cs != null) && (cs._default < l.length))
					crt_sizes_list.setSelectedIndex(cs._default);
			}
		}
	}
	void setSelectedSizes(String cipher, int sizes) {
		if(cipher == null) return;
		String[] ha = Cipher.getHashAlgos(cipher, sizes);
		if(ha == null) {
			System.out.println("ConstituentsAdd: No hashes for cipher: "+cipher+" sz="+sizes);
			return;
		}
		System.out.println("ConstituentsAdd:setSelectedSizes for cipher: "+cipher+" sz="+sizes+" got:"+ha.length);
		crt_hash_algos.removeAllItems();
		for(String h : ha) 
			crt_hash_algos.addItem(h);
		
		if((ha != null) && (ha.length > 0))
			crt_hash_algos.setSelectedIndex(0);
	}
	*/
	/**
	 * This creates the part of the add dialog that handles the static components of the constituent data:
	 *  - name, email, picture, (should sign?), cipher
	 * @return 
	 */
	int initStaticFields() {
		JButton bp;
		int y = 0;
		c.ipadx=10; c.gridx=0; c.gridy=y;c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
		//Language author_lang=new Language("en",null);

		if(!myself) {
			panel.add(new TranslatedLabel("Sign This Data?"),c);
			c.gridx = 1;
			panel.add(signEditor=new JCheckBox("("+__("Current Identity:")+" "+model.getConstituentMyselfNames()+")"),c);
			signEditor.setHorizontalTextPosition(SwingConstants.LEADING);
			y++; y++;
		} else {
			panel.add(new TranslatedLabel("Cipher-Suit"),c);
			c.gridx = 1;/*
			JPanel p_cipher = new JPanel();
			p_cipher.add(crt_cipher = new JComboBox<String>(ciphersuits.Cipher.getAvailableCiphers()));
			p_cipher.add(crt_sizes_list = new JComboBox<String>());
			p_cipher.add(crt_sizes_int = new JTextField());
			p_cipher.add(crt_hash_algos = new JComboBox<String>());
			
			crt_sizes_list.setVisible(false);
			crt_sizes_int.setVisible(false);
			
			crt_cipher.setSelectedItem(Cipher.getDefaultCipher());
			this.setSelectedCipher(Cipher.getDefaultCipher());
			if (crt_sizes_int.isVisible()) {
				this.setSelectedSizes(Cipher.getDefaultCipher(),
					Cipher.getAvailableSizes(Cipher.getDefaultCipher())._default);
			}
			else if (crt_sizes_list.isVisible()) {
				this.setSelectedSizes(Cipher.getDefaultCipher(),
						this.getCrtCipherSize((String)crt_sizes_list.getSelectedItem()));				
			}
			
			crt_cipher.addActionListener(this);
			crt_sizes_list.addActionListener(this);
			crt_sizes_int.getDocument().addDocumentListener(this);
			panel.add(p_cipher, c);
			*/
			panel.add(this.cipherSuite = new CipherSelection(), c);
			y++; y++;
		}
		
		D_Organization org = MainFrame.status.getSelectedOrg(); 
		if (org.hasWeights()) {
			SpinnerModel weight_model =
			        new SpinnerNumberModel(1, //initial value
			                               0, //min
			                               org.getWeightsMax(), //max
			                               1); // step
			weight = new JSpinner(weight_model);
			
			c.ipadx=10; c.gridx=0; 
			c.gridy=y;c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;		
			panel.add(new TranslatedLabel("Voting Shares"),c);
			c.gridx = 1;
			panel.add(weight,c);
			y++;
		}
		
		c.ipadx=10; c.gridx=0; 
		c.gridy=y;c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;		
		panel.add(new TranslatedLabel("Given Name"),c);
		c.gridx = 1;
		panel.add(gnEditor=new JTextField(50),c);
		
		y++;
		c.ipadx=10; c.gridx=0; c.gridy=y;c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new TranslatedLabel("Surname"),c);
		c.gridx = 1;
		panel.add(snEditor=new JTextField(50),c);
		
		y++;
		c.ipadx=10; c.gridx=0; c.gridy=y;c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new TranslatedLabel("Email"),c);
		c.gridx = 1;
		panel.add(emailEditor=new JTextField(50),c);
		
		y++;
		c.ipadx=10; c.gridx=0; c.gridy=y;c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(tpl=new TranslatedLabel("Picture"),c);
		panel.add(jpl, c);
		c.gridx = 1;
		panel.add(bp=new JButton(__("Browse For Picture File ...")),c);//new JFileChooser(),c);
		bp.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent evt){
					int returnVal = tree.fc.showOpenDialog(dialog);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = tree.fc.getSelectedFile();
						ImageIcon ii=null;
						try{
							pictureImage = file.getCanonicalPath();
							System.err.println("Icon="+pictureImage);
							ii = new ImageIcon(pictureImage);
						}catch(Exception e){
							e.printStackTrace();
						}
						Image img = ii.getImage();  
						Image newimg = img.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH);  
						ii = new ImageIcon(newimg);
						
						jpl.setIcon(ii);
						jpl.setText("");
						//c.ipadx=10; c.gridx=0; c.gridy=myself?3:4; 
						//c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.NONE;
						//panel.remove(tpl);
						tpl.setVisible(false);
						//panel.add(jpl,c);
						
						dialog.pack();
					}
				}
		});
		
		y++;
		c.ipadx=10; c.gridx=0; c.gridy=y; c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new TranslatedLabel("Eligible"),c);
		c.gridx = 1;
		panel.add(witness_category,c);
		
		if(DD.CONSTITUENTS_ADD_ASK_TRUSTWORTHINESS && !myself) {
			y++;
			c.ipadx=10; c.gridx=0; c.gridy=y; c.anchor = GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
			panel.add(new TranslatedLabel("Trusted"),c);
			c.gridx = 1;
			panel.add(witness_category_trustworthiness,c);
		}else{
			witness_category_trustworthiness.setSelectedIndex(INDEX_WITNESS_TRUSTWORTHINESS_MYSELF);
		}
		
		y++;
		return y;
	}
	/**
	 * create empty arrays to store data for vfields fields
	 * @param vfields
	 */
	void initArrays(int vfields){
		valueEditor = new JComponent[vfields];
		can_be_provided_later= new boolean[vfields];
		certificated=new boolean[vfields];
		default_val=new String[vfields];
		entry_size=new int[vfields];
		fieldID=new long[vfields];
		label=new String[vfields];
		lov=new String[vfields];
		partNeigh=new int[vfields];
		required=new boolean[vfields];
		tip=new String[vfields];
		label_lang=new String[vfields];
		tl=new TranslatedLabel[vfields];		
	}
	static final String sql_get_field_extras_props_from_org = "SELECT "+net.ddp2p.common.table.field_extra.some_field_extra +
					//" fe.OID, OID_name, " +
					" FROM "+net.ddp2p.common.table.field_extra.TNAME+" AS fe " +
					//" JOIN oids ON fe.OID = oids.OID " +
					" WHERE fe."+net.ddp2p.common.table.field_extra.organization_ID+"=? "+
					 "AND ( ( "+net.ddp2p.common.table.field_extra.partNeigh+ " ISNULL )" +
					" OR ( "+net.ddp2p.common.table.field_extra.partNeigh+" == "+net.ddp2p.common.table.field_extra.partNeigh_non_neighborhood_indicator+" ) " +
					" OR ( "+net.ddp2p.common.table.field_extra.partNeigh+" < "+net.ddp2p.common.table.field_extra.partNeigh_non_neighborhood_upper_indicator+" ) " +
		" );";
	/**
	 * Read the data in the fields that are not neighborhood parts
	 * @return
	 */
	ArrayList<ArrayList<Object>> getProperties(){
		ArrayList<ArrayList<Object>> fe = null;
		try {
			fe = model.db.select(sql_get_field_extras_props_from_org, new String[]{model.getOrganizationID()+""}, DEBUG);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		if (DEBUG) System.out.println("ConstituentsAdd: getProperties #"+fe.size());
		return fe;
	}
	/**
	 * Gets props
	 * @return
	 */	
	ArrayList<D_OrgParam> getPropertiesFromOrg() {
		D_Organization org = model.getOrganization();
		if (org == null) return null;// getProperties();
		ArrayList<D_OrgParam> r = org.getConstituentsPropertiesList();
		if (DEBUG) System.out.println("ConstituentsAdd: getPropertiesFromOrg "+r.size()+"\n from "+org);
		return r;
	}
	//@SuppressWarnings("unchecked")
	void initDynamicFields(int static_rows, long lastPN) {
		if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: enter static_rows="+static_rows+" last="+lastPN);
		D_Organization org = model.getOrganization();
		if (org == null) {
			if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: no org");
			this.initDynamicFieldsFromDB(static_rows, lastPN); 
			return;}
		ArrayList<D_OrgParam> fe = org.getConstituentFieldsListDec();
		initArrays(fe.size());
		if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: #fe="+fe.size());
		for(int k = 0; k < fe.size(); k++) {
			if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: k="+k+" fe="+fe.get(k));
				//String crt_label="";
				can_be_provided_later[k] = fe.get(k).can_be_provided_later;
				certificated[k] = fe.get(k).certificated;
				default_val[k] = fe.get(k).default_value;
				entry_size[k] = fe.get(k).entry_size;
				fieldID[k] = fe.get(k).field_LID;
				label[k] = fe.get(k).label;
				lov[k] = Util.concat(fe.get(k).list_of_values, net.ddp2p.common.table.organization.ORG_VAL_SEP, net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL_DEFAULT);
				partNeigh[k] = fe.get(k).partNeigh;
				required[k] = fe.get(k).required;
				tip[k] = fe.get(k).tip;
				label_lang[k] = fe.get(k).label_lang;
				Language lang = new Language(label_lang[k], null);
				if(DEBUG)System.out.println("Field: "+lang+" l="+label[k]+" f="+fieldID[k]+" pn="+partNeigh[k]+" lpn="+lastPN);
				tl[k]=new TranslatedLabel(label[k],lang);
				if((partNeigh[k] <=lastPN)&&(lastPN>=0)&&(partNeigh[k]>0)){
					if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: jump over: "+partNeigh[k]);
					if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: jump over: "+label[k]);
					continue;
				}
				c.ipadx=10;
				c.gridx=0; c.gridy=k+static_rows;c.anchor = GridBagConstraints.WEST;
				//if(required[k]) crt_label = "*";
				// panel.add(new JLabel(_(label[k])+crt_label),c);
				c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(tl[k],c);
				if(required[k]) {
					//tl[k].getEditor().getEditorComponent().setBackground(Color.lightGray);
					tl[k].getEditor().getEditorComponent().setForeground(Color.red);
					tl[k].getEditor().getEditorComponent().setFont(tl[k].getFont().deriveFont(Font.BOLD));
				}
				c.fill = GridBagConstraints.NONE;
				c.gridx=1; c.gridy=k+static_rows;c.anchor=GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
				if(lov[k].equals("")){
					panel.add(valueEditor[k]=new JTextField(entry_size[k]),c);
					((JTextField)valueEditor[k]).setText(default_val[k]);
				} else {
					String[] items=lov[k].split(net.ddp2p.common.table.field_extra.SEP_list_of_values,0);
					panel.add(valueEditor[k]=getValueCombo(items),c);
					int i = getIndex(items,default_val[k]);
					((JComboBox)valueEditor[k]).setSelectedIndex(i);
				}
				valueEditor[k].setToolTipText(tip[k]);
		}
		c.fill = GridBagConstraints.BOTH;
		c.gridx=0; c.gridy = fe.size()+static_rows;
		c.gridwidth = 2;
		instr_reg = new DocumentEditor();
		//D_Organization org = model.getOrganization();
		D_Document dd = new D_Document();
		dd.setDBDoc(org.params.instructions_registration);
		if (DEBUG) System.out.println("ConstituentsAdd: initDynFi: " + org.params.instructions_registration);
		instr_reg.setType(dd.getFormatString());
		instr_reg.setText(dd.getDocumentString());
		instr_reg.setEnabled(false);
		panel.add(instr_reg.getComponent(),c);
	}

	final static String sql_get_field_extras_org = "SELECT "+net.ddp2p.common.table.field_extra.some_field_extra+
			//" fe.OID, OID_name, " +
			" FROM "+net.ddp2p.common.table.field_extra.TNAME+" AS fe " +
			//" JOIN oids ON fe.OID = oids.OID " +
			" WHERE fe."+net.ddp2p.common.table.field_extra.organization_ID+"=? " +
					" ORDER BY "+net.ddp2p.common.table.field_extra.partNeigh+" DESC;";
	ArrayList<ArrayList<Object>> getFieldExtras(String orgLID) {
		ArrayList<ArrayList<Object>> fe=null;
		try {
			fe = model.db.select(sql_get_field_extras_org, new String[]{orgLID}, DEBUG);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		return fe;
	}
	/**
	 * Create the dynamic neiborhood fields in field_extra
	 * @param static_rows
	 * @param lastPN
	 */
	@SuppressWarnings("unchecked")
	void initDynamicFieldsFromDB(int static_rows, long lastPN){
		if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: enter static_rows="+static_rows+" last="+lastPN);
		ArrayList<ArrayList<Object>> fe = getFieldExtras(model.getOrganizationID()+"");
		initArrays(fe.size());
		for(int k=0; k<fe.size(); k++) {
				//String crt_label="";
				can_be_provided_later[k] = (Util.lval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_LATER),net.ddp2p.common.table.field_extra.SPARAM_LATER_DEFAULT)>0);
				certificated[k] = (Util.lval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_CERT),net.ddp2p.common.table.field_extra.SPARAM_CERT_DEFAULT)>0);
				default_val[k] = Util.sval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_DEFAULT),net.ddp2p.common.table.field_extra.SPARAM_DEFAULT_DEFAULT);
				entry_size[k] = Util.ival(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_SIZE),net.ddp2p.common.table.field_extra.SPARAM_SIZE_DEFAULT);
				fieldID[k] = Util.lval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_EXTRA_FIELD_ID),net.ddp2p.common.table.field_extra.SPARAM_EXTRA_FIELD_ID_DEFAULT);
				label[k] = Util.sval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_LABEL),net.ddp2p.common.table.field_extra.SPARAM_LABEL_DEFAULT);
				lov[k] = Util.sval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL),net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL_DEFAULT);
				partNeigh[k] = Util.ival(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_NEIGH),net.ddp2p.common.table.field_extra.SPARAM_NEIGH_DEFAULT);
				required[k] = (Util.lval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_REQ),net.ddp2p.common.table.field_extra.SPARAM_REQ_DEFAULT)>0);
				tip[k] = Util.sval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_TIP),net.ddp2p.common.table.field_extra.SPARAM_TIP_DEFAULT);
				label_lang[k] = Util.sval(fe.get(k).get(net.ddp2p.common.table.field_extra.SPARAM_LABEL_L),net.ddp2p.common.table.field_extra.SPARAM_LABEL_L_DEFAULT);
				Language lang = new Language(label_lang[k], null);
				if(DEBUG)System.out.println("Field: "+lang+" l="+label[k]+" f="+fieldID[k]+" pn="+partNeigh[k]+" lpn="+lastPN);
				tl[k]=new TranslatedLabel(label[k],lang);
				if((partNeigh[k] <=lastPN)&&(lastPN>=0)&&(partNeigh[k]>0)){
					if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: jump over: "+partNeigh[k]);
					if(DEBUG)System.out.println("ConstituentsAdd:initDynamicFields: jump over: "+label[k]);
					continue;
				}
				c.ipadx=10;
				c.gridx=0; c.gridy=k+static_rows;c.anchor = GridBagConstraints.WEST;
				//if(required[k]) crt_label = "*";
				// panel.add(new JLabel(_(label[k])+crt_label),c);
				c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(tl[k],c);
				if(required[k]) {
					//tl[k].getEditor().getEditorComponent().setBackground(Color.lightGray);
					tl[k].getEditor().getEditorComponent().setForeground(Color.red);
					tl[k].getEditor().getEditorComponent().setFont(tl[k].getFont().deriveFont(Font.BOLD));
				}
				c.fill = GridBagConstraints.NONE;
				c.gridx=1; c.gridy=k+static_rows;c.anchor=GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
				if(lov[k].equals("")){
					panel.add(valueEditor[k]=new JTextField(entry_size[k]),c);
					((JTextField)valueEditor[k]).setText(default_val[k]);
				} else {
					String[] items=lov[k].split(net.ddp2p.common.table.field_extra.SEP_list_of_values,0);
					panel.add(valueEditor[k]=getValueCombo(items),c);
					int i = getIndex(items,default_val[k]);
					((JComboBox)valueEditor[k]).setSelectedIndex(i);
				}
				valueEditor[k].setToolTipText(tip[k]);
		}
		c.fill = GridBagConstraints.BOTH;
		c.gridx=0; c.gridy = fe.size()+static_rows;
		c.gridwidth = 2;
		instr_reg = new DocumentEditor();
		D_Organization org = model.getOrganization();
		if(org==null) return;
		D_Document dd = new D_Document();
		dd.setDBDoc(org.params.instructions_registration);
		instr_reg.setType(dd.getFormatString());
		instr_reg.setText(dd.getDocumentString());
		instr_reg.setEnabled(false);
		panel.add(instr_reg.getComponent(),c);
		
	}
	/**
	 * Avoid a call with a warning
	 * @param items
	 * @return
	 */
	@SuppressWarnings("unchecked")
	JComboBox getValueCombo(String[] items){
		return new JComboBox(items);
	}
	/**
	 * create the dynamic properties (non-neigh=0) from a list of field_extras in fe. Basically replicates initDynamicFields
	 * except that it does not call select 
	 * @param static_rows
	 * @param sub_rows
	 * @param fe
	 */
	//@SuppressWarnings("unchecked")
	@SuppressWarnings("rawtypes")
	void initDynamicPropertiesFromDB(int static_rows, int sub_rows, ArrayList<ArrayList<Object>> fe){
		for(int i=0; i<fe.size(); i++) {
			if(DEBUG) System.out.println("ConstituentaAdd: initDynamicProperties: i="+i+" sub_rows="+sub_rows+" static="+static_rows+" fe="+fe.size()+" k="+ this.can_be_provided_later.length);
			int k=i+sub_rows;
			can_be_provided_later[k] = (Util.lval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_LATER),net.ddp2p.common.table.field_extra.SPARAM_LATER_DEFAULT)>0);
			certificated[k] = (Util.lval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_CERT),net.ddp2p.common.table.field_extra.SPARAM_CERT_DEFAULT)>0);
			default_val[k] = Util.sval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_DEFAULT),net.ddp2p.common.table.field_extra.SPARAM_DEFAULT_DEFAULT);
			entry_size[k] = Util.ival(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_SIZE),net.ddp2p.common.table.field_extra.SPARAM_SIZE_DEFAULT);
			fieldID[k] = Util.lval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_EXTRA_FIELD_ID),net.ddp2p.common.table.field_extra.SPARAM_EXTRA_FIELD_ID_DEFAULT);
			label[k] = Util.sval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_LABEL),net.ddp2p.common.table.field_extra.SPARAM_LABEL_DEFAULT);
			lov[k] = Util.sval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL),net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL_DEFAULT);
			partNeigh[k] = Util.ival(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_NEIGH),net.ddp2p.common.table.field_extra.SPARAM_NEIGH_DEFAULT);
			required[k] = (Util.lval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_REQ),net.ddp2p.common.table.field_extra.SPARAM_REQ_DEFAULT)>0);
			tip[k] = Util.sval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_TIP),net.ddp2p.common.table.field_extra.SPARAM_TIP_DEFAULT);
			label_lang[k] = Util.sval(fe.get(i).get(net.ddp2p.common.table.field_extra.SPARAM_LABEL_L),net.ddp2p.common.table.field_extra.SPARAM_LABEL_L_DEFAULT);
//			can_be_provided_later[k] = (Util.lval(fe.get(i).get(table.field_extra.SPARAM_LATER),0)>0);
//			certificated[k] = (Util.lval(fe.get(i).get(table.field_extra.SPARAM_CERT),0)>0);
//			default_val[k] = Util.sval(fe.get(i).get(table.field_extra.SPARAM_DEFAULT),"");
//			entry_size[k] = Util.ival(fe.get(i).get(table.field_extra.SPARAM_SIZE),50);
//			fieldID[k] = Util.lval(fe.get(i).get(table.field_extra.SPARAM_EXTRA_FIELD_ID),-1);
//			label[k] = Util.sval(fe.get(i).get(table.field_extra.SPARAM_LABEL),_("Unspecified field"));
//			lov[k] = Util.sval(fe.get(i).get(table.field_extra.SPARAM_LIST_VAL),"");
//			partNeigh[k] = Util.ival(fe.get(i).get(table.field_extra.SPARAM_NEIGH),0);
//			required[k] = (Util.lval(fe.get(i).get(table.field_extra.SPARAM_REQ),0)>0);
//			tip[k] = Util.sval(fe.get(i).get(table.field_extra.SPARAM_TIP),null);
//			label_lang[k] = Util.sval(fe.get(i).get(table.field_extra.SPARAM_LABEL_L),"en");
			Language lang = new Language(label_lang[k], null);
			if(DEBUG)System.out.println("Field: "+lang+" l="+label[k]+" f="+fieldID[k]+" pn="+partNeigh[k]);
			tl[k]=new TranslatedLabel(label[k],lang);
			c.ipadx=10;
			c.gridx=0; c.gridy=k+static_rows;c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			panel.add(tl[k],c);
			if(required[k]) {
				//tl[k].getEditor().getEditorComponent().setBackground(Color.lightGray);
				tl[k].getEditor().getEditorComponent().setForeground(Color.red);
				tl[k].getEditor().getEditorComponent().setFont(tl[k].getFont().deriveFont(Font.BOLD));
			}
			c.fill = GridBagConstraints.NONE;
			c.gridx=1; c.gridy=k+static_rows;c.anchor=GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
			if(lov[k].equals("")){
				panel.add(valueEditor[k]=new JTextField(entry_size[k]),c);
				((JTextField)valueEditor[k]).setText(default_val[k]);
			} else {
				String[] items=lov[k].split(net.ddp2p.common.table.field_extra.SEP_list_of_values,0);
				panel.add(valueEditor[k]=new JComboBox(items),c);
				int j = getIndex(items,default_val[k]);
				((JComboBox)valueEditor[k]).setSelectedIndex(j);
			}
			valueEditor[k].setToolTipText(tip[k]);
		}
	}
	void initDynamicProperties(int static_rows, int sub_rows, ArrayList<D_OrgParam> fe){
		if(DEBUG) System.out.println("ConstituentaAdd: initDynamicProperties:="+fe.size());
		for (int i = 0; i < fe.size(); i ++) {
			if(DEBUG) System.out.println("ConstituentaAdd: initDynamicProperties: i="+i+" sub_rows="+sub_rows+" static="+static_rows+" fe="+fe.size()+" k="+ this.can_be_provided_later.length);
			int k = i + sub_rows;
			can_be_provided_later[k] = fe.get(i).can_be_provided_later;
			certificated[k] = fe.get(i).certificated;
			default_val[k] = fe.get(i).default_value;
			entry_size[k] = fe.get(i).entry_size;
			fieldID[k] = fe.get(i).field_LID;
			label[k] = fe.get(i).label;
			lov[k] = Util.concat(fe.get(i).list_of_values, net.ddp2p.common.table.organization.ORG_VAL_SEP, net.ddp2p.common.table.field_extra.SPARAM_LIST_VAL_DEFAULT);
			partNeigh[k] = fe.get(i).partNeigh;
			required[k] = fe.get(i).required;
			tip[k] = fe.get(i).tip;
			label_lang[k] = fe.get(i).label_lang;
			Language lang = new Language(label_lang[k], null);
			if(DEBUG)System.out.println("Field: "+lang+" l="+label[k]+" f="+fieldID[k]+" pn="+partNeigh[k]);
			tl[k]=new TranslatedLabel(label[k],lang);
			c.ipadx=10;
			c.gridx=0; c.gridy=k+static_rows;c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			panel.add(tl[k],c);
			if(required[k]) {
				//tl[k].getEditor().getEditorComponent().setBackground(Color.lightGray);
				tl[k].getEditor().getEditorComponent().setForeground(Color.red);
				tl[k].getEditor().getEditorComponent().setFont(tl[k].getFont().deriveFont(Font.BOLD));
			}
			c.fill = GridBagConstraints.NONE;
			c.gridx=1; c.gridy=k+static_rows;c.anchor=GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
			if(lov[k].equals("")){
				panel.add(valueEditor[k]=new JTextField(entry_size[k]),c);
				((JTextField)valueEditor[k]).setText(default_val[k]);
			} else {
				String[] items=lov[k].split(net.ddp2p.common.table.field_extra.SEP_list_of_values,0);
				panel.add(valueEditor[k]=new JComboBox(items),c);
				int j = getIndex(items,default_val[k]);
				((JComboBox)valueEditor[k]).setSelectedIndex(j);
			}
			valueEditor[k].setToolTipText(tip[k]);
		}
	}
	/**
	 * Build a dialog that is dynamic and allows to fill a new neighbor's data!
	 * @param _tree
	 * @param _tp
	 */
	public ConstituentsAdd(ConstituentsTree _tree, TreePath _tp) {
		super(Util_GUI.findWindow(_tree));
		init(_tree, _tp, false);
	}
	public ConstituentsAdd(ConstituentsTree _tree, TreePath _tp, boolean myself) {
		super(Util_GUI.findWindow(_tree));
		init(_tree, _tp, myself);
	}
	void init(ConstituentsTree _tree, TreePath _tp, boolean _myself){
		myself = _myself;
		panel.setLayout(new GridBagLayout());
		setLayout(new GridBagLayout());
		int static_rows=0;
		tree = (ConstituentsTree) _tree;
		model = (ConstituentsModel) _tree.getModel();
		tp = _tp;
		ConstituentsAddressNode can = (ConstituentsAddressNode)tp.getLastPathComponent();
		if(DEBUG) System.out.println("ConstituentAdd: Extending: "+can+" level="+can.getLevel());
		long lastPN = -1;
		int cnt_subdivisions = -1;
		String subdivisions = can.getNeighborhoodData().names_subdivisions;
		Language sub_lang = can.getNeighborhoodData().name_subdivisions_lang;
		// If subdivisions are  present, then use them rather than field_extra
		String sub_divisions[]=null;
		if(DEBUG)System.out.println("ConstituentAdd: subdivisions="+subdivisions);
		if(subdivisions!=null){
			sub_divisions=D_Neighborhood.splitSubDivisionsFromCAND(subdivisions);
			cnt_subdivisions = sub_divisions.length;
			if(cnt_subdivisions>0) cnt_subdivisions--; // empty subdivisions have length 0; nonempty length>=2
			
			if(DEBUG)System.out.println("ConstituentAdd: "+subdivisions+"-> sub_divisions="+Util.concat(sub_divisions,":"));
			for(int k=1;k<sub_divisions.length;k++){ // first sub_division is always empty: ""
				String s = sub_divisions[k];
				if ((s==null)||(s.trim().length()==0)){
					if(DEBUG)System.out.println("ConstituentAdd: empty component, discard, sub="+s+" k="+k);
					sub_divisions=null;//new String[]{" "};
					break;
				}
			}
			if(DEBUG)System.out.println("ConstituentAdd: _1 sub_divisions="+Util.concat(sub_divisions,":"));
			
			if((sub_divisions==null)||(sub_divisions.length == 0)){
				if(DEBUG)System.out.println("ConstituentAdd: leaf detected sub="+Util.concat(sub_divisions,":"));
				//sub_divisions=new String[]{" "};
				sub_divisions=null;
			}
			
			if(DEBUG)System.out.println("ConstituentAdd: _ sub_divisions="+Util.concat(sub_divisions,":"));
		}
		if((sub_divisions==null)&&can.getNeighborhoodData().neighborhoodID>0){
			sub_divisions=new String[0];
			cnt_subdivisions = 0;
		}
		if(can.getLocation()!=null) lastPN = can.getLocation().partNeigh;//can.location.fieldID;
		/**
		 * Here we create and initialize the static fields in the panel, like:
		 *  - name, weight, crypto-system, cipher-sizes
		 */
		static_rows = initStaticFields();
		
		// If current neighborhood has explicit subdivisions, disregard neighborhood extra-fields
		if ((sub_divisions == null) || (can.getParent() == null)) { // non-explicit subdivisions
			if(DEBUG)System.out.println("ConstituentAdd: neigh+non-neigh: null sub="+Util.concat(sub_divisions,":"));
			initDynamicFields(static_rows,lastPN);
		} else { // explicit sub-divisions
			if(DEBUG)System.out.println("ConstituentAdd: non-neigh: sub="+subdivisions+" sz="+(sub_divisions.length-1));
//			ArrayList<ArrayList<Object>> fe = getProperties();
//			if (fe == null) fe = new ArrayList<ArrayList<Object>>();
			ArrayList<D_OrgParam> fe = getPropertiesFromOrg();
			if (fe == null) fe = new ArrayList<D_OrgParam>();
			int array_sizes = cnt_subdivisions+fe.size();
			initArrays(array_sizes); // space for remaining neigh and others
			if(DEBUG)System.out.println("ConstituentAdd: arrays="+array_sizes+" fe_size="+fe.size()+
					" can.fields="+Util.concat(can.getFieldIDs(),":","null"));
			if (lastPN < cnt_subdivisions) lastPN=sub_divisions.length;
			for (int i=0;i<cnt_subdivisions;i++) {
				int ii = i+1; // the actual subdivision considered (starting at 1)
				int k= cnt_subdivisions -1 -i; // from the smallest subdivision, backwards in arrays
				int new_fieldID_idx = can.getFieldIDs().length-cnt_subdivisions+i;//can.level+i; // idx in field_extra_id
				if((new_fieldID_idx<0)||(new_fieldID_idx>=can.getFieldIDs().length)) new_fieldID_idx = 0; //fail grace?
				if(DEBUG) System.out.println("ConstituentAdd: sub="+i+".."+k+"="+sub_divisions[ii]+"\" idx="+new_fieldID_idx);
				can_be_provided_later[k]=false;
				required[k]=true;
				certificated[k]=true;
				entry_size[k]=50;
				fieldID[k]=can.getFieldIDs()[new_fieldID_idx];
				label[k]=sub_divisions[i+1];
				partNeigh[k]=(int)lastPN-i-1;
				label_lang[k]=sub_lang.lang;
				tl[k]=new TranslatedLabel(label[k],sub_lang);
				c.ipadx=10;c.gridx=0; c.gridy=k+static_rows;c.anchor = GridBagConstraints.WEST;
				c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(tl[k],c);
				c.fill = GridBagConstraints.NONE;
				c.gridx=1; c.gridy=k+static_rows;c.anchor=GridBagConstraints.WEST;c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(valueEditor[k]=new JTextField(entry_size[k]),c);
				((JTextField)valueEditor[k]).setText(default_val[k]);
			}
			initDynamicProperties(static_rows,cnt_subdivisions,fe);
		}
		
		c.gridx=0; c.gridy=0;
		add(panel,c);
		
		//c.gridx=0; c.gridy=1;c.anchor=GridBagConstraints.CENTER; c.fill = GridBagConstraints.NONE;
		//add(ok=new JButton(_("Ok")),c);
		
		c.gridx=0; c.gridy=2;c.anchor=GridBagConstraints.CENTER; c.fill = GridBagConstraints.NONE;
		add(ok=new JButton(__("Ok")),c);

		if(myself)setWitnessCategoryMyself();
		
		pack();
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
		       	if(DEBUG) System.out.println("ok actionPerformed: "+evt+" data="+this);
		       	dialog.accepted = true;
				dialog.setVisible(false);
			}
		});
		setModal(true);
		//setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setVisible(true);
		validate();		
	}
	public void setWitnessCategoryMyself() {
       	if(DEBUG) System.out.println("ConstituentsAdd:setWitnessCategoryMyself");
		witness_category.setSelectedIndex(INDEX_WITNESS_MYSELF);
		witness_category_trustworthiness.setSelectedIndex(INDEX_WITNESS_TRUSTWORTHINESS_MYSELF);
	}
	/*
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.crt_cipher) {
			this.setSelectedCipher((String) this.crt_cipher.getSelectedItem());
		}
		if (e.getSource() == this.crt_sizes_list) {
			Object sel = this.crt_sizes_list.getSelectedItem();
			if(sel == null)
				return;
			this.setSelectedSizes((String) this.crt_cipher.getSelectedItem(),
					//this.crt_sizes_list.getSelectedIndex());
					this.getCrtCipherSize((String)sel));
		}
	}
	private void cipherSizeChanged () {
		int size = this.getCrtCipherSize(); //Integer.parseInt(this.crt_sizes_int.getText());
		this.setSelectedSizes((String) this.crt_cipher.getSelectedItem(), size);		
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	@Override
	public void changedUpdate(DocumentEvent e) {
		cipherSizeChanged();
	}
	*/
}

/*
     			//modal translations on accept
    			System.err.println("Label: "+k+" idx="+dialog.tl[k].getSelectedIndex()+
    					" ("+dialog.tl[k].getSelectedItem()+")");
    			if((dialog.tl!=null) &&(dialog.tl[k]!=null) && (dialog.tl[k].getSelectedIndex() == -1)){
    				String translation = dialog.tl[k].getSelectedItem().toString();
    				try{
    					ArrayList<ArrayList<Object>> sel =
    						model.db.select("select ROWID from translation where submitter_ID=? " +
    							" and value = ? and translation_lang = ? and translation_flavor = ?;",
    							new String[]{DDTranslation.constituentID+"",
    							dialog.tl[k].getOriginalText(),
    							DDTranslation.authorship_lang.lang,
    							DDTranslation.authorship_lang.flavor});
    					for(int j = 0; j < sel.size(); j++) {
    						model.db.delete(table.translation.TNAME, new String[]{"ROWID"},
    								new String[]{Util.sval(sel.get(j).get(0), "")});
    					}
    					String gtID = Util.getGlobalID("translations", 
    							dialog.label[k]+dialog.label_lang[k]+
    							translation+ DDTranslation.authorship_lang.lang+
    							DDTranslation.authorship_charset+ DDTranslation.authorship_lang.flavor+
    							model.organizationID+""+model.constituentID+"");
    					long rowID = model.db.insert(table.translation.TNAME,
    							new String[]{table.translation.global_tID, table.translation.value,table.translation.value_lang,
    							table.translation.value_ctx,table.translation.translation,table.translation.translation_lang,
    							table.translation.translation_charset, table.translation.translation_flavor, 
    							table.translation.organization_ID, table.translation.submitter_ID, table.translation.signature},
    							new String[]{gtID, dialog.label[k], dialog.label_lang[k],
    							"field label", translation, DDTranslation.authorship_lang.lang,
    							DDTranslation.authorship_charset, DDTranslation.authorship_lang.flavor,
    							model.organizationID+"",model.constituentID+"","NULL"});
    				}catch(Exception ev){
    		    		ev.printStackTrace();
    		    		return;
    				}
    			}
 */

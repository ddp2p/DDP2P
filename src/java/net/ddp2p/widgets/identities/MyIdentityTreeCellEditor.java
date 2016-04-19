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
 package net.ddp2p.widgets.identities;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.Component;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import static net.ddp2p.common.util.Util.__;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.ddp2p.widgets.app.Util_GUI;
class MyIdentityData{
	String OID;
	String value;
	String certificate;
	String explain;
	String OID_name;
	public String toString(){
		return "MyIdentityData: OID="+OID+" value="+value+" certificate="+certificate;
	}
}
class MyIdentityTreeCellEditor extends AbstractCellEditor
								implements TreeCellEditor, ActionListener {
	OIDs oid_items[] = new OIDs[0];
	MyIdentityData data=new MyIdentityData(); 
	String identity; 
	JTree tree;
	MyIdentitiesModel model;
	JDialog dialog;
	JTextField valueEditor, identityEditor;
	JComboBox oidSelector;
	JButton button, ok;
	boolean in_leaf;
	protected static final String EDIT = "edit";
	protected static final String EDIT_ID = "editID";
	private static final boolean DEBUG = false;
	class OIDs{
		String OID, OID_name, explanation;
		OIDs(String OID, String OID_name, String explanation) {
			this.OID = OID;
			this.OID_name = OID_name;
			this.explanation = explanation;
		}
		public String toString() {
			return OID_name+" ("+OID+")";
		}
		String getTip(){return explanation;}
	}
	class MyComboBoxRenderer extends BasicComboBoxRenderer {
		    public Component getListCellRendererComponent(JList list, Object value,
		        int index, boolean isSelected, boolean cellHasFocus) {
		      if (isSelected) {
		        setBackground(list.getSelectionBackground());
		        setForeground(list.getSelectionForeground());
		        if (-1 < index) {
		          list.setToolTipText(((OIDs)value).getTip());
		        }
		      } else {
		        setBackground(list.getBackground());
		        setForeground(list.getForeground());
		      }
		      setFont(list.getFont());
		      setText((value == null) ? "" : value.toString());
		      return this;
		    }
		  }
	@SuppressWarnings("unchecked")	  
	public
	MyIdentityTreeCellEditor(JTree tree){
		this.tree = tree;
		model = (MyIdentitiesModel)tree.getModel();
		button = new JButton(__("Editing..."));
		button.setBackground(Color.BLUE);
		dialog = new JDialog(Util_GUI.findWindow(tree));
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		JPanel panel= new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		dialog.setLayout(new GridBagLayout());
		c.gridx=0; c.gridy=0;
		dialog.add(panel,c);
		c.gridx=0; c.gridy=0;
		panel.add(new JLabel(__("Property value")),c);
		c.gridx=1; c.gridy=0; c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(valueEditor=new JTextField(10),c);
		c.gridx=0; c.gridy=1; c.fill = GridBagConstraints.NONE;
		panel.add(new JLabel(__("Property type")),c);
		try {
			ArrayList<ArrayList<Object>> oids=model.db.select(Queries.sql_oids, new String[]{});
			if(DEBUG) System.err.println("Found:"+oids.size());
			oid_items = new OIDs[oids.size()];
			for(int k=0; k<oids.size(); k++) {
				oid_items[k]=new OIDs((String)oids.get(k).get(0), (String)oids.get(k).get(1), (String)oids.get(k).get(2));
				if(DEBUG) System.err.println("Found:"+oid_items[k]);
			}
		}catch(Exception e){e.printStackTrace();}
		c.gridx=1; c.gridy=1; c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(oidSelector=new JComboBox(oid_items),c);
		oidSelector.setRenderer(new MyComboBoxRenderer());
		c.gridx=0; c.gridy=1; c.fill = GridBagConstraints.NONE;
		dialog.add(ok=new JButton(__("Ok")),c);
		dialog.pack();
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				if(DEBUG) System.err.println("ok actionPerformed: "+evt+" data="+data);
				dialog.setVisible(false);
		   		data.value = valueEditor.getText();
		   		OIDs obj = (OIDs)oidSelector.getSelectedItem();
		   		if(obj==null) return;
	    		data.OID = obj.OID;
	    		data.OID_name = obj.OID;
	    		data.explain = obj.explanation;
	    		data.certificate = null;
			}
		});
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		dialog.validate();
		identityEditor = new JTextField(10);
		identityEditor.addActionListener(this);
		identityEditor.setActionCommand(EDIT_ID);
	}
	public
	Component getTreeCellEditorComponent(JTree tree,
            Object value,
            boolean isSelected,
            boolean expanded,
            boolean leaf,
            int row){
		if(DEBUG) System.err.println("getTreeCellEditorComponent: "+value+", leaf = "+leaf);
		if (value instanceof IdentityLeaf) {
			in_leaf = true;
			IdentityLeaf il = (IdentityLeaf)value;
			data.value = il.name;
			data.OID = il.OID;
			data.OID_name = il.OID_name;
			data.certificate = il.certificate;
			data.explain = il.explain;
			return button;
		} else if(value instanceof IdentityBranch){
			in_leaf = false;
			IdentityBranch ib = (IdentityBranch)value;
			data.value = ib.name;
	   		identityEditor.setText(data.value);
	   	 	return identityEditor;
		}
		return null;
	}
    public Object getCellEditorValue() {
		if(in_leaf){
		}else{
			if(DEBUG) System.err.println("getCellValue branch");
			data.value = identityEditor.getText();
		}
		if(DEBUG) System.err.println("getCellEditorValue: "+data);
        return data;
    }
    public void actionPerformed(ActionEvent e) {
    	if(DEBUG) System.err.println("actionPerformed: "+e+" data="+data);
    	if (EDIT.equals(e.getActionCommand())) {
    		if(DEBUG) System.err.println("EDIT actionPerformed: "+e+" data="+data);
    		valueEditor.setText(data.value);
    		for(int k=0; k<oid_items.length; k++) {
    			if(oid_items[k].OID.equals(data.OID))
    			oidSelector.setSelectedItem(oid_items[k]);
    		}
    		dialog.setVisible(true);
    		fireEditingStopped();
    	} else if (EDIT_ID.equals(e.getActionCommand())) {
    		if(DEBUG) System.err.println("EDIT_ID actionPerformed: "+e+" data="+data);
    		fireEditingStopped();
    	}
    }
	public
     boolean 	isCellEditable(EventObject anEvent) {
		if(DEBUG) System.err.println("isCellEditable: "+anEvent);
      	if (anEvent instanceof MouseEvent) {
      		MouseEvent mouseEvent = (MouseEvent)anEvent;
      		if(mouseEvent.getClickCount() == 2) return true;
      	}
		return false;
     }
	public
     boolean 	shouldSelectCell(EventObject anEvent) {
		return false;
     }
}

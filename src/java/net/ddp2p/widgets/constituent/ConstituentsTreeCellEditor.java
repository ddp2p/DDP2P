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
import javax.swing.*;
import javax.swing.tree.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.util.*;

import static net.ddp2p.common.util.Util.__;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.WindowConstants;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.population.ConstituentsAddressNode;
import net.ddp2p.common.population.ConstituentsIDNode;
import net.ddp2p.common.population.ConstituentsPropertyNode;
import net.ddp2p.common.population.Constituents_NeighborhoodData;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.components.Country;
import net.ddp2p.widgets.components.DDCountrySelector;
import net.ddp2p.widgets.components.DDLanguageSelector;
import net.ddp2p.widgets.components.DivisionSelector;
import net.ddp2p.widgets.components.LanguageItem;
import net.ddp2p.widgets.components.Translation;

class NeighborhoodRegistration extends JPanel{
	GridBagConstraints c = new GridBagConstraints();
	JTextField nameEditor;
	DDLanguageSelector nameLanguageSelector;
	DDCountrySelector nameCountrySelector;
	DivisionSelector divisionEditor;
	DDLanguageSelector divisionLanguageSelector;
	DDCountrySelector divisionCountrySelector;
	//JComboBox fieldID;
	JTextField subdivisionsEditor;
	DDLanguageSelector subdivisionsLanguageSelector;
	DDCountrySelector subdivisionsCountrySelector;
	boolean inited=false;
	ConstituentsModel model;
	NeighborhoodRegistration(ConstituentsModel model) {
		this.model = model;
	}
	void init(){
		if(inited) return;
		setLayout(new GridBagLayout());
		c.gridx=0; c.gridy=0; c.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel(__("Neighborhood Name:")),c);
		c.gridx=0; c.gridy=1; c.fill = GridBagConstraints.HORIZONTAL;
		add(nameEditor=new JTextField(10),c);
		//c.gridx=0; c.gridy=1; c.fill = GridBagConstraints.NONE;
		//add(new JLabel(_("Language")),c);
		c.gridx=1; c.gridy=1; c.fill = GridBagConstraints.HORIZONTAL;
		add(nameLanguageSelector=new DDLanguageSelector(),c);
		//c.gridx=0; c.gridy=2; c.fill = GridBagConstraints.NONE;
		//add(new JLabel(_("Country")),c);
		c.gridx=2; c.gridy=1; c.fill = GridBagConstraints.HORIZONTAL;
		add(nameCountrySelector=new DDCountrySelector(),c);
		
		c.gridx=0; c.gridy=3; c.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel(__("Division Name:")),c);
		c.gridx=0; c.gridy=4; c.fill = GridBagConstraints.HORIZONTAL;
		add(divisionEditor=new DivisionSelector(),c);
		c.gridx=1; c.gridy=4; c.fill = GridBagConstraints.HORIZONTAL;
		add(divisionLanguageSelector=new DDLanguageSelector(),c);
		c.gridx=2; c.gridy=4; c.fill = GridBagConstraints.HORIZONTAL;
		add(divisionCountrySelector=new DDCountrySelector(),c);
		
		c.gridx=0; c.gridy=5; c.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel(__("Sub-Divisions Names:")),c);
		c.gridx=0; c.gridy=6; c.fill = GridBagConstraints.HORIZONTAL;
		add(subdivisionsEditor=new JTextField(10),c);
		c.gridx=1; c.gridy=6; c.fill = GridBagConstraints.HORIZONTAL;
		add(subdivisionsLanguageSelector=new DDLanguageSelector(),c);
		c.gridx=2; c.gridy=6; c.fill = GridBagConstraints.HORIZONTAL;
		add(subdivisionsCountrySelector=new DDCountrySelector(),c);
		inited = true;
	}
}

class ConstituentsTreeCellEditor extends AbstractCellEditor
	implements TreeCellEditor, ActionListener {
	boolean in_leaf = false;
	JTree tree;
	ConstituentsModel model;
	JDialog dialog;
	JTextField valueEditor, identityEditor;
	JButton button, ok;
	protected static final String EDIT = "edit";
	protected static final String EDIT_ID = "editID";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	NeighborhoodRegistration panel;
	public
	ConstituentsTreeCellEditor(JTree tree){
		this.tree = tree;
		model = (ConstituentsModel)tree.getModel();
		button = new JButton(__("Editing..."));
		button.setBackground(Color.BLUE);
		dialog = new JDialog(Util_GUI.findWindow(tree));
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);
		
		panel = new NeighborhoodRegistration(model);
		GridBagConstraints c = new GridBagConstraints();
		dialog = new JDialog(Util_GUI.findWindow(tree));
		dialog.setLayout(new GridBagLayout());
		c.gridx=0; c.gridy=0;
		dialog.add(panel,c);
		c.gridx=0; c.gridy=1;
		dialog.add(ok=new JButton(__("Ok")),c);
		dialog.pack();
		ok.setActionCommand(EDIT_ID);
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
		       	if(DEBUG)System.out.println("ok actionPerformed: "+evt);
				dialog.setVisible(false);
				
				nd=new Constituents_NeighborhoodData();
				nd.name_lang=new Language(null,null);
				nd.name_division_lang=new Language(null,null);
				nd.name_subdivisions_lang=new Language(null,null);
				nd.name=panel.nameEditor.getText();
				nd.name_lang.lang=((LanguageItem)panel.nameLanguageSelector.getSelectedItem()).code;
				nd.name_lang.flavor=((Country)panel.nameCountrySelector.getSelectedItem()).code;
				nd.name_division=((Translation)panel.divisionEditor.getSelectedItem()).getOriginal();
				nd.name_division_lang.lang=((LanguageItem)panel.divisionLanguageSelector.getSelectedItem()).code;
				nd.name_division_lang.flavor=((Country)panel.divisionCountrySelector.getSelectedItem()).code;
				nd.names_subdivisions=panel.subdivisionsEditor.getText();
				nd.name_subdivisions_lang.lang=((LanguageItem)panel.subdivisionsLanguageSelector.getSelectedItem()).code;
				nd.name_subdivisions_lang.flavor=((Country)panel.subdivisionsCountrySelector.getSelectedItem()).code;
			}
		});
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		dialog.validate();
	}
	Constituents_NeighborhoodData nd=null;
	//@Override
	public Object getCellEditorValue() {
		if(DEBUG)System.out.println("getValue: "+nd);//+" data="+data);
		return nd;
	}
	//@Override
	public void actionPerformed(ActionEvent e) {
       	if(DEBUG)System.out.println("actionPerformed: "+e);//+" data="+data);
    	if (EDIT.equals(e.getActionCommand())) {
           	//System.err.println("EDIT actionPerformed: "+e+" data="+data);
    		//valueEditor.setText(data.value);
    		dialog.setVisible(true);    		
    		fireEditingStopped();
    	}else if (EDIT_ID.equals(e.getActionCommand())) {
           	if(DEBUG)System.out.println("EDIT_ID actionPerformed: "+e);//+" data="+data);
    		fireEditingStopped();
    	}
	}
	//@Override
	public Component getTreeCellEditorComponent(JTree tree,
			Object value,
			boolean isSelected,
			boolean expanded,
			boolean leaf, int row) {
		if(_DEBUG)System.out.println("getTreeCellEditorComponent: "+value+", leaf = "+leaf);
		if (value instanceof ConstituentsAddressNode) {
			ConstituentsAddressNode node = (ConstituentsAddressNode)value;
			in_leaf = false;
			nd = null;//node.n_data;
			panel.init();
			panel.divisionEditor.init(model.getSubDivisions(),
					((ConstituentsAddressNode)node.getParent()).getNeighborhoodData().names_subdivisions,
					((ConstituentsAddressNode)node.getParent()).getNeighborhoodData().name_subdivisions_lang);
			panel.divisionEditor.setSelectedItem(DDTranslation.translate(node.getNeighborhoodData().name_division,
					node.getNeighborhoodData().name_division_lang));
			panel.subdivisionsEditor.setText(node.getNeighborhoodData().names_subdivisions);
			panel.nameEditor.setText(node.getNeighborhoodData().name);
			if(node.getNeighborhoodData().name_lang!=null) {
				panel.nameLanguageSelector.setChoice(node.getNeighborhoodData().name_lang.lang);
				panel.nameCountrySelector.setChoice(node.getNeighborhoodData().name_lang.flavor);
			}
			if(node.getNeighborhoodData().name_division_lang!=null) {
				panel.divisionLanguageSelector.setChoice(node.getNeighborhoodData().name_division_lang.lang);
				panel.divisionCountrySelector.setChoice(node.getNeighborhoodData().name_division_lang.flavor);
			}
			if(node.getNeighborhoodData().name_subdivisions_lang!=null) {
				panel.subdivisionsLanguageSelector.setChoice(node.getNeighborhoodData().name_subdivisions_lang.lang);
				panel.subdivisionsCountrySelector.setChoice(node.getNeighborhoodData().name_subdivisions_lang.flavor);
			}
			dialog.pack();
			return button;
		}else if (value instanceof ConstituentsIDNode) {
			//in_leaf = false;
			//IdentityBranch ib = (IdentityBranch)value;
			//data.value = ib.name;
	   		//identityEditor.setText(data.value);
	   	 	//return identityEditor;
		} else if(value instanceof ConstituentsPropertyNode){
			//in_leaf = true;
			//IdentityLeaf il = (IdentityLeaf)value;
			//return button;
		}
		return null;
	}
	public
    boolean 	isCellEditable(EventObject anEvent) {
     	//System.err.println("isCellEditable: "+anEvent);
		//     Asks the editor if it can start editing using anEvent.
     	if (anEvent instanceof MouseEvent) {
     		MouseEvent mouseEvent = (MouseEvent)anEvent;
     		if(mouseEvent.getClickCount() != 2) return false;
     		TreePath selPath = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
     		if(!(selPath.getLastPathComponent() instanceof ConstituentsAddressNode)) return false;
     		ConstituentsAddressNode node = (ConstituentsAddressNode) selPath.getLastPathComponent();
     		if((node.getNeighborhoodData()==null)||(node.getNeighborhoodData().neighborhoodID<0)) return false;
      		if(node.getNeighborhoodData().global_nID == null)	return true;
      		else{
      			if(DD.EDIT_VIEW_UNEDITABLE_NEIGHBORHOODS)
      				Application_GUI.warning(__("Cannot edit signed neighborhoods! Create a new one."), __("Not editable"));
      			else
      				Application_GUI.warning(__("Cannot edit signed neighborhoods! Create a new one."+"\n"+
      			__("We will open the editor but changes will not be saved!")), __("Not editable"));
      			return DD.EDIT_VIEW_UNEDITABLE_NEIGHBORHOODS;
      		}
     	}
		return false;
    }
	public
    boolean 	shouldSelectCell(EventObject anEvent) {
		// Returns true if the editing cell should be selected, false otherwise.
		return false;
    }
}

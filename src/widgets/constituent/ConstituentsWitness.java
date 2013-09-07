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
 package widgets.constituent;
import static util.Util._;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.File;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import data.D_Witness;
import util.Util;

public class ConstituentsWitness extends JDialog {
	protected static final boolean DEBUG = false;
	ConstituentsModel model;
	ConstituentsTree tree;
	ConstituentsWitness dialog=this;
	TreePath tp;
	JButton ok;
	JLabel jpl;
	GridBagConstraints c = new GridBagConstraints();
	JPanel panel = new JPanel();
	public boolean accepted = false;
	
	private static final int[] witness_categories_sense = new int[]{D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.UNKNOWN,D_Witness.UNKNOWN,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE};
	private static final String[] witness_categories=new String[]{
				_("Personally known eligibility"),
				_("Hearsay eligibility"),
				_("Unknown"),"",
				_("Nonexistent address"),
				_("No such person at this address"),
				_("Not eligible"),
				_("Error in address")
	};
	@SuppressWarnings("unchecked")
	public JComboBox witness_category =
		new JComboBox(witness_categories);
	
	private static final int[] witness_categories_trustworthiness_sense = new int[]{D_Witness.FAVORABLE,D_Witness.FAVORABLE,D_Witness.UNKNOWN,D_Witness.UNKNOWN,D_Witness.UNFAVORABLE,D_Witness.UNFAVORABLE};
	private static final String[] witness_categories_trustworthiness=new String[]{
				_("Personal trust"),
				_("Hearsay trust"),
				_("Unknown"),"",
				_("Hearsay distrust"),
				_("Personal distrust")
	};
	@SuppressWarnings("unchecked")
	public JComboBox witness_category_trustworthiness =
		new JComboBox(witness_categories_trustworthiness);
	//public final int first_negative=2;
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
	public ConstituentsWitness(ConstituentsTree _tree, TreePath _tp, int sense) {
		super(Util.findWindow(_tree));
		tree = (ConstituentsTree) _tree;
		model = (ConstituentsModel) _tree.getModel();
		tp = _tp;
		if(tp!=null) {
			ConstituentsIDNode can = (ConstituentsIDNode)tp.getLastPathComponent();
			if(DEBUG)System.out.println("ConstituentsWitness: Witnessing: "+can);
		}
		JButton bp;
		panel.setLayout(new GridBagLayout());
		setLayout(new GridBagLayout());

		c.ipadx=10; c.gridx=0; c.gridy=4; c.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel(_("Eligibility")),c);
		c.gridx = 1;
		panel.add(witness_category,c);

		c.ipadx=10; c.gridx=0; c.gridy=5; c.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel(_("Trustworthiness")),c);
		c.gridx = 1;
		panel.add(witness_category_trustworthiness,c);

		c.gridx=0; c.gridy=0;
		add(panel,c);
	
		c.gridx=0; c.gridy=1;c.anchor=GridBagConstraints.CENTER; c.fill = GridBagConstraints.NONE;
		add(ok=new JButton(_("Ok")),c);
		pack();
		ok.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				if(DEBUG) System.out.println("ConstituentsWitness: ok actionPerformed: "+evt+" data="+this);
				dialog.accepted = true;
				dialog.setVisible(false);
			}
		});
		setModal(true);
		//setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setVisible(true);
		validate();
	}
}

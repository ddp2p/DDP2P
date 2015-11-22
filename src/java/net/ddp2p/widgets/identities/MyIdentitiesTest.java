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
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.ddp2p.common.util.DBInterface;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
@SuppressWarnings("serial")
public class MyIdentitiesTest extends JPanel {
    MyIdentitiesTree tree;
    public MyIdentitiesTest(DBInterface db) {
	super(new BorderLayout());
	tree = new MyIdentitiesTree( new MyIdentitiesModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(200, 200));
	add(scrollPane, BorderLayout.CENTER);
    }
    private static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("MyIdentities Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MyIdentitiesTest newContentPane = new MyIdentitiesTest(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
    public static void main(String[] args) {
	if(args.length==0) return;
	final String dbname = args[0];
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    try {
			final DBInterface db = new DBInterface(dbname);
			createAndShowGUI(db);
		    }catch(Exception e){
			JOptionPane.showMessageDialog(null,"Error opening database!");
			return;
		    }
		}
	    });
    }
}

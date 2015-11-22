/*   Copyright (C) 2012 
		Author: Ossamah Dhannoon
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
package net.ddp2p.widgets.wireless;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.ddp2p.common.util.DBInterface;
@SuppressWarnings("serial")
public class Interfaces extends JPanel {
	WLAN_widget tree;
    public Interfaces(DBInterface db) {
    	super(new BorderLayout());
    	tree = new WLAN_widget( new WlanModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        add(scrollPane, BorderLayout.CENTER);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Wlan Widget");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Interfaces newContentPane = new Interfaces(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}

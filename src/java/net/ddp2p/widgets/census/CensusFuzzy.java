/*   Copyright (C) 2012 Song Qin
		Author: Song Qin: qsong2008@my.fit.edu
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
package net.ddp2p.widgets.census;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
@SuppressWarnings("serial")
public class CensusFuzzy extends JTable implements MouseListener{
	public CensusFuzzy() {
		super(new CensusFuzzyModel(-1,-1));
	}	
	public CensusFuzzy(int sign, long orgID){
		super(new CensusFuzzyModel(sign,orgID));
	}
	public CensusFuzzyModel getModel(){
		return (CensusFuzzyModel)super.getModel();
	}
	public JFrame getCensusFuzzyFrame(){
		JFrame frame = new JFrame();
		frame.setContentPane(new CensusPanel(this));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return frame;
	}
	public JScrollPane getScrollPane() {
		return new JScrollPane(this);
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
	public void mousePressed(MouseEvent arg0) {
	}
	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}

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
package net.ddp2p.widgets.justifications;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.JustificationsListener;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_MotionChoice;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Motion.MotionChoiceSupport;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.DBSelector;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.MainFrame;
import org.jdesktop.swingx.MultiSplitLayout;
import org.jdesktop.swingx.MultiSplitPane;
import static net.ddp2p.common.util.Util.__;
@SuppressWarnings("serial")
public class JustificationsByChoicePanel extends JPanel implements MotionsListener, DBListener {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private static final int TOTALS_HEIGHT = 0;
	public boolean SCROLL = true;
	MultiSplitPane multiSplitPane = new MultiSplitPane();
	D_Motion moti;
	String motion_ID;
	D_Organization org;
	D_MotionChoice[] choices = new D_MotionChoice[0];
	Justifications j[] = new Justifications[0];
	JScrollPane js[] = new JScrollPane[0];
	JLabel jl[] = new JLabel[0];
	public JustificationsByChoicePanel(){
		connectWidget();
	}
	public void connectWidget() {
		MainFrame.status.addMotionStatusListener(this); 
	}
	public void disconnectWidget() {
		MainFrame.status.removeMotListener(this);
		Application.getDB().delListener(this);
	}
	JustificationViewer _jedit = null;
	Component just_panel = null;
	public Component getComboPanel() {
		if (just_panel != null) return just_panel;
		if (DEBUG) System.out.println("createAndShowGUI: added jbc");
    	if (_jedit == null) _jedit = new JustificationViewer();
		if (DEBUG) System.out.println("createAndShowGUI: added justif editor");
    	just_panel = MainFrame.makeJBCViewPanel(_jedit, this);
    	JustificationsByChoicePanel _jbc = this; 
    	_jbc.addListener(MainFrame.status);
		if(DEBUG) System.out.println("createAndShowGUI: added jbc");
		if (_jedit != null) MainFrame.status.addJustificationStatusListener(_jedit);
		return just_panel; 
	}
	private void enrollDB(){
		Hashtable<String,DBSelector[]> h = new Hashtable<String,DBSelector[]>();
		DBSelector s = new DBSelector(net.ddp2p.common.table.signature.motion_ID, motion_ID);
		h.put(net.ddp2p.common.table.signature.TNAME, new DBSelector[]{s});
		Application.getDB().addListener(this,
				new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.signature.TNAME)),
				h);
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if(DEBUG) System.out.println("JBC:motion_update motID="+motID);
		if((motID==null) || (d_motion==null)) {
			Application.getDB().delListener(this);
			this.clean();
			this.moti = d_motion;
			motion_ID = motID;
			if(DEBUG) System.out.println("JBC:motion_update quit null motID or d_motion="+d_motion);
			return;
		}
		if ((motion_ID!=null)&&(motion_ID.equals(motID))) {
			if(DEBUG) System.out.println("JBC:motion_update quit same motID="+motID);
			return;
		}
		this.clean();
		this.moti = d_motion;
		motion_ID = motID;
		enrollDB();
		if ((org == null) || (!moti.getOrganizationLIDstr().equals(org.getLIDstr())))
			try {
				long _orgID = Util.lval(moti.getOrganizationLIDstr(),-1);
				if(_orgID > 0) org = D_Organization.getOrgByLID_NoKeep(_orgID, true);
				else{
					if(DEBUG) System.out.println("JBC:motion_update quit no orgID="+_orgID);
					return;
				}
			} catch (Exception e) {e.printStackTrace();return;}
		if ((moti.getChoices() != null) && (moti.getChoices().length > 0)) choices = moti.getChoices();
		else choices = org.getDefaultMotionChoices();
		if ((choices == null) || (choices.length == 0)) {
			if(DEBUG) System.out.println("JBC:motion_update quit no choices="+Util.nullDiscrimArray(choices, " ; "));
			return;
		}
		update_layout();
		update(null,null);
		for(Justifications _j :j){
			if(_j == null){
				if(DEBUG) System.out.println("JBC:motion_update null Justifications");
				continue;
			}
			_j.getModel().motion_update(motion_ID, col, moti);
		}
		adjustSize();
		shareListeners();
		if(DEBUG) System.out.println("JBC:motion_update quit done");
	}
	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if ((choices == null) || (choices.length == 0)) return;
		for (int k = 0; k < choices.length; k ++) {
			long val = 0;
			double sum = 0.;
			{
				MotionChoiceSupport l = D_Motion.getMotionChoiceSupport(motion_ID, choices[k].short_name);
				val = l.getCnt();
				sum = l.getWeight();
				jl[k].setText(choices[k].name+" (#:"+val+" W:"+sum+")");
			}
		}
	}
	final static ArrayList<Justifications> linkedJBC = new ArrayList<Justifications>();
	private void update_layout() {
		if(DEBUG) System.out.println("JBC:update_layout");
		if(choices.length==1) {
			update_one();
			return;
		}
		List<MultiSplitLayout.Node> children;
		multiSplitPane = new MultiSplitPane();
		org.jdesktop.swingx.MultiSplitLayout.Node node1 = new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[0].short_name);
		org.jdesktop.swingx.MultiSplitLayout.Node node2 = new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[1].short_name);
		org.jdesktop.swingx.MultiSplitLayout.Split _modelRoot;
		children = 
			Arrays.asList(node1,
					new org.jdesktop.swingx.MultiSplitLayout.Divider(), 
					node2);
		_modelRoot = new org.jdesktop.swingx.MultiSplitLayout.Split();
		_modelRoot.setChildren(children);
		for (int k=2; k<choices.length; k++) {
			children = 
				Arrays.asList(_modelRoot,
						new org.jdesktop.swingx.MultiSplitLayout.Divider(), 
						new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[k].short_name));
			_modelRoot = new org.jdesktop.swingx.MultiSplitLayout.Split();
			_modelRoot.setChildren(children);
		}
		multiSplitPane.getMultiSplitLayout().setModel(_modelRoot);
		this.add(multiSplitPane); 
		j = new Justifications[choices.length];
		js = new JScrollPane[choices.length];
		jl = new JLabel[choices.length];
		/**
		 * Remove listener for justifications
		 */
		for (Justifications justif : linkedJBC) {
			MainFrame.status.removeJustificationListener(justif);
		}
		linkedJBC.clear();
		for (int k = 0; k < choices.length; k ++){
			Justifications jus;
			jus = new Justifications(300);
			j[k] = jus;
			JustificationsModel model = jus.getModel();
			/**
			 * Keep track of added justifications
			 */
			MainFrame.status.addJustificationStatusListener(jus);
			linkedJBC.add(jus);
			D_MotionChoice choice = choices[k];
			String short_name = (choice == null) ? (__("Choice") + "_" + k) : choice.short_name;
			model.setCrtChoice(short_name);
			jus.init(); 
			if (DEBUG) System.out.println("JBC:update_layout: name="+choices[k].short_name);
			js[k] = jus.getScrollPane();
			jl[k] = new JLabel();
			jl[k].setPreferredSize(new Dimension(0,TOTALS_HEIGHT));
			multiSplitPane.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,jl[k],js[k]),short_name);
		}
	}
	private void update_one() {
		if(_DEBUG) System.out.println("JBC:update_out");
		j = new Justifications[1];
		js = new JScrollPane[1];
		jl = new JLabel[1];
		j[0] = new Justifications(300);
		linkedJBC.add(j[0]);
		js[0] = j[0].getScrollPane();
		jl[0] = new JLabel();
		jl[0].setPreferredSize(new Dimension(0,TOTALS_HEIGHT));
		this.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, jl[0],js[0]));
	}
	private void clean() {
		if (DEBUG) System.out.println("JBC:clean");
		/**
		 * Remove listener for justifications
		 */
		for (Justifications justif : linkedJBC) {
			MainFrame.status.removeJustificationListener(justif);
		}
		linkedJBC.clear();
		if (multiSplitPane!=null) this.remove(multiSplitPane);
		try{
			for (Component comp : j) this.remove(comp);
		}catch(Exception e){if(DD.DEBUG_TODO)e.printStackTrace();}
		j = new Justifications[0];
		choices = new D_MotionChoice[0];
		jl = new JLabel[0];
		js = new JScrollPane[0];
	}
	ArrayList<JustificationsListener> listeners=new ArrayList<JustificationsListener>();
	public void shareListeners() {
		for(JustificationsListener l: listeners) {
			for(Justifications _j : this.j) _j.addListener(l);
		}
	}
	public void addListener(JustificationsListener l){
		listeners.add(l);
		for(Justifications _j : this.j) _j.addListener(l);
	}
	public void removeListener(JustificationsListener l){
		listeners.remove(l);
		for(Justifications _j : this.j) _j.removeListener(l);
	}
	@Override
	public void motion_forceEdit(String motID) {}
	public void adjustSize() {
		int DIV = 5;
		if((choices==null)||(choices.length==0)) return;
		this.multiSplitPane.setDividerSize(DIV);
		int w = MainFrame.tabbedPane.getWidth();
		if(multiSplitPane!=null)this.multiSplitPane.setPreferredSize(new Dimension(w-20-DIV*choices.length, MainFrame.tabbedPane.getHeight()-75-jl[0].getHeight()));
	}
}

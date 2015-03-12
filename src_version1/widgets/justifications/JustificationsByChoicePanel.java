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
package widgets.justifications;

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

import org.jdesktop.swingx.MultiSplitLayout;
import org.jdesktop.swingx.MultiSplitPane;

import util.P2PDDSQLException;

import config.Application;
import config.DD;

import data.D_Motion;
import data.D_MotionChoice;
import data.D_Organization;

import util.DBInfo;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.motions.MotionsListener;
import static util.Util._;

//46 * 40 LSSBS
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
		DD.status.addMotionStatusListener(this); //this also rehister to the db, as needed
	}
	public void disconnectWidget() {
		DD.status.removeMotListener(this);
		Application.db.delListener(this);
	}
	public Component getComboPanel() {
		JustificationsByChoicePanel _jbc = this; //new JustificationsByChoicePanel();
    	//_jbc.addListener(_jedit);
    	_jbc.addListener(DD.status);
 		//DD.status.addMotionStatusListener(_jbc);
     	//javax.swing.JScrollPane jbc = new javax.swing.JScrollPane(_jbc);
		//tabbedPane.addTab("JBC", _jbc);
		if(DEBUG) System.out.println("createAndShowGUI: added jbc");
		return _jbc;
	}
	private void enrollDB(){
		Hashtable<String,DBSelector[]> h = new Hashtable<String,DBSelector[]>();
		DBSelector s = new DBSelector(table.signature.motion_ID, motion_ID);
		h.put(table.signature.TNAME, new DBSelector[]{s});
		Application.db.addListener(this,
				new ArrayList<String>(Arrays.asList(table.signature.TNAME)),
				h);
	}
	
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		if(DEBUG) System.out.println("JBC:motion_update motID="+motID);
		if((motID==null) || (d_motion==null)) {
			Application.db.delListener(this);
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
			
		if((org == null) || (!moti.organization_ID.equals(org.organization_ID)))
			try {
				long _orgID = Util.lval(moti.organization_ID,-1);
				if(_orgID > 0) org = new D_Organization(_orgID);
				else{
					if(DEBUG) System.out.println("JBC:motion_update quit no orgID="+_orgID);
					return;
				}
			} catch (Exception e) {e.printStackTrace();return;}
		if((moti.choices!=null) && (moti.choices.length>0)) choices = moti.choices;
		else choices = org.getDefaultMotionChoices();
		if((choices == null) || (choices.length==0)) {
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
	String sql_label = 
		"SELECT count(*), sum(c."+table.constituent.weight+") " +
		" FROM "+table.signature.TNAME+" AS s "+
		" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=s."+table.signature.constituent_ID+")"+
		" WHERE s."+table.signature.choice+"=? AND s."+table.signature.motion_ID+"=?;";
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if((choices==null)||(choices.length==0)) return;
		for(int k =0; k<choices.length; k++) {
			long val=0;
			double sum=0.;
			try {
				ArrayList<ArrayList<Object>> l = Application.db.select(sql_label, new String[]{choices[k].short_name,motion_ID}, DEBUG);
				if(l.size()>0){ val = Util.lval(l.get(0).get(0), -1); sum = Util.dval(l.get(0).get(1), 0.);}
				jl[k].setText(choices[k].name+" (#:"+val+" W:"+sum+")");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
	}


	private void update_layout() {
		if(DEBUG) System.out.println("JBC:update_layout");
		if(choices.length==1) {
			update_one();
			return;
		}
		List<MultiSplitLayout.Node> children;
		multiSplitPane = new MultiSplitPane();
		//String layoutDef = "(ROW (LEAF name=left weight=0.5) (LEAF name=right weight=0.5))";
		//MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
		
		org.jdesktop.swingx.MultiSplitLayout.Node node1 = new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[0].short_name);
		org.jdesktop.swingx.MultiSplitLayout.Node node2 = new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[1].short_name);
		org.jdesktop.swingx.MultiSplitLayout.Split _modelRoot;

		children = 
			Arrays.asList(node1,
					new org.jdesktop.swingx.MultiSplitLayout.Divider(), 
					node2);
		_modelRoot = new org.jdesktop.swingx.MultiSplitLayout.Split();
		_modelRoot.setChildren(children);
		
		for(int k=2; k<choices.length; k++) {
			children = 
				Arrays.asList(_modelRoot,
						new org.jdesktop.swingx.MultiSplitLayout.Divider(), 
						new org.jdesktop.swingx.MultiSplitLayout.Leaf(choices[k].short_name));
			_modelRoot = new org.jdesktop.swingx.MultiSplitLayout.Split();
			_modelRoot.setChildren(children);
		}
		
		multiSplitPane.getMultiSplitLayout().setModel(_modelRoot);
		
		//if(SCROLL) this.add(new JScrollPane(multiSplitPane));
		//else 
		this.add(multiSplitPane); //new Dimension(1000,1000))));
		
		j = new Justifications[choices.length];
		js = new JScrollPane[choices.length];
		jl = new JLabel[choices.length];
		for(int k=0; k<choices.length; k++){
			Justifications jus;
			jus = new Justifications(300);
			j[k]=jus;
			JustificationsModel model = jus.getModel();
			D_MotionChoice choice = choices[k];
			String short_name = (choice==null)?(_("Choice")+"_"+k):choice.short_name;
			model.setCrtChoice(short_name);
			if(DEBUG) System.out.println("JBC:update_layout: name="+choices[k].short_name);
			js[k]=jus.getScrollPane();
			jl[k]=new JLabel();
			jl[k].setPreferredSize(new Dimension(0,TOTALS_HEIGHT));
			multiSplitPane.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT,jl[k],js[k]),short_name);
			//j[k].setPreferredSize(new Dimension(1000,1000));
		}
		//multiSplitPane.setPreferredSize(new Dimension(1000,1000));
		//else this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,multiSplitPane,new JPanel())); //)));
	}

	private void update_one() {
		if(_DEBUG) System.out.println("JBC:update_out");
		j = new Justifications[1];
		js = new JScrollPane[1];
		jl = new JLabel[1];
		j[0] = new Justifications(300);
		js[0] = j[0].getScrollPane();
		jl[0]=new JLabel();
		jl[0].setPreferredSize(new Dimension(0,TOTALS_HEIGHT));
		this.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, jl[0],js[0]));
	}

	private void clean() {
		if(DEBUG) System.out.println("JBC:clean");
		if(multiSplitPane!=null) this.remove(multiSplitPane);
		try{
			for(Component comp : j) this.remove(comp);
		}catch(Exception e){if(DD.DEBUG_TODO)e.printStackTrace();}
		j = new Justifications[0];
		choices = new D_MotionChoice[0];
		jl = new JLabel[0];
		js = new JScrollPane[0];
	}
	
	ArrayList<JustificationsListener> listeners=new ArrayList<JustificationsListener>();
	/*
	public void fireForceEdit(String orgID) {		
		if(DEBUG) System.out.println("Justifications:fireForceEdit: row="+orgID);
		for(JustificationsListener l: listeners){
			if(DEBUG) System.out.println("Justifications:fireForceEdit: l="+l);
			try{
				if(orgID==null) ;//l.forceEdit(orgID);
				else l.forceEdit(orgID);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	void fireListener(int row, int col) {
		if(_DEBUG) System.out.println("Justifications:fireListener: row="+row);
		for(JustificationsListener l: listeners){
			if(DEBUG) System.out.println("Justifications:fireListener: l="+l);
			try{
				if(row<0) l.justUpdate(null, col);
				else l.justUpdate(Util.getString(this.getModel()._justifications[this.convertRowIndexToModel(row)]), col);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	*/
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
		int w = DD.tabbedPane.getWidth();
        //System.out.println("componentResized: f="+DD.frame.getWidth());
        //System.out.println("componentResized: s="+this.multiSplitPane.getWidth());
        //System.out.println("componentResized: t="+w);
		/*
			int scroll = 0;
			if ((js!=null)&&(js.length>0)) scroll = js[0].getVerticalScrollBar().getSize().width;
			for(Justifications _j :j) _j.setPreferredSize(new Dimension(w/choices.length-DIV-scroll,_j.getSize().height));
		*/
		//this.multiSplitPane.setPreferredSize(DD.tabbedPane.getSize()-20);
		if(multiSplitPane!=null)this.multiSplitPane.setPreferredSize(new Dimension(w-20-DIV*choices.length, DD.tabbedPane.getHeight()-75-jl[0].getHeight()));
		//this.setPreferredSize(new Dimension(w-20, DD.tabbedPane.getHeight()-20-100));
	}
	
}
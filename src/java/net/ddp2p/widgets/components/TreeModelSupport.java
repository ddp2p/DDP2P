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
 package net.ddp2p.widgets.components;
import javax.swing.event.*;
import java.util.*;

public class TreeModelSupport {
    protected static final boolean DEBUG = false;
	Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();
    /* Adds a listener for the TreeModelEvent posted after the tree changes.*/
    public void	addTreeModelListener(TreeModelListener listener) {
    	if ( listener != null && !listeners.contains( listener ) ) {
    		listeners.addElement( listener );
    	}
    }
    /* Removes a listener previously added with addTreeModelListener.*/
    public void	removeTreeModelListener(TreeModelListener listener) {
    	if ( listener != null ) {
    		listeners.removeElement( listener );
    	}
    }
    public void raiseEvent(Object[] parent,Object[] removed) {
    	for(int i=0; i<listeners.size(); i++) {
    		if(DEBUG) System.err.println("***handle listener: "+listeners.get(i));
    		// listeners.get(i).treeNodesChanged(new TreeModelEvent(this,parent));
    		//if(removed != null)listeners.get(i).treeNodesRemoved(new TreeModelEvent(this,removed));
    		listeners.get(i).treeStructureChanged(new TreeModelEvent(this,parent));
    	}
    }
    public void fireTreeNodesChanged( TreeModelEvent e ) {
    	if(DEBUG) System.err.println("***fireTreeNodesChanged: "+e);
    	Enumeration<TreeModelListener> listeners = this.listeners.elements();
    	while ( listeners.hasMoreElements() ) {
    		TreeModelListener listener = (TreeModelListener)listeners.nextElement();
    		listener.treeNodesChanged( e );
    	}
    }
    public void fireTreeNodesInserted( TreeModelEvent e ) {
    	if(DEBUG) System.err.println("***fireTreeNodesInserted: "+e);
	Enumeration<TreeModelListener> listeners = this.listeners.elements();
	while ( listeners.hasMoreElements() ) {
	    TreeModelListener listener = (TreeModelListener)listeners.nextElement();
	    listener.treeNodesInserted( e );
	}
    }
    
    public void fireTreeNodesRemoved( TreeModelEvent e ) {
    	if(DEBUG) System.err.println("***fireTreeNodesRemoved: "+e);
	Enumeration<TreeModelListener> listeners = this.listeners.elements();
	while ( listeners.hasMoreElements() ) {
	    TreeModelListener listener = (TreeModelListener)listeners.nextElement();
	    listener.treeNodesRemoved( e );
	}
    }

    public void fireTreeStructureChanged( TreeModelEvent e ) {
    	if(DEBUG) System.err.println("***fireTreeStructureChanged: "+e);
	Enumeration<TreeModelListener> listeners = this.listeners.elements();
	while ( listeners.hasMoreElements() ) {
	    TreeModelListener listener = (TreeModelListener)listeners.nextElement();
	    listener.treeStructureChanged( e );
	}
    }

    /*
    public void treeNodesChanged(TreeModelEvent e) {
	System.err.println("Change: "+e);TreePath tp=e.getTreePath();System.err.println("Change: "+tp);}
    public void treeNodesInserted(TreeModelEvent e) {}
    public void treeNodesRemoved(TreeModelEvent e) {}
    public void treeStructureChanged(TreeModelEvent e) {}
    */
}

/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.common.population;

import static net.ddp2p.common.util.Util.__;

public abstract class ConstituentsBranch extends ConstituentsNode {
	protected static final boolean DEBUG = false;
	private int nchildren = 0;
	ConstituentsInterfaceInput model;
	Constituents_AddressAncestors ancestors[];
	private ConstituentsNode children[]=new ConstituentsNode[0];
	private int neighborhoods = 0;
	ConstituentsBranch (ConstituentsInterfaceInput _model, ConstituentsBranch _parent,
			Constituents_AddressAncestors[] _ancestors, int _nchildren) {
		super(_parent);
		model = _model;
		ancestors = _ancestors;
		setNchildren(_nchildren);
		//if(ancestors == null) System.err.println("ROOOOOOOOOR\nROOOOOT?\nROOOOT!");
	}
    public abstract int getIndexOfChild(Object child);
    public abstract void populate();
    public int getChildCount() {
    	return getNchildren();
    }
	public void colapsed() {
		if(DEBUG) System.err.println("colapsed: "+this);
		if(this != model.getRoot()) setChildren(new ConstituentsNode[0]);
		if(getNchildren() == 0) setNchildren(1);
	}

	public boolean isColapsed() {
		if ((getChildren()==null) || (getChildren().length == 0))
			return true;
		return false;
	}

    public String convertValueToText() {
    	return toString();
    }
    public void setNChildren(int _nchildren) {
    	setNchildren(_nchildren);
    	Object[] path2parent;
    	if(DEBUG) System.err.println("#children:"+getNchildren()+" for "+this);
    	if(getParent() == null) {return;}
    	path2parent = getParent().getPath();
    	if(getParent().getNchildren() == ((ConstituentsAddressNode)getParent()).getChildren().length) {
    		int idx = getParent().getIndexOfChild(this);
    		if(idx<0) return;
    		model.updateCensus(this, path2parent, idx);
    	}
    }
    public Object getChild (int index) {
		if (DEBUG) System.err.println("ConstituentsBranch:getChild:Creating ConstituentsPropertyNode ");
    	if (index < 0) return null;
     	if ((getChildren().length == 0) && (this != model.getRoot())) populate();
    	if (index < getChildren().length) return getChildren()[index];
    	System.out.println("ConstituentsModel: getChild: "+index+" this="+this);
    	return __("Right click for a menu!");
    }
    public void populateChild(ConstituentsNode child, int index) {
    	ConstituentsNode _children[] = new ConstituentsNode[getChildren().length+1];
    	if((index<0) || (index>getChildren().length)) return;
    	for(int i=0; i<index; i++)	_children[i] = getChildren()[i];
    	_children[index] = child;
    	for(int i=index; i<getChildren().length;i++) _children[i+1] = getChildren()[i];
    	setChildren(_children);
    }
    public void addChild(ConstituentsNode child, int index) {
    	populateChild(child, index);
    	if(getChildren().length > getNchildren())
    		setNChildren(getNchildren()+1);
    }
	public int getNeighborhoods() {
		return neighborhoods;
	}
	public void setNeighborhoods(int neighborhoods) {
		this.neighborhoods = neighborhoods;
	}
	public int getNchildren() {
		return nchildren;
	}
	public void setNchildren(int nchildren) {
		this.nchildren = nchildren;
	}
	public ConstituentsNode[] getChildren() {
		return children;
	}
	public void setChildren(ConstituentsNode children[]) {
		this.children = children;
	}
}
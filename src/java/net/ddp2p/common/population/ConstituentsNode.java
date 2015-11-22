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
package net.ddp2p.common.population;
public class ConstituentsNode {
	private ConstituentsBranch parent;
	public Object[] getPath(){
		if(getParent() == null) return new Object[]{this};
		Object[] presult = getParent().getPath();
		Object[] result = new Object[presult.length+1];
		for(int k=0; k<presult.length; k++) result[k] = presult[k];
		result[presult.length] = this;
		return result;
	}
	public ConstituentsNode(ConstituentsBranch _parent){
		setParent(_parent);
	}
	public ConstituentsBranch getParent() {
		return parent;
	}
	public void setParent(ConstituentsBranch parent) {
		this.parent = parent;
	}
}

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
import static net.ddp2p.common.util.Util.__;
public class ConstituentsPropertyNode extends ConstituentsNode {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private ConstituentProperty property;
	public ConstituentsPropertyNode (ConstituentProperty _property, ConstituentsBranch _parent) {
		super(_parent);
		setProperty(_property);
		if(DEBUG) System.err.println("Creating ConstituentsPropertyNode: "+getProperty());
	}
	public String getTip() {
		if(DEBUG)System.err.println("ConstituentModel:ConstituentPropertyNode:getTip");
		if(getProperty()==null) return __("Poperty not initialized!");
    	if(getProperty().label==null) return getProperty().explain+"("+getProperty().OID_name+": "+getProperty().OID+")";
    	if(getProperty().explain == null)
    		return getProperty().label+" ("+getProperty().OID_name+": "+getProperty().OID+")";
    	return getProperty().label+"("+getProperty().explain+": "+getProperty().OID_name+": "+getProperty().OID+")";
	}
	public long get_field_valuesID(){
		if(getProperty() == null) return -1;
		return getProperty().field_valuesID;
	}
	public String toString() {
		if(getProperty()==null){
			if(DEBUG) System.err.println("ConstituentsPropertyModel: toString null const");
			return __("Unknown!");
		}
		return getProperty().value;
	}
	public ConstituentProperty getProperty() {
		return property;
	}
	public void setProperty(ConstituentProperty property) {
		this.property = property;
	}
}

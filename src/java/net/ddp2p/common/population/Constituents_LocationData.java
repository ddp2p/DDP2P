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

public class Constituents_LocationData {
	public String value;
	public int inhabitants;
	public long organizationID;
	private String label;
	public long fieldID;
	public long field_valuesID;
	public String list_of_values;
	public int partNeigh;
	public long fieldID_above;
	private long fieldID_default_next;
	public String tip;
	public long neighborhood;
	public boolean censusDone = false;
	
	public String toString(){
		return "LocationData: "+value+" #"+fieldID;
	}
	public long getFieldID_default_next() {
		return fieldID_default_next;
	}
	public void setFieldID_default_next(long fieldID_default_next) {
		this.fieldID_default_next = fieldID_default_next;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
}
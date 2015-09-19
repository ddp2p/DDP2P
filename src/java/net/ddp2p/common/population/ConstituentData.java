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

import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.IconObject;

public class ConstituentData {
	private String cGID;
	private long constituentLID;
	private String given_name;
	private String surname; 
	public int witness_for;
	public int witness_against;
	private DisplayableIconObject icon;
	private String slogan;
	public boolean inserted_by_me;
	public int witnessed_by_me; // 1 for positive, -1 for negative, 2-added by myself
	public boolean external;
	public String email;
	public String submitter_ID;
	public boolean blocked;
	public boolean broadcast;
	public int witness_neuter;
	public int myself;
	public D_Constituent constituent;
	public ConstituentData(D_Constituent constituent) {
		this.constituent = constituent;
	}
	public String toString() {
		return "ConstituentData: "+getSurname()+"," +getGivenName()+": "+getSlogan()+"="+ witness_for+"/"+witness_against;
	}
	public String getC_GID() {
		if (constituent != null) return constituent.getGID();
		return cGID;
	}
	public void setC_GID(String global_constituentID) {
		this.cGID = global_constituentID;
	}
	public long getC_LID() {
		if (constituent != null) return constituent.getLID();
		return constituentLID;
	}
	public void setC_LID(long constituentLID) {
		this.constituentLID = constituentLID;
		// uncomment next if always using constituent
		//constituent = D_Constituent.getConstByLID(constituentLID, false, false);
	}
	public String getSlogan() {
		if (constituent != null) return constituent.getSlogan();
		return slogan;
	}
	public void setSlogan(String slogan) {
		this.slogan = slogan;
	}
	public String getGivenName() {
		if (constituent != null) return constituent.getForename();
		return given_name;
	}
	public void setGivenName(String given_name) {
		this.given_name = given_name;
	}
	public String getSurname() {
		if (constituent != null) return constituent.getSurname();
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	public DisplayableIconObject getIcon() {
		if (constituent != null) return displayableIcon(constituent.getIconObject());
		return icon;
	}
	public void setIcon(IconObject icon) {
		this.icon = displayableIcon(icon);
	}
	/**
	 * Converts the iconObject serialized inside a constituent, into an icon displayable by this GUI.
	 * Will have to subclass that visual class and to implement the interface.
	 * @param iconObject
	 * @return
	 */
	static public DisplayableIconObject displayableIcon(IconObject iconObject) {
		// TODO Auto-generated method stub
		return null;
	}
}
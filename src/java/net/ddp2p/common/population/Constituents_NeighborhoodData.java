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
import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.D_Neighborhood;
public class Constituents_NeighborhoodData {
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String address;
	public long neighborhoodID = -2;
	public String global_nID;
	public long parent_nID;
	public String name;
	public Language name_lang;
	public String name_division;
	public Language name_division_lang;
	public String names_subdivisions;
	public Language name_subdivisions_lang;
	public long submitterID;
	public long organizationID;
	public byte[] signature;
	public boolean blocked;
	public boolean broadcasted;
	void getFrom(D_Neighborhood dn) {
		if (dn == null) return;
		organizationID = dn.getOrgLID();
		address = dn.getAddress();
		neighborhoodID = dn.getLID();
		global_nID = dn.getGID();
		parent_nID = dn.getParentLID();
		name = dn.getName();
		name_lang = dn.getName_Language();
		name_division = dn.getName_division();
		name_division_lang = dn.getName_division_Language();
		names_subdivisions = dn.getNames_subdivisions_str();
		name_subdivisions_lang = dn.getNames_subdivisions_Language();
		submitterID = dn.getSubmitterLID();
		signature = dn.getSignature();
	}
	public String toString(){
		String result="";
		result += "address="+address+"\n";
		result += "neighborhoodID="+neighborhoodID+"\n";
		result += "global_nID="+global_nID+"\n";
		result += "parent_nID="+parent_nID+"\n";
		result += "name="+name+"\n";
		result += "name_lang="+name_lang+"\n";		
		result += "name_division="+name_division+"\n";		
		result += "name_division_lang="+name_division_lang+"\n";		
		result += "names_subdivisions="+names_subdivisions+"\n";		
		result += "name_subdivisions_lang="+name_lang+"\n";		
		result += "submitterID="+submitterID+"\n";		
		result += "organizationID="+organizationID+"\n";		
		return result;
	}
	/**
	 * Get the neighborhood subdivision at index idx
	 * @param idx
	 * @return
	 */
	public String getChildDivision(int idx) {
		return D_Neighborhood.getChildDivision(names_subdivisions, idx);
	}
	/**
	 * Returns the subdivisions after the idx-th position
	 * @param idx
	 * @return
	 */
	public String getChildSubDivision(int idx) {
		return D_Neighborhood.getChildSubDivision(names_subdivisions, idx);
	}
	public Constituents_NeighborhoodData(long nID, long organizationID) {
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(nID, true, false);
		this.organizationID = organizationID;
		getFrom(dn);
	}
	public Constituents_NeighborhoodData(String value, long parent_nID, long organizationID) {
		read(value, parent_nID, organizationID);
	}
	public Constituents_NeighborhoodData(long nID, long parent_nID, long organizationID) {
		read(nID, parent_nID, organizationID);
	}
	public Constituents_NeighborhoodData() {
		this.neighborhoodID = -2;
	}
	void read(String value, long parent_nID, long organizationID) {
		if (parent_nID == -2) {
			this.name = value;
			this.organizationID = organizationID;
			this.neighborhoodID = -2;
			this.parent_nID = parent_nID;
			return;
		}
		this.organizationID = organizationID;
		long LID = D_Neighborhood.getNeighLIDByNameAndParent(value, parent_nID, organizationID);
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(LID, true, false);
		getFrom(dn);
	}
	void read(long nID, long parent_nID, long organizationID) {
		if (parent_nID == -2) {
			this.name = ""+nID;
			this.organizationID = organizationID;
			this.neighborhoodID = -2;
			this.parent_nID = parent_nID;
			return;
		}
		this.organizationID = organizationID;
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(nID, true, false);
		if (dn == null) return;
		if (parent_nID > 0) {
			if (dn.getParentLID() != parent_nID) {
				System.out.println("ConstituentModel: read: inconsistent parent "+parent_nID+ " for "+ dn);
				return;
			}
		} else {
			if (dn.getParentLID() > 0) {
				System.out.println("ConstituentModel: read: inconsistent null parent "+parent_nID+ " for "+ dn);
				return;
			}
		}
		getFrom(dn);
	}
}

/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
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
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

import data.D_Organization;

class OrgItem {
	private String org_name;
	D_Organization org;
	private byte[] icon;
	
	public String toString() {
		if (org == null) return getOrgName();
		return org.getOrgNameOrMy();
	}

	public byte[] getIcon() {
		if (icon != null) return icon;
		if (org != null) return org.getIcon();
		return null;
	}

	public void setIcon(byte[] icon) {
		this.icon = icon;
	}

	public String getOrgName() {
		if (org_name != null) return org_name;
		if (org != null) return org.getOrgNameOrMy();
		return "Why no name!";
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}
}
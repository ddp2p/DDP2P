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

package config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import util.P2PDDSQLException;

import util.P2PDDSQLException;
import util.Util;
import widgets.org.Orgs;

public class AppListener implements ActionListener {

	private static final boolean DEBUG = false;

	@Override
	public void actionPerformed(ActionEvent ae) {
		if(DD.COMMAND_NEW_ORG.equals(ae.getActionCommand())){
			createNewOrganization();
		}
	}
	/**
	 * create a new organization into the database, and highlight it in listening Orgs widgets
	 */
	void createNewOrganization() {
		long org_id;
		try {
			String currentTime = Util.getGeneralizedTime();
			org_id=Application.db.insert(table.organization.TNAME,
					new String[]{table.organization.creation_date, table.organization.creation_date, table.organization.hash_org_alg},
					new String[]{currentTime, currentTime, table.organization.hash_org_alg_crt});
			if(DEBUG) System.out.println("AppListener:createNewOrganization: inserted org_id="+org_id);
			Orgs ao = Application.orgs;
			if(ao!=null){
				if(DEBUG) System.out.println("AppListener:createNewOrganization: org_id="+org_id);
				ao.setCurrent(org_id);
				//Application.orgs.revalidate();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

}

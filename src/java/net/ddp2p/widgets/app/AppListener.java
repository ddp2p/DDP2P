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
package net.ddp2p.widgets.app;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.org.Orgs;
/**
 * Meant as a unified point of access for action from buttons on various widgets.
 * Only used for creating an organization 
 * @author msilaghi
 *
 */
public class AppListener implements ActionListener {
	private static final boolean DEBUG = false;
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (DD.COMMAND_NEW_ORG.equals(ae.getActionCommand())){
			createNewOrganization();
		}
	}
	/**
	 * create a new organization into the database, and highlight it in listening Orgs widgets
	 */
	static void createNewOrganization() {
		long org_id;
		org_id = D_Organization.createNewOrg();
		if(DEBUG) System.out.println("AppListener:createNewOrganization: inserted org_id="+org_id);
		Orgs ao = GUI_Swing.orgs;
		if (ao != null) {
			if (DEBUG) System.out.println("AppListener:createNewOrganization: org_id="+org_id);
			ao.setCurrent(org_id);
		}
	}
}

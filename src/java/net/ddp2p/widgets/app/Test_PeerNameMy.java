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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
public
class Test_PeerNameMy {
	public static void main(String args[]) {
		String dbfile;
		long ID;
		String name;
		if (args.length < 1) {
			dbfile = Application.DELIBERATION_FILE;
		} else dbfile = args[0];
		if (args.length < 2) {
			ID = 0;
		} else ID = Long.parseLong(args[1]); 
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		try {
			Application.setDB(new DBInterface(dbfile));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		D_Peer p = D_Peer.getPeerByLID_NoKeep(ID, true);
		System.out.println("Peer: "+p);
		if (p == null) {
			System.out.println("Peer: no p=: "+p);
			return;
		}
		if (args.length < 3) {
			return; 
		} else name = args[2];
		p = D_Peer.getPeerByPeer_Keep(p);
		if (p == null) {
			System.out.println("Peer: cannot keep: "+p);
			return;
		}
		p.setName_My(name);
		p.dirty_my_data = true;
		p.storeRequest();
		p.releaseReference();
		try {
			synchronized(p) {
				p.wait(1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

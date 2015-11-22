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
package util.tools;
import java.io.File;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DD_Address;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class TestBMP {
	/**
	 * call path peerLID
	 * @param args
	 */
	public static void main (String [] args) {
		if (args.length != 2) {
			System.out.println("To encode the StegoStructure based on D_Peer 'peerLID', into file 'fileURL', call as:\n"
					+ "program fileURL peerLID");
			return;
		}
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		String fileURL = args[0];
		File file = new File(fileURL);
		String[] explain = new String[1];
		long l = Util.lval(args[1]);
		D_Peer p = D_Peer.getPeerByLID(l, true, false);
		if (p == null) {
			System.out.println("No Peer: "+l);
			return;
		}
		System.out.println("Peer: "+p);
		boolean r = DD.embedPeerInBMP(file, explain, new DD_Address(p));
		if (!r) 
			System.out.println("failure: "+explain[0]);
		else
			System.out.println("success: "+explain[0]);
	}
}

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
package net.ddp2p.widgets.app;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;

public
class Test_Peer {
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		System.out.println("D_Peer:main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		/*
		HandlingMyself_Peer.loadIdentity(null);
		D_Peer me = HandlingMyself_Peer.getPeer(Identity.current_peer_ID);
		System.out.println("D_Peer: main: me= "+me);
		HandlingMyself_Peer.setMyself(me, false);
		*/
		
		//D_Peer d = D_Peer.getPeerByLID(1, true);
		//System.out.println("D_Peer: main:read(1) "+d);
		D_Peer d2 = D_Peer.getPeerByLID_NoKeep(pID, true);
		//System.out.println("D_Peer: main: cache="+dumpDirCache());
		System.out.println("\nD_Peer: main: d["+pID+"]="+d2);
		if (d2 == null) {
			System.out.println("D_Peer: no d2=: " + d2);
			return;
		}
		
		//System.out.println("\nD_Peer: main 1: ********\n");
		//d2.verifySignature();
		//System.out.println("\nD_Peer: main 2: ********\n");
		//d2.sign();
		System.out.println("\nD_Peer: main 3: ********\n");
		if (verif > 0) {
			boolean r = d2.verifySignature();
			System.out.println("\nD_Peer: main 3: verif result="+r);			
		}
		if (sign > 0) {
			d2.sign();
			boolean r2 = d2.verifySignature();
			System.out.println("\nD_Peer: main 4: verif result=" + r2);			
		}
		if (store > 0) d2.storeSynchronouslyNoException();
		
	}	
}
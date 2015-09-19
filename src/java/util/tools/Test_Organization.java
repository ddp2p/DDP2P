package util.tools;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;

public
class Test_Organization {
	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		System.out.println("D_Organization: main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		/*
		HandlingMyself_Peer.loadIdentity(null);
		D_Organization me = HandlingMyself_Peer.getPeer(Identity.current_peer_ID);
		System.out.println("D_Organization: main: me= "+me);
		HandlingMyself_Peer.setMyself(me, false);
		*/
		
		//D_Organization d = D_Organization.getPeerByLID(1, true);
		//System.out.println("D_Organization: main:read(1) "+d);
		D_Organization d2 = D_Organization.getOrgByLID_NoKeep(pID, true);
		//System.out.println("D_Organization: main: cache="+dumpDirCache());
		System.out.println("\nD_Organization: main: d["+pID+"]="+d2);
		if (d2 == null) {
			System.out.println("D_Organization: no d2=: " + d2);
			return;
		}
		
		//System.out.println("\nD_Organization: main 1: ********\n");
		//d2.verifySignature();
		//System.out.println("\nD_Organization: main 2: ********\n");
		//d2.sign();
		System.out.println("\nD_Organization: main 3: ********\n");
		if (verif > 0) {
			boolean r = d2.verifySignature();
			System.out.println("\nD_Organization: main 3: verif result="+r);			
		}
		if (sign > 0) {
			d2.sign();
			boolean r2 = d2.verifySignature();
			System.out.println("\nD_Organization: main 4: verif result=" + r2);			
		}
		if (store > 0) d2.storeSynchronouslyNoException();
		
	}	
}
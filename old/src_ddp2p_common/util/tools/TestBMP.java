package util.tools;

import java.io.File;

import util.DBInterface;
import util.DD_Address;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.DD;
import data.D_Peer;

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
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
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
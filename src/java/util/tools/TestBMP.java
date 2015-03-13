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
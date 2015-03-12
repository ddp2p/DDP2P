package util.tools;

import hds.DirectoryRequest;
import hds.DirectoryServer;
import hds.DirectoryServerUDP;
import streaming.GlobalClaimedDataHashes;
import streaming.OrgPeerDataHashes;
import util.DBInterface;
import util.GetOpt;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import data.D_Organization;

public class DumpOrgPeerHashes {
	private static final boolean DEBUG = false;

	static public void main(String args[]) {
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		System.out.println("Running DumpOrgPeerHashes parameters="+args.length);
		if (args.length == 0) return;
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			long oLID = -1;
			
			char c;
			opts:
			while ((c = GetOpt.getopt(args, "d:O:h")) != GetOpt.END) {
				System.out.println("Options received"+GetOpt.optopt +" optind="+GetOpt.optind+" arg="+GetOpt.optarg);
				switch (c) {
				case 'h':
					System.out.println("java -cp jars/sqlite4java.jar:jars/sqlite-jdbc-3.7.2.jar:jars/DD.jar util.tools.AddrBook "
							+ "\n\t -h          Help"
							+ "\n\t -d file     Use file as current database (dir database in the same folder)"
							+ "\n\t -O oLID     organization LID"
							);
					//printHELP();
					//break;
					System.exit(-1);//return;
				case 'd':
					if (DEBUG) System.out.println("Option d: "+GetOpt.optarg);
					Application.DIRECTORY_FILE = GetOpt.optarg;
					break;
				case 'O':
					if (DEBUG) System.out.println("Option O: "+GetOpt.optarg);
					oLID = Util.lval(GetOpt.optarg);
					break;
				case GetOpt.END:
					if (DEBUG) System.out.println("REACHED END OF OPTIONS");
					break;
				case '?':
					System.out.println("Options ?:"+GetOpt.optopt +" optind="+GetOpt.optind+" "+GetOpt.optarg);
					return;
				case ':':
					System.out.println("Options \":\" for "+GetOpt.optopt);
					break;
				default:
					System.out.println("DumpOrgPeerHashes: unknown option error: \""+c+"\"");
					break opts;
					//return;
				}
			}
			
			if (oLID <= 0) {
				// System.out.println("DumpOrgPeerHashes: unknown organization error: \""+oLID+"\"");
				GlobalClaimedDataHashes o = streaming.GlobalClaimedDataHashes.get();
				System.out.println("DumpOrgPeerHashes: Globals: \""+o+"\"");
				return;
			}
			
			D_Organization org = D_Organization.getOrgByLID(oLID, true, false);
			OrgPeerDataHashes sp = org.getSpecificRequest();
			System.out.println("DumpOrgPeerHashes: requests: \""+sp+"\"");
			
			
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}
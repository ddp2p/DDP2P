package util.tools;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.hds.DirectoryRequest;
import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.DirectoryServerUDP;
import net.ddp2p.common.streaming.GlobalClaimedDataHashes;
import net.ddp2p.common.streaming.OrgPeerDataHashes;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.GetOpt;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class DumpOrgPeerHashes {
	private static final boolean DEBUG = false;
	static public void main(String args[]) {
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		System.out.println("Running DumpOrgPeerHashes parameters="+args.length);
		if (args.length == 0) return;
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
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
					System.exit(-1);
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
				}
			}
			if (oLID <= 0) {
				GlobalClaimedDataHashes o = net.ddp2p.common.streaming.GlobalClaimedDataHashes.get();
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

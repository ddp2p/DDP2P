package net.ddp2p.common.data;

import java.util.regex.Pattern;

public class D_GIDH {
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	public static final String d_Tran = "L:"; 
	public static final String d_Vote = "V:";
	public static final String d_News = "E:";
	public static final String d_Peer = "P:";
	public static final String d_OrgGrassSign = "G:";
	public static final String d_OrgAuthSign = "O:";
	public static final String d_ConsE = "C:"; // external
	public static final String d_ConsR = "R:"; // with key
	public static final String d_Neigh = "N:";
	public static final String d_Moti  = "M:";
	public static final String d_Just  = "J:";
	public static final String d_OrgExtraFieldSign = "F:";
	public static final String d_Witn = "W:"; 
	public static final String d_Tester = "T:"; 

	/**
	 * Checks that this is compacted at transmission into a number (dictionary entry)
	 * @param gID
	 * @return
	 */
	public static boolean isCompactedGID(String gID) {
		return (gID != null) && (gID.length() < 4);
	}
	public static boolean isGIDH(String gIDH) {
		if (gIDH == null) return false;
		//String pat = Pattern.quote(":");  // do not use pattern with indexOf
		int i = gIDH.indexOf(":");
		//if (DEBUG) System.out.println("D_GIDH: isGIDH -> p="+i+" "+i+" <-"+gIDH);
		return (i == 1);
	}
	public static boolean isGID(String gID) {
		if (gID == null) return false;
		//String pat = Pattern.quote(":");
		int i = gID.indexOf(":");
		//if (DEBUG) System.out.println("D_GIDH: isGIDH -> p="+i+" "+i+" <-"+gID);
		if (gID.startsWith(d_OrgGrassSign)) return true;
		if (gID.startsWith(d_ConsE)) return true;
		return (i != 1);
	}
	public static String getGID(String gid_or_hash) {
		if (isGID(gid_or_hash)) return gid_or_hash;
		return null;
	}
	public static String getGIDH(String gid_or_hash) {
		if (isGIDH(gid_or_hash)) return gid_or_hash;
		return null;
	}
}

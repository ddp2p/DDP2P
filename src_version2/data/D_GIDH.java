package data;

public class D_GIDH {
	public static final String d_Peer = "P:";
	public static final String d_OrgGrassSign = "G:";
	public static final String d_OrgAuthSign = "O:";
	public static final String d_ConsE = "C:";
	public static final String d_ConsR = "R:";
	public static final String d_Neigh = "N:";
	public static final String d_Moti  = "M:";
	public static final String d_Just  = "J:";
	public static String d_OrgExtraFieldSign = "F:"; 

	/**
	 * Checks that this is compacted at transmission into a number (dictionary entry)
	 * @param gID
	 * @return
	 */
	public static boolean isCompactedGID(String gID) {
		return (gID != null) && (gID.length() < 4);
	}
}

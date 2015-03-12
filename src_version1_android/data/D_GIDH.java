package data;

public class D_GIDH {
	public static final String d_Peer = "P:";

	public static boolean isCompactedGID(String gID) {
		return (gID != null) && (gID.length() < 4);
	}
}

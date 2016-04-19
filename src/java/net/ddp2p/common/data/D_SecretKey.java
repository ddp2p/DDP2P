package net.ddp2p.common.data;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class D_SecretKey {
	private static final boolean DEBUG = false;
	static final String sql_orgGIDitem =
			"SELECT k."+net.ddp2p.common.table.key.public_key+",k."+net.ddp2p.common.table.key.ID_hash+",k."+net.ddp2p.common.table.key.name+
			" FROM " + net.ddp2p.common.table.key.TNAME + " AS k " +
			" WHERE "+net.ddp2p.common.table.key.secret_key+" IS NOT NULL;";
	public static ArrayList<OrgGIDItem> getOrgGIDItems() {
		ArrayList<OrgGIDItem> result = new ArrayList<OrgGIDItem>();
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.getDB().select(sql_orgGIDitem, new String[]{}, DEBUG);
			for (ArrayList<Object> i: c) {
				String gid = Util.getString(i.get(0));
				String hash = Util.getString(i.get(1));
				String k_name = Util.getString(i.get(2));
				String name, p_name = null; 
				D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(gid, null, true, false, false, null);
				if (peer != null) p_name = peer.getName_MyOrDefault();
				if (p_name != null) name = __("PEER:")+" "+p_name;
				else name = k_name;
				OrgGIDItem crt = new OrgGIDItem(gid,hash,name);
				result.add(crt);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
}

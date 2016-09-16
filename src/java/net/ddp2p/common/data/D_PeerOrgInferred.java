package net.ddp2p.common.data;

import java.util.ArrayList;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

/**
 * Here store relations peer - organization about which they advertised GIDHs.
 * We will contact them for it even if they do not broadcast the org.
 * 
 * Should probably add the list of hashes the peer claims to no (longer) have.
 * 
 * @author msilaghi
 *
 */
public class D_PeerOrgInferred {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public long peer_org_inferred_ID;
	public long peer_ID;
	public long organization_ID;
	public boolean dirty;
	
	public D_PeerOrgInferred() {}

	public D_PeerOrgInferred(ArrayList<Object> ipoi) {
		peer_org_inferred_ID = Util.lval(ipoi.get(net.ddp2p.common.table.peer_org_inferred.COL_ID));
		peer_ID = Util.lval(ipoi.get(net.ddp2p.common.table.peer_org_inferred.COL_PEER_ID));
		organization_ID = Util.lval(ipoi.get(net.ddp2p.common.table.peer_org_inferred.COL_ORG_ID));
	}

	public D_PeerOrgInferred(long pID, long orgID, boolean _dirty) {
		this.peer_ID = pID;
		this.organization_ID = orgID;
		this.dirty = _dirty;
	}

	public String toLongString() {
		return "D_PeerOrgInferred[ID=" + peer_org_inferred_ID+" d="+dirty
				+ " pID=" + peer_ID 
				+ " organization_ID = " + organization_ID + "]";
	}
	public String toString() {
		return "D_PeerOrgInferred[ID=" + peer_org_inferred_ID+"(d="+dirty+")"
				+ " pID=" + peer_ID 
				+ " organization_ID = " + organization_ID + "]";
	}

	public void store(D_Peer d_Peer) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_PeerOrgInferred: store: "+this);
		dirty = false;
		String[] params = new String[net.ddp2p.common.table.peer_org_inferred.fields_list.length];
		params[net.ddp2p.common.table.peer_org_inferred.COL_PEER_ID] = d_Peer.getLIDstr();
		params[net.ddp2p.common.table.peer_org_inferred.COL_ORG_ID] = Util.getStringID(organization_ID);
		if (peer_org_inferred_ID > 0) {
			params[net.ddp2p.common.table.peer_org_inferred.COL_ID] = Util.getStringID(peer_org_inferred_ID);
			Application.getDB().update(net.ddp2p.common.table.peer_org_inferred.TNAME,
					net.ddp2p.common.table.peer_org_inferred.fields_no_ID_list,
					new String[]{net.ddp2p.common.table.peer_org_inferred.peer_org_inferred_ID},
					params,
					DEBUG);
		} else {
			try {
				peer_org_inferred_ID = Application.getDB().insert(net.ddp2p.common.table.peer_org_inferred.TNAME,
						net.ddp2p.common.table.peer_org_inferred.fields_list,
						params,
						DEBUG);
			} catch (Exception e) {e.printStackTrace();}
		}
		if (DEBUG) System.out.println("D_PeerOrgInferred: store: null orgGID");
	}
}
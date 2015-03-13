package util.tools;

import static net.ddp2p.common.util.Util.__;

import java.io.File;

import net.ddp2p.ciphersuits.KeyManagement;
import net.ddp2p.ciphersuits.SK;

import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;

/**
  * @param args
 */
public class UpdatePeerSKMyself {
	public static void main(String[]args) {
		if(args.length<2){
			System.out.println("Usage: prog db sk setMyself");
			return;
		}
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		//DEBUG = true;
		//D_PeerAddress.DEBUG = true;
		String db = args[0];
		String sk = args[1];
		try{
			String err;
			if((err=DD.testProperDB(db))!=null) {
				System.out.println("Usage: prog db sk setMyself.\n\n Improper db="+db+"\nerror="+err);
				return;				
			}
			Application.db = new DBInterface(db);
			File fileLoadSK = new File(sk);
			if(!fileLoadSK.exists()) {
				Application_GUI.warning(__("Inexisting file: "+fileLoadSK.getPath()), __("Inexisting file!"));
				return;
			}
			String [] __pk = new String[1];
			SK new_sk = KeyManagement.loadSecretKey(fileLoadSK.getCanonicalPath(), __pk);
			if(new_sk==null){
				System.out.println("Usage: prog db sk setMyself\n empty sk");
				return;
			}
			//String old_gid = model.getGID(row);
			String _pk=__pk[0];//Util.stringSignatureFromByte(new_sk.getPK().getEncoder().getBytes());
			if(_pk==null){
				System.out.println("null PK");
				return;
			}
			D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(_pk, null, true, true, false, null);
			peer.component_basic_data.globalID = _pk;
			peer.component_basic_data.globalIDhash=null;
			peer.setLID(null);

			if ((args.length > 2) && (Util.stringInt2bool(args[2], false))) {
				HandlingMyself_Peer.setMyself(peer, true, Identity.current_peer_ID, false, false); // peer is not kept
				HandlingMyself_Peer.updateAddress(peer);
			}
			peer.sign(new_sk);
			//String peerID = 
			peer.storeSynchronouslyNoException();

		}catch(Exception e2){e2.printStackTrace();}
	} 
	
}
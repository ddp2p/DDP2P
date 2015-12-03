package net.ddp2p.simulator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DDP2P_Peer_Installation;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB;
import net.ddp2p.widgets.app.MainFrame;
class InstallationThread extends Thread {
	int id = -1;
	SocketAddress endpoint;
	public InstallationThread(int id, String interface_ip) {
		this.id = id;
		try {
			InetAddress addr = InetAddress.getByName(interface_ip);
			endpoint = new InetSocketAddress(addr, 7070);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	public void run() {
		try {
			MainFrame.run_instance(Application.installations[id].getDB(), null, null, false);
		} catch (NumberFormatException | P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}
public class Simulator {
	public static void main(String[] args) throws P2PDDSQLException {
		String db_path 			= args[0];
		String interface_list 	= args[1];
		int installation_count 	= InterfaceSupplier.ready_list.size();
		InterfaceSupplier.init(interface_list);
		Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		Application.installations = new DDP2P_Peer_Installation[installation_count];
		DDP2P_Network_Implementation network = new DDP2P_Network_Implementation(installation_count);
		for (int i = 0; i < installation_count; i ++) {
			InetAddress ip = null;
			try {
				ip = InetAddress.getByName("10.0.0." + (1 + i));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			if (ip == null) continue;
			network.add_installation_address(i, ip);
		}
		for (int i = 0; i < installation_count; i ++) {
			String db = db_path + "_" + Integer.toString(i); 
			Application.installations[i] = new DDP2P_Peer_Installation();
			Application.installations[i].setDB(new DBInterface(db));
			String vacant_interface = InterfaceSupplier.getVacant();
			(new InstallationThread(i, vacant_interface)).start();
		}
	}
}

package net.ddp2p.simulator;

import java.util.ArrayList;
import java.util.Hashtable;

import net.ddp2p.common.network.sim.ServerSocket;
import net.ddp2p.common.network.sim.Socket;

public class DDP2P_Network {
	static DDP2P_Network singleton = new DDP2P_Network(0);
	Hashtable<Integer, ArrayList<Object>> queues = new Hashtable<Integer, ArrayList<Object>>();
	public int installations;

	public DDP2P_Network(int installations) {
		this.installations = installations;
		singleton = this;
	}
	
	public static DDP2P_Network getNetwork() {
		// TODO Auto-generated method stub
		return singleton;
	}
	public void register(int port, ServerSocket serverSocket, int installation) {
		
		// TODO Auto-generated method stub
		queues.put(port*installations + installation, new ArrayList<Object>());
	}

	public int register(int port, Socket result,
			int currentInstallationFromThread) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getFreePort(int currentInstallationFromThread) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
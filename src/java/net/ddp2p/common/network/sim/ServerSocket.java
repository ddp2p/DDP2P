package net.ddp2p.common.network.sim;

import java.io.IOException;
import java.net.SocketException;

import net.ddp2p.common.config.Application;
import net.ddp2p.simulator.DDP2P_Network;

public class ServerSocket extends java.net.ServerSocket {

	public ServerSocket() throws IOException {
		super();
		if (! Socket.simulated) return;
		// TODO Auto-generated constructor stub
	}
	
	public ServerSocket(int port) throws IOException {
		super(port);
		if (! Socket.simulated) return;
		// TODO Auto-generated constructor stub

		DDP2P_Network.getNetwork().register(port, this, Application.getCurrentInstallationFromThread());
	}
	
	public void setSoTimeout(int timeout) throws SocketException {
		if (! Socket.simulated) { super.setSoTimeout(timeout); return; }
		
	}
	
	public java.net.Socket	accept() throws IOException {
		if (! Socket.simulated) { return super.accept(); }
		Socket result = new Socket();
		result.port = DDP2P_Network.getNetwork().getFreePort(Application.getCurrentInstallationFromThread());
		DDP2P_Network.getNetwork().register(result.port, result, Application.getCurrentInstallationFromThread());
		return result;
	}
}
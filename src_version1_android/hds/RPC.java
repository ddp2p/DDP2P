package hds;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import util.DirectoryAddress;
import util.P2PDDSQLException;
import util.Util;

import config.DD;
import config.Identity;

public class RPC extends util.DDP2P_ServiceThread {
	ServerSocket rpc;
	String controlIP;
	private static final byte CMD_START_UDPSERVER = 0;
	private static final byte CMD_SET_DIRSERVER = 1;
	public RPC(String _controlIP) {
		super("RPC", true);
		controlIP = _controlIP;
	}

	@Override
	public void _run() {
		try {
			__run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void __run() throws IOException {
		rpc = new ServerSocket(54321);

		for (;;) {
			Socket s = rpc.accept();
			InetSocketAddress ss = (InetSocketAddress) s.getRemoteSocketAddress();
			if (!controlIP.equals(ss.getAddress().toString())) continue;
			InputStream is = s.getInputStream();
			byte []cmd = new byte[1000];
			int sz = is.read(cmd);
			switch(cmd[0]) {
			case CMD_START_UDPSERVER:
				try {
					DD.startUServer(cmd[1] != 0, Identity.current_peer_ID);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			case CMD_SET_DIRSERVER:
				byte cmd2[] = new byte[999];
				Util.copyBytes(cmd2, 0, cmd, cmd2.length, 1);
				String val = new String(cmd2);
				DirectoryAddress.reset(val);
			}
		}
	}
	//rpc.close();
	
}
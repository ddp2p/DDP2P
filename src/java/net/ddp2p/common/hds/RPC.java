/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package net.ddp2p.common.hds;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.util.DirectoryAddress;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

/**
 * This is a class used to remotely control an agent (e.g. starting/stopping various servers, in particular
 * the udpServer and the DirectoryServers).
 * I think it was not yet tested well...
 * 
 * @author msilaghi
 *
 */
public class RPC extends net.ddp2p.common.util.DDP2P_ServiceThread {
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
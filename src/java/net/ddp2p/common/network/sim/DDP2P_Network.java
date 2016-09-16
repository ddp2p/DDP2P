/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.common.network.sim;

import java.net.InetAddress;

public interface DDP2P_Network {

	/**
	 * Called in the constructor of a Server Socket to register it with this
	 * @param port
	 * @param serverSocket
	 * @param installation
	 */
	public void register(int port, ServerSocket serverSocket, int installation);
	/**
	 * 
	 * Called at accept on a ServerSocket, to register a port on the IP of this installation, to be associated with the socket
	 * @param port
	 * @param result
	 * @param currentInstallationFromThread
	 * @return
	 */
	public int register(int port, Socket result, int installation);
	/**
	 * Called at accept on a ServerSocket, to get a new unused port with a client
	 * @param currentInstallationFromThread
	 * @return
	 */
	public int getFreePort(int currentInstallationFromThread) ;
	/**
	 * Return an interface with the list of addresses planned for this instance
	 * @param installation 
	 * @return
	 */
	public DDP2P_NetworkInterface get_NetworkInterface(int installation);
	
	public void close(ServerSocket serverSocket, int installation);
	
	public void close(Socket socket, int installation);
	public Socket accept(ServerSocket serverSocket, int installation, int timeout);
	public void connect(InetAddress address, int port);
	public byte[] read(int installation, int port, Socket socket, int timeout);
	public void write(int installation, int port, Socket socket, byte[] data);
	public void write(Socket socket, byte[] data);
}

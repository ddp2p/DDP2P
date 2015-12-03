/*   Copyright (C) 2015 Marius C. Silaghi and Faris Alsalama
		Author: Marius Silaghi: msilaghi@fit.edu and falsalama2014@my.fit.edu
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
package net.ddp2p.common.network.sim;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
class DDP2P_OutputStream extends OutputStream {
	public DDP2P_OutputStream(Socket socket) {
	}
	@Override
	public void write(int b) throws IOException {
	}
}
class DDP2P_InputStream extends InputStream {
	public DDP2P_InputStream(Socket socket) {
	}
	@Override
	public int read() throws IOException {
		return 0;
	}
}
public class Socket extends java.net.Socket {
	public static DDP2P_Network singleton_network = null; 
	/**
	 * get the singleton
	 * @return
	 */
	public static DDP2P_Network getNetwork() {
		return singleton_network;
	}
	public int port;
	/** Connection: */
	public InetAddress remote_address;
	public int remote_port;
	public Socket () {
		super();
		if (! isSimulated()) return;
	}
	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		if (! isSimulated()) super.connect(endpoint);
		Socket.getNetwork().connect(((InetSocketAddress)endpoint).getAddress(), ((InetSocketAddress)endpoint).getPort());
	}
	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		if (! isSimulated()) super.setSoTimeout(timeout);
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (! isSimulated()) return super.getOutputStream();
		return new DDP2P_OutputStream(this);
	}
	@Override
	public InputStream getInputStream() throws IOException {
		if (! isSimulated()) return super.getInputStream();
		return new DDP2P_InputStream(this);
	}
	public static boolean isSimulated() {
		return singleton_network != null;
	}
}

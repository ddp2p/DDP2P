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
import java.net.SocketException;
import net.ddp2p.common.config.Application;
public class ServerSocket extends java.net.ServerSocket {
	private int timeout = -1;
	public int port;
	public ServerSocket() throws IOException {
		super();
		if (! Socket.isSimulated()) return;
	}
	public ServerSocket(int port) throws IOException {
		super(port);
		if (! Socket.isSimulated()) return;
		this.port = port;
		Socket.getNetwork().register(port, this, Application.getCurrentInstallationFromThread());
	}
	public void setSoTimeout(int timeout) throws SocketException {
		if (! Socket.isSimulated()) { super.setSoTimeout(timeout); return; }
		this.timeout = timeout;
	}
	public java.net.Socket	accept() throws IOException {
		if (! Socket.isSimulated()) { return super.accept(); }
		Socket result = new Socket();
		if ((result = Socket.getNetwork().accept(this, Application.getCurrentInstallationFromThread(), timeout)) == null) throw new IOException();
		result.port = Socket.getNetwork().getFreePort(Application.getCurrentInstallationFromThread());
		Socket.getNetwork().register(result.port, result, Application.getCurrentInstallationFromThread());
		return result;
	}
	@Override
	public void close() {
		Socket.getNetwork().close(this, Application.getCurrentInstallationFromThread());
	}
}

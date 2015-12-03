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
package net.ddp2p.common.network.sim;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import net.ddp2p.common.config.Application;
public class DDP2P_NetworkInterface {
	NetworkInterface _interface;
	boolean simulated = false;
	private Enumeration<InetAddress> addresses;
	DDP2P_NetworkInterface (NetworkInterface _interface) {
		this._interface = _interface;
	}
	public DDP2P_NetworkInterface(Enumeration<InetAddress> enumeration) {
		simulated = true;
		addresses = enumeration;
	}
	public static Enumeration<DDP2P_NetworkInterface> getNetworkInterfaces() throws SocketException {
		ArrayList<DDP2P_NetworkInterface> result = new ArrayList<DDP2P_NetworkInterface>(); 
		if (! Socket.isSimulated()) {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				result.add(new DDP2P_NetworkInterface(netint));
			}
			return Collections.enumeration(result);
		}
		result.add(Socket.getNetwork().get_NetworkInterface(Application.getCurrentInstallationFromThread()));
		return Collections.enumeration(result);
	}
	public Object getDisplayName() {
		if (! simulated) return _interface.getDisplayName();
		return "wifi0";
	}
	public boolean isUp() throws SocketException {
		if (! simulated) return _interface.isUp();
		return true;
	}
	public boolean isLoopback() throws SocketException {
		if (! simulated) return _interface.isLoopback();
		return false;
	}
	public boolean isPointToPoint() throws SocketException {
		if (! simulated) return _interface.isPointToPoint();
		return false;
	}
	public Object getName() {
		if (! simulated) return _interface.getName();
		return "wifi0";
	}
	public boolean isVirtual() {
		if (! simulated) return _interface.isVirtual();
		return false;
	}
	public boolean supportsMulticast() throws SocketException {
		if (! simulated) return _interface.supportsMulticast();
		return true;
	}
	public Enumeration<InetAddress> getInetAddresses() {
		if (! simulated) return _interface.getInetAddresses();
		return addresses;
	}
}

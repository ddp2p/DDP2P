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
package net.ddp2p.common.hds;
import java.net.InetSocketAddress;
import net.ddp2p.common.util.Util;
/**
 * Unlike Address_SocketResolved, this class misses an InetAddress
 * @author msilaghi
 *
 */
public class Address_SocketResolved_TCP{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private Address addr;
	public InetSocketAddress isa_tcp, isa_udp; 
	/**
	 * Sets all parameters
	 * @param _isa_tcp
	 * @param _isa_udp
	 * @param _ad
	 */
	public Address_SocketResolved_TCP(InetSocketAddress _isa_tcp, InetSocketAddress _isa_udp, Address _ad){
		setAddress(_ad);
		if (DEBUG) {
			System.out.println("Address_SocketResolved_TCP <init3>: setting "+_ad.toLongString());
			Util.printCallPath("");
		}
		isa_tcp = _isa_tcp;
		isa_udp = _isa_udp;
		if (isa_udp == null) isa_udp = isa_tcp;
	}
	/**
	 * Sets both the TCP and UDP sockets to the same value: _isa
	 * @param _isa
	 * @param _ad
	 */
	public Address_SocketResolved_TCP(InetSocketAddress _isa, Address _ad) {
		if (DEBUG) {
			System.out.println("Address_SocketResolved_TCP <init2>: setting "+_ad.toLongString());
			Util.printCallPath("");
		}
		setAddress(_ad);
		isa_udp = isa_tcp = _isa;
	}
	public String toString() {
		return "[SockAd_D: ad="+getAddress()+" sock="+isa_tcp+" udp="+isa_udp+"]";
	}
	/**
	 * Creates a Address_SocketResolved_TCP based just on the tcp socket (setting the udp to this tcp)
	 * @param addr
	 * @return
	 */
	public static Address_SocketResolved_TCP getTCP(Address_SocketResolved addr) {
		Address_SocketResolved_TCP sad = new Address_SocketResolved_TCP(addr.isa_tcp, addr.addr);
		return sad;
	}
	/**
	 * Creates a Address_SocketResolved_TCP based just on the udp socket (setting the tcp to this udp)
	 * 
	 * @param addr
	 * @return
	 */
	public static Address_SocketResolved_TCP getUDP(Address_SocketResolved addr) {
		Address_SocketResolved_TCP sad = new Address_SocketResolved_TCP(addr.isa_udp, addr.addr);
		return sad;
	}
	public Address getAddress() {
		return addr;
	}
	public void setAddress(Address ad) {
		this.addr = ad;
	}
}

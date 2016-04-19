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
import java.net.InetAddress;
import java.net.InetSocketAddress;
/**
 * Encapsulate 
 * Address ad, InetAddress ia, InetSocketAddress isa_tcp, isa_udp (ia + ports)
 * @author msilaghi
 *
 */
public class Address_SocketResolved{
	public Address addr;
	InetSocketAddress isa_tcp, isa_udp;
	public InetAddress ia;
	Address_SocketResolved(InetAddress _ia, InetSocketAddress _isa_tcp, InetSocketAddress _isa_udp, Address _ad){
		addr = _ad;
		ia = _ia;
		isa_tcp = _isa_tcp;
		isa_udp = _isa_udp;
	}
	public String toString() {
		return "[SockAddr_D: ad="+addr+" ia="+ia+" tcp="+isa_tcp+" udp="+isa_udp+"]";
	}
}

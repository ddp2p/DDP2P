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

package net.ddp2p.common.network;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.network.pcp.Client_PCP;
import net.ddp2p.common.network.pcp.OpCodeInfo;
import net.ddp2p.common.network.pcp.Request;
import net.ddp2p.common.network.upnp.AddressPortMapping;
import net.ddp2p.common.network.upnp.Client_UPNP;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class NATServer extends net.ddp2p.common.util.DDP2P_ServiceThread {
	public NATServer() {
		super("NATServer", true, null);
	}
	private static final long SLEEP_MIN = 30;
	private static final long SLEEP_SEC = SLEEP_MIN*60;
	private static final long SLEEP_MS = SLEEP_SEC*1000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	/**
	 *  how much longer to map, than the SLEEP_MIN
	 */
	private static final long MAP_MULT = 4; 
	/**

	 */
	String m_crt_internal_IP;
	String m_crt_NAT_IP;
	String m_crt_external_IP;
	String address_external;
	
	int m_crt_external_port_UDP;
	int m_crt_external_port_TCP;
	private boolean stop;
		
	/*
	public void run() {
		try {
			_run();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	if (_DEBUG) System.out.println("NATServer: main: exit");
	}
	*/
	public void _run() {
		boolean first = true;
		for (;!stop;) {
	    	if (DEBUG) System.out.println("NATServer: run: loop");
			synchronized(this) {
				//go to sleep
				try {
					if (! first) {
				    	if (DEBUG) System.out.println("NATServer: main: will wait");
						this.wait(SLEEP_MS);
						if (stop) return;
						this.m_crt_external_port_UDP = -1;
						this.m_crt_external_port_TCP = -1;
					}
					first = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
	    	if (DEBUG) System.out.println("NATServer: run: get IPs");
			ArrayList<InetAddress> non_site = Util.getLocalIPs(false);
	    	if (DEBUG) System.out.println("NATServer: run: non_site#"+non_site.size());
			if (non_site.size() > 0) {
		    	if (DEBUG) System.out.println("NATServer: run: non_sites="+Util.concat(non_site, " ", "N"));
				continue;
			}

			// try upnp	(or pcp?)
			if (try_getting_address()) {
		    	if (DEBUG) System.out.println("NATServer: main: will announce");
								
				// wait for myself
				D_Peer me = net.ddp2p.common.data.HandlingMyself_Peer.get_myself_or_null();
				if (me == null) continue;
				
				Address addr = new Address();
				addr.domain = this.m_crt_external_IP;
				addr.tcp_port = this.m_crt_external_port_TCP;
				addr.udp_port = this.m_crt_external_port_UDP;
				addr.pure_protocol = Address.SOCKET;
				ArrayList<Address> addrs = new ArrayList<Address>();
				addrs.add(addr);
				try {
					HandlingMyself_Peer.updateAddress(me, addrs); // currently adds new addresses besides old, but may want to drop old ones!
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					continue;
				}
				//on success, add address to myself
				UDPServer.announceMyselfToDirectories();
			}
	    	if (DEBUG) System.out.println("NATServer: main: loop");
				
		}
	}

	private boolean try_getting_address() {
		
    	if (DEBUG) System.out.println("NATSErver: try_address: Start");
		if (try_upnp()) {
	    	if (DEBUG) System.out.println("NATSErver: try_address: upnp End true");
			return true;
		}
		if (try_natpmp()) {
	    	if (DEBUG) System.out.println("NATSErver: try_address: upnp End true");
			return true;
		}
    	if (DEBUG) System.out.println("NATServer: try_address: End false");
		return false;
	}
	private boolean try_upnp() {
    	if (DEBUG) System.out.println("NATSErver: try_upnp: Start");
		Client_UPNP client = new Client_UPNP();
        if (! client.discover(Client_UPNP.NAT_MULTICAST, Client_UPNP.UPNP_UDP_BROADCAST_PORT)) {
        	if (_DEBUG) System.out.println("NATSErver: try_upnp: No discovery");
        	return false;
        }
        if (! client.getUpnpDescription() ) {
        	if (_DEBUG) System.out.println("NATSErver: try_upnp: No router description");
        	return false;
        }

        AddressPortMapping mapping = new AddressPortMapping();
        
        address_external = client.getExternalIPAddress( mapping );
        if (address_external == null) {
        	if (_DEBUG) System.out.println("NATSErver: try_upnp: No external");
        	return false;
        }
        try {
			InetAddress address_external_inet =  InetAddress.getByName(address_external);
			
			this.m_crt_external_IP = mapping.getExternalIPAddress();
			
			if (address_external_inet.isSiteLocalAddress() || address_external_inet.isLinkLocalAddress()) {
	        	if (_DEBUG) System.out.println("NATServer: try_upnp: Multilevel: "+address_external_inet);
				return false;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}

        String protocol;
        int internalPortToMap;
        int externalPortToMap;

        internalPortToMap = Identity.getPeerUDPPort();
        if (internalPortToMap <= 0) internalPortToMap = net.ddp2p.common.hds.Server.PORT;
        if (internalPortToMap > 0) {
            protocol = "UDP";
	        externalPortToMap = net.ddp2p.common.hds.Server.PORT;
			if (! client.addPortMapping( internalPortToMap, externalPortToMap, protocol,
	                "DDP2P_UDP_server", SLEEP_MIN*MAP_MULT //0
	                , mapping )) return false;
	        this.m_crt_external_port_UDP = mapping.getExternalPort();
	        this.m_crt_external_IP = mapping.getExternalIPAddress();

	        //client.getExternalIPMapping(mapping, externalPortToMap, protocol);
        }
        
        internalPortToMap = Identity.getPeerTCPPort();
        if (internalPortToMap <= 0) internalPortToMap = net.ddp2p.common.hds.Server.PORT;
        if (internalPortToMap > 0) {
            protocol = "TCP";
	        externalPortToMap = net.ddp2p.common.hds.Server.PORT;
			if (! client.addPortMapping( internalPortToMap, externalPortToMap, protocol,
	                "DDP2P_TCP_server", SLEEP_MIN*MAP_MULT//0
	                , mapping )) return false;
	        this.m_crt_external_port_TCP = mapping.getExternalPort();
	        this.m_crt_external_IP = mapping.getExternalIPAddress();

	        //client.getExternalIPMapping(mapping, externalPortToMap, protocol);
        }
        

		return true;
	}
	public static void main(String args[]) {
		NATServer server = new NATServer();
		server.run();//try_upnp();//server.try_natpmp();		
	}

	public boolean try_natpmp() {
        // General setup.
        Client_PCP client = new Client_PCP();
        AddressPortMapping mapping = new AddressPortMapping();
        
        String natpmpDeviceAddress = address_external; //"10.0.0.1";
        if (natpmpDeviceAddress == null) natpmpDeviceAddress = proposeGateway();
        if (natpmpDeviceAddress == null) return false;

        int portToMap = Identity.getPeerUDPPort();
        if (portToMap > 0) {
	        if(client.natpmpMapWrapper( natpmpDeviceAddress
	                ,net.ddp2p.common.network.natpmp.Request.NATPMP_OPCODE_UDP
	                ,net.ddp2p.common.network.natpmp.Request.NATPMP_CLIENT_PORT
	                ,portToMap
	                ,SLEEP_SEC*MAP_MULT
	                ,mapping) < 0 ) return false;
        }
        
        this.m_crt_external_port_UDP = mapping.getExternalPort();
        portToMap = Identity.getPeerTCPPort();
        if (portToMap > 0) {
        	if (client.natpmpMapWrapper( natpmpDeviceAddress
                ,net.ddp2p.common.network.natpmp.Request.NATPMP_OPCODE_TCP
                ,net.ddp2p.common.network.natpmp.Request.NATPMP_CLIENT_PORT
                ,portToMap
                ,SLEEP_SEC*MAP_MULT
                ,mapping) < 0) return false;
        }
        this.m_crt_external_port_TCP = mapping.getExternalPort();
        this.m_crt_external_IP = mapping.getExternalIPAddress();
        return true;
	}
	/**
	 * Only for IPv4
	 * 
	 * @return
	 */
	private String proposeGateway() {
		ArrayList<InetAddress> site = Util.getLocalIPs(true);
		for (InetAddress s : site) {
			if (s instanceof Inet6Address) continue;
			Inet4Address s4 = (Inet4Address)s;
			byte[] b = Util.getInetAddressBytes_from_HostName_IP(s4.getHostAddress());
			if (b[b.length-1] == 1) continue; // likely a virtual network (wmware, android)
			b[b.length-1] = 1;
			return Util.concat(b, ".", "");
		}
		return null;
	}
	public boolean try_pcp() {
        // General setup.
        Client_PCP client = new Client_PCP();
        AddressPortMapping mapping = new AddressPortMapping();
        
        String natpmpDeviceAddress = address_external; //"10.0.0.1";
        if (natpmpDeviceAddress == null) natpmpDeviceAddress = proposeGateway();
        if (natpmpDeviceAddress == null) return false;

        int portToMap = Identity.getPeerUDPPort();
        if (portToMap > 0) {
        	if (client.pcpMap( natpmpDeviceAddress
                ,OpCodeInfo.PROTOCOL_UDP
                ,Request.PCP_CLIENT_PORT
                ,portToMap
                ,new byte[4]
                ,SLEEP_SEC*MAP_MULT //delay seconds
                ,mapping ) < 0) return false;
        }
        this.m_crt_external_port_UDP = mapping.getExternalPort();
        
        portToMap = Identity.getPeerTCPPort();
        if (portToMap > 0) {
        	if (client.pcpMap( natpmpDeviceAddress
                ,OpCodeInfo.PROTOCOL_TCP
                ,Request.PCP_CLIENT_PORT
                ,portToMap
                ,new byte[4]
                ,SLEEP_SEC*MAP_MULT // delay seconds
                ,mapping ) < 0) return false;
        }
        this.m_crt_external_port_TCP = mapping.getExternalPort();
        this.m_crt_external_IP = mapping.getExternalIPAddress();
        return true;
	}

	public void turnOff() {
		stop = true;
	}
}


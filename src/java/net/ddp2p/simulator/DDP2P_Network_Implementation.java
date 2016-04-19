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
package net.ddp2p.simulator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import net.ddp2p.common.network.sim.DDP2P_Network;
import net.ddp2p.common.network.sim.DDP2P_NetworkInterface;
import net.ddp2p.common.network.sim.ServerSocket;
import net.ddp2p.common.network.sim.Socket;
class Remote_Peers {
	InetAddress ia;
	int port;
}
class QueuesPair {
	/** readers would wait on this queue for messages */
	ArrayList<Object> in_queue = new ArrayList<Object>();
	public ArrayList<Remote_Peers> peers = new ArrayList<Remote_Peers>();
}
class QueuesPort {
	Hashtable<Integer,  QueuesPair> queues_by_port = new Hashtable<Integer, QueuesPair>();
	public void clean(int port, Object serverSocket) {
		queues_by_port.remove(port);
	}
	public void createQueue(int port) {
		QueuesPair qp = queues_by_port.get(port);
		if (qp != null) return;
		queues_by_port.put(port, new QueuesPair());
	}
}
class Queues {
	Hashtable<Integer,  QueuesPort> queues_by_installation = new Hashtable<Integer, QueuesPort> ();
	QueuesPort get_or_create (int installation) {
		QueuesPort result = queues_by_installation.get(installation);
		if (result == null) queues_by_installation.put(installation, result = new QueuesPort());
		return result;
	}
	public ArrayList<Object> createQueue(int port, int installation) {
		QueuesPort qp = get_or_create(installation);
		qp.createQueue(port);
		return null;
	}
	public void clean(int port, int installation, Object serverSocket) {
		QueuesPort qp = queues_by_installation.get(installation);
		if (qp == null) return;
		qp.clean(port, serverSocket);
	}
}
/**
 * Have to start by setting Socket.singleton = new DDP2P_Network_Implementation(installations);
 * @author msilaghi
 *
 */
public class DDP2P_Network_Implementation implements DDP2P_Network {
	/** Maximum messages in a queue */
	public static final int MAX_QUEUE = 100;
	/** the IP address of each installation (installation : inet_address)*/
	public Hashtable<Integer, InetAddress> IP_addresses = new Hashtable<Integer, InetAddress>();
	public Hashtable<InetAddress, Integer> installation_addresses = new Hashtable<InetAddress, Integer>();
	/** All the ports busy for the current installation (removed on close, added on connect) */
	Hashtable<Integer, Hashtable<Integer, Object>> installation_ports = new Hashtable<Integer, Hashtable<Integer, Object>>();
	Queues queues = new Queues();
	/** number of installations */
	public int installations;
	/**
	 *  set the number of installations, and set this as singleton_network in Socket 
	 * @param installations
	 */
	public DDP2P_Network_Implementation(int installations) {
		this.installations = installations;
		Socket.singleton_network = this;
	}
	/**
	 * Call this before starting the simulation, after the constructor was called
	 * @param installation
	 * @param ip
	 */
	public void add_installation_address (Integer installation, InetAddress ip) {
		IP_addresses.put(installation, ip);
		installation_addresses.put(ip, installation);
	}
	/**
	 * Called in the constructor of a Server Socket to register it with this
	 * @param port
	 * @param serverSocket
	 * @param installation
	 */
	public void register(int port, ServerSocket serverSocket, int installation) {
		registerPort(port, installation, serverSocket);
		queues.createQueue(port, installation);
	}
	/**
	 * Do the actual registration of the port as busy for this installation.
	 * @param port
	 * @param installation
	 */
	private void registerPort(int port, int installation, Object socket) {
		Hashtable<Integer, Object> hm = installation_ports.get(installation);
		if (hm == null) installation_ports.put(installation, hm = new Hashtable<Integer, Object>());
		hm.put(new Integer(port), socket);
	}
	/**
	 * 
	 * Called at accept on a ServerSocket, to register a port on the IP of this installation, to be associated with the socket
	 * @param port
	 * @param result
	 * @param installation
	 * @return
	 */
	public int register(int port, Socket socket, int installation) {
		socket.port = port;
		registerPort(port, installation, socket);
		queues.createQueue(port, installation);
		return port;
	}
	/**
	 * Called at accept on a ServerSocket, to get a new unused port with a client
	 * @param installation
	 * @return
	 */
	public int getFreePort(int installation) {
		Hashtable<Integer, Object> hm = installation_ports.get(installation);
		if (hm == null) installation_ports.put(installation, hm = new Hashtable<Integer, Object>());
		for (int i = 1 ; i > 0;  i ++) {
			if (hm.containsKey(i)) continue;
			hm.put(i, new Object());
			return i;
		}
		return 0;
	}
	/**
	 * Return an interface with the list of addresses planned for this instance.
	 * That list has to be pre-loaded in the structure "IP_addresses".
	 * @param installation 
	 * @return
	 */
	public DDP2P_NetworkInterface get_NetworkInterface(int installation) {
		ArrayList<InetAddress> adr = new ArrayList<InetAddress>();
		try {
			InetAddress ia = IP_addresses.get(new Integer(installation));
			if (ia != null) adr.add(ia);
			else adr.add(InetAddress.getByName("10.0.0." + (installation + 1)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		DDP2P_NetworkInterface result = new DDP2P_NetworkInterface(Collections.enumeration(adr));
		return result;
	}
	public void close(ServerSocket serverSocket, int installation) {
		Integer port = serverSocket.port;
		if (port == null) return;
		queues.clean(port.intValue(), installation, serverSocket);
		Hashtable<Integer, Object> hm = installation_ports.get(installation);
		if (hm == null) installation_ports.put(installation, hm = new Hashtable<Integer, Object>());
		hm.remove(port);
	}
	public void close(Socket socket, int installation) {
		Integer port = socket.port; 
		if (port == null) return;
		queues.clean(port.intValue(), installation, socket);
		Hashtable<Integer, Object> hm = installation_ports.get(installation);
		if (hm == null) installation_ports.put(installation, hm = new Hashtable<Integer, Object>());
		hm.remove(port);
	}
	public QueuesPair getQueuesPair(int installation, int port) {
		QueuesPort q = queues.get_or_create(installation);
		QueuesPair qp = q.queues_by_port.get(port);
		return qp;
	}
	public Socket accept(ServerSocket serverSocket, int installation, int timeout) {
		Integer port = serverSocket.port;
		QueuesPair qp = getQueuesPair(installation, port);
		synchronized (qp.in_queue) {
			if (qp.peers.size() == 0) { 
				try {
					if (timeout > 0) qp.in_queue.wait(timeout);
					else qp.in_queue.wait();
				} catch (Exception e) {return null;}
			}
		}
		Socket result = new Socket();
		result.remote_address = qp.peers.get(0).ia;
		result.remote_port = qp.peers.get(0).port;
		return result;
	}
	@Override
	public void connect(InetAddress address, int port) {
		int installation = this.installation_addresses.get(address);
		QueuesPair qp = getQueuesPair(installation, port);
		synchronized (qp.in_queue) {
			Remote_Peers e = new Remote_Peers();
			e.ia = address;
			e.port = port;
			qp.peers.add(e);
			qp.in_queue.notify();
		}
	}
	public byte[] read(int installation, int port, Socket socket, int timeout) {
		QueuesPair qp = getQueuesPair(installation, port);
		synchronized (qp.in_queue) {
			if (qp.in_queue.size() == 0) { 
				try {
					if (timeout > 0) qp.in_queue.wait(timeout);
					else qp.in_queue.wait();
				} catch (Exception e) {return null;}
			}
			if (qp.in_queue.size() == 0) return null;
			return (byte[])qp.in_queue.remove(0);
		}
	}
	/**
	 * Here port should be socket.remote_port, and installation should be the remote installation.
	 */
	public void write(int installation, int port, Socket socket, byte[] data) {
		QueuesPair qp = getQueuesPair(installation, port);
		synchronized (qp.in_queue) {
			qp.in_queue.add(data);
			qp.in_queue.notifyAll();
		}
	}
	public void write(Socket socket, byte[] data) {
		int installation = this.installation_addresses.get(socket.remote_address);
		QueuesPair qp = getQueuesPair(installation, socket.remote_port);
		synchronized (qp.in_queue) {
			while(qp.in_queue.size() > MAX_QUEUE)
				try {
					qp.in_queue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			qp.in_queue.add(data);
			qp.in_queue.notifyAll();
		}
	}
}

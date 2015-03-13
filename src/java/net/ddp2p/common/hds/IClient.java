package net.ddp2p.common.hds;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public interface IClient {

	void turnOff();

	void start();

	//Object get_wait_lock();

	void wakeUp();

	/**
	 * Called after the socket addresses are known
	 * @param tcp_sock_addresses
	 * @param udp_sock_addresses
	 * @param old_address : address of directory, used for messages in debug window
	 * @param s_address
	 * @param type
	 * @param global_peer_ID
	 * @param peer_name
	 * @param peer_directories_udp_sockets
	 * @param peer_directories
	 * @param peer_ID
	 * @return
	 */
	boolean try_connect(
			ArrayList<Address_SocketResolved_TCP> tcp_sock_addresses,
			ArrayList<Address_SocketResolved_TCP> udp_sock_addresses,
			String old_address, // a DIR address or some other address
			String s_address,  // an address or a list of addresses (from a DIR) separated by ","
			String type,       // type of old_address
			String global_peer_ID,
			String peer_name,
			ArrayList<InetSocketAddress> peer_directories_udp_sockets,
			ArrayList<String> peer_directories);

}

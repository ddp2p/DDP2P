package net.ddp2p.common.config;

import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.IClient;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.network.NATServer;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.wireless.BroadcastClient;
import net.ddp2p.common.wireless.BroadcastServer;

public 
class DDP2P_Peer_Instalation {

	private DBInterface db;
	private DBInterface db_dir;

	public  DirectoryServer g_DirectoryServer;
	public  Server g_TCPServer;
	public  UDPServer g_UDPServer; //have to unify with the one in DD
	public  NATServer g_NATServer;
	public  BroadcastServer g_BroadcastServer = null; // reference to the unique BroadcastServer
	public  BroadcastClient g_BroadcastClient = null; // reference to the unique BroadcastClient
	public  IClient g_PollingStreamingClient;
	
	public DBInterface getDB() {
		return db;
	}
	public void setDB(DBInterface db) {
		this.db = db;
	}
	public DBInterface getDB_Dir() {
		return db_dir;
	}
	public void setDB_Dir(DBInterface db_dir) {
		this.db_dir = db_dir;
	}
}
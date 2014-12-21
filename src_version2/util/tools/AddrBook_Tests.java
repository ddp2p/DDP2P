package util.tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

import hds.Address;
import hds.Connections;
import hds.Connections.DirectoryRequestAnswer;
import hds.DirectoryAnnouncement;
import hds.DirectoryAnnouncement_Address;
import hds.DirectoryAnswerMultipleIdentities;
import hds.DirectoryRequest;
import hds.DirectoryServer;
import hds.DirectoryServerUDP;
import hds.Server;
import hds.UDPServer;
import util.DBInterface;
import util.GetOpt;
import util.P2PDDSQLException;
import config.Application;
import config.DD;
import config.Identity;
import data.D_Peer;

public class AddrBook_Tests {
	private static boolean DEBUG = false;
	private static boolean udp = false;
	private static boolean announcement = false;
	private static boolean request = false;
	private static boolean ping = false;
	private static int DS_PORT = 10000;
	private static String DS_DOMAIN = "debatedecide.org";

	public static void main(String[] args) {
		if (DEBUG) {
			System.out.println("AddrBook: len="+args.length);
			for (int k = 0; k < args.length; k ++) {
				System.out.println("AddrBook: len="+k+" "+args[k]);		
			}
		}
		try {
			util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			
			char c;
			opts:
			while ((c = GetOpt.getopt(args, "P:D:d:huvarp")) != GetOpt.END) {
				System.out.println("Options received"+GetOpt.optopt +" optind="+GetOpt.optind+" arg="+GetOpt.optarg);
				switch (c) {
				case 'h':
					System.out.println("java -cp jars/sqlite4java.jar:jars/sqlite-jdbc-3.7.2.jar:jars/DD.jar util.tools.AddrBook "
							+ "\n\t -h          Help"
							+ "\n\t -v          Verbose"
							+ "\n\t -u          udp"
							+ "\n\t -d file     Use file as current database (dir database in the same folder)"
							+ "\n\t -P port     port"
							+ "\n\t -D IP      domain"
							+ "\n\t -a         Send announcement for myself"
							+ "\n\t -p         Send pingempty for myself"
							+ "\n\t -r         Request myself"
							);
					//printHELP();
					//break;
					System.exit(-1);//return;
				case 'd':
					if (DEBUG) System.out.println("Option d: "+GetOpt.optarg);
					Application.DIRECTORY_FILE = GetOpt.optarg;
					break;
				case 'v':
					DEBUG = true;
					Server.DEBUG = true;
					UDPServer.DEBUG = true;
					Connections.DEBUG = true;
					DirectoryAnnouncement.DEBUG = true;
					DirectoryAnnouncement_Address.DEBUG = true;
					DirectoryServer.DEBUG = true;
					DirectoryServerUDP.DEBUG = true;
					DirectoryRequest.DEBUG = true;
					if (DEBUG) System.out.println("Option v: "+GetOpt.optarg);
					break;
				case 'P':
					if (DEBUG) System.out.println("Option P: "+GetOpt.optarg);
					try {
						DS_PORT = Integer.parseInt(GetOpt.optarg);
					} catch(Exception e) {e.printStackTrace();}
					break;
				case 'D':
					if (DEBUG) System.out.println("Option D: "+GetOpt.optarg);
					try {
						DS_DOMAIN = GetOpt.optarg;
					} catch(Exception e) {e.printStackTrace();}
					break;
				case 'u':
					udp = true;
					if (DEBUG) System.out.println("Option u: "+GetOpt.optarg);
					break;
				case 'a':
					announcement = true;
					if (DEBUG) System.out.println("Option a: "+GetOpt.optarg);
					break;
				case 'r':
					request = true;
					if (DEBUG) System.out.println("Option r: "+GetOpt.optarg);
					break;
				case 'p':
					ping = true;
					if (DEBUG) System.out.println("Option p: "+GetOpt.optarg);
					break;
				case GetOpt.END:
					if (DEBUG) System.out.println("REACHED END OF OPTIONS");
					break;
				case '?':
					System.out.println("Options ?: optopt="+GetOpt.optopt +" optind="+GetOpt.optind+" "+GetOpt.optarg);
					System.out.println("AddrBook_Test: run: exit unknown");
					return;
				case ':':
					System.out.println("Options \":\" for "+GetOpt.optopt);
					break;
				default:
					System.out.println("AddrBook: unknown option error: \""+c+"\"");
					break opts;
					//return;
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("AddrBook_Test: run: options done");

		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}

		boolean quit_on_failure = false;
		boolean set_peer_myself = true;
		boolean announce_dirs = false;
		Identity.init_Identity(quit_on_failure, set_peer_myself = true, announce_dirs  = false);
		//Identity.initMyCurrentPeerIdentity_fromDB(quit_on_failure);
		System.out.println("AddrBook_Test: run: identity");
		
		if (announcement) {
			System.out.println("AddrBook_Test: run: announcement start detect domains");
			try {
				Server.detectDomain();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			System.out.println("AddrBook_Test: run: wait local addresses");
			Object obj = new Object();
			while(Identity.countLocalAddresses() <= 0) {
				synchronized (obj) {
					try {
						obj.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("AddrBook_Test: run: got local addresses");
			DirectoryAnnouncement a = UDPServer.prepareDirectoryAnnouncement();
			a.sign();
			System.out.println("AddrBook_Test: run: announcement ="+a);
			try {
				if (udp) sendUDPAnnouncement(a);
				else
					sendTCPAnnouncement(a);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (request) {
			
			System.out.println("AddrBook_Test: run: request");
			try {
				if (udp) sendUDPRequest();
				else
					sendTCPrequest();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void sendTCPrequest() {
		System.out.println("AddrBook_Test: sendTCPrequest: tcp request");
		Address adr = new Address();
		adr.setDomain(DS_DOMAIN);
		adr.setPorts(DS_PORT, DS_PORT);
		adr.inetSockAddr = new InetSocketAddress(DS_DOMAIN, DS_PORT);
		adr.branch = DD.BRANCH;
		adr.agent_version = DD.VERSION;

		D_Peer myself = data.HandlingMyself_Peer.get_myself_or_null();
		if (myself == null) {
			System.out.println("AddrBook_Test: sendTCPrequest: no myself");
			return;
		}
		System.out.println("AddrBook_Test: sendTCPrequest: got myself");
		try {
			DirectoryRequestAnswer dra = Connections.requestDirectoryAnswer(myself.getGID(), myself.getLIDstr(), adr.inetSockAddr, adr);
			System.out.println("AddrBook_Test: sendTCPrequest: result ="+dra.da);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendUDPRequest() throws IOException, ASN1DecoderFail, P2PDDSQLException {
		System.out.println("AddrBook_Test: sendUDPRequest: udp request");
		Address adr = new Address();
		adr.setDomain(DS_DOMAIN);
		adr.setPorts(DS_PORT, DS_PORT);
		adr.inetSockAddr = new InetSocketAddress(DS_DOMAIN, DS_PORT);
		adr.branch = DD.BRANCH;
		adr.agent_version = DD.VERSION;

		Application.aus = new UDPServer(Identity.getCurrentPeerIdentity_NoQuitOnFailure());

		D_Peer myself = data.HandlingMyself_Peer.get_myself_or_null();
		if (myself == null) {
			System.out.println("AddrBook_Test: sendUDPRequest: no myself");
			return;
		}

		DirectoryRequest dr = Connections.getDirAddressUDP(adr.inetSockAddr, adr, myself.getGID(), myself.getName(), myself.getLIDstr());
		System.out.println("AddrBook_Test: sendUDPRequest: sent request = " + dr);
		
		byte[] _buffer = new byte[UDPServer.UDP_BUFFER_LENGTH];
		DatagramPacket pak = new DatagramPacket(_buffer, UDPServer.UDP_BUFFER_LENGTH);
		// calling the DatagramPacket receive call
		UDPServer.getUDPSocket().setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER); // might have changed
		System.out.println("AddrBook_Test: sendUDPRequest: waiting answer");
		UDPServer.getUDPSocket().receive(pak);
		System.out.println("AddrBook_Test: sendUDPRequest: waiting answer done");

		byte[] msg = null;
		msg = pak.getData();
		//msg_len = msg.length;
		Decoder dec = new Decoder(msg, pak.getOffset(), pak.getLength());
		
		DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities(dec);
		
		System.out.println("AddrBook_Test: sendUDPRequest: answer =" + dami);
	}

	private static void sendTCPAnnouncement(DirectoryAnnouncement a) throws UnknownHostException, IOException {
		System.out.println("AddrBook_Test: sendTCPAnnouncement: tcp announcement");
		Address adr = new Address();
		adr.setDomain(DS_DOMAIN);
		adr.setPorts(DS_PORT, DS_PORT);
		adr.inetSockAddr = new InetSocketAddress(DS_DOMAIN, DS_PORT);
		adr.branch = DD.BRANCH;
		adr.agent_version = DD.VERSION;
		boolean result;
		result = Server.announceMyselfToDirectory(a, adr, false);
		System.out.println("AddrBook_Test: run: result ="+result);

//		Socket s = new Socket(DS_DOMAIN, DS_PORT);
//		s.getOutputStream().write(a.encode());
//		byte[] b = new byte[1000];
//		s.getInputStream().read(b);
	}

	private static void sendUDPAnnouncement(DirectoryAnnouncement a) throws P2PDDSQLException {
		Address adr = new Address();
		adr.setDomain(DS_DOMAIN);
		adr.setPorts(DS_PORT, DS_PORT);
		adr.inetSockAddr = new InetSocketAddress(DS_DOMAIN, DS_PORT);
		adr.branch = DD.BRANCH;
		adr.agent_version = DD.VERSION;
		boolean result;
		Application.aus = new UDPServer(Identity.getCurrentPeerIdentity_NoQuitOnFailure());
		result = UDPServer._announceMyselfToDirectories(a, UDPServer.getUDPSocket(), adr);		
		System.out.println("AddrBook_Test: run: udp result ="+result);
	}
}
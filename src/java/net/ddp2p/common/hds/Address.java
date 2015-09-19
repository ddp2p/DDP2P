/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
/*
 * Describes an address as sent between a directory and a client
 */
package net.ddp2p.common.hds;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.data.TypedAddressComparator;
import net.ddp2p.common.util.DirectoryAddress;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

/**
 * The version util.DirectoryADdress has an extra Name, but a single port and no extra preferences,
 * to be used only for messages about directories 
 * @author msilaghi
 *
 */
public class Address extends ASNObj {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	//DirectoryServer.ADDR_SEP=',';
	/**
	 * Used in sending addresses over the network
	 */
	public static final String ADDR_PROT_NET_SEP = "^";
	/**
	 * Used for internal storage in database
	 */
	public static final String PRI_SEP = "/"; // http://domain:tcp:udp/priority
	public static final String ADDR_PROT_INTERN_SEP = "://";
	public static final String ADDR_PART_SEP = ":";
	public static final String SOCKET = "Socket";
	public static final String DIR = "DIR";
	public static final String NAT = "NAT";
	public static final int V0 = 0;
	public static final int V1 = 1;
	public static final int V2 = 2;
	public static final int V3 = 3;
	public static final String PROT_SEP = "%"; 
	public String domain;
	public String pure_protocol;
	public int tcp_port = -1;
	public int udp_port = -1;
	public String name;
	public String branch;
	public String agent_version;
	public int version_structure = V2;

	public long _instance_ID;
	public String instance_ID_str;
	public String instance;
	public boolean certified;
	public int priority;
	public String last_contact;
	public String arrival_date;
	public String peer_address_ID;
	@Deprecated
	public String address;

	/**
	 * Local variable, used for directories
	 */
	public InetSocketAddress inetSockAddr;
	public boolean active = true;
	public boolean dirty = false; 
	
	/**
	 * "active" is not added in the output, even if it is parsed
	 * if active is false, then the whole data is added (including empty name)
	 */
	public String toLongNiceString() {
		return "[\tVersion ="+version_structure+"," +
				"\ttype    ="+pure_protocol+"," +
				"\tbranch  ="+branch+"," +
				"\tagent   ="+agent_version+"," +
				"\tdomain  ="+domain+","+
				"\ttcp     ="+tcp_port+"," +
				"\tudp     ="+udp_port+"," +
				//"\tname    ="+name+"," +
				"\tcertify ="+certified+"," +
				//"\tpriority="+priority+"?," +
				//"\tactive="+active+"," +
				"]";
	}
	public String toLongString() {
		return "[V="+version_structure+",t="+pure_protocol+",b="+branch+",v="+agent_version+
				",tcp="+tcp_port+",u="+udp_port+",n="+name+",c="+certified+
				",p="+priority+",a="+active+",d="+domain+"]";
	}
	public String toString() {
		return toStringStorage();
	}
	public String toStringStorage() {
		switch (version_structure) {
		case V1:
			return toString_V1();
		case V2:
		default:
			return toString_V2();
		}
	}
	public String toString_V1() {
		if (DEBUG) System.out.println("Address:toString: V1");
		if (domain==null) return address;
		String result = "";
		result += ((pure_protocol!=null)?(pure_protocol+ADDR_PROT_INTERN_SEP):"");
		result += domain + ADDR_PART_SEP+tcp_port;
		if ((udp_port != tcp_port) || (name != null) || !active) {
			if (udp_port <= 0)
				result += ADDR_PART_SEP + tcp_port;
			else
				result += ADDR_PART_SEP + udp_port;
			if ((name != null) || (!active))
				result += ADDR_PART_SEP + Util.getString(name, "");
		}
		return result;
	}
	public String toString_V2() {
		if (DEBUG) System.out.println("Address:toString: V2");
		if (domain==null) return address;
		String result = "";
		String prot = getProtocolVersion();
		result += ((prot!=null)?(prot + ADDR_PROT_INTERN_SEP):"");
		
		result += domain + ADDR_PART_SEP + tcp_port;
		if ((udp_port != tcp_port) || (name != null) || !active) {
			if (udp_port <= 0)
				result += ADDR_PART_SEP + tcp_port;
			else
				result += ADDR_PART_SEP + udp_port;
			if ((name != null)||(!active))
				result += ADDR_PART_SEP + Util.getString(name, "");
		}
		return result;
	}
	/**
	 * Separated with % (PROT_SEP)
	 * @return
	 */
	public String getProtocolVersion() { 
		if ((pure_protocol == null) && (branch == null) && (agent_version==null))
			return null;
		if ((branch==null) && (agent_version == null)) return getPureProtocol();
		if (agent_version == null) return getPureProtocol()+PROT_SEP+getBranch();
		return getPureProtocol()+PROT_SEP+getBranch()+PROT_SEP+getAgentVersion();
	}
	public String getAgentVersion() {
		if (agent_version == null) return "V_";
		return agent_version;
	}
	public String getBranch() {
		if (branch == null) return "B_";
		return branch;
	}
	public String getPureProtocol() {
		if (pure_protocol == null) return Address.SOCKET;
		return pure_protocol;
	}
	/**
	 * Like toString(), but does not include eventual name in result.
	 * Also a udp_port of -1 is considered equal with the tcp_port
	 * Used to extract an address from a directory description in application
	 * @return
	 */
	public String getStringAddress() {
		String result = "";
		result += ((pure_protocol!=null)?(pure_protocol+ADDR_PROT_INTERN_SEP):"");
		result += domain + ADDR_PART_SEP+tcp_port;
		if ((udp_port != tcp_port) && (udp_port > 0)) {
			result += ADDR_PART_SEP + udp_port;
		}
		return result;
	}
	public Address instance() {return new Address();}
	public Address(){} // For arrays
	/**
	 * Expects "[<pure[%branch[%version]]>://]<domain>:<tcp>:<udp>:<name>[:<active_int_boolean>:...]"
	 * Separated by PROT_SEP=%, PRI_SEP=/, ADDR_PROT_INTERN_SEP=://, ADDR_PART_SEP=:
	 * Extracts "active" (meant for my own's peer directories)
	 * otherwise:
	 *  calls _setAddress(), and 
	 *  sets "this.address" to received parameter
	 *  ==============================================
	 * _setAddress
	 * Only fields that can be clearly identified in the string address
	 * [<PROT>://]domain:...:...:<name>[:<active_int_boolean>:...]
	 * pure_protocol (if original null)
	 * branch (if original null)
	 * agent_version (if original null)
	 * domain (if original null)
	 * tcp, udp
	 * name (if original null)
	 * ------
	 * Leaves version_structure to default (V2)
	 * @param domain_port
	 */
	public Address(String domain_port){
		setAddress(domain_port);
	}
	/**
	 * Expects "[<pure[%branch[%version]]>://]<domain>:<tcp>:<udp>:<name>[:<active_int_boolean>:...]"
	 * Separated by PROT_SEP=%, PRI_SEP=/, ADDR_PROT_INTERN_SEP=://, ADDR_PART_SEP=:
	 * Only fields that can be clearly identified in the string address
	 * [<PROT>://]domain:...:...:<name>[:<active_int_boolean>:...]
	 * pure_protocol (if original null)
	 * branch (if original null)
	 * agent_version (if original null)
	 * domain (if original null)
	 * tcp, udp
	 * name (if original null)
	 * 
	 * Does not parse "active"
	 * @param domain_port
	 */
	public void _setAddress(String domain_port) {
		if (DEBUG) System.out.println("Address:_setAddress: enter "+domain_port);
		//if (DEBUG) Util.printCallPath("Where?");
		if (domain_port == null) return;
		String _protocol = getProtocol(domain_port);
		if (DEBUG) System.out.println("Address:_setAddress: protocol "+_protocol);
		String _pure_protocol = parsePureProtocolFromProtocol(_protocol);
		if ((_pure_protocol != null) && (pure_protocol == null)) pure_protocol = _pure_protocol;
		if (DEBUG) System.out.println("Address:_setAddress: pure_protocol "+_pure_protocol+" -> "+pure_protocol);
		String _branch = parseBranchFromProtocol(_protocol);
		if ((_branch != null) && (branch == null)) branch = _branch;
		if (DEBUG) System.out.println("Address:_setAddress: branch "+branch+" -> "+branch);
		String _agent_version = parseAgentVersionFromProtocol(_protocol);
		if ((_agent_version != null) && (agent_version == null)) agent_version = _agent_version;
		if (DEBUG) System.out.println("Address:_setAddress: _agent_version "+_agent_version+" -> "+agent_version);
		String _domain = getDomain(domain_port);
		if ((_domain != null) && (domain == null)) domain = _domain;
		if (DEBUG) System.out.println("Address:_setAddress: domain "+_domain+" -> "+domain);
		tcp_port = getTCP(domain_port);
		udp_port = getUDP(domain_port);
		String _name = getName(domain_port);
		if ((_name != null) && (name == null)) name = _name;
	}
	/**
	 * Expects "[<pure[%branch[%version]]>://]<domain>:<tcp>:<udp>:<name>[:<active_int_boolean>:...]"
	 * Separated by PROT_SEP=%, PRI_SEP=/, ADDR_PROT_INTERN_SEP=://, ADDR_PART_SEP=:
	 * Extracts "active" (meant for my own's peer directories)
	 * otherwise:
	 *  calls _setAddress(), and 
	 *  sets "this.address" to received parameter
	 *  ==============================================
	 * _setAddress
	 * Only fields that can be clearly identified in the string address
	 * [<PROT>://]domain:...:...:<name>[:<active_int_boolean>:...]
	 * pure_protocol (if original null)
	 * branch (if original null)
	 * agent_version (if original null)
	 * domain (if original null)
	 * tcp, udp
	 * name (if original null)
	 * @param domain_port
	 */
	public void setAddress(String domain_port) {
		if (DEBUG) System.out.println("Address:setAddress: enter(active) "+domain_port);
		if (domain_port == null) return;
		this.address = domain_port;
		_setAddress(domain_port);
		active = getActive(domain_port);
		/*
		String dp[] = domain_port.split(":");
		if(dp.length<2) return;
		domain = dp[0];
		String[]proto = domain.split(Pattern.quote(ADDR_PROT_SEP));
		if(proto.length>1){
			protocol = proto[0];
			domain = proto[1];
		}else{
			domain = proto[0];
		}
		tcp_port = Integer.parseInt(dp[1]);
		if(dp.length>2)
			udp_port = Integer.parseInt(dp[2]);
		else udp_port = tcp_port;
		*/
	}
	private String parseAgentVersionFromProtocol(String _protocol) {
		if(_protocol == null) return null;
		String prots[] = _protocol.split(Pattern.quote(PROT_SEP));
		if (prots.length <= 2) return null;
		return prots[2]; 
	}
	private String parseBranchFromProtocol(String _protocol) {
		if(_protocol == null) return null;
		String prots[] = _protocol.split(Pattern.quote(PROT_SEP));
		if (prots.length <= 1) return null;
		return prots[1]; 
	}
	private String parsePureProtocolFromProtocol(String _protocol) {
		if(_protocol == null) return null;
		String prots[] = _protocol.split(Pattern.quote(PROT_SEP));
		if (prots.length <= 0) return null;
		return prots[0]; 
	}
	public Address(String _domain, int _tcp_port, int _udp_port){
		if(_domain == null) return;
		domain = _domain; tcp_port = _tcp_port; udp_port = _udp_port;
	}
	/**
	 * No longer used (only Client1)
	 * No longer knowing if passed parameter is pure protocol or with version
	 * @param _adr host:tcp:udp
	 * @param _protocol type
	 */
	@Deprecated
	Address(String _adr, String _pure_protocol){
		if(_adr == null) return;
		pure_protocol = _pure_protocol;
		domain = Address.getDomain(_adr); tcp_port = Address.getTCP(_adr); udp_port = Address.getUDP(_adr);
	}
	public Address(ArrayList<Object> _address, D_Peer peer) {
		init(_address, peer);
		if ((domain == null) && (address != null)) {
			this._setAddress(address);
			this.dirty = true;
		}
	}
	public Address(DirectoryAddress d) {
		this.pure_protocol = d.pure_protocol;
		this.branch = d.branch;
		this.agent_version = d.agent_version;
		this.domain = d.domain;
		this.tcp_port = d.tcp_port;
		this.udp_port = d.udp_port;
		this.name = d.name;
		this.active = d.active;
		this.instance = d.instance;
		this.version_structure = Util.ival(d.version,0);
	}
	public Address(Address d) {
		this.pure_protocol = d.pure_protocol;
		this.branch = d.branch;
		this.agent_version = d.agent_version;
		this.domain = d.domain;
		this.tcp_port = d.tcp_port;
		this.udp_port = d.udp_port;
		this.name = d.name;
		this.active = d.active;
		this.dirty = d.dirty;
		this.instance = d.instance;
		this.instance_ID_str = d.instance_ID_str;
		this._instance_ID = d._instance_ID;
		this.certified = d.certified;
		this.priority = d.priority;
		this.last_contact = d.last_contact;
		this.arrival_date = d.arrival_date;
		this.peer_address_ID = d.peer_address_ID;
		this.address = d.address;
		this.version_structure = d.version_structure;
	}
	// Guessing an address from a socket address
	public Address(SocketAddress psa, D_PeerInstance dpi) {
		this.pure_protocol = Address.SOCKET;
		this.domain = Util.get_IP_from_SocketAddress(psa); // psa.getHostString();
		this.tcp_port = ((InetSocketAddress)psa).getPort();
		this.udp_port = this.tcp_port;
		if (dpi != null) {
			this.branch = dpi.branch;
			this.agent_version = dpi.agent_version;
			this.instance = dpi.peer_instance;
			this.last_contact = Util.getGeneralizedTime();
		}
	}
	private void init(ArrayList<Object> in, D_Peer peer) {
		address = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_ADDRESS));
		pure_protocol = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_TYPE));
		branch = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_BRANCH));
		agent_version = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_AGENT_VERSION));
		instance_ID_str = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_INSTANCE_ID));
		_instance_ID = Util.lval(instance_ID_str, -1);
		if (_instance_ID > 0) {
			D_PeerInstance dpi = null;
			if (peer != null){
				dpi = peer.getPeerInstance_ByID(instance_ID_str);
				//this._peer_ID = peer.get_ID();
			}
			if (dpi != null) {
				instance = dpi.peer_instance;
				if (! this.isDIR()) {
					this.branch = dpi.branch;
					this.agent_version = dpi.agent_version;
				}
			} else {
				try {
					System.out.println("Address:init: should not come here!");
					ArrayList<ArrayList<Object>> inst = Application.getDB().select(
							"SELECT "+net.ddp2p.common.table.peer_instance.peer_instance+
							" FROM "+net.ddp2p.common.table.peer_instance.TNAME+
							" WHERE "+net.ddp2p.common.table.peer_instance.peer_instance_ID+"=?;",
							new String[] {instance_ID_str}, _DEBUG);
					if (inst.size() > 0)
						instance = Util.getString(inst.get(0));
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}
		domain = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_DOMAIN));
		tcp_port = Util.ival(in.get(net.ddp2p.common.table.peer_address.PA_TCP_PORT), -1);
		udp_port = Util.ival(in.get(net.ddp2p.common.table.peer_address.PA_UDP_PORT), -1);
		certified = Util.stringInt2bool(in.get(net.ddp2p.common.table.peer_address.PA_CERTIFIED), false);
		priority = Util.ival(in.get(net.ddp2p.common.table.peer_address.PA_PRIORITY), 0);
		last_contact = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_CONTACT));
		arrival_date = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_ARRIVAL));
		peer_address_ID = Util.getString(in.get(net.ddp2p.common.table.peer_address.PA_PEER_ADDR_ID));
	}
	public boolean isDIR() {
		return DIR.equals(this.pure_protocol);
	}
	public void store(String peer_ID, String instance_ID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("Address: store: "+peer_ID+" inst="+instance_ID);
		String params[];
		assert(Util.equalStrings_null_or_not(instance_ID, this.instance_ID_str));
		if (instance_ID_str == null) setInstanceID(instance_ID);
		if (peer_address_ID == null)
			params = new String[net.ddp2p.common.table.peer_address.FIELDS_NOID];
		else
			params = new String[net.ddp2p.common.table.peer_address.FIELDS];
		params[net.ddp2p.common.table.peer_address.PA_INSTANCE_ID] = instance_ID_str;
		params[net.ddp2p.common.table.peer_address.PA_PEER_ID] = peer_ID;
		params[net.ddp2p.common.table.peer_address.PA_TYPE] = pure_protocol;
		params[net.ddp2p.common.table.peer_address.PA_DOMAIN] = domain;
		if (this.isDIR() || this.instance_ID_str == null) {
			params[net.ddp2p.common.table.peer_address.PA_BRANCH] = branch;
			params[net.ddp2p.common.table.peer_address.PA_AGENT_VERSION] = agent_version;
		}
		params[net.ddp2p.common.table.peer_address.PA_CERTIFIED] = Util.bool2StringInt(certified);
		params[net.ddp2p.common.table.peer_address.PA_PRIORITY] = ""+priority;
		params[net.ddp2p.common.table.peer_address.PA_CONTACT] = last_contact;
		params[net.ddp2p.common.table.peer_address.PA_ARRIVAL] = arrival_date;
		params[net.ddp2p.common.table.peer_address.PA_TCP_PORT] = ""+this.tcp_port;
		params[net.ddp2p.common.table.peer_address.PA_UDP_PORT] = ""+this.udp_port;
		params[net.ddp2p.common.table.peer_address.PA_ADDRESS] = address;
		if (peer_address_ID == null) {
			peer_address_ID = ""+Application.getDB().insert(net.ddp2p.common.table.peer_address.TNAME, 
					net.ddp2p.common.table.peer_address.fields_noID_list, params, DEBUG);
		} else {
			params[net.ddp2p.common.table.peer_address.PA_PEER_ADDR_ID] = peer_address_ID;
			Application.getDB().update(net.ddp2p.common.table.peer_address.TNAME, 
					net.ddp2p.common.table.peer_address.fields_noID_list, 
					new String[]{net.ddp2p.common.table.peer_address.peer_address_ID},
					params, DEBUG);
		}		
	}
	public void setInstanceID(String instance_ID) {
		this.instance_ID_str = instance_ID;
		this._instance_ID = Util.lval(instance_ID, -1);
	}
	public boolean equals(Object _a){
		final boolean DBG = false;
		if(DBG) System.out.print("Address:equals: "+this+" vs "+_a+" :");
		if((!(_a instanceof Address)) || (_a == null)){
			if(DBG) System.out.println("1->"+_a);
			return false;
		}
		Address a=(Address)_a;
		if(!Util.equalStrings_null_or_not(a.domain, this.domain)){
			if(DBG) System.out.println("3->"+a.domain+" vs "+this.domain);
			return false;
		}
		/*
		if((a.domain==null)||(this.domain==null))
			if(! ((a.domain==null)&&(this.domain==null))){
				if(DBG) System.out.println("2->"+a.domain+" or "+this.domain);
				return false;
			}
		if((a.domain!=null) && !a.domain.equals(this.domain)){
			if(DBG) System.out.println("3->");
			return false;
		}
		*/
		if(a.tcp_port!=this.tcp_port){
			if(DBG) System.out.println("4->"+a.tcp_port+" vs "+this.tcp_port);
			return false;
		}
		if(a.udp_port!=this.udp_port){
			if(DBG) System.out.println("5->"+a.udp_port+" vs "+this.udp_port);
			return false;
		}
		if (!Util.equalStrings_null_or_not(
				this.getProtocolVersion(),
				a.getProtocolVersion())) {
			if(DBG) System.out.println("6->"+a.getProtocolVersion()+" vs "+this.getProtocolVersion());
			return false;
		}
		/*
		if(DBG) if((a.pure_protocol!=null)||(this.pure_protocol!=null)) Util.printCallPath("adr?");
		if((a.pure_protocol==null)&&(this.pure_protocol!=null))
			if(!Address.SOCKET.equals(this.pure_protocol)){
				if(DBG) System.out.println("6a->"+a.pure_protocol+" or "+this.pure_protocol);
				if(DBG) Util.printCallPath("adr?");
				return false;
			}
		if((a.pure_protocol!=null)&&(this.pure_protocol==null))
			if(!Address.SOCKET.equals(a.pure_protocol)){
				if(DBG) System.out.println("6b->"+a.pure_protocol+" or "+this.pure_protocol);
				if(DBG) Util.printCallPath("adr?");
				return false;
			}
			*/
		/*
		if((a.protocol==null)||(this.protocol==null))
			if(! ((a.protocol==null)&&(this.protocol==null))){
				System.out.println("6->"+a.protocol+" or "+this.protocol);
				return false;
			}
		*/
		/*
		if((a.pure_protocol!=null) && (this.pure_protocol!=null) && !a.pure_protocol.equals(this.pure_protocol)){
			if(DBG) System.out.println("7->");
			return false;
		}
		*/
		//if(DBG) System.out.println("=");
		return true;
	}
	/**
	 * For version V0 must be TAG_SEQUENCE
	 * @return
	 */
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
	public boolean checkVersionV0() {
		String port = ""+tcp_port;
		//if (tcp_port <= 0) port = "" + udp_port;
		//else port = "" + tcp_port;
		String __address = domain+":"+port;
		String ___address = pure_protocol+"://"+domain+":"+port;
		if ((address != null) && !Util.equalStrings_null_or_not(__address, address)
				&&!Util.equalStrings_null_or_not(__address+":"+udp_port, address)
				&&
			!Util.equalStrings_null_or_not(___address, address)
				) { // eventually I want to migrate to __address from address
			System.out.println("Address:checkVersionV0: computed="+__address+" vs expected="+address+" in="+this.toLongString());
			Util.printCallPath("Address:"+this.toLongString());
			return false;
		}
		return true;
	}
	/**
	 * Version 2 is with branch/agent version
	 */
	public Encoder getEncoder() {
		switch (this.version_structure) {
		case V0: // like old TypedAddress
			if (DEBUG) System.out.println("Address:enc:V0");
			return getEncoder_V0();
		case V1:
			if (DEBUG) System.out.println("Address:enc:V1");
			return getEncoder_V1();
		case V2:
			if (DEBUG) System.out.println("Address:enc:V2");
			return getEncoder_V2();
		case V3:
			if (DEBUG) System.out.println("Address:enc:V2");
			return getEncoder_V2();
		default:
			if (DEBUG) System.out.println("Address:enc:V2");
			return getEncoder_V2();
		}
	}
	@Deprecated
	public String getOldAddress(){
		String port = ""+tcp_port;
		//if (tcp_port <= 0) port = "" + udp_port;
		//else port = "" + tcp_port;
		//String __address = domain+":"+port;
		String ___address = pure_protocol+"://"+domain+":"+port;
		return ___address;
	}
	/**
--V0
Address ::= SEQUENCE {
 address UTF8String,
 type PrintableString,
 priority INTEGER IF certified
 }
	 * 
	 * @return
	 */
	public Encoder getEncoder_V0() {
		Encoder enc = new Encoder().initSequence();
		String port = ""+tcp_port;
		//if (tcp_port <= 0) port = "" + udp_port;
		//else port = "" + tcp_port;
		String __address = domain+":"+port;
		String ___address = pure_protocol+"://"+domain+":"+port;
		String enc_address = address;
		if (address == null) enc_address = __address;
		if (address != null && 
				! Util.equalStrings_null_or_not(__address, address)
				&& ! Util.equalStrings_null_or_not(__address+":"+udp_port, address)
				&&
			! Util.equalStrings_null_or_not(___address, address)
				) { // eventually I want to migrate to __address from address
			System.out.println("Address:getEncoder_V0: computed="+__address+" vs expected="+address+" in="+this.toLongString());
			Util.printCallPath("Address:"+this.toLongString());
		}
		enc.addToSequence(new Encoder(enc_address).setASN1Type(Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(pure_protocol).setASN1Type(Encoder.TAG_PrintableString));
		if (certified) {
			enc.addToSequence(new Encoder(priority));
		}
		return enc;
	}
	/**
-- V1
Address ::= SEQUENCE {
	domain PrintableString,
	tcp_port INTEGER,
	udp_port INTEGER,
	pure_protocol PrintableString OPTIONAL
}
	 * @return
	 */
	public Encoder getEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(domain).setASN1Type(Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(tcp_port));
		enc.addToSequence(new Encoder(udp_port));
		if(pure_protocol!=null)
			enc.addToSequence(new Encoder(pure_protocol).setASN1Type(Encoder.TAG_PrintableString));
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	/**
-- V2
Address ::= SEQUENCE {
	version_structure [AC0] IMPLICIT INTEGER,
	domain PrintableString,
	tcp_port INTEGER,
	udp_port INTEGER,
	pure_protocol [AC1] IMPLICIT UTF8String OPTIONAL,
	branch [AC2] IMPLICIT UTF8String OPTIONAL,
	agent_version [AC3] IMPLICIT UTF8String OPTIONAL,
	priority [AC4] IMPLICIT INTEGER OPTIONAL CASE certified
}
	 * @return
	 */
	public Encoder getEncoder_V2() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version_structure).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(domain).setASN1Type(Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(tcp_port));
		enc.addToSequence(new Encoder(udp_port));
		if (pure_protocol != null) enc.addToSequence(new Encoder(pure_protocol).setASN1Type(DD.TAG_AC1));
		if (branch != null) enc.addToSequence(new Encoder(branch).setASN1Type(DD.TAG_AC2));
		if (agent_version != null) enc.addToSequence(new Encoder(agent_version).setASN1Type(DD.TAG_AC3));
		if (certified) enc.addToSequence(new Encoder(priority).setASN1Type(DD.TAG_AC4));
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	public Address decode(Decoder dec) {
		Decoder content;
		try {
			content = dec.getContent();
			if (content.getTypeByte() != DD.TAG_AC0) {
				if (content.getTypeByte() == Encoder.TAG_UTF8String) version_structure = V0;
				else
					if (content.getTypeByte() == Encoder.TAG_PrintableString) version_structure = V1;
					else {
						Util.printCallPath("Type="+content.getTypeByte());
						throw new RuntimeException("Unknow Address version");
					}
			}
			else {
				if (DEBUG) System.out.println("Address:decode tag="+content.getTypeByte());
				version_structure = content.getFirstObject(true).getInteger(DD.TAG_AC0).intValue();
			}
			switch (version_structure) {
			case V0:
				if (DEBUG) System.out.println("Address:dec:V0");
				return decode_V0(content);
			case V1:
				if (DEBUG) System.out.println("Address:dec:V1");
				return decode_V1(content);
			case V2:
			case V3:
				if (DEBUG) System.out.println("Address:dec:V2");
				return decode_V2(content);
			default:
				Util.printCallPath("Type not yet supported="+version_structure);
				throw new RuntimeException("Unknow Address version:" +version_structure);
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return this;
		}
	}
	public Address decode_V0(Decoder content) {
		address = content.getFirstObject(true).getString();
		setAddress(address);
		pure_protocol = content.getFirstObject(true).getString();
		Decoder d = content.getFirstObject(true);
		if (d != null) {
			certified = true;
			priority = d.getInteger().intValue();
		}
		return this;
	}
	public Address decode_V1(Decoder content) {
		//this._version_structure = V1;
		domain = content.getFirstObject(true).getString();
		tcp_port = content.getFirstObject(true).getInteger().intValue();
		Decoder udp_dec = content.getFirstObject(true);
		if (udp_dec != null) udp_port = udp_dec.getInteger().intValue();
		else {
			Util.printCallPath("Null decoder udp for "+this);
			udp_port = tcp_port;
		}
		if (content.getFirstObject(false) != null)
			pure_protocol = content.getFirstObject(true).getString();
		branch = null;
		this.agent_version = null;
		return this;
	}
	public Address decode_V2(Decoder content) throws ASN1DecoderFail {
		//this.version_structure = V2;
		domain = content.getFirstObject(true).getString();
		tcp_port = content.getFirstObject(true).getInteger().intValue();
		udp_port = content.getFirstObject(true).getInteger().intValue();
		if (content.getTypeByte() == DD.TAG_AC1)
			pure_protocol = content.getFirstObject(true).getString(DD.TAG_AC1);
		else pure_protocol = null;
		if (content.getTypeByte() == DD.TAG_AC2)
			branch = content.getFirstObject(true).getString(DD.TAG_AC2);
		else branch = null;
		if (content.getTypeByte() == DD.TAG_AC3)
			agent_version = content.getFirstObject(true).getString(DD.TAG_AC3);
		else agent_version = null;
		if (content.getTypeByte() == DD.TAG_AC4) {
			priority = content.getFirstObject(true).getInteger(DD.TAG_AC4).intValue();
			certified = true;
		} else certified = false;
		return this;
	}
	/**
	 * Returns a string made with this (the branch and version read from DD)
	 * @param _domain
	 * @param _tcp_port
	 * @param _udp_port
	 * @param _protocol
	 * @return
	 */
	public static String getAddress(String _domain, int _tcp_port, int _udp_port, String _pure_protocol) {
		Address a = new Address();
		a.domain = _domain;
		a.tcp_port = _tcp_port;
		a.udp_port = _udp_port;
		a.pure_protocol = _pure_protocol;
		a.branch = DD.BRANCH;
		a.agent_version = DD.VERSION;
		return a.toString();
		/*
		String port;
		if(_udp_port>=0) port = _tcp_port+ADDR_PART_SEP+_udp_port;
		else port = _tcp_port+"";
		if(_protocol == null) return _domain+ADDR_PART_SEP+port;
		return _protocol+ADDR_PROT_SEP+_domain+ADDR_PART_SEP+port;
		*/
	}
	public static String getProtocol(String address){
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if(protos.length > 1) return protos[0].trim();
		return null;
	}
	public static String getDomain(String address){
		String domain;
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if(protos.length > 1) domain = protos[1];
		else domain = protos[0];
		return domain.split(Pattern.quote(ADDR_PART_SEP))[0].trim();
	}
	public static int getTCP(String address){
		String domain;
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if (DEBUG) System.out.println("Address:getTCP: adr="+address+" splits="+Util.concat(protos, " - "));
		if(protos.length > 1) domain = protos[1];
		else domain = protos[0];
		String[] ports = domain.split(Pattern.quote(ADDR_PART_SEP));
		if (DEBUG) System.out.println("Address:getTCP: dom="+domain+" splits="+Util.concat(ports, " - "));
		if(ports.length<2) return -1;
		if((ports[1]==null) || (ports[1].length()==0)) return 0;
		return Integer.parseInt(ports[1]);
	}
	/**
	 * Returns tcp address if no udp specified
	 * @param address
	 * @return
	 */
	public static int getUDP(String address){
		String domain;
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if(protos.length > 1) domain = protos[1];
		else domain = protos[0];
		String[] ports = domain.split(Pattern.quote(ADDR_PART_SEP));
		if(ports.length<2) return -1; 
		try{
			if (ports.length == 2) return Integer.parseInt(ports[1]); 
			return Integer.parseInt(ports[2]);
		} catch(Exception e) {
			System.out.println("Error parsing:"+address);
			return -1; // happens with name of directory in application table
		}
	}
	/**
	 * [<PROT>://]domain:...:...:<name>[:<active_int_boolean>:...]
	 * @param address
	 * @return
	 */
	public static String getName(String address){
		String domain;
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if(protos.length > 1) domain = protos[1];
		else domain = protos[0];
		String[] ports = domain.split(Pattern.quote(ADDR_PART_SEP));
		if(ports.length < 4) return null; 
		return ports[3]; 
	}
	/**
	 * [<PROT>://]domain:...:...:....:<active_int_boolean>[:...]
	 * @param address
	 * @return
	 */
	public static boolean getActive(String address){
		String domain;
		String[] protos = address.split(Pattern.quote(ADDR_PROT_INTERN_SEP));
		if (protos.length > 1) domain = protos[1];
		else domain = protos[0];
		String[] ports = domain.split(Pattern.quote(ADDR_PART_SEP));
		if (DEBUG) System.out.println("Address:getActive "+domain+" ports="+Util.concat(ports, "=="));
		if (ports.length < 5) return true; 
		return Util.stringInt2bool(ports[4], false); 
	}
	public static String joinAddresses(String addresses1, String addrSep,
			String addresses2) {
		if(addresses1==null) return addresses2;
		if(addresses2==null) return addresses1;
		return addresses1+addrSep+addresses2;
	}
	/**
	 * Uses DirectoryServer.ADDR_SEP="," as separator
	 * @param addresses1
	 * @param addresses2
	 * @return
	 */
	public static String joinAddresses(String addresses1,
			String addresses2) {
		if(addresses1==null) return addresses2;
		if(addresses2==null) return addresses1;
		//if((addresses1==null)&&(addresses2==null)) return null;
		return addresses1+DirectoryServer.ADDR_SEP+addresses2;
	}
	/**
	 * Splits string by DirectoryServer.ADDR_SEP (,)
	 * @param address
	 * @return
	 */
	public static String[] split(String address) {
		if(address==null) return new String[0];
		String[] result;
		if(DEBUG) System.out.println("Address:split: parsing address "+address+" BY "+DirectoryServer.ADDR_SEP);
		result = address.split(Pattern.quote(DirectoryServer.ADDR_SEP));
		if(DEBUG) System.out.println("Address:split: parsing address #="+result.length);
		if(DEBUG)
			for(int k=0;k<result.length; k++) {
				System.out.println("Address:split: parsing address #["+k+"]="+result[k]);
			}
		return result;
	}
	public static String joinAddresses(Address[] addresses) {
		return Util.concat(addresses, DirectoryServer.ADDR_SEP, null);
	}
	/**
	 * Assumes that the string is well formed:
	 * IP:tcp[:udp[:name[:...]]],IP:tcp[:udp[:name[:...]]], 
	 * Where "," is DirectoryServer.ADDR_SEP
	 * @param a
	 * @return
	 */
	public static Address[] getAddresses(String a) {
		if (a == null) return null;
		String[] s = split(a);
		Address[] A = new Address[s.length];
		for (int k = 0; k < A.length; k++) {
			A[k] = new Address(s[k]);
		}
		return A;
	}
	/**
	 * Used to specify defaults (e.g. addresses from directory_listings)
	 * @param dir2
	 */
	public void setPureProtocol(String dir2) {
		this.pure_protocol = dir2;
	}
	public void setDomain(String string) {
		this.domain = string;
	}
	public void setBothPorts(String string) {
		try{
			this.tcp_port = this.udp_port = Integer.parseInt(string);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public void setName(String string) {
		this.name = string;
	}
	public void setBranch(String string) {
		this.branch =  string;
	}
	public void setAgentVersion(String string) {
		this.agent_version = string;
	}
	public int getTCPPort() {
		return tcp_port;
	}
	public String getIP() {
		return domain;
	}
	public String ipPort() {
		return getIP()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+getTCPPort();
	}
	public static int getStructureVersion(Address dir_address) {
		int version;
		if (Util.isVersionNewer(DirectoryAnnouncement.AGENT_VERSION_V1, dir_address.agent_version)) {
			if (DEBUG) System.out.println("DirRequest: V2: "+dir_address);
			version = V1;
		}else {
			if (DEBUG) System.out.println("DirRequest: V3: "+dir_address);
			version = V2;
		}
		return version;
	}
	public void setActive(boolean value) {
		active = value;
	}
	public String toDirActivityString() {
		String result = this.toString();
	
		if (Address.DIR.equals(pure_protocol) && (branch==null) && (agent_version==null))
			result = this.getDomain()+ADDR_PART_SEP+this.getTCPPort();
		if (!active)
		 result += Address.ADDR_PART_SEP+Util.bool2StringInt(active);
		return result;
	}
	public void set_version_structure(int address_version) {
		if (DEBUG) System.out.println("Address: set version: "+address_version+" old="+this.version_structure);
		this.version_structure = address_version;
	}
	/**
	 * parse the string, split by ".", make integers
	 * returns null if the conversion fails (default version is "V_")
	 * @return
	 */
	public int[] getAgentVersionInts() {
		String av = getAgentVersion();
		try {
			String[] avs = av.split(Pattern.quote("."));
			int[] _avs = new int[avs.length];
			for (int k = 0; k < _avs.length; k++) {
				_avs[k] = Integer.parseInt(avs[k]);
			}
			return _avs;
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	public static final int COMPONENTS = 2; // http://domain:tcp:udp/priority
	public static ArrayList<Address> getAddress(String addresses) {
		//boolean DEBUG = true;
		if(D_Peer.DEBUG) System.out.println("PeerAddress:getAddress: parsing addresses "+addresses);
		String[]addresses_l = Address.split(addresses);
		// may have to control addresses, to defend from DOS based on false addresses
		ArrayList<Address> address = new ArrayList<Address>(addresses_l.length);
		for(int k=0; k<addresses_l.length; k++) {
			if(D_Peer.DEBUG)System.out.println("PeerAddress:getAddress: parsing address "+addresses_l[k]);
			Address a = new Address();
			String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PROT_NET_SEP),COMPONENTS);
			if(taddr.length < 2) continue;
			a.setExtendedProtocol(taddr[0]);
			String adr = taddr[1];
			if(adr==null){address.add(a); continue;}
			String pri[] = adr.split(Pattern.quote(Address.PRI_SEP));
			a.setSocketAddress(pri[0]);
			if(pri.length > 1) {
				a.certified = true;
				try{
					a.priority = Integer.parseInt(pri[1]);
				}catch(Exception e){e.printStackTrace();}
			}
			address.add(a);
			if(D_Peer.DEBUG)System.out.println("PeerAddress:getAddress: got typed address "+a);
		}
		return address;
	}
	public static ArrayList<Address> 
	getOrderedCertifiedTypedAddresses(ArrayList<Address> addr) {
		if (DEBUG) System.out.println("Address:getOrdCTA: start "+Util.concatA(addr, "---", "NULL"));
        if ((addr == null) || (addr.size() == 0)) return null;
        ArrayList<Address> result = new ArrayList<Address>();
        int k = 0;
        for (Address ta : addr) {
        	if (ta == null) {
        		if (DEBUG || DD.DEBUG_TODO) {
        			System.out.println("TypedAddress:getOrdCer: No address: "+k);
        			
        			for (int i = 0; i < addr.size(); i ++) {
        				System.out.println("TypedAddress:getOrdCert: adr["+i+"]="+ta);
        			}
        		}
        		continue;
        	}
        	if (!ta.certified) continue;
        	if ((ta.address == null) && (ta.domain == null)) continue;
        	if (ta.domain == null) ta._setAddress(ta.address);
        	result.add(ta);
        	k++;
        }
		if (DEBUG) System.out.println("Address:getOrdCTA: result "+Util.concatA(result, "---", "NULL"));
        if (result.size() == 0) return null;
        //Address[] r = result.toArray(new TypedAddress[]{});
        //Collections.sort(result, new TypedAddressComparator());
        //Arrays.sort(result, new TypedAddressComparator());
        Collections.sort(result, new TypedAddressComparator());
        return result;
	}
	public String getDomain() {
		return domain;
	}
	
	public long get_peer_address_ID() {
		return Util.lval (this.peer_address_ID, -1);
	}
	public String getSocketAddress() {
		return this.domain+":"+this.tcp_port+":"+this.udp_port;
	}
	public void setSocketAddress(String adr) {
		if (adr == null) return;
		String [] a = adr.split(Pattern.quote(":"));
		domain = a[0];
		if (a.length == 1) return;
		udp_port = tcp_port = Integer.parseInt(a[1]);
		if (a.length == 2) return;
		udp_port = Integer.parseInt(a[2]);
	}
	public String getExtendedProtocol() {
		return getProtocolVersion();
//		if (branch == null) return this.pure_protocol;
//		return this.pure_protocol+"."+this.branch+"."+this.agent_version;
	}
	public void setExtendedProtocol(String adr) {

		if (adr == null) return;
		String [] a = adr.split(Pattern.quote(PROT_SEP));
		pure_protocol = a[0];
		agent_version = branch = null;
		if (a.length == 1) return;
		branch = a[1];
		if (a.length == 2) return;
		agent_version = a[2];
	}
	public void setDirType() {
		this.setPureProtocol(Address.DIR);
		this.setAgentVersion(DD.VERSION);
		this.setBranch(DD.BRANCH);
		this.version_structure = Address.getLastVersion();
	}
	public static int getLastVersion() {
		return V3;
	}
	public void setPorts(int t_port, int u_port) {
		this.tcp_port = t_port;
		this.udp_port = u_port;
	}
	public boolean validAddress() {
		if (this.domain == null) return false;
		if (this.tcp_port <= 0 && this.udp_port <= 0) return false;
		return true;
	}
	/**
	 * return true if they have the same domain (used to compare NAT - IP addresses)
	 * @param detected_sa
	 * @param old_detected_NAT
	 * @return
	 */
	public static boolean sameDomain(Address adr1, Address adr2) {
		if (adr1 == null || adr2 == null) return false;
		boolean result = Util.equalStrings_null_or_not(adr1.getDomain(), adr2.getDomain());
		return result;
	}
}

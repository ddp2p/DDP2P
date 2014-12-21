package util;

import java.util.ArrayList;
import java.util.Calendar;

import hds.Address;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
/**
 * Version of Address
 * Is it really worth having an extra class just for the member name and no udp port?
 * @author msilaghi
 *
 */
public class DirectoryAddress extends ASNObj{
	public static boolean DEBUG = false;
	public String version = "0";
	public String pure_protocol;
	public String agent_version;
	public String branch;
	public String domain;
	public String name; // this is added in addition to Address
	public int tcp_port; // only one port
	public int udp_port; // only one port
	
	public long directory_address_ID = -1;
	public byte[] signature;
	public String comments;
	public String GID;
	public String GIDH;
	public String instance;
	public boolean revoked = false;
	public boolean active = true;
	public Calendar contact;
	public Calendar creation;

	public String toLongString() {
		String str = "[DirectoryAddress:v="+version+" p="+pure_protocol+" av="+agent_version+" b="+branch+" d="+domain+" t="+tcp_port+" u="+udp_port+" n="+name+"]";
		return str;
	}
	public DirectoryAddress() {}
	public DirectoryAddress(Address a) {
		pure_protocol = a.pure_protocol;
		agent_version = a.agent_version;
		branch = a.branch;
		domain = a.domain;
		tcp_port = a.tcp_port;
		udp_port = a.udp_port;
		name = a.name;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(domain));
		enc.addToSequence(new Encoder(tcp_port));
		if (pure_protocol != null) {
			enc.addToSequence(new Encoder(pure_protocol));
			enc.addToSequence(new Encoder(agent_version));
			enc.addToSequence(new Encoder(branch));
		}
		if (name != null) enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AP1));
		enc.addToSequence(new Encoder(udp_port).setASN1Type(DD.TAG_AP2));
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	@Override
	public DirectoryAddress decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		domain = d.getFirstObject(true).getString();
		tcp_port = d.getFirstObject(true).getInteger().intValue();
		byte type = d.getTypeByte();
		if ((type != 0) && (type != DD.TAG_AP1)) {
			pure_protocol = d.getFirstObject(true).getString();
			agent_version = d.getFirstObject(true).getString();
			branch = d.getFirstObject(true).getString();
		}
		type = d.getTypeByte();
		if (type == DD.TAG_AP1) name = d.getFirstObject(true).getString();
		type = d.getTypeByte();
		if (type == DD.TAG_AP2) udp_port = d.getFirstObject(true).getInteger().intValue();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new DirectoryAddress();}
	public String toString() {
		Address adr = new Address(this);
		/*
		adr.domain = domain;
		adr.tcp_port = tcp_port;
		adr.udp_port = udp_port;
		adr.pure_protocol = pure_protocol;
		adr.branch = branch;
		adr.agent_version = agent_version;
		*/
		return adr.toStringStorage();
		//return domain+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+port;
	}
	public static byte getASN1Type() {
		return DD.TAG_AC17;
	}

	public static DirectoryAddress getDirectoryAddress(ArrayList<Object> a) {
		DirectoryAddress result = new DirectoryAddress();
		result.active = Util.stringInt2bool(a.get(table.directory_address.DA_ACTIVE), false);
		result.revoked = Util.stringInt2bool(a.get(table.directory_address.DA_REVOKATION), false);
		result.version = Util.getString(a.get(table.directory_address.DA_VERSION));
		result.domain = Util.getString(a.get(table.directory_address.DA_DOMAIN));
		result.pure_protocol = Util.getString(a.get(table.directory_address.DA_PROTOCOL));
		result.branch = Util.getString(a.get(table.directory_address.DA_BRANCH));
		result.agent_version = Util.getString(a.get(table.directory_address.DA_AGENT_VERSION));
		result.tcp_port = Util.ival(a.get(table.directory_address.DA_TCP_PORT), -1);
		result.udp_port = Util.ival(a.get(table.directory_address.DA_UDP_PORT), -1);
		if (result.udp_port <= 0) { if (DEBUG) System.out.println("DirAddr:getDirAddress:udp="+result.domain+":"+result.udp_port+":"+result.tcp_port); result.udp_port = result.tcp_port;}
		result.name = Util.getString(a.get(table.directory_address.DA_NAME));
		result.comments = Util.getString(a.get(table.directory_address.DA_COMMENTS));
		result.signature = Util.byteSignatureFromString(Util.getString(a.get(table.directory_address.DA_SIGN)));
		result.contact = Util.getCalendar(Util.getString(a.get(table.directory_address.DA_CONTACT)));
		result.creation = Util.getCalendar(Util.getString(a.get(table.directory_address.DA_DATE_SIGNATURE)));
		result.directory_address_ID = Util.lval(a.get(table.directory_address.DA_DIRECTORY_ADDR_ID), -1);
		result.GID = Util.getString(a.get(table.directory_address.DA_GID));
		result.GIDH = Util.getString(a.get(table.directory_address.DA_GIDH));
		result.instance = Util.getString(a.get(table.directory_address.DA_INSTANCE));
		return result;
	}
	public void store() {
		if (DEBUG) System.out.println("DirectoryAddress: store: start LID="+directory_address_ID);
		String param[] = null;
		if (this.directory_address_ID <= 0) 
			param = new String[table.directory_address.FIELDS_NOID];
		else
			param = new String[table.directory_address.FIELDS];
		param[table.directory_address.DA_ACTIVE] = Util.bool2StringInt(active);
		param[table.directory_address.DA_REVOKATION] = Util.bool2StringInt(revoked);
		param[table.directory_address.DA_BRANCH] = branch;
		param[table.directory_address.DA_VERSION] = version;
		param[table.directory_address.DA_AGENT_VERSION] = agent_version;
		param[table.directory_address.DA_PROTOCOL] = pure_protocol;
		param[table.directory_address.DA_DOMAIN] = domain;
		param[table.directory_address.DA_GID] = GID;
		param[table.directory_address.DA_GIDH] = GIDH;
		param[table.directory_address.DA_INSTANCE] = instance;
		param[table.directory_address.DA_NAME] = name;
		param[table.directory_address.DA_COMMENTS] = comments;
		param[table.directory_address.DA_TCP_PORT] = tcp_port+"";
		param[table.directory_address.DA_UDP_PORT] = udp_port+"";
		param[table.directory_address.DA_CONTACT] = Encoder.getGeneralizedTime(contact);
		param[table.directory_address.DA_DATE_SIGNATURE] = Encoder.getGeneralizedTime(creation);
		try {
			if (this.directory_address_ID <= 0) {
				if (DEBUG) System.out.println("DirectoryAddress: store: inserting");
				this.directory_address_ID = Application.db.insert(table.directory_address.TNAME, 
						table.directory_address.fields_noID_list,
						param, DEBUG);
			} else {
				param[table.directory_address.DA_DIRECTORY_ADDR_ID] = directory_address_ID + "";
				Application.db.update(table.directory_address.TNAME, 
						table.directory_address.fields_noID_list,
						new String[]{table.directory_address.directory_address_ID},
						param, DEBUG);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	static final String getActiveDirs = "SELECT "+table.directory_address.fields+
			" FROM "+table.directory_address.TNAME+
			" WHERE "+table.directory_address.active+"='1';";
	public static DirectoryAddress[] getActiveDirectoryAddresses() {
		DirectoryAddress[] result;
		try {
			ArrayList<ArrayList<Object>> d = Application.db.select(getActiveDirs, new String[0]);
			result = new DirectoryAddress[d.size()];
			for (int k = 0; k < d.size(); k++) {
				result[k] = DirectoryAddress.getDirectoryAddress(d.get(k));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return   new DirectoryAddress[0];
		}
		return result;
	}
	static final String getDirs = "SELECT "+table.directory_address.fields+
			" FROM "+table.directory_address.TNAME+";";
	public static DirectoryAddress[] getDirectoryAddresses() {
		DirectoryAddress[] result;
		try {
			ArrayList<ArrayList<Object>> d = Application.db.select(getDirs, new String[0]);
			result = new DirectoryAddress[d.size()];
			for (int k = 0; k < d.size(); k++) {
				result[k] = DirectoryAddress.getDirectoryAddress(d.get(k));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return   new DirectoryAddress[0];
		}
		return result;
	}
	public void delete() {
		if (this.directory_address_ID <= 0) return;
		try {
			Application.db.delete(table.directory_address.TNAME, 
					new String[]{table.directory_address.directory_address_ID},
					new String[]{this.directory_address_ID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void setDomain(String string) {
		domain = string;
		
	}
	public void setBothPorts(String string) {
		this.udp_port = this.tcp_port = Util.ival(string, this.tcp_port);
		
	}
	public void setName(String string) {
		name = string;
		
	}
	public void setBranch(String string) {
		branch = string;
		
	}
	public void setAgentVersion(String string) {
		agent_version = string;
		
	}
	public void setActive(Boolean value) {
		active = value;
		
	}
	public String ipPort() {
		return getIP()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+getTCPPort();
	}
	public int getTCPPort() {
		return this.tcp_port;
	}
	public String getIP() {
		
		return domain;
	}
	public static String getDirectoryAddressesStr() {
		DirectoryAddress[] da = getDirectoryAddresses();
		return Util.concat(da,DD.APP_LISTING_DIRECTORIES_SEP);
	}
	public static void reset(String val) {
		try {
			Application.db.delete(table.directory_address.TNAME, new String[]{}, 
					new String[]{}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		DD_DirectoryServer ds = new DD_DirectoryServer();
		ds.parseAddress(val);
		//System.out.println("DirectoryAddress: reset: val = "+ds);
		if (!ds.empty()) ds.save();
	}

}
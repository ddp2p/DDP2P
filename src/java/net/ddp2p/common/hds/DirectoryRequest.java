package net.ddp2p.common.hds;
import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;
import java.io.InputStream;
import java.util.ArrayList;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_DirectoryServerPreapprovedTermsInfo;
import net.ddp2p.common.table.directory_forwarding_terms;
import net.ddp2p.common.table.directory_tokens;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class DirectoryRequestPerInstance extends ASNObj {
	String instance;
	public DIR_Terms_Preaccepted[] instance_terms;
	public String toString() {
		return "DirRPI: [inst="+instance+"\ninst_term="+Util.concat(instance_terms, "\n ")+"]";
	}
	/**
DirectoryRequestPerInstance ::= SEQUENCE [AC27] IMPLICIT {
	instance [AC1] IMPLICIT OPTIONAL DEFAULT (NULL),
	instance_terms [AC2] IMPLICIT SEQUENCE OF DIR_Terms_Preaccepted
}
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if (instance != null) enc.addToSequence(new Encoder(instance).setASN1Type(DD.TAG_AC1));
		enc.addToSequence(Encoder.getEncoder(instance_terms).setASN1Type(DD.TAG_AC2)); 
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	@Override
	public DirectoryRequestPerInstance decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if (d.isFirstObjectTagByte(DD.TAG_AC1))
			instance = d.getFirstObject(true).getString(DD.TAG_AC1);
		else instance = null;
		instance_terms = d.getFirstObject(true).getSequenceOf(DIR_Terms_Preaccepted.getASN1Type(), new DIR_Terms_Preaccepted[0], new DIR_Terms_Preaccepted());
		return this;
	}
	public DirectoryRequestPerInstance instance() {
		return new DirectoryRequestPerInstance();
	}
	static byte getASN1Type() {
		return DD.TAG_AC27;
	}
}
public class DirectoryRequest extends ASNObj{
	/** 
	 * TODO It seems that the last director server only answers correctly  to V3 (have to test why).
	 * Enable others only after verifying that the correct version of directory agents is stored in table "peer_address"
	 */
	private static final int V1 = 1;
	private static final int V2 = 2;
	private static final int V3 = 3;
	private static final int V_LAST = V3;
	public static boolean DEBUG = false;
	private static boolean _DEBUG = true;
	private static int MAX_VERSION_SUPPORTED = V3;
	public int version = V2; 
	public int[] agent_version = DD.getMyVersion();
	public String branch = DD.BRANCH;
	public String globalID;
	public String globalIDhash; 
	public DIR_Terms_Preaccepted[] terms_default; 
	public String initiator_globalID; 
	public String initiator_globalIDhash; 
	public String initiator_instance;
	public int UDP_port;
	public byte[] signature;
	public String directory_domain;
	public int director_udp_port;
	public boolean only_given_instances = true; 
	public ArrayList<DirectoryRequestPerInstance> req_instances;
	private String dir_address; 
	private Address _dir_address; 
	private String peer_ID; 
	public String toString() {
		String result= "[DirectoryRequest:" +
				"v="+version+" agent_version="+Util.concat(agent_version, ".", "NULL")+" branch="+branch+
				((this.globalID!=null)?"\n gID="+Util.trimmed(globalID):"")+
				((this.globalIDhash!=null)?"\n gIDH="+Util.trimmed(globalIDhash):"")+
		       "\n  from:"+Util.trimmed(initiator_globalID)+"\n  UDPport="+UDP_port ;
			if(terms_default!=null)
		       for(int i=0; i<terms_default.length; i++){
		       	result+="\n  terms["+i+"]\n"+ terms_default[i];
		       }
			else result+="\n terms=null";
			result += 
					((this.initiator_globalID != null) ? ("\n i_gID="+Util.trimmed(initiator_globalID)) : "")+
					((this.initiator_globalIDhash != null) ? ("\n i_gIDH="+Util.trimmed(initiator_globalIDhash)) : "")+
					"\n only="+only_given_instances+
					"\n sign="+Util.byteToHexDump(signature)+
					"\n req_inst="+Util.concat(req_instances, "\n  ", "NULL");
	    return result+"]";
	}
	static boolean warned_version = false;
	@Override
	public DirectoryRequest decode(Decoder dec) throws ASN1DecoderFail {
		Decoder dr = dec.getContent();
		version = 0;
		if (dr.getTypeByte() == Encoder.TAG_INTEGER) {
			int _version = dr.getFirstObject(true).getInteger().intValue();
			if (DEBUG) System.out.println("DirRequest:decode: decoded _version = "+_version);
			if (_version > MAX_VERSION_SUPPORTED) {
				Util.printCallPath("Need to update software. I do not understand Requests v:"+_version+" v="+version);
				if (!warned_version) {
					warned_version = true;
					Application_GUI.warning(__("New version of DirectoryRequest:")+_version, __("New version available"));
				}
				version = MAX_VERSION_SUPPORTED;
			} else 
				version = _version;
		}
		if (DEBUG) System.out.println("DirRequest:decode: version = "+version);
		switch (version) {
		case 0:
		case 1:
		case 2:
			return decode_2(dr);
		case 3:
		default:
			return decode_3(dr);
		}
	}
	public DirectoryRequest decode_2(Decoder dr) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("DirRequest:decode2: enter");
		globalID = dr.getFirstObject(true).getString();
		if(dr.getTypeByte() == DD.TAG_AC5)
			terms_default = dr.getFirstObject(true).getSequenceOf(DIR_Terms_Preaccepted.getASN1Type(), new DIR_Terms_Preaccepted[]{}, new DIR_Terms_Preaccepted());
		initiator_globalID = dr.getFirstObject(true).getString();
		this.UDP_port = dr.getFirstObject(true).getInteger().intValue();
		if((version!=0) && (dr.getTypeByte()==Encoder.TAG_OCTET_STRING))
			signature = dr.getFirstObject(true).getBytesAnyType();
		if (DEBUG) System.out.println("DirRequest:decode2: obtained:"+this);
		return this;
	}
	public DirectoryRequest decode_3(Decoder dr) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("DirRequest:decode3: enter");
		if((dr.isFirstObjectTagByte(DD.TAG_AC1)))
			agent_version = dr.getFirstObject(true).getIntsArray();
		else
			System.out.println("DirRequest:decode3: no agent_version in version3!!!");
		if ((dr.isFirstObjectTagByte(DD.TAG_AC2)))
			globalID = dr.getFirstObject(true).getString(DD.TAG_AC2);
		if (dr.isFirstObjectTagByte(DD.TAG_AC3))
			globalIDhash = dr.getFirstObject(true).getString(DD.TAG_AC3);
		if (dr.isFirstObjectTagByte(DD.TAG_AC12))
			this.only_given_instances = dr.getFirstObject(true).getBoolean(DD.TAG_AC12);
		else
			System.out.println("DirRequest:decode3: no only_given_instance in version3!!!");
		if (dr.isFirstObjectTagByte(DD.TAG_AC13))
			branch = dr.getFirstObject(true).getString(DD.TAG_AC13);
		if (dr.getTypeByte() == DD.TAG_AC5)
			terms_default = dr.getFirstObject(true).getSequenceOf(DIR_Terms_Preaccepted.getASN1Type(), new DIR_Terms_Preaccepted[]{}, new DIR_Terms_Preaccepted());
		if((version < 3)||(dr.isFirstObjectTagByte(DD.TAG_AC6)))
			initiator_globalID = dr.getFirstObject(true).getString(DD.TAG_AC6);
		if (DEBUG) System.out.println("DirRequest:decode3: tag7="+dr.getTypeByte()+" vs "+DD.TAG_AC7);
		if (dr.isFirstObjectTagByte(DD.TAG_AC7))
			initiator_globalIDhash = dr.getFirstObject(true).getString(DD.TAG_AC7);
		if (DEBUG) System.out.println("DirRequest:decode3: tag8="+dr.getTypeByte());
		if(dr.isFirstObjectTagByte(DD.TAG_AC8))
			initiator_instance = dr.getFirstObject(true).getString(DD.TAG_AC8);
		if (DEBUG) System.out.println("DirRequest:decode3: tag10="+dr.getTypeByte());
		if(dr.isFirstObjectTagByte(DD.TAG_AC10))
			this.UDP_port = dr.getFirstObject(true).getInteger(DD.TAG_AC10).intValue();
		if(dr.isFirstObjectTagByte(DD.TAG_AP16))
			this.directory_domain = dr.getFirstObject(true).getString(DD.TAG_AP16);
		if(dr.isFirstObjectTagByte(DD.TAG_AP17))
			this.director_udp_port = dr.getFirstObject(true).getInteger(DD.TAG_AP17).intValue();
		if (DEBUG) System.out.println("DirRequest:decode3: tag14="+dr.getTypeByte());
		if(dr.isFirstObjectTagByte(DD.TAG_AC14))
			this.req_instances = dr.getFirstObject(true).getSequenceOfAL(DirectoryRequestPerInstance.getASN1Type(), new DirectoryRequestPerInstance()); 
		if (DEBUG) System.out.println("DirRequest:decode3: tag sign="+dr.getTypeByte());
		if((version!=0) && (dr.getTypeByte()==Encoder.TAG_OCTET_STRING))
			signature = dr.getFirstObject(true).getBytesAnyType();
		if (DEBUG) System.out.println("DirRequest:decode3: tag final="+dr.getTypeByte());
		if (DEBUG) System.out.println("DirRequest:decode3: obtained:"+this);
		return this;
	}
	/**
DirectoryRequest = SEQUENCE { -- probably an old version no longer used
 		globalID PrintableString,
 		initiator_globalID PrintableString,
 		UDP_port INTEGER
 }
DirectoryRequest ::= SEQUENCE { -- V3
	version INTEGER,
	agent_version [AC1] IMPLICIT SEQUENCE OF INTEGERS,
	globalID [AC2] IMPLICIT PrintableString OPTIONAL,
	globalID_hash [AC3] IMPLICIT PrintableString OPTIONAL,
	instance [AC11] IMPLICIT PrintableString OPTIONAL,
	only_given_instance [AC12] IMPLICIT BOOLEAN,
	branch [AC13] IMPLICIT UTF8String OPTIONAL,
	terms [AC5] IMPLICIT SEQUENCE OF DIR_Terms_Preaccepted OPTIONAL,
	initiator_globalID [AC6] IMPLICIT PrintableString OPTIONAL,
	initiator_globalIDhash [AC7] IMPLICIT PrintableString OPTIONAL,
	UDP_port [AC10] IMPLICIT INTEGER OPTIONAL DEFAULT (-1),
	req_instances [AC14] IMPLICIT SEQUENCE OF DirectoryRequestPerInstance,
	signature NULLOCTETSTRING
}
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc;
		switch(version){
		case 0:
		case 1:
		case 2:
			enc = getEncoder_2();
			break;
		case 3:
		default:
			enc = getEncoder_3();
			break;
		}
		enc.setASN1Type(getASN1Tag());
		return enc;
	}
	/**
DirectoryRequest ::= SEQUENCE { -- V2
	version INTEGER,
	globalID PrintableString,
	terms [AC5] SEQUENCE OF DIR_Terms_Preaccepted OPTIONAL,
	initiator_globalID PrintableString,
	UDP_port INTEGER
	signature NULLOCTETSTRING
}
	 * @return
	 */
	public Encoder getEncoder_2() {
		Encoder enc = new Encoder().initSequence();
		if(version != 0) enc.addToSequence(new Encoder(version));
		if(globalID != null) enc.addToSequence(new Encoder(globalID, false));
		else{
			System.out.println("DirectoryRequest: This version expects a globalID. v="+version);
			System.out.println("DirectoryRequest: "+this);
			Util.printCallPath("call");
			throw new RuntimeException("Receiver expects GID!");
		}
		if((version != 0)&&(terms_default!=null)) enc.addToSequence(Encoder.getEncoder(terms_default).setASN1Type(DD.TAG_AC5));
		if(this.initiator_globalID != null)enc.addToSequence(new Encoder(this.initiator_globalID, false));
		else{
			System.out.println("DirectoryRequest: This version expects an initiator_globalID. v="+version);
			System.out.println("DirectoryRequest: "+this);
			Util.printCallPath("call");
			throw new RuntimeException("Receiver expects initiatorGID!");
		}
		enc.addToSequence(new Encoder(this.UDP_port));
		if(version != 0) enc.addToSequence(new Encoder(signature));
		return enc;
	}
	/**
DirectoryRequest ::= SEQUENCE { -- V3
	version INTEGER,
	agent_version [AC1] IMPLICIT SEQUENCE OF INTEGERS,
	globalID [AC2] IMPLICIT PrintableString OPTIONAL,
	globalID_hash [AC3] IMPLICIT PrintableString OPTIONAL,
	instance [AC11] IMPLICIT PrintableString OPTIONAL,
	only_given_instance [AC12] IMPLICIT BOOLEAN,
	branch [AC13] IMPLICIT UTF8String OPTIONAL,
	terms [AC5] IMPLICIT SEQUENCE OF DIR_Terms_Preaccepted OPTIONAL,
	initiator_globalID [AC6] IMPLICIT PrintableString OPTIONAL,
	initiator_globalIDhash [AC7] IMPLICIT PrintableString OPTIONAL,
	UDP_port [AC10] IMPLICIT INTEGER OPTIONAL DEFAULT (-1),
	req_instances [AC14] IMPLICIT SEQUENCE OF DirectoryRequestPerInstance,
	signature NULLOCTETSTRING
}
	 * @return
	 */
	public Encoder getEncoder_3() {
		Encoder enc = new Encoder().initSequence();
		if (version != 0) enc.addToSequence(new Encoder(version));
		if (version >= 3) enc.addToSequence(Encoder.getEncoderArray(agent_version).setASN1Type(DD.TAG_AC1));
		if (globalID != null) enc.addToSequence(new Encoder(globalID, false).setASN1Type(DD.TAG_AC2));
		if (globalIDhash != null) enc.addToSequence(new Encoder(globalIDhash, false).setASN1Type(DD.TAG_AC3));
		enc.addToSequence(new Encoder(this.only_given_instances).setASN1Type(DD.TAG_AC12));
		if (branch != null) enc.addToSequence(new Encoder(branch).setASN1Type(DD.TAG_AC13));
		if ((version != 0)&&(terms_default!=null)) enc.addToSequence(Encoder.getEncoder(terms_default).setASN1Type(DD.TAG_AC5));
		if (this.initiator_globalID != null) enc.addToSequence(new Encoder(this.initiator_globalID, false).setASN1Type(DD.TAG_AC6));
		if (this.initiator_globalIDhash != null) enc.addToSequence(new Encoder(this.initiator_globalIDhash, false).setASN1Type(DD.TAG_AC7));
		if (this.initiator_instance != null) enc.addToSequence(new Encoder(this.initiator_instance, false).setASN1Type(DD.TAG_AC8));
		if (this.UDP_port > 0) enc.addToSequence(new Encoder(this.UDP_port).setASN1Type(DD.TAG_AC10));
		if (this.directory_domain != null) enc.addToSequence(new Encoder(this.directory_domain).setASN1Type(DD.TAG_AP16));
		if (this.director_udp_port > 0) enc.addToSequence(new Encoder(this.director_udp_port).setASN1Type(DD.TAG_AP17));
		enc.addToSequence(Encoder.getEncoder(this.req_instances).setASN1Type(DD.TAG_AC14));
		if (version!=0) enc.addToSequence(new Encoder(signature));
		return enc;
	}
	public DirectoryRequest(byte[]_buffer, int peek, InputStream is) throws Exception {
		assert(DirectoryServer.MAX_DR_DA>=peek);
		Decoder dec = new Decoder(_buffer);
		if(dec.contentLength()>DirectoryServer.MAX_DR_DA) throw new Exception("Max buffer DirectoryServer.MAX_DR_DA="+DirectoryServer.MAX_DR_DA+
				" is smaller than request legth: "+dec.contentLength());
		/** useless complication to use shared buffer, since it synchronizes requests and can be used in attacks
		synchronized (buffer) {
			Encoder.copyBytes(buffer, 0, _buffer, peek, 0);
			read(buffer, peek, is);
		}
		*/
		read(_buffer, peek, is);
	}
	public DirectoryRequest() {
	}
	public DirectoryRequest(Decoder dec) {
		try {
			decode(dec);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	/**
	 * Build request with pre-approved terms for service.
	 * The terms are read from the "directory_forwarding_terms" table.
	 * 
	 * Typically not signed. (Signatures may be requested on DoS detection/payments)
	 * @param target_GID
	 * @param initiator_GID
	 * @param initiator_udp
	 * @param target_peer_ID : needed to retrieve/maintain current terms
	 * @param dir_address : needed to filter/maintain terms
	 */
	public DirectoryRequest(
			String target_GID,
			String initiator_GID,
			String initiator_instance,
			int initiator_udp,
			String target_peer_ID, 
			Address dir_address) {
		if (DEBUG) System.out.println("DirectoryRequest<init>: GPID="+target_GID+" iniID="+initiator_GID+" udp="+initiator_udp+" pID="+target_peer_ID+" adr="+dir_address);
		globalID = target_GID;
		this.initiator_globalID = initiator_GID;
		this.initiator_instance = initiator_instance;
		this.UDP_port = initiator_udp;
		this.peer_ID = target_peer_ID;
		if (dir_address != null) {
			this.dir_address = dir_address.toString();
			version = getVersionStructure(dir_address);
		}
		this.terms_default = getTerms();
		this.only_given_instances = false;
		this.signature=null; 
	}
	public DirectoryRequest(
			String target_GID,
			String target_instance,
			String initiator_GID,
			String initiator_instance,
			int initiator_udp,
			String target_peer_ID, 
			Address dir_address) {
		if (DEBUG) System.out.println("DirectoryRequest<init>: GPID="+target_GID+" inst="+target_instance+" iniID="+initiator_GID+" udp="+initiator_udp+" pID="+target_peer_ID+" adr="+dir_address);
		if (dir_address != null) {
			this.directory_domain = dir_address.getIP();
			this.director_udp_port = dir_address.udp_port;
		}
		globalID = target_GID;
		this.initiator_globalID = initiator_GID;
		this.initiator_instance = initiator_instance;
		this.UDP_port = initiator_udp;
		this.peer_ID = target_peer_ID;
		if (dir_address != null) {
			this._dir_address = dir_address;
			this.dir_address = dir_address.toString();
			version = getVersionStructure(dir_address);
		}
		this.terms_default = getTerms();
		this.only_given_instances = true;
		this.req_instances = new ArrayList<DirectoryRequestPerInstance>();
		DirectoryRequestPerInstance drpi = new DirectoryRequestPerInstance();
		drpi.instance = target_instance;
		drpi.instance_terms = getTerms();
		this.req_instances.add(drpi);
		this.signature=null; 
	}
	/**
	* TODO to use this function one has to:
	*   1. make sure that the directory does understand V2 (not yet tested and probably not working)
	*   2. Make sure to store the directory agent version/branch in table "peer_address", not done in 2014
	*   
	 * 
	 * @param peer_address
	 * @return
	 *  Currently will always return V3 until the problem is solved!
	 */
	static int getVersionStructure(Address peer_address){
		if (DEBUG) System.out.println("DirRequest: getVersionStructure: enter "+peer_address.toLongString());
		int version;
		if (Util.isVersionNewer(DirectoryAnnouncement.AGENT_VERSION_V1, peer_address.agent_version)) {
			/**
			 * TODO to use this function one has to:
			 *   1. make sure that the directory does understand V2 (not yet tested and probably not working)
			 *   2. Make sure to store the directory agent version/branch in table "peer_address", not done in 2014
			 */
			if (DEBUG) System.out.println("DirRequest: getVersionStructure: V2 (setting V3 until you fix it!): "+peer_address);
			version = V_LAST;
		}else {
			if (DEBUG) System.out.println("DirRequest: getVersionStructure: V3: "+peer_address);
			version = V_LAST;
		}
		return version;
	}
	DIR_Terms_Preaccepted[] getTerms(){
		String sql;
		String[]params;
		Address adrs;
		if (this._dir_address != null) adrs = this._dir_address;
		else adrs = new Address(this.dir_address);
		sql = "SELECT "+directory_forwarding_terms.fields_terms+
			" FROM  "+directory_forwarding_terms.TNAME+
		    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
			" AND (("+directory_forwarding_terms.dir_domain+" =? "+
			" AND "+directory_forwarding_terms.dir_tcp_port+" =? )"+	
			" OR  ("+directory_forwarding_terms.dir_domain+" is NULL "+
			" AND "+directory_forwarding_terms.dir_tcp_port+" is NULL ))"+
			" AND " + directory_forwarding_terms.priority_type+" = 1 ;";
		params = new String[]{this.peer_ID, adrs.domain, ""+adrs.tcp_port};
		if (DEBUG) System.out.println("DirectoryRequest:getTerms: select directory this.dir_address: "+ this.dir_address);
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.getDB().select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if (u == null || u.size() == 0) { 
		    if (DEBUG) System.out.println("DirectoryRequest:getTerms: select for Global directory");
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
			" FROM  "+directory_forwarding_terms.TNAME+
		    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
			" AND "+directory_forwarding_terms.dir_domain+" is NULL "+
			" AND "+directory_forwarding_terms.dir_tcp_port+" is NULL "+
			" AND " + directory_forwarding_terms.priority_type+" = 1 ;";
			params = new String[]{"0"};
			try {
				u = Application.getDB().select(sql, params, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return null;
			}	
		}
		if (u == null || u.size() == 0) return null;
		DIR_Terms_Preaccepted[] dir_terms = new DIR_Terms_Preaccepted[u.size()];
		int i=0; 
		for(ArrayList<Object> _u :u){
			D_DirectoryServerPreapprovedTermsInfo ui = new D_DirectoryServerPreapprovedTermsInfo(_u);
			dir_terms[i] = new DIR_Terms_Preaccepted();
			if(ui.topic){
				ArrayList<Object> record = directory_tokens.searchForToken(Util.lval(this.peer_ID, -1), ui.peer_instance_ID,
						 adrs.domain, adrs.tcp_port+"");
				if (record != null)
					dir_terms[i].topic = Util.getString(record.get(net.ddp2p.common.table.directory_tokens.PA_TOKEN)); 
			}
			else dir_terms[i].topic = null;
			if( ui.ad)
				 dir_terms[i].ad = 1;
			else dir_terms[i].ad = 0;
			if(ui.plaintext)
				 dir_terms[i].plaintext = 1;
			else dir_terms[i].plaintext = 0;
			if(ui.service!=-1)
				dir_terms[i].services_acceptable = Integer.toString(ui.service).getBytes(); 
			if(ui.payment ){
			   dir_terms[i].payment = new DIR_Payment();
			   dir_terms[i].payment.amount = 0;
			   dir_terms[i].payment.method = "Any Method";
			   dir_terms[i].payment.details = "Any details";
			}else dir_terms[i].payment = null;   
			if(DEBUG) System.out.println("DirectoryRequest:getTerms:  add: "+ui);
			i++;
		}
		return dir_terms;
	}
	public DIR_Terms_Preaccepted[] updateTerms(
			DIR_Terms_Requested[] terms, String peer_ID, String global_peer_ID,
			Address dir_address, DIR_Terms_Preaccepted[] terms2) {
		String sql;
		String[]params;
		sql = "SELECT "+directory_forwarding_terms.fields_terms+
			" FROM  "+directory_forwarding_terms.TNAME+
		    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
			" AND ("+directory_forwarding_terms.dir_domain+" =? "+
			" OR  "+directory_forwarding_terms.dir_domain+" is NULL )"+
			" AND " + directory_forwarding_terms.priority_type+" > 1 " +
					" ORDER BY "+directory_forwarding_terms.priority_type+";";
		params = new String[]{this.peer_ID, this.dir_address};
		if(DEBUG) System.out.println("DirectoryRequest:getTerms: select directory this.dir_address: "+ this.dir_address);
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.getDB().select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(u==null || u.size()==0){ 
		    if(DEBUG) System.out.println("DirectoryRequest:getTerms: select for Global directory");
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
			" FROM  "+directory_forwarding_terms.TNAME+
		    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
			" AND "+directory_forwarding_terms.dir_domain+" is NULL "+
			" AND " + directory_forwarding_terms.priority_type+" > 1 " +
			" ORDER BY "+directory_forwarding_terms.priority_type+";";
			params = new String[]{"0"};
			try {
				u = Application.getDB().select(sql, params, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return null;
			}	
		}
		if(u==null || u.size()==0) return null;
		ArrayList<DIR_Terms_Preaccepted> _dir_terms = new ArrayList<DIR_Terms_Preaccepted>();
		DIR_Terms_Preaccepted[] dir_terms; 
		int i=0; 
		ArrayList<Object> record = null; 
		for(ArrayList<Object> _u :u){
			D_DirectoryServerPreapprovedTermsInfo ui = new D_DirectoryServerPreapprovedTermsInfo(_u);
			DIR_Terms_Preaccepted dir_terms_i = new DIR_Terms_Preaccepted();
			if (ui.topic) {
				if (record != null)
					record = directory_tokens.searchForToken(Util.lval(this.peer_ID, -1), ui.peer_instance_ID,
						 dir_address.domain, dir_address.tcp_port+"");			
				 dir_terms_i.topic = Util.getString(record.get(net.ddp2p.common.table.directory_tokens.PA_TOKEN));
			}
			else dir_terms_i.topic = null;
			if( ui.ad)
				 dir_terms_i.ad = 1;
			else dir_terms_i.ad = 0;
			if(ui.plaintext)
				 dir_terms_i.plaintext = 1;
			else dir_terms_i.plaintext = 0;
			if(ui.service!=-1)
				dir_terms_i.services_acceptable = Integer.toString(ui.service).getBytes(); 
			if(ui.payment ){
			   dir_terms_i.payment = new DIR_Payment();
			   dir_terms_i.payment.amount = 0;
			   dir_terms_i.payment.method = "Any Method";
			   dir_terms_i.payment.details = "Any details";
			}else dir_terms_i.payment = null;   
			if(DEBUG) System.out.println("DirectoryRequest:getTerms:  add: "+ui);
			_dir_terms.add(dir_terms_i);
			if(sufficientTerms(_dir_terms, terms)){
				break;
			}
			i++;
		}
		dir_terms = _dir_terms.toArray(new DIR_Terms_Preaccepted[0]);
		return dir_terms;
	}
	/**
	 * Check if the terms2 are satisfied by _dir_terms
	 * @param _dir_terms
	 * @param terms2
	 * @return
	 */
	private boolean sufficientTerms(
			ArrayList<DIR_Terms_Preaccepted> _dir_terms,
			DIR_Terms_Requested[] terms2) {
		return false;
	}
	void read(byte[]buffer, int peek, InputStream is)  throws Exception{
		if (DEBUG) out.println("dirRequest read: ["+peek+"]="+ Util.byteToHexDump(buffer, peek));
		int bytes = peek;
		if (peek <= 0) {
			bytes = is.read(buffer);
			out.println("dirRequest reread: ["+bytes+"]="+ Util.byteToHexDump(buffer, " "));
		}
		int content_length, type_length, len_length, request_length;
		if (bytes < 1) {
			out.println("dirRequest exiting: bytes < 1 ="+bytes+", peek="+peek+" buflen="+buffer.length);
			return;
		}
		Decoder asn = new Decoder(buffer);
		if (asn.type() != Encoder.TYPE_SEQUENCE) {
			out.println("dirRequest exiting, not sequence: ="+asn.type());
			return;
		}
		do {
			type_length = asn.typeLen();
			if (type_length <= 0) {
				out.println("dirRequest reread type =" + type_length);
				if (bytes == DirectoryServer.MAX_DR_DA) throw new Exception("Buffer Type exceeded!");
				if (is.available() <= 0)  throw new Exception("Data not available for type!");
				bytes += is.read(buffer, bytes, DirectoryServer.MAX_DR_DA-bytes);
			}
		}while(type_length <= 0);
		if(DEBUG)out.println(" dirRequest type ="+type_length);
		do{
			len_length = asn.lenLen();
			if(len_length <=0) {
				out.println("dirRequest reread len len ="+len_length);
				if(bytes == DirectoryServer.MAX_DR_DA) throw new Exception("Buffer Length exceeded!");
				if(is.available()<=0)  throw new Exception("Data not available for length!");
				bytes += is.read(buffer, bytes, DirectoryServer.MAX_DR_DA-bytes);
			}
		}while(len_length <= 0);
		if (DEBUG) out.println(" dirRequest len len ="+len_length);
		content_length = asn.contentLength();
		request_length = content_length + type_length + len_length;
		if (DEBUG) out.println(" dirRequest req_len ="+request_length);
		if (request_length > DirectoryServer.MAX_LEN) {
			throw new Exception("Buffer Content exceeded!");
		}
		byte[] buffer_all = buffer;
		if (bytes < request_length) {
			buffer_all = new byte[request_length];
			Encoder.copyBytes(buffer_all, 0, buffer, bytes);
			do {
				bytes += is.read(buffer_all,bytes,request_length - bytes);
			} while(bytes < request_length);
		}
		Decoder dr = new Decoder(buffer_all);
		decode(dr);
	}
	public static byte getASN1Tag() {
		return Encoder.TAG_SEQUENCE;
	}
	public boolean empty() {
		return this.initiator_globalID == null && this.initiator_globalIDhash == null;
	}
}

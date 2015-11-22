/*   Copyright (C) 2011 Marius C. Silaghi
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
 package net.ddp2p.common.util;
import static net.ddp2p.common.util.Util.__;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.data.D_PeerOrgs;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.SR;
public class DD_Address implements StegoStructure {
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int MAX_ID_DESCRIPTION = 50;
	/** Nobody uses this, but it is V3 encoding without addresses */
	public static final String V0 = "0";
	/** Without signature of address	 */
	public static final String V1 = "1";
	/** sign Addresses as TypedAddress[] */
	public static final String V2 = "2";
	/** sign ArrayList<Address */
	public static final String V3 = "3";
	public D_Peer peer;
	public String version=V3;
	public String peer_version;
	public String globalID;
	public String name;
	public String emails;
	public String phones;
	public String slogan;
	public String address;
	public boolean broadcastable;
	public String[] hash_alg;
	public byte[] signature;
	public String creation_date;
	public byte[] picture;
	public D_PeerOrgs[] served_orgs;
	public static String BEGIN = "<<DD ADDRESS>>\n";
	public static String END = "\n<</DD ADDRESS>>";
	public static String VERSION = "\n<<VERSION>>\n";
	public static String NAME = "\n<<NAME>>\n";
	public static String SLOGAN = "\n<<SLOGAN>>\n";
	public static String EMAILS = "\n<<EMAILS>>\n";
	public static String PHONES = "\n<<PHONES>>\n";
	public static String ADDRESS = "\n<<ADDRESS>>\n";
	public static String DATE = "\n<<DATE>>\n";
	public static String BROADCAST = "\n<<BROADCAST>>\n";
	public static String HASH_ALG  = "\n<<HASH_ALG>>\n";
	public static String SERVING = "\n<<SERVING>>\n";
	public static String SIGNATURE = "\n<<SIGNATURE>>\n";
	private static final int DD_TAGS = 11+2;
	public static String SEPREP = "\n#\n";
	public static String SEP = "\n<<";
	public DD_Address() {
		hash_alg = SR.HASH_ALG_V1;
		signature = new byte[0];
	}
	/**
	 * Call with the peer that you want to encode into an image.
	 * @param dd
	 */
	public DD_Address(D_Peer dd) {
		if (V1.equals(version)) { init_V2(dd); return;}
		if (V2.equals(version)) { init_V2(dd); return;}
		if (V3.equals(version)) { init_V3(dd); return;}
	}
	void init_V3(D_Peer dd) {
		if (DEBUG) System.out.println("DD_Address: init_V3: peer="+dd);
		this.peer = dd;
		init_V2(dd); 
		System.out.println("DD_Address: init_V3: done");
	}
	void init_V2(D_Peer dd) {
		if (DEBUG) System.out.println("DD_Address: init_V2");
		boolean encode_addresses = true;
		peer_version = dd.component_basic_data.version;
		globalID = dd.component_basic_data.globalID;
		name = dd.component_basic_data.name;
		slogan = dd.component_basic_data.slogan;
		emails = dd.component_basic_data.emails;
		phones = dd.component_basic_data.phones;
		creation_date = dd.getCreationDate(); 
		picture = dd.component_basic_data.picture;
		if (V2.equals(version)) Util.printCallPath("Need to encode addresses: v="+version+" p="+dd);
		if(encode_addresses && (dd.hasAddresses())){
			address = "";
			for(int k=0; k<dd.shared_addresses.size(); k++) {
				if(!"".equals(address)) address += DirectoryServer.ADDR_SEP;
				if(DD.EXPORT_DDADDRESS_WITH_LOCALHOST) {
					if(dd.shared_addresses.get(k).address.startsWith("localhost:")) continue;
					if(dd.shared_addresses.get(k).address.startsWith("127.0.0.1:")) continue;
				}
				address += dd.shared_addresses.get(k).getPureProtocol()+Address.ADDR_PART_SEP+dd.shared_addresses.get(k).getDomain();
				if(dd.shared_addresses.get(k).certified) address += Address.PRI_SEP+dd.shared_addresses.get(k).priority;
			}
		}
		broadcastable = dd.component_basic_data.broadcastable;
		hash_alg = dd.signature_alg;
		signature = dd.getSignature();
		served_orgs = dd.served_orgs;
		if (DEBUG) System.out.println("DD_Address: init_V2: got "+this);
	}
	/**
	 * Should use the signature in D_PeerAddress
	 * @param sk
	 */
	@Deprecated
	public void sign_DDAddress(SK sk) {
		signature = new byte[0];
		byte[] msg = getBytes();
		if(DEBUG)System.err.println("DDAddress: sign: msg=["+msg.length+"]"+Util.byteToHex(msg, ":"));
		signature = Util.sign(msg,  sk);
		if(DEBUG)System.err.println("DDAddress: sign: sign=["+signature.length+"]"+Util.byteToHex(signature, ":"));
	}
	/**
	 * Does not update version
	 * @param sk
	 */
	public String getString() {
		String result=BEGIN+sane(globalID);
		result+=VERSION+sane(peer_version);
		result+=NAME+sane(name);
		if(V2.equals(peer_version)) {
			result+=EMAILS+sane(emails);
			result+=PHONES+sane(phones);
		}
		result+=SLOGAN+sane(slogan);
		result+=DATE+sane(creation_date);
		result+=ADDRESS+sane(address);
		result+=BROADCAST+sane(broadcastable?"1":"0");
		result+=HASH_ALG+((hash_alg==null)?"NULL":"\""+sane(Util.concat(hash_alg,":"))+"\"");
		result+=SERVING+sane(Util.concatPeerOrgs(served_orgs));
		result+=SIGNATURE+sane(Util.stringSignatureFromByte(signature));
		return result+END;
	}
	public String toString() {
		return getString();
	}
	/**
	 * get a description that fits a warning/acknowledge graphic
	 * @return
	 */
	public String getNiceDescription() {
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: start ");
		String result=BEGIN+sane(Util.trimmed(globalID,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: GID");
		result+=VERSION+sane(Util.trimmed(peer_version,MAX_ID_DESCRIPTION));
		result+=NAME+sane(Util.trimmed(name,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: name");
		if(V2.equals(peer_version)) {
			result+=EMAILS+sane(Util.trimmed(emails,MAX_ID_DESCRIPTION));
			if(DEBUG)System.err.println("DDAddress: getNiceDescription: emails");
			result+=PHONES+sane(Util.trimmed(phones,MAX_ID_DESCRIPTION));
			if(DEBUG)System.err.println("DDAddress: getNiceDescription: phones");
		}
		result+=SLOGAN+sane(Util.trimmed(slogan,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: date");
		result+=DATE+sane(Util.trimmed(creation_date,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: slogan");
		result+=ADDRESS;
		if(address!=null){
			String[] _address=address.split(Pattern.quote(DirectoryServer.ADDR_SEP), 6);
			for(int i=0;i<_address.length;i++){
				result += sane(Util.trimmed(_address[i],MAX_ID_DESCRIPTION));
				if(i < _address.length-1) result+="\n";
			}
		}
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: address");
		result+=BROADCAST+sane(broadcastable?"1":"0");
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: broadcastable");
		result+=HASH_ALG+((hash_alg==null)?"NULL":"\""+sane(Util.trimmed(Util.concat(hash_alg,":"),MAX_ID_DESCRIPTION))+"\"");
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: alg");
		result+=SERVING+sane(Util.trimmed(Util.concatPeerOrgsTrimmed(served_orgs)));
		result+=SIGNATURE;
		if (signature != null)
			result+=sane(Util.trimmed(Util.byteToHex(signature,0,Math.min(MAX_ID_DESCRIPTION/2,signature.length),":"),MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: end ");
		return result+END;
	}
	public void saveSync() throws P2PDDSQLException {
		save();
	}
	public void save() throws P2PDDSQLException {
		if (V0.equals(this.version)) save_V2();
		if (V1.equals(this.version)) save_V2();
		if (V2.equals(this.version)) save_V2();
		if (V3.equals(this.version)) save_V3();
	}
	public void save_V3() throws P2PDDSQLException {
		this.init_V2(peer);
		if (DEBUG) System.out.println("DD_Address: save_V3: start peer="+peer+" \nDD="+this);
		if (this.peer == null) {
			if (DEBUG) System.out.println("DD_Address: save_V3: peer="+peer+" \nDD="+this);
			return;
		}
		String description=null;
		description = getNiceDescription();
		if (!peer.verifySignature()) {
			if (_DEBUG) System.out.println("DDAddress:save: verification failed: "+this);
			int c = Application_GUI.ask(__("Wrong signature for:\n"+description+"\nDo you still want it?"), __("Wrong signature"), 
					Application_GUI.OK_CANCEL_OPTION);
			if (c > 0) {
				Application_GUI.warning(__("Saving cancelled!"), __("Cancelled!"));
				return;
			} else Application_GUI.warning(__("You decided that the address will be saved anyhow!"), __("Saved Anyhow!"));
		}
		int ok = Application_GUI.ask(
				__("Obtained Data:")+"\n"+description+"\n"+__("Save and Trust"),
				__("Save and Trust Obtained Address?"),
				Application_GUI.OK_CANCEL_OPTION);
		if (ok != 0) {
			if (DEBUG) System.out.println("DD_Address: save_V3: abandonDD = "+ok);
			return;
		}
		D_Peer.storeReceived(peer, true, true, null);
	}
	public void save_V2() throws P2PDDSQLException {
		if (address == null) {
			if (DEBUG) System.out.println("DDAddress:save: nothing to save ");
			Application_GUI.warning(__("No address to save!"), __("Saving failed!"));
			return;
		}
		if (DEBUG) System.out.println("DDAddress:save: will verify: "+this);
		if (!D_Peer.verify(this)) {
			if (_DEBUG) System.out.println("DDAddress:save: verification failed: "+this);
			int c = Application_GUI.ask(__("Wrong signature for:\n"+this.getNiceDescription()+"\nDo you still want it?"), __("Wrong signature"), 
					Application_GUI.OK_CANCEL_OPTION);
			if (c > 0) {
				Application_GUI.warning(__("Saving cancelled!"), __("Cancelled!"));
				return;
			} else Application_GUI.warning(__("Address Saved Anyhow!"), __("Saved Anyhow!"));
		}
		if (DEBUG) System.out.println("DDAddress:save: will save");
		Calendar _date = Util.CalendargetInstance();
		String date = Encoder.getGeneralizedTime(_date);
		String adr[] = address.split(Pattern.quote(DirectoryServer.ADDR_SEP));
		if (DEBUG) System.out.println("DDAddress:save: will save address: ["+adr.length+"] "+address);
		ArrayList<Address> _a = new ArrayList<Address>(adr.length);
		for (int k = 0; k < adr.length; k ++) {
			if (DEBUG) System.out.println("DDAddress:save: will save address: "+adr[k]);
			String pr[] = adr[k].split(Pattern.quote(Address.PRI_SEP));
			String[] ds = pr[0].split(Pattern.quote(Address.ADDR_PART_SEP));
			if (DEBUG) System.out.println("DDAddress:save: address parts: "+ds.length);
			if (ds.length < 3) continue; 
			String type = ds[0];
			String target = ds[1]+Address.ADDR_PART_SEP+ds[2];
			if (ds.length > 3) target += Address.ADDR_PART_SEP + ds[3];
			boolean certificate = false;
			int priority = 0;
			if (pr.length > 1) {
				certificate = true;
				priority=Util.get_int(pr[1]);
			}
			Address a = new Address();
			a.setAddress(target);
			a.pure_protocol = type;
			a.address = target;
			a.certified = certificate;
			a.priority = priority;
			a.set_version_structure(Address.V0);
			if (DEBUG) System.out.println("DDAddress:save: address "+a.toLongString()+" --> "+a);
		}
		String description=null;
		description = getNiceDescription();
		int ok = Application_GUI.ask(
				__("Obtained Data:")+"\n"+description+"\n"+__("Save"),
				__("Save Obtained Address?"),
				Application_GUI.OK_CANCEL_OPTION);
		if (ok != 0) return;
		if (DEBUG) System.out.println("DDAddress:save: will save "+this);
		D_Peer saved = D_Peer.loadPeer(this, true);
		if (DEBUG) Application_GUI.warning(__("Address Saved as #")+saved.getLIDstr_keep_force()+"! ", __("Saved!"));
	}
	String sane(String in) {
		if(in==null) return "";
		String out = in.replaceAll("#", "##");
		return out.replaceAll(SEP, SEPREP);
	}
	String clean(String in) {
		if ("".equals(in)) return null;
		String out = in.replaceAll(SEPREP, SEP);
		return out.replaceAll("##", "#");
	}
	public boolean parseAddress(String adr) {
		boolean DEBUG = true;
		int  k=0, t=0;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA1:"+adr);
		adr = adr.trim();
		if (!adr.startsWith(BEGIN)) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA2");
		if (!adr.endsWith(END)) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA3");
		String elem[] = adr.split(Pattern.quote(SEP));
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA4:elems="+elem.length);
		if ((elem.length != DD_TAGS) && (elem.length != DD_TAGS-2)) {
			System.out.println("DDAddress:parseAddress:pA4_2:elems="+elem.length+"\nadr="+adr);
			return false;
		}
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA5");
		if (!elem[++k].startsWith(VERSION.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(NAME.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA6");
		if(V2.equals(peer_version)) {
			if(DEBUG)System.out.println("DDAddress:parseAddress:pA6a_");
			if (!elem[++k].startsWith(EMAILS.substring(SEP.length()))) return false;
			if(DEBUG)System.out.println("DDAddress:parseAddress:pA6a");
			if (!elem[++k].startsWith(PHONES.substring(SEP.length()))) return false;
			if(DEBUG)System.out.println("DDAddress:parseAddress:pA6b");
		}
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA6b_");
		if (!elem[++k].startsWith(SLOGAN.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA7");
		if (!elem[++k].startsWith(DATE.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(ADDRESS.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA8");
		if (!elem[++k].startsWith(BROADCAST.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA9");
		if (!elem[++k].startsWith(HASH_ALG.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA10");
		if (!elem[++k].startsWith(SERVING.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(SIGNATURE.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA12");
		globalID = clean(elem[t++].substring(BEGIN.length()));
		peer_version = clean(elem[t++].substring(VERSION.length()-SEP.length()));
		name = clean(elem[t++].substring(NAME.length()-SEP.length()));
		if(V2.equals(peer_version)){
			emails = clean(elem[t++].substring(EMAILS.length()-SEP.length()));
			phones = clean(elem[t++].substring(PHONES.length()-SEP.length()));
		}
		slogan = clean(elem[t++].substring(SLOGAN.length()-SEP.length()));
		creation_date = clean(elem[t++].substring(DATE.length()-SEP.length()));
		address = clean(elem[t++].substring(ADDRESS.length()-SEP.length()));
		broadcastable = Integer.parseInt(clean(elem[t++].substring(BROADCAST.length()-SEP.length())))>0;
		String s_hash_alg = clean(elem[t++].substring(HASH_ALG.length()-SEP.length()));
		if (s_hash_alg == null) hash_alg = new String[]{};
		else {
			String[]q_hash_alg = s_hash_alg.split(Pattern.quote("\""));
			if (q_hash_alg.length < 2) hash_alg = null;
			else hash_alg = (q_hash_alg[1]==null)?(new String[]{}):q_hash_alg[1].split(Pattern.quote(":"));
		}
		served_orgs = Util.parsePeerOrgs(elem[t++].substring(SERVING.length()-SEP.length()));
		String s_signature = clean(elem[t++].substring(SIGNATURE.length()-SEP.length()));
		signature = (s_signature==null)?(new byte[]{}):(Util.byteSignatureFromString(s_signature));
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA3");
		return true;
	}
	public byte[] getBytes() {
		return getEncoder().getBytes(); 
	}
	public Encoder getEncoder() {
		Encoder enc = _getEncoder();
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}
	public Encoder _getEncoder() {
		if(V1.equals(version)) return _getBytes_V1();
		if(V2.equals(version)) return _getBytes_V2();
		if(V3.equals(version)) return _getBytes_V3(); 
		throw new RuntimeException("Unknown DDAddress version:"+peer_version);
	}
	public Encoder _getBytes_V3() {
		Encoder enc = new Encoder().initSequence();
		if (version != null) enc.addToSequence(new Encoder(version,false).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(peer.getEncoder());
		ArrayList<D_PeerInstance> dpi = new ArrayList<D_PeerInstance>( peer._instances.values());
		enc.addToSequence(Encoder.getEncoder(dpi));
		return enc;
	}
	public Encoder _getBytes_V1() {
		Encoder enc = new Encoder().initSequence();
		if(version!=null) enc.addToSequence(new Encoder(version,false).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(globalID,false));
		if(creation_date!=null)enc.addToSequence(new Encoder(Util.getCalendar(creation_date)));
		if(name==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		if(slogan==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(slogan,Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(address,false));
		enc.addToSequence(new Encoder(broadcastable));
		enc.addToSequence(Encoder.getStringEncoder(hash_alg, Encoder.TAG_PrintableString));
		if(this.served_orgs!=null)enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(signature).setASN1Type(Encoder.TAG_OCTET_STRING));
		return enc;
	}
	public Encoder _getBytes_V2() {
		Encoder enc = new Encoder().initSequence();
		if(version!=null)enc.addToSequence(new Encoder(version,false).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(globalID,false));
		if(creation_date!=null)enc.addToSequence(new Encoder(Util.getCalendar(creation_date)));
		if(name==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		if(V2.equals(peer_version)){
			if(emails==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
			else enc.addToSequence(new Encoder(emails,Encoder.TAG_UTF8String));			
			if(phones==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
			else enc.addToSequence(new Encoder(phones,Encoder.TAG_UTF8String));			
		}
		if(slogan==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(slogan,Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(address,false));
		enc.addToSequence(new Encoder(broadcastable));
		enc.addToSequence(Encoder.getStringEncoder(hash_alg, Encoder.TAG_PrintableString));
		if(this.served_orgs!=null)enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(signature).setASN1Type(Encoder.TAG_OCTET_STRING));
		return enc;
	}
	/**
	 * Attempts to decode may profit by detecting a miss-matched object type by not accepting data with no name.
	 */
	@Override
	public void setBytes(byte[] msg) throws ASN1DecoderFail {
		_setBytes(msg);
		if (V0.equals(this.version)) return;
		if (V1.equals(this.version)) return;
		if (V2.equals(this.version)) return;
		if (this.peer == null || this.peer.getName() == null) {
			if (_DEBUG) System.out.println("DD_Address: setBytes: we do not allow peers with no name:"+peer);
			throw new net.ddp2p.ASN1.ASNLenRuntimeException("No name in received peer!");
		}
	}
	/**
	 * This version always returns true. Could be configured to return false on wrong ASN1 tag by uncommenting return condition.
	 * @param msg
	 * @return
	 * @throws ASN1DecoderFail
	 */
	public boolean _setBytes(byte[] msg) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("DD_Address: setBytes: enter");
		if (_DEBUG) System.out.println("DD_Address: setBytes: enter msg=#"+msg.length+": "+Util.byteToHexDump(msg, 30));
		Decoder dec = new Decoder(msg);
		BigInteger expected = new BigInteger(""+this.getSignShort());
		BigInteger _found = dec.getTagValueBN();
		if (! expected.equals(_found)) {
			if (_DEBUG) System.err.println("DD_Address: setBytes: Got: message not ASN1 tag of ="+this.getClass()+" "+expected+" vs "+_found);
		}
		dec = dec.getContent();
		peer_version = null;
		if (dec.getTypeByte() == DD.TAG_AC0) version = peer_version = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if (V0.equals(version)) {
			if (DEBUG) System.out.println("DD_Address: setBytes: V0");
			setBytes_V2(dec); return true;}
		if (V1.equals(version)) {
			if (DEBUG) System.out.println("DD_Address: setBytes: V1");
			setBytes_V2(dec); return true;}
		if (V2.equals(version)) {
			if (DEBUG) System.out.println("DD_Address: setBytes: V2");
			setBytes_V2(dec); 
			if (DEBUG) System.out.println("DD_Address: setBytes: done V2. I am=:"+this);
			return true;}
		if (V3.equals(version)) {
			if (DEBUG) System.out.println("DD_Address: setBytes: V3");
			setBytes_V3(dec); 
			return true;}
		if (DEBUG) System.out.println("DD_Address: setBytes: exit: \""+version+"\"");
		return true;
	}
	public void setBytes_V3(Decoder dec) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("DD_Address: setBytes_V3: enter");
		peer = D_Peer.getEmpty().decode(dec.getFirstObject(true));
		ArrayList<D_PeerInstance> dpi = 
				dec.getFirstObject(true).getSequenceOfAL(D_PeerInstance.getASN1Type(), new D_PeerInstance());
		for (D_PeerInstance i : dpi) {
			peer.putPeerInstance(i.peer_instance, i, false);
		}
		if (DEBUG) System.out.println("DD_Address: setBytes_V3: exit 0:"+peer);
		if (DEBUG) System.out.println("DD_Address: setBytes_V3: exit 1:"+this.getNiceDescription());
		this.init_V2(peer);
		if (DEBUG) System.out.println("DD_Address: setBytes_V3: exit 2:"+this.getNiceDescription());
	}
	public void setBytes_V2(Decoder dec) throws ASN1DecoderFail {
		globalID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if (dec.getTypeByte()==Encoder.TAG_GeneralizedTime)
			creation_date = dec.getFirstObject(true).getGeneralizedTimeAnyType();
		name = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if("".equals(name)) name = null;
		if(V2.equals(peer_version)) {
			emails = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
			if("".equals(emails)) emails = null;
			phones = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
			if("".equals(phones)) phones = null;			
		}
		slogan = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if ("".equals(slogan)) slogan = null;
		address = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		broadcastable = dec.getFirstObject(true).getBoolean();
		hash_alg = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte() == DD.TAG_AC12)
			served_orgs = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_PeerOrgs[]{}, new D_PeerOrgs());
		signature = dec.getFirstObject(true).getBytes();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects");
	}
	public void setDDAddress(DD_Address d) {
		address = d.address;
		this.version = d.version;
		this.peer_version = d.peer_version;
		this.creation_date = d.creation_date;
		this.broadcastable = d.broadcastable;
		this.globalID = d.globalID;
		this.hash_alg = d.hash_alg;
		this.name = d.name;
		this.emails = d.emails;
		this.phones = d.phones;
		this.signature = d.signature;
		this.slogan = d.slogan;
		this.served_orgs = d.served_orgs;
	}
	@Override
	public short getSignShort() {
		return DD.STEGO_SIGN_PEER;
	}
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.STEGO_SIGN_PEER+"");
	}
	public String getSlogan_MyOrDefault() {
		if (this.peer != null) return peer.getSlogan_MyOrDefault();
		return slogan;
	}
	public String getName() {
		if (this.peer != null) return peer.getName();
		return name;
	}
}

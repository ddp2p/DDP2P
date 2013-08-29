/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
 package hds;

import static util.Util._;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import ciphersuits.PK;
import ciphersuits.SK;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_PeerAddress;
import data.D_PeerOrgs;

import streaming.UpdateMessages;
import streaming.UpdatePeersTable;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

public class DDAddress implements StegoStructure{
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int MAX_ID_DESCRIPTION = 50;
	public static final String V0 = "0";
	public static final String V1 = "1";
	public static final String V2 = "2";
	public String version=V2;
	public String globalID;
	public String name;
	public String emails;
	public String phones;
	public String slogan;
	public String address;
	//public String type;
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
//	public static String TYPE = "\n<<TYPE>>\n";
	public static String DATE = "\n<<DATE>>\n";
	public static String BROADCAST = "\n<<BROADCAST>>\n";
	public static String HASH_ALG  = "\n<<HASH_ALG>>\n";
	public static String SERVING = "\n<<SERVING>>\n";
	public static String SIGNATURE = "\n<<SIGNATURE>>\n";
	private static final int DD_TAGS = 11;
	public static String SEPREP = "\n#\n";
	public static String SEP = "\n<<";
	public DDAddress(){
		hash_alg = SR.HASH_ALG_V1;//new String[0];//SR.peerAddressHashAlg;
		signature = new byte[0];
	}
	public DDAddress(D_PeerAddress dd) {
		boolean encode_addresses = true;
		version = dd.version;
		globalID = dd.globalID;
		name = dd.name;
		slogan = dd.slogan;
		emails = dd.emails;
		phones = dd.phones;
		creation_date = Encoder.getGeneralizedTime(dd.creation_date);
		picture = dd.picture;
		if(encode_addresses && (dd.address!=null)){
			address = "";
			for(int k=0; k<dd.address.length; k++) {
				if(!"".equals(address)) address += DirectoryServer.ADDR_SEP;
				if(DD.EXPORT_DDADDRESS_WITH_LOCALHOST) {
					if(dd.address[k].address.startsWith("localhost:")) continue;
					if(dd.address[k].address.startsWith("127.0.0.1:")) continue;
				}
				address += dd.address[k].type+Address.ADDR_PART_SEP+dd.address[k].address;
				if(dd.address[k].certified) address += TypedAddress.PRI_SEP+dd.address[k].priority;
			}
		}
		broadcastable = dd.broadcastable;
		hash_alg = dd.signature_alg;
		//signature_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		signature = dd.signature;
		served_orgs = dd.served_orgs;
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
	@Deprecated
	public void sign(SK sk) {
		signature = new byte[0];
		D_PeerAddress pa = new D_PeerAddress(this,true);
		//byte[] msg = getBytes();
		//if(DEBUG)System.err.println("DDAddress: sign: msg=["+msg.length+"]"+Util.byteToHex(msg, ":"));
		if(DEBUG)System.err.println("DDAddress: sign: peer_addr="+pa);
		signature = pa.sign(sk); //Util.sign(pa,  sk);
		if(DEBUG)System.err.println("DDAddress: sign: sign=["+signature.length+"]"+Util.byteToHex(signature, ":"));
	}
	public boolean verify() {
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("DDAddress: verify: start ");
		if((signature==null) || (signature.length==0)){
			if(_DEBUG)System.err.println("DDAddress: verify: exit: empty signature ");
			return false;
		}
		//byte[] sign = signature;
		//signature = new byte[0];
		D_PeerAddress pa;
		if(V2.equals(version))
			pa = new D_PeerAddress(this,true);
		else //V0
			pa = new D_PeerAddress(this,false);			
		//byte[] msg = getBytes();
		//if(DEBUG)System.err.println("DDAddress: verify: msg=["+msg.length+"]"+Util.byteToHex(msg, ":"));
		//signature = sign;
		if(DEBUG)System.err.println("DDAddress: verify: will get PK from "+globalID);
		//PK senderPK = ciphersuits.Cipher.getPK(this.globalID);
		if(DEBUG)System.err.println("DDAddress: verify: will verify signature of pa="+pa);
		if(DEBUG)System.err.println("DDAddress: verify: sign=["+signature.length+"]"+Util.byteToHex(signature, ":"));
		//return Util.verifySign(pa, senderPK, sign);
		boolean result = pa.verifySignature();
		if(DEBUG)System.err.println("DDAddress: verify: got = " + result);
		return result;
	}
	public String getString() {
		String result=BEGIN+sane(globalID);
		result+=VERSION+sane(version);
		result+=NAME+sane(name);
		if(V2.equals(version)) {
			result+=EMAILS+sane(emails);
			result+=PHONES+sane(phones);
		}
		result+=SLOGAN+sane(slogan);
		result+=DATE+sane(creation_date);
		result+=ADDRESS+sane(address);
		//result+=TYPE+sane(type);
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
		//boolean DEBUG=true;
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: start ");
		String result=BEGIN+sane(Util.trimmed(globalID,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: GID");
		result+=VERSION+sane(Util.trimmed(version,MAX_ID_DESCRIPTION));
		result+=NAME+sane(Util.trimmed(name,MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: name");
		if(V2.equals(version)) {
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
		//result+=TYPE+sane(type);
		result+=BROADCAST+sane(broadcastable?"1":"0");
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: broadcastable");
		result+=HASH_ALG+((hash_alg==null)?"NULL":"\""+sane(Util.trimmed(Util.concat(hash_alg,":"),MAX_ID_DESCRIPTION))+"\"");
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: alg");
		result+=SERVING+sane(Util.trimmed(Util.concatPeerOrgsTrimmed(served_orgs)));
		result+=SIGNATURE+sane(Util.trimmed(Util.byteToHex(signature,0,Math.min(MAX_ID_DESCRIPTION/2,signature.length),":"),MAX_ID_DESCRIPTION));
		if(DEBUG)System.err.println("DDAddress: getNiceDescription: end ");
		return result+END;
	}
	
	public void save() throws P2PDDSQLException {
		// boolean DEBUG = true;
		if(address == null){
			if(DEBUG) System.out.println("DDAddress:save: nothing to save ");
			Application.warning(_("No address to save!"), _("Saving failed!"));
			return;
		}
		if(DEBUG) System.out.println("DDAddress:save: will verify");
		if(!verify()) {
			if(_DEBUG) System.out.println("DDAddress:save: verification failed: "+this);
			int c=Application.ask(_("Wrong signature for:\n"+this.getNiceDescription()+"\nDo you still want it?"), _("Wrong signature"), 
					JOptionPane.OK_CANCEL_OPTION);
			if(c>0){
				Application.warning(_("Saving cancelled!"), _("Cancelled!"));
				return;
			}else Application.warning(_("Address Saved Anyhow!"), _("Saved Anyhow!"));
		}
		if(DEBUG) System.out.println("DDAddress:save: will save");
		String date = Util.getGeneralizedTime();
		long peer_ID = D_PeerAddress.storeVerified(globalID, name, emails, phones, date, slogan, true, broadcastable, Util.concat(hash_alg,":"),
				signature, this.creation_date, this.picture, this.version, served_orgs);
		//UpdatePeersTable.integratePeerOrgs(pa.served_orgs, peer_ID, crt_date);
		String adr[] = address.split(Pattern.quote(DirectoryServer.ADDR_SEP));
		if(DEBUG) System.out.println("DDAddress:save: will save address: ["+adr.length+"] "+address);
		for(int k=0; k<adr.length; k++) {
			if(DEBUG) System.out.println("DDAddress:save: will save address: "+adr[k]);
			String pr[] = adr[k].split(Pattern.quote(TypedAddress.PRI_SEP));
			String[] ds = pr[0].split(Pattern.quote(Address.ADDR_PART_SEP));
			if(DEBUG) System.out.println("DDAddress:save: address parts: "+ds.length);
			if(ds.length < 3) continue; //TypedAddress
			String type = ds[0];
			String target = ds[1]+Address.ADDR_PART_SEP+ds[2];
			if(ds.length>3) target += Address.ADDR_PART_SEP+ds[3];
			//long address_ID = 
			boolean certificate = false;
			int priority = 0;
			if(pr.length>1){
				certificate = true;
				priority=Util.get_int(pr[1]);
			}
			D_PeerAddress.get_peer_addresses_ID(target, type, peer_ID, date, certificate, priority);
			//long peers_orgs_ID = Client.get_peers_orgs_ID(peer_ID, global_organizationID);
			//long organizationID = Client.get_organizationID (global_organizationID, org_name);
		}
		//D_PeerAddress.integratePeerOrgs(served_orgs, peer_ID, date);
//		if(this.served_orgs!=null)
//			for(int k=0; k<this.served_orgs.length; k++) {
//				long org_ID = UpdateMessages.get_organizationID(served_orgs[k].global_organization_ID, served_orgs[k].org_name, date, served_orgs[k].global_organization_IDhash);
//				D_PeerAddress.get_peers_orgs_ID(peer_ID,org_ID, date);
//			}
		if(DEBUG) Application.warning(_("Address Saved as #"+peer_ID+"!"), _("Saved!"));
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
	public boolean parseAddress(String adr){
		int  k=0, t=0;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA1:"+adr);
		adr = adr.trim();
		if (!adr.startsWith(BEGIN)) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA2");
		if (!adr.endsWith(END)) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA3");
		String elem[] = adr.split(Pattern.quote(SEP));
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA4:elems="+elem.length);
		if (elem.length!=DD_TAGS) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA5");
		if (!elem[++k].startsWith(VERSION.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(NAME.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA6");
		if(V2.equals(version)) {
			if (!elem[++k].startsWith(EMAILS.substring(SEP.length()))) return false;
			if(DEBUG)System.out.println("DDAddress:parseAddress:pA6a");
			if (!elem[++k].startsWith(PHONES.substring(SEP.length()))) return false;
			if(DEBUG)System.out.println("DDAddress:parseAddress:pA6b");
		}
		if (!elem[++k].startsWith(SLOGAN.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA7");
		if (!elem[++k].startsWith(DATE.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(ADDRESS.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA8");
		//if (!elem[4].startsWith(TYPE.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(BROADCAST.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA9");
		if (!elem[++k].startsWith(HASH_ALG.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA10");
		if (!elem[++k].startsWith(SERVING.substring(SEP.length()))) return false;
		if (!elem[++k].startsWith(SIGNATURE.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA12");
		globalID = clean(elem[t++].substring(BEGIN.length()));
		version = clean(elem[t++].substring(VERSION.length()-SEP.length()));
		name = clean(elem[t++].substring(NAME.length()-SEP.length()));
		if(V2.equals(version)){
			emails = clean(elem[t++].substring(EMAILS.length()-SEP.length()));
			phones = clean(elem[t++].substring(PHONES.length()-SEP.length()));
		}
		slogan = clean(elem[t++].substring(SLOGAN.length()-SEP.length()));
		creation_date = clean(elem[t++].substring(DATE.length()-SEP.length()));
		address = clean(elem[t++].substring(ADDRESS.length()-SEP.length()));
		//type = clean(elem[4].substring(TYPE.length()-SEP.length()));
		broadcastable = Integer.parseInt(clean(elem[t++].substring(BROADCAST.length()-SEP.length())))>0;
		String s_hash_alg = clean(elem[t++].substring(HASH_ALG.length()-SEP.length()));
		if(s_hash_alg==null) hash_alg=new String[]{};
		else {
			String[]q_hash_alg = s_hash_alg.split(Pattern.quote("\""));
			if(q_hash_alg.length<2) hash_alg = null;
			else hash_alg = (q_hash_alg[1]==null)?(new String[]{}):q_hash_alg[1].split(Pattern.quote(":"));
		}
		served_orgs = Util.parsePeerOrgs(elem[t++].substring(SERVING.length()-SEP.length()));
		String s_signature = clean(elem[t++].substring(SIGNATURE.length()-SEP.length()));
		//signature = (s_signature==null)?(new byte[]{}):(Util.hexToBytes(s_signature.split(":")));
		signature = (s_signature==null)?(new byte[]{}):(Util.byteSignatureFromString(s_signature));
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA3");
		return true;
	}
	public byte[] getBytes() {
		Encoder enc = new Encoder().initSequence();
		if(version!=null)
			enc.addToSequence(new Encoder(version,false).setASN1Type(DD.TAG_AC0));
		enc.addToSequence(new Encoder(globalID,false));
		if(creation_date!=null)enc.addToSequence(new Encoder(Util.getCalendar(creation_date)));
		
		if(name==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		
		if(V2.equals(version)){
			if(emails==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
			else enc.addToSequence(new Encoder(emails,Encoder.TAG_UTF8String));			
			if(phones==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
			else enc.addToSequence(new Encoder(phones,Encoder.TAG_UTF8String));			
		}
		
		if(slogan==null) enc.addToSequence(new Encoder("",Encoder.TAG_UTF8String));
		else enc.addToSequence(new Encoder(slogan,Encoder.TAG_UTF8String));
		
		enc.addToSequence(new Encoder(address,false));
		//enc.addToSequence(new Encoder(type,false));
		enc.addToSequence(new Encoder(broadcastable));
		enc.addToSequence(Encoder.getStringEncoder(hash_alg, Encoder.TAG_PrintableString));
		if(this.served_orgs!=null)enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(signature).setASN1Type(Encoder.TAG_OCTET_STRING));
		return enc.getBytes();
	}
	public void setBytes(byte[] msg) throws ASN1DecoderFail{
		Decoder dec = new Decoder(msg);
		dec=dec.getContent();
		version = null;
		if(dec.getTypeByte()==DD.TAG_AC0)
			version = dec.getFirstObject(true).getString();
		//if(V0.equals(version)){
		globalID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime)
			creation_date = dec.getFirstObject(true).getGeneralizedTimeAnyType();
		name = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if("".equals(name)) name = null;
		if(V2.equals(version)) {
			emails = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
			if("".equals(emails)) emails = null;
			phones = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
			if("".equals(phones)) phones = null;			
		}
		slogan = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if("".equals(slogan)) slogan = null;
		address = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		//type = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		broadcastable = dec.getFirstObject(true).getBoolean();
		hash_alg = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte() == DD.TAG_AC12)
			served_orgs = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_PeerOrgs[]{}, new D_PeerOrgs());
		signature = dec.getFirstObject(true).getBytes();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects");
	}
	
	public void setDDAddress(DDAddress d) {
		address = d.address;
		this.version = d.version;
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
}

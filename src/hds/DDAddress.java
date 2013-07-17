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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import util.BMP;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

public class DDAddress {
	//public final static int STEGO_PIX_HEADER=12;
	public final static short STEGO_BYTE_HEADER=48;//STEGO_PIX_HEADER*3;
	public static final int STEGO_BITS = 4;
	public static final short STEGO_SIGN = 0x0D0D;
	public static final int STEGO_LEN_OFFSET = 8;
	public static final int STEGO_SIGN_OFFSET = 0;
	public static final int STEGO_OFF_OFFSET = 4;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int MAX_ID_DESCRIPTION = 50;
	public static final String V0 = "0";
	public static final String V1 = "1";
	public String version=V0;
	public String globalID;
	public String name;
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
			}
		}
		broadcastable = dd.broadcastable;
		hash_alg = dd.signature_alg;
		//signature_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		signature = dd.signature;
		served_orgs = dd.served_orgs;
	}
	public void sign_DDAddress(SK sk) {
		signature = new byte[0];
		byte[] msg = getBytes();
		if(DEBUG)System.err.println("DDAddress: sign: msg=["+msg.length+"]"+Util.byteToHex(msg, ":"));
		signature = Util.sign(msg,  sk);
		if(DEBUG)System.err.println("DDAddress: sign: sign=["+signature.length+"]"+Util.byteToHex(signature, ":"));
	}
	public void sign(SK sk) {
		signature = new byte[0];
		D_PeerAddress pa = new D_PeerAddress(this,false);
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
		D_PeerAddress pa = new D_PeerAddress(this,false);
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
	
	public static int getWidth(int size, int bits, int Bpp, int height){
		return (int)Math.round(Math.ceil(getSteganoSize(size,bits)/(Bpp*height*1.0)));
	}
	public static int getSteganoSize(int size, int bits){
		return (int)Math.round(Math.ceil(size*8.0/bits));
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
		long peer_ID = D_PeerAddress.storeVerified(globalID, name, date, slogan, true, broadcastable, Util.concat(hash_alg,":"),
				signature, this.creation_date, this.picture, this.version, served_orgs);
		//UpdatePeersTable.integratePeerOrgs(pa.served_orgs, peer_ID, crt_date);
		String adr[] = address.split(Pattern.quote(DirectoryServer.ADDR_SEP));
		if(DEBUG) System.out.println("DDAddress:save: will save address: ["+adr.length+"] "+address);
		for(int k=0; k<adr.length; k++) {
			if(DEBUG) System.out.println("DDAddress:save: will save address: "+adr[k]);
			String[] ds = adr[k].split(Pattern.quote(Address.ADDR_PART_SEP));
			if(DEBUG) System.out.println("DDAddress:save: address parts: "+ds.length);
			if(ds.length < 3) continue; //TypedAddress
			String type = ds[0];
			String target = ds[1]+Address.ADDR_PART_SEP+ds[2];
			if(ds.length>3) target += Address.ADDR_PART_SEP+ds[3];
			//long address_ID = 
			D_PeerAddress.get_peer_addresses_ID(target, type, peer_ID, date);
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
		if (!elem[1].startsWith(VERSION.substring(SEP.length()))) return false;
		if (!elem[2].startsWith(NAME.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA6");
		if (!elem[3].startsWith(SLOGAN.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA7");
		if (!elem[4].startsWith(DATE.substring(SEP.length()))) return false;
		if (!elem[5].startsWith(ADDRESS.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA8");
		//if (!elem[4].startsWith(TYPE.substring(SEP.length()))) return false;
		if (!elem[6].startsWith(BROADCAST.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA9");
		if (!elem[7].startsWith(HASH_ALG.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA10");
		if (!elem[8].startsWith(SERVING.substring(SEP.length()))) return false;
		if (!elem[9].startsWith(SIGNATURE.substring(SEP.length()))) return false;
		if(DEBUG)System.out.println("DDAddress:parseAddress:pA12");
		globalID = clean(elem[0].substring(BEGIN.length()));
		version = clean(elem[1].substring(VERSION.length()-SEP.length()));
		name = clean(elem[2].substring(NAME.length()-SEP.length()));
		slogan = clean(elem[3].substring(SLOGAN.length()-SEP.length()));
		creation_date = clean(elem[4].substring(DATE.length()-SEP.length()));
		address = clean(elem[5].substring(ADDRESS.length()-SEP.length()));
		//type = clean(elem[4].substring(TYPE.length()-SEP.length()));
		broadcastable = Integer.parseInt(clean(elem[6].substring(BROADCAST.length()-SEP.length())))>0;
		String s_hash_alg = clean(elem[7].substring(HASH_ALG.length()-SEP.length()));
		if(s_hash_alg==null) hash_alg=new String[]{};
		else {
			String[]q_hash_alg = s_hash_alg.split(Pattern.quote("\""));
			if(q_hash_alg.length<2) hash_alg = null;
			else hash_alg = (q_hash_alg[1]==null)?(new String[]{}):q_hash_alg[1].split(Pattern.quote(":"));
		}
		served_orgs = Util.parsePeerOrgs(elem[8].substring(SERVING.length()-SEP.length()));
		String s_signature = clean(elem[9].substring(SIGNATURE.length()-SEP.length()));
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
	
	public static DDAddress setSteganoImage(BufferedImage bi) throws ASN1DecoderFail, P2PDDSQLException{
		byte[] sign= Util.getBytes(bi,DDAddress.STEGO_SIGN_OFFSET,
				Util.ceil(2*8/DDAddress.STEGO_BITS));
		System.out.println("Got image sign bytes: "+Util.byteToHex(sign, " "));
		/*
		System.out.println("Got image type: "+bi.getType()+" ==?"
				+BufferedImage.TYPE_INT_RGB+" "
				+BufferedImage.TYPE_3BYTE_BGR+" "
				+BufferedImage.TYPE_4BYTE_ABGR+" "
				+BufferedImage.TYPE_4BYTE_ABGR_PRE+" "
				+BufferedImage.TYPE_INT_ARGB+" "
				+BufferedImage.TYPE_INT_BGR+" "
				+BufferedImage.TYPE_USHORT_555_RGB+" "
				+BufferedImage.TYPE_BYTE_BINARY+" "
				+BufferedImage.TYPE_BYTE_GRAY+" "
				+BufferedImage.TYPE_BYTE_INDEXED+" "
				+BufferedImage.TYPE_CUSTOM+" "
				+BufferedImage.TYPE_INT_ARGB_PRE+" "//
				+BufferedImage.TYPE_USHORT_555_RGB+" "
				+BufferedImage.TYPE_USHORT_565_RGB+" "
				+BufferedImage.TYPE_USHORT_GRAY
				);
				*/
		byte[] signature = DDAddress.extractSteganoBytes(sign, 0, 1,
				DDAddress.STEGO_BITS, 2);
		short signature_val = 0;
		signature_val=Util.extBytes(signature, 0, signature_val);
		if(signature_val!=DDAddress.STEGO_SIGN){
			JOptionPane.showMessageDialog(JFrameDropCatch.mframe,
        			_("When trying to use locally saved image got Wrong Signature: "+signature_val+
        					"\nThe source of the drag might have changed the image content (like Safari/use Firefox!). " +
        					"\n"+_("We will try other methods")+
        					"\nYou can also save the file and drag/load it as a file."),
        			_("Wrong signature"), JOptionPane.WARNING_MESSAGE);
			return null;
		}
		
		byte[] off= Util.getBytes(bi,DDAddress.STEGO_OFF_OFFSET,
				Util.ceil(2*8/DDAddress.STEGO_BITS));
		byte[] offset = DDAddress.extractSteganoBytes(off, 0, 1,
				DDAddress.STEGO_BITS, 2);
		short offset_val = 0;
		offset_val=Util.extBytes(offset, 0, offset_val);
		if(offset_val!=DDAddress.STEGO_BYTE_HEADER){
			int n = JOptionPane.showConfirmDialog(JFrameDropCatch.mframe, _("Accept code: ")+offset_val+"!="+DDAddress.STEGO_BYTE_HEADER,
					_("Accept old file?"), JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if(n!=JOptionPane.YES_OPTION)
				return null;
		}
		
		byte[] len= Util.getBytes(bi,DDAddress.STEGO_LEN_OFFSET,
				Util.ceil(4*8/DDAddress.STEGO_BITS));
		byte[] length = DDAddress.extractSteganoBytes(len, 0, 1,
				DDAddress.STEGO_BITS, 4);
		int bytes_len = 0;
		bytes_len=Util.extBytes(length, 0, bytes_len);
		
		//System.err.println("Imglen:"+Util.byteToHex(len, " ")+" l="+bytes_len);
		int stegoBytes = Util.ceil((bytes_len*8)/DDAddress.STEGO_BITS);
		byte[] useful= Util.getBytes(bi, offset_val, stegoBytes);
		System.err.println("StegData:"+Util.byteToHex(useful, " "));
		byte datab[] = DDAddress.extractSteganoBytes(useful, 0, 1, DDAddress.STEGO_BITS, bytes_len);
		DDAddress data = new DDAddress();
		data.setBytes(datab);
		System.out.println(data.toString());
		data.save();

		return data;
	}
	
	
	/**
	 *  Extract all stegano bytes (potentially more than encoded)
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @return
	 */
	public byte[] extractSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits){
		int bytes_len = (int)Math.round(Math.ceil(bits*(buffer.length - offset)/(word_bytes*8.0)));
		return extractSteganoBytes(buffer, offset, word_bytes, bits, bytes_len);
	}
	/**
	 *  Convert stegano bytes in pure bytes
	 * @param buffer
	 * @param offset
	 * @param word_bytes : bytes/color (amount to jump before next byte)
	 * @param bits : how many bits are used per word
	 * @param bytes_len : how many bytes to extract
	 * @return newly created buffer
	 */
	public static byte[] extractSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits, int bytes_len){
		byte[]result = new byte[bytes_len];
		int crt_src=offset;
		int crt_dst=0;
		int carry = 0;
		int carry_bits=0;
		for(;crt_dst<result.length;crt_dst++) {
			do{
				if(crt_src >= buffer.length) break;
				byte b = (byte) (buffer[crt_src]&((1<<bits) - 1));
				//carry = b|(carry<<bits);
				carry = carry | (b<<carry_bits);
				carry_bits+=bits;
				crt_src += word_bytes;
			}while(carry_bits<8);
			if(carry_bits == 0) break;
			result[crt_dst]= (byte)(carry & 0x0ff);
			carry_bits -= 8;
			carry = (carry>>8) & ((1<<carry_bits) - 1);
		}
		if(DEBUG) System.out.println("Extracted: "+Util.byteToHex(result, " "));
		return result;
	}
	/**
	 * Init DDAddres from BMP data
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @throws ASN1DecoderFail
	 */
	public void setSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits) throws ASN1DecoderFail{
		byte[] sign=extractSteganoBytes(buffer, offset+STEGO_SIGN_OFFSET, word_bytes, bits, 2);
		short _sign=0;
		_sign = Util.extBytes(sign, 0, _sign);
		if(_sign!=STEGO_SIGN) throw new ASN1DecoderFail("Wrong SIGNATURE!");

		byte[] len=extractSteganoBytes(buffer, offset+STEGO_LEN_OFFSET, word_bytes, bits, 4);
		int bytes_len=0;
		bytes_len = Util.extBytes(len, 0, bytes_len);

		byte[] off=extractSteganoBytes(buffer, offset+STEGO_OFF_OFFSET, word_bytes, bits, 4);
		short _off=0;
		_off = Util.extBytes(off, 0, _off);

		if(_off != STEGO_BYTE_HEADER) {
    		int n = JOptionPane.showConfirmDialog(null, _("Accept code: ")+_off+"!="+STEGO_BYTE_HEADER,
        			_("Accept old file?"), JOptionPane.YES_NO_OPTION,
        			JOptionPane.QUESTION_MESSAGE);
    		if(n!=JOptionPane.YES_OPTION)
        		return;
		}
		
		setSteganoBytes(buffer, offset+_off*word_bytes, word_bytes, bits, bytes_len);
		//setBytes(extractSteganoBytes(buffer, offset, word_bytes, bits));
	}
	
	/**
	 * Init DDAddress from a buffer of stegano bytes of known useful length and offset
	 * @param buffer
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @param bytes_len
	 * @throws ASN1DecoderFail
	 */
	public void setSteganoBytes(byte[]buffer, int offset, int word_bytes, int bits, int bytes_len) throws ASN1DecoderFail{
		setBytes(extractSteganoBytes(buffer, offset, word_bytes, bits, bytes_len));
	}
	
	
	public byte[] getSteganoBytes(int offset, int word_bytes, int bits) {
		byte[] ddb = getBytes();
		byte[] stg = new byte[offset+(int)Math.ceil(ddb.length/(double)bits)*word_bytes];
		return getSteganoBytes(ddb, stg, offset, word_bytes, bits);
	}
	public byte[] getSteganoBytes(byte[] stg, int offset, int word_bytes, int bits) {
		byte[] ddb = getBytes();
		return getSteganoBytes(ddb, stg, offset, word_bytes, bits);
	}
	public static byte[] getSteganoBytes(byte[] ddb, byte[] stg,
			int offset, int word_bytes, int bits) {
		byte[] len = new byte[4];
		Util.copyBytes(len, 0, ddb.length);
		getSteganoBytesRaw(len, stg, offset+STEGO_LEN_OFFSET, word_bytes, bits);
		byte[] sign = new byte[2];
		Util.copyBytes(sign, 0, STEGO_SIGN);
		getSteganoBytesRaw(sign, stg, offset+STEGO_SIGN_OFFSET, word_bytes, bits);
		Util.copyBytes(sign, 0, STEGO_BYTE_HEADER);
		getSteganoBytesRaw(sign, stg, offset+STEGO_OFF_OFFSET, word_bytes, bits);
		return getSteganoBytesRaw(ddb, stg, offset+STEGO_BYTE_HEADER*word_bytes, word_bytes, bits);
	}
	/**
	 *  Fills buffer with bytes from result
	 * @param ddb input full bytes data
	 * @param stg destination steganodata buffer 
	 * @param offset
	 * @param word_bytes
	 * @param bits
	 * @return filled stg
	 */
	public static byte[] getSteganoBytesRaw(byte[] ddb, byte[] stg,
			int offset, int word_bytes, int bits) {
		int crt_src=0;
		int crt_bit=0;
		int crt_dst=offset;
		for(;;crt_dst+=word_bytes) {
			if(crt_src >= ddb.length) return stg;
			if(crt_dst >= stg.length) return stg;
			int available_bits = Math.min(8-crt_bit, bits);
			//System.out.println(crt_src+" CRT src: "+Util.getHEX(ddb[crt_src])+" crt="+crt_bit+" av="+available_bits);
			byte b1 = (byte) ((ddb[crt_src]>>crt_bit));
			byte b2 = (byte) (((1<<available_bits) - 1));
			byte b = (byte) ((ddb[crt_src]>>crt_bit) & ((1<<available_bits) - 1));
			//System.out.println(" CRT b: "+Util.getHEX(b)+" CRT b1: "+Util.getHEX(b1)+" CRT b2: "+Util.getHEX(b2));
			crt_bit += available_bits;
			if(crt_bit >=8) {crt_src++; crt_bit=0;}
			if(available_bits<bits) {
				//crt_src++;
				if(crt_src >= ddb.length) return stg;
				//System.out.println(crt_src+" + CRT src: "+Util.getHEX(ddb[crt_src]));
				crt_bit=bits-available_bits;
				b |= (byte) ( (ddb[crt_src] & ((1<<crt_bit) - 1)) << available_bits );
				//System.out.println(" + CRT b: "+Util.getHEX(ddb[crt_src]));
			}
			//System.out.print(crt_dst+" Old: "+Util.getHEX(stg[crt_dst])+" <- "+Util.getHEX(b));
			stg[crt_dst] &= (byte) ~((1<<bits) - 1);
			stg[crt_dst] |= b;
			//System.out.println(" New: "+Util.getHEX(stg[crt_dst]));
		}
	}
	public boolean fromBMPFileSave(File file) throws IOException, P2PDDSQLException{
		String explain="";
		boolean fail= false;
		FileInputStream fis=new FileInputStream(file);
		byte[] b = new byte[(int) file.length()];
		fis.read(b);
		fis.close();
		BMP data = new BMP(b, 0);

		if((data.compression!=BMP.BI_RGB) || (data.bpp<24)){
			explain = " - "+_("Not supported compression: "+data.compression+" "+data.bpp);
			fail = true;
		}else{
			int offset = data.startdata;
			int word_bytes=1;
			int bits = 4;
			try {
				setSteganoBytes(b, offset, word_bytes, bits);
			} catch (ASN1DecoderFail e1) {
				explain = " - "+ _("No valid data in picture!");
				fail = true;
			}
		}
		if(fail) throw new IOException(explain);
		save();
		return true;
	}
	public boolean fromBMPStreamSave(InputStream in) throws IOException, ASN1DecoderFail, P2PDDSQLException {
		int k;
		short sign_val=0, off_val=0;
		byte[] bmp= new byte[BMP.DATA];
		//boolean DEBUG=true;
		if(DEBUG) System.err.println("fromBMPStreamSave: will read header");
		k=Util.readAll(in, bmp);
		if(k<BMP.DATA) throw new IOException("EOF BMP Header");
		BMP bmpheader = new BMP(bmp,0);
		int startdata = bmpheader.startdata;
		byte useless[] = new byte[startdata-BMP.DATA];
		if(useless.length>0) {k=Util.readAll(in, useless); if(k<useless.length) throw new IOException("EOF useless Header");}
		
		byte[] stg = new byte[4];
		byte[] sign;
		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF sign Header");
		if(DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=DDAddress.extractSteganoBytes(stg, 0, 1, DDAddress.STEGO_BITS,2);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got mac signature: "+k+"..."+Util.byteToHex(sign, " "));
		sign_val = Util.extBytes(sign, 0, sign_val);
		if(sign_val!=DDAddress.STEGO_SIGN) throw new IOException("BAD sign Header");

		k=Util.readAll(in, stg);if(k<stg.length) throw new IOException("EOF off Header");
		if(DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=DDAddress.extractSteganoBytes(stg, 0, 1, DDAddress.STEGO_BITS,2);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got mac offset: "+k+"..."+Util.byteToHex(sign, " "));
		off_val = Util.extBytes(sign, 0, off_val);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got offset: "+off_val);
		
		int length_val=0;
		stg = new byte[8];
		//byte[] sign;
		k=Util.readAll(in, stg); if(k<stg.length) throw new IOException("EOF length Header: "+k);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got stg: "+k+"..."+Util.byteToHex(stg, " "));
		sign=DDAddress.extractSteganoBytes(stg, 0, 1, DDAddress.STEGO_BITS, 4);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"..."+Util.byteToHex(sign, " "));
		length_val = Util.extBytes(sign, 0, length_val);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got length: "+length_val);
		
		byte skipped[]=new byte[off_val-16];
		k=Util.readAll(in, skipped);if(k<skipped.length) throw new IOException("EOF skipped Header");
		if(DEBUG) System.out.println("fromBMPStreamSave: Got skipped: "+skipped.length);
		
		stg=new byte[Util.ceil(length_val*8.0/DDAddress.STEGO_BITS)];
		if(DEBUG) System.out.println("fromBMPStreamSave: Will read bmp: "+stg.length);
		//k=in.read(stg);
		k=Util.readAll(in,stg);
		if(DEBUG) System.out.println("fromBMPStreamSave: Got bmp: "+k);
		if(k<stg.length){
			if(DEBUG) System.out.println("fromBMPStreamSave: Got data length: "+k+"<"+stg.length);
			throw new IOException("EOF data");
		}
		sign=DDAddress.extractSteganoBytes(stg, 0, 1, DDAddress.STEGO_BITS, length_val);
		//DDAddress data = new DDAddress();
		//data.
		setBytes(sign);
		//System.out.println("Got DDAddress: "+data);
		//data.
		if(DEBUG) System.out.println("fromBMPStreamSave: Got bytes ");
		if(DEBUG) System.out.println("fromBMPStreamSave: Got address: "+this);
		save();
		if(DEBUG) System.out.println("fromBMPStreamSave: Done");
		
		return true;
	}
	public void setDDAddress(DDAddress d) {
		address = d.address;
		this.version = d.version;
		this.creation_date = d.creation_date;
		this.broadcastable = d.broadcastable;
		this.globalID = d.globalID;
		this.hash_alg = d.hash_alg;
		this.name = d.name;
		this.signature = d.signature;
		this.slogan = d.slogan;
	}
}

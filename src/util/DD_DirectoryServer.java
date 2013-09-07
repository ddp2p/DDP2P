package util;

import static util.Util._;
import hds.StegoStructure;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.regex.Pattern;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
import config.Identity;
class Dir extends ASNObj{
	String version = "0";
	String domain;
	int port;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(domain));
		enc.addToSequence(new Encoder(port));
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	@Override
	public Dir decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		domain = d.getFirstObject(true).getString();
		port = d.getFirstObject(true).getInteger().intValue();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new Dir();}
	public String toString(){
		return domain+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+port;
	}
	public static byte getASN1Type() {
		return DD.TAG_AC17;
	}
}
class SaverThread extends Thread {
	DD_DirectoryServer ds;
	SaverThread(DD_DirectoryServer ds){
		this.ds = ds;
	}
	public void run(){
		try {
			_run();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void _run() throws P2PDDSQLException{
    	String ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
    	if(ld != null) ld = ld.trim();
    	else ld = "";
    	String old_dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	//String new_dir = domain+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+port;
    	for(Dir d : ds.dirs) {
    		String new_dir = d.toString();
			if(Util.contains(old_dirs, new_dir)<0) {
	    		ld = new_dir+DD.APP_LISTING_DIRECTORIES_SEP+ld;
	    		Identity.listing_directories_string.add(new_dir);
	    		try{
	    			Identity.listing_directories_inet.add(new InetSocketAddress(InetAddress.getByName(d.domain),d.port));
	    		}catch(Exception e) {
	    			Application.warning(_("Error for "+d+"\nError: "+e.getMessage()), _("Error installing directories"));
	    		}
	    	}
    	}
    	DD.setAppText(DD.APP_LISTING_DIRECTORIES, ld);
	}
}

public class DD_DirectoryServer extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	int version = 0;
	ArrayList<Dir> dirs = new ArrayList<Dir>();
	
	@Override
	public void save() {
		if(DEBUG) System.out.println("DD_DirServ:save");
		new SaverThread(this).start();
    	if(true){
    		Application.warning(_("Adding directories:")+" \n"+this.toString(), _("Import"));
    		//return;
    	}
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_DirectoryServer:decode "+asn1.length);
		decode(new Decoder(asn1));
		if(DEBUG)System.out.println("DD_DirectoryServer:decoded");
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return this.toString();
	}

	@Override
	public String getString() {
		return "DD_DirectoryServer:\n"+Util.concat(dirs, DD.APP_LISTING_DIRECTORIES_SEP,"");
	}

	public void add(String domain, int port){
		Dir d = new Dir();
		d.domain = domain;
		d.port = port;
		dirs.add(d);
		if(DEBUG) System.out.println("DD_DirServ:added:"+d);
	}
	/**
	 * return a directory string
	 * compatible with what is stored in application table
	 */
	@Override
	public String toString() {
		return Util.concat(dirs, DD.APP_LISTING_DIRECTORIES_SEP,null);
	}

	/**
	 *  add the directories from a string (as one from "application" table)
	 */
	@Override
	public boolean parseAddress(String content) {
		if(content==null) return false;
    	String old_dirs[] = content.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	for(String l : old_dirs){
    		String[] d = l.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    		if(d.length >= 2) {
    			try{
    				this.add(d[0], Integer.parseInt(d[1]));
    			}catch(Exception e){
    				e.printStackTrace();
    			}
    		}
    	}
		if(DEBUG) System.out.println("DD_DirServ:parsed:"+this.getString());
		return true;

	}

	@Override
	public short getSignShort() {
		if(DEBUG) System.out.println("DD_DirServ:get_sign=:"+DD.STEGO_SIGN_DIRECTORY_SERVER);
		return DD.STEGO_SIGN_DIRECTORY_SERVER;
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(Encoder.getEncoder(dirs));
		return enc;
	}

	@Override
	public DD_DirectoryServer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		dirs = d.getFirstObject(true).getSequenceOfAL(Dir.getASN1Type(), new Dir());
		return this;
	}
	
}
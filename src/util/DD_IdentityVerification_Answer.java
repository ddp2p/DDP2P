package util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import config.Application;
import config.DD;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import data.D_Constituent;
import data.D_PeerAddress;
import data.D_Witness;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import hds.EmbedInMedia;
import hds.StegoStructure;
import static util.Util._;

public class DD_IdentityVerification_Answer extends ASNObj implements StegoStructure, DD_EmailableAttachment{
	public static final byte IDENTITY_VERIFICATION = DD.TYPE_DD_IDENTITY_VERIFICATION_ANSWER;
	private static final boolean DEBUG = false;
	public static final int R_PRIM_SIZE = 300;
	static String V0 = "0";
	String version = V0;
	String d; // timestamp
	BigInteger r; // random challenge
	D_Constituent verified;
	D_PeerAddress verifier; // my addresses
	public BigInteger r_prim;
	private byte[] signature;
	
	public String toString() {
		return "DD_IdentityVerification_Answer:\n"
				+"Version: "+version+"\n"
				+"Date: "+d+"\n"
					+"Verified: "+((verified!=null)?verified.getName():null)+"\n"
					+"Verified: GID="+((verified!=null)?verified.global_constituent_id_hash:null)+"\n"
					+"Verifier: "+((verifier!=null)?verifier.name:null)+"\n"
					+"Verifier: email="+((verifier!=null)?verifier.emails:null)+"\n"
				+"Signature: "+Util.byteToHexDump(signature, 20)+"\n"
				+"R: "+this.r+"\n"
				+"R_Prim: "+this.r_prim+"\n"
				;
	}

	public DD_IdentityVerification_Answer(
			DD_IdentityVerification_Request req) {
		d = req.d;
		r = req.r;
		this.verified = req.verified;
		this.verifier = req.verifier;
	}

	public DD_IdentityVerification_Answer() {
	}
	@Override
	public void save() throws P2PDDSQLException {
		long constituent_id = -1;
		if(this.verified.constituent_ID == null) {
			this.verified.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(
					verified.global_constituent_id,
					verified.global_constituent_id_hash, null);
		}
		String _my_r = null;
		if(this.verified.constituent_ID != null) {
			constituent_id = Util.lval(this.verified.constituent_ID);
			_my_r = table.constituent_verification.getChallenge(constituent_id);
		}
		//DD.getAppText("VI:"+this.verified.global_constituent_id_hash);
		BigInteger my_r = null;
		if(_my_r!=null)
			my_r= new BigInteger(_my_r);
		if((_my_r==null) || !my_r.equals(this.r)){
			Application.warning(_("We have not generated this request:")+"\n"+this, _("Constituent Verification"));
			return;
		}
		
		boolean v=false;
		if(v=verifySign()) {
			Application.warning(_("Successfull verification of:")+this, _("Constituent Verification"));
		}else{
			Application.warning(_("Failed verification of:")+this, _("Constituent Verification"));
		}
		// start witness dialog
		D_Constituent c = new D_Constituent(this.verified.global_constituent_id,
				this.verified.global_constituent_id, D_Constituent.EXPAND_NONE);
		D_Witness.witness(c);
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification_A:decode");
		try{
			decode(new Decoder(asn1));
		}catch(Exception e){
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("DD_IdentityVerification_A:decoded");
	}

	@Override
	public byte[] getBytes() {
		return encode();
	}

	@Override
	public String getNiceDescription() {
		return toString();
	}

	@Override
	public String getString() {
		return toString();
	}
	/**
	 * Not yet supported
	 */
	@Override
	public boolean parseAddress(String content) {
		return false;
	}

	@Override
	public short getSignShort() {
		if(DEBUG)System.out.println("DD_IdentityVerification_Answer: short");
		return DD.STEGO_SIGN_CONSTITUENT_VERIF_ANSWER;
	}

	public byte getASN1Type() {
		return IDENTITY_VERIFICATION;
	}

	@Override
	public Encoder getEncoder() {
		if(DEBUG)System.out.println("DD_IV_Answer: getEncoder: here="+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if(verified==null){
			Application.warning(_("Unknown constituent to verify!"), _("Verifying Constituent"));
			verified = new D_Constituent();
			Util.printCallPath("Unknown constituent!");
		}
		enc.addToSequence(verified.getEncoder());
		enc.addToSequence(new Encoder(r));
		enc.addToSequence(new Encoder(d));
		enc.addToSequence(new Encoder(r_prim));
		enc.addToSequence(new Encoder(signature));
		if(verifier != null) enc.addToSequence(verifier.getEncoder());
		enc.setASN1Type(getASN1Type());
		return enc;
	}

	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode");
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: version = "+version);
		verified = new D_Constituent().decode(d.getFirstObject(true));
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: verified decoded");
		r = d.getFirstObject(true).getInteger();
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: r="+r);
		this.d = d.getFirstObject(true).getString();
		r_prim = d.getFirstObject(true).getInteger();
		signature = d.getFirstObject(true).getBytes();
		byte type_verifier = d.getFirstObject(false).getTypeByte();
		if(type_verifier == D_PeerAddress.getASN1Type()) {
			verifier = new D_PeerAddress().decode(d.getFirstObject(true));
			if(DEBUG)System.out.println("DD_IdentityVerification_A: decoded verifier");
		}else{
			if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: type="+type_verifier+
					" vs "+D_PeerAddress.getASN1Type());			
		}
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decoded done");
		return this;
	}

	public void generateSignature(SK sk) {
		if(DEBUG)System.out.println("DD_IV_Answer: Will sign:"+this);
		D_PeerAddress _me = this.verifier;
		this.verifier = null;
		this.signature = new byte[0];

		if(DEBUG)System.out.println("DD_IV_Answer: Will sign:"+this);
		byte[] digest = this.encode();
		signature = Util.sign(digest, sk);
		if(DEBUG)System.out.println("pk="+sk+"\nmsg="+Util.getGID_as_Hash(digest)+"\nsgn="+Util.getGID_as_Hash(signature));

		this.verifier = _me;
		if(DEBUG)System.out.println("DD_IV_Answer: Signed="+this);
	}

	boolean verifySign(){
		byte[] _signature = signature;
		D_PeerAddress _me = this.verifier;
		this.verifier = null;
		signature = new byte[0];
		
		byte[] digest = this.encode();
		PK pk = Cipher.getPK(this.verified.global_constituent_id);
		if(DEBUG)System.out.println("pk="+pk+"\nmsg="+Util.getGID_as_Hash(digest)+"\nsgn="+Util.getGID_as_Hash(signature));
		boolean val = Util.verifySign(digest, pk, _signature);

		
		signature = _signature;
		this.verifier = _me;
		return val;
	}

	@Override
	public String get_To() {
		if((verifier==null)||(verifier.emails==null)){
			Application.warning(_("No emails set in peer!"), _("Destination email absent"));
			return Application.input(_("Can you enter the destination email?"), _("Destination"),
					JOptionPane.QUESTION_MESSAGE);
			// return "unknown@unknown";
		}
		String my_email = verifier.emails;
		if(my_email!=null) my_email = my_email.split(Pattern.quote(","))[0].trim();
		return  verifier.name +"<"+my_email+">";
	}

	@Override
	public String get_From() {
		if((verified==null)||(verified.email==null)){
			Application.warning(_("No emails set in this constituent!"), _("Your email absent"));
			return Application.input(_("Can you enter the sender email?"), _("Sender"),
					JOptionPane.QUESTION_MESSAGE);
			//return "unknown@unknown";
		}
		return verified.email;
	}

	@Override
	public String get_Subject() {
		return _("DirectDemocracyP2P:Re:")+Util.trimmed(verified.forename, 10)+" "+Util.trimmed(verified.surname, 10)+" -- " +_("Identity verification");
	}

	@Override
	public String get_FileName() {
		return _("DirectDemocracyP2P_Verif_Reply_from_")+
				((verifier!=null)?Util.sanitizeFileName(verifier.name):null)+".bmp";
	}

	@Override
	public byte[] get_ByteContent() {
		byte[] ddb = encode();
		
		if(DEBUG) System.out.println("DD_IV_A:get_ByteContent: "+ddb.length);
		DD_IdentityVerification_Answer ov = new DD_IdentityVerification_Answer();
		try {
			ov.decode(new Decoder(ddb));
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("DD_IV_A:get_ByteContent: Got: "+ov);
		//if(true) return;
		
        return EmbedInMedia.createSteganoBMP(ddb, this.getSignShort());
	}

	@Override
	public String get_Greetings() {
		String g = _("Dear")+" "+verifier.name+"\r\n"+
				_("I answer to verification of this constituent.")+"\r\n"+
				_("Name:")+" \""+verified.getName()+"\"\r\n"+
				_("GID:")+" \""+verified.global_constituent_id_hash+"\"\r\n"+
				_("R:")+" \""+r+"\"\r\n"+
				_("Please drag the attached image to your agent, to finalize the test.")+"\r\n"+
			"\r\n\t"+_("Sincerely,")+" "+verified.getName();
	return g;
	}
	public static void main(String args[]){
		try {
			if(args.length == 0) {
				System.out.println("prog database id fix verbose");
				return;
			}
			
			String database = Application.DELIBERATION_FILE;
			if(args.length>0) database = args[0];
			Application.db = new DBInterface(database);

			//util.DD_IdentityVerification_Request r = new DD_IdentityVerification_Request();
			util.DD_IdentityVerification_Answer a = new DD_IdentityVerification_Answer();
			a.d = Util.getGeneralizedTime();
			a.r = new BigInteger(300, new SecureRandom());
			a.r_prim = new BigInteger(300, new SecureRandom());
			a.verifier = new D_PeerAddress(1);
			a.verified = new D_Constituent(2);
			a.generateSignature(Util.getStoredSK(a.verified.global_constituent_id));
			System.out.println("a="+a);
			byte[] msg = a.encode();
			util.DD_IdentityVerification_Answer b = new DD_IdentityVerification_Answer();
			b.decode(new Decoder(msg));
			System.out.println("b="+b);
			
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

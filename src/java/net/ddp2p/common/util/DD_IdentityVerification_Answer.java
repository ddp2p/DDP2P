package net.ddp2p.common.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Witness;
import static net.ddp2p.common.util.Util.__;

public class DD_IdentityVerification_Answer extends ASNObj implements StegoStructure, DD_EmailableAttachment{
	//public static final byte IDENTITY_VERIFICATION = DD.TYPE_DD_IDENTITY_VERIFICATION_ANSWER;
	private static final boolean DEBUG = false;
	public static final int R_PRIM_SIZE = 300;
	static String V0 = "0";
	String version = V0;
	String d; // timestamp
	BigInteger r; // random challenge
	D_Constituent verified;
	D_Peer verifier; // my addresses
	public BigInteger r_prim;
	private byte[] signature;
	
	public String toString() {
		return "DD_IdentityVerification_Answer:\n"
				+"Version: "+version+"\n"
				+"Date: "+d+"\n"
					+"Verified: "+((verified!=null)?verified.getNameFull():null)+"\n"
					+"Verified: GID="+((verified!=null)?verified.getGIDH():null)+"\n"
					+"Verifier: "+((verifier!=null)?verifier.component_basic_data.name:null)+"\n"
					+"Verifier: email="+((verifier!=null)?verifier.component_basic_data.emails:null)+"\n"
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
	public void saveSync() throws P2PDDSQLException {
		save();
	}
	@Override
	public void save() throws P2PDDSQLException {
		long constituent_id = -1;
		if(this.verified.getLIDstr() == null) {
			this.verified.setConstituentLID(D_Constituent.getLIDFromGID_or_GIDH(
					verified.getGID(),
					verified.getGIDH(),
					verified.getOrganizationLID()));
		}
		String _my_r = null;
		if(this.verified.getLIDstr() != null) {
			constituent_id = this.verified.getLID();
			_my_r = net.ddp2p.common.table.constituent_verification.getChallenge(constituent_id);
		}
		//DD.getAppText("VI:"+this.verified.global_constituent_id_hash);
		BigInteger my_r = null;
		if(_my_r!=null)
			my_r= new BigInteger(_my_r);
		if((_my_r==null) || !my_r.equals(this.r)){
			Application_GUI.warning(__("We have not generated this request:")+"\n"+this, __("Constituent Verification"));
			return;
		}
		
		boolean v = false;
		if (v = verifySign()) {
			Application_GUI.warning(__("Successfull verification of:")+this, __("Constituent Verification"));
		}else{
			Application_GUI.warning(__("Failed verification of:")+this, __("Constituent Verification"));
		}
		// start witness dialog
		Long oID = this.verified.getOrganizationLID();
		if ( oID <= 0 ) oID = D_Organization.getLIDbyGID(verified.getGID());
		D_Constituent c = 
				D_Constituent.getConstByGID_or_GIDH(this.verified.getGID(), null, true, false, oID);
				//new D_Constituent(this.verified.global_constituent_id,
				//this.verified.global_constituent_id, D_Constituent.EXPAND_NONE);
		D_Witness.witness(c);
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification_A:decode");
		try{
			decode(new Decoder(asn1));
		}
		catch(net.ddp2p.ASN1.ASN1DecoderFail | net.ddp2p.ASN1.ASNLenRuntimeException e) {
			throw e;
		} catch(Exception e) {
			//if (DEBUG) 
				e.printStackTrace();
			throw e;
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
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.STEGO_SIGN_CONSTITUENT_VERIF_ANSWER+"");
	}

//	public byte getASN1Type() {
//		return IDENTITY_VERIFICATION;
//	}

	@Override
	public Encoder getEncoder() {
		if(DEBUG)System.out.println("DD_IV_Answer: getEncoder: here="+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if (verified == null){
			Application_GUI.warning(__("Unknown constituent to verify!"), __("Verifying Constituent"));
			verified = D_Constituent.getEmpty();
			Util.printCallPath("Unknown constituent!");
		}
		enc.addToSequence(verified.getEncoder());
		enc.addToSequence(new Encoder(r));
		enc.addToSequence(new Encoder(d));
		enc.addToSequence(new Encoder(r_prim));
		enc.addToSequence(new Encoder(signature));
		if(verifier != null) enc.addToSequence(verifier.getEncoder());
		//enc.setASN1Type(getASN1Type());
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}

	@Override
	public DD_IdentityVerification_Answer decode(Decoder dec) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode");
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: version = "+version);
		verified = D_Constituent.getEmpty().decode(d.getFirstObject(true));
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: verified decoded");
		r = d.getFirstObject(true).getInteger();
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: r="+r);
		this.d = d.getFirstObject(true).getString();
		r_prim = d.getFirstObject(true).getInteger();
		signature = d.getFirstObject(true).getBytes();
		byte type_verifier = d.getFirstObject(false).getTypeByte();
		if(type_verifier == D_Peer.getASN1Type()) {
			verifier = D_Peer.getEmpty().decode(d.getFirstObject(true));
			if(DEBUG)System.out.println("DD_IdentityVerification_A: decoded verifier");
		}else{
			if(DEBUG)System.out.println("DD_IdentityVerification_A: decode: type="+type_verifier+
					" vs "+D_Peer.getASN1Type());			
		}
		if(DEBUG)System.out.println("DD_IdentityVerification_A: decoded done");
		return this;
	}

	public void generateSignature(SK sk) {
		if(DEBUG)System.out.println("DD_IV_Answer: Will sign:"+this);
		D_Peer _me = this.verifier;
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
		D_Peer _me = this.verifier;
		this.verifier = null;
		signature = new byte[0];
		
		byte[] digest = this.encode();
		PK pk = Cipher.getPK(this.verified.getGID());
		if(DEBUG)System.out.println("pk="+pk+"\nmsg="+Util.getGID_as_Hash(digest)+"\nsgn="+Util.getGID_as_Hash(signature));
		boolean val = Util.verifySign(digest, pk, _signature);

		
		signature = _signature;
		this.verifier = _me;
		return val;
	}

	@Override
	public String get_To() {
		if((verifier==null)||(verifier.component_basic_data.emails==null)){
			Application_GUI.warning(__("No emails set in peer!"), __("Destination email absent"));
			return Application_GUI.input(__("Can you enter the destination email?"), __("Destination"),
					Application_GUI.QUESTION_MESSAGE);
			// return "unknown@unknown";
		}
		String my_email = verifier.component_basic_data.emails;
		if(my_email!=null) my_email = my_email.split(Pattern.quote(","))[0].trim();
		return  verifier.component_basic_data.name +"<"+my_email+">";
	}

	@Override
	public String get_From() {
		if((verified==null)||(verified.getEmail()==null)){
			Application_GUI.warning(__("No emails set in this constituent!"), __("Your email absent"));
			return Application_GUI.input(__("Can you enter the sender email?"), __("Sender"),
					Application_GUI.QUESTION_MESSAGE);
			//return "unknown@unknown";
		}
		return verified.getEmail();
	}

	@Override
	public String get_Subject() {
		return __("DirectDemocracyP2P:Re:")+Util.trimmed(verified.getForename(), 10)+" "+Util.trimmed(verified.getSurname(), 10)+" -- " +__("Identity verification");
	}

	@Override
	public String get_FileName() {
		return __("DirectDemocracyP2P_Verif_Reply_from_")+
				((verifier!=null)?Util.sanitizeFileName(verifier.component_basic_data.name):null)+".bmp";
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
		String g = __("Dear")+" "+verifier.component_basic_data.name+"\r\n"+
				__("I answer to verification of this constituent.")+"\r\n"+
				__("Name:")+" \""+verified.getNameFull()+"\"\r\n"+
				__("GID:")+" \""+verified.getGIDH()+"\"\r\n"+
				__("R:")+" \""+r+"\"\r\n"+
				__("Please drag the attached image to your agent, to finalize the test.")+"\r\n"+
			"\r\n\t"+__("Sincerely,")+" "+verified.getNameFull();
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
			Application.setDB(new DBInterface(database));

			//util.DD_IdentityVerification_Request r = new DD_IdentityVerification_Request();
			net.ddp2p.common.util.DD_IdentityVerification_Answer a = new DD_IdentityVerification_Answer();
			a.d = Util.getGeneralizedTime();
			a.r = new BigInteger(300, new SecureRandom());
			a.r_prim = new BigInteger(300, new SecureRandom());
			a.verifier = D_Peer.getPeerByLID_NoKeep(1, true);
			a.verified = D_Constituent.getConstByLID(new Long(2), true, false);
			a.generateSignature(Util.getStoredSK(a.verified.getGID()));
			
			System.out.println("a="+a);
			byte[] msg = a.encode();
			net.ddp2p.common.util.DD_IdentityVerification_Answer b = new DD_IdentityVerification_Answer();
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

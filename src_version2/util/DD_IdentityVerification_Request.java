package util;

import static util.Util.__;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import ciphersuits.SK;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application_GUI;
import config.DD;
import data.D_Constituent;
import data.D_Organization;
import data.D_Peer;

public class DD_IdentityVerification_Request extends ASNObj implements StegoStructure, DD_EmailableAttachment {
	//public static final byte IDENTITY_VERIFICATION = DD.TYPE_DD_IDENTITY_VERIFICATION;
	private static final boolean DEBUG = false;
	public static final int R_SIZE = 300;
	//String C; // GID of verified person
	//String n; // declared name
	//String s; // my name (verifier)
	//TypedAddress[] a; // my addresses
	static String V0 = "0";
	String version = V0;
	public String d; // timestamp 
	public BigInteger r; // random challenge 

	//D_PeerAddress verified;
	public D_Constituent verified;
	public D_Peer verifier; // my addresses
	
	public String toString() {
		return "DD_IdentityVerification_Request:\n"
				+"Version: "+version+"\n"
					+"Date: "+d+"\n"
					+"Verified: "+((verified!=null)?verified.getNameFull():null)+"\n"
					+"Verified: GID="+((verified!=null)?verified.getGIDH():null)+"\n"
					+"Verifier: "+((verifier!=null)?verifier.component_basic_data.name:null)+"\n"
					+"Verifier: email="+((verifier!=null)?verifier.component_basic_data.emails:null)+"\n"
				;
	}

//	public byte getASN1Type() {
//		return IDENTITY_VERIFICATION;
//	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if(verified==null){
			Application_GUI.warning(__("Unknown constituent to verify!"), __("Verifying Constituent"));
			verified = D_Constituent.getEmpty();
			Util.printCallPath("Unknown constituent!");
		}
		enc.addToSequence(verified.getEncoder());
		enc.addToSequence(new Encoder(r));
		enc.addToSequence(new Encoder(d));
		if(verifier != null) enc.addToSequence(verifier.getEncoder());
		//enc.setASN1Type(getASN1Type());
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}

	@Override
	public DD_IdentityVerification_Request decode(Decoder dec) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification: decode");
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		if(DEBUG)System.out.println("DD_IdentityVerification: decode: version = "+version);
		verified = D_Constituent.getEmpty().decode(d.getFirstObject(true));
		r = d.getFirstObject(true).getInteger();
		if(DEBUG)System.out.println("DD_IdentityVerification: decode: r="+r);
		this.d = d.getFirstObject(true).getString();
		byte type_verifier = d.getFirstObject(false).getTypeByte();
		if(type_verifier == D_Peer.getASN1Type()) {
			verifier = D_Peer.getEmpty().decode(d.getFirstObject(true));
			if(DEBUG)System.out.println("DD_IdentityVerification: decoded verifier");
		}else{
			if(DEBUG)System.out.println("DD_IdentityVerification: decode: type="+type_verifier+
					" vs "+D_Peer.getASN1Type());			
		}
		if(DEBUG)System.out.println("DD_IdentityVerification: decoded done");
		return this;
	}
	public String getEmailSubject() {
		return __("DirectDemocracyP2P:")+Util.trimmed(verified.getForename(), 10)+" "+Util.trimmed(verified.getSurname(), 10)+" -- " +__("Identity verification");
	}


	@Override
	public void save() throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_IdentityVerification:save");
		//Application.warning(_("Got:")+this, _("Identity Verification"));
		SK sk = Util.getStoredSK(verified.getGID());
		if(sk == null){
			Application_GUI.warning(__("I do not have the secret key for:")+"\n"+this,
					__("Identity Verification"));
			//email it!
			return;
		}
		Long oID = this.verified.getOrganizationLID();
		if ( oID <= 0 ) oID = D_Organization.getLIDbyGID(verified.getGID());
		D_Constituent mine = D_Constituent.getConstByGID_or_GIDH(verified.getGID(), null, true, false, oID);
				//new D_Constituent(verified.global_constituent_id,
				//verified.global_constituent_id, D_Constituent.EXPAND_NONE);
		if(mine == null) {
			Application_GUI.warning(__("I do not have the constituent:")+"\n"+this,
					__("Identity Verification"));
			return;
		}
		if((!mine.getGID().equals(this.verified.getGID()))||
				(!mine.getGIDH().equals(this.verified.getGIDH()))) {
			Application_GUI.warning(__("Inconsistent Global Identifiers:")+
					"\n GIDs match: "+mine.getGID().equals(this.verified.getGID())+
				"\n GIDshash =: "+mine.getGIDH().equals(this.verified.getGIDH())+
					"\n"+this,
					__("Identity Verification"));
			return;
		}
		
		DD_IdentityVerification_Answer answer = new DD_IdentityVerification_Answer(this);
		if(DEBUG)System.out.println("Translated answer:"+answer);
		answer.r_prim = new BigInteger(DD_IdentityVerification_Answer.R_PRIM_SIZE, new SecureRandom());
		if(DEBUG)System.out.println("Will sign:"+answer);
		answer.generateSignature(sk);
		if(DEBUG)System.out.println("Will email answer:"+answer);
		//answer.verifySign();

		//byte msg[] = answer.encode();
		// could save it to file, but for now just email it

		String description=null;
		description = getNiceDescription();
		Application_GUI.info(
           			__("Obtained Verification Request:")+"\n"+description,
           			__("Handling Verification Request"));
		//EmailManager.
		Application_GUI.sendEmail(answer);		
	}

	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification:decode "+asn1.length);
		decode(new Decoder(asn1));
		if(DEBUG)System.out.println("DD_IdentityVerification:decoded");
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
		return this.toString();
	}

	/**
	 * Address parsing not yet supported for identity verification
	 */
	@Override
	public boolean parseAddress(String content) {
		return false;
	}

	@Override
	public short getSignShort() {
		if(DEBUG)System.out.println("DD_IdentityVerification: short");
		return DD.STEGO_SIGN_CONSTITUENT_VERIF_REQUEST;
	}
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.STEGO_SIGN_CONSTITUENT_VERIF_REQUEST+"");
	}

	@Override
	public String get_To() {
		if((verified==null)||(verified.getEmail()==null)){
			Application_GUI.warning(__("No emails set in this constituent!"), __("Destination email absent"));
			return Application_GUI.input(__("Can you enter the destination email?"), __("Destination"),
					Application_GUI.QUESTION_MESSAGE);
			//return "unknown@unknown";
		}
		return Application_GUI.input(__("Enter an email that you personally know for this claimed identity?")+
				"\n"+__("The claimed email is:")+" "+verified.getEmail(), __("Destination"),
				Application_GUI.QUESTION_MESSAGE);
		// return verified.email;
	}

	@Override
	public String get_From() {
		if((verifier==null)||(verifier.component_basic_data.emails==null)){
			Application_GUI.warning(__("No emails set in your peer!"), __("Sender email absent"));
			return Application_GUI.input(__("Can you enter the sender email?"), __("Sender"),
					Application_GUI.QUESTION_MESSAGE);
			// return "unknown@unknown";
		}
		String my_email = verifier.component_basic_data.emails;
		if(my_email!=null) my_email = my_email.split(Pattern.quote(","))[0].trim();
		return  verifier.component_basic_data.name +"<"+my_email+">";
	}

	@Override
	public String get_Subject() {
		return this.getEmailSubject();
	}

	@Override
	public String get_FileName() {
		return __("DirectDemocracyP2P_Verif_from_")+
				((verifier!=null)?Util.sanitizeFileName(verifier.component_basic_data.name):null)+".bmp";
	}

	@Override
	public byte[] get_ByteContent() {
        return EmbedInMedia.createSteganoBMP(encode(), this.getSignShort());
	}

	@Override
	public String get_Greetings() {
		String g = __("Dear")+" "+verified.getNameFull()+"\r\n"+
					__("I want to verify that this constituent data belongs to you.")+"\r\n"+
					__("Name:")+" \""+verified.getNameFull()+"\"\r\n"+
					__("GID:")+" \""+verified.getGIDH()+"\"\r\n"+
					__("R:")+" \""+r+"\"\r\n"+
					__("Please drag the attached image to your agent, to perfom the test.")+"\r\n"+
				"\r\n\t"+__("Sincerely,")+" "+verifier.component_basic_data.name;
		return g;
	}	
}
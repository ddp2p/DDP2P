package util;

import static util.Util._;
import hds.EmbedInMedia;
import hds.JFrameDropCatch;
import hds.StegoStructure;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
import data.D_Constituent;
import data.D_PeerAddress;

public class DD_IdentityVerification_Request extends ASNObj implements StegoStructure, DD_EmailableAttachment {
	public static final byte IDENTITY_VERIFICATION = DD.TYPE_DD_IDENTITY_VERIFICATION;
	private static final boolean DEBUG = false;
	public static final int R_SIZE = 300;
	//String C; // GID of verified person
	//String n; // declared name
	//String s; // my name (verifier)
	//TypedAddress[] a; // my addresses
	static String V0 = "0";
	String version = V0;
	String d; // timestamp
	BigInteger r; // random challenge

	//D_PeerAddress verified;
	D_Constituent verified;
	D_PeerAddress verifier; // my addresses
	
	public String toString() {
		return "DD_IdentityVerification_Request:\n"
				+"Version: "+version+"\n"
					+"Date: "+d+"\n"
					+"Verified: "+((verified!=null)?verified.getName():null)+"\n"
					+"Verified: GID="+((verified!=null)?verified.global_constituent_id_hash:null)+"\n"
					+"Verifier: "+((verifier!=null)?verifier.name:null)+"\n"
					+"Verifier: email="+((verifier!=null)?verifier.emails:null)+"\n"
				;
	}

	public byte getASN1Type() {
		return IDENTITY_VERIFICATION;
	}
	@Override
	public Encoder getEncoder() {
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
		if(verifier != null) enc.addToSequence(verifier.getEncoder());
		enc.setASN1Type(getASN1Type());
		return enc;
	}

	@Override
	public DD_IdentityVerification_Request decode(Decoder dec) throws ASN1DecoderFail {
		if(DEBUG)System.out.println("DD_IdentityVerification: decode");
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		if(DEBUG)System.out.println("DD_IdentityVerification: decode: version = "+version);
		verified = new D_Constituent().decode(d.getFirstObject(true));
		r = d.getFirstObject(true).getInteger();
		if(DEBUG)System.out.println("DD_IdentityVerification: decode: r="+r);
		this.d = d.getFirstObject(true).getString();
		byte type_verifier = d.getFirstObject(false).getTypeByte();
		if(type_verifier == D_PeerAddress.getASN1Type()) {
			verifier = new D_PeerAddress().decode(d.getFirstObject(true));
			if(DEBUG)System.out.println("DD_IdentityVerification: decoded verifier");
		}else{
			if(DEBUG)System.out.println("DD_IdentityVerification: decode: type="+type_verifier+
					" vs "+D_PeerAddress.getASN1Type());			
		}
		if(DEBUG)System.out.println("DD_IdentityVerification: decoded done");
		return this;
	}
	public String getEmailSubject() {
		return _("DirectDemocracyP2P:")+Util.trimmed(verified.forename, 10)+" "+Util.trimmed(verified.surname, 10)+" -- " +_("Identity verification");
	}


	@Override
	public void save() throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_IdentityVerification:save");
		//Application.warning(_("Got:")+this, _("Identity Verification"));
		SK sk = Util.getStoredSK(verified.global_constituent_id);
		if(sk == null){
			Application.warning(_("I do not have the secret key for:")+"\n"+this,
					_("Identity Verification"));
			//email it!
			return;
		}
		D_Constituent mine = new D_Constituent(verified.global_constituent_id,
				verified.global_constituent_id, D_Constituent.EXPAND_NONE);
		if(!mine.LocallyAvailable()){
			Application.warning(_("I do not have the constituent:")+"\n"+this,
					_("Identity Verification"));
			return;
		}
		if((!mine.global_constituent_id.equals(this.verified.global_constituent_id))||
				(!mine.global_constituent_id_hash.equals(this.verified.global_constituent_id_hash))) {
			Application.warning(_("Inconsistent Global Identifiers:")+
					"\n GIDs match: "+mine.global_constituent_id.equals(this.verified.global_constituent_id)+
				"\n GIDshash =: "+mine.global_constituent_id_hash.equals(this.verified.global_constituent_id_hash)+
					"\n"+this,
					_("Identity Verification"));
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
		JOptionPane.showMessageDialog(JFrameDropCatch.mframe,
           			_("Obtained Verification Request:")+"\n"+description,
           			_("Handling Verification Request"), JOptionPane.INFORMATION_MESSAGE);
		EmailManager.sendEmail(answer);		
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

	@Override
	public String get_To() {
		if((verified==null)||(verified.email==null)){
			Application.warning(_("No emails set in this constituent!"), _("Destination email absent"));
			return Application.input(_("Can you enter the destination email?"), _("Destination"),
					JOptionPane.QUESTION_MESSAGE);
			//return "unknown@unknown";
		}
		return Application.input(_("Enter an email that you personally know for this claimed identity?")+
				"\n"+_("The claimed email is:")+" "+verified.email, _("Destination"),
				JOptionPane.QUESTION_MESSAGE);
		// return verified.email;
	}

	@Override
	public String get_From() {
		if((verifier==null)||(verifier.emails==null)){
			Application.warning(_("No emails set in your peer!"), _("Sender email absent"));
			return Application.input(_("Can you enter the sender email?"), _("Sender"),
					JOptionPane.QUESTION_MESSAGE);
			// return "unknown@unknown";
		}
		String my_email = verifier.emails;
		if(my_email!=null) my_email = my_email.split(Pattern.quote(","))[0].trim();
		return  verifier.name +"<"+my_email+">";
	}

	@Override
	public String get_Subject() {
		return this.getEmailSubject();
	}

	@Override
	public String get_FileName() {
		return _("DirectDemocracyP2P_Verif_from_")+
				((verifier!=null)?Util.sanitizeFileName(verifier.name):null)+".bmp";
	}

	@Override
	public byte[] get_ByteContent() {
        return EmbedInMedia.createSteganoBMP(encode(), this.getSignShort());
	}

	@Override
	public String get_Greetings() {
		String g = _("Dear")+" "+verified.getName()+"\r\n"+
					_("I want to verify that this constituent data belongs to you.")+"\r\n"+
					_("Name:")+" \""+verified.getName()+"\"\r\n"+
					_("GID:")+" \""+verified.global_constituent_id_hash+"\"\r\n"+
					_("R:")+" \""+r+"\"\r\n"+
					_("Please drag the attached image to your agent, to perfom the test.")+"\r\n"+
				"\r\n\t"+_("Sincerely,")+" "+verifier.name;
		return g;
	}
	
}
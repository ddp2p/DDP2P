package ASN1;

import hds.ASNSyncRequest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import util.GetOpt;

public class Example {
	static class HSS_Elem extends ASNObj {
		String key;
		String value;
		@Override
		public Encoder getEncoder() {
			Encoder enc = new Encoder().initSequence();
			enc.addToSequence(new Encoder(key, false));
			if (value != null) enc.addToSequence(new Encoder(value, false));
			return enc;
		}

		@Override
		public Object decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			key = d.getFirstObject(true).getString();
			if (d.getTypeByte() == Encoder.TAG_PrintableString)
				value = d.getFirstObject(true).getString();
			return this;
		}
		@Override
		public HSS_Elem instance() {
			return new HSS_Elem();
		}
		
	}
	static class RequestData extends ASNObj {
		ArrayList<String> orgs; // AC15
		ArrayList<String> neig; // PrintableString
		ArrayList<HSS_Elem> cons; // [AC13]
		ArrayList<String> witn;
		ArrayList<String> moti;
		ArrayList<String> just;
		ArrayList<HSS_Elem> sign;
		ArrayList<String> tran;
		ArrayList<String> news;
		String global_organization_ID_hash;
		RequestData(){}
		RequestData (String gid) {
			orgs = new ArrayList<String>(); orgs.add(gid);
			global_organization_ID_hash = gid;
		}
		@Override
		public Encoder getEncoder() {
			Encoder enc = new Encoder();
			enc.addToSequence(Encoder.getStringsEncoder(orgs, Encoder.TAG_PrintableString).setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED,  (byte)15));
			enc.addToSequence(Encoder.getStringsEncoder(neig, Encoder.TAG_PrintableString));
			enc.addToSequence(Encoder.getEncoder(cons).setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED,  (byte)13));
			enc.addToSequence(Encoder.getStringsEncoder(witn, Encoder.TAG_PrintableString));
			enc.addToSequence(Encoder.getStringsEncoder(moti, Encoder.TAG_PrintableString));
			enc.addToSequence(Encoder.getStringsEncoder(just, Encoder.TAG_PrintableString));
			enc.addToSequence(Encoder.getEncoder(sign).setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED,  (byte)13));
			enc.addToSequence(Encoder.getStringsEncoder(tran, Encoder.TAG_PrintableString));
			enc.addToSequence(Encoder.getStringsEncoder(news, Encoder.TAG_PrintableString));
			enc.addToSequence(new Encoder(this.global_organization_ID_hash));
			return enc.setASN1Type(RequestData.getASN1Type());
		}

		public static byte getASN1Type() {
			return Encoder.TAG_SEQUENCE;
		}
		@Override
		public RequestData decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			orgs = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			neig = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			cons = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_SEQUENCE, new HSS_Elem());
			witn = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			moti = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			just = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			sign = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_SEQUENCE, new HSS_Elem());
			tran = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			news = d.getFirstObject(true).getSequenceOfAL(Encoder.TAG_PrintableString);
			global_organization_ID_hash = d.getFirstObject(true).getString();
			return this;
		}
		
		@Override
		public RequestData instance() {
			return new RequestData();
		}
	}
	static class SpecificRequest extends ASNObj {
		ArrayList<RequestData> rd;
		
		SpecificRequest(){}
		SpecificRequest(String gid) {
			rd = new ArrayList<RequestData>();
			rd.add(new RequestData(gid));
		}
		@Override
		public Encoder getEncoder() {
			Encoder enc = new Encoder();
			enc.addToSequence(Encoder.getEncoder(rd));
			return enc;
		}

		@Override
		public SpecificRequest decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			rd = d.getFirstObject(true).getSequenceOfAL(RequestData.getASN1Type(), new RequestData());
			return this;
		}
		
	}
	static class ASNSyncRequest extends ASNObj {
		String version = "2";
		SpecificRequest request;
		byte[] signature;
		
		ASNSyncRequest(String gid) {
			request = new SpecificRequest(gid);
		}
		
		@Override
		public Encoder getEncoder() {
			Encoder enc = new Encoder();
			enc.addToSequence(new Encoder(version));
			enc.addToSequence(request.getEncoder());
			enc.addToSequence(new Encoder(signature));
			return enc;
		}

		@Override
		public Object decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			version = d.getFirstObject(true).getString();
			request = new SpecificRequest().decode(d.getFirstObject(true));
			signature = d.getFirstObject(true).getBytes();
			return this;
		}
		
	}
	static public void main(String args[]) {
		String gid = null;
		String filename = "default_file_for_message.der";
		
	 	char c;
	 	while(( c = util.GetOpt.getopt(args, "g:f:"))!=GetOpt.END){
	 	    switch(c) {
	 	    case 'f': filename = GetOpt.optarg; break;
	 	    case 'g': gid = GetOpt.optarg; break;
	 	    //case GetOpt.END: System.out.println(""); break;
	 	    case '?': 
	 	    default:
	 		 System.out.println("Error: You should call the program with parameters:"
	 		 		+ "\n\t	javac -cp DD.jar <options>"
	 		 		+ "\n\t\t Available options are:"
	 		 		+ "\n\t\t\t -f <filename> : by default the filename is dafault_filename.der"
	 		 		+ "\n\t\t\t -g <GIDHASH> : the GIDHASH");
	 		 return;
	 	    }
	 	}
	 	if (gid == null) {
	 		 System.out.println("Error: Missing GID. You should call the program with parameters:"
		 		 		+ "\n\t	javac -cp DD.jar <options>"
		 		 		+ "\n\t\t Available options are:"
		 		 		+ "\n\t\t\t -f <filename> : by default the filename is dafault_filename.der"
		 		 		+ "\n\t\t\t -g <GIDHASH> : the GIDHASH");
	 		System.exit(-1);
	 	}
		
		ASNSyncRequest asr = new ASNSyncRequest(gid);
		FileOutputStream dos;
		try {
			dos = new FileOutputStream(filename);
			dos.write(asr.encode());
			dos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

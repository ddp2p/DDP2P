package data;

import updates.VersionInfo;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.PK;
import ciphersuits.SK;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import config.Application_GUI;
import static util.Util.__;

public class D_SoftwareUpdatesReleaseInfoDataSignedByTester extends ASN1.ASNObj{
	D_SoftwareUpdatesReleaseInfoByTester tester_info;
	D_ReleaseQuality quality_release[];
	VersionInfo version_info;
	public D_SoftwareUpdatesReleaseInfoDataSignedByTester() {}
	public D_SoftwareUpdatesReleaseInfoDataSignedByTester(VersionInfo a, int idx) {
		version_info = a;
		this.quality_release = a.releaseQD;
		tester_info = a.testers_data[idx];
	}
	public String toString() {
		return "D_TesterSignedData [\n"+
				"\n   tester_info = "+tester_info+"\n"+
				"\n   quality_release = "+Util.concat(quality_release, " || ")+
				"\n   version_info = "+version_info+"\n"+
	"]";
	}
	public void generateXMLFile (String fileName){
		if(fileName == null){
			Application_GUI.warning(__("file name cannot be null ,need xml_file_name;"), __("TESTER GENERATE XML"));
			return;
		}
		try{
		
			FileWriter xmlFile = new FileWriter(fileName);
	//		if(!xmlFile.isFile()){
	//			Application.warning(_("Bad parameters list: need existing xml_file_name;"), _("TESTER GENERATE XML"));
	//			return;			
	//		}
	//		
			BufferedWriter xmlOut = new BufferedWriter(xmlFile);
		/*	xmlOut.write("<?xml version="1.0" encoding="ISO-8859-1"?>");
			String[] tester */
			
		
		}catch (IOException e)
		{
		    System.out.println("Exception ");
		 
		}

	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
			enc.addToSequence(tester_info.getSignableEncoder());
			enc.addToSequence(Encoder.getEncoder(quality_release));
			enc.addToSequence(version_info.getSignableEncoder());
		return enc;
	}

	@Override
	public D_SoftwareUpdatesReleaseInfoDataSignedByTester decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		//d.getFirstObject(true);
		throw new ASN1DecoderFail("Not meant to be decoded, encoder only for signatures");
		//return null;
	}
	public byte[] sign(SK sk) {
		byte[] result;
		tester_info.signature = new byte[0];
		byte[] msg = this.encode();
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************");
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: VI tester info="+this);
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: ************************\n");
		result = tester_info.signature = Util.sign(msg, sk);
		System.out.println("D_UpdatesKeyInfo: verifySignaturesOfVI: signature="+Util.stringSignatureFromByte(result));
		return result;
	}
	public boolean verifySignature(PK pk) {
		byte[] msg = this.encode();
		return Util.verifySign(msg, pk, tester_info.signature);
	}
}
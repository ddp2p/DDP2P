package tools;

import static util.Util.__;

import java.io.File;

import util.Base64Coder;
import ASN1.Encoder;
import config.Application_GUI;
import data.D_ReleaseQuality;

/**
 * Used to create the VersionInfo raw file, before signing
 * @author msilaghi
 *
 */
public class ReleaseQuality {
	
	public static void main(String[]args){
		if(args.length!=1){
			Application_GUI.warning(__("Bad parameters list: need xml_file_name;"), __("RELEASE QUALITY"));
			return;
		}
		File xml = new File(args[0]);
		if(!xml.isFile()){
			Application_GUI.warning(__("Bad parameters list: need existing xml_file_name;"), __("RELEASE QUALITY"));
			return;			
		}
		D_ReleaseQuality[] rq = data.D_ReleaseQuality.parseXML(xml);
		Encoder enc = Encoder.getEncoder(rq);
		System.out.println(new String(Base64Coder.encode(enc.getBytes()))); 
	}

}

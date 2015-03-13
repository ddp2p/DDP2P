package tools;

import static net.ddp2p.common.util.Util.__;

import java.io.File;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_ReleaseQuality;
import net.ddp2p.common.util.Base64Coder;

/**
 * Used to create the VersionInfo raw file, before signing
 * @author M Silaghi and K Alhamed
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
		D_ReleaseQuality[] rq = net.ddp2p.common.data.D_ReleaseQuality.parseXML(xml);
		Encoder enc = Encoder.getEncoder(rq);
		System.out.println(new String(Base64Coder.encode(enc.getBytes()))); 
	}

}

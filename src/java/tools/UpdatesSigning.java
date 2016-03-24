package tools;

import static net.ddp2p.common.util.Util.__;

import java.io.File;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.Util;

public class UpdatesSigning {
	public static void main(String args[]) {
		try {
			//System.out.println("ClientUpdates: main: start");
			if (args.length != 4) {System.err.println("Call with parameters: input key output install: ["+args.length+"] "+Util.concat(args, ",")); return;}
			String input = args[0];
			String key = args[1];
			String output = args[2];
			String install = args[3];
			System.out.println(__("Called with parameters: ")+input+" "+key+"  "+output+" "+install);
			install += Application.OS_PATH_SEPARATOR;
			File _input = new File(input);
			File _key = new File(key);
			File _output = new File(output);
			File _install = new File(install);

			boolean ya=true;
			if(!_input.exists()|| !_input.isFile())
			{ya = false;Application_GUI.warning(__("Input file is not good:")+input, __("Signing did not work"));}
			if(!_key.exists()|| !_key.isFile())
			{ya=false;Application_GUI.warning(__("Key file is not good:")+key, __("Signing did not work"));}
			if(_output.exists())
			{ya=false;Application_GUI.warning(__("Ouput file exists:")+output, __("Signing did not work"));}
			if(!_install.exists() || !_install.isDirectory())
			{ya=false;Application_GUI.warning(__("Install director wrong:")+install, __("Signing did not work"));}
			if(!ya){
				System.out.println(__("Call with bad parameters: ")+input+" "+key+"  "+output);
				return;
			}

			System.out.println("Called with good parameters: "+input+" "+key+"  "+output+" "+install);

			
			//System.out.println("ClientUpdates: main: will sign");
			boolean result = ClientUpdates.create_info(input, key, output, install);
			if(!result) Application_GUI.warning(__("Something bad happened signing"), __("Signing did not work"));
			System.out.println("ClientUpdates: main: done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

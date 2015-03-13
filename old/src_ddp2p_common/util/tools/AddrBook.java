/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package util.tools;

import util.GetOpt;
import hds.DirectoryRequest;
import hds.DirectoryServer;
import hds.DirectoryServerUDP;
import config.Application;

public class AddrBook {
	private static boolean DEBUG = false;

	public static void main(String[] args) {
		if (DEBUG) {
			System.out.println("AddrBook: len="+args.length);
			for (int k = 0; k < args.length; k ++) {
				System.out.println("AddrBook: len="+k+" "+args[k]);		
			}
		}
		try {
			util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			
			char c;
			opts:
			while ((c = GetOpt.getopt(args, "d:P:I:r:a:hv")) != GetOpt.END) {
				System.out.println("Options received"+GetOpt.optopt +" optind="+GetOpt.optind+" arg="+GetOpt.optarg);
				switch (c) {
				case 'h':
					System.out.println("java -cp jars/sqlite4java.jar:jars/sqlite-jdbc-3.7.2.jar:jars/DD.jar util.tools.AddrBook "
							+ "\n\t -h          Help"
							+ "\n\t -v          Verbose"
							+ "\n\t -d file     Use file as current database (dir database in the same folder)"
							+ "\n\t -P port     port"
							+ "\n\t -I IP       The IP address from which one can get messages"
							+ "\n\t -c GID      Accept requests for peer with this GID as current peer"
							+ "\n\t -a GID      Accept announcements/pings/requests for peer with this GID as current peer"
							);
					//printHELP();
					//break;
					System.exit(-1);//return;
				case 'd':
					if (DEBUG) System.out.println("Option d: "+GetOpt.optarg);
					Application.DIRECTORY_FILE = GetOpt.optarg;
					break;
				case 'v':
					DEBUG = true;
					DirectoryServer.DEBUG = true;
					DirectoryServerUDP.DEBUG = true;
					DirectoryRequest.DEBUG = true;
					if (DEBUG) System.out.println("Option d: "+GetOpt.optarg);
					break;
				case 'P':
					if (DEBUG) System.out.println("Option P: "+GetOpt.optarg);
					try {
						DirectoryServer.PORT = Integer.parseInt(GetOpt.optarg);
					} catch(Exception e) {e.printStackTrace();}
					break;
				case 'I':
					DirectoryServer.mAcceptedIPs.add(GetOpt.optarg);
					break;
				case 'a':
					DirectoryServer.mAcceptedGIDH_Addresses.add(GetOpt.optarg);
					break;
				case 'c':
					DirectoryServer.mAcceptedGIDH_Clients.add(GetOpt.optarg);
					break;
				case GetOpt.END:
					if (DEBUG) System.out.println("REACHED END OF OPTIONS");
					break;
				case '?':
					System.out.println("Options ?:"+GetOpt.optopt +" optind="+GetOpt.optind+" "+GetOpt.optarg);
					return;
				case ':':
					System.out.println("Options \":\" for "+GetOpt.optopt);
					break;
				default:
					System.out.println("AddrBook: unknown option error: \""+c+"\"");
					break opts;
					//return;
				}
			}
			// if (args.length > 0) Application.DIRECTORY_FILE = args[0];
			// if (args.length > 1) DirectoryServer.PORT = Integer.parseInt(args[1]);
			DirectoryServer ds = new DirectoryServer(DirectoryServer.PORT);
			ds.start();
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}		
	}

}

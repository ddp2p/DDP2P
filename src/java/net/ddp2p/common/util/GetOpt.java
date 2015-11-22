/*   Copyright (C) 2004,2014 Marius C. Silaghi
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
package net.ddp2p.common.util;
/**
 * The class to handle options unix style
 * @author msilaghi
 *
 */
public class GetOpt {
	/**
	 * The currently handled option (to be used when optarg is not found and the function getopt returns ":" or "?".
	 */
    public static char optopt;
	/**
	 * The next points to the parameter of an option with argument (:)
	 */
    public static String optarg = null;
    /**
     * The index of the next character option in the string of the currently analyzed String parameter.
     * Like 2 when next handling v for "-gva".
     */
    static int inoptind = 0;
    /**
     * The index of the next handled argument String. 
     * Could be the same as current if this contains multiple options not yet ready.
     */
    public static int optind = 0;
    /**
     * An global error marker, not used
     */
    public static int opterr = 1;
       /**
     * The result returned after successful processing of all objects
     */
    public static final char END = (char) -1;
    /**
     * Multiple options may be in the same parameter string, but if one has an argument,
     * then the remaining ones are discarded and the argument is taken from the next parameter string.
     * 
     * Returns END when encountering -- or end of parameters.
     * 
     * A parameter that is not a recognized option is switched with the next parameter that
     * is starting with -. If that one is taking an argument then also the argument is switched
     * (something that may not be the expected and desired effect, so may be changed in the future).
     * The desired behavior is to shift all next parameters to the right, and bring the argument on the next position.
     * <pre>
     * {@code
     public static void main(String[]args){
 	char c;
 	while((c = getopt(args, "fc:d")) != END) {
 	    switch(c){
 	    case 'f': System.out.println("f"); break;
 	    case 'c': System.out.println("c "+optarg); break;
 	    case 'd': System.out.println("d"); break;
 	    case END: System.out.println("-1 (done)"); break;
	    case '?': System.out.println("?: unknown option "+optopt); return;
	    case ':': System.out.println("? missing parameter for:"+optopt); return;
 	    default:
 		 System.out.println("Error: "+c);
 		 return;
 	    }
 	}
 	if (optind < args.length)
 	    System.out.println("OPTS:"+args[optind]);
     }
     }
     * </pre>
     * 
     * 
     * @param argv
     * @param optstring
     * @return Result may not be the same with GetOpt.optopt, 
     * specially when result is "?", ":" or GetOpt.END
     */
    public static char getopt(String argv[], String optstring) {
    	return getopt(argv,optstring.toCharArray());
    }
    public static char getopt(String argv[], char[] optstring) {
    	int ind_potential_optarg = 0,crt_opt_ind = 0;  
    	if ((optind < argv.length) && (inoptind >= argv[optind].length())) {
    		inoptind = 0;
    		optind ++;
    	} 
    	if (optind >= argv.length) {
    		return END;
    	}
    	for	(crt_opt_ind = optind; crt_opt_ind < argv.length; crt_opt_ind ++, inoptind = 0) {
			ind_potential_optarg = crt_opt_ind + 1;
		    optopt = argv[crt_opt_ind].charAt(inoptind);
		    if (inoptind == 0) {
		    	if ((optopt == '-') &&
		    			(argv[crt_opt_ind].length() == 2) &&
		    			(argv[crt_opt_ind].charAt(1) == '-')) {
		    		optind = crt_opt_ind+1;
		    		return END;
		    	}
		    	if ((optopt != '-') || (argv[crt_opt_ind].length() == 1)) {
		    		int index_to_switch = 0;
		    		for (index_to_switch = crt_opt_ind + 1; index_to_switch < argv.length; index_to_switch ++) {
		    			if ((argv[index_to_switch].charAt(0) == '-')
		    					&& ((argv[index_to_switch].length() > 2) ||
		    					((argv[index_to_switch].length() == 2)
		    							&& (argv[index_to_switch].charAt(1) != '-')))) {
		    				String tmp = argv[index_to_switch];
		    				argv[index_to_switch] = argv[crt_opt_ind];
		    				argv[crt_opt_ind] = tmp;
		    				ind_potential_optarg = index_to_switch + 1;
		    				break;
		    			} else {
		    				if ((argv[index_to_switch].length() == 2)
		    						&& (argv[index_to_switch].charAt(0) == '-')
		    						&& (argv[index_to_switch].charAt(1) == '-')) {
		    					String tmp = argv[index_to_switch];
		    					argv[index_to_switch] = argv[crt_opt_ind];
		    					argv[crt_opt_ind] = tmp;
		    					optind = crt_opt_ind + 1;
		    					return END;		
		    				}
		    			}
		    		}
		    		if (index_to_switch == argv.length) {
		    			optind = crt_opt_ind;
		    			return (char) -1;
		    		}
		    	}
		    	inoptind ++;
		    	optopt = argv[crt_opt_ind].charAt(inoptind);
		    }
		    int ind_opt_in_optstring;
		    for (ind_opt_in_optstring = 0; ind_opt_in_optstring < optstring.length; ind_opt_in_optstring ++) {
		    	if (optstring[ind_opt_in_optstring] == optopt) {
		    		if ((ind_opt_in_optstring < optstring.length - 1) && (optstring[ind_opt_in_optstring + 1] == ':')) {
		    			if ( ind_potential_optarg >= argv.length) {
		    				optind ++;
			    			inoptind = -1;
		    				return ':';
		    			}
		    			String tmp = argv[ind_potential_optarg];
		    			argv[ind_potential_optarg] = argv[crt_opt_ind + 1];
		    			argv[crt_opt_ind + 1] = tmp; 
		    			optind = crt_opt_ind + 2;
		    			inoptind = -1;
		    			optarg = argv[crt_opt_ind + 1];
		    		}
		    		inoptind ++;
		    		return optopt;
		    	}
		    }
		    if (ind_opt_in_optstring == optstring.length) return '?';
		}
		return END;
    }
}

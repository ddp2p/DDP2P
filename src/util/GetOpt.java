//By Marius Calin Silaghi, FIT
//June, 15 2004
// Package implementing standard UNIX "getopt" for JAVA
// To be inherited by main application classes

package util;

public class GetOpt{
    public static String optarg=null;
    public static int optind=0, opterr=1;
    public static char optopt;
    public static final char END=(char)-1;
    static int inoptind=0;
//     public static void main(String[]args){
// 	char c;
// 	while((c=getopt(args, "fc:d"))!=END){
// 	    switch(c){
// 	    case 'f': System.out.println("f"); break;
// 	    case 'c': System.out.println("c "+optarg); break;
// 	    case 'd': System.out.println("d"); break;
// 	    case END: System.out.println("-1"); break;
// 	    case '?': System.out.println("?:"+optopt); return;
// 	    default:
// 		 System.out.println("Error: "+c);
// 		 return;
// 	    }
// 	}
// 	if(optind<args.length)
// 	    System.out.println("OPTS:"+args[optind]);
//     }
    public static char getopt(String argv[],
			      String optstring){
	return getopt(argv,optstring.toCharArray());
    }
    public static char getopt(String argv[],
			      char[] optstring){
	int ind=0, i=0, o=0, k=0;
	if((optind<argv.length)&&(inoptind>=argv[optind].length())){
	    inoptind = 0;
	    optind++;
	} 
	if(optind>=argv.length){
	    return END;
	}
	for(k=optind; k<argv.length; k++, inoptind=0) {
	    ind=k+1;
	    optopt=argv[k].charAt(inoptind);
	    if(inoptind==0){
		if((optopt == '-') &&
		   (argv[k].length()==2)&&
		   (argv[k].charAt(1) == '-')){
		    optind = k+1;
		    return END;
		}
		if((optopt != '-')||(argv[k].length()==1)){
		    for(i=k+1; i<argv.length; i++)
			if((argv[i].charAt(0)=='-')&&((argv[i].length()>2)||
			   ((argv[i].length()==2)&&(argv[i].charAt(1) != '-')))){
			    String tmp=argv[i];
			    argv[i] = argv[k];
			    argv[k] = tmp;
			    ind=i+1;
			    break;
			}else
			    if((argv[i].length()==2)&&(argv[i].charAt(0) == '-')
			       &&(argv[i].charAt(1) == '-')){
				String tmp=argv[i];
				argv[i] = argv[k];
				argv[k] = tmp;
				optind=k+1;
				return END;		
			    }
		    if(i==argv.length){//end of options
			optind = k;
			return (char)-1;
		    }
		}
		inoptind++;
		optopt=argv[k].charAt(inoptind);
	    }
	    for(o=0; o<optstring.length; o++){
		if(optstring[o]==optopt){
		    if((o<optstring.length-1)&&(optstring[o+1]==':')){
			if(ind>=argv.length) return ':';
			String tmp=argv[ind];
			argv[ind] = argv[k+1];
			argv[k+1] = tmp;
			optind = k+2;
			inoptind=-1;
			optarg=argv[k+1];
		    }
		    inoptind++;
		    return optopt;
		}
	    }
	    if(o==optstring.length) return '?';
	}
	return END;
    }
}

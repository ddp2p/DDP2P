/*   Copyright (C) 2012
		Author: Khalid Alhamed
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
 * @(#)updateJARfile.java
 *
 *
 * @author Khalid Alhamed 
 * @version 1.00 2012/11/16
 */
import java.io.File;
import java.io.RandomAccessFile;
public class updateJARfile {
    private final static int MATCHSIZE=4; 
    public static void main(String [] a) throws Exception{
       if(a.length!=1){
          System.err.println("You should provide a jar-file as an input ");
    	  System.exit(0);
       }
       File f = new File(a[0]); 
       RandomAccessFile io = new RandomAccessFile(f,"rw");
       byte[] buffer = new byte[(int)f.length() +1]; 
   	   byte[] match = new byte[MATCHSIZE];
   	   int len = 0;
  	   int i=0;
       while ( (len = io.read(buffer)) > 0 ) {
          if (i==0) for(int j=0; j<4; j++)
      	 			  match[j] = buffer[10+j]; 
      	  for (int j=0; j< len; j++)
      	     if(buffer[j]==match[0] && (j+3) < len &&buffer[j+1]==match[1] && buffer[j+2]==match[2] && buffer[j+3]==match[3])
      	 	 { io.seek(j);
      	       byte[] b = new byte[4];
      	 	   io.write(b);
      	 	 }
      	 	 io.seek(buffer.length);	
      	  i++;	         
          if(i>1) {System.out.println("The file should be remake!: "+ i +"  " + len );	throw new Exception("Cannot read whole file to ram at once");}
       }
       io.close();
   } 
}

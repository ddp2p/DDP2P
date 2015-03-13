/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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

package net.ddp2p.common.util;

import java.net.SocketAddress;

public class CommEvent {
	Object source;
	String peer_name;
	SocketAddress peer_address;
	String message_type;
	String message;
	public CommEvent(Object source, String peer_name, SocketAddress peer_address, 
			String message_type, String message){
		this.source = source;
		this.peer_name = peer_name;
		this.peer_address = peer_address;
		this.message_type = message_type;
		this.message = message;
	}
	public String toString(){
		String name="";
		if(peer_name!=null) name="\""+peer_name+"\"";
		String address="";
		if(peer_address!=null) address=" at addr=\""+peer_address+"\"";
		return "CommEvent from Source: \""+source+"\" Peer: "+name+address+" Message_type: \""+message_type+"\" Message: \""+message+"\"";
	}
}

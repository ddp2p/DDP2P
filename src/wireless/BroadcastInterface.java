/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Ossamah Dhannoon
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
package wireless;
import java.net.*;

public class BroadcastInterface{
	public static final InetAddress BROADCAST_ADDRESS = getByAddress(new byte[]{(byte) 255,(byte) 255,(byte) 255,(byte) 255});
    public InetAddress broadcast_address = BROADCAST_ADDRESS;
	public int servPort=BroadcastServer.BROADCAST_SERVER_PORT;
    BroadcastInterface(){}
    public static InetAddress getByAddress(byte[] addr) {
    	try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
    }
    public BroadcastInterface(InetAddress _address){
    	broadcast_address = _address;
    	
    }
}

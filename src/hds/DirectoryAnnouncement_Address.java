/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

package hds;

import java.math.BigInteger;

import ASN1.Encoder;
/**
 * DirectoryAnnouncement_Address = SEQUENCE {
 * 		addresses UTF8String,
 * 		udp_port INTEGER
 * }
 * @author msilaghi
 *
 */
public class DirectoryAnnouncement_Address{
		public String addresses; // will store a string of domain:port,domain:port...
		public int udp_port;
		DirectoryAnnouncement_Address(){}
		public String toString() {
			//return domain+":"+port;
			return addresses+" (UDP port="+udp_port+")";
		}
		Encoder getEncoder() {
			Encoder enc = new Encoder()
			.initSequence()
			.addToSequence(new Encoder(addresses))
			.addToSequence(new Encoder(new BigInteger(""+udp_port)))
			;
			return enc;
		}
}
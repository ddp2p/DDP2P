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

import util.Util;

import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
/**
 * DirectoryAnnouncement_Address = SEQUENCE {
 * 		addresses UTF8String,
 * 		udp_port INTEGER
 * }
 * @author msilaghi
 *
 */
public class DirectoryAnnouncement_Address extends ASNObj{
		int version = 2;
		public Address[] _addresses;
		//public String addresses; // will store a string of domain:port,domain:port...
		public int udp_port;
		DirectoryAnnouncement_Address(){}
		DirectoryAnnouncement_Address(int _version){version = _version;}
		public String toString() {
			//return domain+":"+port;
			return addresses()+" (UDP port="+udp_port+")";
		}
		public String addresses(){
			//return Util.concat(_addresses, DirectoryServer.ADDR_SEP, null);
			return Address.joinAddresses(_addresses);
		}
		public String setAddresses(String addr){
			if(addr == null) return null;
			try{
			 String[] adr = Address.split(addr);
			 _addresses = new Address[adr.length];
			 for(int k=0; k<adr.length; k++)
				 _addresses[k] = new Address(adr[k]);
			}catch(Exception e){e.printStackTrace();}
			return addr;
		}
		/**
		 * DD.TAG_AC10
		 * @return
		 */
		static byte getASN1Tag(){
			return DD.TAG_AC10;
		}
		@Override
		public Encoder getEncoder() {
			switch(version){
			case 0:
			case 1:
				return getEncoder_1();
			case 2:
			default:
				return getEncoder_2();
			}
		}
		public Encoder getEncoder_1() {
			Encoder enc = new Encoder()
			.initSequence()
			.addToSequence(new Encoder(addresses()))
			.addToSequence(new Encoder(new BigInteger(""+udp_port)))
			;
			enc.setASN1Type(getASN1Tag());
			return enc;
		}
		public Encoder getEncoder_2() {
			Encoder enc = new Encoder()
			.initSequence()
			.addToSequence(Encoder.getEncoder(_addresses));
			;
			enc.setASN1Type(getASN1Tag());
			return enc;
		}
		@Override
		public DirectoryAnnouncement_Address decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			switch(version){
			case 0:
			case 1:
				return decode_1(d);
			case 2:
			default:
				return decode_2(d);
			}
		}
		public DirectoryAnnouncement_Address decode_1(Decoder d) throws ASN1DecoderFail {
			setAddresses(d.getFirstObject(true).getString());
			udp_port = d.getFirstObject(true).getInteger().intValue();
			return this;
		}
		public DirectoryAnnouncement_Address decode_2(Decoder d) throws ASN1DecoderFail {
			_addresses = d.getFirstObject(true).getSequenceOf(Address.getASN1Type(), new Address[]{}, new Address());
			return this;
		}
}
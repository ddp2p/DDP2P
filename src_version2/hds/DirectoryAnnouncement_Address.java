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
import java.util.ArrayList;

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
		private static final int V1 = 1;
		private static final boolean DEBUG = false;
		int version = 2; // version of address
		int address_version = Address.V2;
		public Address[] _addresses;
		//public String addresses; // will store a string of domain:port,domain:port...
		public int udp_port;
		DirectoryAnnouncement_Address(){}
		DirectoryAnnouncement_Address(int _version){version = _version;}
		DirectoryAnnouncement_Address(int _version, int _address_version){
			version = _version;
			address_version = _address_version;
		}
		public String toString() {
			//return domain+":"+port;
			return "[V="+version+" AD="+address_version+" A="+addresses()+" (UDP port="+udp_port+")]";
		}
		public String addresses() {
			//return Util.concat(_addresses, DirectoryServer.ADDR_SEP, null);
			return Address.joinAddresses(_addresses);
		}
		/*
		 * This is avoided as it needs joining which is unsafe
		 *
		*
		public String setAddresses(String addr){
			if (addr == null) return null;
			try {
			 String[] adr = Address.split(addr);
			 _addresses = new Address[adr.length];
			 for (int k = 0; k < adr.length; k ++) {
				 _addresses[k] = new Address(adr[k]);
				 _addresses[k].set_version_structure(Address.V1); 
			 }
			} catch(Exception e){e.printStackTrace();}
			return addr;
		}
		*/
		/**
		 * 
		 * @param addr
		 * @return
		 */
		public Address[] setAddresses(ArrayList<Address> addr) {
			if (addr == null) return null;
			try {
			 //String[] adr = Address.split(addr);
			 _addresses = new Address[addr.size()];
			 for (int k = 0; k < addr.size(); k ++) {
				 _addresses[k] = new Address(addr.get(k));
				 _addresses[k].set_version_structure(Address.V1); 
			 }
			} catch (Exception e) {e.printStackTrace();}
			return _addresses;
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
				if (DEBUG) System.out.println("DAA:enc:V1");
				return getEncoder_1();
			case 2:
			default:
				if (DEBUG) System.out.println("DAA:enc:V2");
				return getEncoder_2();
			}
		}
		/**
 DirectoryAnnouncement_Address = SEQUENCE [AC10] IMPLICIT { -- V1
 		addresses UTF8String,
 		udp_port INTEGER
 }
		 * @author msilaghi
		 *
		 */
		public Encoder getEncoder_1() {
			String adr1 = addresses();
			if (DEBUG) System.out.println("DAA:enc1:adr="+adr1);
			if(_addresses!=null)for (Address a : _addresses) a.set_version_structure (Address.V1);
			String adr_1 = addresses();
			if (DEBUG) System.out.println("DAA:enc1:adr="+adr_1);
			
			Encoder enc = new Encoder()
			.initSequence()
			.addToSequence(new Encoder(adr_1))
			.addToSequence(new Encoder(new BigInteger(""+udp_port)))
			;
			enc.setASN1Type(getASN1Tag());
			return enc;
		}
		/**
DirectoryAnnouncement_Address = SEQUENCE [AC10] IMPLICIT { -- V2
	version INTEGER,
	address_version INTEGER
 	addresses SEQUENCE OF Address,
}
		 * @author msilaghi
		 *
		 */
		public Encoder getEncoder_2() {
			if (_addresses != null) for (Address a : _addresses) a.set_version_structure (address_version); //Address.V2;
			Encoder enc = new Encoder()
			.initSequence()
			.addToSequence(new Encoder(version))
			.addToSequence(new Encoder(address_version))
			.addToSequence(Encoder.getEncoder(_addresses));
			;
			if (udp_port > 0)
				enc.addToSequence(new Encoder(udp_port).setASN1Type(DD.TAG_AP1));
			enc.setASN1Type(getASN1Tag());
			return enc;
		}
		@Override
		public DirectoryAnnouncement_Address decode(Decoder dec) throws ASN1DecoderFail {
			Decoder d = dec.getContent();
			version = V1;
			if (d.getTypeByte() == Encoder.TAG_INTEGER) {
				version = d.getFirstObject(true).getInteger().intValue(); 
				//System.out.println("DA_A: version="+version);
			}
			switch (version) {
			case 0:
			case 1:
				return decode_1(d);
			case 2:
			default:
				return decode_2(d);
			}
		}
		public DirectoryAnnouncement_Address decode_1(Decoder d) throws ASN1DecoderFail {
			String addr = d.getFirstObject(true).getString();
			 String[] adr = Address.split(addr);
			 ArrayList<Address> _addresses = new ArrayList<Address>();//[adr.length];
			 for (int k = 0; k < adr.length; k ++) {
				 Address _addr = new Address(adr[k]);
				 _addr.set_version_structure(Address.V1);
				 _addresses.add(_addr);
			 }
			setAddresses(_addresses);
			udp_port = d.getFirstObject(true).getInteger().intValue();
			this.address_version = Address.V1;
			return this;
		}
		public DirectoryAnnouncement_Address decode_2(Decoder d) throws ASN1DecoderFail {
			this.address_version = d.getFirstObject(true).getInteger().intValue();
			_addresses = d.getFirstObject(true).getSequenceOf(Address.getASN1Type(), new Address[]{}, new Address());
			udp_port = -1;
			if (d.isFirstObjectTagByte(DD.TAG_AP1))
				udp_port = d.getFirstObject(true).getInteger(DD.TAG_AP1).intValue();
			return this;
		}
}
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

import static java.lang.System.out;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import util.Util;
import ASN1.Decoder;
import ASN1.Encoder;

public class DirectoryAnswer {
	static final int MAX_DA = 1000;
	private static final int MAX_LEN = 10000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public Calendar date = Util.CalendargetInstance();
	//ArrayList<Address> address=new ArrayList<InetSocketAddress>();
	public ArrayList<Address> addresses=new ArrayList<Address>();
	public DirectoryAnswer() {}
	public String toString() {
		String result = Encoder.getGeneralizedTime(date)+" [";
		for(int k=0; k<addresses.size(); k++) {
			result += addresses.get(k)+",";
		}
		return result+"]";
	}
	public DirectoryAnswer(InputStream is) throws Exception {
		byte[] buffer = new byte[MAX_DA];
		int len=is.read(buffer);
		Decoder dec_da = new Decoder(buffer);
		int req_len=dec_da.objectLen();
		if(req_len > MAX_LEN)  throw new Exception("Unacceptable package!");
		if(req_len<0) throw new Exception("Not enough bytes received in first package!");
		if(req_len>len){
			if(dec_da.objectLen()>MAX_DA) {
				byte[] old_buffer = buffer;
				buffer = new byte[req_len];
				Encoder.copyBytes(buffer, 0, old_buffer, len, 0);
			}
			len += is.read(buffer, len, req_len-len);
			if(req_len!=len) throw new Exception("Not enough bytes received!");
			dec_da = new Decoder(buffer);
		}
		Decoder dec_da_content=dec_da.getContent();
		String gdate = dec_da_content.getFirstObject(true).getGeneralizedTime(Encoder.TAG_GeneralizedTime);
		if(DEBUG) out.println("Record date: "+gdate);
		//date.setTime(new Date(gdate));
		date = Util.getCalendar(gdate);
		Decoder dec_addresses = dec_da_content.getFirstObject(true).getContent();
		while(dec_addresses.type()!=Encoder.TAG_EOC) {
			if(DEBUG) out.println("Reading record: "+dec_addresses.dumpHex());
			Decoder dec_addr = dec_addresses.getFirstObject(true).getContent();
			String domain = dec_addr.getFirstObject(true).getString();
			if(DEBUG) out.println("domain: "+domain);
			if(DEBUG) out.println("port object: "+dec_addr.dumpHex());
			int tcp_port = dec_addr.getFirstObject(true).getInteger().intValue();
			if(DEBUG) out.println("tcp_port: "+tcp_port);
			int udp_port = dec_addr.getInteger().intValue();
			if(DEBUG) out.println("udp_port: "+udp_port);
			//address.add(new InetSocketAddress(domain, port));
			addresses.add(new Address(domain, tcp_port,udp_port));
		}
	}
	byte[] encode() {
		Encoder da = new Encoder().initSequence().addToSequence(new Encoder(Util.CalendargetInstance()));
		Encoder addresses_enc = new Encoder().initSequence();
		for(int k=0; k<this.addresses.size(); k++) {
			Address cA = this.addresses.get(k);
			Encoder crt = new Encoder().initSequence();
			crt.addToSequence(new Encoder(cA.domain));//.toString().split(":")[0]));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.tcp_port)));
			crt.addToSequence(new Encoder(new BigInteger(""+cA.udp_port)));
			addresses_enc.addToSequence(crt);
			if(DEBUG) out.println("Added: "+cA.domain+":"+cA.tcp_port+":"+cA.udp_port);
		}
		da.addToSequence(addresses_enc);
		return da.getBytes();
	}
}
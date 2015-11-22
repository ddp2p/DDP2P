/*   Copyright (C) 2012
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.common.data;
import java.util.ArrayList;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class D_DirectoryServerPreapprovedTermsInfo extends net.ddp2p.ASN1.ASNObj{
	private static final boolean _DEBUG = true;
	public static  boolean DEBUG = false;
	public long term_ID;
	public int priority;
	public boolean topic;
	public boolean ad;
	public boolean plaintext;
	public boolean payment;                    
	public long peer_ID;
	public long peer_instance_ID;
	public String dir_addr; 
	public String dir_tcp_port;
	public int service;
	public int priority_type; 
	public D_DirectoryServerPreapprovedTermsInfo() {
	}
	public D_DirectoryServerPreapprovedTermsInfo(ArrayList<Object> _u) {
		init(_u);
	}
	public String toString() {
		return "D_TermsInfo: ["+
				"\n\t term_ID="+term_ID+
				"\n\t peer_ID="+peer_ID+
				"\n\t peer_instance_ID="+peer_instance_ID+
				"\n\t dir_addr="+dir_addr+
				"\n\t dir_tcp_port="+dir_tcp_port+
				"\n\t priority="+priority+
				"\n\t topic="+topic+
				"\n\t ad="+ad+
				"\n\t plaintext="+plaintext+
				"\n\t payment="+payment+
				"\n\t payment="+service+
				"\n\t payment="+priority_type+
				"]";
	}
	public void init(ArrayList<Object> _u){
		if(DEBUG) System.out.println("D_TermsInfo: <init>: start");
		term_ID = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_ID),-1);
		topic = Util.stringInt2bool(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_TOPIC), false);
		priority = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PRIORITY), -1);
		ad = Util.stringInt2bool(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_AD), false);
		plaintext = Util.stringInt2bool(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PLAINTEXT), false);
		payment = Util.stringInt2bool(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PAYMENT), false);
		peer_ID = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PEER_ID),-1);
	    peer_instance_ID = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PEER_INSTANCE_ID),-1);
	    dir_addr = Util.getString(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_DIR_DOMAIN));
	    dir_tcp_port = Util.getString(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_DIR_TCP_PORT));
	    service = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_SERVICE), -1);
	    priority_type = Util.ival(_u.get(net.ddp2p.common.table.directory_forwarding_terms.F_PRIORITY_TYPE), -1);
		if(DEBUG) System.out.println("D_TermsInfo: <init>: done");
	}
 	@Override
	public Encoder getEncoder() {
		return null;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		return null;
	}
	public void storeNoSync(String cmd) throws P2PDDSQLException {
		String params[] = new String[net.ddp2p.common.table.directory_forwarding_terms.F_FIELDS];
		params[net.ddp2p.common.table.directory_forwarding_terms.F_PRIORITY] = Util.getString(this.priority);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_TOPIC] = Util.getIntStringBool(this.topic);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_AD] = Util.getIntStringBool(this.ad);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_PLAINTEXT] = Util.getIntStringBool(this.plaintext);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_PAYMENT] = Util.getIntStringBool(this.payment);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_PEER_ID] = Util.getString(this.peer_ID);
	    params[net.ddp2p.common.table.directory_forwarding_terms.F_PEER_INSTANCE_ID] = Util.getString(this.peer_instance_ID);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_DIR_DOMAIN] = this.dir_addr;
		params[net.ddp2p.common.table.directory_forwarding_terms.F_DIR_TCP_PORT] = this.dir_tcp_port;
		params[net.ddp2p.common.table.directory_forwarding_terms.F_SERVICE] = Util.getString(this.service);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_PRIORITY_TYPE] = Util.getString(this.priority_type);
		params[net.ddp2p.common.table.directory_forwarding_terms.F_ID] = Util.getString(this.term_ID);
		if(cmd.equals("update"))
			Application.getDB().updateNoSyncNULL(
					net.ddp2p.common.table.directory_forwarding_terms.TNAME, 
					net.ddp2p.common.table.directory_forwarding_terms._fields_terms_no_ID,
					new String[]{net.ddp2p.common.table.directory_forwarding_terms.term_ID},
					params,_DEBUG);
		if(cmd.equals("insert")){
			String params2[]=new String[net.ddp2p.common.table.directory_forwarding_terms.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			this.term_ID = 
					Application.getDB().insertNoSync(
							net.ddp2p.common.table.directory_forwarding_terms.TNAME,
							net.ddp2p.common.table.directory_forwarding_terms._fields_terms_no_ID,
							params2, _DEBUG);
		}
	}
}

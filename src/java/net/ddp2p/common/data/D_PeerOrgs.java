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

package net.ddp2p.common.data;

import java.util.ArrayList;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class D_PeerOrgs extends ASNObj {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String org_name; //OPT
	private String global_organization_IDhash; //OPT
	public String global_organization_ID; //OPT
	
	//not sent
	private boolean served;
	private String last_sync_date;
	public long organization_ID;
	
	public boolean dirty;
	public long peer_org_ID;
	
	public D_PeerOrgs(){}
	public D_PeerOrgs(long orgID) {
		this.organization_ID = orgID;
		D_Organization org = D_Organization.getOrgByLID_NoKeep(orgID, true);
		this.org_name = org.getName();
		this.global_organization_ID = org.getGID();
		this.global_organization_IDhash = org.getGIDH_or_guess();
		served = true;
		dirty = true;
	}
	public String getOrgGIDH_Or_Null() {
		return global_organization_IDhash;
	}
	public String getOrgGIDH() {
		if (global_organization_IDhash == null) {
			if (global_organization_ID == null) return null;
			return global_organization_IDhash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
		}
		return global_organization_IDhash;
	}
	public boolean getServed() {return served;}
	/**
	 * Used in the JCombobox in peer
	 */
	public String toString() {
		if (org_name == null) return global_organization_IDhash;
		return org_name;
	}
	public String toLongString() {
		return "PeerOrg[org_name="+org_name+" IDhash="+((global_organization_IDhash==null)?"null":("\""+global_organization_IDhash+"\""))+" orgID="+global_organization_ID+"]";
	}
	/**
	 * D_PeerOrgs ::= SEQUENCE {
	 * org_name [AC0] IMPLICIT UTF8String OPTIONAL,
	 * global_organization_IDhash [AC1] IMPLICIT UTF8String OPTIONAL,
	 * global_organization_ID [AC2] IMPLICIT UTF8String OPTIONAL,
	 * }
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		Encoder enc = new Encoder().initSequence();
		if (org_name != null) enc.addToSequence(new Encoder(org_name).setASN1Type(DD.TAG_AC0));
		if ((global_organization_IDhash != null) && (global_organization_ID == null)) {
			String repl_GIDH = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_IDhash);
			enc.addToSequence(new Encoder(repl_GIDH).setASN1Type(DD.TAG_AC1));
		}
		if (global_organization_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
			enc.addToSequence(new Encoder(repl_GID).setASN1Type(DD.TAG_AC2));
		}
		return enc;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if (org_name != null) enc.addToSequence(new Encoder(org_name).setASN1Type(DD.TAG_AC0));
		if (global_organization_IDhash != null) enc.addToSequence(new Encoder(global_organization_IDhash).setASN1Type(DD.TAG_AC1));
		if (global_organization_ID != null) enc.addToSequence(new Encoder(global_organization_ID).setASN1Type(DD.TAG_AC2));
		return enc;
	}
	@Override
	public D_PeerOrgs decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d=dec.getContent();
		if(d.getTypeByte()==DD.TAG_AC0) org_name = d.getFirstObject(true).getString(DD.TAG_AC0);
		if(d.getTypeByte()==DD.TAG_AC1) global_organization_IDhash = d.getFirstObject(true).getString(DD.TAG_AC1);
		if(d.getTypeByte()==DD.TAG_AC2) global_organization_ID = d.getFirstObject(true).getString(DD.TAG_AC2);
		if(d.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra objects in PeerOrgs");
		return this;
	}
    public D_PeerOrgs instance() {
        return new D_PeerOrgs();
    }
	public void store(D_Peer p) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_PeerOrgs: store: "+this);
		if (global_organization_ID != null) {
			//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			if (organization_ID <= 0) {
				organization_ID = UpdateMessages.get_organizationID(global_organization_ID, org_name, p.getArrivalDate(), null);
			}
			//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			//long peers_orgs_ID = get_peers_orgs_ID(peer_ID, organizationID, adding_date);
			dirty = false;
			if (peer_org_ID > 0) {
				Application.getDB().update(net.ddp2p.common.table.peer_org.TNAME,
						new String[]{net.ddp2p.common.table.peer_org.peer_ID, net.ddp2p.common.table.peer_org.organization_ID, net.ddp2p.common.table.peer_org.served, net.ddp2p.common.table.peer_org.last_sync_date},
						new String[]{net.ddp2p.common.table.peer_org.peer_org_ID},
						new String[]{p.getLIDstr(), Util.getStringID(organization_ID), "1", last_sync_date, Util.getStringID(peer_org_ID)},
						DEBUG);
			}else{
				peer_org_ID = Application.getDB().insert(net.ddp2p.common.table.peer_org.TNAME,
					new String[]{net.ddp2p.common.table.peer_org.peer_ID, net.ddp2p.common.table.peer_org.organization_ID, net.ddp2p.common.table.peer_org.served, net.ddp2p.common.table.peer_org.last_sync_date},
					new String[]{p.getLIDstr(), Util.getStringID(organization_ID), "1", last_sync_date}, DEBUG);
			}
		} else {
			if (DEBUG) System.out.println("D_PeerOrgs: store: null orgGID");
		}
	}
	/**
	 * 
	 * @param s
	 * @param _dirty
	 * @return true on change
	 */
	public boolean setServed(boolean s, boolean _dirty) {
		boolean r = s ^ served;
		served = s;
		if (_dirty) dirty = true;
		return r;
	}
	public void set_last_sync_date(String s, boolean _dirty) {
		this.last_sync_date = s;
		if (_dirty) dirty = true;
	}
	public String get_last_sync_date() {
		return last_sync_date;
	}
	public void setOrgGIDH(String string) {
		this.global_organization_IDhash = string;
	}
}

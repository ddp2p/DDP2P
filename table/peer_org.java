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
package table;

import java.util.regex.Pattern;



import data.D_PeerOrgs;

import streaming.UpdateMessages;
import streaming.UpdatePeersTable;
import util.Util;

public class peer_org {
	 public static final String peer_org_ID = "peer_org_ID";
	 public static final String peer_ID = "peer_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String served = "served";
	 public static final String last_sync_date = "last_sync_date";
	public static final String TNAME = "peer_org";
	public static final String ORG_NAME_SEP = "^"; // should not conflict with base64
	public static final String ORG_SEP = ";"; // should not conflict with base64
	public static final boolean DEBUG = false;
	//public static final String peersFields2[] = new String[]{"global_peer_ID","name","slogan","addresses","date","global_organizationIDs_names","peer_hash_alg","peer_signature"};
	//public static final String peersFieldsTypes2[] = new String[]{"TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT"};
	public static D_PeerOrgs[] peerOrgsFromString(String _orgs) {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: "+_orgs);
		if(_orgs==null) return null;
		String[]orgs = _orgs.split(Pattern.quote(peer_org.ORG_SEP));
		if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: ["+orgs.length+"]="+Util.concat(orgs, ","));
		//ArrayList<D_PeerOrgs> _result = new ArrayList<D_PeerOrgs>();
		D_PeerOrgs[]result = new D_PeerOrgs[orgs.length];
		for(int o=0; o<orgs.length; o++) {
			if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: handle #"+o+" org="+orgs[0]);
			String[] global_organizationID_name = orgs[o].split(Pattern.quote(peer_org.ORG_NAME_SEP), 2);
			if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: GID+name= ["+global_organizationID_name.length+"]="+Util.concat(global_organizationID_name, ","));
			String global_organizationID = global_organizationID_name[0];
			String org_name=null;
			if(global_organizationID_name.length>1){
				if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: handle= >1");
				String name64 = global_organizationID_name[1];
				if("N".equals(name64)) org_name = null;
				else org_name = util.Base64Coder.decodeString(name64);
				if("null".equals(org_name)){
					if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: handle= null name");
					org_name=null;
				}
			}
			result[o] = new D_PeerOrgs();
			result[o].org_name = org_name;
			result[o].global_organization_ID = global_organizationID;
			if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: handle="+result[o]);
		}
		if(DEBUG) System.out.println("UpdatePeersTable: peerOrgsFromString: return="+Util.concat(result," "));
		return result;
	}
	public static String stringFromPeerOrgs(D_PeerOrgs[] po){
		if(DEBUG) System.out.println("peer_org: stringFromPeerOrgs:  po="+Util.concat(po,";","NULL"));
		String result = null;
		for(D_PeerOrgs o: po) {
			String name = o.org_name; //make "null" for null
			String name64 = "N";
			if(name!=null)name64 = util.Base64Coder.encodeString(name);
			String global_org_ID = o.global_organization_ID+peer_org.ORG_NAME_SEP+name64;
			if(DEBUG) System.out.println("peer_org: stringFromPeerOrgs: next ="+global_org_ID);
			if(null==result) result = global_org_ID;
			else result = result+peer_org.ORG_SEP+global_org_ID;
		}
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;		
	}
}

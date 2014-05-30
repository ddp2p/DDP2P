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
package widgets.identities;

import static util.Util._;

class IdentityLeaf extends IdentityNode{
    long id;
    String OID, explain, OID_name;
    String certificate;
    long certificateID;
    long sequence=0;
    public IdentityBranch identityBranch;
    public IdentityLeaf(String _name, String _tip, long _id_identity_value, IdentityBranch iB) {
    	super(_name, _tip);
    	id = _id_identity_value;
    	OID=_("Property type not specified");
    	OID_name=_("Property name not specified");
    	explain=_("No explanation given");
    	identityBranch = iB;
    	if(DEBUG) System.err.println("leaf:"+name+" OID:="+OID+" explain:="+explain);
    }
    public IdentityLeaf(String _name, String _oid, String _oid_name, String explain, long _id_identity_value, long seq, IdentityBranch iB) {
    	super(_name, _oid+" ("+_oid_name+": "+explain+")");
    	OID = _oid;
    	OID_name = _oid_name;
    	this.explain = explain;
    	certificate = null;
    	id = _id_identity_value;
    	sequence = seq;
    	identityBranch = iB;
    	if(DEBUG) System.err.println("leaf:"+name+" OID="+OID+" explain="+explain);
    }
    public String getTip() {
    	if(OID_name==null) return null;
    	if(explain == null) return OID+" ("+OID_name+")";
    	return OID+" ("+OID_name+": "+explain+")";
    }
    public String toString() {
    	return name;//+" #"+id;
    }
    public void save(MyIdentitiesModel model, String name, String OID, String certificate, String explain, String OID_name) {
    	try {
    		model.db.update(table.identity_value.TNAME,
    				new String[]{table.identity_value.oid_ID,table.identity_value.value,table.identity_value.sequence_ordering,table.identity_value.certificate_ID}, new String[]{table.identity_value.identity_value_ID},
    				new String[]{OID,name,sequence+"",certificateID+"",id+""}
    		);
    	}catch(Exception e){}
		this.name = name;
		this.OID = OID;
		this.certificate = certificate;
		this.OID_name = OID_name;
		this.explain = explain;
		tip = OID+" ("+OID_name+": "+explain+")";
    }
}

/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.common.population;
import static net.ddp2p.common.util.Util.__;
import java.util.ArrayList;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_OID;
import net.ddp2p.common.data.D_OrgParam;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.util.Util;
public class ConstituentsIDNode extends ConstituentsBranch {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private int fixed_fields = 1;
	private ConstituentData constituent;
	String sql;
	String sql_params[] = new String[0];
	public void updateWitness(){  
		ArrayList<ArrayList<Object>> identities;
        try{
                String sql = "select w."+net.ddp2p.common.table.witness.sense_y_n+", count(*) " +
                                " from "+net.ddp2p.common.table.witness.TNAME+" as w " +
                                " where w."+net.ddp2p.common.table.witness.target_ID+" = ? " +
                                " GROUP BY w."+net.ddp2p.common.table.witness.sense_y_n+"; ";
                identities = Application.getDB().select(sql, new String[]{getConstituent().getC_LID()+""});
                getConstituent().witness_against = getConstituent().witness_for = 0;
                for (int k = 0; k < identities.size(); k ++) {
                        int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                        int wcount = Util.ival(identities.get(k).get(1), 1);
                        if (wsense == D_Witness.FAVORABLE) getConstituent().witness_for = wcount;
                        else if(wsense==D_Witness.UNFAVORABLE) getConstituent().witness_against = wcount;
                        else getConstituent().witness_neuter = wcount;
                }
                sql = "select w."+net.ddp2p.common.table.witness.sense_y_n +
                " from "+net.ddp2p.common.table.witness.TNAME+" as w " +
                " where w."+net.ddp2p.common.table.witness.target_ID+" = ? and w."+net.ddp2p.common.table.witness.source_ID+" = ? " +
                " ; ";
                long myself = model.getConstituentIDMyself();
                identities = Application.getDB().select(sql, 
                		new String[]{getConstituent().getC_LID()+"", myself+""});
                getConstituent().witnessed_by_me=D_Witness.UNSPECIFIED;
                if (identities.size() > 1) Application_GUI.warning(__("For constituent:")+" "+getConstituent().getC_LID(), __("Multiple witness from me"));
                for(int k=0; k<identities.size(); k++) {
                	int wsense = Util.ival(identities.get(k).get(0), D_Witness.UNKNOWN);
                	getConstituent().witnessed_by_me = wsense;
                	if(DEBUG) System.err.println("Witnessed by me with: "+wsense);
                }
                if(myself == getConstituent().getC_LID()) getConstituent().myself = 1; 
                else getConstituent().myself = 0; 
        }catch(Exception e){
                e.printStackTrace();                    
        }
	}
	public ConstituentsIDNode (ConstituentsInterfaceInput _model, ConstituentsBranch _parent, ConstituentData _data,
			String _sql_children, String[] _sql_params,
			Constituents_AddressAncestors[] _ancestors) {
		super(_model, _parent, _ancestors, 0);
		sql = _sql_children;
		sql_params = _sql_params;
		setConstituent(_data);
		D_Constituent cons = D_Constituent.getConstByLID(getConstituent().getC_LID(), true, false);
		if (cons == null) return;
		if(_data.external)  fixed_fields ++;
		setNchildren(cons.getFieldValuesFixedNB() + fixed_fields);
		updateWitness();
		if(DEBUG) System.err.println("ConstituentsModel: ConstituentsIDNode: "+getConstituent()+" #"+getNchildren());
	}
    public int getIndexOfChild(Object child) {
     	if((child==null)||!(child instanceof ConstituentsPropertyNode)) return -1;
    	for(int i=0; i<getChildren().length; i++){
    		if (getChildren()[i] == child) return i;
    		if (((ConstituentsPropertyNode)getChildren()[i]).get_field_valuesID()==
    			((ConstituentsPropertyNode)child).get_field_valuesID()) return i;
    	}
    	return -1;
    }
    public long get_constituentID() {
    	if(getConstituent() == null) return -1;
    	return getConstituent().getC_LID();
    }
    public String getTip() {
		if ((getConstituent() == null)||(getConstituent().getSlogan() == null)) return null;
    	return getConstituent().getSlogan();
    }
    /**
     * The display is based on ConstituentTree:getTreeCellRendererComponentCIN
     */
    public String toString() {
    	String result;
		if(getConstituent() == null){
			if(DEBUG) System.err.println("ConstituentsIDNodeModel: toString null const");
			return __("Unknown!");
		}
		if ((getConstituent().getGivenName() == null) || 
				(getConstituent().getGivenName().equals("")))
			result = getConstituent().getSurname();
		else result = getConstituent().getSurname()+", "+getConstituent().getGivenName();    
		return result+ " "+getConstituent().email+ " ::"+ getConstituent().getSlogan();
    }
    public void populate() {
    	if (DEBUG) System.err.println("ConstituentsIDNode: populate this="+this);
		setChildren(new ConstituentsPropertyNode[0]);
		D_Constituent c = D_Constituent.getConstByLID(getConstituent().getC_LID(), true, false);
		if (c == null) { 
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address for: "+getConstituent().getC_LID());
	    	if (_DEBUG) System.err.println("ConstituentsIDNode: populate null c or address: "+c);
		}
		int identities_size = 0;
		D_Organization org = null;
		org = D_Organization.getOrgByLID_NoKeep(c.getOrganizationLID(), true);
		if (c.address != null) {
			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+c.address.length);
			for ( int i = 0 ; i < c.address.length; i ++ ) {
				if (DEBUG) System.out.println("ConstituentsModel: populate: addresses #"+i+" -> "+c.address[i]);
	    		D_OrgParam fe = c.address[i].field_extra;
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 1 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			fe = org.getFieldExtra(c.address[i].field_extra_GID);
	    		}
	    		if (fe == null) {
	    			if (_DEBUG) System.out.println("ConstituentsModel: populate: addresses 2 null fe #"+i+" -> "+c.address[i].field_extra_GID);
	    			continue;
	    		}
	    		if (fe.partNeigh > 0) {
	    			if (DEBUG) System.out.println("ConstituentsModel: populate: addresses 3 neigh fe #"+i+" - "+fe.partNeigh+": -> "+fe);
	    			continue;
	    		}
	    		identities_size ++;
	    		D_OID oid = D_OID.getBySequence(fe.oid);
	    		String value;
	    		Object obj;
	    		obj = c.address[i].value; 
	    		if (obj != null) value = obj.toString();else value = null;
	    		ConstituentProperty data = new ConstituentProperty();
	    		data.value = value;
	    		data.label = fe.label; 
	    		if (oid != null) {
		    		data.OID_name = oid.OID_name; 
		    		data.OID = oid.sequence; 
		    		data.explain = oid.explanation; 
	    		}
	    		populateChild(new ConstituentsPropertyNode(data, this),0);
	    	}
		}
		if ( fixed_fields >= 1) {
    		ConstituentProperty data = new ConstituentProperty();
    		data.value = this.getConstituent().email;
    		data.label = __("Email");//;Util.getString(identities.get(i).get(1));
    		populateChild(new ConstituentsPropertyNode(data, this),0);			
		}
		if( fixed_fields >= 2){
    		ConstituentProperty data = new ConstituentProperty();
    		data.label = __("Submitter");
    		String subm_ID = this.getConstituent().submitter_ID;
    		data.value = subm_ID;
    		long s_ID = Util.lval(subm_ID, -1);
    		if (s_ID > 0) {
    			D_Constituent wc = D_Constituent.getConstByLID(subm_ID, true, false);
    			wc.loadNeighborhoods(D_Constituent.EXPAND_ONE);
    			data.value = wc.getSurname()+", "+wc.getForename()+" <"+wc.getEmail()+">";
    			if ((wc.getNeighborhood()!=null) && (wc.getNeighborhood().length>0))
    				data.value += "("+wc.getNeighborhood()[0].getName_division()+":"+wc.getNeighborhood()[0].getName()+")";
    		} else {
    			data.value = "-";
    		}
   			populateChild(new ConstituentsPropertyNode(data, this),0);
		}
    	if( (fixed_fields+identities_size)!=getNchildren()) setNChildren(identities_size+ fixed_fields);
    	model.updateCensusStructure(model, getPath());
   }
	public static void advertise(ConstituentsIDNode can, String orgGID) {
		String hash = D_Constituent.getGIDHashFromGID(can.getConstituent().getC_GID());
		String org_hash = D_Organization.getOrgGIDHashGuess(orgGID);
		ClientSync.addToPayloadFix(net.ddp2p.common.streaming.RequestData.CONS, hash, org_hash, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}
	public void toggle_block() { 
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.getConstituent().getC_LID(), true, true);
			boolean blocked = constit.toggleBlock();
			constit.storeRequest();
			constit.releaseReference();
			this.getConstituent().blocked = blocked;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public void toggle_broadcast() {
		try {
			D_Constituent constit = D_Constituent.getConstByLID(this.getConstituent().getC_LID(), true, true);
			boolean broadcast = constit.toggleBroadcast();
			constit.storeRequest();
			constit.releaseReference();
			this.getConstituent().broadcast = broadcast;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	public D_Constituent zapp() {
		D_Constituent constit = D_Constituent.zapp(this.getConstituent().getC_LID());
		return constit;
	}
	public ConstituentData getConstituent() {
		return constituent;
	}
	public void setConstituent(ConstituentData constituent) {
		this.constituent = constituent;
	}
}

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
package net.ddp2p.common.hds;
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
/**
 * class SyncAnswer is to be replaced with ASNSyncPayload
 * @author silaghi
 *
 */
public
class SyncAnswer extends ASNSyncPayload{
	public SyncAnswer(){
		super();
	}
	public SyncAnswer(Calendar _upToDate, String globalID){
		super(_upToDate, globalID);
	}
	@Override
	public SyncAnswer decode(Decoder decoder) throws ASN1DecoderFail {
		return (SyncAnswer)super.decode(decoder);
	}
	@Override
	public Encoder getEncoder(){
		return super.getEncoder();
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasConstituent(String gid, String creation_date) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasWitness(String gid) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasNeighborhod(String gid) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasMotion(String gid) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasJustification(String gid) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasOrg(String gid) {
		return false;
	}
	/**
	 * TODO the following methods should be implemented to enable detection of whether a data 
	 * was already added to the answer (either in the requested or in the list of orgs_data), to not add it twice! Any taker?
	 */
	public boolean hasSignature(String gid, String creation_date) {
		return false;
	}
	public String elements() {
		return 
				(((tables!=null) && (tables.tables!=null) && (tables.tables.length>0) && (tables.tables[0].rows!=null))?
						("p="+this.tables.tables[0].rows.length):"")+
				((orgData!=null)?(" o="+this.orgData.length):"")+
				((this.requested!=null)?(
				" o="+this.requested.orgs.size()+
				" c="+this.requested.cons.size()+
				" n="+this.requested.neig.size()+
				" m="+this.requested.moti.size()+
				" j="+this.requested.just.size()+
				" s="+this.requested.sign.size()
				):"");
	}
	public boolean hasPeer(String gid, String string) {
		return false; 
	}
}

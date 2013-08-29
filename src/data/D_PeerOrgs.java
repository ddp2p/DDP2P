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

package data;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
public class D_PeerOrgs extends ASNObj{
	public String org_name; //OPT
	public String global_organization_IDhash; //OPT
	public String global_organization_ID; //OPT
	
	//not sent
	public boolean served;
	public String last_sync_date;
	public long organization_ID;
	
	public D_PeerOrgs(){}
	public String toString(){
		return "PeerOrg[org_name="+org_name+" IDhash="+((global_organization_IDhash==null)?"null":("\""+global_organization_IDhash+"\""))+" orgID="+global_organization_ID+"]";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(org_name!=null) enc.addToSequence(new Encoder(org_name).setASN1Type(DD.TAG_AC0));
		if(global_organization_IDhash!=null) enc.addToSequence(new Encoder(global_organization_IDhash).setASN1Type(DD.TAG_AC1));
		if(global_organization_ID!=null) enc.addToSequence(new Encoder(global_organization_ID).setASN1Type(DD.TAG_AC2));
		return enc;
	}
	@Override
	public D_PeerOrgs decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d=dec.getContent();
		if(d.getTypeByte()==DD.TAG_AC0) org_name = d.getFirstObject(true).getString();
		if(d.getTypeByte()==DD.TAG_AC1) global_organization_IDhash = d.getFirstObject(true).getString();
		if(d.getTypeByte()==DD.TAG_AC2) global_organization_ID = d.getFirstObject(true).getString();
		if(d.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra objects in PeerOrgs");
		return this;
	}
    public D_PeerOrgs instance() {
        return new D_PeerOrgs();
    }
}

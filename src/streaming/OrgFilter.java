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

package streaming;

import java.util.ArrayList;
import java.util.Calendar;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
public
class OrgFilter extends ASNObj{
	String version = "1";
	public String orgGID; //OPT [AC2] PrintableString
	public String orgGID_hash; //[AC3]
	private Calendar last_sync_date;
	private String _last_sync_date;
	String[] motions; //[Priv 4] PrintableString OPTIONAL
	boolean motions_excluding;
	String[] plugins; // [APPLIC 2] SEQ OF PrintableString OPT
	boolean plugins_excluding;
	public String toString(){
		return "[OrgFilter ver="+version+"] orgID="+orgGID+" motions="+motions+"-"+motions_excluding+" plugins="+plugins+"-"+plugins_excluding;
	}
	public OrgFilter instance(){
		return new OrgFilter();
	}
	public void setDate(Calendar lsd){
		last_sync_date = lsd;
		_last_sync_date = Encoder.getGeneralizedTime(lsd);
	}
	public void setGT(String lsd){
		_last_sync_date = lsd;
		last_sync_date = Util.getCalendar(lsd);
	}
	public Encoder getEncoder(){
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		if(orgGID!=null) enc.addToSequence(new Encoder(orgGID).setASN1Type(DD.TAG_AC2));
		if(orgGID_hash!=null) enc.addToSequence(new Encoder(orgGID_hash).setASN1Type(DD.TAG_AC3));
		if(last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if(motions!=null){
			Encoder encM = new Encoder().initSequence();
			for (int i = 0; i<motions.length; i++) {
				encM.addToSequence(new Encoder(motions[i])
				.setASN1Type(DD.TYPE_MotionID));
			}
			enc.addToSequence(encM);
		}
		enc.addToSequence(new Encoder(motions_excluding));
		if(plugins!=null){
			Encoder encP = new Encoder().initSequence();
			for (int i = 0; i<plugins.length; i++) {
				encP.addToSequence(new Encoder(plugins[i])
				.setASN1Type(Encoder.TAG_PrintableString));
			}
			enc.addToSequence(encP.setASN1Type(DD.TAG_AC2));
		}
		enc.addToSequence(new Encoder(plugins_excluding));
		return enc;
	}
	public OrgFilter decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		version = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==DD.TAG_AC2) orgGID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3) orgGID_hash = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime) setDate(dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType());
		if(dec.getTypeByte()==Encoder.TYPE_SEQUENCE) {
			Decoder d_m = dec.getFirstObject(true, Encoder.TYPE_SEQUENCE);
			ArrayList<String> motions = new ArrayList<String>();
			for(;;) {
				Decoder c_m = d_m.getFirstObject(true, DD.TYPE_TableName);
				if(c_m == null) break;
				motions.add(c_m.getString(DD.TYPE_MotionID));
			}
			this.motions = motions.toArray(new String[]{});
		}else motions = null;
		motions_excluding = dec.getFirstObject(true).getBoolean();
		if(dec.getTypeByte()==DD.TAG_AC2) {
			Decoder d_m = dec.getFirstObject(true, DD.TAG_AC2);
			ArrayList<String> plugins = new ArrayList<String>();
			for(;;) {
				Decoder c_m = d_m.getFirstObject(true, Encoder.TAG_PrintableString);
				if(c_m == null) break;
				plugins.add(c_m.getString(DD.TYPE_MotionID));
			}
			this.plugins = plugins.toArray(new String[]{});
		}else plugins = null;
		plugins_excluding = dec.getFirstObject(true).getBoolean();
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		return this;
	}
}
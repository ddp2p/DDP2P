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

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

public class HashOrg extends ASNObj{
	public String global_organization_ID;
	public String global_creator_ID;
	public String name;
	public String description;
	public String instructions_registration;
	public String instructions_new_motions; 
	public String category;
	public String creation_date;
	public String name_organization;
	public String name_forum;
	public String name_motion;
	public String name_justification;
	public String languages[];
	public String default_scoring_options[];
	public String certification_methods[];
	public HashFields fields[];
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID).setASN1Type(DD.TAG_AC0));
		if(global_creator_ID!=null)enc.addToSequence(new Encoder(global_creator_ID).setASN1Type(DD.TAG_AC1));
		if(name!=null)enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AC2));
		if(description!=null)enc.addToSequence(new Encoder(description).setASN1Type(DD.TAG_AC3));
		if(instructions_registration!=null)enc.addToSequence(new Encoder(instructions_registration).setASN1Type(DD.TAG_AC4));
		if(instructions_new_motions!=null)enc.addToSequence(new Encoder(instructions_new_motions).setASN1Type(DD.TAG_AC5));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC6));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		if(name_organization!=null)enc.addToSequence(new Encoder(name_organization).setASN1Type(DD.TAG_AC8));
		if(name_forum!=null)enc.addToSequence(new Encoder(name_forum).setASN1Type(DD.TAG_AC9));
		if(name_motion!=null)enc.addToSequence(new Encoder(name_motion).setASN1Type(DD.TAG_AC10));
		if(name_justification!=null)enc.addToSequence(new Encoder(name_justification).setASN1Type(DD.TAG_AC11));
		if(languages!=null)enc.addToSequence(Encoder.getStringEncoder(languages, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC12));
		if(default_scoring_options!=null)enc.addToSequence(Encoder.getStringEncoder(default_scoring_options, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC13));
		if(certification_methods!=null)enc.addToSequence(Encoder.getStringEncoder(certification_methods, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC14));
		if(fields!=null)enc.addToSequence(Encoder.getEncoder(fields).setASN1Type(DD.TAG_AC15));
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		return null;
	}
}

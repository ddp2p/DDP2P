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
package net.ddp2p.common.table;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
public class HashConstituent  extends ASNObj {
	public String global_constituent_ID;
	public String global_constituent_ID_hash;
	public String global_neighborhood_ID;
	public String global_organization_ID;
	public String hash_constituent_alg;
	public String email;
	public boolean external=false;
	public String forename;
	public String[] languages;
	public String name;
	public String slogan;
	public byte[] picture;
	public String creation_date;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(external).setASN1Type(DD.TAG_AC0));
		if(global_constituent_ID!=null) enc.addToSequence(new Encoder(global_constituent_ID).setASN1Type(DD.TAG_AC1));
		if(global_constituent_ID_hash!=null) enc.addToSequence(new Encoder(global_constituent_ID_hash).setASN1Type(DD.TAG_AC2));
		if(global_neighborhood_ID!=null) enc.addToSequence(new Encoder(global_neighborhood_ID).setASN1Type(DD.TAG_AC3));
		if(global_organization_ID!=null) enc.addToSequence(new Encoder(global_organization_ID).setASN1Type(DD.TAG_AC4));
		if(hash_constituent_alg!=null) enc.addToSequence(new Encoder(hash_constituent_alg).setASN1Type(DD.TAG_AC5));
		if(email!=null) enc.addToSequence(new Encoder(email).setASN1Type(DD.TAG_AC6));
		if(forename!=null) enc.addToSequence(new Encoder(forename).setASN1Type(DD.TAG_AC7));
		if(languages!=null)enc.addToSequence(Encoder.getStringEncoder(languages, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC8));
		if(name!=null) enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AC9));
		if(slogan!=null) enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC10));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC11));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC12));
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		return null;
	}
}

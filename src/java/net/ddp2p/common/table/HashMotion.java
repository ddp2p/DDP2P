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
public class HashMotion  extends ASNObj {
	public String enhances_global_motion_ID;
	public String format_title_type;
	public String format_text_type;
	public String motion_title;
	public String motion_text;
	public String global_constituent_ID;
	public String global_organization_ID;
	public String creation_date;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(enhances_global_motion_ID!=null) enc.addToSequence(new Encoder(enhances_global_motion_ID)).setASN1Type(DD.TAG_AC1);
		if(format_title_type!=null) enc.addToSequence(new Encoder(format_title_type)).setASN1Type(DD.TAG_AC2);
		if(format_text_type!=null) enc.addToSequence(new Encoder(format_text_type)).setASN1Type(DD.TAG_AC3);
		if(motion_title!=null) enc.addToSequence(new Encoder(motion_title)).setASN1Type(DD.TAG_AC4);
		if(motion_text!=null) enc.addToSequence(new Encoder(motion_text)).setASN1Type(DD.TAG_AC5);
		if(global_constituent_ID!=null) enc.addToSequence(new Encoder(global_constituent_ID)).setASN1Type(DD.TAG_AC6);
		if(global_organization_ID!=null) enc.addToSequence(new Encoder(global_organization_ID)).setASN1Type(DD.TAG_AC7);
		if(creation_date!=null) enc.addToSequence(new Encoder(creation_date)).setASN1Type(DD.TAG_AC8);
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		return null;
	}
}

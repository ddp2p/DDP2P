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
package net.ddp2p.common.util;
import java.util.regex.Pattern;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
public class DD_Slogan extends ASNObj implements StegoStructure {
	String version = "1";
	String slogan;
	@Override
	public void saveSync() throws P2PDDSQLException {
		save();
	}
	@Override
	public void save() throws P2PDDSQLException {
		D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
		me = D_Peer.getPeerByPeer_Keep(me);
		me.setSlogan(slogan);
		me.setCreationDate();
		me.storeAct();
		me.releaseReference();
	}
	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		Decoder d = new Decoder(asn1);
		decode(d);
	}
	@Override
	public byte[] getBytes() {
		return encode();
	}
	@Override
	public String getNiceDescription() {
		return slogan;
	}
	@Override
	public String getString() {
		return "DD_Slogan:"+version+" "+slogan;
	}
	@Override
	public boolean parseAddress(String content) {
		String s[] = content.split(Pattern.quote(" "), 2);
		if (s.length > 1) slogan = s[1];
		return true;
	}
	@Override
	public short getSignShort() {
		return DD.STEGO_SLOGAN;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(slogan));
		return enc;
	}
	@Override
	public DD_Slogan decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getString();
		slogan = d.getFirstObject(true).getString();
		return this;
	}
}

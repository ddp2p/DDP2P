package net.ddp2p.common.data;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;

/**
 * A class to describe ASN serializable icons to be stored with a constituent
 * @author msilaghi
 *
 */
public class IconObject extends net.ddp2p.ASN1.ASNObj {
	private byte[] image = null;
	private int id = -1;
	private String url = null;
	public IconObject() {}
	public IconObject(byte[] _image, int _id, String _url) {
		setImage(_image);
		setID(_id);
		setURL(_url);
	}
	//@Override
	public static byte getASN1Type() {
		return DD.TAG_AC10;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if (getID() >= 0) enc.addToSequence(new Encoder(getID()).setASN1Type(DD.TAG_AP0));
		if (getURL() != null) enc.addToSequence(new Encoder(getURL()).setASN1Type(DD.TAG_AP1));
		if (getImage() != null) enc.addToSequence(new Encoder(getImage()).setASN1Type(DD.TAG_AP2));
		return enc.setASN1Type(getASN1Type());
	}

	@Override
	public IconObject decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if (d.getTypeByte() == DD.TAG_AP0) {
			setID(d.getFirstObject(true).getInteger(DD.TAG_AP0).intValue());
		}
		if (d.getTypeByte() == DD.TAG_AP1) {
			setURL(d.getFirstObject(true).getString(DD.TAG_AP1));
		}
		if (d.getTypeByte() == DD.TAG_AP2) {
			setImage(d.getFirstObject(true).getBytes(DD.TAG_AP2));
		}
		if (d.getTypeByte() != 0) throw new net.ddp2p.ASN1.ASNLenRuntimeException("Extra Objects");
		return this;
	}
	public boolean empty() {
		return getImage() == null && getURL() == null && getID() < 0;
	}
	public byte[] getImage() {
		return image;
	}
	public void setImage(byte[] image) {
		this.image = image;
	}
	public int getID() {
		return id;
	}
	public void setID(int id) {
		this.id = id;
	}
	public String getURL() {
		return url;
	}
	public void setURL(String url) {
		this.url = url;
	}
}
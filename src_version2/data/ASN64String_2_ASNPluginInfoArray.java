package data;

import hds.ASNPluginInfo;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class ASN64String_2_ASNPluginInfoArray extends ASNObj {
	private ASNPluginInfo[] _plugin_info;
	public ASN64String_2_ASNPluginInfoArray(ASNPluginInfo[] pi) {
		set_PluginInfo(pi);
	}
	public ASN64String_2_ASNPluginInfoArray(Decoder dec) throws ASN1DecoderFail {
		decode(dec);
	}
	public ASN64String_2_ASNPluginInfoArray(String _plugin_info) throws ASN1DecoderFail {
		if (_plugin_info == null) return;
		try {
			decode(new Decoder(Util.byteSignatureFromString(_plugin_info)));
		}
		catch (ASN1DecoderFail df) {
			throw df;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ASN1DecoderFail(e.getLocalizedMessage());
		}
	}
	@Override
	public Encoder getEncoder() {
		return Encoder.getEncoder(get_PluginInfo());
	}

	@Override
	public ASN64String_2_ASNPluginInfoArray decode(Decoder dec) throws ASN1DecoderFail {
		dec.getSequenceOf(ASNPluginInfo.getASN1Type(), new ASNPluginInfo[0], new ASNPluginInfo());
		return this;
	}

	public String get_PluginInfoString() {
		if (this.get_PluginInfo() == null) return null;
		return Util.stringSignatureFromByte(encode());
	}
	public ASNPluginInfo[] get_PluginInfo() {
		return _plugin_info;
	}

	public void set_PluginInfo(ASNPluginInfo[] _plugin_info) {
		this._plugin_info = _plugin_info;
	}
	
}
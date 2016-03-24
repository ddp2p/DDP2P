package net.ddp2p.common.data;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.util.Util;

class ASN64String_2_StringArray extends ASNObj {
		private String [] strs_array;
		/**
		 * This is available only if the constructor provided it.
		 * It is used only for debug messages
		 */
		private String input;
		public ASN64String_2_StringArray (String [] _strs) {
			setStrsArray(_strs);
		}
		public ASN64String_2_StringArray(Decoder dec) throws ASN1DecoderFail {
			decode(dec);
		}
		public ASN64String_2_StringArray(Decoder dec, String in) throws ASN1DecoderFail {
			input = in;
			decode(dec);
		}
		/**
		 * null for input "null"
		 * @param in
		 * @throws ASN1DecoderFail
		 */
		public ASN64String_2_StringArray(String in) throws ASN1DecoderFail {
			input = in;
			if (in == null || in.equals("null")) return;
			byte[] data = Util.byteSignatureFromString(in);
//			try {
				if (data == null)  throw new ASN1DecoderFail("Wrong bas64 for String Array");
				Decoder dec = new Decoder(data);
				decode(dec);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}
		public byte getASN1Tag() {
			return Encoder.TAG_SEQUENCE;
		}
		@Override
		public Encoder getEncoder() {
			return Encoder.getStringEncoder(getStrsArray(), Encoder.TAG_UTF8String).setASN1Type(getASN1Tag());
		}

		@Override
		public ASN64String_2_StringArray decode(Decoder dec) throws ASN1DecoderFail {
			if (dec.getTypeByte() != getASN1Tag()) {
				 System.err.println("OrgConcepts:stringArrayFromString: parsing \""+dec+"\"");
				 System.err.println("OrgConcepts:stringArrayFromString: parsing data:\"["+dec.getMSGLength()+"]=" + input);
				throw new ASN1DecoderFail("Wrong tag for String Array");
			}
			setStrsArray(dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String));
			return this;
		}
		public String [] getStrsArray() {
			return strs_array;
		}
		public void setStrsArray(String [] strs_array) {
			this.strs_array = strs_array;
		}
		/**
		 * Returns an encoded String (ASN1 + base64)
		 * @return
		 */
		public String getEncodedStr() {
			String result = null;
			if (strs_array == null) return null;
			byte[] data = encode(); //Encoder.getStringEncoder(in,Encoder.TAG_UTF8String).getBytes();
			result = Util.stringSignatureFromByte(data);
			return result;
		}
		
	}
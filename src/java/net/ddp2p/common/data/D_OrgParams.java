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
package net.ddp2p.common.data;
import java.util.ArrayList;
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.util.Util;
public
class D_OrgParams extends ASNObj {
	public int certifMethods; 
	public String hash_org_alg = net.ddp2p.common.table.organization.hash_org_alg_crt; 
	public Calendar creation_time;
	public String creator_global_ID;
	/**
	 * 0 for all constituents having weight 0 or 1
	 */
	public int mWeightsType = net.ddp2p.common.table.organization.WEIGHTS_TYPE_DEFAULT;
	public int mWeightsMax = net.ddp2p.common.table.organization.WEIGHTS_MAX_DEFAULT;
	public String category; 
	public byte[] certificate; 
	public String[] default_scoring_options; 
	public String instructions_new_motions; 
	public String instructions_registration; 
	public String description; 
	public String[] languages; 
	public byte[] icon;
	public D_OrgParam[] orgParam; 
	public String toString() {
		return "Org Params: ["+
		";\n     mWeightsType="+mWeightsType+
		";\n     mWeightMax="+mWeightsMax+
		";\n     certifMethods="+Util.nullDiscrim(certifMethods)+
		";\n     hash_org_alg="+Util.nullDiscrim(hash_org_alg)+
		";\n     creation_time="+((creation_time==null)?"null":Encoder.getGeneralizedTime(creation_time))+
		";\n     creator="+Util.trimmed(creator_global_ID)+
		";\n     category="+Util.nullDiscrim(category)+
		";\n     certificate="+Util.nullDiscrim(Util.byteToHex(certificate, ":"))+
		";\n     icon="+Util.nullDiscrim(Util.trimmed(Util.byteToHex(icon, ":")))+
		";\n     default_scoring_options=["+Util.nullDiscrim(Util.concat(default_scoring_options, ","))+"]"+
		";\n     instructions_new_motions="+Util.nullDiscrim(instructions_new_motions)+
		";\n     instructions_registration="+Util.nullDiscrim(instructions_registration)+
		";\n     description="+Util.nullDiscrim(description)+
		";\n     languages=["+Util.concat(languages, ":")+"]"+
		";\n     orgParam=["+Util.concat(orgParam, "\t")+"]"+
		 "\n  ]";
	}
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) { 
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgParams: "+this);
		boolean DEBUG = D_Organization.DEBUG;
		Encoder enc = new Encoder().initSequence();
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px1: "+D_Organization.hashEnc(enc));
		enc.addToSequence(new Encoder(certifMethods));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px2: "+D_Organization.hashEnc(enc));
		if(hash_org_alg != null) enc.addToSequence(new Encoder(hash_org_alg, false).setASN1Type(DD.TAG_AC0));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px3: "+D_Organization.hashEnc(enc));
		if(creation_time != null) enc.addToSequence(new Encoder(creation_time).setASN1Type(DD.TAG_AC1));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px4: "+D_Organization.hashEnc(enc));
		if(creator_global_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, creator_global_ID);
			enc.addToSequence(new Encoder(repl_GID, false).setASN1Type(DD.TAG_AC2));
			if (DEBUG) System.out.println("OrgParamSignHash = 3_px5: "+D_Organization.hashEnc(enc));
		}
		if(category != null) enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC3));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px6: "+D_Organization.hashEnc(enc));
		if(certificate != null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC4));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px7: "+D_Organization.hashEnc(enc));
		if(default_scoring_options != null) enc.addToSequence(Encoder.getStringEncoder(default_scoring_options, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px8: "+D_Organization.hashEnc(enc));
		if(instructions_new_motions != null) enc.addToSequence(new Encoder(instructions_new_motions).setASN1Type(DD.TAG_AC6));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_px9: "+D_Organization.hashEnc(enc));
		if(instructions_registration != null) enc.addToSequence(new Encoder(instructions_registration).setASN1Type(DD.TAG_AC7));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p10: "+D_Organization.hashEnc(enc));
		if(description != null) enc.addToSequence(new Encoder(description).setASN1Type(DD.TAG_AC10));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p11: "+D_Organization.hashEnc(enc));
		if (languages != null) enc.addToSequence(Encoder.getStringEncoder(languages, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p12: "+D_Organization.hashEnc(enc));
		if (orgParam != null) {
			D_OrgParam[] _orgParam = D_Organization.getNonEphemeral(orgParam);
			if (_orgParam != null && _orgParam.length > 0) {
				enc.addToSequence(Encoder.getEncoder(_orgParam, dictionary_GIDs).setASN1Type(DD.TAG_AC9));
				if (DEBUG) System.out.println("OrgParamSignHash = 3_p13: "+D_Organization.hashEnc(enc));
			}
		}
		if (mWeightsType != net.ddp2p.common.table.organization.WEIGHTS_TYPE_DEFAULT)
			enc.addToSequence(new Encoder(mWeightsType).setASN1Type(DD.TAG_AC11));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p14: "+D_Organization.hashEnc(enc));
		if (mWeightsMax != net.ddp2p.common.table.organization.WEIGHTS_MAX_DEFAULT)
			enc.addToSequence(new Encoder(mWeightsMax).setASN1Type(DD.TAG_AC12));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p15: "+D_Organization.hashEnc(enc));
		if (icon != null) enc.addToSequence(new Encoder(icon).setASN1Type(DD.TAG_AC14));
		if (DEBUG) System.out.println("OrgParamSignHash = 3_p16: "+D_Organization.hashEnc(enc));
		if (ASNSyncRequest.DEBUG)System.out.println("Encoded OrgParams: "+this);
		return enc;
	}
	@Override
	public D_OrgParams decode(Decoder decoder) throws ASN1DecoderFail {
		if (ASNSyncRequest.DEBUG) System.out.println("DEcoding OrgParams: "+this);
		Decoder dec = decoder.getContent();
		certifMethods = dec.getFirstObject(true).getInteger().intValue();
		if(dec.getTypeByte()==DD.TAG_AC0) hash_org_alg = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC1) creation_time = dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		if(dec.getTypeByte()==DD.TAG_AC2) creator_global_ID = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC3) category = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC4) certificate= dec.getFirstObject(true).getBytesAnyType();
		if(dec.getTypeByte()==DD.TAG_AC5) default_scoring_options=dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC6) instructions_new_motions = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC7) instructions_registration = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC10) description = dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC8) languages=dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC9) orgParam = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_OrgParam[]{}, new D_OrgParam());
		if(dec.getTypeByte()==DD.TAG_AC11) mWeightsType = dec.getFirstObject(true).getInteger(DD.TAG_AC11).intValue();
		if(dec.getTypeByte()==DD.TAG_AC12) mWeightsMax = dec.getFirstObject(true).getInteger(DD.TAG_AC12).intValue();
		if(dec.getTypeByte()==DD.TAG_AC14) icon = dec.getFirstObject(true).getBytesAnyType();
		if(dec.getFirstObject(false) != null) {
			if (ASNSyncRequest.DEBUG)  System.out.println("DEcoding OrgParams: Extra objects!");
		}
		if(ASNSyncRequest.DEBUG)System.out.println("DEcoded OrgParams: "+this);
		return this;
	}
	/**
	 * TODO
	 * should probably construct a full object and get the deep clone of all its data....
	 * @return
	 */
	public D_OrgParams getClone() {
		return this;
	}
}

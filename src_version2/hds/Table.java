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

package hds;

import java.util.ArrayList;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;
/**
TableName := IMPLICIT [PRIVATE 0] UTF8String;
FieldName := IMPLICIT [PRIVATE 2] UTF8String;
FieldType := IMPLICIT [PRIVATE 3] UTF8String;
Table := SEQUENCE {
	name TableName,
	fields SEQUENCE OF FieldName,
	fieldTypes SEQUENCE OF FieldType,
	rows SEQUENCE OF SEQUENCE OF NULLOCTETSTRING;
}
 */

public class Table extends ASNObj { 
	public String name; // TableName::= [PRIVATE 0] IMPLICIT UTF8String
	public String fields[]; // SEQUENCE OF FieldName ::= [PRIVATE 2] IMPLICIT UTF8String
	public String fieldTypes[]; // SEQUENCE OF FieldType ::= [PRIVATE 3] IMPLICIT PrintableString
	public byte[] rows[][]; // CHOICE NULL, OCTET_STRING
	public String toString() {
		String result = "[TABLE]name="+name;
		result+="\n fields=";
		for(int k=0; k<fields.length;k++)
			result+="\n  ["+k+"]"+fields[k];
		result+="\n fieldTypes=";
		for(int k=0; k<fieldTypes.length;k++)
			result+="\n  ["+k+"]"+fieldTypes[k];
		result+="\n rows=";
		for(int k=0; k<rows.length;k++) {
			result+="\n  ["+k+"]";
				result += printRow(rows[k]);
		}
		return result;
	}
	public String toSummaryString() {
		String result = "[TABLE]name="+name;
		//result+="\n fields=";
		//for(int k=0; k<fields.length;k++) result+="\n  ["+k+"]"+fields[k];
		//result+="\n fieldTypes=";
		//for(int k=0; k<fieldTypes.length;k++) result+="\n  ["+k+"]"+fieldTypes[k];
		result+="\n rows=";
		for(int k=0; k<rows.length;k++) {
			result += "\n  ["+k+"]="+Util.trimmedStrObj(rows[k][1])+" slogan="+Util.trimmedStrObj(rows[k][2])+" addr="+Util.trimmedStrObj(rows[k][3]);
			//result += printRow(rows[k]);
		}
		return result;
	}		
	public static String printRow(byte[][] row) {
		String result="";
		if(row==null) return result;
		if(row.length>0){
			int peerID = 0;
			try{
				peerID = new Decoder(row[0]).getInteger(Encoder.TAG_INTEGER).intValue();
				result+="\n   \t["+0+"]"+"::"+peerID;
			}catch(Exception e){
				result+="\n   \t["+0+"]"+"::"+Util.trimmed(new String(row[0]));
			}
		}

		for(int i=1; i<row.length;i++)
			if(row[i]==null)
				result+="\n   \t["+i+"]"+"NULL";
			else{
				if(i==0){
				}else{
					// result+="\n   \t["+i+"]"+Util.byteToHex(rows[k][i]," ")+"::"+new String(rows[k][i]);
					result+="\n   \t["+i+"]"+"::"+Util.trimmed(new String(row[i]));
				}
			}
		return result;
	}
	/**
TableName := IMPLICIT [PRIVATE 0] UTF8String;
FieldName := IMPLICIT [PRIVATE 2] UTF8String;
FieldType := IMPLICIT [PRIVATE 3] UTF8String;
Table := SEQUENCE {
	name TableName,
	fields SEQUENCE OF FieldName,
	fieldTypes SEQUENCE OF FieldType,
	rows SEQUENCE OF SEQUENCE OF NULLOCTETSTRING;
}
	 */
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding Table: "+name);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name,DD.TAG_PP0));
		//
		Encoder fields_enc = new Encoder().initSequence();
		for(int k=0;k<fields.length;k++) fields_enc.addToSequence(new Encoder(fields[k],DD.TYPE_FieldName));
		enc.addToSequence(fields_enc);
		//
		Encoder fieldTypes_enc = new Encoder().initSequence();
		for(int k=0;k<fieldTypes.length;k++) fieldTypes_enc.addToSequence(new Encoder(fieldTypes[k],DD.TYPE_FieldType));
		enc.addToSequence(fieldTypes_enc);
		//
		Encoder rows_enc = new Encoder().initSequence();
		for(int k=0;k<rows.length;k++){
			Encoder row_enc = new Encoder().initSequence();
			for(int i=0; i<rows[k].length; i++)
				row_enc.addToSequence(new Encoder(rows[k][i]));
			rows_enc.addToSequence(row_enc);
		}
		enc.addToSequence(rows_enc);
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded Table: "+name);
		return enc;
	}
	public Table decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		name = dec.getFirstObject(true, DD.TAG_PP0).getString(DD.TAG_PP0);
		//
		Decoder fields_dec = dec.getFirstObject(true, Encoder.TYPE_SEQUENCE).getContent();
		ArrayList<String> fields_list = new ArrayList<String>();
		for(;;){
			Decoder d_t = fields_dec.getFirstObject(true, DD.TYPE_FieldName);
			if(d_t==null) break;
			fields_list.add(d_t.getString(DD.TYPE_FieldName));
		}
		fields = fields_list.toArray(new String[]{});
		//
		Decoder fieldTypes_dec = dec.getFirstObject(true, Encoder.TYPE_SEQUENCE).getContent();
		ArrayList<String> fieldTypes_list = new ArrayList<String>();
		for(;;){
			Decoder d_t = fieldTypes_dec.getFirstObject(true, DD.TYPE_FieldType);
			if(d_t==null) break;
			fieldTypes_list.add(d_t.getString(DD.TYPE_FieldType));
		}
		fieldTypes = fieldTypes_list.toArray(new String[]{});
		//
		Decoder rows_dec = dec.getFirstObject(true, Encoder.TYPE_SEQUENCE).getContent();
		ArrayList<byte[][]> rows_list = new ArrayList<byte[][]>();
		for(;;){
			Decoder d_row = rows_dec.getFirstObject(true, Encoder.TYPE_SEQUENCE);
			if(d_row==null) break;
			d_row=d_row.getContent();
			//System.err.println("Crt Row"+d_row.dumpHex());
			ArrayList<byte[]> row_list = new ArrayList<byte[]>();
			for(;;){
				Decoder d_f = d_row.getFirstObject(true);
				if(d_f==null) break;
				//System.err.println("Crt Object"+d_f.dumpHex());
				byte type=d_f.getTypeByte();
				if(type==Encoder.TAG_OCTET_STRING) row_list.add(d_f.getBytes());
				else if(type==Encoder.TAG_NULL) row_list.add(null);
				else throw new ASN1DecoderFail("Table Field has tag: "+type+" "+d_f.dumpHex(5));
			}
			rows_list.add(row_list.toArray(new byte[][]{}));
		}
		rows = rows_list.toArray(new byte[][][]{});
		//
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEcoding Table");
		return this;
	}
}
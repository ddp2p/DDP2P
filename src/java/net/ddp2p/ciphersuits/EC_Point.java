/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Marius C. Silaghi
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
package net.ddp2p.ciphersuits;

import java.math.BigInteger;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;

/**
 * Implements a point on an elliptic curve (with compression).
 * A null is considered to be a point at INFINITY
 * @author msilaghi
 *
 */
class EC_Point extends ASNObj implements AdditiveGroup{
	public static final EC_Point INFINITY = null;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	ECC _curve;
	public static ECC default_curve;
	boolean inf = false; // null is infinity
	boolean compressed = false;
	boolean compressed_y;
	BigInteger x;
	BigInteger y;
	public EC_Point(BigInteger x, BigInteger y, ECC curve) {
		this.x = x;
		this.y = y;
		this._curve = curve;
	}
	public EC_Point mul(BigInteger k) {
		return ECC.mul(this, k);
	}
	public EC_Point minus() {
		ECC curve = getCurve();
		return new EC_Point(x, curve.minus(y), curve);
		//return new ECP(x, curve.p.subtract(y).mod(curve.p), curve);
	}
	public EC_Point(BigInteger x, boolean b, ECC ec) {
		this.x = x;
		compressed=true;
		compressed_y = b;
		_curve = ec;
		decompress();
	}
	public EC_Point(ECC __curve, Decoder d) throws ASN1DecoderFail {
		_curve = __curve;
		decode(d);
	}
	public EC_Point(Decoder d) throws ASN1DecoderFail {
		decode(d);
	}
	public EC_Point(EC_Point x2) {
		x = new BigInteger(x2.x.toByteArray());
		y = new BigInteger(x2.getY().toByteArray());
		this.compressed = x2.compressed;
		this.compressed_y = x2.compressed_y;
		this._curve = x2._curve;
	}
	ECC getCurve(){
		if(_curve==null) return default_curve;
		return _curve;
	}
	public BigInteger getY() {
		if(inf) throw new RuntimeException("Wrong point at inf");
		if(y!=null) return y;
		if(compressed){
			decompress();
			if(y != null){
				return y;
			} else {
				System.out.println("EC_Point: getY not decompressing"+this);
			}
		}else{
			System.out.println("EC_Point: getY not compressed?"+this);
		}
		throw new RuntimeException("Wrong point");
	}
	public boolean getCompressedY() {
		compress();
		return this.compressed_y;
	}

	public void setEC(ECC curve, boolean global){
		if(global){
			default_curve = curve;
		}else
			this._curve = curve;
	}
	public void decompress() {
		if (y != null) {
			if (DEBUG) System.out.println("EC_Point: decompress  y not null: "+y);
			return;
		} else {
			if (DEBUG) System.out.println("EC_Point: decompress not null: "+this);
		}
		force_decompress();
	}
	/**
	 * Execute even if y already existed (overwriting it
	 */
	public void force_decompress() {
		if (DEBUG) System.out.println("EC_Point: force_decompress: start");
		if (! compressed) {
			System.out.println("EC_Point: decompressed.. not compressed");
			return;
		}
		if (getCurve() == null) throw new RuntimeException("Set EC!");
		y = getCurve().evaluate_y(x);
		if (DEBUG) System.out.println("EC_Point: force_decompress was: "+y);
		if ((compressed_y) && (! y.testBit(0))) {
			if (DEBUG) System.out.println("EC_Point: force_decompress <- "+y);
			y = getCurve().minus(y);
			if (DEBUG) System.out.println("EC_Point: force_decompress -> "+y);
			return;
		}
		if ((! compressed_y) && (y.testBit(0))) {
			if (DEBUG) System.out.println("EC_Point: .force_decompress <- "+y);
			y = getCurve().minus(y);
			if (DEBUG) System.out.println("EC_Point: .force_decompress -> "+y);
			return;
		}
	}
	public void compress() {
		if (inf || (y == null)) return;
		compressed_y = y.testBit(0);
		compressed = true;
	}
	public BigInteger getX() {
		return x;
	}
	public boolean equals(EC_Point a){
		if(!a.getX().equals(getX())) return false;
		if(!a.getY().equals(getY())) return false;
		return true;
	}
	public String toString(){
		if(inf) return "INFINITY";
		compress();
		return "("+Util.toString16(getX())+","+Util.toString16(y)+"/"+compressed_y+
				"\n\tPoint curve="+getCurve()+")";
	}
	public static String toString(EC_Point _x, ECC curve) {
		if (_x == null) return "null";
		if(_x.inf) return "INFINITY";
		_x.compress();
		return "("+Util.toString16(_x.getX())+","+Util.toString16(_x.y)+"/"+_x.compressed_y+
				((curve == _x.getCurve())?"":"\n\tPoint curve="+_x.getCurve())+")";
	}
	/**
	 * TAC_AC13
	 * @return
	 */
	public static byte getASN1TAG() {
		return DD.TAG_AC13;
	}
	@Override
	public Encoder getEncoder() {
		Encoder e = new Encoder().initSequence();
		compress();
		if (inf) {
			e.addToSequence(new Encoder(inf));
		}else{
			e.addToSequence(new Encoder(x));
			e.addToSequence(new Encoder(compressed_y));
		}
		return e.setASN1Type(getASN1TAG());
	}
	@Override
	public EC_Point decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		if (d.getFirstObject(false).getTypeByte() == Encoder.TAG_BOOLEAN) {
			inf = true;
			return this;
		}
		x = d.getFirstObject(true).getInteger();
		compressed_y = d.getFirstObject(true).getBoolean();
		compressed = true;
		if (DEBUG) System.out.println("EC_Point: decode: uncompressed yet is: "+this);
		if (getCurve() != null) decompress();
		if (DEBUG) System.out.println("EC_Point: decode: decompressed yep is: "+this);
		return this;
	}
	public boolean nullEnd() {
		boolean r = BigInteger.ZERO.equals(getY());
		if (DEBUG) System.out.println("nullEnd: "+r+" due to y="+getY());
		return r;
	}
	@Override
	public AdditiveGroup add(AdditiveGroup a1, AdditiveGroup a2) {
		return _curve.add((EC_Point)a1, (EC_Point	)a2);
	}
	@Override
	public AdditiveGroup getIdentity() {
		return EC_Point.INFINITY;
	}
	@Override
	public AdditiveGroup getInverse() {
		return minus();
	}
	@Override
	public boolean isIdentity() {
		return inf;
	}
}
/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.io.ByteArrayOutputStream;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.widget.ImageView;

public class PhotoUtil {

	@SuppressLint("NewApi")
	public static String BitmapToString(Bitmap bmp) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 100, baos);
		byte[] b = baos.toByteArray();
		String temp = Base64.encodeToString(b, Base64.DEFAULT);
		return temp;
	}

	public static byte[] BitmapToByteArray(Bitmap bmp, int quality) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, quality, baos);
		return baos.toByteArray();
	}

	public static void ClearImage(ImageView iv) {
		if (!(iv.getDrawable() instanceof BitmapDrawable))
			return;

		BitmapDrawable b = (BitmapDrawable) iv.getDrawable();
		if (b == null) return;
		Bitmap bi = b.getBitmap();
		if (bi == null) return;
		bi.recycle();
		iv.setImageDrawable(null);
	}

	public static Bitmap decodeSampledBitmapFromFile (String path, 
			 int reqWidth, int reqHeight) {
		final BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opt);
		
		opt.inSampleSize = calculateInSampleSize(opt, reqWidth, reqHeight);
		
		opt.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path, opt);
	}
	
	public static Bitmap decodeSampledBitmapFromResource (Resources res, int resId, 
			 int reqWidth, int reqHeight) {
		final BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, opt);
		
		opt.inSampleSize = calculateInSampleSize(opt, reqWidth, reqHeight);
		
		opt.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, opt);
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options opt,
			int reqWidth, int reqHeight) {
		final int height = opt.outHeight;
		final int width = opt.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqHeight);
			} else {
				inSampleSize = Math.round((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}
}

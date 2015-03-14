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
		b.getBitmap().recycle();
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

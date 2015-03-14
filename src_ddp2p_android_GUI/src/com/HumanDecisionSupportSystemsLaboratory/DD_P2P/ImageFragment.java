package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.app.ActionBar.LayoutParams;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageFragment extends DialogFragment {

	private ImageView iv;
	public static final String EXTRA_IMAGE_BYTE_ARRAY = "safe_profile_image";

	public static ImageFragment newInstance(byte[] imgByteArray) {
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_IMAGE_BYTE_ARRAY, imgByteArray);
		ImageFragment fragment = new ImageFragment();
		fragment.setArguments(args);
		fragment.setStyle(STYLE_NO_TITLE, 0);
		return fragment;

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (iv != null)
			PhotoUtil.ClearImage(iv);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		iv = new ImageView(getActivity());
		byte[] icon = getArguments().getByteArray(EXTRA_IMAGE_BYTE_ARRAY);
		Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0, icon.length - 1);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		iv.setLayoutParams(params);
		iv.setImageBitmap(bmp);
		return iv;
	}

}

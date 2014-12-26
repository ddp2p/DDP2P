/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
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
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

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
	public static final String EXTRA_IMAGE_BYTE_ARRAY = 
			"safe_profile_image";
	
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
        PhotoUtil.ClearImage(iv);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		iv = new ImageView(getActivity());		
		byte[] icon = getArguments().getByteArray(EXTRA_IMAGE_BYTE_ARRAY);
		Bitmap bmp = BitmapFactory.decodeByteArray(icon, 0, icon.length - 1);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		iv.setLayoutParams(params);
		iv.setImageBitmap(bmp);
		return iv;
	}
	
	
}

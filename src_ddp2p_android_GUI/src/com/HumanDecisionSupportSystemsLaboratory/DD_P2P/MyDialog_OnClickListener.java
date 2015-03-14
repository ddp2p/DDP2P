package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.content.DialogInterface;

abstract class MyDialog_OnClickListener implements DialogInterface.OnClickListener {
	Object ctx;
	MyDialog_OnClickListener (Object _ctx) {
		ctx = _ctx;
	}
	@Override
	public void onClick(DialogInterface dialog, int which) {
		_onClick(dialog, which);
	}

	abstract void _onClick(DialogInterface dialog, int which);
}
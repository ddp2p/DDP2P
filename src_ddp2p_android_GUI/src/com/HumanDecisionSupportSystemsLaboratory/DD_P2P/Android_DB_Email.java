package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.io.BufferedReader;
import java.io.File;

import android.app.Activity;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;

public
class Android_DB_Email implements net.ddp2p.common.config.Vendor_DB_Email {

	//add a new parameter to pass the context
	private Activity activity;
	
	//alter the constructor to allow passing context
	public Android_DB_Email(Activity act) {
		this.activity = act;
	}
	
	@Override
	public boolean db_copyData(File arg0, File arg1, BufferedReader arg2,
			String[] arg3, boolean arg4) {
		return false;
	}

	@Override
	public String[] extractDDL(File arg0, int arg1) {
		return null;
	}

	@Override
	public DB_Implementation get_DB_Implementation() {
		return new DB_Implementation_Android_SQLITE(activity, "deliberation-app.db");
	}

	@Override
	public void sendEmail(DD_IdentityVerification_Answer arg0) {
		
	}
}
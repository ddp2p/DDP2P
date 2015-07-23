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

import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;

import android.app.Activity;
import android.content.Context;

import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.updates.VersionInfo;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;

public
class Android_DB_Email implements net.ddp2p.common.config.Vendor_DB_Email {

	//add a new parameter to pass the context
	private Context activity;
	
	//alter the constructor to allow passing context
	public Android_DB_Email(Context act) {
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
        Application.directory_status = new Android_Directories_View();
        return new DB_Implementation_Android_SQLITE(activity, "deliberation-app.db");
	}

	@Override
	public void sendEmail(DD_IdentityVerification_Answer arg0) {
		
	}

    @Override
    public URL isWSVersionInfoService(String s) {
        return null;
    }

    @Override
    public VersionInfo getWSVersionInfo(URL url, String s, SK sk, Hashtable<Object, Object> objectObjectHashtable) {
        return null;
    }
}
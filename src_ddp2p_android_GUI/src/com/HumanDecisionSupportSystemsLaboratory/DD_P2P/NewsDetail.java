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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import net.ddp2p.common.data.D_News;
import net.ddp2p.common.util.Util;


public class NewsDetail extends Activity{

	private TextView titleTextView; 
	private TextView contentTextView;
	private String title;
	private String content;
	private String[] review;

    public String organization_LID;
    public String news_LID;
    public String news_title;
    public String news_body;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent i = this.getIntent();
        Bundle b = i.getExtras();

        // top panel setting
        organization_LID = b.getString(Orgs.O_LID);
        news_LID = b.getString(News.N_LID);
        news_title = b.getString(News.N_TITLE);
        news_body = b.getString(News.N_BODY);

		setContentView(R.layout.news_detail);

		titleTextView = (TextView) findViewById(R.id.news_detail_title);
		contentTextView = (TextView) findViewById(R.id.news_detail_content);

        long iLID = Util.lval(news_LID, -1);
        if (iLID > 0) {
            D_News n = D_News.getNewsByLID(iLID);
            titleTextView.setText(n.getTitleStrOrMy());
            contentTextView.setText(n.getNewsBodyStr());
        } else {
            titleTextView.setText(news_title);
            contentTextView.setText(news_body);
        }
	}

	
}

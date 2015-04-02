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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.Util;

public class News extends ListActivity {
	private static final String TAG = "news";
    public static final String N_LID = "NLID";
    public static final String N_TITLE = "NT";
    public static final String N_BODY = "NB";
	private String[] newsTitle;
	private ActionBar actionbar = null;

    private static int organization_position;
    private static String organization_LID;
    private static String organization_GIDH;
    private static String organization_name;
    long oLID;
    D_Organization org;

    public ArrayList<NewsItem> news = new ArrayList<NewsItem>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent i = this.getIntent();
        Bundle b = i.getExtras();

        // top panel setting
        organization_position = b.getInt(Orgs.O_ID);
        organization_LID = b.getString(Orgs.O_LID);
        organization_GIDH = b.getString(Orgs.O_GIDH);
        organization_name = b.getString(Orgs.O_NAME);

        oLID = Util.lval(organization_LID, -1);
        if (oLID <= 0) return;
        this.org = D_Organization.getOrgByLID(oLID, true, false);
        if (org == null) return;

		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);

        String motion_LID = null;
        String justif_LID = null;
        boolean hide_hidden = false;
        boolean order_creation = false;
        int LIMIT = 100;
        ArrayList<java.util.ArrayList<Object>> news_LIDs =
                D_News.getAllMotions(organization_LID, hide_hidden, motion_LID, justif_LID, LIMIT, order_creation);

        news = new ArrayList<NewsItem>();

        for (ArrayList<Object> nobs: news_LIDs) {
            long nLID = Util.lval(nobs.get(D_News.SELECT_ALL_NEWS_LID), -1);
            D_News news_obj = D_News.getNewsByLID(nLID);
            if (news_obj == null) continue;
            NewsItem news_item = new NewsItem();
            news_item.lid = Util.getStringID(nLID);
            news_item.title = news_obj.getTitleStrOrMy();
            news_item.content = news_obj.getNewsBodyStr();
            String date = Encoder.getGeneralizedTime(news_obj.getCreationDate());
            if (date != null)
                news_item.date_time = date.substring(0,4)+"-"+date.substring(4,6)+"-"+date.substring(6,8)+" "+date.substring(8,12);
            else news_item.date_time = Util.__("Unknown");
            if (news_item.content == null) news_item.content = Util.__("No News Object Body");

            net.ddp2p.common.data.D_Constituent constituent = news_obj.getConstituentForce();
            if (constituent != null) news_item.author = constituent.getNameOrMy();
            news.add(news_item);
        }
        if (news.size() == 0) {
            NewsItem news_item = new NewsItem();
            news_item.lid = null;
            news_item.title = Util.__("No News Item in this Organization!");
            news_item.content = Util.__("You can add news using the + button in the menu above!");
            news_item.author = Util.__("Hang Dong and Marius Silaghi");
            news_item.date_time = "2015-03-29 1800";
            news.add(news_item);
        }
        /*
		NewsItem news1 = new NewsItem();
		news1.title = "With Torres watching, Griezmann and Mandzukic power Atletico to victory";
		news1.content = "Atletico Madrid welcomed in 2015 and kept in the hunt to "
				+ "retain their La Liga title with a comfortable 3-1 victory over Levante "
				+ "at the Vicente Calderon on Saturday afternoon. "
				+ "It could not have been more vintage Atleti if they tried, with all three goals coming from headers."
				+ "The returning Fernando Torres watched on from the stands, "
				+ "and he will have liked what he saw as Los Rojiblancos were too strong for their opponents from start"
				+ " to finish. However, with the side currently purring in attack, "
				+ "especially with Antoine Griezmann and Mario Mandzukic showing an excellent "
				+ "understanding of each other, El Nino will have a job getting into the side.Griezmann "
				+ "seems spurred on by the pressure of competition for his place, and his two headers now m"
				+ "ean he is Atleti's top scorer in the league this season with eight. For such a diminutive "
				+ "figure, the Frenchman possesses incredible aerial prowess, hammering home his first before "
				+ "showing his predatory instinct to follow up as strike partner Mandzukic was spectacularly d"
				+ "enied by Levante keeper Diego Marino.After the visitors threatened a comeback, "
				+ "Diego Simeone's men battened down the hatches and rode the storm, before "
				+ "Diego Godin popped up with five minutes to spare to put the game to bed from Tiago's centre. "
				+ "Next up, Real Madrid.Player Ratings (1-10; 10=best. Players introduced after 70 minutes get no rating)"
				+ ":GK Miguel Angel Moya, 6 -- A quiet afternoon in which the only job"
				+ " Moya had was to pick the ball out of the back of his net. Could do little for "
				+ "the goal.";
		news1.author = "ESPN";
		NewsItem news2 = new NewsItem();
		news2.title = "FA Cup gives Arsene Wenger and Arsenal a chance to revive season";
		news2.content = "Arsene Wenger has a remarkable love affair with the FA Cup, winning it five times in his 18 years in charge at Arsenal. The Frenchman has lifted it in 1998, 2002, 2003, 2005 and again last May when they beat Sunday's third round opponents, Hull City, at Wembley."
				+ "So whatever team Wenger puts out at the Emirates, there will be a realisation his side need to win this tie -- and it's a potentially tricky one. Hull's priority will surely be survival this season, but manager Steve Bruce would surely love a bit of revenge."
				+ "They came so close to winning the FA Cup back in May -- they went 2-0 up and were only a Kieran Gibbs clearance away from making it three. Arsenal came back to win in extra time with Aaron Ramsey scoring the winner; appeasing the fans with the club's first trophy since they won it in 2005."
				+ "It would have been so difficult for Wenger to sign a new contract had he lost the final and it would have been even harder for the club to convince fans it was the right decision. But the Cup success was a game changer. In that context, the competition has been massive for the Frenchman during the course of his career in England.";
		news2.author = "Sky Sports";
		news.add(news1);
	    news.add(news2);
        */
	    
		Log.d(TAG, "news: item" + news );
		NewsAdapter listAdapter = new NewsAdapter(this, 
				R.layout.news_list, news);
		setListAdapter(listAdapter);
		
		ListView listview = getListView();
		listview.setDivider(null);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.news_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.add_new_news:
			Intent i = new Intent();
			i.setClass(this, AddNews.class);

            Bundle b = new Bundle();
            b.putString(Orgs.O_LID, organization_LID);
            // b.putString(News.N_LID, organization_LID);
            i.putExtras(b);

            startActivity(i);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
/*
		Toast.makeText(this,
				getListView().getItemAtPosition(position).toString(),
				Toast.LENGTH_SHORT).show();
*/
		Intent intent = new Intent();
		intent.setClass(this, NewsDetail.class);
		Bundle b = new Bundle();

		// should pass the news name to newsdetail
		/*
		 * b.putString("org_name",mo[position]); intent.putExtras(b);
		 */

        if (position >= news.size()) return;

        b.putString(Orgs.O_LID, organization_LID);
        b.putString(News.N_LID, news.get(position).lid);
        b.putString(News.N_TITLE, news.get(position).title);
        b.putString(News.N_BODY, news.get(position).content);
        intent.putExtras(b);
		startActivity(intent);

	}

	private class NewsAdapter extends ArrayAdapter<NewsItem> {

		private ArrayList<NewsItem> news;

		public NewsAdapter(Context context, int textViewResourceId,
				List<NewsItem> objects) {
			super(context,  textViewResourceId, objects);
			this.news = (ArrayList<NewsItem>) objects;
		}
		@Override
		public int getCount() {
			return news.size();
		}

		@Override
		public NewsItem getItem(int position) {
			return news.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.news_list, null);
			}


			Log.d(TAG, "news: item" + news );
			NewsItem item = news.get(position);
			
			Log.d(TAG, "news: position" + position + " : " + item.title);
			
			if (item != null) {

				TextView dateAndTime = (TextView) v
						.findViewById(R.id.news_list_date_and_time);
				TextView title = (TextView) v
						.findViewById(R.id.news_list_title);
				TextView content = (TextView) v
						.findViewById(R.id.news_list_content);

				TextView author = (TextView) v.findViewById(R.id.news_list_author);
				title.setText(item.title);
				Calendar c = Calendar.getInstance();
                if (item.date_time == null)
                    dateAndTime.setText("2015-03-29 1800");
                else
    				dateAndTime.setText(item.date_time);
    
				author.setText(item.author);

                String text = "" + Html.fromHtml(item.content);
                if (text.length() <= 200) {
					content.setText(text);
				} else {
					content.setText(text.substring(0, 200) + "...");
				}
				
			}

			return v;
		}

	}

	private class NewsItem {
        public String date_time;
        public String lid;
		public String title;
		public String content;
		public String author;
	}

}

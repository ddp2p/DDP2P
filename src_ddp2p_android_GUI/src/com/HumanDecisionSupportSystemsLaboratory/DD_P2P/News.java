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

public class News extends ListActivity {
	private static final String TAG = "news";
	private String[] newsTitle;
	private ActionBar actionbar = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);

		ArrayList<NewsItem> news = new ArrayList<NewsItem>();
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
			startActivity(i);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {

		Toast.makeText(this,
				getListView().getItemAtPosition(position).toString(),
				Toast.LENGTH_SHORT).show();

		Intent intent = new Intent();
		intent.setClass(this, NewsDetail.class);
		Bundle b = new Bundle();

		// should pass the news name to newsdetail
		/*
		 * b.putString("org_name",mo[position]); intent.putExtras(b);
		 */
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
				dateAndTime.setText("2015-1-3-1950");
    
				author.setText(item.author);
				
				if (item.content.length() <= 200) {
					content.setText(item.content);
				} else {
					content.setText(item.content.substring(0, 200) + "...");
				}
				
			}

			return v;
		}

	}

	private class NewsItem {
		public String dt;
		public String title;
		public String content;
		public String author;
	}

}

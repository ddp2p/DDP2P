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

import net.ddp2p.common.util.Util;

import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_MotionChoice;
import net.ddp2p.common.data.D_Justification.JustificationSupportEntry;
import net.ddp2p.common.data.D_Motion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class JustificationBySupportType extends Fragment {
    private static final boolean DEBUG = false;

	private ArrayList<String> parentItems = new ArrayList<String>();
	private ArrayList<Object> childItems = new ArrayList<Object>();
    private ToAddJustificationDialog dialog;
	private ArrayList<JustificationSupportEntry> justification_list;
	private TextView label_justification;
	int crt_choice;
	public static final String POS = "pos";
	private String title;
	private String body;
	private String e_title;
/*	private WebView titleTextView;
	private WebView contentTextView;
	private WebView enhancingView;*/
	
	private TextView titleTextView;
	private TextView contentTextView;
	private TextView enhancingView;
	
//    private Button choice_0;
//    private Button choice_1;
//    private Button choice_2;
	
    final static Object checked_Monitor = new Object();

private static final String TAG = "JustificationBySupportType";

    // The justification currently selected
    static long checkedJustifLID = -1;
    // The view (widget) currently selected
    static View checked_View = null;
    
    private MyExpandableAdapter adapter;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.justification_support, container,
				false);
		
		Bundle b = getArguments();
		crt_choice = Util.ival(b.getString(POS), 0);
		title = b.getString("title");
		body = b.getString("body");
		e_title = b.getString("enhance");
		Log.d(TAG, "JustificationBySupportType: ->title="+title);
		
		
		// Create Expandable List and set it's properties
		ExpandableListView expandableList = (ExpandableListView) v.findViewById(R.id.justification_support_expandablelist);
		expandableList.setDividerHeight(2);
		expandableList.setGroupIndicator(null);
		expandableList.setClickable(true);
        expandableList.setChoiceMode(ExpandableListView.CHOICE_MODE_SINGLE);
        
        View header = getLayoutInflater(null).inflate(R.layout.justification_header, null);

        /*titleTextView = (WebView) header.findViewById(R.id.motion_detail_title);
		contentTextView = (WebView) header.findViewById(R.id.motion_detail_content);
		enhancingView = (WebView) header.findViewById(R.id.motion_detail_enhancing);*/
		
        titleTextView = (TextView) header.findViewById(R.id.justification_header_title);
		contentTextView = (TextView) header.findViewById(R.id.justification_header_body);
        
		//titleTextView.loadData("<H1><p style=\"font-size:large;\"><b><i>"+title+"</i></b></p></H1>", "text/html", null);
		//titleTextView.loadData("<html><H2><i>T="+title+"</i></H2></html>", "text/html", null);
		titleTextView.setText(Html.fromHtml("<html><H2><i>"+title+"</i></H2></html>"));
		
		// contentTextView.loadData("<html><b>Bold?</b> <i>Italic!=</i></html>", "text/html", null);//.setText(body);
		//contentTextView.loadData(body, "text/html", null);
		contentTextView.setText(Html.fromHtml(body));
		
		Log.d(TAG, "JustificationBySupportType: ->titletext="+titleTextView);
		label_justification = (TextView) header.findViewById(R.id.label_justification);
		if (invalidChoice(crt_choice)) {
			label_justification.setText(Util.__("ALL JUSTIFICATIONS").toUpperCase());
		} else {
			label_justification.setText(MotionDetail.crt_motion.getActualChoices()[crt_choice].name.toUpperCase());
		}

        expandableList.addHeaderView(header);     
		
		// Set the Items of Parent
		setGroupParents();
		// Set The Child Data
		setChildData();

		// Create the Adapter
		 adapter = new MyExpandableAdapter(parentItems,
				childItems);

		adapter.setInflater(
				(LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE),
				getActivity());

		// Set the Adapter to expandableList
		expandableList.setAdapter(adapter);
/*		expandableList.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				return false;
			}
		});*/

		
		return v;

	}

	public static JustificationBySupportType newInstance(String title, String body, String enhance, String text, int pos) {

		JustificationBySupportType f = new JustificationBySupportType();
		Bundle b = new Bundle();
		b.putString("support", text);
		b.putString(POS, ""+pos);
        b.putString("title", title);
		b.putString("body", body);
		b.putString("enhance", enhance);
		f.setArguments(b);
        

		return f;
	}

	boolean invalidChoice(int choice) {
		if (choice < 0) return true;
		if (choice >= MotionDetail.crt_motion.getActualChoices().length) return true;
		return false;
	}
	// method to add parent Items
	public void setGroupParents() {
		//D_Justification.getAllJustificationsCnt(crt_motion_LID, crt_choice, crt_answered_LID);
		D_Motion m = MotionDetail.crt_motion;
		if (m == null) return;
		//ArrayList<JustificationSupportList> 

		if (invalidChoice(crt_choice))
			justification_list = D_Justification.getAllJustificationsCnt(m.getLIDstr(), null, null);
		else
			justification_list = D_Justification.getAllJustificationsCnt(m.getLIDstr(),
				//m.getSupportChoice(),
				m.getActualChoices()[crt_choice].short_name,
				null);
		
		
		parentItems.clear();
		
		if (justification_list.size() == 0) parentItems.add("No Justification Available");

		for (JustificationSupportEntry jsl : justification_list) {
			D_Justification j = D_Justification.getJustByLID(jsl.getJustification_LIDstr(), true, false);
			if (j == null) continue;
			
			Object title = j.getTitleOrMy();
			if (DEBUG) Log.d("Justification", "JustificationAgainst: setGroupParents: title=" + title);
			if (title instanceof D_Document) {
				if (DEBUG) Log.d("Justification", "JustificationAgainst: setGroupParents: doc title=" + title);
				title = ((D_Document)title).getDocumentUTFString();
				if (DEBUG) Log.d("Justification", "JustificationAgainst: setGroupParents: new doc title=" + title);
			}
			if (title instanceof D_Document_Title) {
				if (DEBUG) Log.d("Justification", "JustificationAgainst: setGroupParents: doc title=" + title);
				title = ((D_Document_Title)title).title_document.getDocumentUTFString();
				if (DEBUG) Log.d("Justification", "JustificationAgainst: setGroupParents: new doc title=" + title);
			}
			parentItems.add(Util.getString(title));
			
		}
		//parentItems.add("Fruits");
		//parentItems.add("Flowers");
		//parentItems.add("Animals");
		//parentItems.add("Birds");
	}

	// method to set child data of each parent
	public void setChildData() {
		D_Motion m = MotionDetail.crt_motion;
		if (m == null) return;
		D_MotionChoice[] ch = m.getActualChoices();
		//ArrayList<JustificationSupportList> 
		if (ch.length <= 1) return;

		childItems.clear();
		if (justification_list == null) return;
		
		if (justification_list.size() == 0) {
			ArrayList<String> child = new ArrayList<String>();
			//child.add("You can justify votes!");
			if (invalidChoice(crt_choice)) {
				child.add("You can add justification");
			} else {
				child.add("You can justify: " + ch[crt_choice].name);
			}
			childItems.add(child);
		}
		for (JustificationSupportEntry jsl : justification_list) {
			D_Justification j = D_Justification.getJustByLID(jsl.getJustification_LIDstr(), true, false);
			if (j == null) continue;
			
			ArrayList<String> child = new ArrayList<String>();
			child.add(j.getJustificationBody().getDocumentUTFString());
			childItems.add(child);
		}
		
//		// Add Child Items for Fruits
//		ArrayList<String> child = new ArrayList<String>();
//		child.add("Apple");
//		childItems.add(child);
//
//		// Add Child Items for Flowers
//		child = new ArrayList<String>();
//		child.add("Rose");
//		childItems.add(child);
//
//		// Add Child Items for Animals
//		child = new ArrayList<String>();
//		child.add("Lion");
//		childItems.add(child);
//
//		// Add Child Items for Birds
//		child = new ArrayList<String>();
//		child.add("Parrot");
//		childItems.add(child);
	}

	
	
	@Override
	public void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
	}



	private class MyExpandableAdapter extends BaseExpandableListAdapter {

		private static final boolean _DEBUG = true;
		private Activity activity;
		private ArrayList<Object> childtems;
		private LayoutInflater inflater;
		private ArrayList<String> parentItems, child;

		// constructor
		public MyExpandableAdapter(ArrayList<String> parents,
				ArrayList<Object> childern) {
			this.parentItems = parents;
			this.childtems = childern;
		}

		public void setInflater(LayoutInflater inflater, Activity activity) {
			this.inflater = inflater;
			this.activity = activity;
		}

		// method getChildView is called automatically for each child view.
		// Implement this method as per your requirement
		@Override
		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			JustificationSupportEntry justification = null;
			if (groupPosition < childtems.size()) {
				child = (ArrayList<String>) childtems.get(groupPosition);
			} else {
				child = new ArrayList<String>();
			}
			if (groupPosition >= justification_list.size()) {
				Toast.makeText(getActivity(), "No group at: "+groupPosition, Toast.LENGTH_SHORT).show();
				//return null;
				//justification = new 
			} else {
				justification = justification_list.get(groupPosition);
			}
			
			TextView textView = null;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.justification_child_view, null);
			}

			// get the textView reference and set the value
			textView = (TextView) convertView.findViewById(R.id.textViewChild);
			textView.setText(Html.fromHtml(child.get(childPosition)));

			// set the ClickListener to handle the click event on child item
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Toast.makeText(activity, child.get(childPosition),
							Toast.LENGTH_SHORT).show();
				}
			});
/*
			choice_0 = (Button) convertView.findViewById(R.id.justification_support);
			choice_1 = (Button) convertView.findViewById(R.id.justification_against);
			choice_2 = (Button) convertView.findViewById(R.id.justification_neutral);
			
			D_MotionChoice[] choices = MotionDetail.crt_motion.getActualChoices();
			if (choices.length >= 1) {
				choice_0.setText(choices[0].name);
				choice_0.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_0.setVisibility(android.view.View.INVISIBLE);
			}
			if (choices.length >= 2) {
				choice_1.setText(choices[1].name);
				choice_1.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_1.setVisibility(android.view.View.INVISIBLE);
			}
			if (choices.length >= 3) {
				choice_2.setText(choices[2].name);
				choice_2.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_2.setVisibility(android.view.View.INVISIBLE);
			}
			
			choice_0.setOnClickListener(new My_OnClickListener(justification) {
				
				@Override
				public void _onClick(View v) {
					//update name dialog
					JustificationSupportEntry justification = (JustificationSupportEntry)ctx;
					FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, justification, 
				    		MotionDetail.crt_motion.getActualChoices()[0].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});
			
			choice_1.setOnClickListener(new My_OnClickListener(justification) {
				
				@Override
				public void _onClick(View v) {
					JustificationSupportEntry justification = (JustificationSupportEntry)ctx;
					//update name dialog
					FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, justification, 
				    		MotionDetail.crt_motion.getActualChoices()[1].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});
			
			choice_2.setOnClickListener(new My_OnClickListener(justification) {
				
				@Override
				public void _onClick(View v) {
					//update name dialog
					JustificationSupportEntry justification = (JustificationSupportEntry)ctx;
					FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, justification, 
				    		MotionDetail.crt_motion.getActualChoices()[2].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});
*/
			return convertView;
		}

		// method getGroupView is called automatically for each parent item
		// Implement this method as per your requirement
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {

			

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.justification_parent_view, null);// parent);
			}

			CheckedTextView ctv = (CheckedTextView) convertView.findViewById(R.id.textViewGroupName);
			
			ctv.setText(parentItems.get(groupPosition));
			ctv.setChecked(isExpanded);
			
			if (groupPosition >= justification_list.size()) return convertView;
						
			JustificationSupportEntry j = justification_list.get(groupPosition);
			D_Justification justification = D_Justification.getJustByLID(j.getJustification_LIDstr(), true, false);

			synchronized(checked_Monitor) {
				if (j.getJustification_LID() == checkedJustifLID) {
					convertView.setBackgroundColor(Color.RED); // current icon
					if (DEBUG) Log.d("CHECK", "getGroupView ["+groupPosition+"/"+crt_choice+"] rLID="+checkedJustifLID);
				} else {
					if ((justification.getAnswerToLID() == checkedJustifLID) && (checkedJustifLID > 0))
						convertView.setBackgroundColor(Color.YELLOW); // answering this, icon
					else {
						D_Justification current = null;
						if (checkedJustifLID > 0)
							current = D_Justification.getJustByLID(checkedJustifLID, true, false);
						if ((current != null) && (current.getAnswerToLID() == justification.getLID())) {
							convertView.setBackgroundColor(Color.GREEN); // answered icon
						} else {
							convertView.setBackgroundColor(Color.WHITE);
						}
					}
					if (DEBUG) Log.d("CHECK", "getGroupView ["+groupPosition+"/"+crt_choice+"] wLID="+checkedJustifLID);
				}
			}
			
			if (! invalidChoice(crt_choice)) {
				long votes = justification.getActivityNb_ByChoice_WithCache(""+crt_choice, false);
				TextView tup = (TextView) convertView.findViewById(R.id.justification_parent_thumbs_up);
	/*			TextView tdn = (TextView) convertView.findViewById(R.id.justification_parent_thumbs_down);*/
				tup.setText(""+votes);
			} else {
				long votes = justification.getActivityNb_All();//.getActivityNb_ByChoice_WithCache(""+crt_choice, false);
				TextView tup = (TextView) convertView.findViewById(R.id.justification_parent_thumbs_up);
				tup.setText(""+votes);
			}
			class ViewJustif {
				JustificationSupportEntry j;
				View convertView;
				int pos;
				ViewGroup parent;
				public ViewJustif (JustificationSupportEntry j, View convertView, int pos, ViewGroup parent) {
					this.j = j;
					this.convertView = convertView;
					this.pos = pos;
					this.parent = parent;
				}
			}
			
			convertView.setOnClickListener(new My_OnClickListener(new ViewJustif(j, convertView, groupPosition, parent)) {
				
				@Override
				public void _onClick(View v) {
					ViewJustif vj = (ViewJustif) ctx;
					synchronized (checked_Monitor) {
						JustificationSupportEntry j = vj.j;
						if (checked_View != null) {
							checked_View.setBackgroundColor(Color.WHITE);
							if (DEBUG) Log.d("CHECK", "JustificationBySupportType: onClick ["+vj.pos+"] oLID="+checkedJustifLID);
						}
						
						if (j.getJustification_LID() == checkedJustifLID) {
							checkedJustifLID = -1;
							checked_View = null;
							return; // This makes it not expand when clicking a selected
						} else {
							checkedJustifLID = j.getJustification_LID();
							checked_View = vj.convertView;
							
							if (checked_View != null) {
								checked_View.setBackgroundColor(Color.RED);
								if (DEBUG) Log.d("CHECK", "JustificationBySupportType: onClick ["+vj.pos+"] nLID="+checkedJustifLID);
							}
						}
					}
					ExpandableListView eLV = (ExpandableListView) vj.parent;
					boolean e = eLV.expandGroup(vj.pos);
					if (!e) eLV.collapseGroup(vj.pos);
				}
			});
			
			return convertView;
		}

		
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return ((ArrayList<String>) childtems.get(groupPosition)).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return parentItems.size();
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}

}



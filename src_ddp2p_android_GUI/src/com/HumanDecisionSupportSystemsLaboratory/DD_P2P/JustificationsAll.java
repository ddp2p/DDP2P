package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.util.ArrayList;

import net.ddp2p.common.util.Util;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Justification.JustificationSupportEntry;
import net.ddp2p.common.data.D_MotionChoice;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;


public class JustificationsAll extends Fragment{
	private ArrayList<String> parentItems = new ArrayList<String>();
	private ArrayList<Object> childItems = new ArrayList<Object>();
    private ToAddJustificationDialog dialog;
	private ArrayList<JustificationSupportEntry> justification_list;
	
    private Button choice_0;
    private Button choice_1;
    private Button choice_2;

	public static JustificationsAll newInstance(String text) {

		JustificationsAll f = new JustificationsAll();
		Bundle b = new Bundle();
		b.putString("support", text);

		f.setArguments(b);

		return f;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.justification_against, container,
				false);

		// Create Expandable List and set it's properties
		ExpandableListView expandableList = (ExpandableListView) v.findViewById(R.id.justification_against_expandablelist);
		expandableList.setDividerHeight(2);
		expandableList.setGroupIndicator(null);
		expandableList.setClickable(true);

		// Set the Items of Parent
		setGroupParents();
		// Set The Child Data
		setChildData();

		// Create the Adapter
		MyExpandableAdapter adapter = new MyExpandableAdapter(parentItems,
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

	// method to add parent Items
	public void setGroupParents() {
		D_Motion m = MotionDetail.crt_motion;
		if (m == null) return;
		D_MotionChoice[] ch = m.getActualChoices();
		//ArrayList<JustificationSupportList> 
		if (ch.length <= 1) return;
		
		parentItems.clear();
		//justification_list = D_Justification.getAllJustificationsCnt(m.getLIDstr(), ch[ch.length - 1].short_name, null);
		justification_list = D_Justification.getAllJustificationsCnt(m.getLIDstr(), null, null);
		
		if (justification_list.size() == 0) parentItems.add("No Justification Available");

		for (JustificationSupportEntry jsl : justification_list) {
			D_Justification j = D_Justification.getJustByLID(jsl.getJustification_LIDstr(), true, false);
			if (j == null) continue;
			
			Object title = j.getTitleOrMy();
			Log.d("Justification", "JustificationAgainst: setGroupParents: title=" + title);
			if (title instanceof D_Document) {
				Log.d("Justification", "JustificationAgainst: setGroupParents: doc title=" + title);
				title = ((D_Document)title).getDocumentUTFString();
				Log.d("Justification", "JustificationAgainst: setGroupParents: new doc title=" + title);
			}
			if (title instanceof D_Document_Title) {
				Log.d("Justification", "JustificationAgainst: setGroupParents: doc title=" + title);
				title = ((D_Document_Title)title).title_document.getDocumentUTFString();
				Log.d("Justification", "JustificationAgainst: setGroupParents: new doc title=" + title);
			}
			parentItems.add(Util.getString(title));
			
		}
//		parentItems.add("Fruits");
//		parentItems.add("Flowers");
//		parentItems.add("Animals");
//		parentItems.add("Birds");
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
			child.add("You can justify: " + ch[ch.length - 1].name);
			childItems.add(child);
		}
		for (JustificationSupportEntry jsl : justification_list) {
			D_Justification j = D_Justification.getJustByLID(jsl.getJustification_LIDstr(), true, false);
			if (j == null) continue;
			
			ArrayList<String> child = new ArrayList<String>();
			child.add(j.getJustificationBodyText());
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

	private class MyExpandableAdapter extends BaseExpandableListAdapter {

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



			if (convertView == null) {
				convertView = inflater.inflate(R.layout.justification_child_view, null);
			}
			child = (ArrayList<String>) childtems.get(groupPosition);

			TextView textView = null;

			// get the textView reference and set the value
			textView = (TextView) convertView.findViewById(R.id.textViewChild);
			textView.setText(child.get(childPosition));

			// set the ClickListener to handle the click event on child item
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Toast.makeText(activity, child.get(childPosition),
							Toast.LENGTH_SHORT).show();
				}
			});
			
			
/*			choice_0 = (Button) convertView.findViewById(R.id.justification_support);
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

			
			choice_0.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//update name dialog
					android.support.v4.app.FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, 
				    		MotionDetail.crt_motion.getActualChoices()[0].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});
			
			choice_1.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//update name dialog
					android.support.v4.app.FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, 
				    		MotionDetail.crt_motion.getActualChoices()[1].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});
			
			choice_2.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//update name dialog
					FragmentManager fm = getFragmentManager();
				    dialog = new ToAddJustificationDialog(MotionDetail.crt_motion, 
				    		MotionDetail.crt_motion.getActualChoices()[2].short_name);
				    dialog.show(fm, "fragment_to_add_justification");
					
				}
			});*/

			return convertView;
		}

		// method getGroupView is called automatically for each parent item
		// Implement this method as per your requirement
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.justification_parent_view, null);
			}

			CheckedTextView ctv = (CheckedTextView) convertView.findViewById(R.id.textViewGroupName);
			TextView thumbsUp = (TextView) convertView.findViewById(R.id.justification_parent_thumbs_up);
/*			TextView thumbsDown = (TextView) convertView.findViewById(R.id.justification_parent_thumbs_down);*/
			ctv.setText(parentItems
					.get(groupPosition));
			ctv.setChecked(isExpanded);


			
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

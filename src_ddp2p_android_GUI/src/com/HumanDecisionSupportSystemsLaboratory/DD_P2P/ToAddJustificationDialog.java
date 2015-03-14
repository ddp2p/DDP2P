package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import net.ddp2p.common.config.Identity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Justification.JustificationSupportEntry;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Vote;

public class ToAddJustificationDialog extends DialogFragment {
	private static final boolean DEBUG = false;
	String type;
	private D_Motion crt_motion;
	long oLID;
	private JustificationSupportEntry crt_justification;
	public ToAddJustificationDialog() {
		
//		D_Constituent myself = Identity.getCrtConstituent(oLID = crt_motion.getLID());
//		if (myself == null) {
//			Toast.makeText(getActivity(), "Fill your Profile!", Toast.LENGTH_LONG).show();
//			//detach();
//			return;
//		}
		
	}
	public ToAddJustificationDialog(D_Motion crt_motion, String i) {
		type = i;
		this.crt_motion = crt_motion;
		
		D_Constituent myself = Identity.getCrtConstituent(oLID = crt_motion.getOrganizationLID());
		if (DEBUG) Log.d("CONST", "ToAddJust<init 2>: oLID="+oLID+" c="+myself);
		if (myself == null) {
			Toast.makeText(getActivity(), "Fill your Profile!", Toast.LENGTH_LONG).show();
//			FragmentTransaction ft = getFragmentManager()
//					.beginTransaction();
//			ft.detach(ToAddJustificationDialog.this);
//			ft.commit();
			//detach();
			return;
		}
	}

	public ToAddJustificationDialog(D_Motion crt_motion,
			JustificationSupportEntry justificationSupport, String short_name) {
		type = short_name;
		this.crt_motion = crt_motion;
		this.crt_justification = justificationSupport;
		
		D_Constituent myself = Identity.getCrtConstituent(oLID = crt_motion.getOrganizationLID());
		if (DEBUG) Log.d("CONST", "ToAddJust<init 3>: oLID="+oLID+" c="+myself);
		if (myself == null) {
//			Toast.makeText(getActivity(), "Fill your Profile!", Toast.LENGTH_LONG).show();
//			FragmentTransaction ft = getFragmentManager()
//					.beginTransaction();
//			ft.detach(ToAddJustificationDialog.this);
//			ft.commit();
			//detach();
			return;
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_to_add_justification,
				container);
		final Button butYes = (Button) view
				.findViewById(R.id.dialog_add_new_justification);
		final Button butNo = (Button) view
				.findViewById(R.id.do_not_add_new_justification);

		
		
		getDialog().setTitle("Justification");

		// add a new justification
		butYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				D_Constituent myself = Identity.getCrtConstituent(oLID);
				if (myself == null) {
					Toast.makeText(getActivity(), "Fill your Profile!", Toast.LENGTH_LONG);
					return;
				}
				Intent i = new Intent();
				i.setClass(getActivity(), AddJustification.class);
				Bundle b = new Bundle();
				b.putString(Motion.M_MOTION_CHOICE,
						ToAddJustificationDialog.this.type);
				b.putString(Motion.M_MOTION_LID,
						ToAddJustificationDialog.this.crt_motion.getLIDstr());
				String jLID = "";
				if (ToAddJustificationDialog.this.crt_justification != null)
					jLID = ToAddJustificationDialog.this.crt_justification
							.getJustification_LIDstr();
				b.putString(Motion.J_JUSTIFICATION_LID, jLID);
				i.putExtras(b);
				startActivity(i);
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				ft.detach(ToAddJustificationDialog.this);
				ft.commit();
				Toast.makeText(getActivity(), "Add a new justification",
						Toast.LENGTH_SHORT).show();
			}
		});

		// if not add a new justification
		butNo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				D_Constituent myself = Identity.getCrtConstituent(oLID);
				if (myself == null) {
					Toast.makeText(getActivity(), "Fill your Profile!", Toast.LENGTH_LONG);
					return;
				}
				D_Justification j = null;
				if (ToAddJustificationDialog.this.crt_justification != null)
					j = D_Justification.getJustByLID(
							ToAddJustificationDialog.this.crt_justification
									.getJustification_LIDstr(), true, false);
				D_Vote vote = D_Vote.createVote(crt_motion, j, type);
				if (DEBUG) Log.d("VOTE", "Bad vote=" + vote);

				// D_Vote vote = new D_Vote();
				// vote.setMotionAndOrganizationAll(crt_motion);
				// if (ToAddJustificationDialog.this.crt_justification != null)
				// {
				// D_Justification j =
				// D_Justification.getJustByLID(ToAddJustificationDialog.this.crt_justification.getJustification_LIDstr(),
				// true, false);
				// if (j != null) vote.setJustificationAll(j);
				// }
				// vote.setConstituentAll(Identity.getCrtConstituent(crt_motion.getOrganizationLID()));
				// String choice = type;
				// Log.d("VOTE", "Sign type="+type);
				// vote.setChoice(choice);
				// Log.d("VOTE", "Sign oLID="+crt_motion.getOrganizationLID());
				//
				// Log.d("VOTE", "Sign oLID="+vote);
				// vote.sign();
				// try {
				// vote.storeVerified();
				// } catch (P2PDDSQLException e) {
				// e.printStackTrace();
				// }

				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				ft.detach(ToAddJustificationDialog.this);
				ft.commit();

				Intent intent = getActivity().getIntent();
				getActivity().finish();
				startActivity(intent);
			}
		});

		return view;
	}

}

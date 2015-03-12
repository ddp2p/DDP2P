/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Osamah Dhannoon
 		Author: Osamah Dhannoon: odhannoon2011@my.fit.edu
 		        Marius Silaghi: msilaghi@fit.edu
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

package wireless;

import java.util.regex.Pattern;

import util.P2PDDSQLException;

import config.DD;

public class Broadcasting_Probabilities {
	public static float broadcast_constituent = 1/1000000.f;
	public static float broadcast_witness =30/100.f;  
	public static float broadcast_organization = 1/1000000.f;    
	public static float broadcast_peer=1/1000000.f;  
	public static float broadcast_neighborhood = 1/1000000.f;
	public static float broadcast_vote = 1 - broadcast_constituent - broadcast_witness -
			broadcast_organization - broadcast_peer - broadcast_neighborhood;


	
	public static float broadcast_justification;
	public static float broadcast_motion;

	// normalized prefix values, from the largest to the smallest
	private static float broadcast_queue_md = 0.9f;
	private static float broadcast_queue_c = 0.1f;
	private static float broadcast_queue_ra = 0.5f;
	private static float broadcast_queue_re = 0.7f;
	private static float broadcast_queue_bh = 0.3f;
	private static float broadcast_queue_br = 0.4f;
	
	public static final float F_MD = 0.2f;//0.04f;
	public static final float F_C = 0.8f;//0.04f;
	public static final float F_RA = 0.04f;
	public static final float F_RE = 0.8f;
	public static final float F_BH = 0.04f;
	public static final float F_BR = 0.04f;
	// modified externally, with a probability for each
	public static float __broadcast_queue_md = F_MD;
	public static float __broadcast_queue_c = F_C;
	public static float __broadcast_queue_ra = F_RA;
	public static float __broadcast_queue_re = F_RE;
	public static float __broadcast_queue_bh = F_BH;
	public static float __broadcast_queue_br = F_BR;
	// normalization
	private static float __broadcast_queue_sum = setInitCurrentProbabilities();
	
	// not normalized prefix values
	public static float ___broadcast_queue_md;
	public static float ___broadcast_queue_c;
	public static float ___broadcast_queue_ra;
	public static float ___broadcast_queue_re;
	public static float ___broadcast_queue_bh;
	public static float ___broadcast_queue_br;

	//defaults
	private static final boolean B_MD = false;
	private static final boolean B_C = false;
	private static final boolean B_RA = false;
	private static final boolean B_RE = false;
	private static final boolean B_BH = false;
	private static final boolean B_BR = false;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = false;

	// modified externally with enabling disabling
	public static boolean _broadcast_queue_md = B_MD;
	public static boolean _broadcast_queue_c = B_C;
	public static boolean _broadcast_queue_ra = B_RA;
	public static boolean _broadcast_queue_re = B_RE;
	public static boolean _broadcast_queue_bh = B_BH;
	public static boolean _broadcast_queue_br = B_BR;
	
	public static boolean monitoredInited;
	public static final Object monitorInit = new Object();
	public static void initFromDB(){
		if(_DEBUG) System.out.println("StartUpThread: initFromDB: start!");
		synchronized(monitorInit){
			try {
				if(monitoredInited){
					if(_DEBUG) System.out.println("StartUpThread: initFromDB: already done!");
					return;
				}
				/*
				Broadcasting_Probabilities._broadcast_queue_md = DD.getAppBoolean(DD.APP_Q_MD, Broadcasting_Probabilities._broadcast_queue_md);
				Broadcasting_Probabilities._broadcast_queue_c = DD.getAppBoolean(DD.APP_Q_C, Broadcasting_Probabilities._broadcast_queue_c);
				Broadcasting_Probabilities._broadcast_queue_ra = DD.getAppBoolean(DD.APP_Q_RA, Broadcasting_Probabilities._broadcast_queue_ra);
				Broadcasting_Probabilities._broadcast_queue_re = DD.getAppBoolean(DD.APP_Q_RE, Broadcasting_Probabilities._broadcast_queue_re);
				Broadcasting_Probabilities._broadcast_queue_bh = DD.getAppBoolean(DD.APP_Q_BH, Broadcasting_Probabilities._broadcast_queue_bh);
				Broadcasting_Probabilities._broadcast_queue_br = DD.getAppBoolean(DD.APP_Q_BR, Broadcasting_Probabilities._broadcast_queue_br);
				*/
				Broadcasting_Probabilities._broadcast_queue_md = DD.getAppBoolean(DD.APP_Q_MD, Broadcasting_Probabilities.B_MD);
				Broadcasting_Probabilities._broadcast_queue_c = DD.getAppBoolean(DD.APP_Q_C, Broadcasting_Probabilities.B_C);
				Broadcasting_Probabilities._broadcast_queue_ra = DD.getAppBoolean(DD.APP_Q_RA, Broadcasting_Probabilities.B_RA);
				Broadcasting_Probabilities._broadcast_queue_re = DD.getAppBoolean(DD.APP_Q_RE, Broadcasting_Probabilities.B_RE);
				Broadcasting_Probabilities._broadcast_queue_bh = DD.getAppBoolean(DD.APP_Q_BH, Broadcasting_Probabilities.B_BH);
				Broadcasting_Probabilities._broadcast_queue_br = DD.getAppBoolean(DD.APP_Q_BR, Broadcasting_Probabilities.B_BR);
				
				
				//System.out.println("DB Selecting right check: re"+Broadcasting_Probabilities._broadcast_queue_re);
				Broadcasting_Probabilities.load_broadcast_queue_probabilities(DD.getAppText(DD.BROADCASTING_QUEUE_PROBABILITIES));
				Broadcasting_Probabilities.setCurrentProbabilities();
				//System.out.println("scSelecting right check"+Broadcasting_Probabilities._broadcast_queue_re);
				monitoredInited = true;
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(_DEBUG) System.out.println("StartUpThread: initFromDB: done");
	}
	
	
	/**
	 * Initialized in Start_Up_Thread
	 * @return
	 */
	public static float setInitCurrentProbabilities() {
		return __broadcast_queue_sum;
		/*
		//synchronized(StartUpThread.monitorInit){
			//if(StartUpThread.monitoredInited) return __broadcast_queue_sum;
			_broadcast_queue_md = B_MD;
			_broadcast_queue_c = B_C;
			_broadcast_queue_ra = B_RA;
			_broadcast_queue_re = B_RE;
			_broadcast_queue_bh = B_BH;
			_broadcast_queue_br = B_BR;		

			__broadcast_queue_md = F_MD;
			__broadcast_queue_c = F_C;
			__broadcast_queue_ra = F_RA;
			__broadcast_queue_re = F_RE;
			__broadcast_queue_bh = F_BH;
			__broadcast_queue_br = F_BR;
			//return __broadcast_queue_sum;
			return setCurrentProbabilities();
		//}
		 
		 */
	}
	public static float setCurrentProbabilities(){	
		
		if(DEBUG){
		System.out.println("B_MD="+_broadcast_queue_md);
		System.out.println("F_MD="+__broadcast_queue_md);
		System.out.println("P_MD="+___broadcast_queue_md);
		System.out.println("MD="+broadcast_queue_md);

		System.out.println("B_C="+_broadcast_queue_c);
		System.out.println("F_C="+__broadcast_queue_c);
		System.out.println("P_C="+___broadcast_queue_c);
		System.out.println("C="+broadcast_queue_c);
		}
		__broadcast_queue_sum =
			(___broadcast_queue_md=((_broadcast_queue_md)?__broadcast_queue_md:0) +
			(___broadcast_queue_c=((_broadcast_queue_c)?__broadcast_queue_c:0) +
			(___broadcast_queue_ra=((_broadcast_queue_ra)?__broadcast_queue_ra:0) +
			(___broadcast_queue_re=((_broadcast_queue_re)?__broadcast_queue_re:0) +
			(___broadcast_queue_bh=((_broadcast_queue_bh)?__broadcast_queue_bh:0) +
			(___broadcast_queue_br=((_broadcast_queue_br)?__broadcast_queue_br:0)))))));
		
		if(__broadcast_queue_sum==0) {
			___broadcast_queue_md = ___broadcast_queue_c = ___broadcast_queue_ra = 
			___broadcast_queue_re =___broadcast_queue_bh = ___broadcast_queue_br =	0;
			
			broadcast_queue_md = broadcast_queue_c = broadcast_queue_ra = 
			broadcast_queue_re = broadcast_queue_bh = broadcast_queue_br = 0;
			
			
			if(DEBUG){
			System.out.println("B_MD="+_broadcast_queue_md);
			System.out.println("F_MD="+__broadcast_queue_md);
			System.out.println("P_MD="+___broadcast_queue_md);
			System.out.println("MD="+broadcast_queue_md);

			System.out.println("B_C="+_broadcast_queue_c);
			System.out.println("F_C="+__broadcast_queue_c);
			System.out.println("P_C="+___broadcast_queue_c);
			System.out.println("C="+broadcast_queue_c);
			}
			return 0;
		}

		broadcast_queue_md = ___broadcast_queue_md / __broadcast_queue_sum;
		broadcast_queue_c = ___broadcast_queue_c / __broadcast_queue_sum;
		broadcast_queue_ra = ___broadcast_queue_ra / __broadcast_queue_sum;
		broadcast_queue_re = ___broadcast_queue_re / __broadcast_queue_sum;
		broadcast_queue_bh = ___broadcast_queue_bh / __broadcast_queue_sum;
		broadcast_queue_br = ___broadcast_queue_br / __broadcast_queue_sum;
		if(DEBUG){
		System.out.println("B_MD="+_broadcast_queue_md);
		System.out.println("F_MD="+__broadcast_queue_md);
		System.out.println("P_MD="+___broadcast_queue_md);
		System.out.println("MD="+broadcast_queue_md);

		System.out.println("B_C="+_broadcast_queue_c);
		System.out.println("F_C="+__broadcast_queue_c);
		System.out.println("P_C="+___broadcast_queue_c);
		System.out.println("C="+broadcast_queue_c);
		
		}
		return __broadcast_queue_sum;
	}
	
	public static String getCurrentProbabilitiesListAsString(
			String broadcastingProbabilities) {
		if(broadcastingProbabilities==null) return "C:1/1000000.f,";
		return broadcastingProbabilities;
	}
	public static final String SEP = ",";
	public static final String PROB_Q_MD = "MD:";
	public static final String PROB_Q_C = "C:";
	public static final String PROB_Q_RA = "RA:";
	public static final String PROB_Q_RE = "RE:";
	public static final String PROB_Q_BH = "BH:";
	public static final String PROB_Q_BR = "BR:";
	public static String getCurrentQueueProbabilitiesListAsString(
			String broadcastingQueueProbabilities) {
		if(broadcastingQueueProbabilities!=null) return broadcastingQueueProbabilities;
		return PROB_Q_MD+__broadcast_queue_md+SEP+PROB_Q_C+__broadcast_queue_c+SEP+PROB_Q_RA+__broadcast_queue_ra+SEP+PROB_Q_RE+__broadcast_queue_re+SEP+PROB_Q_BH+__broadcast_queue_bh+SEP+PROB_Q_BR+__broadcast_queue_br;
	}

	public static void load_broadcast_queue_probabilities(String val) {
		if(val == null) return;
		String ps[] = val.split(Pattern.quote(SEP));
		for(String p : ps) {
			String[] v = p.split(Pattern.quote(":"));
			if(v.length!=2) continue;
			setQueueProb(v);
		}
	}


	private static void setQueueProb(String[] v) {
		if(PROB_Q_MD.equals(v[0]+":")){ __broadcast_queue_md = Float.parseFloat(v[1]); return;}
		if(PROB_Q_C.equals(v[0]+":")){ __broadcast_queue_c = Float.parseFloat(v[1]); return;}
		if(PROB_Q_RA.equals(v[0]+":")){ __broadcast_queue_ra = Float.parseFloat(v[1]); return;}
		if(PROB_Q_RE.equals(v[0]+":")){ __broadcast_queue_re = Float.parseFloat(v[1]); return;}
		if(PROB_Q_BH.equals(v[0]+":")){ __broadcast_queue_bh = Float.parseFloat(v[1]); return;}
		if(PROB_Q_BR.equals(v[0]+":")){ __broadcast_queue_br = Float.parseFloat(v[1]); return;}
	}

	public static float get_broadcast_queue_c() {
		return broadcast_queue_c;
	}

	public static float get_broadcast_queue_br() {
		return broadcast_queue_br;
	}

	public static float get_broadcast_queue_bh() {
		return broadcast_queue_bh;
	}

	public static float get_broadcast_queue_ra() {
		return broadcast_queue_ra;
	}

	public static float get_broadcast_queue_re() {
		return broadcast_queue_re;
	}	
	public static float get_broadcast_queue_md() {
		return broadcast_queue_md;
	}	
	
	public static void main(String args[]){
		
		System.out.println("bc : "+get_broadcast_queue_c());
		System.out.println("br : "+get_broadcast_queue_br());
		System.out.println("bh : "+get_broadcast_queue_bh());
		System.out.println("ra : "+get_broadcast_queue_ra());
		System.out.println("re : "+get_broadcast_queue_re());
		System.out.println("md : "+get_broadcast_queue_md());
		
		System.out.println(" set sum : "+setCurrentProbabilities());
		
		
	}
	
}

/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014
		Authors: Khalid Alhamed, Marius Silaghi
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
package recommendationTesters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;

import util.Util;
import config.DD;
import data.D_RecommendationOfTester;
import data.D_Tester;

/**
 * It loads all the recommendations available at this time.
 * Checks an expiration time (e.g. 6 months) and deletes the old ones.
 *
 */
public class RecommenderOfTesters extends util.DDP2P_ServiceThread {
	static RecommenderOfTesters runningRecommender;
	final static int EXPIRATION_DELAY_RECEIVED_RECOMMENDATIONS_IN_DAYS = 360;
	final static int EXPIRATION_DELAY_PRODUCED_RECOMMENDATIONS_IN_DAYS = 1;
	static boolean USER_SOPHISTICATED_IN_SELECTING_TESTERS = false;
	
	final static float f_AMORTIZATION_OF_SCORE_APPLIED_AT_EACH_TRANSFER_BETWEEN_USERS = 0.9f;  // amortization factor
	final static float k_DIVERSITY_FACTOR_AS_TRADEOFF_AGAINST_PROXIMITY = 2.0f;  // diversity factor
	final static float Pr_PROBABILITY_OF_REPLACING_A_USED_TESTER_WITH_A_HIGHER_SCORED_ONE = 0.5f; // switching probability
	final static int N_MAX_NUMBER_OF_TESTERS = 3; // in our experiments we set N=10
	
	public final static int MAX_SIZE_OF_TESER_INTRODUCERS_IN_A_MESSAGE=10;// shouldn't exceed this limit
	public final static int EXPERATION_OF_RECOMMENDATION_BY_INTRODUCER_IN_DAYS=100;// 
	
	private static final boolean DEBUG = false;
	public static boolean STOP_RECOMMENDATION_OF_TESTERS = true;
	public RecommenderOfTesters() {
		super(Util.__("RecommenderOfTesters"), true);
		STOP_RECOMMENDATION_OF_TESTERS = DD.getAppBoolean(DD.APP_STOP_RECOMMENDATION_OF_TESTERS, false);
		USER_SOPHISTICATED_IN_SELECTING_TESTERS = DD.getAppBoolean(DD.APP_USER_SOPHISTICATED_IN_SELECTING_TESTERS, false);
		runningRecommender = this;
	}
	/**
	 * To stop the thread.
	 */
	public void stopRecommenders() {
		RecommenderOfTesters.STOP_RECOMMENDATION_OF_TESTERS = true;
		DD.setAppBoolean(DD.APP_STOP_RECOMMENDATION_OF_TESTERS, STOP_RECOMMENDATION_OF_TESTERS);
		this.interrupt();
		runningRecommender = null;
	}
	/**
	 * This creates a new thread, stopping previous ones
	 */
	public static void startRecommenders() {
		if (runningRecommender != null) runningRecommender.stopRecommenders();
		RecommenderOfTesters.STOP_RECOMMENDATION_OF_TESTERS = false;
		DD.setAppBoolean(DD.APP_STOP_RECOMMENDATION_OF_TESTERS, STOP_RECOMMENDATION_OF_TESTERS);
		runningRecommender = new RecommenderOfTesters();
	}
	public static void setSophisticated(boolean sophisticated) {
		if (runningRecommender != null) runningRecommender.stopRecommenders();
		RecommenderOfTesters.USER_SOPHISTICATED_IN_SELECTING_TESTERS = sophisticated;
		DD.setAppBoolean(DD.APP_USER_SOPHISTICATED_IN_SELECTING_TESTERS, USER_SOPHISTICATED_IN_SELECTING_TESTERS);
	}
	private long timeLeftInMillis(String last) {
		if (last == null) return 0;
		Calendar _last = Util.getCalendar(last, null);
		if (_last == null) return 0;
		Calendar now = Util.CalendargetInstance();
		return _last.getTimeInMillis() - now.getTimeInMillis() + EXPIRATION_DELAY_PRODUCED_RECOMMENDATIONS_IN_DAYS*24*60*60*1000l;
	}
/** For unit test
	@Override 
	public void run(){
			if(DEBUG)System.out.println("1)RUN()");
		try {
			recommendTesters();
			if(DEBUG)System.out.println("2)RUN()");
			RecommendationOfTestersSender.announceRecommendation(knownTestersList, usedTestersList);
			if(DEBUG)System.out.println("3)RUN()");
		} catch(Exception e) {
			if(DEBUG)e.printStackTrace();
		}
		//_run();
	}
*/
	@Override
	public void _run() {
		if (DEBUG) System.out.println("___RUN()");
		for (;;) {
			if (STOP_RECOMMENDATION_OF_TESTERS) break;
			long delayLeftInMillis;
			String last;
			last = DD.getAppTextNoException(DD.APP_LAST_RECOMMENDATION_OF_TESTERS);
			if ((delayLeftInMillis = timeLeftInMillis(last)) > 0) {
				try {
					sleep(delayLeftInMillis);
				} catch (InterruptedException e) {
					if (DEBUG) e.printStackTrace();
				}
				continue;
			}
			try {
				recommendTesters();
				RecommendationOfTestersSender.announceRecommendation(knownTestersList, usedTestersList);
			} catch(Exception e) {
				
			}
			DD.setAppTextNoException(DD.APP_LAST_RECOMMENDATION_OF_TESTERS, Util.getGeneralizedTime());
		}
	}
	TesterAndScore[] knownTestersList;
	TesterAndScore[] usedTestersList;
	/**
	 * Read all recommendations,
	 * Store result in local tables used by the local update modules. 
	 */
	private void recommendTesters() {
		Float scoreMatrix[][];
		Long[] receivedTesters;
		synchronized (this) {
			receivedTesters = D_RecommendationOfTester.retrieveAllTestersLIDFromRecievedRecommendation().toArray(new Long[0]);
			if(DEBUG) System.out.println("[   T"+ Util.concat(receivedTesters, " | T")+"]");
			Long[] sourcePeers = D_RecommendationOfTester.retrieveAllRecommendationSenderPeerLID().toArray(new Long[0]);
			scoreMatrix = extractScoreMatrix(sourcePeers, receivedTesters);
			
			if(!DEBUG) System.out.println("[   T"+ Util.concat(receivedTesters, " | T")+"]");
			if(!DEBUG){ 
				for(int i=0; i<sourcePeers.length; i++){
					System.out.print("P"+sourcePeers[i]);
					for(int j=0; j<receivedTesters.length; j++)
						System.out.print(" "+scoreMatrix[i][j]);
					System.out.println();
				}
					
                				
			}
		}
		knownTestersList = buildKnownTestersList(receivedTesters, scoreMatrix);
		if(!DEBUG) System.out.println("knownTestersList: ["+ Util.concat(knownTestersList, "|")+"]"); 
		TesterAndScore[] currentUsedTesters = retriveCurrentUsedTesters();
		if(!DEBUG) System.out.println("currentUsedTesters: ["+ Util.concat(currentUsedTesters, "|")+"]");
		usedTestersList = buildusedTestersList(currentUsedTesters);
		if(!DEBUG) System.out.println("usedTestersList: ["+ Util.concat(usedTestersList, "|")+"]");
		updateTestersWieght();
	}
	private void updateTestersWieght() {
		// all weights set to -1 , flags set to 0 (reference, used,..)
		D_Tester.initAllTestersRecommendations();
		setKnownTesters(knownTestersList);
		setUsedTesters(usedTestersList);
		
	}
	
	private void setUsedTesters(TesterAndScore[] usedTestersList) {
		for(int i = 0; i<usedTestersList.length; i++) {
			String score = ""+usedTestersList[i].score;
			if (usedTestersList[i].score == -1) score = null;
			D_Tester.setUsedTester(usedTestersList[i].testerID, score);
		}
	}
	
	private void setKnownTesters(TesterAndScore[] knownTestersList) {
		for(int i = 0; i < knownTestersList.length; i++) {
			String score = ""+knownTestersList[i].score;
			if (knownTestersList[i].score == -1) score = null;
			D_Tester.setKnownTester(knownTestersList[i].testerID, score);
		}
	}
	
	/**
	 * Create  the list that is specifying the testers to be used in evaluations of updates.
	 * It has to have size at most N
	 * @param knownTestersList
	 * @param currentUsedTesters
	 * @return
	 */
	private TesterAndScore[] buildusedTestersList(
			TesterAndScore[] currentUsedTesters) {
		
		if (currentUsedTesters.length < N_MAX_NUMBER_OF_TESTERS && currentUsedTesters.length <= knownTestersList.length)
			return TopNwithFilter(knownTestersList, N_MAX_NUMBER_OF_TESTERS);
		
		ArrayList<TesterAndScore>  usedTestersListTemp = new ArrayList<TesterAndScore>();
		
		if (currentUsedTesters.length < N_MAX_NUMBER_OF_TESTERS && currentUsedTesters.length > knownTestersList.length)
			for(int i=0; i < currentUsedTesters.length; i++)
				usedTestersListTemp.add(currentUsedTesters[i]);
		else{
		      assert(currentUsedTesters.length >= N_MAX_NUMBER_OF_TESTERS);
		      //if(currentUsedTesters.length>=N)
		      for(int i=0; i < N_MAX_NUMBER_OF_TESTERS; i++)
			      usedTestersListTemp.add(currentUsedTesters[i]); // usedTesters got the topN of currentList
		}
		if(DEBUG) System.out.println("usedTestersList: ["+ Util.concat(usedTestersListTemp.toArray(new TesterAndScore[0]), "|")+"]");
		removeAndReplaceNigativeWieght(usedTestersListTemp);
		TesterAndScore candidateTesterForReplacement=null;
		int index;
		//changing the weight of testers in the UT list with new testers's weight in KT list
		for(int i=0; i<knownTestersList.length; i++)
			if((index=isExist(knownTestersList[i], usedTestersListTemp))!=-1){
				//update score for used testers list from KT list
				usedTestersListTemp.get(index).score = knownTestersList[i].score;
			} 
			else if(candidateTesterForReplacement==null){
				candidateTesterForReplacement = knownTestersList[i]; 
			}
		if(candidateTesterForReplacement!=null && switchBasedOnPr()){
					usedTestersListTemp.get(usedTestersListTemp.size()-1).testerID = candidateTesterForReplacement.testerID;
					usedTestersListTemp.get(usedTestersListTemp.size()-1).score = candidateTesterForReplacement.score;
				    Collections.sort(usedTestersListTemp);
		}		
		return usedTestersListTemp.toArray(new TesterAndScore[0]);
	}
	private void removeAndReplaceNigativeWieght(
			ArrayList<TesterAndScore> usedTesterListTemp) {
		int index;
		int removeCount=0;
		// Remove Step
		for(int i=0; i<knownTestersList.length; i++)
			if((index=isExist(knownTestersList[i], usedTesterListTemp))!=-1){
				if(knownTestersList[i].score<=0){
				     usedTesterListTemp.remove(index);		    
					removeCount++;
				}
			} 
		// Replace Step (Add)
		boolean found=false;
		for(int k=0; k<removeCount; k++){
			for(int i=0; i<knownTestersList.length; i++)
				if((index=isExist(knownTestersList[i], usedTesterListTemp))==-1  
				                                         && knownTestersList[i].score>0){
					     usedTesterListTemp.add(knownTestersList[i]);		    
					     found=true;
				}
			if(found==false)
				break;
			} 
		
	}
	private boolean switchBasedOnPr() {
		Random rand = new Random();
		float index = rand.nextFloat();
		if(index>=0.0 &&  index<Pr_PROBABILITY_OF_REPLACING_A_USED_TESTER_WITH_A_HIGHER_SCORED_ONE)
			return true;
		return false;
	}
	private static int isExist(TesterAndScore testerAndScore,
			ArrayList<TesterAndScore> usedTestersList) {
		for(int i =0; i<usedTestersList.size(); i++)
			if(testerAndScore.testerID==usedTestersList.get(i).testerID)
				return i;
		return -1;
	}
	static int isExist(TesterAndScore testerAndScore,
			                   TesterAndScore[] usedTestersList) {
		for(int i =0; i<usedTestersList.length; i++)
			if(testerAndScore.testerID==usedTestersList[i].testerID)
				return i;
		return -1;
	}
	private TesterAndScore[] TopN(TesterAndScore[] TestersList, int nMaxNumberOfTesters) {
		int topNsize = nMaxNumberOfTesters;
		if(TestersList.length<nMaxNumberOfTesters)
			topNsize = TestersList.length;
		TesterAndScore[] topN = new TesterAndScore[topNsize];
		for(int i=0; i<topNsize; i++)
			topN[i] = TestersList[i];	
		return topN;
	}
	private TesterAndScore[] TopNwithFilter(TesterAndScore[] TestersList, int nMaxNumberOfTesters) {
		ArrayList<TesterAndScore> topNlist = new ArrayList<TesterAndScore>();
		int count=0;
		for(TesterAndScore tScore:TestersList){
			if(tScore.score<=0)
				continue;
			if(count>=nMaxNumberOfTesters)
				break;
			topNlist.add(tScore);
		}	
		return topNlist.toArray(new TesterAndScore[0]);
	}
/**
 retrieve from the database the currently used testers
*/
	private TesterAndScore[] retriveCurrentUsedTesters() {
		ArrayList<D_Tester> usedTesters = D_Tester.retrieveAllUsedTesters();
		if(DEBUG) System.out.println("retriveCurrentUsedTesters():usedTesters: ["
		                              + Util.concat(usedTesters.toArray(new D_Tester[0]), "|")+"]");
		ArrayList<TesterAndScore> testerAndScore = new ArrayList<TesterAndScore>();
		TesterAndScore ts =null;
		for(int i=0; i<usedTesters.size(); i++){
			ts = new TesterAndScore();
			ts.testerID = usedTesters.get(i).tester_ID;
			ts.score = Util.Fval(usedTesters.get(i).trustWeight, null);
			testerAndScore.add(ts);
		}
		
		Collections.sort(testerAndScore);
		return testerAndScore.toArray(new TesterAndScore[0]) ;
	}
/**
List after applying the formula for all testers received from others.
*/
	private TesterAndScore[] buildKnownTestersList(Long[] receivedTesters,
			Float[][] scoreMatrix) {
		int n = receivedTesters.length;
		ArrayList<TesterAndScore> knownTestersList = new  ArrayList<TesterAndScore>();
		Float newWeight = null;
		for(int i=0; i<n; i++){
			newWeight = calculateWeight(scoreMatrix,i, n);
			TesterAndScore tScore = new TesterAndScore();
			tScore.testerID = receivedTesters[i];
			tScore.score = newWeight;
			knownTestersList.add(tScore);
		}
		Collections.sort(knownTestersList);
		return knownTestersList.toArray(new TesterAndScore[0] );
	}
	private Float calculateWeight(Float[][] scoreMatrix,int testerIndex, int n) {
		//ArrayList<Float> testerScoresList = new ArrayList<Float> ;
		int countScores = 0;
		float maxScore = 0;
		
		for(int i=0; i<scoreMatrix.length; i++){
			if(scoreMatrix[i][testerIndex] !=null){
				countScores++;
				if(maxScore<scoreMatrix[i][testerIndex].floatValue())
					maxScore=scoreMatrix[i][testerIndex].floatValue();
			}
		}
		
		Float newWeight= (Float) f_AMORTIZATION_OF_SCORE_APPLIED_AT_EACH_TRANSFER_BETWEEN_USERS * (1 - countScores/(k_DIVERSITY_FACTOR_AS_TRADEOFF_AGAINST_PROXIMITY*n)) * maxScore;
		
		return newWeight;
	}
	private Float[][] extractScoreMatrix(Long[] sourcePeers,
			Long[] receivedTesters) {
		Float scoreMatrix[][] = new Float[sourcePeers.length][receivedTesters.length];
		// -1 means a tester is not yet rated by a peer
		initMatrix(scoreMatrix, null);
		for(int i=0; i<sourcePeers.length; i++){
			ArrayList<D_RecommendationOfTester> recomList = D_RecommendationOfTester.retrieveAllRecommendationTestersKeep(""+sourcePeers[i]);
			for(int j=0; j<receivedTesters.length;j++)
				scoreMatrix[i][j] = scoreForTester(recomList, receivedTesters[j]);
		}
		return scoreMatrix;
	}
	private Float scoreForTester(ArrayList<D_RecommendationOfTester> recomList,
			Long testerID) {
		if(DEBUG) System.out.println("scoreForTester() recomList size: "+ recomList.size() +" testerID: "+testerID);
		for(D_RecommendationOfTester rList : recomList)
			if(rList.testerLID == testerID)
				return rList._weight;
		return null;
	}
	private void initMatrix(Float[][] scoreMatrix, Float initValue) {
		for(int i=0; i < scoreMatrix.length ; i++)
			for(int j=0; j < scoreMatrix[i].length; j++)
				scoreMatrix[i][j]= initValue;
	}
	
}

class TesterAndScore implements Comparable<TesterAndScore>{
	private static final boolean _DEBUG = true;
	long testerID;
	Float score;
	@Override
	public int compareTo(TesterAndScore testerScore) {
		assert(this.score != null && testerScore.score != null);
		if (this.score == null) {
			this.score = 100.0f;
			if (_DEBUG) System.out.println("RecommenderOfTester: compareTo: why null score for this: "+this);
		}
		if (testerScore.score == null) {
			testerScore.score = 100.0f;
			if (_DEBUG) System.out.println("RecommenderOfTester: compareTo: why null score for tester: "+testerScore);
		}

		if ( this.score.floatValue() == testerScore.score.floatValue())
            return 0;
        else if (this.score.floatValue() < testerScore.score.floatValue())
            return 1;
        else
            return -1;
	}
	
	@Override
	public String toString(){
		String result= testerID+":"+score;
		return result;
	}
}
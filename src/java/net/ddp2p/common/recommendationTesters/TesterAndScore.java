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
package net.ddp2p.common.recommendationTesters;



public class TesterAndScore implements Comparable<TesterAndScore>{
	private static final boolean _DEBUG = true;
	public long testerID;
	public Float score;
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
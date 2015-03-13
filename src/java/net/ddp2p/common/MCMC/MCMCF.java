package net.ddp2p.common.MCMC;

import java.util.ArrayList;
import java.util.Random;
class witness_item{
	int witnessed;
	int[] colors_witness = new int[MCMCF.C];
	witness_item(){}
	witness_item(int _witnessed, int[]_cw){witnessed = _witnessed; colors_witness = _cw;}
}
public class MCMCF {
	final public static int RW = 1;
	final public static int CS = 2;
	final public static int T = 1;
	final public static int F = 0;
	final public static int ___ = -1;
	int n; // number of consts
	public static int C = 2;
	//int[][][] witness; // witness[A][B][color]
	ArrayList<witness_item>[]witness; // witness[a].add(new witness_item(b, new int[]{1,1}))
	
	// query variables
	int N[][];
	int state[][];
	boolean fixed[][];
	int fixedC=0;

	// CPTs
	double prior[]=new double[]{0,0.5,0.5}; //[2]
	double CPT_CS[][]; // CPT[cs,rw]
	double CPT_RW[][]; // CPT[rwa,rwb]
	double CPT_observer_CS[][]; // CPT[cs,rw]
	double CPT_observer_RW[][]; // CPT[rwa,rwb]
	double[][] CPT_self_CS; //[cs,rw]
	double[] CPT_self_RW;
	double[][] CPT_observer_self_CS; //[cs,rw]
	double[] CPT_observer_self_RW;
	double sums_T[][]; // [a][c]
	double sums_F[][]; 
	double Pac[][];
	public int observer; 
	static Random rnd = new Random(0);
	private int sample(double d) {
		 float v = random(1.0f);
		 if(v<d) return T;
		 return F;
	}
	public static float random(float max){
		float result = rnd.nextFloat()*max;
		return result;
	}

	public void mcmc(int rounds) {
		for(int r=0; r < rounds; r++) {
			for(int a=1; a<=n; a++) {
				if(!fixed[a][RW])mcmc_A_RW(a);
				if(!fixed[a][CS])mcmc_A_CS(a,r);
			}
		}		
	}

	private void mcmc_A_CS(int a, int round) {
		// TODO update based on a is witnessed
		boolean dbg = false;
		//if((round>=R_error)&&(a==8)) dbg = true;
		
		double alpha_T = 1*prior[CS];
		double alpha_F = 1*(1-prior[CS]);
		for(int _b=1; _b<=witness[a].size(); _b++) {
			witness_item Bs = witness[a].get(_b);
			int b = Bs.witnessed;
			//....
			double[][] CPT_crt_CS = CPT_CS;
			double[][] CPT_crt_self_CS = CPT_self_CS;
			if(b==observer){CPT_crt_CS = CPT_observer_CS;CPT_crt_self_CS=CPT_observer_self_CS;}
			if(a!=b) {
				if(Bs.colors_witness[CS]!=___) {// a summation for Wba_CS
					if(Bs.colors_witness[CS]==T){
						int state_b_RW = state[a][RW];
						alpha_T *= CPT_crt_CS[state_b_RW][T];
						alpha_F *= CPT_crt_CS[state_b_RW][F];
					}else{
						int state_b_RW = state[a][RW];
						alpha_T *= 1-CPT_crt_CS[state_b_RW][T];
						alpha_F *= 1-CPT_crt_CS[state_b_RW][F];
					}
				}
				/// handle result in the right aggregator
				state[b][CS] = sample(alpha_T/(alpha_T+alpha_F));
				if(dbg) {
					System.out.println("\n8: alpha_T="+alpha_T+" F="+alpha_F);
				}
				if(state[b][CS]==T) N[b][CS]++;
			}else{
				if(Bs.colors_witness[CS]!=___) {// a summation for Waa_CS
					if(Bs.colors_witness[CS]==T) {
						int state_a_RW = state[a][RW];
						alpha_T *= CPT_crt_self_CS[state_a_RW][T];
						alpha_F *= CPT_crt_self_CS[state_a_RW][F];
					}else{
						int state_a_RW = state[a][RW];
						alpha_T *= 1-CPT_crt_self_CS[state_a_RW][T];
						alpha_F *= 1-CPT_crt_self_CS[state_a_RW][F];
					}
				}
				///
				state[a][CS] = sample(alpha_T/(alpha_T+alpha_F));
				if(dbg) {
					System.out.println("\n8: alpha_T="+alpha_T+" F="+alpha_F);
				}
				if(state[a][CS]==T) N[a][CS]++;
			}
		}
	}

	private void mcmc_A_RW(int a) {
		// TODO updated based on how he witnesses
		// TODO updated based on how he is witnessed		
	}
}
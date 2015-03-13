package net.ddp2p.common.MCMC;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Random;
import java.util.Scanner;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.util.Util;

class BN{
	final public static int RW = 1;
	final public static int CS = 2;
	final public static int T = 1;
	final public static int F = 0;
	final public static int ___ = -1;
	int n; // number of consts
	int C =2;
	// observable
	//int[][] self_censable; // witness[A][color]
	int[][][] witness; // witness[A][B][color]

	// query variables
	int N[][];
	int state[][];
	boolean fixed[][];
	int fixedC=0;
	//double[][] status; // status[A][color]
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
	public static float random(float max){
		float result = rnd.nextFloat()*max;
		return result;
	}
	
	private void setStatus(BigInteger cnt) {
		int i=0;
//		System.out.println(cnt+" "+cnt.testBit(0));
		cnt.byteValue();
		for(int a=1;a<=n;a++)
			for(int c=1;c<=C;c++){
				if(fixed[a][c]) continue;
				state[a][c] = cnt.testBit(i++)?F:T;
				//System.out.println("setStatus[a="+a+"][c="+c+"]: "+state[a][c]);
			}
		//if(i != bits) System.out.println("max status bits="+i+" max="+bits);
	}
	BigInteger inc(BigInteger c){
		synchronized(this){
			cnt=cnt.add(BigInteger.ONE);
		}
		return cnt;
	}
	int bits=0;
	BigInteger cnt=BigInteger.ZERO, max=BigInteger.ONE;
	double enumerate(int _a, int _c){
		bits = n*C-fixedC;
		System.out.println("bits="+bits);
		max = BigInteger.ONE.shiftLeft(bits);
		cnt = BigInteger.ZERO;
		double sum_T = 0;
		double sum_F = 0;
		double prob_fixed=1;
		for(int __a=1;__a<=n;__a++)
			for(int __c=1;__c<=C;__c++){
				Pac[__a][__c] = sums_T[__a][__c] = sums_F[__a][__c] = 0;
				//if(fixed[__a][__c]) prob_fixed *= prior[__c];
			}
		for(; cnt.compareTo(max)<0; inc(cnt)) {
			setStatus(cnt);
			//if(state[observer][RW] != T) continue;

			//for(int __a=1;__a<=n;__a++)for(int __c=1;__c<=C;__c++)System.out.print(" "+__a+":"+__c+"="+state[__a][__c]);
			//System.out.println(" ");
			
			double prob = 1;///prob_fixed;
			for(int a=1;a<=n;a++)
				for(int c=1;c<=C;c++){
					if(!fixed[a][c])
						prob *= prior[c]; 
				}
			for(int a=1; a<=n; a++){
				if(a==observer) continue; //assumtion its probs are all 1
				for(int b=1; b<=n; b++){
					//System.out.println("   a="+a+" b="+b+" wr="+witness[a][b][RW]+" p="+prob);
					if(witness[a][b][RW]!=___){
						double s;
						double[][] CPT_crt_RW = CPT_RW;
						double[] CPT_crt_self_RW = CPT_self_RW;
						if(a==observer){CPT_crt_RW = CPT_observer_RW;CPT_crt_self_RW=CPT_observer_self_RW;}
						if(a!=b)
							s = CPT_crt_RW[state[a][RW]][state[b][RW]];
						else
							s = CPT_crt_self_RW[state[a][RW]];
						//System.out.println("    s_a_r="+state[a][RW]+" s_b_r="+state[b][RW]+" CPT="+s);
						if(witness[a][b][RW]==T){
							prob *= s;
						}else{
							prob *= 1-s;
						}
					}					
					//System.out.println("   a="+a+" b="+b+" wc="+witness[a][b][CS]+" p="+prob);
					if(witness[a][b][CS]!=___){
						double s;
						double[][] CPT_crt_CS = CPT_CS;
						double[][] CPT_crt_self_CS = CPT_self_CS;
						if(a==observer){CPT_crt_CS = CPT_observer_CS;CPT_crt_self_CS=CPT_observer_self_CS;}
						if(a!=b)
							s = CPT_crt_CS[state[a][RW]][state[b][CS]];
						else
							s = CPT_crt_self_CS[state[a][RW]][state[b][CS]];
						//System.out.println("    s_a_c="+state[a][CS]+" s_b_s="+state[b][RW]+" CPT="+s);
						if(witness[a][b][CS]==T){
							prob *= s;
						}else{
							prob *= 1-s;
						}
					}
				}
			}
			//System.out.println("  cnt="+cnt+"/"+max+" p="+prob);
			if(state[_a][_c]==T) {
				sum_T += prob;
			}else{
				sum_F += prob;
			}
			for(int __a=1;__a<=n;__a++)
				for(int __c=1;__c<=C;__c++)
					if(state[__a][__c]==T) {
						sums_T[__a][__c] += prob;
					}else{
						sums_F[__a][__c] += prob;
					}
		}
		for(int __a=1;__a<=n;__a++)
			for(int __c=1;__c<=C;__c++){
				Pac[__a][__c] = sums_T[__a][__c]/(sums_T[__a][__c]+sums_F[__a][__c]);
				//System.out.println("Prob ["+__a+"]["+__c+"]="+(sums_T[__a][__c]+sums_F[__a][__c]));
			}
		
		return sum_T/(sum_T+sum_F);
	}
	/**
	 * Assign values to each query variable
	 */
	public void init() {
		for(int a=1 ; a<=n; a++)
			for(int c=1; c<=C; c++){
				//status[a][c] = (int)Math.floor(Util.random(2));
				state[a][c] = (int)Math.floor(random(2));
				fixed[a][c] = false;
				N[a][c] = 0;
			}
		//System.out.println("State length="+state.length);
		if((observer>0)&&(observer<=n)){
			for(int a=1 ; a<=n; a++){
				for(int c=1; c<=C; c++){
					if((this.witness[observer][a][c]!=___) && (getObserverCPT(observer,a,c)==0)) {
						fixed[a][c] = true; this.fixedC++;
						state[a][c] = witness[observer][a][c];
						//System.out.println("initStatus[a="+a+"][c="+c+"]: "+state[a][c]);
					}
				}
			}
			if(fixed[observer][RW] == false) {
				fixed[observer][RW] = true;
				fixedC++;
				state[observer][RW] = T;
			}
		}else if(observer!=0)System.out.println("Invalid observer!");
	}
	private double getObserverCPT(int observer2, int a, int c2) {
		if(observer==a){
			switch(c2){
			case CS:
				return total(this.CPT_observer_self_CS[T][T]);
			case RW:
				return total(this.CPT_observer_self_RW[T]);
			}
		}else{
			switch(c2){
			case CS:
				return total(this.CPT_observer_CS[T][T]);
			case RW:
				return total(this.CPT_observer_RW[T][T]);
			}
		}
		return 0;
	}

	private double total(double d) {
		return d*(1-d);
	}

	//int R_error = 1147243;
	public void mcmc(int rounds) {
		for(int r=0; r< rounds; r++) {
			for(int a=1; a<=n; a++) {
				//if(a!=observer)
				if(!fixed[a][RW])
					mcmc_A_RW(a);
				if(!fixed[a][CS])
					mcmc_A_CS(a,r);
			}
			//System.out.println(r+": Obama RW="+N[8][RW]+" CS="+N[8][CS]);
			//if(r<R_error) continue;
			//System.out.print(r+":"+N[8][CS]+" ");
			//if(N[8][CS]!=0) return;
		}
		System.out.println("n:"+n);
		for(int a=1; a<=n;a++) {
			for(int c=1; c<=C; c++){
				System.out.println("A"+a+"["+c+"]="+(N[a][c]*1.0/rounds));
				//if(c==2&&a==13)//8:Obama
					//System.out.println("Micky="+(N[a][c]*1.0/rounds));
			}
		}
	}
	private void mcmc_A_RW(int a) {
		double alpha_T = 1*prior[RW];
		double alpha_F = 1*(1-prior[RW]);
		
		
		for(int b=1; b<=n; b++) {
			double[][] CPT_crt_CS = CPT_CS;
			double[][] CPT_crt_self_CS = CPT_self_CS;
			double[][] CPT_crt_RW = CPT_RW;
			double[] CPT_crt_self_RW = CPT_self_RW;
			
			if(a!=b) {
				// a summation for Wab_RW
				if(witness[a][b][RW]!=___){
					if(witness[a][b][RW]==T){
						//alpha_T *= (CPT_crt_RW[T][T]*state[b][RW]+CPT_crt_RW[T][F]*(1-state[b][RW]));
						//alpha_F *= (CPT_crt_RW[F][T]*state[b][RW]+CPT_crt_RW[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= CPT_crt_RW[T][state_b_RW];
						alpha_F *= CPT_crt_RW[F][state_b_RW];
					}else{
						//alpha_T *= 1-(CPT_crt_RW[T][T]*state[b][RW]+CPT_crt_RW[T][F]*(1-state[b][RW]));
						//alpha_F *= 1-(CPT_crt_RW[F][T]*state[b][RW]+CPT_crt_RW[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= 1-CPT_crt_RW[T][state_b_RW];
						alpha_F *= 1-CPT_crt_RW[F][state_b_RW];
					}
				}
				// a summation for Wab_CS
				if(witness[a][b][CS]!=___){
					if(witness[a][b][CS]==T){
						//alpha_T *= (CPT_crt_CS[T][T]*state[b][CS]+CPT_crt_CS[T][F]*(1-state[b][CS]));
						//alpha_F *= (CPT_crt_CS[F][T]*state[b][CS]+CPT_crt_CS[F][F]*(1-state[b][CS]));
						int state_b_CS = state[b][CS];
						alpha_T *= CPT_crt_CS[T][state_b_CS];
						alpha_F *= CPT_crt_CS[F][state_b_CS];
					}else{
						//alpha_T *= 1-(CPT_crt_CS[T][T]*state[b][CS]+CPT_crt_CS[T][F]*(1-state[b][CS]));
						//alpha_F *= 1-(CPT_crt_CS[F][T]*state[b][CS]+CPT_crt_CS[F][F]*(1-state[b][CS]));
						int state_b_CS = state[b][CS];
						alpha_T *= 1-CPT_crt_CS[T][state_b_CS];
						alpha_F *= 1-CPT_crt_CS[F][state_b_CS];
					}
				}
				
				if(b==observer){CPT_crt_CS = CPT_observer_CS;CPT_crt_self_CS=CPT_observer_self_CS;}
				if(b==observer){CPT_crt_RW = CPT_observer_RW;CPT_crt_self_RW=CPT_observer_self_RW;}
				// a summation for Wba_RW
				if(witness[b][a][RW]!=___){
					if(witness[b][a][RW]==T){
						//alpha_T *= (CPT_crt_RW[T][T]*state[b][RW]+CPT_crt_RW[F][T]*(1-state[b][RW]));
						//alpha_F *= (CPT_crt_RW[T][F]*state[b][RW]+CPT_crt_RW[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= CPT_crt_RW[state_b_RW][T];
						alpha_F *= CPT_crt_RW[state_b_RW][F];
					}else{
						//alpha_T *= 1-(CPT_crt_RW[T][T]*state[b][RW]+CPT_crt_RW[F][T]*(1-state[b][RW]));
						//alpha_F *= 1-(CPT_crt_RW[T][F]*state[b][RW]+CPT_crt_RW[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= 1-CPT_crt_RW[state_b_RW][T];
						alpha_F *= 1-CPT_crt_RW[state_b_RW][F];
					}
				}
			}else{
				if(witness[a][a][CS]!=___) {// a summation for Waa_CS
					if(witness[a][a][CS]==T) {
						//alpha_T *= (CPT_crt_self_CS[T][T]*state[a][CS]+CPT_crt_self_CS[T][F]*(1-state[a][CS]));
						//alpha_F *= (CPT_crt_self_CS[F][T]*state[a][CS]+CPT_crt_self_CS[F][F]*(1-state[a][CS]));
						int state_a_CS = state[a][CS];
						alpha_T *= CPT_crt_self_CS[T][state_a_CS];
						alpha_F *= CPT_crt_self_CS[F][state_a_CS];
					}else{
						//alpha_T *= 1-(CPT_crt_self_CS[T][T]*state[a][CS]+CPT_crt_self_CS[F][T]*(1-state[a][CS]));
						//alpha_F *= 1-(CPT_crt_self_CS[T][F]*state[a][CS]+CPT_crt_self_CS[F][F]*(1-state[a][CS]));
						int state_a_CS = state[a][CS];
						alpha_T *= 1-CPT_crt_self_CS[T][state_a_CS];
						alpha_F *= 1-CPT_crt_self_CS[F][state_a_CS];
					}
				}
				if(witness[a][a][RW]!=___) {// a summation for Waa_CS
					if(witness[a][a][RW]==T) {
						alpha_T *= (CPT_crt_self_RW[T]);//*state[b][RW]+CPT_crt_self_RW[F]*(1-state[b][RW]));
						alpha_F *= (CPT_crt_self_RW[F]);//*state[b][RW]+CPT_crt_self_RW[F]*(1-state[b][RW]));
					}else{
						alpha_T *= 1-(CPT_crt_self_RW[T]);//*state[b][CS]+CPT_crt_self_RW[[T]*(1-state[b][CS]));
						alpha_F *= 1-(CPT_crt_self_RW[F]);//*state[b][CS]+CPT_crt_self_RW[F]*(1-state[b][CS]));
					}
				}
			}
		}
		state[a][RW]=sample(alpha_T/(alpha_T+alpha_F));
		if(state[a][RW]==T) N[a][RW]++;
	}
	
	private void mcmc_A_CS(int a, int round) {
		boolean dbg = false;
		//if((round>=R_error)&&(a==8)) dbg = true;
		
		double alpha_T = 1*prior[CS];
		double alpha_F = 1*(1-prior[CS]);
		for(int b=1; b<=n; b++) {
			double[][] CPT_crt_CS = CPT_CS;
			double[][] CPT_crt_self_CS = CPT_self_CS;
			if(b==observer){CPT_crt_CS = CPT_observer_CS;CPT_crt_self_CS=CPT_observer_self_CS;}
			if(a!=b) {
				if(witness[b][a][CS]!=___) {// a summation for Wba_CS
					if(witness[b][a][CS]==T){
						//alpha_T *= (CPT_crt_CS[T][T]*state[b][RW]+CPT_crt_CS[F][T]*(1-state[b][RW]));
						//alpha_F *= (CPT_crt_CS[T][F]*state[b][RW]+CPT_crt_CS[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= CPT_crt_CS[state_b_RW][T];
						alpha_F *= CPT_crt_CS[state_b_RW][F];
					}else{
						//alpha_T *= 1-(CPT_crt_CS[T][T]*state[b][RW]+CPT_crt_CS[F][T]*(1-state[b][RW]));
						//alpha_F *= 1-(CPT_crt_CS[T][F]*state[b][RW]+CPT_crt_CS[F][F]*(1-state[b][RW]));
						int state_b_RW = state[b][RW];
						alpha_T *= 1-CPT_crt_CS[state_b_RW][T];
						alpha_F *= 1-CPT_crt_CS[state_b_RW][F];
					}
				}
			}else{
				if(witness[a][a][CS]!=___) {// a summation for Waa_CS
					if(witness[a][a][CS]==T) {
						//alpha_T *= (CPT_crt_self_CS[T][T]*state[a][RW]+CPT_crt_self_CS[F][T]*(1-state[a][RW]));
						//alpha_F *= (CPT_crt_self_CS[T][F]*state[a][RW]+CPT_crt_self_CS[F][F]*(1-state[a][RW]));
						int state_a_RW = state[a][RW];
						alpha_T *= CPT_crt_self_CS[state_a_RW][T];
						alpha_F *= CPT_crt_self_CS[state_a_RW][F];
					}else{
						//alpha_T *= 1-(CPT_crt_self_CS[T][T]*state[a][RW]+CPT_crt_self_CS[F][T]*(1-state[a][RW]));
						//alpha_F *= 1-(CPT_crt_self_CS[T][F]*state[a][RW]+CPT_crt_self_CS[F][F]*(1-state[a][RW]));
						int state_a_RW = state[a][RW];
						alpha_T *= 1-CPT_crt_self_CS[state_a_RW][T];
						alpha_F *= 1-CPT_crt_self_CS[state_a_RW][F];
					}
				}
			}
		}
		state[a][CS] = sample(alpha_T/(alpha_T+alpha_F));
		if(dbg) {
			System.out.println("\n8: alpha_T="+alpha_T+" F="+alpha_F);
		}
		if(state[a][CS]==T) N[a][CS]++;
	}
	private int sample(double d) {
		 float v = random(1.0f);
		 if(v<d) return T;
		 return F;
	}
	// to be used with the parser
	private String storedLine = null;
	boolean hasNextLine(Scanner s){
		if(storedLine!=null) return true;
		storedLine = getLine(s);
		return (storedLine != null);
	}
	String getLine(Scanner s){
		String line = storedLine;
		if(line!=null){
			storedLine = null;
			return line;
		}
		try{
			line = s.nextLine();
			if(line.startsWith("#")) line = s.nextLine();
			if("".equals(line.trim())) line = s.nextLine();
		}catch(Exception e){
			return null;
		}
		return line;
	}
	 BN load(String[] args){

	        System.out.println("Arguments="+args.length);
	        System.out.println(args[2]);
	        System.out.println(args[3]);
	        Scanner s = null;
	        BufferedReader in = null;
	        int size = 0;
	        String line;
	        try {
	            in = new BufferedReader(
	                    new FileReader(args[2]));
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }
	        s = new Scanner(in);
	        size=Integer.parseInt(getLine(s));//Read the first line
	        System.out.println("Size="+size);
	        n=size;
	        this.observer=Integer.parseInt(args[4]);
//	        System.out.println("size:"+size);
	        witness=new int[size+1][size+1][C+1];
	        N=new int[size+1][C+1];
	        state=new int[size+1][C+1];
	        fixed=new boolean[size+1][C+1];
	        sums_T=new double[size+1][C+1];
	        sums_F=new double[size+1][C+1];
	        Pac=new double[size+1][C+1];
	        //self_censable=new int[size+1][C+1];
	        for(int i=1;i<=size;i++){
	            for(int j=1;j<=size;j++){
	                witness[i][j][RW]=-1;
	                witness[i][j][CS]=-1;
	            }
	        }
	        while (hasNextLine(s)) {
	            line = getLine(s);
	            String[] split = line.split(" ");
	            if(split.length==4){
	                int i=Integer.parseInt(split[0]);
	                int j=Integer.parseInt(split[1]);
	                int k=Integer.parseInt(split[2]);
	                int t=Integer.parseInt(split[3]);
	                witness[i][j][CS]=k;
	                witness[i][j][RW]=t;
	            }else{
	            	System.err.println("Each BN witness line should be: witnesser witnessed CS RW");
	            	System.err.println("Failure on line \""+line+"\"");
	            	Application_GUI.warning("Bad work!", "Bad work!");
	            	return null;
	            }
//	            if(split.length==2){
//	                int i=Integer.parseInt(split[0]);
//	                int j=Integer.parseInt(split[1]);
//	                self_censable[i][CS]=j;
////	                self_censable[i][1]=j;
//	            }
	        }
	        try {
	            in = new BufferedReader(
	                    new FileReader(args[3]));//Read CPT file
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }
	        CPT_CS=new double[2][2];
	        CPT_RW=new double[2][2];
	        CPT_self_CS=new double[2][2];
	        CPT_self_RW=new double[2];
	        CPT_observer_self_RW=new double[2];
    		CPT_observer_self_CS=new double[2][2];
    		CPT_observer_RW=new double[2][2];
    		CPT_observer_CS=new double[2][2];
	        s = new Scanner(in);
	        s.nextLine();//Read the name of CPT
	        s.nextLine();//Read the headers of CPT
	        CPT_CS[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_CS[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_CS[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_CS[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT
	        s.nextLine();//Read the headers of CPT
	        CPT_RW[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_RW[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_RW[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_RW[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT
	        s.nextLine();//Read the headers of CPT
	        CPT_self_CS[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_self_CS[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_self_CS[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_self_CS[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT: CPT_self_RW
	        s.nextLine();//Read the headers of CPT_self_RW
	        CPT_self_RW[T]=Double.parseDouble(s.nextLine().split(" ")[1]);
	        CPT_self_RW[F]=Double.parseDouble(s.nextLine().split(" ")[1]);
	        s.nextLine();//Read the name of CPT: Priors
	        s.nextLine();//Read the headers of CPT
	        String p= s.nextLine();
	        prior[CS]=Double.parseDouble(p.split(" ")[0]);
	        prior[RW]=Double.parseDouble(p.split(" ")[1]);
	        s.nextLine();//Read the name of CPT_observer_CS
	        s.nextLine();//Read the headers of CPT: CS_O RW_A P(W_AO_Psi)
	        CPT_observer_CS[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_CS[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_CS[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_CS[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT_observer_RW
	        s.nextLine();//Read the headers of CPT: RW_O RW_A P(W_AO_Phi)
	        CPT_observer_RW[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_RW[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_RW[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_RW[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT_observer_self_CS
	        s.nextLine();//Read the headers of CPT: CS_O RW_O P(W_OO_Psi)
	        CPT_observer_self_CS[T][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_self_CS[T][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_self_CS[F][T]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        CPT_observer_self_CS[F][F]=Double.parseDouble(s.nextLine().split(" ")[2]);
	        s.nextLine();//Read the name of CPT: CPT_observer_self_RW
	        s.nextLine();//Read the headers of RW_O P(W_OO_Psi)
	        CPT_observer_self_RW[T]=Double.parseDouble(s.nextLine().split(" ")[1]);
	        CPT_observer_self_RW[F]=Double.parseDouble(s.nextLine().split(" ")[1]);
	        return this;
	    }
	 /*
		private void _mcmc_A_CS(int a) {
			double alpha_T = 1*prior[CS];
			double alpha_F = 1*(1-prior[CS]);
			for(int b=1; b<=n; b++) {
				if(a!=b) {
					if(witness[b][a][CS]!=_) {// a summation for Wba_CS
						if(witness[b][a][CS]==T){
							alpha_T *= (CPT_CS[T][T]*status[b][RW]+CPT_CS[T][F]*(1-status[b][RW]));
							alpha_F *= (CPT_CS[F][T]*status[b][RW]+CPT_CS[F][F]*(1-status[b][RW]));
						}else{
							alpha_T *= 1-(CPT_CS[T][T]*status[b][RW]+CPT_CS[T][F]*(1-status[b][RW]));
							alpha_F *= 1-(CPT_CS[F][T]*status[b][RW]+CPT_CS[F][F]*(1-status[b][RW]));
						}
					}
				}else{
					if(self_censable[a][CS]!=_) {// a summation for Waa_CS
						if(self_censable[a][CS]==T) {
							alpha_T *= (CPT_self_CS[T][T]*status[b][RW]+CPT_self_CS[T][F]*(1-status[b][RW]));
							alpha_F *= (CPT_self_CS[F][T]*status[b][RW]+CPT_self_CS[F][F]*(1-status[b][RW]));
						}else{
							alpha_T *= 1-(CPT_self_CS[T][T]*status[b][RW]+CPT_self_CS[T][F]*(1-status[b][RW]));
							alpha_F *= 1-(CPT_self_CS[F][T]*status[b][RW]+CPT_self_CS[F][F]*(1-status[b][RW]));
						}
					}
				}
			}
			status[a][CS]=alpha_T/(alpha_T+alpha_F);
		}
		*/
	 /*
		private void _mcmc_A_RW(int a) {
			double alpha_T = 1*prior[RW];
			double alpha_F = 1*(1-prior[RW]);
			
			for(int b=1; b<=n; b++) {
				if(a!=b) {
					// a summation for Wab_RW
					if(witness[a][b][RW]!=_){
						if(witness[a][b][RW]==T){
							alpha_T *= (CPT_RW[T][T]*status[b][RW]+CPT_RW[T][F]*(1-status[b][RW]));
							alpha_F *= (CPT_RW[F][T]*status[b][RW]+CPT_RW[F][F]*(1-status[b][RW]));
						}else{
							alpha_T *= 1-(CPT_RW[T][T]*status[b][RW]+CPT_RW[T][F]*(1-status[b][RW]));
							alpha_F *= 1-(CPT_RW[F][T]*status[b][RW]+CPT_RW[F][F]*(1-status[b][RW]));
						}
					}
					// a summation for Wab_CS
					if(witness[a][b][CS]!=_){
						if(witness[a][b][CS]==T){
							alpha_T *= (CPT_CS[T][T]*status[b][CS]+CPT_CS[T][F]*(1-status[b][CS]));
							alpha_F *= (CPT_CS[F][T]*status[b][CS]+CPT_CS[F][F]*(1-status[b][CS]));
						}else{
							alpha_T *= 1-(CPT_CS[T][T]*status[b][CS]+CPT_CS[T][F]*(1-status[b][CS]));
							alpha_F *= 1-(CPT_CS[F][T]*status[b][CS]+CPT_CS[F][F]*(1-status[b][CS]));
						}
					}
					// a summation for Wba_RW
					if(witness[b][a][RW]!=_){
						if(witness[b][a][RW]==T){
							alpha_T *= (CPT_RW[T][T]*status[b][RW]+CPT_RW[F][T]*(1-status[b][RW]));
							alpha_F *= (CPT_RW[T][F]*status[b][RW]+CPT_RW[F][F]*(1-status[b][RW]));
						}else{
							alpha_T *= 1-(CPT_RW[T][T]*status[b][RW]+CPT_RW[F][T]*(1-status[b][RW]));
							alpha_F *= 1-(CPT_RW[T][F]*status[b][RW]+CPT_RW[F][F]*(1-status[b][RW]));
						}
					}
				}else{
					if(self_censable[a][CS]!=_) {// a summation for Waa_CS
						if(self_censable[a][CS]==T) {
							alpha_T *= (CPT_self_CS[T][T]*status[b][CS]+CPT_self_CS[F][T]*(1-status[b][CS]));
							alpha_F *= (CPT_self_CS[T][F]*status[b][CS]+CPT_self_CS[F][F]*(1-status[b][CS]));
						}else{
							alpha_T *= 1-(CPT_self_CS[T][T]*status[b][CS]+CPT_self_CS[F][T]*(1-status[b][CS]));
							alpha_F *= 1-(CPT_self_CS[T][F]*status[b][CS]+CPT_self_CS[F][F]*(1-status[b][CS]));
						}
					}
				}
			}
			status[a][RW]=alpha_T/(alpha_T+alpha_F);
		}
		*/
}
class Reporter extends Thread{
	private static final long TIMEOUT_MS = 3000;
	BN bn;
	Calendar date;
	Reporter(BN _bn){
		this.setDaemon(true);
		bn = _bn;
		date = Util.CalendargetInstance();
		//System.out.println("Will wait const "+TIMEOUT_MS);
	}
	public void run(){
		//System.out.println("Will wait "+TIMEOUT_MS);
		for(;;) {
			try {
				//System.out.println("Will wait "+TIMEOUT_MS); 
				synchronized(bn){
					bn.wait(TIMEOUT_MS);
				
					System.out.println("CRT "+bn.cnt+"/"+bn.max+"   at "+(Util.CalendargetInstance().getTimeInMillis()-date.getTimeInMillis()));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
public class MCMC{
	
	public static void main(String[] args) {
//		int observer=1;
		BN bn= new BN().load(args);
		
		bn.init();
//		bn.observer = observer;
		if("MCMC".equals(args[0]))
				bn.mcmc(Integer.parseInt(args[1]));
		if("ENUM".equals(args[0])){
			new Reporter(bn).start();
			for(int a=1;a<=bn.n;a++)
				for(int c=1;c<=bn.C;c++){
					double v = bn.enumerate (a,c);
					System.out.println("A="+a+" C="+c+" P="+v);
				}
		}
		if("ENUMALL".equals(args[0])){
			new Reporter(bn).start();
			double v = bn.enumerate (1,1);
			
			for(int a=1;a<=bn.n;a++)
				for(int c=1;c<=bn.C;c++){
					System.out.println("A="+a+" C="+c+" P="+bn.Pac[a][c]);
				}
		}
	}
}


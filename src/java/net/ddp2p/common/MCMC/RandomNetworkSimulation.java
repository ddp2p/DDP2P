package net.ddp2p.common.MCMC;
import java.util.Random;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class GraphNeighborhood{
	long[] IDs;
	int[] type;
	int[] e;
	int[][] neighbors;
	Random r = new Random();
	GraphNeighborhood(int size, int density){
		IDs = new long[size];
		type = new int[size];
		e = new int[size];
		neighbors = new int[size][];
		for(int k=0; k<size; k++) {
			neighbors[k] = new int[density];
			for(int i=0; i<density; i++){
				int j = r.nextInt(size);
				while(contains(j, neighbors[k],i)){
					j++;
					if(j>=size) j=0;
				}
				neighbors[k][i] = j;
			}
		}
		for(int a=0;a<size; a++){
			type[a] = 0; 
			e[a] = 1;
		}
	}
	private boolean contains(int needle, int[] haystack, int size) {
		for(int k=0; k<size; k++) if(haystack[k] == needle) return true;
		return false;
	}
	void createAttackers(int _type, float percentage){
		createAttackers(_type, (int)Math.ceil(percentage)*IDs.length);
	}
	void createAttackers(int _type, int number){
		for(int k=0; k<number; k++){
			int i = r.nextInt(IDs.length);
			while(type[i]!=0){
				i++;
				if(i>=IDs.length) i=0;
			}
			type[i] = _type;
		}
	}
	void createIneligible(int number){
		for(int k=0; k<number; k++){
			int i = r.nextInt();
			while(e[i]!=0) {
				i++;
				if(i>=IDs.length) i=0;
			}
			e[i] = 0;
		}
	}
}
public class RandomNetworkSimulation extends Thread{
	private static final int SIZE_NEIGHBORHOOD = 100;
	private static final int DENSITY_NEIGHBORHOOD = 20;
	private static final int NB_NEIGHBORHOODS = 100;
	private static final int NB_EXTERNAL_NEIGHBORS = 10;
	MCMC mcmc;
	int SizeofNetwork;
	int SizeofActiveHonestConstituents;
	int SizeofInactiveConstituents;
	int SizeofAttackerType1;
	int SizeofAttackerType2;
	int SizeofAttackerType3;
	int ObserverID = 0;
	String orgGID;
	Random r = new Random();
	private int SizeofIneligible;
	/**
	 * 
	 * @param global_organization_id
	 * @param sizeActiveHonestConsts
	 * @param nbAttackersIneligibleIDs_1
	 * @param nbAttackersWitnessForIneligible_2
	 * @param nbAttackersWitnessAgainstEligible_3
	 * @param nbIneligible
	 */
	public RandomNetworkSimulation( String global_organization_id,
			int sizeActiveHonestConsts,
			int nbAttackersIneligibleIDs_1, int nbAttackersWitnessForIneligible_2,
			int nbAttackersWitnessAgainstEligible_3,
			int nbIneligible){
		init(global_organization_id,
				sizeActiveHonestConsts, 0,
				nbAttackersIneligibleIDs_1, nbAttackersWitnessForIneligible_2,
				nbAttackersWitnessAgainstEligible_3,
				nbIneligible);
	}	
	RandomNetworkSimulation( String global_organization_id,
			int sizeActiveHonestConsts,int sizeInactiveConsts,
			int nbAttackersIneligibleIDs_1, int nbAttackersWitnessForIneligible_2,
			int nbAttackersWitnessAgainstEligible_3,
			int nbIneligible){
		init(global_organization_id,
				sizeActiveHonestConsts, sizeInactiveConsts,
				nbAttackersIneligibleIDs_1, nbAttackersWitnessForIneligible_2,
				nbAttackersWitnessAgainstEligible_3,
				nbIneligible);
	}
	void init( String global_organization_id,
			int sizeActiveHonestConsts,int sizeInactiveConsts,
			int nbAttackersIneligibleIDs_1, int nbAttackersWitnessForIneligible_2,
			int nbAttackersWitnessAgainstEligible_3,
			int nbIneligible){
		orgGID = global_organization_id;
		this.SizeofActiveHonestConstituents=sizeActiveHonestConsts;
		this.SizeofInactiveConstituents=sizeInactiveConsts;
		this.SizeofAttackerType1=nbAttackersIneligibleIDs_1;
		this.SizeofAttackerType2=nbAttackersWitnessForIneligible_2;
		this.SizeofAttackerType3=nbAttackersWitnessAgainstEligible_3;
		this.SizeofIneligible=nbIneligible;
	}
	public void run(){
		generate_Data(orgGID);
	}
	void generate_Data( String global_organization_id){
		SizeofNetwork=SizeofActiveHonestConstituents+
				SizeofInactiveConstituents+SizeofAttackerType1+SizeofAttackerType2+SizeofAttackerType3;
		GraphNeighborhood _gn[] = new GraphNeighborhood[NB_NEIGHBORHOODS];
		SK sk[][] = new SK[NB_NEIGHBORHOODS][];
		for(int n=0; n<NB_NEIGHBORHOODS; n++) {
			System.out.print("N"+n+":");
			_gn[n] = new GraphNeighborhood(SIZE_NEIGHBORHOOD, DENSITY_NEIGHBORHOOD);
			GraphNeighborhood gn = _gn[n];
			gn.createIneligible(this.SizeofIneligible);
			gn.createAttackers(2, this.SizeofAttackerType2);
			gn.createAttackers(3, this.SizeofAttackerType3);
			sk[n] = new SK[gn.IDs.length]; 
			D_Neighborhood d_n = null;
			for(int c = 0; c < gn.IDs.length; c++) {
				System.out.print("C"+c+",");
				String forename = "Constituent_"+c+" "+gn.type[c];
			   	Cipher keys = null;
		    	if (keys==null) {
		    		String now = Util.getGeneralizedTime();
		    		keys = Util.getKeyedGlobalID("Constituent", forename+" "+now);
		    		keys.genKey(256);
		    		sk[n][c] = keys.getSK();
		    	}
				String gcd = Util.getKeyedIDPK(keys);
				String sID = Util.getKeyedIDSK(keys);
				long p_oLID = D_Organization.getLIDbyGID(global_organization_id);
				D_Constituent _c = D_Constituent.getConstByGID_or_GIDH(gcd, null, true, true, true, null, p_oLID);
				_c.setSurname("Neighborhood_"+n);
				_c.setForename(forename);
				_c.setOrganizationGID(global_organization_id);
				_c.setCreationDate(Util.CalendargetInstance());
				_c.setGID(gcd, null, p_oLID); 
				if(_c.getGID() != null){
					_c.setGIDH(D_Constituent.getGIDHashFromGID(_c.getGID()));
				}else{
					Application_GUI.warning(("NULL Random Net???"), ("Null const"));
				}
				if(c==0) {
					d_n = D_Neighborhood.getEmpty();
					d_n.setOrgGID(global_organization_id);
					d_n.setBlocked(false);
					d_n.setCreationDate(Util.CalendargetInstance());
					d_n.setName("Neighborhood "+n);
					d_n.setBroadcasted(true);
					d_n.setSubmitter_GID(_c.getGID());
					d_n.submitter = _c;
					d_n.setGID(d_n.make_ID());
					d_n.sign(sk[n][c]);
					d_n.storeRemoteThis(sID, p_oLID, d_n.getCreationDateStr(), null, null, null);
				}
				if(d_n!=null)
					_c.setNeighborhoodGID(d_n.getGID());
				_c.setSK(sk[n][c]);
				_c.sign();
				gn.IDs[c] = _c.storeRequest_getID();
				_c.releaseReference();
			}
			for(int c = 0; c < gn.IDs.length; c++) {
				for(int w = 0; w < gn.neighbors[c].length; w++) {
					System.out.print("W"+w+",");
					int t = gn.neighbors[c][w];
					D_Witness _w = new D_Witness();
					_w.sense_y_n = getVote(_gn[n].type[c], _gn[n].e[t]); 
					_w.creation_date = Util.CalendargetInstance();
					_w.global_organization_ID = global_organization_id;
					_w.witnessed_constituentID = gn.IDs[t];
					_w.witnessing_constituentID = gn.IDs[c];
					_w.sign(sk[n][c]);
					_w.global_witness_ID = _w.make_ID();
					try {
						_w.storeVerified(false, _w.creation_date);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
				}	
			}
		}
		for(int n=0; n<RandomNetworkSimulation.NB_NEIGHBORHOODS; n++){
			System.out.print("n"+n+":");
			for(int c = 0; c<_gn[n].neighbors[c].length; c++){
				System.out.print("c"+c+",");
				for(int v = 0; v <= NB_EXTERNAL_NEIGHBORS; v++) {
					int k;
					while((k = r.nextInt(NB_EXTERNAL_NEIGHBORS)) == n);
					int t = r.nextInt(_gn[k].IDs.length);
					D_Witness _w = new D_Witness();
					_w.sense_y_n = getVote(_gn[n].type[c], _gn[k].e[t]); 
					_w.creation_date = Util.CalendargetInstance();
					_w.global_organization_ID = global_organization_id;
					_w.witnessed_constituentID = _gn[k].IDs[t];
					_w.witnessing_constituentID = _gn[n].IDs[c];
					_w.sign(sk[n][c]);
					_w.global_witness_ID = _w.make_ID();
					try {
						_w.storeVerified(false, _w.creation_date);
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	private int getVote(int type, int elig) {
		switch(type){
		case 0:
		case 1:
			return elig;
		case 2:
			return 0;
		case 3:
			return 1;
		}
		return 1;
	}
	public static void main(String[] args) {
		long org = 1;
		if(args.length>0) Long.parseLong(args[0]);
		D_Organization o=null;
		o = D_Organization.getOrgByLID_NoKeep(org, true);
	}
}

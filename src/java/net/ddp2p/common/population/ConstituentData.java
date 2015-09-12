package net.ddp2p.common.population;

import net.ddp2p.common.data.D_Constituent;

public class ConstituentData {
	private String global_constituentID;
	private long constituentID;
	public String given_name;
	public String surname;
	public int witness_for;
	public int witness_against;
	public Object icon;
	private String slogan;
	public boolean inserted_by_me;
	public int witnessed_by_me; // 1 for positive, -1 for negative, 2-added by myself
	public boolean external;
	public String email;
	public String submitter_ID;
	public boolean blocked;
	public boolean broadcast;
	public int witness_neuter;
	public int myself;
	public D_Constituent constituent;
	public String toString(){
		return "ConstituentData: "+surname+"," +given_name+": "+getSlogan()+"="+ witness_for+"/"+witness_against;
	}
	public String getC_GID() {
		return global_constituentID;
	}
	public void setC_GID(String global_constituentID) {
		this.global_constituentID = global_constituentID;
	}
	public long getC_LID() {
		return constituentID;
	}
	public void setC_LID(long constituentID) {
		this.constituentID = constituentID;
	}
	public String getSlogan() {
		return slogan;
	}
	public void setSlogan(String slogan) {
		this.slogan = slogan;
	}
}
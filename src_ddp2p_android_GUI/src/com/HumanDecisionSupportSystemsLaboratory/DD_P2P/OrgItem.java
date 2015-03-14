package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import net.ddp2p.common.data.D_Organization;

class OrgItem {
	private String org_name;
	D_Organization org;
	private byte[] icon;
	
	public String toString() {
		if (org == null) return getOrgName();
		return org.getOrgNameOrMy();
	}

	public byte[] getIcon() {
		if (icon != null) return icon;
		if (org != null) return org.getIcon();
		return null;
	}

	public void setIcon(byte[] icon) {
		this.icon = icon;
	}

	public String getOrgName() {
        boolean block = false;
        boolean served = true;
        if (org != null) {
            block = org.getBlocked();
            served = org.getBroadcasted();
        }
		if (org_name != null) return org_name+ (block?" (B)":"")+(!served?" (S)":"");
		if (org != null) {
            String org_name = org.getOrgNameOrMy();
            return ((org_name!=null)?org_name:"") + (block?" (b)":"")+(!served?" (s)":"");
        }
		return "Why no name!";
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}
}
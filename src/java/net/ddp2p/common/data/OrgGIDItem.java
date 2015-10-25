package net.ddp2p.common.data;

public class OrgGIDItem {
	public String gid;
	public String hash;
	public String name;
	public OrgGIDItem(String _gid, String _hash, String _name) {set(_gid, _hash, _name);}
	public void set(String _gid, String _hash, String _name){
		gid = _gid;
		hash = _hash;
		name = _name;	
	}
	public String toString() {
		if (name != null) return name;
		if (hash == null) return "ORG:"+super.toString();
		return "ORG:"+hash;
	}
}
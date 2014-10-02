package data;

public class MotionsGIDItem{
	public String gid;
	public String id;
	public String name;
	/**
	 *  create from gid, id , name
	 * @param _gid
	 * @param _id
	 * @param _name
	 */
	public MotionsGIDItem(String _gid, String _id, String _name) {set(_gid, _id, _name);}
	public void set(String _gid, String _id, String _name){
		gid = _gid;
		id = _id;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name+((id!=null)?(" #"+id):"");
		if(id == null) return "JUST:"+super.toString();
		return "JUST:"+id;
	}
	public String toStringDump() {
		return "JUST:"+name+" id="+id+" gid="+gid;
	}
}
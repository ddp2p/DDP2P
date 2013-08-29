package data;

import hds.TypedAddress;

import java.util.Comparator;

public class TypedAddressComparator implements Comparator<TypedAddress> {

	@Override
	public int compare(TypedAddress a, TypedAddress b) {
		if( a.certified && !b.certified) return -1;
		if(!a.certified &&  b.certified) return +1;
		if(a.priority < b.priority) return -1;
		if(a.priority > b.priority) return +1;

		int val;
		if(a.type==null){
			if(b.type!=null) return -1;
		}else{
			if(b.type==null) return +1;
			val = a.type.trim().compareTo(b.type.trim());
			if(val!=0) return val;
		}
		val = a.address.trim().compareTo(b.address.trim());
		if(val!=0) return val;
		return val;
	}
	
}

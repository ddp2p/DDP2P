package net.ddp2p.common.population;


public class ConstituentsNode {
	private ConstituentsBranch parent;
	public Object[] getPath(){
		if(getParent() == null) return new Object[]{this};
		Object[] presult = getParent().getPath();
		Object[] result = new Object[presult.length+1];
		for(int k=0; k<presult.length; k++) result[k] = presult[k];
		result[presult.length] = this;
		return result;
	}
	public ConstituentsNode(ConstituentsBranch _parent){
		setParent(_parent);
	}
	public ConstituentsBranch getParent() {
		return parent;
	}
	public void setParent(ConstituentsBranch parent) {
		this.parent = parent;
	}
}
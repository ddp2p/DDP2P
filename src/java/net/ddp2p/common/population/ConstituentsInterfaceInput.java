package net.ddp2p.common.population;

public interface ConstituentsInterfaceInput {
	public void updateCensus(Object source, Object[] path);
	public void updateCensusStructure(Object source, Object[] path);

	public ConstituentsAddressNode getRoot();

	public void updateCensus(Object source,
			Object[] path2parent, int idx);

	public long getConstituentIDMyself();
	public String getSubDivisions();
	public long getOrganizationID();
	public long[] getFieldIDs();
	public void updateCensusInserted(
			Object source, Object[] path2parent,
			int[] idx, Object[] children);
}
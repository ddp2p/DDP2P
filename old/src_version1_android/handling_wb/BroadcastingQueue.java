package handling_wb;

import ASN1.ASN1DecoderFail;

import util.P2PDDSQLException;

public interface BroadcastingQueue{
	public byte[] getNext(long msg_c);
	public void loadAll();
}
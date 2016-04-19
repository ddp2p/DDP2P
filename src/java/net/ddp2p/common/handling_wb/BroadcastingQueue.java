package net.ddp2p.common.handling_wb;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.common.util.P2PDDSQLException;
public interface BroadcastingQueue{
	public byte[] getNext(long msg_c);
	public void loadAll();
}

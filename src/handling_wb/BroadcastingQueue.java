package handling_wb;

import ASN1.ASN1DecoderFail;

import com.almworks.sqlite4java.SQLiteException;

public interface BroadcastingQueue{
	public byte[] getNext(long msg_c);
	public void loadAll();
}
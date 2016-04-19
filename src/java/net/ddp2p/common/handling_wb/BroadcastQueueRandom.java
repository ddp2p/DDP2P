package net.ddp2p.common.handling_wb;
import java.util.ArrayList;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.P2PDDSQLException;
public class BroadcastQueueRandom extends BroadcastQueue {
	@Override
	public void loadAll() {
	}
	public void registerRequest(RequestData rq) {
	}
	@Override
	long loadVotes(ArrayList<PreparedMessage> m_PreparedMessagesVotes2,
			long m_lastVote2) {
		return 0;
	}
	@Override
	long loadConstituents(ArrayList<PreparedMessage> m_PreparedMessagesConstituents2,
			long m_lastConstituent2) {
		return 0;
	}
	@Override
	long loadPeers(ArrayList<PreparedMessage> m_PreparedMessagesPeers2,
			long m_lastPeer2) {
		return 0;
	}
	@Override
	long loadOrgs(ArrayList<PreparedMessage> m_PreparedMessagesOrgs2, long m_lastOrg2)
			throws P2PDDSQLException {
		return 0;
	}
	@Override
	long loadWitnesses(ArrayList<PreparedMessage> m_PreparedMessagesWitnesses2,
			long m_lastWitness2) {
		return 0;
	}
	@Override
	long loadNeighborhoods(ArrayList<PreparedMessage> m_PreparedMessagesNeighborhoods2,
			long m_lastNeighborhoods2) {
		return 0;
	}
	@Override
	public byte[] getNext(long msg_c) {
		return null;
	}
}

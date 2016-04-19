package net.ddp2p.common.network.stun.keepalive;
import java.util.Comparator;
public class KeepAliveDataComparator implements Comparator<KeepAliveData>
{
	@Override
	public int compare( KeepAliveData one, KeepAliveData two )
    {
		long difference = one.getNextSendTime() - two.getNextSendTime();
		return (int)difference;
	}
	public boolean equals( KeepAliveData one, KeepAliveData two )
    {
		long difference = one.getNextSendTime() - two.getNextSendTime();
		if( difference == 0 )
        {
			return true;
		}
		return false;
	}
}

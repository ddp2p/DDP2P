package net.ddp2p.common.data;
import java.util.Comparator;
import net.ddp2p.common.hds.Address;
public class TypedAddressComparator implements Comparator<Address> {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	@Override
	public int compare(Address a, Address b) {
		int v = _compare(a, b);
		return v;
	}
	public int _compare(Address a, Address b) {
		if ( a.certified && !b.certified) return -1;
		if (!a.certified &&  b.certified) return +1;
		if (a.priority < b.priority) return -1;
		if (a.priority > b.priority) return +1;
		int val = 0;
		if (a.pure_protocol == null) {
			if (b.pure_protocol != null) return -1;
		} else {
			if (b.pure_protocol == null) return +1;
			val = a.pure_protocol.trim().compareTo(b.pure_protocol.trim());
			if (val != 0) return val;
		}
		val = a.domain.trim().compareTo(b.domain.trim());
		if (val != 0) return val;
		if (a.tcp_port != b.tcp_port) return (a.tcp_port - b.tcp_port);
		if (a.udp_port != b.udp_port) return (a.tcp_port - b.tcp_port);
		return val;
	}
}

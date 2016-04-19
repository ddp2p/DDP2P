package net.ddp2p.common.config;
import net.ddp2p.common.data.D_Constituent;
public interface ConstituentListener {
	void constituentUpdate(D_Constituent c, boolean me, boolean selected);
}

package net.ddp2p.common.config;
import java.util.Hashtable;
import net.ddp2p.common.hds.DirectoryAnswerMultipleIdentities;
public interface DirectoriesData_View {
	void setData(
			Hashtable<String, Hashtable<String, DirectoryAnswerMultipleIdentities>> dir_data);
}

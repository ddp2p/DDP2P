package net.ddp2p.common.config;

import java.io.BufferedReader;
import java.io.File;

import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;

public interface Vendor_DB_Email {
	public DB_Implementation get_DB_Implementation();
	public String[] extractDDL(File database_file, int JDBC_SQLITE4JAVA);
	public boolean db_copyData(File database_old, File database_new,
			BufferedReader dDL, String[] _DDL, boolean _SQLITE4JAVA);
	public void sendEmail(DD_IdentityVerification_Answer answer);
}
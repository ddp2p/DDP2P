package net.ddp2p.java.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.updates.VersionInfo;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.java.WSupdate.HandleService;

public class Vendor_JDBC_EMAIL_DB implements net.ddp2p.common.config.Vendor_DB_Email {

	@Override
	public DB_Implementation get_DB_Implementation() {
		return new DB_Implementation_JDBC_SQLite();
	}

	@Override
	public String[] extractDDL(File database_file, int i) {
		if (i == 0) // SQLITE4JAVA
			return DBAlter_Implementation_Sqlite4Java.__extractDDL(database_file);
		else
			return DBAlter_Implementation_JDBC.__extractDDL(database_file);
	}

	@Override
	public boolean db_copyData(File database_old, File database_new,
			BufferedReader DDL, String[] _DDL, boolean _SQLITE4JAVA) {
		if(_SQLITE4JAVA){
			try{
				boolean result = DBAlter_Implementation_Sqlite4Java._copyData(database_old, database_new, DDL, _DDL);
				if (result) return result;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			boolean result = DBAlter_Implementation_JDBC._copyData(database_old, database_new, DDL, _DDL);
			if (result) return result;
		}catch(Exception e){
			e.printStackTrace();
			if(!_SQLITE4JAVA)
				try {
					return DBAlter_Implementation_Sqlite4Java._copyData(database_old, database_new, DDL, _DDL);
				} catch (IOException | P2PDDSQLException e1) {
					e1.printStackTrace();
					return false;
				}
		}
		return false;
	}

	@Override
	public void sendEmail(DD_IdentityVerification_Answer answer) {
		net.ddp2p.java.email.EmailManager.sendEmail(answer);
	}

	public static void initJDBCEmail() {
		Application_GUI.dbmail = new Vendor_JDBC_EMAIL_DB();
	}

	@Override
	public URL isWSVersionInfoService(String url_str) {
		return HandleService.isWSVersionInfoService(url_str);
	}

	@Override
	public VersionInfo getWSVersionInfo(URL _url, String myPeerGID,
			SK myPeerSK, Hashtable<Object, Object> ctx) {
		return HandleService.getWSVersionInfo(_url, myPeerGID, myPeerSK, ctx);
	}
}
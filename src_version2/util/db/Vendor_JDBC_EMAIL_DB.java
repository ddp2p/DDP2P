package util.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import config.Application_GUI;

import util.DB_Implementation;
import util.DD_IdentityVerification_Answer;
import util.P2PDDSQLException;

public class Vendor_JDBC_EMAIL_DB implements config.Vendor_DB_Email {

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
		util.email.EmailManager.sendEmail(answer);
	}

	public static void initJDBCEmail() {
		Application_GUI.dbmail = new Vendor_JDBC_EMAIL_DB();
	}
}
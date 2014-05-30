package simulator;



import util.P2PDDSQLException;
import config.Application;
import util.DBInterface;
import ASN1.ASN1DecoderFail;

public class DB_zap {
	
	@SuppressWarnings("static-access")
	public static void main (String[] args) throws ASN1DecoderFail, InterruptedException, P2PDDSQLException{
		Application.db = new DBInterface("deliberation-app.db");
		Fill_database f = new Fill_database();
		f.cleanDatabase();
		System.out.println("db_deleted");
	}

}

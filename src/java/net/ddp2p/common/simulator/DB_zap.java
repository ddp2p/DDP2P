package net.ddp2p.common.simulator;



import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;

public class DB_zap {
	
	@SuppressWarnings("static-access")
	public static void main (String[] args) throws ASN1DecoderFail, InterruptedException, P2PDDSQLException{
		Application.db = new DBInterface("deliberation-app.db");
		Fill_database f = new Fill_database();
		f.cleanDatabase();
		System.out.println("db_deleted");
	}

}

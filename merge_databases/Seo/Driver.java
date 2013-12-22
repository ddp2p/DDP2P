/*
 * Author     : Srini Venkatesh, Yonglok Seo
 * Subject    : CSE 5260
 * Instructor : Dr. M. Silaghi
 * */
import java.util.ArrayList;

/*
 * Class: Driver
 * 
 * Implements the control flow to generate database migration.
 * 
 */
public class Driver {
	public static void main(String[] args) {
		Process DBproc = new Process();
		System.out.println("Refreshing DB Start");
		DBproc.recreateDB();
		DBproc.createDB();
		DBproc.refreshDB();
		System.out.println("Refeshing DB Finished");
		System.out.println("Migrating DB Start");
		DBproc.getTableName();
		System.out.println("Migrating DB Finished");
		System.out.println("Reading Config_File Start");
		final configReader cr = new configReader("config_file.txt");
		final ArrayList<Config> clist = cr.read();

		for (int i = 0; i < clist.size(); ++i) {
			clist.get(i).print();
		}
		System.out.println("Reading Config_File Finished");
		System.out.println("Phase 1 Start : Deleting Rows in OLD DB Including constraints");
		for (int i = 0; i < clist.size(); ++i) {
			DBproc.phase1(clist.get(i));
		}
		System.out.println("Phase 1 Finisheded");
		System.out.println("Phase 2 Start : Inserting Rows");
		for (int i = 0; i < clist.size(); ++i) {
			DBproc.phase2(clist.get(i));
		}
		System.out.println("Phase 2 Finished");
		System.out.println("Closing Connection to DB's");
		DBproc.closeConnecetions();
		System.out.println("Closing Connection Successful");
		System.out.println("DB Merging Finished");
	}
}

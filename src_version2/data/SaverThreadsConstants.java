package data;

import hds.DirectoryServerCache;

public class SaverThreadsConstants {
	/** If more than this number of threads is detected running while saving, then delay it puting thread to sleep */
	public static final int MAX_NUMBER_CONCURRENT_SAVING_THREADS = 2;
	/** If a saving error occurs in the database, should it retry? How many times? At least 1 first attempt is needed! */
	public static final int ATTEMPTS_ON_ERROR = 1; // 3
	/** To print a debug message at each saving activity by the cache threads (only D_Motion uses it now) */
	public static final boolean DEBUG_SAVING_ACTIVITY = false;

	/**
	 * Default sleep time
	 */
	public static final long SAVER_SLEEP_BETWEEN_OBJECTS_MSEC = 1;
	public static final long SAVER_SLEEP_WAITING_OBJECTS_MSEC = 2000;
	public static final long SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC = 200;
	/**
	 * The next delays are the delays between the 3 attempts a worker thread is making to save an object
	 */
	public static final long SAVER_SLEEP_WORKER_PEERS_ON_ERROR_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_ORGANIZATION_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_CONSTITUENT_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_NEIGHBORHOOD_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_MOTION_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_JUSTIFICATION_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	public static final long SAVER_SLEEP_WORKER_RECOMMENDATION_TESTER_ON_ERROR = SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC;
	
	/**
	 * The next delays are about blocking the worker thread after saving one object. -1 is used to disable it.
	 * Probably this is of no use.
	 */
	public static final long SAVER_SLEEP_WORKER_BETWEEN_PEERS_MSEC = -1;
	public static final long SAVER_SLEEP_WORKER_BETWEEN_ORGANIZATION_MSEC = -1;
	public static final long SAVER_SLEEP_WORKER_BETWEEN_CONSTITUENT_MSEC = -1;
	public static final long SAVER_SLEEP_WORKER_BETWEEN_NEIGHBORHOOD_MSEC = -1;
	public static final long SAVER_SLEEP_WORKER_BETWEEN_MOTION_MSEC = -1; // for this -1 was implemented as no wait (not implemented the same for others!)
	public static final long SAVER_SLEEP_WORKER_BETWEEN_JUSTIFICATION_MSEC = -1;
	public static final long SAVER_SLEEP_WORKER_RECOMMENDATION_TESTER_MSEC = -1;
	
	/**
	 * Next are the delays between starting new threads on new objects that have to be saved
	 */
	// The next are delays before starting a new server thread (each such thread saving one object, with three attempts on error)
	public static final long SAVER_SLEEP_BETWEEN_PEER_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC;
	public static final long SAVER_SLEEP_BETWEEN_ORGANIZATION_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BEETWEEN_CONSTITUENT_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BETWEEN_NEIGHBORHOOD_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BETWEEN_MOTION_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BETWEEN_JUSTIFICATION_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BETWEEN_RECOMMENDATION_TESTER_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_BETWEEN_DIRS_MSEC = SAVER_SLEEP_BETWEEN_OBJECTS_MSEC;

	public static final long SAVER_SLEEP_WAITING_PEER_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC;
	public static final long SAVER_SLEEP_WAITING_ORGANIZATION_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_CONSTITUENT_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_NEIGHBORHOOD_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_MOTION_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_JUSTIFICATION_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_RECOMMENDATION_TESTER_MSEC = SAVER_SLEEP_WAITING_OBJECTS_MSEC; // ms to sleep
	public static final long SAVER_SLEEP_WAITING_DIRS_MSEC = hds.DirectoryServerCache.D_Directory_Storage.SaverThread.SAVER_SLEEP;// SAVER_SLEEP_WAITING_OBJECTS_MSEC;

	/**
	 * 
	 */
	///** The intention was to potentially put all threads to eternal sleep on this monitor until they are notified */
	//public static final Object saver_threads_unique_monitor = new Object(); // to be used as a unique monitor
	/** Incremented and decremented when a thread starts or stops. */
	public static int threads_peers = 0, threads_orgs = 0, threads_cons = 0, threads_neig = 0, threads_moti = 0, threads_just = 0, threads_dirs = 0, threads_test = 0;
	/** Sound practice is to lock this while changing the thread counts! */
	public static final Object monitor_threads_counts = new Object();

	//public static int items_peers = 0, items_orgs = 0, items_cons = 0, items_neig = 0, items_moti = 0, items_just = 0, items_dirs = 0, items_test = 0; 

	/**
	 * Sum of counters. The directory savers do not generate threads, so its count will be always 0.
	 * @return
	 */
	public static int getNumberRunningSaverThreads() {
		return threads_peers + threads_orgs + threads_cons + threads_neig + threads_moti + threads_just + threads_dirs + threads_test; 
	}
	/**
	 * Computed on the fly from HashMaps
	 * @return
	 */
	public static int getNumberRunningSaverWaitingItems() {
		return 0 + D_Peer.getNumberItemsNeedSaving() 
				+ D_Organization.getNumberItemsNeedSaving()//; //_need_saving_obj.size();
				+ D_Constituent.getNumberItemsNeedSaving()
				+ D_Neighborhood.getNumberItemsNeedSaving()
				+ D_Motion.getNumberItemsNeedSaving()
				+ D_Justification.getNumberItemsNeedSaving()
				+ DirectoryServerCache.getNumberItemsNeedSaving()
				+ D_RecommendationOfTester.getNumberItemsNeedSaving()
				;
	}
	/**
	 * The following are constants defining the size of the cache of objects.
	 * It is defined both in number of objects (min and max), and in the maximum RAM size in bytes.
	 * The number of bytes of an item is computed as the size of its ASN encoding which is stored
	 * as a cache for efficient sending. 
	 * 
	 * In fact one has to verify whether this cache is really used
	 * to avoid keeping it redundantly. Further, storing it leads to practically duplicating the size 
	 * of RAM used. So the values specified here tells the size of the ASN1 cached objects, being less than
	 * half of the total RAM used by the cache.
	 */
	public static int MAX_LOADED_PEERS = 10000;
	public static long MAX_PEERS_RAM = 10000000; 
	static final int MIN_LOADED_PEERS = 2;
	
	public static int MAX_LOADED_ORGS = 10000;
	public static long MAX_ORGS_RAM = 10000000;
	static final int MIN_LOADED_ORGS = 2;
	
	public static int MAX_LOADED_CONSTS = 10000;
	public static long MAX_CONSTS_RAM = 10000000;
	static final int MIN_LOADED_CONSTS = 2;
	
	public static int MAX_LOADED_NEIGHS = 10000;
	public static long MAX_NEIGHS_RAM = 10000000;
	static final int MIN_LOADED_NEIGHS = 2;
	
	public static int MAX_LOADED_MOTIONS = 10000;
	public static long MAX_MOTIONS_RAM = 10000000;
	static final int MIN_LOADED_MOTIONS = 2;
	
	public static int MAX_LOADED_JUSTIFICATIONS = 10000;
	public static long MAX_JUSTIFICATIONS_RAM = 10000000;
	static final int MIN_LOADED_JUSTIFICATIONS = 2;
	
	public static int MAX_LOADED_RECOMMENDATIONS_OT = 10000;
	public static long MAX_RECOMMENDATIONS_OT_RAM = 10000000;
	static final int MIN_LOADED_RECOMMENDATIONS_OT = 2;
}
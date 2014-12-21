package data;

public class SaverThreadsConstants {
	/**
	 * Default sleep time
	 */
	public static final long SAVER_SLEEP_BETWEEN_OBJECTS_MSEC = 3000;
	public static final long SAVER_SLEEP_BETWEEN_OBJECTS_ON_ERROR_MSEC = 2000;
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
	
}
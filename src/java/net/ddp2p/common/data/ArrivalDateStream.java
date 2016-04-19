package net.ddp2p.common.data;
import java.util.Calendar;
import net.ddp2p.common.util.Util;
public class ArrivalDateStream {
	static Object monitor = new Object();
	static Calendar lastDate;
	/**
	 * Makes sure that no two objects are stores with the same arrival date (but to avoid setting arrival dates in the future, we wait)
	 * We could implement it with arrival dates in the past...
	 * @return
	 */
	static Calendar getNextArrivalDate() {
		synchronized (monitor) {
			Calendar result = Util.CalendargetInstance();
			if (lastDate != null && lastDate.equals(result)) {
				result.setTimeInMillis(result.getTimeInMillis() + 1);
				try {
					monitor.wait(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return lastDate = result;
		}
	}
}

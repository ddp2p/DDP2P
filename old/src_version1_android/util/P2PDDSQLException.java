package util;

/*
 select fv.name, fv.forename, fv.constituentID, fv.external, fv.global_constituentID  from constituents AS fv JOIN fields_values AS f ON f.constituentID = fv.constituentID   JOIN fields_values AS fv0 ON fv.constituentID=fv0.constituentID  JOIN fields_values AS fv1 ON fv.constituentID=fv1.constituentID  JOIN fields_values AS fv2 ON fv.constituentID=fv2.constituentID  JOIN fields_values AS fv3 ON fv.constituentID=fv3.constituentID  JOIN fields_values AS fv4 ON fv.constituentID=fv4.constituentID  JOIN fields_values AS fv5 ON fv.constituentID=fv5.constituentID  WHERE fv.organizationID = 100 AND  (  f.value = 'Lunii' AND f.fieldID = 17  AND fv0.value = 'EU' AND fv0.fieldID = 11  AND fv1.value = 'RO' AND fv1.fieldID = 12  AND fv2.value = 'Transylvania' AND fv2.fieldID = 14  AND fv3.value = 'Cluj' AND fv3.fieldID = 14  AND fv4.value = 'Cluj-Napoca' AND fv4.fieldID = 15  AND fv5.value = 'Zorilor' AND fv5.fieldID = 16  ) OR fv.neighborhoodID = 29  GROUP BY fv.constituentID;
 */
@SuppressWarnings("serial")
public class P2PDDSQLException extends Exception{
	Object _e;
	String msg;
	public P2PDDSQLException(String _msg) {
		super();
		msg = _msg;
	}
	public P2PDDSQLException(Exception e) {
		super();
		msg = e.getLocalizedMessage();
		_e = e;
	}
	public String toString(){
		return (msg==null)?Util.getString(_e):msg;
	}
}
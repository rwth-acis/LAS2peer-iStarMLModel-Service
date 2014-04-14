package i5.las2peer.services.iStarMLModelService.data;

import java.util.Date;

/**
 * Stores information about version retrieved from the DB
 * @author Alexander
 *
 */

public class ResourceVersion {
	/**
	 * Initialization with values
	 * @param rev revision number
	 * @param date date of creation
	 * @param user user which committed the revision
	 */
	public ResourceVersion(int rev, Date date, String user)
	{
		_revision=rev;
		_date=date;
		_user=user;
	}	
	public int getRevision() {
		return _revision;
	}
	
	public Date getDate() {
		return _date;
	}
	
	public String getUser() {
		return _user;
	}
	
	private int _revision;
	private Date _date;
	private String _user;
	
	public String toString()
	{
		return "Revision: "+_revision+" Date: "+_date+" User: "+_user;
	}

}

package i5.las2peer.services.iStarMLModelService.data;

import java.util.Date;
/**
 * Stores name and date of a resource (used as a return value)
 * @author Alexander
 *
 */
public class ResourceData {

	private String _name;
	private Date _lastModified=null;
	
	public String getName()
	{
		return _name;
	}
	public Date getLastModified()
	{
		return _lastModified;
	}
	
	public ResourceData(String name, Date lastModified)
	{
		_name=name;
		_lastModified=lastModified;
	}
}

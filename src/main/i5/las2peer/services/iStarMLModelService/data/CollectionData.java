package i5.las2peer.services.iStarMLModelService.data;
/**
 * Stores name and owner of a collection (simple return type)
 * @author Alexander
 *
 */
public class CollectionData {

	private String _name="";
	private String _owner="";
	
	public String getName()
	{
		return _name;
	}
	public String getOwner()
	{
		return _owner;
	}
	public CollectionData(String name, String owner)
	{
		_name=name;
		_owner=owner;
	}

}

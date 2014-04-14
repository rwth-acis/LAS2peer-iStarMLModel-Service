package i5.las2peer.services.iStarMLModelService.data;

import java.io.Serializable;

/**
 * Stores write operations onto a resource before the changes are send to the database
 * @author Alexander
 *
 */
public class ModelBuffer implements Serializable{

	private static final long serialVersionUID = -4319617250114028256L;
	
	public ModelBuffer(String content, String collection, String resource)
	{
		_content=content;
		_resource=resource;
		_collection=collection;
	}
	private String _content;
	private String _resource;
	private String _collection;
	public String getContent()
	{
		return _content;
	}	
	public String getResource()
	{
		return _resource;
	}
	public String getCollection()
	{
		return _collection;
	}
	public void setCollection(String collection) {
		_collection=collection;
		
	}
	public void setResource(String resource) {
		_resource=resource;
		
	}
	public void setContent(String content) {
		_content=content;
		
	}
}

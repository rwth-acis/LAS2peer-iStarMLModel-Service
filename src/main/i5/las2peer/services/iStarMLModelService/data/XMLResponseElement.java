package i5.las2peer.services.iStarMLModelService.data;


import java.util.ArrayList;
/**
 * stores a generalized XML node, which can have children
 * used to generate a XML as a service response
 * @author Alexander
 *
 */
public class XMLResponseElement 
{
	
	
	private String _name;
	private ArrayList<XMLResponseElementChild> _children= new ArrayList<>();
	
	public XMLResponseElement(String name)
	{
		_name=name;		
	}
	
	public String getName()
	{
		return _name;
	}
	
	public ArrayList<XMLResponseElementChild> getChildren()
	{
		return _children;
	}
	
	public void addChild(XMLResponseElementChild child)
	{
		_children.add(child);
	}
	
	public String toString()
	{
		return getName();
	}
	
}

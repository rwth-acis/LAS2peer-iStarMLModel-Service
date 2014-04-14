package i5.las2peer.services.iStarMLModelService.data;


import java.util.ArrayList;
/**
 * Used to store individual elements with attribute values
 * inside an XML see {@link XMLResponseElement}
 * @author Alexander
 *
 */
public class XMLResponseElementChild
{
	private String _name;
	private ArrayList<StringTuple> _attributes=new ArrayList<>();
	
	public XMLResponseElementChild(String name)
	{
		_name=name;
	}
	
	public ArrayList<StringTuple> getAttributes()
	{
		return _attributes;
	}
	public void addAttribute(StringTuple attr)
	{
		_attributes.add(attr);
	}
	public void addAttribute(String attr, String val)
	{
		addAttribute(new StringTuple(attr, val));
	}
	public String getName()
	{
		return _name;
	}
	
	public String toString()
	{
		return getName();
	}
}
package i5.las2peer.services.iStarMLModelService.data;

/**
 * Stores a String pair
 * @author Alexander
 *
 */
public class StringTuple {
	
	public StringTuple(String s1, String s2)
	{
		_s1=s1;
		_s2=s2;
	}
	public String getS1() {
		return _s1;
	}
	public String getS2() {
		return _s2;
	}
	public void setS1(String s1) {
		_s1=s1;
	}
	public void setS2(String s2) {
		_s2=s2;
	}
	
	private String _s1;
	private String _s2;
	
	public String toString()
	{
		return _s1+","+_s2;
	}
	
	@Override
	public int hashCode() {		
		return toString().hashCode();
	}
	
	
	@Override
	public boolean equals(Object arg0) {
		
		return toString().equals(arg0.toString());
	}

}


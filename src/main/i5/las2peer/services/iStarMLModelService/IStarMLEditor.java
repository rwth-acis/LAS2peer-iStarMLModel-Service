package i5.las2peer.services.iStarMLModelService;


import i5.las2peer.services.iStarMLModelService.data.StringTuple;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;


import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Modifies IStarML files
 * @author Alexander
 *
 */
public class IStarMLEditor {

	private static final String COMMENTTAG = "comment";
	private static final String NAMETAG = "name";
	private static final String IDTAG = "id";
	private static final String DEPENDEETAG = "dependee";
	private static final String DEPENDERTAG = "depender";
	private static final String DEPENDENCYTAG = "dependency";
	private static final String IELEMENTTAG = "ielement";
	private static final String ACTORTAG = "actor";
	private static final String AREFTAG = "aref";
	private static final String TYPETAG = "type";
	private static final String ISTARMLTAG = "istarml";
	private static final String ACTORLINKTAG = "actorLink";
	
	
	private Document _doc;
    private DocumentBuilder _dBuilder;
	private static final String DIAGRAMTAG="diagram";
	private Node _diagramNode=null;
	private HashSet<String> _idHash= new HashSet<>();
	private HashSet<StringTuple> _edgeHash=new HashSet<>();
	private XPath _xPath =  XPathFactory.newInstance().newXPath();
    private Validator _validator;
	
	/**
	 * Initialization
	 * @throws Exception 
	 */
	public IStarMLEditor() throws Exception
	{
        DocumentBuilderFactory _dbFactory = DocumentBuilderFactory.newInstance();
		_dBuilder = _dbFactory.newDocumentBuilder();
        SchemaFactory _factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema _schema = _factory.newSchema(new StreamSource(new StringReader(getFile("./xsd/check.xsd"))));
		_validator = _schema.newValidator();
		
		
	}
	/**
	 * returns a free id in the current document (better specify ids manually)
	 * @return
	 * @throws Exception
	 */
	public String generateSomeFreeID() throws Exception
	{
		int rand=(int)(Math.random() * (1000000+1));
		String val;
		int abortCounter=200;
		do
		{
			val=Integer.toString(rand);
		}while(_idHash.contains(val)&&abortCounter>0);
		
		if(abortCounter<=0)
			throw new Exception("Free ID for new node could not be found. Please provide an ID manually.");
		
		return val;
		
	}
	/**
	 * Checks if given type is a valid type for an actor element
	 * @param type
	 * @return
	 */
	private boolean isAllowedActorType(String type)
	{
        return type.equals("actor") || type.equals("role")
                || type.equals("agent") || type.equals("position");

    }
	/**
	 * Checks if a given type is a valid type for an ielement
	 * @param type
	 * @return
	 */
	private boolean isAllowedIElementType(String type)
	{
        return type.equals("resource") || type.equals("task")
                || type.equals("goal") || type.equals("softgoal") || type.equals("belief");

    }
	/**
	 * Checks if a given type is a valid type for an actorLink
	 * @param type
	 * @return
	 */
	private boolean isAllowedActorLinkType(String type)
	{
        return type.equals("is_part_of") || type.equals("is_a")
                || type.equals("instance_of") || type.equals("plays")
                || type.equals("covers") || type.equals("occupies");

    }
	/**
	 * Reads a local file from a given path
	 * @param path
	 * @return file contents
	 * @throws Exception
	 */
	private String getFile(String path)throws Exception 
	{		   
		File file = new File(path); 		  
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int)file.length()];
		fis.read(data);
		fis.close();

        return new String(data, "UTF-8");
	}
	/**
	 * Creates a new empty XML DOM Tree
	 * @throws Exception
	 */
	public void createXML() throws Exception
	{
		_idHash.clear();
		_doc=_dBuilder.newDocument();
		Element root=_doc.createElement(ISTARMLTAG);
		Element diagram=_doc.createElement(DIAGRAMTAG);
		diagram.setAttribute(NAMETAG, "diagram");
		_doc.appendChild(root);
		root.appendChild(diagram);
		_diagramNode=diagram;
	}
	/**
	 * Loads an existing XML
	 * @param xml as string
	
	 * @return returns an empty string or an error message, if the validation of the XML fails
	 * @throws Exception
	 */
	public String loadXML(String xml) throws Exception
	{		
		return loadXML(xml,true);
	}
	/**
	 * Loads an existing XML
	 * @param xml as string
	 * @param validate whether to validate the xml
	 * @return returns an empty string or an error message, if the validation of the XML fails
	 * @throws Exception
	 */
	public String loadXML(String xml, boolean validate) throws Exception
	{				
		StringBuilder sb=new StringBuilder();
		if(validate)
		{
			if(!validateIStarML(xml, sb))//validation check
			{
				return ("Error: XML not supported iStarML: "+ sb.toString());
			}
		}
		_idHash.clear();
		_doc= _dBuilder.parse(new InputSource(new StringReader(xml)));
		_doc.getDocumentElement().normalize();
		
		
		//if previous whitespaces do not get removed before processing, later output might have wrong indentation!
		NodeList emptyTextNodes = (NodeList)_xPath.compile("//text()[normalize-space(.) = '']").evaluate(_doc, XPathConstants.NODESET);		
		for (int i = 0; i < emptyTextNodes.getLength(); i++) {
		    Node emptyTextNode = emptyTextNodes.item(i);
		    emptyTextNode.getParentNode().removeChild(emptyTextNode);
		}
		
		
	    
		NodeList nList= _doc.getElementsByTagName(DIAGRAMTAG);
		if(nList.getLength()!=1)
		{
			throw new Exception("Invalid amount of diagram nodes: "+nList.getLength());
		}
		else
		{
			_diagramNode=nList.item(0);
			//check for duplicate ids
			NodeList nodeList =(NodeList) _xPath.compile("//*[@"+IDTAG+"]").evaluate(_doc, XPathConstants.NODESET);
			for(int i=0; i<nodeList.getLength();i++)
			{
				Element el =(Element)nodeList.item(i);
				String id=el.getAttribute(IDTAG);
				if(!_idHash.add(id))//check XML for duplicate ids
				{
					return("Error: XML contains duplicate id: "+id);
				}
			}
			
			//init hashsets for id/aref checks
			NodeList actorLinkList =(NodeList) _xPath.compile("//"+ACTORLINKTAG).evaluate(_doc, XPathConstants.NODESET);
			NodeList dependerkList =(NodeList) _xPath.compile("//"+DEPENDERTAG).evaluate(_doc, XPathConstants.NODESET);
			NodeList dependeeList =(NodeList) _xPath.compile("//"+DEPENDEETAG).evaluate(_doc, XPathConstants.NODESET);
			
			
			updateEdgeHashSet("",actorLinkList,1);
			updateEdgeHashSet(DEPENDERTAG,dependerkList,2);
			updateEdgeHashSet(DEPENDEETAG,dependeeList,2);
		}
		return "";
	}
	/**
	 * Fills the hash set for checking on duplicate edges
	 * @param nl node list
	 * @param parentLevel how many levels above the current node is the parent node with ID information?
	 * @throws Exception
	 */
	private void updateEdgeHashSet(String type, NodeList nl,int parentLevel) throws Exception
	{
		for (int i = 0; i < nl.getLength(); i++) 
		{
			Element el =(Element)nl.item(i);
			String aref=el.getAttribute(AREFTAG);
			if(aref.isEmpty())
				throw new Exception("Element "+el.getTagName()+"has no aref attribute");
			
			int parentLevelCount=parentLevel;
			Element parent=(Element) el.getParentNode();
			while(--parentLevelCount>0)
			{
				 parent=(Element) parent.getParentNode();
			}
			
			String id=parent.getAttribute(IDTAG);
			if(id.isEmpty())
				throw new Exception("Parent "+parent.getTagName()+" has no id");
			
			if(!type.trim().isEmpty())
				aref=type+aref;
			StringTuple st = new StringTuple(id, aref);
			if(!_edgeHash.add(st))//check for duplicate edges
			{
				throw new Exception("XML contains duplicate edge: "+st.toString());
			}
				
		}
	}
	
	/**
	 * Adds a new Node element (like actor or ielement)
	 * @param tagName xml tag in the document
	 * @param id unique id	
	 * @param name name of the element
	 * @param type type of the element
	 * @param comment optional comment
	 * @throws Exception
	 */
	private void createNode(String tagName, String id, String name, String type, String comment) throws Exception
	{
		 if(_diagramNode==null)
			 throw new Exception("No diagram node.");
		 
		 if(!_idHash.add(id))//if id already in list
		 {
			 /*System.out.println("Element id "+id+" already existing.");
			 throw new Exception("Element id "+id+" already existing.");*/
			 
			 //instead edit:
			 if(tagName.equals(ACTORTAG))
			 {
				 if(!((Element)getNodeByID(id)).getTagName().equals(ACTORTAG))
					 throw new Exception("Element id "+id+" already existing and is not an actor.");
					 
				 if(!comment.isEmpty())//if comment empty: do not overwrite comment
					 editActor(id, name, type, comment);
				 else
					 editActor(id, name, type);
			 }
			 else//ielement
			 {
				 if(!((Element)getNodeByID(id)).getTagName().equals(IELEMENTTAG))
					 throw new Exception("Element id "+id+" already existing and is not an ielement.");
				 if(!comment.isEmpty())
					 editIElement(id, name, type, comment);		
				 else
					 editActor(id, name, type);
			 }
			 return;
			 
		 }
		 Element elem = _doc.createElement(tagName);
		 elem.setAttribute(IDTAG, id);//explicit, since each actor MUST have an id
		 elem.setAttribute(NAMETAG, name);
		 elem.setAttribute(TYPETAG, type);
		 if(!comment.isEmpty())
		 	elem.setAttribute(COMMENTTAG, comment);
		 else
			elem.setAttribute(COMMENTTAG, "ID: "+id);
		 if(tagName.equals(IELEMENTTAG))
			 elem.appendChild(_doc.createElement(DEPENDENCYTAG));
		_diagramNode.appendChild(elem);
	}
	/**
	 * Adds a new Actor
	 * @param id unique id
	 * @param name name of element to display
	 * @param type type of actor
	 * @param comment optional comment
	 * @throws Exception
	 */
	public void createActor(String id, String name, String type, String comment) throws Exception
	{
		type=type.toLowerCase();
		if(isAllowedActorType(type))
			createNode(ACTORTAG, id, name, type, comment);
		else
			throw new Exception("Actor type: "+type+" not supported.");
	}
	/**
	 * Deletes an Actor
	 * @param id
	 * @throws Exception
	 */
	public void deleteActor(String id) throws Exception
	{		
		Node node=getNodeByID(id);
		if(!((Element)node).getTagName().equals(ACTORTAG))
			throw new Exception("Deletion: Element with id : "+id+" is not an actor.");
		NodeList nodes=node.getChildNodes();
		
		for (int i = 0; i < nodes.getLength(); i++) {			
			Element el = (Element) nodes.item(i);
			
			if(el.getTagName().equals(ACTORLINKTAG))
			{
				String aref=el.getAttribute(AREFTAG);				
				StringTuple st = new StringTuple(id, aref);
				_edgeHash.remove(st);
				
			}
		}
		node.getParentNode().removeChild(node);
		_idHash.remove(id);
		
		//remove/cleanup external references
		NodeList nodeList =(NodeList) _xPath.compile("//*[@"+AREFTAG+"='"+id+"']").evaluate(_doc, XPathConstants.NODESET);
		for(int i=0; i<nodeList.getLength();i++)
		{
			Element el =(Element)nodeList.item(i);
			Element parent=(Element)el.getParentNode();
			String parentID=parent.getAttribute(IDTAG);
					
			String tagName=el.getTagName().trim();
			String aref=el.getAttribute(AREFTAG);
            switch(tagName)
            {
                case ACTORLINKTAG:
                    deleteActorLink(parentID, aref);
                    break;
                case DEPENDEETAG:
                    deleteDependee(parentID, aref);
                    break;
                case DEPENDERTAG:
                    deleteDepender(parentID, aref);
                    break;
            }
			
			
		}
	}	
	/**
	 * Edits name, type, comment of an existing node
	 * @param id
	 * @param name
	 * @param type
	 * @param comment
	 * @throws Exception
	 */
	public void editActor(String id, String name, String type, String comment) throws Exception
	{		
		editActor(id,name,type);
		Element node=(Element) getNodeByID(id);	
		node.setAttribute(COMMENTTAG,comment);
				
	}	
	/**
	 * Edits name, type of an existing node
	 * @param id
	 * @param name
	 * @param type
	 * @throws Exception
	 */
	public void editActor(String id, String name, String type) throws Exception
	{		
		Element node=(Element) getNodeByID(id);	
		node.setAttribute(NAMETAG,name);
		node.setAttribute(TYPETAG,type);
		
				
	}	
	/**
	 * Adds an ActorLink to an Actor Node (each Actor can have one)
	 * @param actorID unique id of the actor
	 * @param type type of the actor link
	 * @param aref id of target node of the link
	 * @throws Exception
	 */
	public void createActorLink(String actorID, String type, String aref) throws Exception
	{
		type=type.toLowerCase();
		if(!isAllowedActorLinkType(type))		
			throw new Exception("ActorLink type: "+type+" not supported.");
		
		Node actor=getNodeByID(actorID);
		if(actor==null)//not existing ID
			throw new Exception("ID: "+actorID+" does not exist.");
		/*if(actor.getChildNodes().getLength()>0)
		{
			throw new Exception("ID: "+actorID+" has already a link element.");
		}*/
		if(actorID.equals(aref))
		{
			throw new Exception("ID: "+actorID+" actorLink points on its parent.");
		}
		if(!((Element)actor).getTagName().equals(ACTORTAG))//not actor
			throw new Exception(actorID+ " not ID of an actor");		
		
		if(!_idHash.contains(aref)||!((Element)getNodeByID(aref)).getTagName().equals(ACTORTAG))
		{
			throw new Exception(aref+ " not ID of an existing actor");
		}
		if(!_edgeHash.add(new StringTuple(actorID, aref)))
		{
			//throw new Exception("Actor Link ID: "+actorID+" aref: "+aref+" already existng");
			editActorLink(actorID, aref, type);
			return;
		}
		
		Element elem= _doc.createElement(ACTORLINKTAG);
		elem.setAttribute(TYPETAG, type);
		elem.setAttribute(AREFTAG, aref);
		actor.appendChild(elem);
		
		
	}
	/**
	 * Deletes an ActorLink
	 * @param actorID unique id of the actor
	 * @param aref id of target node of the link
	 * @throws Exception
	 */
	public void deleteActorLink(String actorID, String aref) throws Exception
	{
		Node node = getActorLink(actorID, aref);			
		node.getParentNode().removeChild(node);
		StringTuple st = new StringTuple(actorID, aref);
		_edgeHash.remove(st);
	}	
	/**
	 * Retrieves the ActorLink Node of an Actor Node
	 * The ActroLink is uniquely identified by its parent id and the aref attribute
	 * @param actorID unique id of the actor
	 * @param aref id of target node of the link
	 * @return
	 * @throws Exception
	 */
	private Node getActorLink(String actorID, String aref) throws Exception
	{
		Node actor=getNodeByID(actorID);
		
		Node node =(Node) _xPath.compile("actorLink[@"+AREFTAG+" = '" + aref + "']").evaluate(actor, XPathConstants.NODE);
		if(node==null)
		{
			throw new Exception("Link of actor "+actorID+ " with aref "+aref+" not found");
		}
		return node;
	}
	/**
	 * Edits the type of a given actorLink
	 * @param actorID
	 * @param aref
	 * @param type
	 * @throws Exception
	 */
	public void editActorLink(String actorID, String aref, String type) throws Exception
	{		
		Element node = (Element)getActorLink(actorID, aref);
		
		
		
		node.setAttribute(TYPETAG, type);
		
		
	}	
	/**
	 * Adds a new ielement (for goal,task etc)
	 * @param id unique id for the ielement
	 * @param name name of element
     * @param type type of element
     * @param comment optional comment text
	 * @throws Exception
	 */
	public void createIElement(String id, String name, String type, String comment) throws Exception
	{
		type=type.toLowerCase();
		if(isAllowedIElementType(type))
		{			
			
			createNode(IELEMENTTAG, id, name, type, comment);
		}
		else
			throw new Exception("ielement type: "+type+" not supported.");		
		
		
	}
	/**
	 * Deletes an ielement
	 * @param id unique id of the ielement
	 * @throws Exception
	 */
	public void deleteIElement(String id) throws Exception
	{
		Node node=getNodeByID(id);
		if(!((Element)node).getTagName().equals(IELEMENTTAG))
			throw new Exception("Deletion: Element with id : "+id+" is not an ielement.");
		NodeList nodes=node.getFirstChild().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {			
			Element el = (Element) nodes.item(i);
			if(el.getTagName().equals(DEPENDEETAG))
			{
				String aref=el.getAttribute(AREFTAG);
				deleteDependee(id, aref);
			}
			else if(el.getTagName().equals(DEPENDERTAG))
			{
				String aref=el.getAttribute(AREFTAG);
				deleteDepender(id, aref);
				
			}
		}
		node.getParentNode().removeChild(node);
		_idHash.remove(id);
		//remove/cleanup external references
		/*NodeList nodeList =(NodeList) _xPath.compile("//*[@"+AREFTAG+"='"+id+"']").evaluate(_doc, XPathConstants.NODESET);
		for(int i=0; i<nodeList.getLength();i++)
		{
			Element el =(Element)nodeList.item(i);
			Element parent=(Element)el.getParentNode();
			String parentID=parent.getAttribute(IDTAG);
					
			String tagName=el.getTagName().trim();
			String aref=el.getAttribute(AREFTAG);
			if(tagName.equals(ACTORLINKTAG))
			{
				deleteActorLink(parentID, aref);
			}
			else if(tagName.equals(DEPENDEETAG))
			{
				deleteDependee(parentID, aref);
			}
			else if(tagName.equals(DEPENDERTAG))
			{
				deleteDepender(parentID, aref);
			}
		}*/
	}
	/**
	 * Edits name, type, comment of an existing ielement
	 * @param id
	 * @param name
	 * @param type
	 * @param comment
	 * @throws Exception
	 */
	public void editIElement(String id, String name, String type, String comment) throws Exception
	{
		editActor(id,name, type, comment);
	}	
	
	/**
	 * Edits name, type of an existing ielement
	 * @param id
	 * @param name
	 * @param type
	 * @throws Exception
	 */
	public void editIElement(String id, String name, String type) throws Exception
	{
		editActor(id,name, type);
	}	
	
	/**
	 * Adds a new depender to an ielement
	 * @param ielementID unique id of the ielement
	 * @param aref id of the target node
	 * @throws Exception
	 */
	public void createDepender(String ielementID, String aref) throws Exception
	{
		createDependency(ielementID,DEPENDERTAG,aref);
	}
	/**
	 * Adds a new dependee to an ielement
	 * @param ielementID unique id of the element
	 * @param aref id of the target node
	 * @throws Exception
	 */
	public void createDependee(String ielementID, String aref) throws Exception
	{
		createDependency(ielementID,DEPENDEETAG,aref);
	}
	/**
	 * Adds a new dependency (dependee or depender) to an ielement
	 * @param ielementID unique id of the ielement
	 * @param type type (either dependee or depender)
	 * @param aref id of the target node
	 * @throws Exception
	 */
	private void createDependency(String ielementID, String type, String aref) throws Exception
	{
		//System.out.println(new StringTuple(ielementID, aref).toString());
		if(!_idHash.contains(aref)||!((Element)getNodeByID(aref)).getTagName().equals(ACTORTAG))
		{
			throw new Exception(aref+ " not ID of an existing actor");
		}
		Node node= getNodeByID(ielementID);//select dependency node
		
		if(!((Element)node).getTagName().equals(IELEMENTTAG))//not ielement
			throw new Exception(ielementID+ " not ID of an ielement");
		if(node.getFirstChild()==null)
			throw new Exception("ielement has no dependency child");
		if(_edgeHash.add(new StringTuple(ielementID, type+aref)))
		{
			Element newNode = _doc.createElement(type);
			newNode.setAttribute(AREFTAG, aref);
			node.getFirstChild().appendChild(newNode);
		}
		
		
		
		
	}
	/**
	 * Changes the aref attribute of a depender
	 * @param ielementID unique id of the ielement
	 * @param aref id of the target node
	 * @param new_aref new target node id
	 * @throws Exception
	 */
	public void editDepender(String ielementID, String aref, String new_aref) throws Exception
	{
		editDependency(ielementID,DEPENDERTAG,aref,new_aref);
	}
	/**
	 * Changes the aref attribute of a dependee
	 * @param ielementID unique id of the ielement
	 * @param aref id of the target node
	 * @param new_aref new target node id
	 * @throws Exception
	 */
	public void editDependee(String ielementID, String aref, String new_aref) throws Exception
	{
		editDependency(ielementID,DEPENDEETAG,aref,new_aref);
	}
	/**
	 * Changes the aref attribute of a depender/dependee
	 * @param ielementID unique id of the ielement
	 * @param type type of the dependency (depender or dependee)
	 * @param aref id of target node
	 * @param new_aref new target node id
	 * @throws Exception
	 */
	private void editDependency(String ielementID, String type, String aref, String new_aref)throws Exception
	{				
		Element node= (Element)getDependency(ielementID, type, aref);
		
		StringTuple st = new StringTuple(ielementID, type+aref);
		_edgeHash.remove(st);
		if(!_edgeHash.add(new StringTuple(ielementID, type+new_aref)))
		{
			throw new Exception("Dependency ID: "+ielementID+" aref: "+new_aref+" of type "+type+" already existng");
		}
		node.setAttribute(AREFTAG, new_aref);		
	}	
	/**
	 * Deletes a depender from an ielement
	 * @param ielementID unique id of the ielement
	 * @param aref id of the target node
	 * @throws Exception
	 */
	public void deleteDepender(String ielementID, String aref) throws Exception
	{
		deleteDependency(ielementID,DEPENDERTAG,aref);
	}
	/**
	 * Deletes a dependee from an ielement
	 * @param ielementID unique id of the ielement
	 * @param aref id of the target node
	 * @throws Exception
	 */
	public void deleteDependee(String ielementID, String aref) throws Exception
	{
		deleteDependency(ielementID,DEPENDEETAG,aref);
	}
	/**
	 * Deletes a depender/dependee from an ielement
	 * @param ielementID unique id of the element
	 * @param type type of the dependency (either depender or dependee)
	 * @param aref id of the target node
	 * @throws Exception
	 */
	private void deleteDependency(String ielementID, String type, String aref)throws Exception
	{
		Node node= getDependency(ielementID, type, aref);
		node.getParentNode().removeChild(node);
		StringTuple st = new StringTuple(ielementID, type+aref);
		_edgeHash.remove(st);
	}
	/**
	 * Gets a specific dependency (dependeee or depender) of an ielement
	 * @param ielementID unique id of the ielement
	 * @param type type of the dependency ( either depender or dependee)
	 * @param aref id of the target node
	 * @return
	 * @throws Exception
	 */
	private Node getDependency(String ielementID, String type, String aref)throws Exception 
	{
		Node ielem= getNodeByID(ielementID).getFirstChild();
		Node node =(Node) _xPath.compile(type+"[@"+AREFTAG+" = '" + aref + "']").evaluate(ielem, XPathConstants.NODE);
		if(node==null)
		{
			throw new Exception(type +" of ielement "+ielementID+ " with aref "+aref+" not found");
		}
		return node;
	}
	/**
	 * Retrieves a Node (Actor or ielement) by the unique ID
	 * @param id unique id of the node element
	 * @return
	 * @throws Exception
	 */
	private Node getNodeByID(String id) throws Exception //gets Element by ID via XPath for getElementByID 
	//tooo much overhead is needed (schemas, DTD etc) especially in the xml itself
	{
		Node node =(Node) _xPath.compile("//*[@"+IDTAG+" = '" + id + "']").evaluate(_doc, XPathConstants.NODE);
		if(node==null)
			throw new Exception("Node with id: "+id+" not found");
		return node;
	}	
	/**
	 * Outputs the XML pretty printed as a String
	 */
	public String toString()
	{
		if(_doc!=null)
		{			
			try
			{				
				Transformer t = TransformerFactory.newInstance().newTransformer();
				StreamResult out = new StreamResult(new StringWriter());
				t.setOutputProperty(OutputKeys.INDENT, "yes"); //pretty printing
				t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
				t.transform(new DOMSource(_doc),out);
				return out.getWriter().toString();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return "";
			}
			
		}
		else
			return "";
	}
	/**
	 * Validates a given XML file
	 * @param xml
	 * @param message reference to return an error message
	 * @return true/false if file is valid/invalid
	 * @throws Exception
	 */
	private boolean validateIStarML(String xml, StringBuilder message ) throws Exception
	{		
			
		try
	    {	       
	        _validator.validate(new StreamSource(
	        		new StringReader(xml.replaceFirst("<istarml", "<istarml xmlns=\"istarml/check\" "))));
	        return true;
	    }
	    catch(Exception ex)
	    {
	    	message.append(ex.getMessage());
	    	return false;	    	
	    }		
		
	}
}


package i5.las2peer.services.iStarMLModelService;

import i5.las2peer.api.Service;



import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.StorageException;

import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.iStarMLModelService.data.*;
import i5.las2peer.services.iStarMLModelService.mail.*;

import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;

import java.io.File;
import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
import java.io.StringReader;


//import java.io.PrintWriter;
import java.io.StringWriter;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;
import org.apache.commons.codec.binary.Base64;
/**
 * Service responsible for model management of iStarML models
 * @author Alexander
 *
 */
@Path("IStarMLModelService")
public class IStarMLModelService extends Service
{
	private static final String ENVELOPE_NAME = "_IStarMLModelService_eXistWrite";
	private static final String SERVICE_CONFIG = "./config/serviceConfig.conf";
	private static final String DEPENDEE_TYPE = "dependee";
	private static final String DEPENDER_TYPE = "depender";
	private static final String IELEMENT_NODE_TYPE = "ielement";
	private static final String ACTOR_NODE_TYPE = "actor";
	static final String OK = "ok";

	protected static String existURI =  "xmldb:exist://localhost:8080/exist/xmlrpc";//URI to the Database

	private XPath _xPath =  XPathFactory.newInstance().newXPath();
	
	private Document _doc;
    private DocumentBuilder _dBuilder;
	private MailData _userCreatedMail;
	private String _admin;
	private String _adminPass;
    private boolean _useHTTPS=false;

    private HashMap<Long,ModelBuffer> bufferHack=new HashMap<>();
    private static Timer timer=new Timer();
	/**
	 * Reads a local file from a given path
	 * @param path
	 * @return file contents as a String
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
	/*
	private void setFile(String path, String text) throws Exception
	{
		PrintStream out = null;
		try {
		    out = new PrintStream(new FileOutputStream(path));
		    out.print(text);
		}
		finally {
		    if (out != null) out.close();
		}
	}*/
	/**
	 * Reads the service config file
	 * @throws Exception
	 */
	private void getConfig() throws Exception
	{
		try {
			String contents=getFile(SERVICE_CONFIG);
			_doc= _dBuilder.parse(new InputSource(new StringReader(contents)));
			_doc.getDocumentElement().normalize();
			
			existURI =(String) _xPath.compile("//uris/existuri/@value").evaluate(_doc, XPathConstants.STRING);
			_admin = (String) _xPath.compile("//dblogin/admin/@value").evaluate(_doc, XPathConstants.STRING);
			_adminPass = (String) _xPath.compile("//dblogin/adminPass/@value").evaluate(_doc, XPathConstants.STRING);
            boolean _sendMail = ((String) _xPath.compile("//mail/@value").evaluate(_doc, XPathConstants.STRING)).toLowerCase().trim().equals("true");
			_useHTTPS=((String) _xPath.compile("//dblogin/https/@value").evaluate(_doc, XPathConstants.STRING)).toLowerCase().trim().equals("true");
			_userCreatedMail=null;
			
			if(_sendMail)
			{
				String to=(String) _xPath.compile("//mail/to/@value").evaluate(_doc, XPathConstants.STRING);
				String host=(String) _xPath.compile("//mail/host/@value").evaluate(_doc, XPathConstants.STRING);
				int port=Integer.valueOf((String) _xPath.compile("//mail/port/@value").evaluate(_doc, XPathConstants.STRING));
				String user=(String) _xPath.compile("//mail/user/@value").evaluate(_doc, XPathConstants.STRING);
				String pass=(String) _xPath.compile("//mail/pass/@value").evaluate(_doc, XPathConstants.STRING);
				String subject=(String) _xPath.compile("//mail/subject/@value").evaluate(_doc, XPathConstants.STRING);
				String text=(String) _xPath.compile("//mail/text/@value").evaluate(_doc, XPathConstants.STRING);
				
				_userCreatedMail = new MailData(to, host, port, user, pass, subject, text);
			}
			
		} catch (Exception e) {
			System.out.println("Could not read config file "+ SERVICE_CONFIG);
			e.printStackTrace();
		}
		
	}
	/**
	 * Retrieves the data stored in the users service envelope
	 * @return
	 */
	private ModelBuffer getEnvelopeData()
	{
		try {
			Agent agent=this.getContext().getMainAgent();
			Long userID=agent.getId();
			
			if(!bufferHack.containsKey(userID))
            {
                bufferHack.put(userID,new ModelBuffer("","",""));
            }
            else
                return bufferHack.get(userID);

            /*String envelopeName=Long.toString(userID)+ENVELOPE_NAME;
			Envelope envi;
			try
			{
				envi=Envelope.fetchClassIdEnvelope(ModelBuffer.class, envelopeName);
			}
			catch(StorageException | ArtifactNotFoundException se)//Envelope not existing?
			{
				
				return null;
			}
			if(envi!=null)
			{
				envi.open(agent);//open
				ModelBuffer mb=envi.getContent(ModelBuffer.class);//read
				envi.close();//close
				return mb;
			}*/
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Updates the current envelope contents or creates a new envelope, if none exists
	 * @param update new contents
	 */
	private void updateEnvelopeData(ModelBuffer update)
	{
		try 
		{
			
			Agent agent=this.getContext().getMainAgent();
			Long userID=agent.getId();

            if(!bufferHack.containsKey(userID))
                bufferHack.put(userID,update);
            else
            {
                bufferHack.put(userID, update);
            }

            cleanUp();
			/*String envelopeName=Long.toString(userID)+ENVELOPE_NAME;
			Envelope envi;
			try
			{
				envi=Envelope.fetchClassIdEnvelope(ModelBuffer.class, envelopeName);
			}
			catch(StorageException | ArtifactNotFoundException se)//Envelope not existing?
			{
				//create new
				envi = Envelope.createClassIdEnvelope(update,envelopeName,this.getContext().getMainAgent());
				envi.open(agent);
				envi.setOverWriteBlindly(true);
				envi.store();	
				envi.close();
				
			}
			if(envi!=null)
			{
				envi.open(agent);
				ModelBuffer mb =envi.getContent(ModelBuffer.class);	//change the contents
				mb.setCollection(update.getCollection());
				mb.setResource(update.getResource());
				mb.setContent(update.getContent());
				envi.store();//re serialize it
				
				envi.close();
				
			}*/


				
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	/**
	 * Commits the buffered contents to the database
	 * @param buffer
	 */
	private void commitModelBuffer(ModelBuffer buffer) 
	{
		try
		{
			if(!buffer.getContent().trim().isEmpty())//only if content is provided, empty "" is illegal anyway
			{
				StringTuple st =getDBLogin();
				Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
				storage.loadCollection(buffer.getCollection());				
				storage.setResource(buffer.getResource(), buffer.getContent());		
				storage.closeCollection();
				updateEnvelopeData(new ModelBuffer("", "", ""));//remove, so no further repeated 'ghost' commits
			}
		}
		catch(Exception e)
		{
			updateEnvelopeData(new ModelBuffer("", "", ""));
			e.printStackTrace();
		}
	}
	/**
	 * cleanUp is called from the web connector before user logout.
	 * Commits all buffered changes
	 */
	public void cleanUp()
	{		
		ModelBuffer buffer=getEnvelopeData();
		if(buffer!=null)
		{
			commitModelBuffer(buffer);
			
		}


		
	}
	/**
	 * Retrieves the contents of a resource from the DB
	 * @param collection path
	 * @param resource resource name
	 * @return XML content as String
	 * @throws Exception
	 */
	private String getResourceContentFromDB(String collection, String resource)throws Exception
	{
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		return storage.getResource(resource);
		
	}
	/** 
	 * Loads the {@link IStarMLEditor} and the contents from the proper source (buffer or DB)
	 * @param collection
	 * @param resource
	 * @return {@link IStarMLEditor} with the already loaded XML
	 * @throws Exception
	 */
	private IStarMLEditor getEditor(String collection,String resource) throws Exception
	{
		IStarMLEditor editor=new IStarMLEditor();
		ModelBuffer mb = getEnvelopeData();
		String resContent;
		boolean validate;
		
		if(mb==null||mb.getContent().isEmpty()||mb.getResource().isEmpty())//if buffer is not existent or empty, load from DB
		{
			resContent=getResourceContentFromDB(collection,resource);
			validate=true;//validate from database
		}
		else
		{
			
			if(!(mb.getCollection().equals(collection)&&mb.getResource().equals(resource)))//some other resource specified in the buffer?
			{
				commitModelBuffer(mb); //commit and get from DB
				resContent=getResourceContentFromDB(collection,resource);
				validate=true;//validate from database
			}
			else
			{
				resContent=mb.getContent();//get from envelope
				validate=false;//don't validate from envelope
				
			}
		}
		//System.out.println(validate);
		//load and check
		if(editor.loadXML(resContent, validate).startsWith("Error:"))
		{
			updateEnvelopeData(new ModelBuffer("", "", ""));//if error (invalid XML), remove buffer
			
			/*resContent=getResourceContentFromDB(collection,resource);
			if(editor.loadXML(resContent).startsWith("Error:"))
			{
				StringTuple st =getDBLogin();
				Storage storage= new Storage(existURI, st.getS1(), st.getS2());
				storage.loadCollection(collection);
				ResourceVersion[] rv=storage.getVersions(resource);
				int highestrev=0;
				for(int i=0;i<rv.length;i++)
				{
					int rev=rv[i].getRevision();
					if(rev>highestrev)
						highestrev=rev;
				}
				if(highestrev>0)//restore last probably correct
				{
					resContent=storage.getVersion(resource, highestrev);
					editor.loadXML(resContent);
				}
			}*/
		}
		return editor;
	}
	/**
	 * Constructor, reads config
	 * @throws Exception
	 */
	public IStarMLModelService() throws Exception
	{


        DocumentBuilderFactory _dbFactory = DocumentBuilderFactory.newInstance();
		_dBuilder = _dbFactory.newDocumentBuilder();
		getConfig();
/*
        timer.scheduleAtFixedRate(
                new TimerTask()
                {
                    public void run()
                    {
                       cleanUp();
                    }
                },
                0,      // run first occurrence immediately
                5*1000);
*/
		
	}
    public String getRESTMapping()
    {
        String result="";
        try
        {
            result= RESTMapper.getMethodsAsXML(this.getClass());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }

	/**
	 * Converts the given path from the URI to a valid Collection path (col1.col12.col123-> col1/col12/col123)
	 * @param collection URI collection path to convert
	 * @return valid collection path
	 */
	private String getCollectionPath(String collection) {
		return collection.replace('.', '/');
	}
	/**
	 * Creates a XML as a String to return multiple data values to the service caller
	 * @param data shallow tree of data elements
	 * @return XML
	 * @throws Exception
	 */
	private String createXMLResponse(XMLResponseElement data) throws Exception
	{
		return createXMLResponse(new XMLResponseElement[]{data});
	}
	/**
	 * Creates a XML as a String to return multiple data values to the service caller
	 * @param data shallow trees of data elements
	 * @return XML
	 * @throws Exception
	 */
	private String createXMLResponse(XMLResponseElement[] data) throws Exception
	{
		_doc=_dBuilder.newDocument();
		Element root=_doc.createElement("ModelResponse");//wrap everything into a root node
		_doc.appendChild(root);
		
		for (XMLResponseElement xmlResponseElement : data) //for each group
		{
			Element elem=_doc.createElement(xmlResponseElement.getName());
			for (XMLResponseElementChild dataElem : xmlResponseElement.getChildren()) //for each element of a group
			{
				Element subElem=_doc.createElement(dataElem.getName()); //create an element and set attributes
				for (StringTuple attribute : dataElem.getAttributes()) 
				{
					subElem.setAttribute(attribute.getS1(), attribute.getS2());		
				}
				elem.appendChild(subElem);
			}
			root.appendChild(elem);			
		}		
		return prettyPrintXML(_doc);
	}
	/**
	 * Creates automatic user name and password from las2peer login data (private key is hashed)
	 * @return name and password for DB login
	 * @throws Exception
	 */
	 
	private StringTuple getDBLogin() throws Exception
	{
		Long userID=this.getContext().getMainAgent().getId();
		String name=((UserAgent)(this.getContext().getLocalNode().getAgent(userID))).getLoginName();
		String key=Base64.encodeBase64String(this.getContext().getMainAgent().getPrivateKey().getEncoded());
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");		
		md.update(key.getBytes("UTF-8")); 
		byte[] digest = md.digest();
		String pass=Base64.encodeBase64String(digest);
		
		name=name.replace(" ", "_");
		return new StringTuple(name,pass);
	}
	
	/**
	 * Creates a new account for the database, if not already existing and informs someone via mail (if set in the config file)
	 * @param message optional personal message to the mail receiver
	 * @return OK
	 * @throws Exception
	 */
	@POST
	@Path("setting/register/DB")
	public String createDBAccount(@DefaultValue("") @ContentParam() String message) throws Exception
	{
		try
        {
            StringTuple st =getDBLogin();
            Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());


            _userCreatedMail.setSubject("User registered for DB: "+st.getS1());
            _userCreatedMail.setText("User message: \n\n"+message);

            storage.createUser(_admin, _adminPass, st.getS1(), st.getS2(), _userCreatedMail);

            return OK;
        }
        catch(Exception e)
        {
            return e.getMessage();
        }

	}
	
	/**
	 * Checks, if a DB account already exists for the current user
	 * @return true, if user has already a DB account
	 * @throws Exception
	 */
	@GET
	@Path("setting/register/DB")
	public boolean existsDBAccount() throws Exception
	{
		StringTuple st =getDBLogin();
		try
		{
			Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
			storage.loadCollection("");
			storage.closeCollection();
			return true;
		}
		catch(Exception e) //lazy check (for an xquery a login is needed)
		{
			return false;
		}
		
		
	}
	/**
	 * Simply checks, if valid login data is provided for the service
	 * @return
	 * @throws Exception 
	 */
	@GET
	@Path("")
	public String loginCheck() throws Exception
	{
		
		
	
		
		return OK;
	}
	/**
	 * Lists the contents (sub-Collections and resources) of a collection, allows search
	 * @param collection root collection to search/list
	 * @param searchQuery optional search query for a resource or node element
	 * @param searchType type of search: model or node type (actor, role, goal etc)
	 * @return list of contents (or matches of the search)
	 * @throws Exception
	 */
	@GET
	@Path("{collection}")
	public String listCollectionContents(@PathParam("collection") String collection,
			@QueryParam(name="search",defaultValue = "") String searchQuery, @QueryParam(name="searchType", defaultValue = "model") String searchType) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI, _useHTTPS,st.getS1(), st.getS2());
		storage.loadCollection(collection);
		String result="";
		if(!searchQuery.trim().isEmpty())//user wants to search
		{
			
			String[] found=storage.searchStorage(collection,searchQuery,searchType);
			XMLResponseElement resp = new XMLResponseElement("SearchResult"); //construct return XML structure
            for(String aFound : found)
            {


                String name = aFound;
                String path = aFound;
                int lastSlash = name.lastIndexOf('/');
                if(lastSlash > 0)
                {
                    name = name.substring(lastSlash + 1); //separate path and resource name
                    path = path.substring(0, lastSlash);
                }

                XMLResponseElementChild respChild = new XMLResponseElementChild("Resource");
                respChild.addAttribute("name", name);
                respChild.addAttribute("path", path);
                respChild.addAttribute("lastModified", storage.getLastModifiedDate(path, name).toString());
                resp.addChild(respChild);
            }
			result=createXMLResponse(resp);
			
		}
		else//just output collection contents
		{
			
			CollectionData[] cols=storage.listSubCollections(collection);
			ResourceData[] res=storage.listResources(collection);
			XMLResponseElement respCol = new XMLResponseElement("Collections");
            for(CollectionData col : cols)
            {
                XMLResponseElementChild respChild = new XMLResponseElementChild("Collection");
                respChild.addAttribute("name", col.getName());
                respChild.addAttribute("owner", col.getOwner());
                respCol.addChild(respChild);
            }
			
			XMLResponseElement respRes = new XMLResponseElement("Resources");
            for(ResourceData re : res)
            {
                XMLResponseElementChild respChild = new XMLResponseElementChild("Resource");
                respChild.addAttribute("name", re.getName());
                respChild.addAttribute("lastModified", re.getLastModified().toString());
                respRes.addChild(respChild);
            }
			
			result=createXMLResponse(new XMLResponseElement[]{respCol,respRes});
		}
		storage.closeCollection();
		return result;
	}
	/**
	 * Retrieves the contents of a resource (XML)
	 * @param collection
	 * @param resource	 
	 * @return XML content of the given resource
	 * @throws Exception
	 */
	@GET
	@Path("{collection}/{resource}")
	public String getResourceContents(@PathParam("collection") String collection,
			@PathParam("resource") String resource) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI, _useHTTPS,st.getS1(), st.getS2());
		storage.loadCollection(collection);
		String result;
		result=storage.getResource(resource);		
		storage.closeCollection();
		
		return result;
	}
	/**
	 * Copies a resource
	 * @param collection source collection
	 * @param resource source resource
	 * @param targetCollection target collection
	 * @param targetResource target resource
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/copy/{targetCollection}/{targetResource}")
	public String copyResource(@PathParam("collection") String collection,
			@PathParam("resource") String resource,
			@PathParam("targetCollection") String targetCollection,
			@PathParam("targetResource") String targetResource
			) throws Exception
	{
		
		String res=getResourceContents(collection, resource);
		setResourceContent(targetCollection,targetResource,res);
		
		return OK;
	}
	/**
	 * Copies a specific resource version
	 * @param collection source collection
	 * @param resource source resource
	 * @param version version of source resource, use 0 for base version.
	 * @param targetCollection target collection
	 * @param targetResource target resource
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/versions/{version}/copy/{targetCollection}/{targetResource}")
	public String copyResource(@PathParam("collection") String collection,
			@PathParam("resource") String resource,
			@PathParam("version") int version,
			@PathParam("targetCollection") String targetCollection,
			@PathParam("targetResource") String targetResource
			) throws Exception
	{
		String res=getResourceContents(collection, resource,version);
		setResourceContent(targetCollection,targetResource,res);
		
		return OK;
	}
	/**
	 * Lists all previous versions of a resource
	 * @param collection
	 * @param resource
	 * @return list with previous versions (name, revision, date, user)
	 * @throws Exception
	 */
	@GET
	@Path("{collection}/{resource}/versions")
	public String getResourceVersions(@PathParam("collection") String collection,
			@PathParam("resource") String resource) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		String result;
		ResourceVersion[] versionData=storage.getVersions(resource);
		
		XMLResponseElement respRes = new XMLResponseElement("Versions");
        for(ResourceVersion aVersionData : versionData)
        {
            XMLResponseElementChild respChild = new XMLResponseElementChild("Version");
            respChild.addAttribute("name", resource);
            respChild.addAttribute("revision", Integer.toString(aVersionData.getRevision()));
            respChild.addAttribute("date", aVersionData.getDate().toString());
            respChild.addAttribute("user", aVersionData.getUser());
            respRes.addChild(respChild);
        }
		result=createXMLResponse(respRes);
		
		
		storage.closeCollection();
		return result;
	}
	/**
	 * Gets the specific version of a resource
	 * @param collection
	 * @param resource
	 * @param version use 0 for base version
	 * @return XML content of the specific version
	 * @throws Exception
	 */
	@GET
	@Path("{collection}/{resource}/versions/{version}")
	public String getResourceContents(@PathParam("collection") String collection,
			@PathParam("resource") String resource,@PathParam("version") int version) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		String result;
		result=storage.getVersion(resource,version);
		storage.closeCollection();
		return result;
	}
	/**
	 * Creates a new collection
	 * @param collection path to the new collection
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}")
	public String createCollection(@PathParam("collection") String collection) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();

        Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());

		storage.createCollection(collection);

		String result=OK;
		storage.closeCollection();

		return result;
	}
	/**
	 * Creates a new resource. Already existing resources are not overwritten.
	 * @param collection
	 * @param resource
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}")
	public String createResource(@PathParam("collection") String collection,
			@PathParam("resource") String resource) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		storage.createResource(resource, "", false);
		String result=OK;
		storage.closeCollection();
		
		return result;
	}
	/**
	 * Deletes an existing collection
	 * @param collection
	 * @return OK
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}")
	public String deleteCollection(@PathParam("collection") String collection) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.deleteCollection(collection);
		String result=OK;
		storage.closeCollection();
		
		return result;
	}
	/**
	 * Deletes an existing resource
	 * @param collection
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}/{resource}")
	public String deleteResource(@PathParam("collection") String collection,
			@PathParam("resource") String resource) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		storage.deleteResource(resource);
		String result=OK;
		storage.closeCollection();
		
		return result;
	}
	/**
	 * Creates a new resource with the given content.
	 * Existing resources are overwritten.
	 * @param collection
	 * @param resource
	 * @param content
	 * @return OK
	 * @throws Exception
	 */
	@POST
	@Path("{collection}/{resource}")
	public String setResourceContent(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @ContentParam() String content ) throws Exception
	{
		cleanUp();
		collection = getCollectionPath(collection);
		StringTuple st =getDBLogin();
		Storage storage= new Storage(existURI,_useHTTPS, st.getS1(), st.getS2());
		storage.loadCollection(collection);
		storage.createResource(resource, content,true);
		String result=OK;
		storage.closeCollection();
		
		return result;
	}

	/**
	 * Creates a new node (actor/ielement) in an existing resource
	 * @param collection
	 * @param resource
	 * @param nodeType type of the node (actor/ielement)
	 * @param nodeID id of the node to create
	 * @param name name of the node to create
	 * @param type type of the node (agent, goal, role etc.)
	 * @param comment optional comment
	 * @return OK
	 * @throws Exception
	 */
	private String createNode(String collection,
			String resource, String nodeType, String nodeID,
			String name, String type,  String comment) throws Exception
	{		
		collection = getCollectionPath(collection);
		
		IStarMLEditor editor=getEditor(collection,resource);


        switch(nodeType)
        {
            case ACTOR_NODE_TYPE:
                editor.createActor(nodeID, name, type, comment);
                break;
            case IELEMENT_NODE_TYPE:
                editor.createIElement(nodeID, name, type, comment);
                break;
            default:
                throw new Exception("Not supported node type: " + nodeType);
        }
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
		
		
		//storage.setResource(resource, editor.toString());

        //storage.closeCollection();
		
		return OK;
	}
	/**
	 * Creates a new actor node in a resource
	 * @param collection
	 * @param resource
	 * @param nodeID id of the actor
	 * @param name name of the actor
	 * @param type type of the actor
	 * @param comment optional comment
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/actor/{nodeID}")
	public String createActor(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("nodeID") String nodeID,
			@QueryParam(name="name",defaultValue = "Actor") String name, @QueryParam(name="type",defaultValue = "actor") String type, @QueryParam(name="comment", defaultValue = "") String comment) throws Exception
	{
		return createNode(collection,resource,ACTOR_NODE_TYPE, nodeID, name,type,comment);
	}
	/**
	 * Creates a new ielement node in a resource
	 * @param collection
	 * @param resource
	 * @param nodeID id of the ielement
	 * @param name name of the ielement
	 * @param type type of the ielement
	 * @param comment optional comment
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/ielement/{nodeID}")
	public String createIElement(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("nodeID") String nodeID,
			@QueryParam(name="name",defaultValue = "Goal") String name, @QueryParam(name="type",defaultValue = "goal") String type, @QueryParam(name="comment",defaultValue = "") String comment) throws Exception
	{
		return createNode(collection,resource,IELEMENT_NODE_TYPE, nodeID, name,type,comment);
	}
	/**
	 * Creates a new node in a resource without a specified id. The id is generated internally and returned to the caller.
	 * @param collection
	 * @param resource
	 * @param nodeType actor/ielement
	 * @param name name of the node
	 * @param type type of the node
	 * @param comment optional comment
	 * @return id used for the node
	 * @throws Exception
	 */
	private String createNode(String collection,
			String resource, String nodeType,
			String name, String type,  String comment) throws Exception
	{
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
		String id=editor.generateSomeFreeID();
		createNode(collection, resource, nodeType, id, name, type, comment);
		
		return id;
	}
	/**
	 * Creates a new actor node in a resource without a specific id
	 * @param collection
	 * @param resource
	 * @param name name of the actor
	 * @param type type of the actor
	 * @param comment optional comment
	 * @return id used for the actor
	 * @throws Exception
	 */
	@POST
	@Path("{collection}/{resource}/actor")
	public String createActor(@PathParam("collection") String collection,
			@PathParam("resource") String resource,
			@QueryParam(name="name",defaultValue = "Actor") String name, @QueryParam(name="type",defaultValue = "actor") String type, @QueryParam(name="comment",defaultValue = "") String comment) throws Exception
	{		
		return createNode(collection, resource, ACTOR_NODE_TYPE, name, type, comment);
	}
	/**
	 * Creates a new ielement node in a resource without a specified id
	 * @param collection
	 * @param resource
	 * @param name name of the ielement
	 * @param type type of the ielement
	 * @param comment optional comment
	 * @return id used for the ielement
	 * @throws Exception
	 */
	@POST
	@Path("{collection}/{resource}/ielement")
	public String createIElement(@PathParam("collection") String collection,
			@PathParam("resource") String resource,
			@QueryParam(name="name",defaultValue = "Goal") String name, @QueryParam(name="type",defaultValue = "goal") String type, @QueryParam(name="comment", defaultValue = "") String comment) throws Exception
	{		
		return createNode(collection, resource, IELEMENT_NODE_TYPE, name, type, comment);
	}
	/**
	 * Deletes a node in a resource
	 * @param collection
	 * @param resource
	 * @param nodeType actor/ielement
	 * @param nodeID id of the node to delete
	 * @return OK
	 * @throws Exception
	 */
	private String deleteNode(String collection,
			String resource, String nodeType,
			String nodeID) throws Exception
	{
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
        switch(nodeType)
        {
            case ACTOR_NODE_TYPE:
                editor.deleteActor(nodeID);
                break;
            case IELEMENT_NODE_TYPE:
                editor.deleteIElement(nodeID);
                break;
            default:
                throw new Exception("Not supported node type: " + nodeType);
        }
		
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
        return OK;
	}
	/**
	 * Deletes an actor in a resource
	 * @param collection
	 * @param resource
	 * @param nodeID id of the actor to delete
	 * @return OK
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}/{resource}/actor/{nodeID}")
	public String deleteActor(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("nodeID") String nodeID) throws Exception
	{
		return deleteNode(collection, resource, ACTOR_NODE_TYPE, nodeID);
	}
	/**
	 * Deletes an ielement of a resource
	 * @param collection
	 * @param resource
	 * @param nodeID id of the ielement to delete
	 * @return OK
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}/{resource}/ielement/{nodeID}")
	public String deleteIElement(@PathParam("collection") String collection,
			@PathParam("resource") String resource,@PathParam("nodeID") String nodeID) throws Exception
	{
		return deleteNode(collection, resource, IELEMENT_NODE_TYPE, nodeID);
	}
	/**
	 * Creates a new actorLink in a resource
	 * @param collection
	 * @param resource
	 * @param actorID id of the actor the link starts from
	 * @param targetID id of the target actor
	 * @param type type of the actorLink
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/actor/{actorID}/actorLink/{targetID}")
	public String createActorLink(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("actorID") String actorID, @PathParam("targetID") String targetID,
			@QueryParam(name="type", defaultValue = "is_a") String type) throws Exception
	{
		
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
	
		editor.createActorLink(actorID, type, targetID);
		
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
        return OK;
	}
	/**
	 * Deletes an actorLink in a resource
	 * @param collection
	 * @param resource
	  * @param actorID id of the actor the link starts from
	 * @param targetID id of the target actor
	 * @return OK
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}/{resource}/actor/{actorID}/actorLink/{targetID}")
	public String deleteActorLink(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("actorID") String actorID, @PathParam("targetID") String targetID) throws Exception
	{
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
		String result=OK;
		
		editor.deleteActorLink(actorID, targetID);
		
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
		
		
		return result;
	}
	/**
	 * Creates a new dependency link in a resource
	 * @param collection
	 * @param resource
	 * @param ielementID id of the source ielement
	 * @param dependencyType depender/dependee
	 * @param targetID id of the target actor
	 * @return OK
	 * @throws Exception
	 */
	@PUT
	@Path("{collection}/{resource}/ielement/{ielementID}/{dependencyType}/{targetID}")
	public String createDependency(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("ielementID") String ielementID, @PathParam("dependencyType") String dependencyType,
			@PathParam("targetID") String targetID) throws Exception			
	{
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
		String result=OK;
		
		if(dependencyType.equals(DEPENDER_TYPE))
		{
			editor.createDepender(ielementID, targetID);
		}
		else if(dependencyType.equals(DEPENDEE_TYPE))
		{
			editor.createDependee(ielementID, targetID);
		}
		else
			throw new Exception("Not supported dependency type: "+dependencyType);
		
		
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
		
		return result;
	}
	/**
	 * Deletes a Dependency link for a resource
	 * @param collection
	 * @param resource
	 * @param ielementID id of the source ielement
	 * @param dependencyType depender/dependee
	 * @param targetID id of the target actor
	 * @return OK
	 * @throws Exception
	 */
	@DELETE
	@Path("{collection}/{resource}/ielement/{ielementID}/{dependencyType}/{targetID}")
	public String deleteDependency(@PathParam("collection") String collection,
			@PathParam("resource") String resource, @PathParam("ielementID") String ielementID, @PathParam("dependencyType") String dependencyType,
			@PathParam("targetID") String targetID) throws Exception			
	{
		collection = getCollectionPath(collection);
		IStarMLEditor editor=getEditor(collection,resource);
		String result=OK;

        switch(dependencyType)
        {
            case DEPENDER_TYPE:
                editor.deleteDepender(ielementID, targetID);
                break;
            case DEPENDEE_TYPE:
                editor.deleteDependee(ielementID, targetID);
                break;
            default:
                throw new Exception("Not supported dependency type: " + dependencyType);
        }
		
		
		updateEnvelopeData(new ModelBuffer(editor.toString(),collection,resource));
		
		
		return result;
	}
	
	
	
	/**
	 * creates a readable text from a given XML document
	 * @param document
	 * @return readable XML
	 * @throws Exception
	 */
	private String prettyPrintXML(Document document) throws Exception
	{
		Transformer t = TransformerFactory.newInstance().newTransformer();
		StreamResult out = new StreamResult(new StringWriter());
		t.setOutputProperty(OutputKeys.INDENT, "yes"); //pretty printing
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
		t.transform(new DOMSource(document),out);
		return out.getWriter().toString();
	}

}

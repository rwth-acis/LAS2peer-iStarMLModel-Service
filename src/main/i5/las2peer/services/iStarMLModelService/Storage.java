package i5.las2peer.services.iStarMLModelService;


import i5.las2peer.services.iStarMLModelService.data.*;
import i5.las2peer.services.iStarMLModelService.mail.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Date;


import javax.xml.transform.OutputKeys;


import i5.las2peer.services.iStarMLModelService.exceptions.NotExistException;
import i5.las2peer.services.iStarMLModelService.mail.MailSender;
import org.exist.xmldb.XQueryService;
import org.exist.xmldb.XmldbURI;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
//import org.exist.xmldb.DatabaseImpl;

/**
 * Manages communication with eXist database
 * @author Alexander
 *
 */
public class Storage {
	
	private static final String DB = "/db/RootCollection/";//root collection
	private String _uri="";
	private String _driver = "org.exist.xmldb.DatabaseImpl";
	private final String DOTXML=".xml";
	private final String STARTGROUP="all";
	private String _user;
	private String _pass;
	private Collection _collection=null;
	private boolean _https=false;
	String _lastLoadedCollection;
	/**
	 * Initialization, requires login data to access DB
	 * @param uri uri pointing to the database
	 * @param user username 
	 * @param pass password
	 * @throws Exception
	 */
	public Storage(String uri, boolean https, String user, String pass) throws Exception
	{
		_uri=uri;
		_https=https;
		_user=user;
		_pass=pass;
		initDatabaseDriver();
	}
	/**
	 * Checks if a user with the given username is registered in the database
	 * @param user
	 * @return
	 */
	public boolean userExists(String user)
	{
		boolean result=false;
		try
		{
			String query="import module namespace sm=\"http://exist-db.org/xquery/securitymanager\"; \n"+
					"sm:user-exists(\""+user+"\")";
			result=execXQuery(query).equals("true");
			
		}
		catch(Exception ignored)
		{
		
		}
		return result;
	}
	/** 
	 * Registers a new user in the DB
	 * @param adminName name of the user with rights to create new users
	 * @param adminPass password
	 * @param user username to register
	 * @param pass password of the new user
	 * @param mail optional mail to send, if registration succeeds, set null, if no mail is wished
	 * @return true if creation succeeded or user already exists
	 * @throws Exception
	 */
	public boolean createUser(String adminName, String adminPass, String user, String pass, MailData mail) throws Exception
	{
		
		Collection old=_collection;//save previos context

        closeCollection();
		boolean result=false;
		try
		{

			_collection=DatabaseManager.getCollection(_uri + DB,adminName,adminPass);


			if(userExists(user))//first: is user already existing?
				return true;
			String query="import module namespace sm=\"http://exist-db.org/xquery/securitymanager\"; \n"+
					
					"if(xmldb:login(\"/db\", \""+adminName +"\", \""+adminPass+"\",false())) then\n"+
					"    sm:create-account(\""+user+"\", \""+pass+"\", \""+STARTGROUP+"\", \"\")   \n"+
					"else    \n"+
					"    false()";

			result=!execXQuery(query).equals("false");

			if(result&&mail!=null)//mail notification
			{
				MailSender.Send(mail);
			}

		}
        catch(Exception e)
        {

            e.printStackTrace();
        }
        finally
		{

			closeCollection();//'logout'
			_collection=old;

		}	
		
		
		return result;
	}
	/**
	 * Allows group write access to the newly created collection/resource
	 * also sets the group of the new element to the group of the parent collection (not the users primary group)
	 * (some genius thought it was a great idea to remove default-permission config from eXist DB...)
	 * @param collection path to the created collection
	 * @param resource path to the created resource (if collection was created, leave empty)
	 * @throws Exception
	 */
	private void setGroupPermissions(String collection,String resource) throws Exception
	{
		String uri=collection;
		if(!resource.isEmpty())
		{
			if(!resource.endsWith(DOTXML))
				resource+=DOTXML;
			
			uri+="/"+resource;
		}
		String parent="";
		int lastSlash=uri.lastIndexOf('/');
		if(lastSlash>=0)
		{
			parent=uri.substring(0,lastSlash);
		}
		/*String query="import module namespace sm=\"http://exist-db.org/xquery/securitymanager\"; \n"+
				"sm:chmod(xs:anyURI(\""+DB+uri+"\"), \"rwxrwx---\")";*/
		/*String query="import module namespace sm=\"http://exist-db.org/xquery/securitymanager\"; \n"+
				"sm:add-group-ace(xs:anyURI(\""+DB+uri+"\"), \"all\", true(), \"rwx\")";*/
		String query="xquery version \"3.0\";\n"+
				"import module namespace sm=\"http://exist-db.org/xquery/securitymanager\"; \n"+
				"let $group := sm:get-permissions(xs:anyURI(\""+DB+parent+"\"))/sm:permission/@group\n"+
				"let $uri :=xs:anyURI(\""+DB+uri+"\")\n"+
				"let $a := sm:chmod($uri, \"rwxrwx---\")\n"+
				"return sm:chgrp($uri,$group)";
		execXQuery(query);
		
	}
	/**
	 * Inits the storage
	 * @throws Exception
	 */
	private void initDatabaseDriver() throws Exception
	{




        Class aClass = Class.forName("org.exist.xmldb.DatabaseImpl");
            //System.out.println("aClass.getName() = " + aClass.getName());




         Database database =  (Database) aClass.newInstance();

         if(_https)
        	 database.setProperty("ssl-enable", "true");
         
         database.setProperty( "create-database", "true" );
         DatabaseManager.registerDatabase( database );


	}
	/**
	 * Loads a collection from the given string path
	 * @param collection path to the collection relative from the root node
	 * @return Collection retrieved from the DB
	 * @throws XMLDBException
	 */
	public Collection getCollection(String collection) throws Exception
	{
		Collection col = DatabaseManager.getCollection(_uri + DB+collection,_user,_pass);
		if(col==null)
			throw new NotExistException("Collection "+collection+ " does not exist.");
		return col;
	}
	/**
	 * Tests if a specific collection exists
	 * Collection as path 
	 * @param collection path to the eXist DB collection
	 * @return true if, collection exists
	 * @throws Exception
	 */
	public boolean existsCollection(String collection)throws Exception
	{		
		try
		{
			Collection col = getCollection(collection);		
			col.close();
			return true;
		}
		catch(NotExistException e)
		{
			return false;
		}
		
		
			
	}
	
	
	/**
	 * Selects a collection to work on
	 * @param collection 
	 * @throws Exception
	 */
	public void loadCollection(String collection)throws Exception
	{	
		closeCollection();
        _collection= getCollection(collection);
		_lastLoadedCollection=collection;
		
	}
	/**
	 * Creates a new collection, current collection changes to newly created
	 * @param collection 
	 * @throws Exception
	 */
	public void createCollection(String collection)throws Exception
	{
		if(!existsCollection(collection))
		{
			loadCollection("");
			CollectionManagementService mgtService = getCollectionManagementService();
            _collection= mgtService.createCollection((DB+collection).substring((DB).length()));
	        setGroupPermissions(collection,"");
	        closeCollection();
		}
	}
	/**
	 * Deletes a existing collection
	 * @param collection 
	 * @throws Exception
	 */
	public void deleteCollection(String collection)throws Exception
	{
		if(!existsCollection(collection))
			throw new Exception("Collection "+collection+" does not exist.");
		loadCollection("");
		CollectionManagementService mgtService = getCollectionManagementService();
        mgtService.removeCollection((DB+collection).substring((DB).length()));
        closeCollection();
        
	}
	/**
	 * Lists all sub-collections of a given collection (only top level hierarchy)
	 * @param collection
	 * @return array with name, owner etc.
	 * @throws Exception
	 */
	public CollectionData[] listSubCollections(String collection) throws Exception
	{
		loadCollection(collection);
		String[] cols=_collection.listChildCollections();
		Arrays.sort(cols); 
		CollectionData[] result=new CollectionData[cols.length];
		for (int i = 0; i < cols.length; i++) {
			String query="import module namespace xmldb=\"http://exist-db.org/xquery/xmldb\";\n"+
					"xmldb:get-owner(\""+DB+collection+"/"+cols[i]+"\")";
			String s= execXQuery(query);
			result[i]=new CollectionData(cols[i], s);
		}
		closeCollection();
		
		return result;
	}
	/**
	 * Returns the date, when a specific resource was modified the last time.
	 * @param collection
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	public Date getLastModifiedDate(String collection,String resource) throws Exception
	{
		if(!resource.toLowerCase().endsWith(DOTXML))
			resource+=DOTXML;
		
		String query="import module namespace xmldb=\"http://exist-db.org/xquery/xmldb\";\n"+
				"xmldb:last-modified(\""+DB+collection+"\",\""+resource+"\")";
		String s= execXQuery(query);
		return convertDate(s);
	}
	/**
	 * Lists all resources of a given collection
	 * @param collection
	 * @return
	 * @throws Exception
	 */
	public ResourceData[] listResources(String collection) throws Exception
	{
		loadCollection(collection);
		String[] resources=_collection.listResources();
		Arrays.sort(resources); 
		ResourceData[] result=new ResourceData[resources.length];
		for (int i = 0; i < resources.length; i++) {
			
			result[i]=new ResourceData(resources[i].substring(0,resources[i].length()-4), getLastModifiedDate(collection,resources[i]));
		}
		
		return result;
	}
	/**
	 * Gets the correct CollectionManagementService for internal use
	 * @return
	 * @throws Exception
	 */
	private CollectionManagementService getCollectionManagementService() throws Exception 
	{
		
		Collection root = DatabaseManager.getCollection(_uri+XmldbURI.ROOT_COLLECTION+"/RootCollection", _user, _pass);
        return (CollectionManagementService)root.getService("CollectionManagementService", "1.0");
	}
	
	//resource handling
	
	/**
	 * Creates a new resource in the currently loaded collection
	 * @param resourceName name of the resource, like res.xml
	 * @param resourceContent content to init the resource with
	 * @throws Exception
	 */
	/**
	 * Creates a new resource in the currently loaded collection
	 * @param resourceName name of the resource, like res.xml
	 * @param resourceContent content to init the resource with
	 * @param overwrite whether to overwrite an already existing collection
	 * @throws Exception
	 */
	public void createResource(String resourceName, String resourceContent, boolean overwrite)throws Exception
	{
		if(!resourceName.endsWith(DOTXML))
			resourceName=resourceName+DOTXML;
		if(resourceContent.trim().isEmpty())	
		{
			IStarMLEditor editor = new IStarMLEditor();
			editor.createXML();
			resourceContent=editor.toString();
		}
		
		if(_collection==null)
			throw new Exception("No collection selected");	
		if(!existsResource(resourceName))
		{
			XMLResource document = (XMLResource)_collection.createResource(resourceName, "XMLResource");
			
			document.setContent(resourceContent);	
			_collection.storeResource(document);	
			setGroupPermissions(_lastLoadedCollection, resourceName);
		}		
		else if(overwrite)
		{
			
			setResource(resourceName, resourceContent);
		}
		
	}
	/**
	 * Deletes an existing resource
	 * @param resource name of the resource to delete
	 * @throws Exception
	 */
	public void deleteResource(String resource) throws Exception
	{
		if(!resource.endsWith(DOTXML))
			resource=resource+DOTXML;
		
		if(_collection==null)
			throw new Exception("No collection selected");	
		if(existsResource(resource))
		{
			XMLResource res = (XMLResource)_collection.getResource(resource);
			_collection.removeResource(res);	
		}
	}
	/** 
	 * Checks resource for existence
	 * @param resource name of the resource
	 * @return true if resource exists
	 * @throws Exception
	 */
	public boolean existsResource(String resource)throws Exception
	{
		if(!resource.endsWith(DOTXML))
			resource=resource+DOTXML;
		XMLResource res = (XMLResource)_collection.getResource(resource);
        return res != null;
	}
	/**
	 * Retrieves a specific resource
	 * @param resource name of the resource
	 * @return xml content of the resource, null if resource not found
	 * @throws Exception
	 */
	public String getResource(String resource)throws Exception
	{		
		if(!resource.endsWith(DOTXML))
			resource=resource+DOTXML;
		
		XMLResource res = (XMLResource)_collection.getResource(resource);
		if(res == null)	
		{
			throw new NotExistException("Resource "+resource+" does not exist");
			
		}
		else 
			return res.getContent().toString();
	}
	/**
	 * Sets the content of a specific resource
	 * (existDB versioning catches the changes automatically)
	 * @param resourceName name of the resource
	 * @param resourceContent content which will replace any current content of the resource
	 * @throws Exception
	 */
	public void setResource(String resourceName, String resourceContent)throws Exception
	{
		if(!resourceName.endsWith(DOTXML))
			resourceName=resourceName+DOTXML;
		
		if(_collection==null)
			throw new Exception("No collection selected");
	
		if(existsResource(resourceName))
		{
			XMLResource document = (XMLResource)_collection.getResource(resourceName);
			document.setContent(resourceContent);		
			_collection.storeResource(document);	
			
		}
		else
		{
			throw new Exception("No such resource: "+resourceName);
		}
	}
	
	
	/**
	 * Performs an XQuery on a selected resource
	 * Important: to perform a query first a collection must be loaded
	 * @param query XQuery expression
	 * @return returns the result of the query, multiple results separated by a new line
	 * @throws Exception
	 */	
	public String execXQuery(String query) throws Exception
	{
		
		XQueryService service =(XQueryService) _collection.getService( "XQueryService", "1.0" );
		service.setProperty(OutputKeys.INDENT, "yes");
        service.setProperty(OutputKeys.ENCODING, "UTF-8");
        CompiledExpression compiled = service.compile(query);
        ResourceSet result = service.execute(compiled);//service.query(res,query);//since the queries will be simple, compilation should not bee needed
        StringBuilder sb=new StringBuilder();
        for ( int i = 0; i < (int) result.getSize(); i++ ) {
            XMLResource r = (XMLResource) result.getResource( (long) i ); 
            sb.append(r.getContent().toString()).append("\n");
        }
        return sb.toString().trim();
	}
	/**
	 * Closes the current resource
	 * @throws Exception
	 */
	public void closeCollection() throws Exception
	{	 
         if(_collection != null) 
              _collection.close();
	}
	/**
	 * Gets version data of a resource (Revision,Date,User)
	 * @param resourceName name of the resource
	 * @return Array with (Revision,Date,User) information
	 * @throws Exception
	 */
	public ResourceVersion[] getVersions(String resourceName) throws Exception
	{
		if(!resourceName.endsWith(DOTXML))
			resourceName=resourceName+DOTXML;
		
		String query="import module namespace v=\"http://exist-db.org/versioning\";\n"+					
				    "for $rev in v:history(doc(\""+resourceName+"\"))//v:revision\n"+
				    "return\n" +
				    "concat(data($rev/@rev),\"&#10;\",data($rev//v:date),\"&#10;\",data($rev//v:user))";		
		String result= this.execXQuery(query);
		
		if(result.trim().isEmpty())//no versions
			return new ResourceVersion[0];
		
		String[] s= result.split("\n");
		ArrayList<ResourceVersion> list= new ArrayList<>();
		int rev=0;
		String user;
		Date date=null;

		//SimpleDateFormat inputDateFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX");	
		for(int i=0;i<s.length;i++)
		{			
			
			switch(i%3)
			{
				case 0: rev=Integer.parseInt(s[i]); 
					break;
				case 1: date = convertDate(s[i]);		
					break;
				case 2: user=s[i];				
					list.add(new ResourceVersion(rev, date, user));
					break;
			}			
		}
		return list.toArray(new ResourceVersion[list.size()]);
	}
	private SimpleDateFormat _inputDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX");
	private Date convertDate(String d) throws ParseException
	{
		d=d.replace('T', ' ');
		if(d.indexOf('.')<=-1)//Bug when ms = 000 then .000 suffix omitted -> FormatException
			d=d.replace("+", ".000+");
			
		return _inputDateFormat.parse(d);	
		
	}
	/**
	 * Gets a specific revision of a resource
	 * @param resourceName name of the resource
	 * @param revision version number
	 * @return xml content of the specific resource revision
	 * @throws Exception
	 */
	public String getVersion(String resourceName, int revision) throws Exception
	{
		if(!resourceName.endsWith(DOTXML))
			resourceName=resourceName+DOTXML;
		
		String query="import module namespace v=\"http://exist-db.org/versioning\";\n"+					
			    "v:doc(doc(\""+resourceName+"\"),"+revision+")\n";			   		
		return this.execXQuery(query);	
	}
	/**
	 * Shows the diff of a resource an one of its revisions
	 * @param resourceName name of the resource
	 * @param revision version number
	 * @return diff as XML in eXistDB format
	 * @throws Exception
	 */
	public String getVersionDiff(String resourceName, int revision) throws Exception
	{
		if(!resourceName.endsWith(DOTXML))
			resourceName=resourceName+DOTXML;
		String query="import module namespace v=\"http://exist-db.org/versioning\";\n"+					
			    "v:diff(doc(\""+resourceName+"\"),"+revision+")\n";
			   		
		return this.execXQuery(query);
	
	}
	/**
	 * Searches the storage for a specific node type or resource
	 * @param collection root element to search from
	 * @param query search query (name of the node or name of the resource)
	 * @param type search type ('model' if to search for a resource, otherwise the specific node type)
	 * @return returns the found resources as a path
	 * @throws Exception
	 */
	public String[] searchStorage(String collection, String query, String type) throws Exception {
		
		String xquery;
		if(type.trim().isEmpty())
			return new String[]{};
		else if(type.equals("model"))//search for a resource
		{
		
			xquery="for $doc in collection(\""+DB+collection+"\") where \n"+
					"contains(lower-case(document-uri($doc)), \""+query.toLowerCase()+"\")\n"+ 
        			"return document-uri($doc)";
			
		}
		else //otherwise search for a node
		{
			String nodeType="ielement";
			
			if(type.equals("actor")||type.equals("agent")||type.equals("role")||type.equals("position"))
				nodeType="actor";
			
			xquery="for $doc in collection(\""+DB+collection+"\") where\n"+
				    "$doc//"+nodeType+"[@type=\""+type+"\" and contains(lower-case(@name),\""+query.toLowerCase()+"\")]\n"+
				    "return document-uri($doc)";
		}
			
		String result=this.execXQuery(xquery);
		if(result.trim().length()>0)
		{
			String[] elements= result.split("\n");
			for (int i = 0; i < elements.length; i++) {
				
				elements[i]=elements[i].substring(DB.length(),elements[i].length()-4);//remove .xml suffix and the root collection path
			}
			return elements;
		}
		else
			return new String[]{};
	}
}

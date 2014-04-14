package i5.las2peer.services.iStarMLModelService;

import static org.junit.Assert.*;

import i5.las2peer.services.iStarMLModelService.Storage;
import i5.las2peer.services.iStarMLModelService.data.*;
import i5.las2peer.services.iStarMLModelService.mail.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;


import org.junit.Ignore;
import org.junit.Test;

public class StorageTest {

	protected static String URI =  "xmldb:exist://localhost:8080/exist/xmlrpc";
	private static String _basePath="./testComparisonXMLFiles/";
	
	public static String getFile(String path)throws Exception 
	{
		   String content = null;
		   File file = new File(_basePath+path+".xml"); //for ex foo.txt
		   FileReader reader = null;
		   try {
		       reader = new FileReader(file);
		       char[] chars = new char[(int) file.length()];
		       reader.read(chars);
		       content = new String(chars);
		       reader.close();
		   } catch (IOException e) {
		       e.printStackTrace();
		   }finally {
			   reader.close();
		   }
		   
		   return content;
	}

	@Test
    @Ignore
	public void testStorage1() throws Exception 
	{
		Storage storage=new Storage(URI, false, "hansi","kannsi");
		
		storage.createCollection("CollectionTest/col1");
		storage.createCollection("CollectionTest/col1");
		storage.createCollection("CollectionTest/col2");
		storage.createCollection("CollectionTest/col3");
		storage.loadCollection("CollectionTest");
		CollectionData[] cols= storage.listSubCollections("CollectionTest");
		for (int i = 0; i < cols.length; i++) {
			System.out.println(cols[i].getName()+ " "+ cols[i].getOwner());
			
		}
		ResourceData[] res= storage.listResources("CollectionTest");
		for (int i = 0; i < res.length; i++) {
			System.out.println(res[i].getName()+ " " + res[i].getLastModified().toString());
		}
		
		
		
		
		/*IStarMLModelService service = new IStarMLModelService();
		String s=service.restDecoder("get", "Collection.collection1", new String[][]{{"search","re"}}, "");
		System.out.println(s);*/
		
	}
	@Test
    @Ignore
	public void testMail() throws Exception
	{
		
		//MailSender.Send(new MailData("sumpfkrautjunkie@googlemail.com", "smtp.gmail.com", 587, "istarmlnotifier@googlemail.com", "ich sitze auf einem baum und lache", "Essen ist toll2", "Quark2!"));
	}
	@Test
    @Ignore
	public void testStorage() {
		try {
			@SuppressWarnings("unused")
			Storage storage=new Storage(URI, false, "simpleTestUser1","simpleTestUser1");
		} catch (Exception e) {
			fail("Exception: "+e.getMessage());
		}
	}
	@Test
    @Ignore
	public void testMethods1() throws Exception {
		Storage st=new Storage(URI, false, "hansi","kannsi");
		st.createCollection("Collection");
		st.createResource("test", "",true);
		st.loadCollection("Collection");
		String s=st.getResource("test.xml");
		System.out.println(s);
	
	}
	@Test
    @Ignore
	public void testUserCreation() throws Exception
	{
		//try {
			MailData data= new MailData("sumpfkrautjunkie@googlemail.com", "smtp.gmail.com", 587, "istarmlnotifier@googlemail.com", "ich sitze auf einem baum und lache", "", "");
			data.setSubject("New User "+"hansi"+"Created!");
			data.setText("hansi"+"requests access NSA-Secret Collection.");
			Storage storage=new Storage(URI, false,"hansi","kannsi");
			storage.createUser("UserCreator", "MkIkAePkCeBe", "hansi", "kannsi",data);
			storage.loadCollection("Collection");
			
		/*} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}*/
		
		
	}
	@Test
    @Ignore
	public void testMethods() {
		try {
			String col1="CollectionTest/collection2";
			String res1="res7.xml";
			Storage storage=new Storage(URI,false,"hansi","kannsi");
			storage.createCollection(col1);
			if(!storage.existsCollection(col1))
			{
				fail("collection "+col1+" not created");
			}
			storage.loadCollection(col1);
			storage.deleteResource(res1);
			storage.createResource(res1,getFile("storage1"),true);
			
			//System.out.println(storage.getResource(res1));
			storage.setResource(res1, getFile("storage1"));
			
			
			if(!storage.existsResource(res1))
			{
				fail("resource "+res1+" not created");
			}
			
			String res=storage.getResource(res1);
			
			//db stores only \n but actually it does not matter
			assertEquals("Loading not as expected",getFile("storage1").replace("\r\n", "\n"),res);
			storage.setResource(res1,getFile("storage2"));
			
			res=storage.getResource(res1);
			assertEquals("Updating not as expected",getFile("storage2").replace("\r\n", "\n"),res);
			
			ResourceVersion[] versions=storage.getVersions(res1);
			int oldest=999999;
			for (int i = 0; i < versions.length; i++) {
				
				if(versions[i].getRevision()<oldest)
				{
					oldest=versions[i].getRevision();
					
				}
			}
			res=storage.getVersion(res1, oldest);//should be storage1
			assertEquals("Loading version not as expected",getFile("storage1").replace("\r\n", "\n"),res);
			
			storage.deleteResource(res1);
			if(storage.existsResource(res1))
			{
				fail("resource "+res1+" not deleted");
			}
			
			storage.deleteCollection(col1);
			if(storage.existsCollection(col1))
			{
				fail("collection "+col1+" not deleted");
			}
			storage.closeCollection();
			
		} catch (Exception e) {
			fail("Exception: "+e.getMessage());
		}
	}

	

}

package i5.las2peer.services.iStarMLModelService;

import static org.junit.Assert.*;


import i5.las2peer.services.iStarMLModelService.IStarMLEditor;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IStarMLEditorTest {

	
	private static IStarMLEditor _editor;
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
	@BeforeClass
	public static void testSetup() {		
   	 	
	}

	@AfterClass
	public static void testCleanup() 
	{
		
	}
	
	@Before
	public void prepareTestMethod()
	{
		try 
		{
			_editor=new IStarMLEditor();
		}
		catch (Exception e) 
		{
			fail("Initialization failed");
		}
	}
	@After 
	public void cleanupTestMethod()
	{
		_editor=null;
	}
	@Test
	public void testIStarMLEditor() {
		try
		{
			@SuppressWarnings("unused")
			IStarMLEditor editor = new IStarMLEditor();
			
		}
		catch(Exception e)
		{
			fail("IStarMLEditor object could not be created: "+ e.getMessage());
		}		
	}

	@Test
	public void testCreateXML() {
		try {
			_editor.createXML();
			@SuppressWarnings("unused")
			String xml=_editor.toString();
				
			
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		
	}

	@Test
	public void testLoadXML() {
		try {			
			_editor.loadXML(getFile("loadXML1"));
			assertEquals("Loading content mismatch",getFile("loadXML1"),_editor.toString().replaceAll("\r",""));
			
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		
	}
	@Test
	public void testCreationSpeed() throws Exception {
		long start= System.nanoTime();
		_editor.createXML();
		for(int i=0;i<1000;i++)
		{
			_editor.createActor(Integer.toString(i), "Medium", "actor","");
		}
		long end= System.nanoTime();
		
		System.out.println((end-start)/1000000);
	}
	@Test
	public void testCreation1() {
		//fist some element creation
		try {			
			_editor.createXML();
        	_editor.createActor("1", "Medium", "actor","");
        	_editor.createActor("2", "Group", "actor","");
        	_editor.createActor("3", "Teacher", "agent","");
        	_editor.createIElement("4", "Ask", "goal","");
        	_editor.createIElement("5", "Check", "task","");
        	_editor.createActorLink("1", "is_a", "2");
        	_editor.createActorLink("2", "is_part_of", "3");
        	_editor.createDepender("4", "1");
        	_editor.createDepender("4", "2");
        	_editor.createDependee("4", "3"); 
			assertEquals("Generating content mismatch",getFile("creation1"),_editor.toString().replaceAll("\r",""));
			
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		
		/*//check if duplicate edges cause an exception
		try {
			_editor.createDepender("4", "2");
			_editor.createActorLink("1", "is_a", "2");
			fail("Edge with duplicate ID created");
			
		} catch (Exception e) {
			
		}*/
		//check if dependecy can be added to an actor
		try {
			_editor.createDepender("1", "3");			
			fail("Depender added to actor");
			
		} catch (Exception e) {
			
		}
		//check if actorLink can be added to an ielement
		try {
			_editor.createActorLink("4", "is_a", "2");		
			fail("ActorLink added to ielement");
			
		} catch (Exception e) {
			
		}
		
	}
	
	@Test
	public void testCreation2() throws Exception {
		
		try {
			_editor.loadXML(getFile("creation1"));
			//_editor.createActorLink("2", "instance_of", "1");
			_editor.createActor("6", "Teacher", "agent","");
			_editor.createIElement("7", "Ask", "goal","");
			_editor.createActorLink("6", "is_a", "1");
			_editor.createDepender("7", "3");
			assertEquals("Generating2 content mismatch",getFile("creation2"),_editor.toString().replaceAll("\r",""));
			
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		
		try {
			_editor.deleteActorLink("2", "3");
			_editor.deleteDependee("4", "3");
			_editor.deleteDepender("4", "1");
			_editor.createActorLink("2", "instance_of", "1");
			assertEquals("Generating2_2 content mismatch",getFile("creation2_2"),_editor.toString().replaceAll("\r",""));
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		//insert back. caveat: order of nodes might be different, thus producing a different string (creation2_3)
		try {			
			//_editor.createActorLink("2", "is_part_of", "3");
			_editor.createDepender("4", "1");        	
        	_editor.createDependee("4", "3"); 
			assertEquals("Generating2_3 content mismatch",getFile("creation2_3"),_editor.toString().replaceAll("\r",""));
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		try {
			//_editor.deleteActor("2");			
			//System.out.println(_editor.toString());
			_editor.deleteIElement("4");	
			assertEquals("Generating2_4 content mismatch",getFile("creation2_4"),_editor.toString().replaceAll("\r",""));
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		//node deleted, check if links with old id/aref can now be inserted (cleanup of hashset)
		try {
			
			//_editor.createActor("2", "Teacher", "agent","");
			//_editor.createIElement("4", "Ask", "goal","");	
			//_editor.createDepender("4", "1");      
			//_editor.createActorLink("2", "is_part_of", "3");
			
		} catch (Exception e) {
			fail("Edges could not be created although no id and aref duplicates. "+e.getMessage().replaceAll("\r",""));
		}
		
	}
	
	@Test
	public void testCreation3() throws Exception {
		
		//editTest
		try {
			_editor.loadXML(getFile("creation1"));
			_editor.editActor("1", "Teacher","agent");
			_editor.editIElement("4", "Ask", "goal");
			_editor.editActorLink("1", "2", "is_part_of");
			_editor.deleteDepender("4", "2");
			_editor.editDepender("4", "1", "2");
			_editor.editDependee("4", "3", "1");
			assertEquals("Edit content mismatch",getFile("creation3"),_editor.toString().replaceAll("\r",""));
		} catch (Exception e) {
			fail("Exception: "+ e.getMessage());
		}
		
		
	}
}
	
	

	



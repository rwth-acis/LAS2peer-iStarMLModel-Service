/*package i5.las2peer.services.iStarMLModelService;

import static org.junit.Assert.*;

import i5.las2peer.execution.NoSuchServiceMethodException;

import i5.las2peer.httpConnector.client.AccessDeniedException;
import i5.las2peer.httpConnector.client.AuthenticationFailedException;
//import i5.las2peer.httpConnector.client.Client;
import i5.las2peer.httpConnector.client.ConnectorClientException;
import i5.las2peer.httpConnector.client.NotFoundException;
import i5.las2peer.httpConnector.client.ServerErrorException;
import i5.las2peer.httpConnector.client.TimeoutException;
import i5.las2peer.httpConnector.client.UnableToConnectException;
import i5.las2peer.p2p.LocalNode;

import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
//import i5.las2peer.webConnector.WebConnector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.lang.reflect.InvocationTargetException;

import org.junit.After;

import org.junit.Before;

import org.junit.Ignore;
import org.junit.Test;



public class IStarMLModelServiceTest {

	private static final String FAIL = "fail";
	static IStarMLModelService service;
	static String OK;
	private static String _basePath="./testComparisonXMLFiles/";
	
	
	//private static final String HTTP_ADDRESS = "localhost";
	//private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_CONNECTOR_PORT;

	private LocalNode node;
	//private WebConnector connector;
	private ByteArrayOutputStream logStream;
	private UserAgent adam = null;
	//private Client client=null;
	//private static final String testPass = "adamspass";
	private ServiceAgent testService;
	private static final String testServiceClass = "i5.las2peer.services.iStarMLModelService.IStarMLModelService";
	
	public static String getFile(String path)throws Exception 
	{		   
		   File file = new File(path); //for ex foo.txt
		  
		    FileInputStream fis = new FileInputStream(file);
		    byte[] data = new byte[(int)file.length()];
		    fis.read(data);
		    fis.close();
		    //
		    String s = new String(data, "UTF-8");
		   return s;
	}
	private void sendCommand(String method,String URI, String[][] variables, String content, String expected) throws UnableToConnectException, AuthenticationFailedException, TimeoutException, ServerErrorException, AccessDeniedException, NotFoundException, ConnectorClientException, NoSuchServiceMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, L2pSecurityException
	{
		//String result = (String)client.invoke(testServiceClass, "restDecoder",method,URI,variables,content );
		String result=(String)testService.invoke( "restDecoder",new Object[]{method,URI,variables,content} );
		System.out.println(1);
		//String result=service.restDecoder(method, URI, variables, content);
		String vars="";
		for (int i = 0; i < variables.length; i++) {
			for (int j = 0; j < variables[i].length; j++) {
				vars+=variables[i][j]+" ";
			}
			vars+="|";
		}
		result=result.replace("\r\n", "");
		result=result.replace("\n", "");
		
		if(expected.equals(FAIL))
		{
			if(!result.startsWith("Error"))
				fail(method+" "+URI+ " "+ vars+ "threw no exception: "+result);
		}
		else if(!expected.trim().isEmpty())
			assertEquals(method+" "+URI+ " "+ vars+ "Wrong result", expected,result);
		else 
		{
			if(result.startsWith("Error"))
				fail(method+" "+URI+ " "+ vars+ "Error result: "+result);
		}
	}
	@Before
	public void testSetup() throws Exception
	{		
   	 	
   	 	// start Node
   			node = LocalNode.newNode();
   			adam=MockAgentFactory.getAdam();
   			
   			node.storeAgent(adam);
   			node.launch();

   			testService = ServiceAgent.generateNewAgent(
   					testServiceClass, "a pass");
   			testService.unlockPrivateKey("a pass");

   			
   			node.registerReceiver(testService);
   			// start connector
   			logStream = new ByteArrayOutputStream();
   			
   	 		OK=IStarMLModelService.OK;
   	 	
	}
	@After
	public void shutDownServer() throws Exception {
		
		node.shutDown();

		
		node = null;

		LocalNode.reset();

		System.out.println("Connector-Log:");
		System.out.println("--------------");

		System.out.println(logStream.toString());
	}
	
	@Test
	public void test1() throws Exception
	{		
		sendCommand("get", "", new String[][]{}, "",OK);
	}
	
	@Test
	@Ignore //not executed in a l2p thread ???
	public void test2() throws Exception
	{		
		
		sendCommand("put", "testCollection2.col1", new String[][]{}, "",OK);
		sendCommand("put", "testCollection2.col1/res1", new String[][]{}, "",OK);
		sendCommand("put", "testCollection2.col1/res2", new String[][]{}, "",OK);
		sendCommand("put", "testCollection2.col1/res3", new String[][]{}, "",OK);
		
		String expected="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><ModelResponse>  <Collections>    <Collection name=\"col1\" owner=\"guest\"/>  </Collections>  <Resources/></ModelResponse>";
		sendCommand("get", "testCollection2", new String[][]{}, "",expected);
		expected="<istarml>    <diagram name=\"diagram\"/></istarml>";
		sendCommand("get", "testCollection2.col1/res1", new String[][]{}, "",expected);
		
		sendCommand("get", "testCollection2.col1", new String[][]{{"search","res"}}, "","");
		
		sendCommand("get", "testCollection2.col1/res3/versions", new String[][]{}, "","");
		expected="<istarml>    <diagram name=\"diagram\"/>    <actor id=\"1\" type=\"actor\" name=\"group\"/></istarml>";
		sendCommand("post", "testCollection2.col1/res1", new String[][]{}, expected,OK);
		sendCommand("get", "testCollection2.col1/res1", new String[][]{}, "",expected);
		
		
		sendCommand("delete", "testCollection2.col1/res3", new String[][]{}, "",OK);
		sendCommand("get", "testCollection2.col1/res3", new String[][]{}, "",FAIL);
		
		sendCommand("delete", "testCollection2.col1", new String[][]{}, "",OK);		
		sendCommand("get", "testCollection2.col1", new String[][]{}, "",FAIL);
		sendCommand("delete", "testCollection2.col1", new String[][]{}, "",OK);
	}
	
	@Test
	@Ignore
	public void test3() throws Exception 
	{
		sendCommand("put", "testCollection1.col2", new String[][]{}, "",OK);
		sendCommand("put", "testCollection1.col2/res1", new String[][]{}, "",OK);
		
		sendCommand("put", "testCollection1.col2/res1/actor/1", new String[][]{{"name","Medium"},{"type","actor"}}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/actor/2", new String[][]{{"name","Group"},{"type","actor"}}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/actor/3", new String[][]{{"name","Teacher"},{"type","agent"}}, "",OK);
		
		sendCommand("put", "testCollection1.col2/res1/ielement/4", new String[][]{{"name","Ask"},{"type","goal"}}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/ielement/5", new String[][]{{"name","Check"},{"type","task"}}, "",OK);
		
		sendCommand("put", "testCollection1.col2/res1/actor/1/actorLink/2", new String[][]{{"type","is_a"}}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/actor/2/actorLink/3", new String[][]{{"type","is_part_of"}}, "",OK);
		
		sendCommand("put", "testCollection1.col2/res1/ielement/4/depender/1", new String[][]{}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/ielement/4/depender/2", new String[][]{}, "",OK);
		sendCommand("put", "testCollection1.col2/res1/ielement/4/dependee/3", new String[][]{}, "",OK);
		
		sendCommand("get", "testCollection1.col2/res1", new String[][]{}, "",getFile(_basePath+"restComparison1.xml").replace("\r\n", ""));
		
		
		sendCommand("delete", "testCollection1.col2/res1/ielement/4/depender/2", new String[][]{}, "",OK);
		sendCommand("delete", "testCollection1.col2/res1/ielement/5", new String[][]{}, "",OK);		
		sendCommand("delete", "testCollection1.col2/res1/actor/2/actorLink/3", new String[][]{}, "",OK);		
		sendCommand("delete", "testCollection1.col2/res1/actor/3", new String[][]{}, "",OK);
		
		sendCommand("get", "testCollection1.col2/res1", new String[][]{}, "",getFile(_basePath+"restComparison2.xml").replace("\r\n", ""));
		sendCommand("delete", "testCollection1.col2", new String[][]{}, "",OK);		
	}
	

}*/

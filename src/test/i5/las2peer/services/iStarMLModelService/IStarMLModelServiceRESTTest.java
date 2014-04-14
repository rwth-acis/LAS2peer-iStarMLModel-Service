package i5.las2peer.services.iStarMLModelService;

import static org.junit.Assert.*;

import i5.las2peer.api.Connector;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;

import i5.las2peer.webConnector.client.MiniClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
/**
 * @author Alexander
 */
public class IStarMLModelServiceRESTTest
{
    private static final String HTTP_ADDRESS = "http://127.0.0.1";
    private static final int HTTP_PORT = 8081;

    private static LocalNode node;
    private static WebConnector connector;
    private static ByteArrayOutputStream logStream;

    private static UserAgent testAgent;
    private static final String testPass = "adamspass";

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

    @Before
    public void testSetup() throws Exception
    {

        // start Node
        node = LocalNode.newNode();
        node.storeAgent(MockAgentFactory.getEve());
        node.storeAgent(MockAgentFactory.getAdam());
        node.storeAgent(MockAgentFactory.getAbel());
        node.storeAgent( MockAgentFactory.getGroup1());
        node.launch();

        ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");

        testService.unlockPrivateKey("a pass");


        node.registerReceiver(testService);


        // start connector

        logStream = new ByteArrayOutputStream ();

        //String xml=RESTMapper.mergeXMLs(new String[]{RESTMapper.getMethodsAsXML(TestService.class),RESTMapper.getMethodsAsXML(TestService2.class)});
        //System.out.println(xml);
        /*System.out.println(RESTMapper.getMethodsAsXML(TestService.class));
        System.out.println(RESTMapper.getMethodsAsXML(TestService2.class));*/

        connector = new WebConnector(true,HTTP_PORT,false,1000);
        connector.setSocketTimeout(5000);
        connector.setLogStream(new PrintStream ( logStream));
        connector.start ( node );

        // eve is the anonymous agent!
        testAgent = MockAgentFactory.getAdam();

    }

    @AfterClass
    public static void shutDownServer () throws Exception {
        //connector.interrupt();

        connector.stop();
        node.shutDown();

        connector = null;
        node = null;

        LocalNode.reset();

        System.out.println("Connector-Log:");
        System.out.println("--------------");

        System.out.println(logStream.toString());
        //System.out.println(connector.sslKeystore);


    }
    @Test
    public void test1() throws Exception
    {


        connector.updateServiceList();
        Thread.sleep(2000);
        MiniClient c = new MiniClient();
        c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
        c.setLogin(Long.toString(testAgent.getId()), testPass);
        try
        {

            ClientResponse result=c.sendRequest("get", "IStarMLModelService", "");
            assertEquals("ok",result.getResponse().trim());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail ( "Exception: " + e );
        }

        try
        {
           /* ClientResponse result=c.sendRequest("post", "IStarMLModelService/setting/register/DB", "");
            assertEquals("ok",result.getResponse().trim());

            result=c.sendRequest("put", "IStarMLModelService/collection3", "");
            assertEquals("ok",result.getResponse().trim());

            result=c.sendRequest("put", "IStarMLModelService/collection3/model1", "");
            assertEquals("ok",result.getResponse().trim());


            result=c.sendRequest("put", "IStarMLModelService/collection3/model1/actor/5?name=Hans", "");
            assertEquals("ok",result.getResponse().trim());*/

            //result=c.sendRequest("get", "IStarMLModelService/collection3/model1", "");
           // assertEquals("ok",result.getResponse().trim());
           /* Class aClass = Class.forName("org.exist.xmldb.DatabaseImpl");
            //System.out.println("aClass.getName() = " + aClass.getName());


            String user="UserCreator";
            String pass="hakunamatata";

            Database database =  (Database) aClass.newInstance();



            database.setProperty( "create-database", "true" );
            DatabaseManager.registerDatabase( database );
            DatabaseManager.getCollection("xmldb:exist://localhost:8080/exist/xmlrpc" + "/db/RootCollection/",user,pass);*/
            /*String service1="org.xmldb.api.DatabaseManager";
            Class aClass = Class.forName(service1);
            assertEquals(service1,aClass.getName().trim());*/


            IStarMLModelService serv= new IStarMLModelService();
            String user="adam";
            String pass="klCzfeVgXGfjjNZkaDS98552ReHI3EQTuIWdF+f6sao=";
            String admin="UserCreator";
            String adminpass="hakunamatata";
            String existUri="xmldb:exist://localhost:8080/exist/xmlrpc";

            Storage storage= new Storage(existUri,false, user, pass);
            storage.createCollection("collection2");

            storage.closeCollection();










        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail ( "Exception: " + e );
        }
    }
}



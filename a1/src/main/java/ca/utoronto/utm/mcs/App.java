package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import com.sun.net.httpserver.HttpServer;

public class App implements AutoCloseable
{
	private static Driver DRIVER;
	private static final String URI = "bolt://localhost:7687";
	private static final String USER = "neo4j";
	private static final String PASS = "password";
	public static String BACON_ID = "";
	
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        DRIVER = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASS));
        HttpHandlers.hookUpDriver(DRIVER);
        
        Actors actors = new Actors();
        server.createContext("/api/v1/addActor", actors);
        server.createContext("/api/v1/getActor", actors);
        
        Movies movies = new Movies();
        server.createContext("/api/v1/addMovie", movies);
        server.createContext("/api/v1/getMovie", movies);
        
        Relationships relationships = new Relationships();
          
        server.createContext("/api/v1/addRelationship", relationships);
        server.createContext("/api/v1/hasRelationship", relationships);
        server.createContext("/api/v1/computeBaconNumber", new ComputeBaconNumber());
        server.createContext("/api/v1/computeBaconPath", new ComputeBaconPath());
        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
        
        
    }

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
}

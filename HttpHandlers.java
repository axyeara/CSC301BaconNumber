package ca.utoronto.utm.mcs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class HttpHandlers implements HttpHandler{
	protected static Driver driver;

	@Override
    public void handle(HttpExchange r) throws IOException{

        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("POST")) {
                handlePost(r);
            }
        } catch (IOException e) {
    			writeResponse(r, 500, null);   		
            e.printStackTrace();
        } catch (JSONException e) {
        		writeResponse(r, 400, null);
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
        		writeResponse(r,405, null);
        		e.printStackTrace();
        }
    }
	protected static void checkHttpVerb(HttpExchange r,
			String path) throws JSONException, UnsupportedOperationException{
		
		if (!r.getRequestURI().getPath().startsWith(path)) {
			throw new UnsupportedOperationException("Error: invalid http verb");
		}
	}
	
    protected abstract void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException;

    protected abstract void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException;
    
    public static void hookUpDriver(Driver driver) {
    		HttpHandlers.driver = driver;
        try (Session session = driver.session()) {
			StatementResult kevinBaconResult = session.run(
				"MATCH (a:actor {name:{name}}) "+
				"RETURN a.id AS id",parameters("name","Kevin Bacon"));
			
			try {
				Record kevinBaconRecord = kevinBaconResult.single();
				App.BACON_ID = kevinBaconRecord.get("id").asString();
			} catch (NoSuchRecordException e) {}
            
       }
    }
    
    public static String JSONToString(JSONObject jo) {
    		//fix json formatting
    		return jo.toString().replace("\\\"","");
    }
    
    public static void writeResponse(HttpExchange r, int status, byte[] b) throws IOException {
    		if (b==null){
    			r.sendResponseHeaders(status, -1);
    			//write a no response body
    			return;
    		}
    		r.sendResponseHeaders(status, b.length);
        OutputStream os = r.getResponseBody();
        os.write(b);
        os.close();
    }
    

}

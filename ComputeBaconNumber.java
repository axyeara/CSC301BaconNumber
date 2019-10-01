package ca.utoronto.utm.mcs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;

public class ComputeBaconNumber extends HttpHandlers{

	@Override
	public void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		if (App.BACON_ID == "") {
			writeResponse(r,400,null);
			return;
		}
		JSONObject response = new JSONObject();
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String id = deserialized.getString("actorId");
        
        if (id.equals(App.BACON_ID)) {
        		response.put("baconNumber", 0);
			writeResponse(r,200,JSONToString(response).getBytes());
			return;
        }
        try (Session session = driver.session()) {
        		int pathLength;
        		
        		try { 
        			pathLength = getPathLength(session, id);
            		String baconNumber = Integer.toString(getBaconNumber(pathLength));
            		response.put("baconNumber", baconNumber);
            		writeResponse(r,200,JSONToString(response).getBytes());
            		
        		} catch (NoSuchRecordException e) {
        			writeResponse(r,404,null);
        		}
        }
	}
	
	public static int getBaconNumber(int pathLength) {
		return (int)(pathLength/2);
		/*
		pathLength including the starting actor
		should be odd, as for every new intermediate actor
		you add, you also add a movie node, so we have 2n+1.
		but the path length returned by neo4j does not
		include the starting actor, so just divide by 2
		 */
	}
	
	public static int getPathLength(Session s, String id) throws NoSuchRecordException{
		StatementResult shortestPath = s.run(
    		"MATCH p=shortestPath((a1:actor {id:{id_other}})"
    		+"-[:ACTED_IN*]-(kb:actor {id:{id_bacon}})) RETURN length(p) "
    		+"AS baconNumber",parameters("id_other",id,"id_bacon",App.BACON_ID));
    		
		Record baconNumberRecord = shortestPath.single();
		// this may throw NoSuchRecordException, but it will be caught
		// and handled appropriately by the calling function
		
		return baconNumberRecord.get("baconNumber").asInt();
	}

	@Override
	public void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Error: invalid http verb");
	}
}

package ca.utoronto.utm.mcs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;

public class ComputeBaconPath extends HttpHandlers{

	private static final ComputeBaconNumber ComputeBaconNumber = null;

	@Override
	public void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		if (App.BACON_ID == "") {
			writeResponse(r,400,null);
			return;
		}
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
		JSONObject json = new JSONObject();

        String id = deserialized.getString("actorId");
        
        if (id.equals(App.BACON_ID)) {
        		// doing shortestPath(...) from a node to itself
        		// raises a critical error in neo4j
        		json.put("baconNumber", 0);
        		json.put("baconPath", new JSONArray());
        		writeResponse(r,200,JSONToString(json).getBytes());
        		return;
        }
        
        try (Session session = driver.session()) {
        	
        		StatementResult shortestPath = session.run(
    	        		"MATCH p=shortestPath((a1:actor {id:{id_other}})"
    	        		+"-[:ACTED_IN*]-(kb:actor {id:{id_bacon}})) RETURN nodes(p) "
    	        		+"AS path",parameters("id_other",id,"id_bacon",App.BACON_ID));
        		
        		try {
        			Record baconPathRecord = shortestPath.single();
        			// this may throw NoSuchRecordException
        			
        			int pathLength = ComputeBaconNumber.getPathLength(session, id);
        			int baconNumber = ComputeBaconNumber.getBaconNumber(pathLength);

        			json.put("baconNumber",baconNumber);
        			
                JSONObject[] baconPath = new JSONObject[baconNumber];
        			for (int i=0;i<baconNumber;i++) {
        				int j = i*2+1;
        				JSONObject node = new JSONObject();
        				node.put("actorId",baconPathRecord.get("path").get(j+1).get("id").asString());
        				node.put("movieId",baconPathRecord.get("path").get(j).get("id").asString());
        				baconPath[i] = node;
        			}
        			json.put("baconPath", baconPath);
        			writeResponse(r,200,JSONToString(json).getBytes());
                
        		} catch (NoSuchRecordException e) {
        			writeResponse(r,404,null);
        		}
        }
	}

	@Override
	public void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Error: invalid http verb");
	}
}

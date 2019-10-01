package ca.utoronto.utm.mcs;
import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.util.List;

import org.json.*;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;

public class Actors extends HttpHandlers{

	@Override
	public void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		checkHttpVerb(r,"/api/v1/getActor");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String id = deserialized.getString("actorId");
        
        try (Session session = driver.session()) {

            StatementResult actorSameId = session.run("MATCH (a:actor {id:{id}}) "
            		+"RETURN a.name AS name",parameters("id",id));
            try {
            		Record actor = actorSameId.single();
            		// this is the line that may throw NoSuchRecordException
            		
                JSONObject response = new JSONObject();
                response.put("name", actor.get("name"));
                response.put("id", id);
                
                StatementResult actedIn = session.run("MATCH (:actor {id:{id}})"
                		+"-[:ACTED_IN]->(m:movie) "
                		+"RETURN m.id AS id",parameters("id",id));
                
                List<Record> movieRecords = actedIn.list();
                String[] movies = new String[movieRecords.size()];
                
                for (int i=0; i<movieRecords.size(); i++) {
                		Record movieRecord = movieRecords.get(i);
                		movies[i] = movieRecord.get("id").asString();
                }
                response.put("movies", new JSONArray(movies));
                writeResponse(r,200,JSONToString(response).getBytes());

            } catch (NoSuchRecordException e) {
            		writeResponse(r,404,null);
            }
        }
		
	}

	@Override
	public void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException{
		checkHttpVerb(r,"/api/v1/addActor");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        
        String id = deserialized.getString("actorId");
        String name = deserialized.getString("name");
        if (name.equals("Kevin Bacon")) {
        		App.BACON_ID = id;
        }
        
        try (Session session = driver.session()) {

            StatementResult actorSameId = session.run("MATCH (a:actor {id:{id}}) "
            		+"RETURN a",parameters("id",id));
            if (!actorSameId.hasNext()) {
            		// if an actor with this id has not been added yet
                try (Transaction tx = session.beginTransaction()) {
                    tx.run("CREATE (a:actor {name: {n}, id: {id}})",
                    		parameters("n", name, "id", id));
                    tx.success();
                }	
            }
            writeResponse(r,200,null);
        }
	}
}

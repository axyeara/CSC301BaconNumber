package ca.utoronto.utm.mcs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.sun.net.httpserver.HttpExchange;

public class Relationships extends HttpHandlers {

	@Override
	public void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		checkHttpVerb(r,"/api/v1/hasRelationship");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String actor = deserialized.getString("actorId");
        String movie = deserialized.getString("movieId");

        try (Session session = driver.session()) {
        		StatementResult hasActorMovie = session.run("MATCH (m:movie {id:{id_m}}),"
        				+"(a:actor {id:{id_a}}) RETURN a",
        				parameters("id_m",movie,"id_a",actor));

        		if (!hasActorMovie.hasNext()) {
        			writeResponse(r,404,null);
        			return;	
        		}

            try (Transaction tx = session.beginTransaction()) {

                JSONObject response = new JSONObject();
                response.put("actorId", actor);
                response.put("movieId", movie);
                
                StatementResult result = tx.run("MATCH (m:movie),(a:actor) "
                	+"WHERE a.id = {id_a} "
                	+"and m.id = {id_m} "
                	+"and(a)-[:ACTED_IN]->(m) "
                	+"RETURN a",
                	parameters("id_a", actor, "id_m", movie));
               
                tx.success();
                response.put("hasRelationship", result.hasNext());
                // if result does not have next, then
                // there are no matches
                writeResponse(r,200,JSONToString(response).getBytes());
            }
        }
		
	}

	@Override
	public void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException{
		checkHttpVerb(r,"/api/v1/addRelationship");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String actor = deserialized.getString("actorId");
        String movie = deserialized.getString("movieId");
        
        try (Session session = driver.session()) {
        	
        		StatementResult relationship = session.run("MATCH (m:movie {id:{id_m}}),(a:actor {id:{id_a}}) "
        		+"WHERE (a)-[:ACTED_IN]->(m) RETURN m",parameters("id_m",movie,"id_a",actor));
        		if (relationship.hasNext()) {
        			writeResponse(r,200,"".getBytes());
                return;
        		}
            try (Transaction tx = session.beginTransaction()) {

            		tx.run("MATCH (m:movie {id:{id_m}}),(a:actor{id:{id_a}}) "
            		+"CREATE (a)-[r:ACTED_IN]->(m) RETURN a",
            		parameters("id_a", actor, "id_m", movie));
            		
            		tx.success();
            		writeResponse(r,200,null);
            }
        }
		
	}

}

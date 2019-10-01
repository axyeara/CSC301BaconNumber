package ca.utoronto.utm.mcs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;

public class Movies extends HttpHandlers{

	@Override
	public void handleGet(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException {
		checkHttpVerb(r,"/api/v1/getMovie");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String id = deserialized.getString("movieId");
        
        try (Session session = driver.session()) {

            StatementResult movieSameId = session.run("MATCH (m:movie { id: {id} }) "
            		+"RETURN m.name AS name",parameters("id",id));
            try {
            		Record movie = movieSameId.single();
            		
                JSONObject response = new JSONObject();
                response.put("name", movie.get("name"));
                response.put("id", id);
                
                StatementResult actedIn = session.run("MATCH (a:actor)-[r:ACTED_IN]->"
                	+"(m:movie {id:{id}}) RETURN a.id AS id",parameters("id",id));
                
                List<Record> actorRecords = actedIn.list();
                String[] actors = new String[actorRecords.size()];
                
                for (int i=0; i<actorRecords.size(); i++) {
                		Record actorRecord = actorRecords.get(i);
                		actors[i] = actorRecord.get("id").asString();
                }
                response.put("actors", new JSONArray(actors));
                writeResponse(r,200,JSONToString(response).getBytes());

            } catch (NoSuchRecordException e) {
            		writeResponse(r,404,null);
            }
        }
		
	}

	@Override
	public void handlePost(HttpExchange r) throws IOException, JSONException, UnsupportedOperationException{
		checkHttpVerb(r,"/api/v1/addMovie");
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String name = deserialized.getString("name");
        String id = deserialized.getString("movieId");
        
        try (Session session = driver.session()) {

            StatementResult movieSameId = session.run("MATCH (m:movie {id:{id}}) "
            		+"RETURN m",parameters("id",id));
            if (!movieSameId.hasNext()) {
            		// if a movie with this id has not been added yet
                try  (Transaction tx = session.beginTransaction()) {
                    tx.run("CREATE (m:movie {name: {n}, id: {id}})",
                    		parameters("n", name, "id", id));
                    tx.success();
                }
            }
            
            writeResponse(r,200,null);
        }
        
	}
}

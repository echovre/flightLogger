package com.example.MyApp;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RequestController {

	MongoConnection mongoConnection=new MongoConnection();
	ObjectMapper mapper = new ObjectMapper();

	//curl -X POST -H "Content-Type: application/json" --data @logfile.json http://localhost:8080/
	@PostMapping(path = "/log", consumes = "application/json", produces = "application/json")
	public String log(@RequestBody JsonNode payload) {
		
		String uniqueID = UUID.randomUUID().toString();
		System.out.println("generated uuid:"+uniqueID);

		System.out.println("writing payload of length"+payload.toString().length());
		boolean success=mongoConnection.writeRecord(uniqueID,payload.toString());
		
		if(success) {
			System.out.println("Successfully wrote logfile with uuid: "+uniqueID);
			return uniqueID;
		}else {
			System.out.println("Failed to write logfile with uuid: "+uniqueID);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(path = "/log/{itemid}")
	@ResponseBody
	public String status(@PathVariable("itemid") String itemid){
		String result = mongoConnection.findRecord(itemid);
		if(result==null) {
			String error="Could not find record:"+itemid+", returning empty object!";
			System.out.println(error);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			//TODO: use class response instead:
	        //throw new NotFoundException(HttpStatus.NOT_FOUND, error);
            // this allows us to use @ExceptionHandler and @ResponseStatus
            // as well as any cleanup we might want 
		}
		return result;
	}
	
}

	/*
	//assuming payload body is a primitive string
	@PostMapping(path = "/request", consumes = "application/json", produces = "application/json")
	public String request(@RequestBody Body payload) {
		//TODO error handling if payload body not a string
		String uniqueID = UUID.randomUUID().toString();
		//make request
		Request req=new Request(payload.getBody(),uniqueID);
		boolean success=makeThirdPartyRequest(req);
		//record it
		mongoConnection.writeUpdateRecord(uniqueID, payload.getBody(), null,null);

		if(success) {
			return "Initiated request for document:"+payload.getBody();
		}else {
			return "FAILED to make request for document:"+payload.getBody()+" , not connected?";
		}
	}

	//Stub for third party request
	//
	//Notes:
	//Using apache library here because its supposedly faster
	//Should do some load testing to confirm
	//Just using sysout for logging right now, use something prettier(log4j?)
	//
	//TODO error handling:
	//if the third party has some kind of "status" endpoint,
	//		hit it when starting the controller so we know whether its up
	//log and cache the request for retry later if failed
	//write transaction and whether it succeeded to some "archive" table in database
	private boolean makeThirdPartyRequest(Request req){
		String url="http://example.com/request";
		
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			
			String requestObj=mapper.writeValueAsString(req);
			System.out.println("Object to send:"+requestObj);
			
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new StringEntity(requestObj));
			System.out.println("Executing request " + httpPost.getRequestLine());
			
			ResponseHandler<Integer> responseHandler = response -> {
				return response.getStatusLine().getStatusCode();
			};
			int status = httpclient.execute(httpPost, responseHandler);
			System.out.println("Third party request returned status code:"+status);
			return status >= 200 && status < 300;
		} catch (IOException e) {
			System.out.println("Third party request FAILED!");
			return false;
		}
	}

	//callback from third party service, with "started" message
	//a little unclear here, assuming the "body" is a string and corresponds to the "status" message
	@PostMapping(path = "/callback/{itemid}")
	@ResponseStatus
	public Response postCallback(@PathVariable("itemid") String itemid,
							 	 @RequestBody String message) {
		if (message!="STARTED") {
			System.out.println("Got unrecognized message:"+message);
		}
		System.out.println("got "+itemid);
		mongoConnection.writeUpdateRecord(itemid, null, message, null);
		return Response.status(204).build();
	}

	@PutMapping(path = "/callback/{itemid}")
	@ResponseStatus
	public Response putCallback(@PathVariable("itemid") String itemid,
								@RequestBody StatusDetail payload) {
		System.out.println("got "+itemid+" callback with status:"+payload.getStatus()+", detail:"+payload.getDetail());
		//status: started, processed, completed, error
		mongoConnection.writeUpdateRecord(itemid, null, payload.getStatus(), payload.getDetail());
		return Response.status(204).build();
	}

	@GetMapping(path = "/status/{itemid}")
	@ResponseBody
	public BodyStatusDetail status(@PathVariable("itemid") String itemid){
		Optional<BodyStatusDetail> result = mongoConnection.findRecord(itemid);
		if(result.isEmpty()) {
			System.out.println("Could not find record:"+itemid+" , returning empty object!");
			return new BodyStatusDetail();
		}
		return result.get();
	}
*/

// http://localhost:8080/greeting?name=blah
// ./mvnw spring-boot:run

//(may have to run mvn -N io.takari:maven:wrapper to get .mvn directory)

/*
 * curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{"body":"value"}' http://localhost:8080/request
request initiated for document:value
 */

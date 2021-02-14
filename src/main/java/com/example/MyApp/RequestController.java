package com.example.MyApp;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RequestController {

	MongoConnection mongoConnection=new MongoConnection();
	ObjectMapper mapper = new ObjectMapper();

	//curl -X POST -H "Content-Type: application/json" --data @logfile.json http://localhost:8080/
	@PostMapping(path = "/log", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Object> writeLog(@RequestBody JsonNode payload) {

		String uniqueID = UUID.randomUUID().toString();
		System.out.println("generated uuid:"+uniqueID);

		System.out.println("writing payload of length"+payload.toString().length());
		//TODO: error checking on write
		boolean success=mongoConnection.writeRecord(uniqueID,payload.toString());

		if(success) {
			System.out.println("Successfully wrote logfile with uuid: "+uniqueID);
			//TODO: return response body with href
			return new ResponseEntity<Object>(uniqueID, HttpStatus.OK);
		}else {
			System.out.println("Failed to write logfile with uuid: "+uniqueID);
			return new ResponseEntity<Object>(uniqueID, HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(path = "/log/{itemid}")
	@ResponseBody
	public ResponseEntity<Object> retrieveLog(@PathVariable("itemid") String itemid){
		Object result = mongoConnection.findRecordObj(itemid);
		if(result==null) {
			String error="Could not find record:"+itemid+"!";
			System.out.println(error);
			return new ResponseEntity<Object>(error, HttpStatus.NOT_FOUND);
			//TODO: perhaps use class response instead:
			//throw new NotFoundException(HttpStatus.NOT_FOUND, error);
			// this allows us to use @ExceptionHandler and @ResponseStatus
			// as well as any cleanup we might want 
		}
		return new ResponseEntity<Object>(result, HttpStatus.OK);
	}

	//TODO: retrieve battery state using db query instead
	//TODO: store battery state on insert alongside with log
	//	advantages: faster retrieval (at expense of insert)
	//	disadvantages: excess calculation if its never used, just the one item
	//TODO: store id, log, battery state, and "dirty" bit in db
	//have a separate server thread that updates each battery state
	//when querying for a log, if battery state is "dirty" do recalculation
	//(assumes the same log is written and updated, not just inserting new logs each time)

	@GetMapping(path = "/log/{itemid}/batteryconsumed")
	@ResponseBody
	public ResponseEntity<Object> batteryStatus(@PathVariable("itemid") String itemid){
		Object body = retrieveLog(itemid).getBody();
		try {
			JSONObject obj=new JSONObject(body.toString());
			HashMap<String, ArrayList<String>> items = getFlightLogItems(obj);
			Triplet<Double, Double, Double> triplet = getBatteryVoltage(items);
			
			JSONObject result = new JSONObject().put("Battery start voltage",triplet.getValue0())
					 .put("Battery end voltage",triplet.getValue1())
					 .put("Battery used",triplet.getValue2());
			System.out.println("Battery result:"+result);
			
	        return new ResponseEntity<Object>(result.toMap(), HttpStatus.OK);

		} catch (JsonProcessingException | JSONException e) {
			String error="Could not parse battery voltage!";
			System.out.println(error);
			return new ResponseEntity<Object>(error, HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}

	private Triplet<Double, Double, Double> getBatteryVoltage(HashMap<String, ArrayList<String>> items) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		ArrayList<String> timestamp=items.get("timestamp");

		//get array index of earliest and latest events
		int indexOfEarliest=-1;
		int indexOfLatest=-1;
		ZonedDateTime earliest=ZonedDateTime.now();
		ZonedDateTime latest=ZonedDateTime.now().minusYears(100);
		
		for (int i=0;i<timestamp.size();i++) {
			ZonedDateTime myTime = ZonedDateTime.parse(timestamp.get(i), formatter);
			if(myTime.isBefore(earliest)) {
				earliest=myTime;
				indexOfEarliest=i;
			}else if(myTime.isAfter(latest)) {
				latest=myTime;
				indexOfLatest=i;
			}
		}
		
		//get battery voltage corresponding to earliest/latest events
		ArrayList<String> voltages=items.get("battery_voltage");
		double startVoltage=Double.parseDouble(voltages.get(indexOfLatest));
		double endVoltage=Double.parseDouble(voltages.get(indexOfEarliest));
		double used=startVoltage-endVoltage;
		
		Triplet<Double, Double, Double> triplet= new Triplet<Double, Double, Double>(
				startVoltage,endVoltage,used); 
		return triplet;
	}

	private HashMap<String, ArrayList<String>> getFlightLogItems(JSONObject json) throws JsonMappingException, JsonProcessingException {
		//get flight data and keys
		JSONObject events=json.getJSONObject("message").getJSONObject("flight_logging");
		JSONArray keys=events.getJSONArray("flight_logging_keys");
		JSONArray items=events.getJSONArray("flight_logging_items");
		
		ObjectMapper mapper = new ObjectMapper();
		List<String> keysList 		= mapper.readValue(keys.toString(), new TypeReference<List<String>>(){});
		List<List<String>> dataList = mapper.readValue(items.toString(), new TypeReference<List<List<String>>>(){});
		
		//map it
		//theres probably a more clever/elegant way to do this with list.stream().collect()
		//probably even more elegant way storing in db instead of trying to match up keys/values
		HashMap<String,ArrayList<String>> flightData = new HashMap<>();
		for(String key : keysList) { 
			flightData.put(key, new ArrayList<String>());
		}
		for(int i=0;i<keysList.size();i++) { //for each key
			String key=(String) keysList.get(i);
			for (int j=0; j<dataList.size(); j++) { //for each list of data
				//get i-th value(corresponding to i-th key) in the j-th data list
				String element=dataList.get(j).get(i).toString();
				flightData.get(key).add(element);
			}
		}
		return flightData;
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

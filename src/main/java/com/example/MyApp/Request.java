package com.example.MyApp;

public class Request {

	private String body;
	private String callback;

	public Request() {
	}

	public Request(String body, String id) {
		this.body= body;
		this.callback="/callback/"+id;
	}

	public String getId() {
		return callback;
	}
	
	public String getBody() {
		return body;
	}
}

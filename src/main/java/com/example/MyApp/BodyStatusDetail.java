package com.example.MyApp;

public class BodyStatusDetail {
	
	private String body;
	private String status;
	private String detail;

	public BodyStatusDetail() {
	}
	
	public BodyStatusDetail(String body, String status, String detail) {
		this.body=body;
		this.status=status;
		this.detail=detail;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}

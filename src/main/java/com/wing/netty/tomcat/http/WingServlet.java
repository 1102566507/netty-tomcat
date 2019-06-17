package com.wing.netty.tomcat.http;

public abstract class WingServlet {
	
	public void service(WingRequest request,WingResponse response) throws Exception{
		
		//由service方法来决定，是调用doGet或者调用doPost
		if("GET".equalsIgnoreCase(request.getMethod())){
			doGet(request, response);
		}else{
			doPost(request, response);
		}

	}

	public abstract void doGet(WingRequest request,WingResponse response) throws Exception;

	public abstract void doPost(WingRequest request,WingResponse response) throws Exception;
}

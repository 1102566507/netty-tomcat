package com.wing.netty.tomcat.servlet;

import com.wing.netty.tomcat.http.WingRequest;
import com.wing.netty.tomcat.http.WingResponse;
import com.wing.netty.tomcat.http.WingServlet;

/**
 * @author wing
 * @date 2019-06-17 13:59
 */
public class FirstServlet extends WingServlet {
    public void doGet(WingRequest request, WingResponse response) throws Exception {
        this.doPost(request, response);
    }

    public void doPost(WingRequest request, WingResponse response) throws Exception {
        response.write("This is First Serlvet");
    }
}

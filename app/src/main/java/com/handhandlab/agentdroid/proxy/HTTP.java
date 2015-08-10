package com.handhandlab.agentdroid.proxy;

/**
 * Created by Handhand on 2015/7/16.
 */
public class HTTP {
    public static final String CRLF = "\r\n";
    public static final String EMPTY_LINE = "\r\n\r\n";
    public static final String RESPONSE_200 = "HTTP/1.1 200 OK"+EMPTY_LINE;
    public static final String RESPONSE_404 = "HTTP/1.1 404 Not Found\r\nContent-Length:0\r\n\r\n";
}

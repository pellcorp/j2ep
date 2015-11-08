/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.j2ep;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.j2ep.model.Server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A filter that will locate the appropriate Rule
 * and use it to rewrite any incoming request to
 * get the server targeted. Responses sent back
 * are also rewritten.
 *
 * @author Anders Nyman
 */
public class RewriteFilter implements Filter {
    /** 
     * Logging element supplied by commons-logging.
     */
	private final Log log = LogFactory.getLog(getClass());
    
    /** 
     * The server chain, will be traversed to find a matching server.
     */
    private final ServerChain serverChain;

    public RewriteFilter(final ServerChain serverChain) {
    	this.serverChain = serverChain;
    }

    /**
     * Rewrites the outgoing stream to make sure URLs and headers
     * are correct. The incoming request is first processed to 
     * identify what resource we want to proxy.
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            log.info("Not proxying, already committed.");
            return;
        } else if (!(request instanceof HttpServletRequest)) {
            log.info("Request is not HttpRequest, will only handle HttpRequests.");
            return;
        } else if (!(response instanceof HttpServletResponse)) {
            log.info("Request is not HttpResponse, will only handle HttpResponses.");
            return;
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            Server server = serverChain.evaluate(httpRequest);
            if (server == null) {
                log.info("Could not find a rule for this request, will not do anything.");
                filterChain.doFilter(request, response);
            } else {
                httpRequest.setAttribute("proxyServer", server);
                
                String ownHostName = request.getServerName() + ":" + request.getServerPort();
                UrlRewritingResponseWrapper wrappedResponse = new UrlRewritingResponseWrapper(
                		httpResponse, server, ownHostName, httpRequest.getContextPath(), serverChain);
                
                filterChain.doFilter(httpRequest, wrappedResponse);

                wrappedResponse.processStream();
            }
        }
    }
    
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }
}

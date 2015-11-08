package net.sf.j2ep;

import java.io.File;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ServerCmd {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);
		 
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ServletHandler servletHandler = new ServletHandler();
        context.setServletHandler(servletHandler);
       
        ConfigParser parser = new ConfigParser(new File("src/main/resources/data.xml"));
        ServerChain serverChain = parser.getServerChain();
        
        FilterHolder rewriter = new FilterHolder();
        rewriter.setName("rewriter");
        rewriter.setFilter(new RewriteFilter(serverChain));
        servletHandler.addFilter(rewriter, newMapping("rewriter", "/*"));
        context.addServlet(new ServletHolder(new DefaultServlet()),"/*");
        
        FilterHolder proxy = new FilterHolder();
        proxy.setName("proxy");
        proxy.setFilter(new ProxyFilter(serverChain));
        servletHandler.addFilter(proxy, newMapping("proxy", "/*"));
        context.addServlet(new ServletHolder(new DefaultServlet()),"/*");
        
        context.setContextPath("/");
        server.setHandler(context);
        
        server.start();
        server.join();
	}
	
	private static FilterMapping newMapping(String name, String pathSpec) {
		FilterMapping mapping = new FilterMapping();
		mapping.setFilterName(name);
		mapping.setPathSpec(pathSpec);
		return mapping;
	}
}

package com.spencerstewart.servlets;

import java.io.IOException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;

@Component(
    service = Servlet.class,
    property = {
      "sling.servlet.methods=" + HttpConstants.METHOD_GET,
      "sling.servlet.paths=" + "/bin/1040ez"
    })
public class Form1040EZServlet extends SlingAllMethodsServlet {

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    response.setContentType("text/html");
    response.getWriter().write("<h1>1040EZ Form</h1>");
    response.getWriter().write("<p>This is a placeholder for the 1040EZ form.</p>");
  }
}

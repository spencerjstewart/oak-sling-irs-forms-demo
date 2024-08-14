package com.spencerstewart.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
	service = Servlet.class,
	property = {
		"sling.servlet.methods=" + HttpConstants.METHOD_GET,
		"sling.servlet.paths=" + "/bin/1040ez"
	})
public class Form1040EZServlet extends SlingAllMethodsServlet {

	private static final Logger log = LoggerFactory.getLogger(Form1040EZServlet.class);
	private static final String FORM_TEMPLATE = "/1040ez-form-template.html";

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
		throws ServletException, IOException {

		response.setContentType("text/html");

		log.info("Attempting to load template: {}", FORM_TEMPLATE);

		try (InputStream is = getClass().getResourceAsStream(FORM_TEMPLATE)) {
			if (is != null) {
				log.info("Template found, reading content");
				String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				response.getWriter().write(htmlContent);
				log.info("Template content written to response");
			} else {
				log.error("Template not found: {}", FORM_TEMPLATE);
				response.getWriter().write("<h1>Error: Form template not found</h1>");
				response.getWriter().write("<p>Template path: " + FORM_TEMPLATE + "</p>");
			}
		} catch (Exception e) {
			log.error("Error processing template", e);
			response.getWriter().write("<h1>Error processing template</h1>");
			response.getWriter().write("<p>Error: " + e.getMessage() + "</p>");
		}
	}
}

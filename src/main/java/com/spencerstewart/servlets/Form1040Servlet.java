package com.spencerstewart.servlets;

import com.spencerstewart.services.PDFFormFillerService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/*
- @Component marks this class as an OSGi component.
	- An OSGi component is a class that is treated as a self-contained module that can be loaded
	and unloaded without restarting the entire application.
	- Components live in OSGi's service registry, which is somewhat analogous to Spring's
	application context or bean factory.
 */
@Component(
		// We're telling OSGi to register this component as a service of type Servlet
		// A Servlet handles HTTP requests and responses
		service = Servlet.class, property = {"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.methods=" + HttpConstants.METHOD_POST, "sling.servlet.paths=" + "/bin/1040"})
public class Form1040Servlet extends SlingAllMethodsServlet {

	private static final Logger log = LoggerFactory.getLogger(Form1040Servlet.class);
	private static final String FORM_TEMPLATE = "/1040-form-template.html";

	// Request a JCR Repository dependency
	@Reference
	private Repository repository;

	@Reference
	private PDFFormFillerService pdfFormFillerService;

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

		log.info("Attempting to load template: {}", FORM_TEMPLATE);

		// The response will be our html page
		response.setContentType("text/html");

		// getClass() evaluates to Form1040Servlet.class
		// Form1040Servlet.class and /1040-form-template.html are siblings in the classpath
		try (InputStream is = getClass().getResourceAsStream(FORM_TEMPLATE)) {
			if (is != null) {
				log.info("Template found, reading content");
				// Take the InputStream and create a String
				// TODO: Modify this to use direct streaming, or use Sightly
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

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

		try {
			Map<String, String> formData = new HashMap<>();
			formData.put("filingStatus", request.getParameter("filingStatus"));
			formData.put("firstName", request.getParameter("firstName"));
			formData.put("lastName", request.getParameter("lastName"));
			formData.put("ssn", request.getParameter("ssn"));
			formData.put("wages", request.getParameter("wages"));


			// The ResourceResolver is specific to an authenticated user and their authorizations.
			ResourceResolver resolver = request.getResourceResolver();
			// A JCR Session is a user's authenticated connection to the repository.
			// A Session is associated one-to-one with a Workspace object.
			// A Workspace object represent a view or slice or subtree of the entire content
			// repository.
			Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

			// Check to see if we successfully obtained a JCR Session.
			if (session != null) {
				try {
					// Here we are getting the root of the Workspace, not the entire repository.
					Node rootNode = session.getRootNode();
					Node formsNode = rootNode.hasNode("forms") ? rootNode.getNode("forms") : rootNode.addNode("forms");
					// Each form will be represented by the firstName, lastName, and current time
					// in milliseconds.
					Node formSubmissionNode = formsNode.addNode(formData.get("firstName") + "_" + formData.get("lastName") + "_" + System.currentTimeMillis());

					// For each field in the form the user submitted, we create a property using
					// the key and value from the form field.
					for (Map.Entry<String, String> entry : formData.entrySet()) {
						formSubmissionNode.setProperty(entry.getKey(), entry.getValue());
					}

					// This must be called to save all changes in the session to the repository.
					// If the session is closed before saving, all changes are lost.
					session.save();

					String pdfPath = pdfFormFillerService.fillForm(formData);

					response.setContentType("text/html");
					response.getWriter().write("<h1>Form Submitted Successfully</h1>");
					response.getWriter().write("<p>Thank you for submitting your 1040 form.</p>");
					response.getWriter().write("<p>You can download your filled PDF <a href='" + pdfPath + "'>here</a>.</p>");
				} catch (Exception e) {
					log.error("Error processing form submission", e);
					response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					response.getWriter().write("An error occurred while processing your submission: " + e.getMessage());
				} finally {
					session.logout();
				}
			} else {
				throw new ServletException("Unable to obtain JCR session");
			}
		} catch (Exception e) {
			log.error("Error processing form submission", e);
			response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("An error occurred while processing your submission. Please try again later.");
		}
	}
}

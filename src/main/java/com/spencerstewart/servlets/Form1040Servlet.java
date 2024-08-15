package com.spencerstewart.servlets;

import com.spencerstewart.services.PDFFormFillerService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		service = Servlet.class,
		property = {
				"sling.servlet.methods=" + HttpConstants.METHOD_GET,
				"sling.servlet.methods=" + HttpConstants.METHOD_POST,
				"sling.servlet.paths=" + "/bin/1040"
		})
public class Form1040Servlet extends SlingAllMethodsServlet {

	private static final Logger log = LoggerFactory.getLogger(Form1040Servlet.class);
	private static final String FORM_TEMPLATE = "/1040-form-template.html";

	@Reference
	private Repository repository;

	@Reference
	private PDFFormFillerService pdfFormFillerService;

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

	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		try {
			// Extract form data
			Map<String, String> formData = new HashMap<>();
			formData.put("filingStatus", request.getParameter("filingStatus"));
			formData.put("firstName", request.getParameter("firstName"));
			formData.put("lastName", request.getParameter("lastName"));
			formData.put("ssn", request.getParameter("ssn"));
			formData.put("wages", request.getParameter("wages"));

			// Store data in Oak repository
			ResourceResolver resolver = request.getResourceResolver();
			Session session = resolver.adaptTo(Session.class);

			if (session != null) {
				try {
					Node rootNode = session.getRootNode();
					Node formsNode = rootNode.hasNode("forms") ? rootNode.getNode("forms") : rootNode.addNode("forms");
					Node formSubmissionNode = formsNode.addNode(formData.get("firstName") + "_" + formData.get("lastName") + "_" + System.currentTimeMillis());

					for (Map.Entry<String, String> entry : formData.entrySet()) {
						formSubmissionNode.setProperty(entry.getKey(), entry.getValue());
					}

					session.save();

					// Generate PDF
					String pdfPath = pdfFormFillerService.fillForm(formData);
					formSubmissionNode.setProperty("pdfPath", pdfPath);
					session.save();

					response.setContentType("text/html");
					response.getWriter().write("<h1>Form Submitted Successfully</h1>");
					response.getWriter().write("<p>Thank you for submitting your 1040 form.</p>");
					response.getWriter().write("<p>You can download your filled PDF <a href='/bin/download-pdf?path=" + pdfPath + "'>here</a>.</p>");
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

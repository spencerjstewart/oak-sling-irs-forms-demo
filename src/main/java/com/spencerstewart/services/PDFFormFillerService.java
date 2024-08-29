package com.spencerstewart.services;

import org.apache.jackrabbit.JcrConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component(service = PDFFormFillerService.class)
public class PDFFormFillerService {

	private static final Logger log = LoggerFactory.getLogger(PDFFormFillerService.class);
	private static final String TEMPLATE_PATH = "/f1040.pdf";
	private static final String OUTPUT_PATH = "/content/filled-forms";

	@Reference
	private Repository repository;

	public String fillForm(Map<String, String> formData) throws IOException {

		String outputFileName = "form1040_" + System.currentTimeMillis() + ".pdf";
		log.info("Attempting to fill PDF form. Template: {}, Output: {}", TEMPLATE_PATH, outputFileName);

		Session session = null;

		try {
			// Log in as the admin user
			session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

			InputStream inputStream = getClass().getResourceAsStream(TEMPLATE_PATH);

			if (inputStream == null) {
				log.error("PDF template not found: {}", TEMPLATE_PATH);
				throw new IOException("PDF template not found: " + TEMPLATE_PATH);
			}

			try (PDDocument document = PDDocument.load(inputStream)) {
				PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

				if (acroForm != null) {
					for (Map.Entry<String, String> entry : formData.entrySet()) {
						String fieldName = mapFieldName(entry.getKey());
						String fieldValue = entry.getValue();

						if ("filingStatus".equals(entry.getKey())) {
							setFilingStatus(acroForm, fieldValue);
						} else {
							PDField field = acroForm.getField(fieldName);
							if (field != null) {
								field.setValue(fieldValue);
								log.debug("Set field '{}' to value '{}'", fieldName, fieldValue);
							} else {
								log.warn("Field not found: {}", fieldName);
							}
						}
					}
				} else {
					log.error("No AcroForm found in the PDF template");
					throw new IOException("PDF template does not contain a form");
				}

				Node rootNode = session.getRootNode();
				Node outputNode = rootNode.hasNode("content/filled-forms") ? rootNode.getNode("content/filled-forms") : rootNode.addNode("content/filled-forms", JcrConstants.NT_FOLDER);

				Node fileNode = outputNode.addNode(outputFileName, JcrConstants.NT_FILE);
				Node contentNode = fileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);

				File tempFile = File.createTempFile("form1040_", ".pdf");
				document.save(tempFile);
				log.info("PDF form filled and saved to temporary file: {}", tempFile.getAbsolutePath());

				try (InputStream fileInputStream = new FileInputStream(tempFile)) {
					contentNode.setProperty(JcrConstants.JCR_DATA, fileInputStream);
					contentNode.setProperty(JcrConstants.JCR_MIMETYPE, "application/pdf");
					contentNode.setProperty(JcrConstants.JCR_LASTMODIFIED, System.currentTimeMillis());
				}

				session.save();
				tempFile.delete();

				return OUTPUT_PATH + "/" + outputFileName;
			}
		} catch (Exception e) {
			log.error("Error filling PDF form", e);
			throw new IOException("Error filling PDF form", e);
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private void setFilingStatus(PDAcroForm acroForm, String filingStatus) throws IOException {
		String fieldName = "topmostSubform[0].Page1[0].c1_3[%d]";
		int index;

		switch (filingStatus) {
			case "single":
				index = 0;
				break;
			case "marriedJointly":
				index = 1;
				break;
			case "marriedSeparately":
				index = 2;
				break;
			case "headOfHousehold":
				index = 3;
				break;
			case "qualifyingWidow":
				index = 4;
				break;
			default:
				log.warn("Unknown filing status: {}", filingStatus);
				return;
		}

		String checkboxName = String.format(fieldName, index);
		PDCheckBox checkbox = (PDCheckBox) acroForm.getField(checkboxName);
		if (checkbox != null) {
			checkbox.check();
			log.debug("Set filing status checkbox '{}' to checked", checkboxName);
		} else {
			log.warn("Filing status checkbox not found: {}", checkboxName);
		}
	}

	private String mapFieldName(String key) {
		switch (key) {
			case "firstName":
				return "topmostSubform[0].Page1[0].f1_04[0]";
			case "lastName":
				return "topmostSubform[0].Page1[0].f1_05[0]";
			case "ssn":
				return "topmostSubform[0].Page1[0].f1_06[0]";
			case "wages":
				return "topmostSubform[0].Page1[0].f1_31[0]";
			default:
				return key;
		}
	}
}

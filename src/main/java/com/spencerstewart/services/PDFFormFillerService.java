package com.spencerstewart.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.jackrabbit.JcrConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
- Sling is a web framework that uses Java Content Repository as a data store.
	- Uses OSGi for a modular architecture.
		- Allows loading and unloading of bundles without restarting app.
- JCR has a resource-oriented architecture.
	- each URL maps to a resource in the content repository.
- Apache Jackrabbit Oak is an implementation of the JCR spec.

- A "resource" in Sling is an abstraction that means any kind of data:
	- Node in JCR
	- File or dir in file system.
	- DB record.
	- External web service.
	- Bundle or component in OSGi framework.


*/

@Component(service = PDFFormFillerService.class)
public class PDFFormFillerService {

	// TODO: Fix logger config
	private static final Logger log = LoggerFactory.getLogger(PDFFormFillerService.class);
	private static final String TEMPLATE_PATH = "/f1040.pdf";
	private static final String OUTPUT_PATH = "/content/filled-forms";

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	public String fillForm(Map<String, String> formData) throws IOException {
		String outputFileName = "form1040_" + System.currentTimeMillis() + ".pdf";
		log.info("Attempting to fill PDF form. Template: {}, Output: {}", TEMPLATE_PATH, outputFileName);

		try (ResourceResolver resolver = resourceResolverFactory.getAdministrativeResourceResolver(null)) {
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

				Resource outputResource = resolver.getResource(OUTPUT_PATH);
				if (outputResource == null) {
					outputResource = resolver.create(resolver.getResource("/content"), "filled-forms", null);
				}

				Resource fileResource = resolver.create(outputResource, outputFileName, Map.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE));
				Resource contentResource = resolver.create(fileResource, JcrConstants.JCR_CONTENT, Map.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE));

				File tempFile = File.createTempFile("form1040_", ".pdf");
				document.save(tempFile);
				log.info("PDF form filled and saved to temporary file: {}", tempFile.getAbsolutePath());

				try (InputStream fileInputStream = new FileInputStream(tempFile)) {
					ModifiableValueMap contentMap = contentResource.adaptTo(ModifiableValueMap.class);
					contentMap.put(JcrConstants.JCR_DATA, fileInputStream);
					contentMap.put(JcrConstants.JCR_MIMETYPE, "application/pdf");
					contentMap.put(JcrConstants.JCR_LASTMODIFIED, System.currentTimeMillis());
				}

				resolver.commit();
				tempFile.delete();

				return OUTPUT_PATH + "/" + outputFileName;
			}
		} catch (Exception e) {
			log.error("Error filling PDF form", e);
			throw new IOException("Error filling PDF form", e);
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
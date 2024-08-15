package com.spencerstewart.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PDFFormFillerService.class)
public class PDFFormFillerService {

	private static final Logger log = LoggerFactory.getLogger(PDFFormFillerService.class);
	private static final String TEMPLATE_PATH = "/f1040.pdf";
	private static final String OUTPUT_DIRECTORY = "/filled/";

	public String fillForm(Map<String, String> formData) throws IOException {
		String outputPath = OUTPUT_DIRECTORY + "form1040_" + System.currentTimeMillis() + ".pdf";

		try (PDDocument document = PDDocument.load(new File(TEMPLATE_PATH))) {
			PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

			if (acroForm != null) {
				for (Map.Entry<String, String> entry : formData.entrySet()) {
					String fieldName = mapFieldName(entry.getKey());
					String fieldValue = entry.getValue();

					PDField field = acroForm.getField(fieldName);
					if (field != null) {
						field.setValue(fieldValue);
					} else {
						log.warn("Field not found: {}", fieldName);
					}
				}
			}

			document.save(new File(outputPath));
		}

		return outputPath;
	}

	/*
	Fields:
	filingStatus
	firstName
	lastName
	ssn
	wages

	// filingStatus
	Field: c1_3[0]
	  Type: /Btn
	  Flags: Unknown
	  Value: /1
	  Default Value: None
	Field: c1_3[1]
	  Type: /Btn
	  Flags: Unknown
	  Value: /1
	  Default Value: None
	 */
	private String mapFieldName(String key) {
		switch(key) {
			case "filingStatus":
				return "topmostSubform[0].Page1[0].c1_3[0]";
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

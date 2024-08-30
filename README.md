# Sling Web App Demo

This project is a demo application demonstrating the usage of Apache Sling and Oak. Users fill out a form asking for tax
information. Upon submission, the backend processes the data, stores it in a JCR repository, generates a filled 1040 pdf, 
which is downloadable by the user.

## Technologies Used
- **Apache Sling**: Web framework for the application.
- **Apache Jackrabbit Oak**: Content repository used for storing form submissions.
- **Apache PDFBox**: Library for creating and manipulating PDF documents.
- **OSGi**: Modular system for Java that allows developers to create components called bundles that can be installed, updated, 
or removed without requiring a restart of the entire application.

## Prerequisites
- **Java 17**
- **Maven**
- **Docker**

## Installation and Running 
1. Clone the repository:
```bash
git clone https://github.com/spencerjstewart/oak-sling-irs-forms-demo.git
cd oak-sling-irs-forms-demo
```
2. Build the docker image:
```bash
docker build -t oak-sling-irs-forms-demo .
```
3. Run the docker image:
```bash
docker run --rm -p 8080:8080 oak-sling-irs-forms-demo
```
4. Navigate to http://localhost:8080/bin/1040 to access the form.

## TODO
- Configure logging.
- Configure and use HTL.
- Configure user mapping so that we aren't using admin credentials.
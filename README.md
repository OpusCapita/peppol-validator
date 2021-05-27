# peppol-validator [![CircleCI](https://circleci.com/gh/OpusCapita/peppol-validator.svg?style=svg)](https://circleci.com/gh/OpusCapita/peppol-validator)

Peppol OpusCapita Access Point validator service running on Andariel Platform.

The service reads files from the `peppol.validator.queue.in.name:peppol.message.validate` queue and validates them. 

The validating done using SAX validator, a javax validation implementation. The service requires validation artifacts to be pre-created. Validation artifacts can be created using schematron files usually published by OPEN PEPPOL community. Please refer peppol-artifact-creator for more info about this.

After validation, it sends the container message to the next service in the route config.

Please check the wiki pages for more information:
* [Validator Service](https://opuscapita.atlassian.net/wiki/spaces/IIPEP/pages/107806873/New+Peppol+solution+modules+description#NewPeppolsolutionmodulesdescription-validator)
* [Validation Artifacts Upgrade](https://opuscapita.atlassian.net/wiki/spaces/IIPEP/pages/107806896/Upgrade+of+validation+artifacts)
* [Validator as a Service](https://opuscapita.atlassian.net/wiki/spaces/IIPEP/pages/107806913/Validator+as+a+Service)
.

--------

UPDATING DOCUMENT SUPPORT
If we need to update/add/delete a document type as Access Point, we have to update the configuration in validator service. It will give access point to ability to validate the documents with the new document type. Then we can safely register our customer in SMP with the capability to receive the new document type. 

Explanation of the fields:

* id: an unique integer id. this table can be used as reference.
* description: the visible name of the standard. it should be detailed.
* archetype: the parent standard group of the document type
* localName: the name of the root tag of the body of the document
* documentId: document type identifier value in the SBDH
* processId: process identifier value in the SBDH
* processSchema: identifier tag value just after the process identifier in the SBDH, it is not used most of the time meaning the usage of default “cenbii-procid-ubl”
* version: ubl version of the standard
* rules: validation artifacts to be applied to validate the documents

Once you created a new record in the [configuration file](https://github.com/OpusCapita/peppol-validator/blob/master/src/main/resources/application-validating.yml) and filled the fields you will recognize that you need validation artifacts to set the rules value. We store validation artifacts in a zip file which is in this directory. It is extracted to a folder inside the docker container in the build process. After you find the validation artifacts you should put them inside the zip file and set their path as the value of the rules field.

The validation artifacts are usually released as schematron files (.sch) by the official sources. It is OPENPEPPOL for BIS (see Useful links for PEPPOL section).

Once you acquire the schematron files you have to compile them into xsl or xslt files before adding them to the repository. The native way is to use saxon ddl’s to compile it step by step using an ant script but we have started to use a library recently. It is called ph-schematron and it is really easy to use. Just create a basic maven project set the pom.xml instructed in their documentation and run the application. It will convert schematron files in the configured directory and put the resulting xsl files to the configured directory. 

Alternative 2, to convert schematron files, is the rest API on Webmethods, which will also convert schematrons to an XSL.  No installation or configuration needed for this option. http://localhost:15055/testapi/functional.compileSchematronToSVRL  (replace port with the right port to the integration server)

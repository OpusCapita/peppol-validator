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
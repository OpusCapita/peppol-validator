package com.opuscapita.peppol.validator.rest;

import com.opuscapita.peppol.validator.consumer.ValidatorRestConsumer;
import com.opuscapita.peppol.validator.rest.dto.ValidationRestResponse;
import com.opuscapita.peppol.validator.rule.ValidationRuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ValidatorRestController {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorRestController.class);

    private ValidatorRestConsumer consumer;
    private ValidationRuleConfig ruleConfig;

    @Autowired
    public ValidatorRestController(ValidatorRestConsumer consumer, ValidationRuleConfig ruleConfig) {
        this.consumer = consumer;
        this.ruleConfig = ruleConfig;
    }

    @GetMapping("/public/get-document-types")
    public ResponseEntity<?> getDocumentTypes() {
        return wrap(ruleConfig.getMap());
    }

    @PostMapping("/validate-file")
    public ResponseEntity<?> validateFile(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        logger.info("Validator received api request for file: " + multipartFile.getOriginalFilename());
        ValidationRestResponse response = consumer.consume(multipartFile);
        return wrap(response);
    }

    private <T> ResponseEntity<T> wrap(T body) {
        if (body != null) {
            return ResponseEntity.ok(body);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}

package com.opuscapita.peppol.validator.rest;

import com.opuscapita.peppol.validator.rule.ValidationRuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ValidatorRestController {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorRestController.class);

    private ValidationRuleConfig ruleConfig;

    @Autowired
    public ValidatorRestController(ValidationRuleConfig ruleConfig) {
        this.ruleConfig = ruleConfig;
    }

    @GetMapping("/get-document-types")
    public ResponseEntity<?> getDocumentTypes() {
        return wrap(ruleConfig.getMap());
    }

    private <T> ResponseEntity<T> wrap(T body) {
        if (body != null) {
            return ResponseEntity.ok(body);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}

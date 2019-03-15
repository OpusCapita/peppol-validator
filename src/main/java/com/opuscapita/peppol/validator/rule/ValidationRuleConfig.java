package com.opuscapita.peppol.validator.rule;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.validator.controller.ValidationArtifactsRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "peppol.validator.rules")
public class ValidationRuleConfig {

    private List<ValidationRule> map;

    public List<ValidationRule> getMap() {
        return map;
    }

    @Nullable
    public ValidationRule getRule(ContainerMessage cm) {
        Optional<ValidationRule> rule = map.stream().filter(r -> r.matches(cm)).findAny();
        if (!rule.isPresent()) {
            cm.getHistory().addError("Validation rule not found for file " + cm.getFileName());
        }
        return rule.orElse(null);
    }

    @PostConstruct
    private void validateFiles() {
        for (ValidationRule rule : map) {
            for (String fileName : rule.getRules()) {
                File ruleFile = new File(getClass().getResource(ValidationArtifactsRepository.RULES_ROOT + fileName).getFile());
                if (!ruleFile.exists()) {
                    throw new IllegalArgumentException("Missing validation artifact for " + rule.getDescription());
                }
            }
        }
    }

}
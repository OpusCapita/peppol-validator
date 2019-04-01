package com.opuscapita.peppol.validator.rule;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "peppol.validator.rules")
public class ValidationRuleConfig {

    private List<ValidationRule> map;

    public List<ValidationRule> getMap() {
        return map;
    }

    public void setMap(List<ValidationRule> map) {
        this.map = map;
    }

    @Nullable
    public ValidationRule getRule(ContainerMessage cm) {
        Optional<ValidationRule> rule = map.stream().filter(r -> r.matches(cm)).findAny();
        if (!rule.isPresent()) {
            cm.getHistory().addError("Cannot find a validation artifact for file");
        }
        return rule.orElse(null);
    }

}
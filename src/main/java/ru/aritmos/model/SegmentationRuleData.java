package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
@Data
@Introspected
@Builder
@Serdeable
public class SegmentationRuleData {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    HashMap<String,String> keyProperty;
    String serviceId;
    String queueId;
}

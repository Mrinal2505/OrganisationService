package com.hti.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entitymetadataupdaterequest {

    private Map<String, Object> metadata;

    private Boolean isActive;

    private String updatedBy;
}
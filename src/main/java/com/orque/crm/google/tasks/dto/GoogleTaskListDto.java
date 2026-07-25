package com.orque.crm.google.tasks.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleTaskListDto {
    private String id;
    private String title;
}

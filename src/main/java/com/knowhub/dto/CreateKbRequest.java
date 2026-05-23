package com.knowhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKbRequest {

    @NotBlank(message = "知识库名不能为空")
    @Size(min = 1, max = 100, message = "知识库名长度1-100位")
    private String name;

    private String description;
}

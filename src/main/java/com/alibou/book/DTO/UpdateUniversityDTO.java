// UpdateUniversityDTO.java
package com.alibou.book.dto;

import com.alibou.book.Entity.UniversityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUniversityDTO {
    @NotNull(message = "University ID is required")
    private Long id;

    @NotBlank(message = "University name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "University type is required")
    private UniversityType type;
}

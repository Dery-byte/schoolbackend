package com.alibou.book.DTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BulkImportResultDTO {
    private int imported;
    private int skipped;
    private int failed;
    private List<String> skippedNames = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}

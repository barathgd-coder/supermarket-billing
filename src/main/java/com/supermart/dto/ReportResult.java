package com.supermart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReportResult {
    private String title;
    private List<String> headers;
    private List<List<String>> rows;
    /** Column index used as chart label and as chart value. */
    private int labelColumn;
    private int valueColumn;
    /** "bar", "line" or "pie". */
    private String chartType;
}

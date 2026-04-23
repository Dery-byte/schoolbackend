package com.alibou.book.DTO.Projections;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegionStatsDTO {
    private final String region;
    private final Long count;
    private final Double percentage;
}
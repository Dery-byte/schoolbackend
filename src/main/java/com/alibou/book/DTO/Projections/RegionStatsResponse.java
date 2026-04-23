package com.alibou.book.DTO.Projections;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class RegionStatsResponse {
    private final Long totalBiodata;
    private final List<RegionStatsDTO> regions;
}
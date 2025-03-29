package com.alibou.book.DTO;

import lombok.Data;
import java.util.List;

@Data
public class WaecResponse {
    private ReqStatus reqstatus;
    private Candidate candidate;
    private List<ResultDetail> resultdetails;
}

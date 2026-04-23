//package com.alibou.book.Services;
//
//import com.alibou.book.Entity.CandidateResult;
//import com.alibou.book.Repositories.CandidateResultRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class CandidateResultServiceImpl implements CandidateResultService {
//
//    private final CandidateResultRepository candidateResultRepository;
//
//    @Autowired
//    public CandidateResultServiceImpl(CandidateResultRepository candidateResultRepository) {
//        this.candidateResultRepository = candidateResultRepository;
//    }
//
//    @Override
//    public CandidateResult saveCandidateResult(CandidateResult candidateResult) {
//        // Set reverse mapping for resultdetails
//        if (candidateResult.getResultdetails() != null) {
//            candidateResult.getResultdetails().forEach(detail -> detail.setCandidateResult(candidateResult));
//        }
//        return candidateResultRepository.save(candidateResult);
//    }
//
//    @Override
//    public CandidateResult getCandidateResultByIndex(String cindex) {
//        return candidateResultRepository.findByCindex(cindex);
//    }
//
//    @Override
//    public List<CandidateResult> getAllCandidateResults() {
//        return candidateResultRepository.findAll();
//    }
//}

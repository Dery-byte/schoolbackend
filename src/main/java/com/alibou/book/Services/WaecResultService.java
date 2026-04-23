package com.alibou.book.Services;

import com.alibou.book.Entity.WaecResult;
import com.alibou.book.Repositories.WaecResultRepository;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WaecResultService {

    @Autowired
    private WaecResultRepository waecResultRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * Saves WAEC results for a user.
     */

    public WaecResult saveWaecResult(String username, WaecResult waecResult) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
//        waecResult.setUser(user);
        return waecResultRepository.save(waecResult);
    }

    /**
     * Fetches the WAEC result of a user.
     */
//    public Optional<WaecResult> getUserResults(String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        return waecResultRepository.findByUser(user);
//    }
}

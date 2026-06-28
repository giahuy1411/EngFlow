package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final UserRepository userRepository;

    public Integer getBalance(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getCoins() != null ? user.getCoins() : 0;
    }

    @Transactional
    public void earnCoins(Long userId, int amount) {
        if (amount <= 0) return;
        User user = userRepository.findById(userId).orElseThrow();
        int currentCoins = user.getCoins() != null ? user.getCoins() : 0;

        long newCoins = (long) currentCoins + amount;
        if (newCoins > Integer.MAX_VALUE) {
            newCoins = Integer.MAX_VALUE;
        }

        user.setCoins((int) newCoins);
        userRepository.save(user);
    }
}

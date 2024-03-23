package org.air.service;

import lombok.extern.slf4j.Slf4j;
import org.air.entity.Authority;
import org.air.entity.Refresh;
import org.air.entity.User;
import org.air.repository.TokenRepository;
import org.air.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class CustomUserDetailService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Transactional
    public User loadUserById(String userid) {
        User user = userRepository.existsByUserid(userid) ? userRepository.findByUserid(userid) : null;
        log.info(user.getAuthority().toString());
        return user;
    }

    // user sign up
    public boolean save(User user) {
        try {
            Authority authority = Authority.builder()
                    .authority("ROLE_USER")
                    .build();
            user.setAuthority(authority);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // token save

    @Transactional
    public Refresh token_save(User user, String token) {
        // 없는 유저라면 토큰을 저장하고
        // 기존에 존재하는 유저라면 토큰을 update
        if (user.getRefresh() == null) {
            Refresh refreshToken = Refresh.builder()
                    .id(0)
                    .user(user)
                    .token(token)
                    .build();
            return tokenRepository.save(refreshToken);
        } else {
            Refresh refreshToken = tokenRepository.findByUser_userid(user.getUserid());
            refreshToken.setToken(token);
            log.info(refreshToken.toString());
            return refreshToken;
        }

    }

    // findByToken
    public boolean logout(Long id) {
        try {
            tokenRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
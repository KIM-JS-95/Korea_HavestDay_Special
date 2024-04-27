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
    public User loadUserByUser(User login_user) {
        User user = userRepository.existsByUseridAndPassword(login_user.getUserid(), login_user.getPassword())
                ? userRepository.findByUseridAndPassword(login_user.getUserid(), login_user.getPassword()) : null;

        return user;
    }

    @Transactional
    public User loadUserByToken(String userToken) {
        User user = userRepository.existsByUserid(userToken) ? userRepository.findByUserid(userToken) : null;
        return user;
    }

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
    public boolean token_save(User user, String token) {
        // 없는 유저라면 토큰을 저장하고
        // 로그인중인 유저라면 토큰을 update
        if (user.getRefresh() == null) {
            Refresh refreshToken = Refresh.builder()
                    .id(0)
                    .token(token)
                    .build();
            tokenRepository.save(refreshToken);
            return true;

        } else {
            User user1 = userRepository.findByUserid(user.getUserid());
            user1.getRefresh().setToken(token);
            return true;
        }

    }

    @Transactional
    public boolean modify(User user_modify, String token) {
        try {
            User user = userRepository.existsByUserid(token) ? userRepository.findByUserid(token) : null;
            user.setUserid(user_modify.getUserid());
            user.setEmail(user_modify.getEmail());
            user.setPassword(user_modify.getPassword());
            return true;
        }catch (Exception e){
            return false;
        }
    }

    // findByToken
    public boolean logout(String userid) {
        return true;
    }

}
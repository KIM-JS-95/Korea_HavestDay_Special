package org.air.repository;

import org.air.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    User findByUserid(String userid);
    List<User> findByName(String username);
    boolean existsByUserid(String userid);
    List<User> findAll();
}
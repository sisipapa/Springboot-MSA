package com.sisipapa.study.msa.auth.repository;


import com.sisipapa.study.msa.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String email);
}

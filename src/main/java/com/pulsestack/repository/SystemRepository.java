package com.pulsestack.repository;

import com.pulsestack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pulsestack.model.System;

import java.util.List;
import java.util.Optional;

public interface SystemRepository extends JpaRepository<System, Long> {
    Optional<System> findBySystemId(String systemId);

    List<System> findAllByUser(User user);

    boolean existsBySystemId(String systemId);

    Optional<System> findSystemByName(String name);

}

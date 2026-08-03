package com.github.anyuoyuna.lifeassistant.repository;

import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}

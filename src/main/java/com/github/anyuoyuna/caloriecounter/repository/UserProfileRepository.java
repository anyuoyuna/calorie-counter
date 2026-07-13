package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}

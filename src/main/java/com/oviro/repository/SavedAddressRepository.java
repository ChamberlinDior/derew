package com.oviro.repository;

import com.oviro.model.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedAddressRepository extends JpaRepository<SavedAddress, UUID> {

    List<SavedAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<SavedAddress> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE SavedAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}

package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LibraryProfileJpaRepository extends JpaRepository<LibraryProfileEntity, UUID> {
}

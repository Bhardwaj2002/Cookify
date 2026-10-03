package com.cookify.recipe.repository;

import com.cookify.recipe.entity.EquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {

    Optional<EquipmentEntity> findByName(String name);
}

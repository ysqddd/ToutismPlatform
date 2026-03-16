package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionsRepository extends JpaRepository<Permissions, Long> {
    Permissions findByLevel(int level);
}

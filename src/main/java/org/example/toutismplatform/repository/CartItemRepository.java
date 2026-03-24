package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.id = :id")
    void deleteByUserIdAndId(Long userId, Long id);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteByUserId(Long userId);
}

package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserShopItemRepository extends JpaRepository<UserShopItem, Long> {
    List<UserShopItem> findByUserId(Long userId);
    List<UserShopItem> findByUserIdAndItemItemType(Long userId, String itemType);
    Optional<UserShopItem> findByUserIdAndItemId(Long userId, Long itemId);
    boolean existsByUserIdAndItemId(Long userId, Long itemId);
}

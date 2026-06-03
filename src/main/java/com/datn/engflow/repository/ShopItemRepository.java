package com.datn.engflow.repository;

import com.datn.engflow.model.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    List<ShopItem> findByIsActiveTrue();
    List<ShopItem> findByIsActiveTrueAndItemType(String itemType);
}

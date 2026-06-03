package com.datn.engflow.service;

import com.datn.engflow.model.entity.ShopItem;
import com.datn.engflow.model.entity.UserShopItem;

import java.util.List;
import java.util.Map;

public interface CoinService {
    Integer getBalance(Long userId);
    void earnCoins(Long userId, int amount);
    List<ShopItem> getAvailableShopItems();
    UserShopItem buyItem(Long userId, Long itemId);
    List<UserShopItem> getOwnedItems(Long userId);
    void equipItem(Long userId, Long itemId);
}

package com.datn.engflow.service.impl;

import com.datn.engflow.model.entity.ShopItem;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserShopItem;
import com.datn.engflow.repository.ShopItemRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserShopItemRepository;
import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoinServiceImpl implements CoinService {

    private final UserRepository userRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserShopItemRepository userShopItemRepository;

    @Override
    public Integer getBalance(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getCoins() != null ? user.getCoins() : 0;
    }

    @Override
    @Transactional
    public void earnCoins(Long userId, int amount) {
        if (amount <= 0) return;
        User user = userRepository.findById(userId).orElseThrow();
        int currentCoins = user.getCoins() != null ? user.getCoins() : 0;
        
        long newCoins = (long) currentCoins + amount;
        if (newCoins > Integer.MAX_VALUE) {
            newCoins = Integer.MAX_VALUE;
        }
        
        user.setCoins((int) newCoins);
        userRepository.save(user);
    }

    @Override
    public List<ShopItem> getAvailableShopItems() {
        return shopItemRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    public UserShopItem buyItem(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ShopItem", "id", itemId));
        
        if (!item.getIsActive()) {
            throw new BadRequestException("Vật phẩm hiện không khả dụng");
        }
        
        if (userShopItemRepository.existsByUserIdAndItemId(userId, itemId)) {
            throw new BadRequestException("Bạn đã sở hữu vật phẩm này");
        }
        
        int currentCoins = user.getCoins() != null ? user.getCoins() : 0;
        if (currentCoins < item.getPrice()) {
            throw new BadRequestException("Không đủ xu để mua vật phẩm này");
        }
        
        user.setCoins(currentCoins - item.getPrice());
        userRepository.save(user);
        
        UserShopItem userItem = UserShopItem.builder()
                .user(user)
                .item(item)
                .isEquipped(false)
                .build();
                
        return userShopItemRepository.save(userItem);
    }

    @Override
    public List<UserShopItem> getOwnedItems(Long userId) {
        return userShopItemRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void equipItem(Long userId, Long itemId) {
        UserShopItem itemToEquip = userShopItemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new BadRequestException("Bạn chưa sở hữu vật phẩm này"));
                
        String itemType = itemToEquip.getItem().getItemType();
        
        // Unequip current item of same type
        List<UserShopItem> currentEquipped = userShopItemRepository.findByUserIdAndItemItemType(userId, itemType);
        for (UserShopItem equipped : currentEquipped) {
            if (equipped.getIsEquipped()) {
                equipped.setIsEquipped(false);
                userShopItemRepository.save(equipped);
            }
        }
        
        itemToEquip.setIsEquipped(true);
        userShopItemRepository.save(itemToEquip);
    }
}

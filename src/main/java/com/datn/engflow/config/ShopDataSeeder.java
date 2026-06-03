package com.datn.engflow.config;

import com.datn.engflow.model.entity.ShopItem;
import com.datn.engflow.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class ShopDataSeeder implements CommandLineRunner {

    private final ShopItemRepository shopItemRepository;

    @Override
    public void run(String... args) throws Exception {
        if (shopItemRepository.count() == 0) {
            log.info("Seeding shop items...");
            
            shopItemRepository.saveAll(Arrays.asList(
                ShopItem.builder().name("Avatar Cáo").description("Một hình đại diện con cáo dễ thương").itemType("AVATAR").imageUrl("https://api.dicebear.com/7.x/bottts/svg?seed=fox").price(100).build(),
                ShopItem.builder().name("Avatar Rồng").description("Hình đại diện rồng dũng mãnh").itemType("AVATAR").imageUrl("https://api.dicebear.com/7.x/bottts/svg?seed=dragon").price(300).build(),
                ShopItem.builder().name("Avatar Robot").description("Hình đại diện robot tương lai").itemType("AVATAR").imageUrl("https://api.dicebear.com/7.x/bottts/svg?seed=robot").price(150).build(),
                ShopItem.builder().name("Khung Vàng").description("Khung ảnh đại diện mạ vàng").itemType("BACKGROUND").imageUrl("#FFD700").price(500).build(),
                ShopItem.builder().name("Khung Kim Cương").description("Khung ảnh đại diện lấp lánh").itemType("BACKGROUND").imageUrl("#b9f2ff").price(1000).build(),
                ShopItem.builder().name("Huy hiệu Chăm chỉ").description("Huy hiệu người học chăm chỉ").itemType("BADGE").imageUrl("🏅").price(200).build()
            ));
            
            log.info("Shop seeding completed.");
        }
    }
}

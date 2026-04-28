package com.homekm.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemAclRepository extends JpaRepository<ItemAcl, Long> {

    List<ItemAcl> findByItemTypeAndItemId(String itemType, Long itemId);

    List<ItemAcl> findByItemTypeAndItemIdAndUserId(String itemType, Long itemId, Long userId);

    void deleteByItemTypeAndItemId(String itemType, Long itemId);
}

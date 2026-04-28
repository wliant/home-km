package com.homekm.common;

import com.homekm.auth.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessControlService {

    private final ItemAclRepository aclRepository;

    public AccessControlService(ItemAclRepository aclRepository) {
        this.aclRepository = aclRepository;
    }

    public boolean canRead(String itemType, Long itemId, Long ownerId, Visibility visibility, UserPrincipal principal) {
        if (principal == null) return false;
        if (principal.isAdmin()) return true;
        return switch (visibility == null ? Visibility.HOUSEHOLD : visibility) {
            case HOUSEHOLD -> true;
            case PRIVATE -> ownerId != null && ownerId.equals(principal.getId());
            case CUSTOM -> ownerId != null && ownerId.equals(principal.getId())
                    || hasAcl(itemType, itemId, principal.getId());
        };
    }

    public boolean canWrite(String itemType, Long itemId, Long ownerId, Visibility visibility, UserPrincipal principal) {
        if (principal == null || principal.isChild()) return false;
        if (principal.isAdmin()) return true;
        if (ownerId != null && ownerId.equals(principal.getId())) return true;
        return switch (visibility == null ? Visibility.HOUSEHOLD : visibility) {
            case HOUSEHOLD -> true;
            case PRIVATE -> false;
            case CUSTOM -> aclRepository.findByItemTypeAndItemIdAndUserId(itemType, itemId, principal.getId())
                    .stream().anyMatch(a -> "EDITOR".equalsIgnoreCase(a.getRole()));
        };
    }

    private boolean hasAcl(String itemType, Long itemId, Long userId) {
        return !aclRepository.findByItemTypeAndItemIdAndUserId(itemType, itemId, userId).isEmpty();
    }

    public void replaceAcls(String itemType, Long itemId, List<AclEntry> entries) {
        aclRepository.deleteByItemTypeAndItemId(itemType, itemId);
        for (AclEntry e : entries) {
            ItemAcl acl = new ItemAcl();
            acl.setItemType(itemType);
            acl.setItemId(itemId);
            acl.setUserId(e.userId());
            acl.setRole(e.role() == null ? "VIEWER" : e.role());
            aclRepository.save(acl);
        }
    }

    public List<ItemAcl> listAcls(String itemType, Long itemId) {
        return aclRepository.findByItemTypeAndItemId(itemType, itemId);
    }

    public record AclEntry(Long userId, String role) {}
}

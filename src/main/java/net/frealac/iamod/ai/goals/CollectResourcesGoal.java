package net.frealac.iamod.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * AI Goal that makes the entity collect nearby items.
 */
public class CollectResourcesGoal extends AIGoal {
    private ItemEntity targetItem;
    private final double searchRadius;
    private final double speedModifier;
    private int searchCooldown = 0;
    private static final int SEARCH_INTERVAL = 20; // Search every second

    public CollectResourcesGoal(Mob entity, int priority, double searchRadius, double speedModifier) {
        super(entity, priority);
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
    }

    public CollectResourcesGoal(Mob entity, int priority) {
        this(entity, priority, 8.0, 1.0);
    }

    @Override
    public boolean canUse() {
        if (!isActive) return false;

        searchCooldown--;
        if (searchCooldown > 0) return false;

        searchCooldown = SEARCH_INTERVAL;

        // Find nearest item
        Level level = entity.level();
        List<ItemEntity> items = level.getEntitiesOfClass(
            ItemEntity.class,
            entity.getBoundingBox().inflate(searchRadius),
            item -> item.isAlive() && !item.getItem().isEmpty()
        );

        if (items.isEmpty()) return false;

        // Get closest item
        targetItem = items.stream()
            .min((a, b) -> Double.compare(
                entity.distanceToSqr(a),
                entity.distanceToSqr(b)
            ))
            .orElse(null);

        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isActive) return false;
        if (targetItem == null) return false;
        if (!targetItem.isAlive()) return false;

        double distance = entity.distanceToSqr(targetItem);
        return distance <= searchRadius * searchRadius;
    }

    @Override
    public void start() {
        if (targetItem != null) {
            entity.getNavigation().moveTo(targetItem, speedModifier);
        }
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) return;

        double distance = entity.distanceTo(targetItem);

        if (distance < 1.5) {
            // Try to pick up the item
            ItemStack stack = targetItem.getItem();
            // In a real implementation, you'd add this to the entity's inventory
            // For now, we just remove the item
            targetItem.discard();
            targetItem = null;
        } else if (entity.getNavigation().isDone()) {
            // Repath if navigation failed
            entity.getNavigation().moveTo(targetItem, speedModifier);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        entity.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Collect Resources (radius: " + searchRadius + ")";
    }

    public ItemEntity getTargetItem() {
        return targetItem;
    }
}

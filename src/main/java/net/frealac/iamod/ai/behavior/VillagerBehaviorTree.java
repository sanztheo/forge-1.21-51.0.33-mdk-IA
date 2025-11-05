package net.frealac.iamod.ai.behavior;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;
import com.badlogic.gdx.ai.btree.leaf.Wait;
import net.minecraft.world.entity.npc.Villager;

/**
 * Behavior Tree implementation for Villager AI using gdx-ai.
 * Provides a more sophisticated decision-making system than simple Goals.
 *
 * Structure:
 * - Selector (chooses first successful task)
 *   - Sequence: Check if needs sleep -> Go to bed -> Sleep
 *   - Sequence: Check if needs food -> Find food -> Eat
 *   - Sequence: Check if needs work -> Go to workstation -> Work
 *   - Default: Idle/Wander
 */
public class VillagerBehaviorTree {
    private final BehaviorTree<Villager> tree;
    private final Villager villager;

    public VillagerBehaviorTree(Villager villager) {
        this.villager = villager;
        this.tree = createBehaviorTree();
    }

    /**
     * Create the behavior tree structure.
     */
    private BehaviorTree<Villager> createBehaviorTree() {
        // Root selector: tries tasks in order until one succeeds
        Selector<Villager> root = new Selector<>();

        // Sleep behavior sequence
        Sequence<Villager> sleepSequence = new Sequence<>();
        sleepSequence.addChild(new CheckNeedsSleepTask());
        sleepSequence.addChild(new GoToBedTask());
        sleepSequence.addChild(new SleepTask());

        // Eat behavior sequence
        Sequence<Villager> eatSequence = new Sequence<>();
        eatSequence.addChild(new CheckNeedsFoodTask());
        eatSequence.addChild(new FindFoodTask());
        eatSequence.addChild(new EatTask());

        // Work behavior sequence
        Sequence<Villager> workSequence = new Sequence<>();
        workSequence.addChild(new CheckNeedsWorkTask());
        workSequence.addChild(new GoToWorkstationTask());
        workSequence.addChild(new WorkTask());

        // Social behavior sequence
        Sequence<Villager> socialSequence = new Sequence<>();
        socialSequence.addChild(new CheckNeedsSocialTask());
        socialSequence.addChild(new FindNearbyVillagerTask());
        socialSequence.addChild(new SocializeTask());

        // Default idle/wander behavior
        Task<Villager> idleTask = new WanderTask();

        // Add all sequences to root selector
        root.addChild(sleepSequence);
        root.addChild(eatSequence);
        root.addChild(workSequence);
        root.addChild(socialSequence);
        root.addChild(idleTask);

        // Create and return the behavior tree
        BehaviorTree<Villager> behaviorTree = new BehaviorTree<>(root, villager);
        return behaviorTree;
    }

    /**
     * Execute one step of the behavior tree.
     * Should be called every tick.
     */
    public void step() {
        tree.step();
    }

    /**
     * Reset the behavior tree to its initial state.
     */
    public void reset() {
        tree.reset();
    }

    // ===== Task Implementations =====

    /**
     * Check if villager needs sleep (based on time of day).
     */
    private static class CheckNeedsSleepTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            Villager villager = getObject();
            long timeOfDay = villager.level().getDayTime() % 24000;
            // Villagers sleep from 12000 to 23000 (6 PM to 6 AM)
            boolean needsSleep = timeOfDay >= 12000 && timeOfDay <= 23000;
            return needsSleep ? Status.SUCCEEDED : Status.FAILED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class GoToBedTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, navigate to bed
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class SleepTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            Villager villager = getObject();
            villager.startSleeping(villager.blockPosition());
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    /**
     * Check if villager needs food.
     */
    private static class CheckNeedsFoodTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            Villager villager = getObject();
            // Check if villager has food need (simplified)
            int foodLevel = villager.getInventory().countItem(net.minecraft.world.item.Items.BREAD);
            return foodLevel < 3 ? Status.SUCCEEDED : Status.FAILED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class FindFoodTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, search for food in the area
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class EatTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, consume food item
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    /**
     * Check if villager needs to work.
     */
    private static class CheckNeedsWorkTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            Villager villager = getObject();
            long timeOfDay = villager.level().getDayTime() % 24000;
            // Villagers work from 2000 to 9000 (8 AM to 3 PM)
            boolean isWorkTime = timeOfDay >= 2000 && timeOfDay <= 9000;
            return isWorkTime ? Status.SUCCEEDED : Status.FAILED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class GoToWorkstationTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, navigate to workstation
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class WorkTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, perform work activities
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    /**
     * Check if villager needs social interaction.
     */
    private static class CheckNeedsSocialTask extends LeafTask<Villager> {
        private int ticksSinceLastSocial = 0;

        @Override
        public Status execute() {
            ticksSinceLastSocial++;
            // Need social interaction every 5 minutes (6000 ticks)
            if (ticksSinceLastSocial >= 6000) {
                ticksSinceLastSocial = 0;
                return Status.SUCCEEDED;
            }
            return Status.FAILED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class FindNearbyVillagerTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            Villager villager = getObject();
            // Find nearby villagers
            java.util.List<Villager> nearbyVillagers = villager.level().getEntitiesOfClass(
                Villager.class,
                villager.getBoundingBox().inflate(10.0),
                v -> v != villager
            );
            return !nearbyVillagers.isEmpty() ? Status.SUCCEEDED : Status.FAILED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    private static class SocializeTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, perform social interactions
            return Status.SUCCEEDED;
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }

    /**
     * Default wander behavior.
     */
    private static class WanderTask extends LeafTask<Villager> {
        @Override
        public Status execute() {
            // In a real implementation, wander around
            return Status.RUNNING; // Always keep wandering
        }

        @Override
        protected Task<Villager> copyTo(Task<Villager> task) {
            return task;
        }
    }
}

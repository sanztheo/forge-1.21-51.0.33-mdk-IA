# Exemples avancés du système d'IA

Ce document présente des exemples d'utilisation avancée du système d'IA avec les bibliothèques installées (gdx-ai et fastutil).

## Table des matières

1. [Utilisation des Behavior Trees](#behavior-trees)
2. [Pathfinding optimisé avec fastutil](#pathfinding-optimise)
3. [Combinaison Goals + Behavior Trees](#combinaison)
4. [Exemples pratiques](#exemples-pratiques)

---

## Behavior Trees

Les Behavior Trees offrent une approche plus sophistiquée que les Goals simples. Ils permettent de créer des comportements complexes avec des conditions et des séquences.

### Exemple 1 : Utilisation basique d'un Behavior Tree

```java
import net.frealac.iamod.ai.behavior.VillagerBehaviorTree;
import net.minecraft.world.entity.npc.Villager;

// Dans votre gestionnaire d'entité ou event handler
public void initializeVillagerAI(Villager villager) {
    VillagerBehaviorTree behaviorTree = new VillagerBehaviorTree(villager);

    // Appeler step() chaque tick
    // Dans un event TickEvent.ServerTickEvent par exemple
}

@SubscribeEvent
public void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;

    // Pour chaque villageois avec behavior tree
    behaviorTree.step();
}
```

### Exemple 2 : Utilisation du BehaviorManager

Le `BehaviorManager` permet de basculer entre Goals simples et Behavior Trees :

```java
import net.frealac.iamod.ai.behavior.BehaviorManager;
import net.frealac.iamod.ai.behavior.BehaviorManager.BehaviorMode;

// Créer avec mode Goals (simple)
BehaviorManager manager = new BehaviorManager(mob, BehaviorMode.GOALS);

// Ou avec mode Behavior Tree (avancé)
BehaviorManager manager = new BehaviorManager(villager, BehaviorMode.BEHAVIOR_TREE);

// Tick le système (dans votre event handler)
manager.tick();

// Basculer entre les modes dynamiquement
if (villagerNeedsComplexBehavior) {
    manager.setMode(BehaviorMode.BEHAVIOR_TREE);
} else {
    manager.setMode(BehaviorMode.GOALS);
}
```

### Exemple 3 : Créer un Behavior Tree personnalisé

```java
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;

public class CustomEntityBehaviorTree {
    private final BehaviorTree<MyEntity> tree;

    public CustomEntityBehaviorTree(MyEntity entity) {
        // Créer la structure de l'arbre
        Selector<MyEntity> root = new Selector<>();

        // Séquence d'attaque
        Sequence<MyEntity> attackSequence = new Sequence<>();
        attackSequence.addChild(new CheckEnemyNearbyTask());
        attackSequence.addChild(new ChaseEnemyTask());
        attackSequence.addChild(new AttackTask());

        // Séquence de patrouille
        Sequence<MyEntity> patrolSequence = new Sequence<>();
        patrolSequence.addChild(new CheckShouldPatrolTask());
        patrolSequence.addChild(new PatrolTask());

        // Comportement par défaut (idle)
        Task<MyEntity> idleTask = new IdleTask();

        root.addChild(attackSequence);
        root.addChild(patrolSequence);
        root.addChild(idleTask);

        this.tree = new BehaviorTree<>(root, entity);
    }

    public void step() {
        tree.step();
    }

    // Définir vos tâches personnalisées
    private static class CheckEnemyNearbyTask extends Task<MyEntity> {
        @Override
        public Status execute() {
            MyEntity entity = getObject();
            boolean hasEnemy = entity.level().getNearestPlayer(entity, 10.0) != null;
            return hasEnemy ? Status.SUCCEEDED : Status.FAILED;
        }

        @Override
        protected Task<MyEntity> copyTo(Task<MyEntity> task) {
            return task;
        }
    }

    // ... autres tâches ...
}
```

---

## Pathfinding optimisé

Le système de pathfinding utilise maintenant fastutil pour de meilleures performances.

### Exemple 1 : Pathfinding synchrone

```java
import net.frealac.iamod.ai.pathfinding.PathfindingManager;
import net.minecraft.core.BlockPos;

BlockPos start = entity.blockPosition();
BlockPos goal = new BlockPos(100, 64, 200);

List<BlockPos> path = PathfindingManager.getInstance()
    .findPathSync(entity.level(), start, goal);

if (!path.isEmpty()) {
    // Suivre le chemin
    BlockPos nextStep = path.get(0);
    entity.getNavigation().moveTo(
        nextStep.getX() + 0.5,
        nextStep.getY(),
        nextStep.getZ() + 0.5,
        1.0
    );
}
```

### Exemple 2 : Pathfinding asynchrone (recommandé)

```java
import net.frealac.iamod.ai.pathfinding.PathfindingManager;

BlockPos start = entity.blockPosition();
BlockPos goal = targetPosition;

// Avec callback
PathfindingManager.getInstance().findPathAsync(
    entity.level(),
    start,
    goal,
    path -> {
        // Ce code s'exécute sur le thread principal une fois le chemin trouvé
        if (!path.isEmpty()) {
            navigateToPath(entity, path);
        } else {
            System.out.println("Pas de chemin trouvé !");
        }
    }
);

// Avec CompletableFuture pour plus de contrôle
CompletableFuture<List<BlockPos>> future = PathfindingManager.getInstance()
    .findPathAsync(entity.level(), start, goal);

future.thenAccept(path -> {
    // Traiter le résultat
    entity.level().getServer().execute(() -> {
        // Code sur le thread principal
        handlePath(entity, path);
    });
}).exceptionally(ex -> {
    // Gérer les erreurs
    System.err.println("Erreur de pathfinding: " + ex.getMessage());
    return null;
});
```

### Exemple 3 : Pathfinding avec plusieurs destinations

```java
import java.util.concurrent.CompletableFuture;

List<BlockPos> destinations = Arrays.asList(pos1, pos2, pos3, pos4);
BlockPos start = entity.blockPosition();

// Calculer tous les chemins en parallèle
List<CompletableFuture<List<BlockPos>>> futures = destinations.stream()
    .map(dest -> PathfindingManager.getInstance().findPathAsync(level, start, dest))
    .collect(Collectors.toList());

// Attendre tous les résultats
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenRun(() -> {
        // Tous les chemins sont calculés
        List<List<BlockPos>> allPaths = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // Choisir le chemin le plus court
        List<BlockPos> shortestPath = allPaths.stream()
            .filter(path -> !path.isEmpty())
            .min(Comparator.comparingInt(List::size))
            .orElse(Collections.emptyList());

        if (!shortestPath.isEmpty()) {
            entity.level().getServer().execute(() -> {
                navigateToPath(entity, shortestPath);
            });
        }
    });
```

---

## Combinaison Goals + Behavior Trees

Vous pouvez combiner les deux approches pour une IA hybride.

### Exemple : Behavior Tree qui utilise des Goals

```java
public class HybridAISystem {
    private final BehaviorManager behaviorManager;
    private final AIGoalManager emergencyGoals;

    public HybridAISystem(Mob entity) {
        // Comportement normal avec Behavior Tree
        this.behaviorManager = new BehaviorManager(entity, BehaviorMode.BEHAVIOR_TREE);

        // Goals d'urgence (priorité haute)
        this.emergencyGoals = new AIGoalManager(entity);
        emergencyGoals.addGoal(new FleeFromDangerGoal(entity, 1));
    }

    public void tick() {
        // D'abord vérifier les goals d'urgence
        if (hasActiveEmergencyGoal()) {
            emergencyGoals.tick();
        } else {
            // Sinon, exécuter le behavior tree normal
            behaviorManager.tick();
        }
    }

    private boolean hasActiveEmergencyGoal() {
        return !emergencyGoals.getActiveGoals().isEmpty();
    }
}
```

---

## Exemples pratiques

### Exemple complet : Villageois avec routine quotidienne

```java
import net.frealac.iamod.ai.behavior.VillagerBehaviorTree;
import net.frealac.iamod.ai.data.AIDataProvider;
import net.frealac.iamod.ai.pathfinding.PathfindingManager;

public class SmartVillagerAI {
    private final Villager villager;
    private final VillagerBehaviorTree behaviorTree;

    public SmartVillagerAI(Villager villager) {
        this.villager = villager;
        this.behaviorTree = new VillagerBehaviorTree(villager);

        // Charger les données persistantes
        villager.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
            // Restaurer les lieux connus
            List<BlockPos> knownLocations = aiData.getData().getKnownLocations();
            if (knownLocations.isEmpty()) {
                // Premier lancement, découvrir l'environnement
                discoverSurroundings();
            }
        });
    }

    public void tick() {
        // Mettre à jour le behavior tree
        behaviorTree.step();

        // Sauvegarder l'expérience
        villager.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
            aiData.getData().addExperience(1);
        });
    }

    private void discoverSurroundings() {
        BlockPos villagerPos = villager.blockPosition();

        // Chercher les points d'intérêt dans un rayon de 50 blocs
        for (int x = -50; x <= 50; x += 10) {
            for (int z = -50; z <= 50; z += 10) {
                BlockPos checkPos = villagerPos.offset(x, 0, z);

                // Vérifier si c'est un lieu intéressant (lit, poste de travail, etc.)
                if (isPointOfInterest(checkPos)) {
                    villager.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
                        aiData.getData().addKnownLocation(checkPos);
                    });
                }
            }
        }
    }

    private boolean isPointOfInterest(BlockPos pos) {
        BlockState state = villager.level().getBlockState(pos);
        // Vérifier si c'est un lit, poste de travail, cloche, etc.
        return state.is(BlockTags.BEDS) ||
               state.is(BlockTags.VILLAGER_WORKSTATIONS);
    }
}
```

### Exemple : Système de réputation avec Goals dynamiques

```java
public class ReputationBasedAI {
    private final Mob entity;
    private final BehaviorManager behaviorManager;

    public ReputationBasedAI(Mob entity) {
        this.entity = entity;
        this.behaviorManager = new BehaviorManager(entity);
        updateGoalsBasedOnReputation();
    }

    public void tick() {
        behaviorManager.tick();
    }

    public void onReputationChange(Player player, int reputationDelta) {
        entity.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
            // Mettre à jour la réputation dans la mémoire
            String key = "reputation_" + player.getUUID();
            int currentRep = Integer.parseInt(
                aiData.getData().getMemory(key) != null ?
                aiData.getData().getMemory(key) : "0"
            );
            int newRep = currentRep + reputationDelta;
            aiData.getData().setMemory(key, String.valueOf(newRep));

            // Mettre à jour les goals en fonction de la réputation
            updateGoalsBasedOnReputation();
        });
    }

    private void updateGoalsBasedOnReputation() {
        AIGoalManager goalManager = behaviorManager.getGoalManager();
        if (goalManager == null) return;

        goalManager.clearGoals();

        // Goals de base
        goalManager.addGoal(new PatrolGoal(entity, 3));

        // Goals basés sur la réputation
        entity.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
            int avgReputation = calculateAverageReputation(aiData);

            if (avgReputation > 50) {
                // Réputation élevée : comportement amical
                goalManager.addGoal(new FollowPlayerGoal(entity, 2));
                goalManager.addGoal(new OfferTradeGoal(entity, 1));
            } else if (avgReputation < -50) {
                // Réputation basse : comportement hostile
                goalManager.addGoal(new AvoidPlayerGoal(entity, 1));
                goalManager.addGoal(new CallGuardsGoal(entity, 2));
            }
        });
    }

    private int calculateAverageReputation(IAIData aiData) {
        Map<String, String> memory = aiData.getData().getMemory();
        return memory.entrySet().stream()
            .filter(e -> e.getKey().startsWith("reputation_"))
            .mapToInt(e -> Integer.parseInt(e.getValue()))
            .average()
            .orElse(0.0)
            .intValue();
    }
}
```

---

## Optimisation des performances

### Conseil 1 : Utiliser le pathfinding asynchrone

```java
// ❌ MAUVAIS : Bloque le thread principal
List<BlockPos> path = PathfindingManager.getInstance()
    .findPathSync(level, start, goal);

// ✅ BON : Non-bloquant
PathfindingManager.getInstance().findPathAsync(level, start, goal, path -> {
    // Traiter le résultat
});
```

### Conseil 2 : Limiter la fréquence des calculs

```java
private int pathfindingCooldown = 0;

public void tick() {
    pathfindingCooldown--;

    if (pathfindingCooldown <= 0 && needsNewPath()) {
        pathfindingCooldown = 100; // Recalculer tous les 5 secondes
        PathfindingManager.getInstance().findPathAsync(level, start, goal, this::handlePath);
    }
}
```

### Conseil 3 : Nettoyer le cache périodiquement

```java
// Dans un event de fin de jour ou toutes les heures
@SubscribeEvent
public void onDayEnd(/* event approprié */) {
    PathfindingManager.getInstance().clearAllCaches();
}
```

---

## Ressources supplémentaires

- [Documentation gdx-ai](https://github.com/libgdx/gdx-ai/wiki)
- [Documentation fastutil](https://fastutil.di.unimi.it/)
- [Guide principal AI_SYSTEM_SETUP.md](./AI_SYSTEM_SETUP.md)

---

## Support et contribution

Pour rapporter des bugs ou suggérer des améliorations, veuillez créer une issue sur le dépôt du projet.

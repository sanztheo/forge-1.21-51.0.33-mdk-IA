# Système d'IA - Documentation d'installation et utilisation

## Vue d'ensemble

Ce document décrit l'architecture du système d'IA modulable implémenté dans le mod, ainsi que les instructions pour installer les bibliothèques externes recommandées.

## Fonctionnalités implémentées

### 5.0 - Architecture du système d'IA modulable ✅

Le système d'IA est basé sur un système de **Goals** modulaire qui permet de définir différents comportements pour les entités IA.

#### Classes principales :
- **`AIGoal`** : Classe abstraite de base pour tous les objectifs d'IA
- **`AIGoalManager`** : Gestionnaire qui orchestre l'exécution des goals avec gestion des priorités
- **`PatrolGoal`** : Goal pour faire patrouiller l'entité entre plusieurs points
- **`FollowPlayerGoal`** : Goal pour suivre le joueur le plus proche
- **`CollectResourcesGoal`** : Goal pour collecter les items au sol

#### Utilisation :
```java
// Créer un gestionnaire de goals pour une entité
AIGoalManager goalManager = new AIGoalManager(mob);

// Ajouter des goals avec différentes priorités (plus bas = plus prioritaire)
goalManager.addGoal(new FollowPlayerGoal(mob, 1));
goalManager.addGoal(new PatrolGoal(mob, 2));
goalManager.addGoal(new CollectResourcesGoal(mob, 3));

// Appeler tick() chaque tick de l'entité
goalManager.tick();
```

### 5.1 - Pathfinding avancé pour l'IA ✅

Système de pathfinding A* avec cache pour optimiser les calculs.

#### Classes principales :
- **`PathNode`** : Représente un nœud dans le graphe de pathfinding
- **`AdvancedPathfinder`** : Implémentation de l'algorithme A* avec cache
- **`PathfindingManager`** : Gestionnaire singleton avec support du pathfinding asynchrone

#### Utilisation :
```java
// Pathfinding synchrone
List<BlockPos> path = PathfindingManager.getInstance()
    .findPathSync(level, start, goal);

// Pathfinding asynchrone avec callback
PathfindingManager.getInstance().findPathAsync(level, start, goal, path -> {
    // Ce code s'exécute sur le thread principal une fois le chemin trouvé
    if (!path.isEmpty()) {
        entity.getNavigation().moveTo(path.get(0).getX(), path.get(0).getY(), path.get(0).getZ(), 1.0);
    }
});
```

### 5.2 - Persistance des données de l'IA ✅

Système de capability pour sauvegarder les données de l'IA entre les sessions.

#### Classes principales :
- **`AIData`** : Classe contenant toutes les données persistantes de l'IA
- **`IAIData`** : Interface de la capability
- **`AIDataProvider`** : Provider pour la capability
- **`AIDataCapabilityInit`** : Gestion de l'attachement de la capability aux entités

#### Données sauvegardées :
- Objectif actuel
- Expérience acquise
- Compétences (skills)
- Lieux connus (points d'intérêt)
- Mémoire (clé-valeur)
- Configuration des goals activés

#### Utilisation :
```java
// Accéder aux données d'IA d'une entité
mob.getCapability(AIDataProvider.CAPABILITY).ifPresent(aiData -> {
    AIData data = aiData.getData();

    // Ajouter de l'expérience
    data.addExperience(10);

    // Définir une compétence
    data.setSkill("mining", 5);

    // Ajouter un lieu connu
    data.addKnownLocation(new BlockPos(100, 64, 200));

    // Sauvegarder dans la mémoire
    data.setMemory("last_player", "Steve");
});
```

### 5.3 - Interface utilisateur (GUI) pour l'IA ✅

Interface graphique pour configurer l'IA en jeu.

#### Classes principales :
- **`AIConfigScreen`** : Écran de configuration de l'IA
- **`OpenAIConfigS2CPacket`** : Packet pour ouvrir l'écran (serveur → client)
- **`UpdateAIConfigC2SPacket`** : Packet pour mettre à jour la config (client → serveur)

#### Utilisation :
```java
// Côté serveur : ouvrir l'écran de configuration pour un joueur
NetworkHandler.CHANNEL.send(
    new OpenAIConfigS2CPacket(entityId),
    PacketDistributor.PLAYER.with(serverPlayer)
);
```

## Bibliothèques externes recommandées (optionnelles)

### 1. gdx-ai - IA avancée

**Description** : Framework d'intelligence artificielle avec Behavior Trees, State Machines, Steering Behaviors, et Pathfinding.

**Fonctionnalités** :
- Behavior Trees (arbres de comportement)
- Finite State Machines (machines à états)
- Steering Behaviors (comportements de direction)
- Formation Motion
- Pathfinding avancé

**Installation** :

Ajouter dans `build.gradle` :
```gradle
repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
    implementation 'com.badlogicgames.gdx:gdx-ai:1.8.2'
}
```

**Licence** : Apache 2.0

**Ressources** :
- GitHub : https://github.com/libgdx/gdx-ai
- Wiki : https://github.com/libgdx/gdx-ai/wiki

### 2. GeckoLib - Animations 3D

**Description** : Bibliothèque d'animation pour modèles 3D complexes avec support des animations squelettiques.

**Fonctionnalités** :
- Animations 3D basées sur keyframes
- Support de 30+ easings
- Animations concurrentes
- Keyframes pour sons et particules
- Event keyframes

**Installation** :

Ajouter dans `build.gradle` :
```gradle
repositories {
    maven { url 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
}

dependencies {
    implementation fg.deobf('software.bernie.geckolib:geckolib-forge-1.21:5.2.1')
}
```

**Licence** : MIT

**Ressources** :
- CurseForge : https://www.curseforge.com/minecraft/mc-mods/geckolib
- GitHub : https://github.com/bernie-g/geckolib
- Modrinth : https://modrinth.com/mod/geckolib

### 3. fastutil - Collections optimisées

**Description** : Extension du Java Collections Framework avec des structures optimisées pour les types primitifs.

**Fonctionnalités** :
- Collections type-specific (moins de mémoire)
- Maps, Sets, Lists optimisés
- Support des big arrays (64-bit)
- I/O rapide pour fichiers binaires et texte

**Installation** :

Ajouter dans `build.gradle` :
```gradle
dependencies {
    implementation 'it.unimi.dsi:fastutil:8.5.12'
}
```

**Licence** : Apache 2.0

**Ressources** :
- Site officiel : https://fastutil.di.unimi.it/
- Maven Repository : https://mvnrepository.com/artifact/it.unimi.dsi/fastutil

## Installation complète

### Étape 1 : Mettre à jour build.gradle

Ouvrez le fichier `build.gradle` et ajoutez les dépendances souhaitées dans la section `dependencies` :

```gradle
dependencies {
    // ... dépendances existantes ...

    // [OPTIONNEL] gdx-ai pour IA avancée
    // implementation 'com.badlogicgames.gdx:gdx-ai:1.8.2'

    // [OPTIONNEL] GeckoLib pour animations 3D
    // implementation fg.deobf('software.bernie.geckolib:geckolib-forge-1.21:5.2.1')

    // [OPTIONNEL] fastutil pour collections optimisées
    // implementation 'it.unimi.dsi:fastutil:8.5.12'
}
```

**Note** : Décommentez uniquement les bibliothèques que vous souhaitez utiliser.

### Étape 2 : Ajouter les repositories

Si vous utilisez gdx-ai ou GeckoLib, ajoutez les repositories correspondants :

```gradle
repositories {
    // ... repositories existants ...

    // Pour gdx-ai
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }

    // Pour GeckoLib
    maven { url 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
}
```

### Étape 3 : Recharger le projet Gradle

Exécutez la commande suivante pour télécharger les dépendances :

```bash
./gradlew --refresh-dependencies
```

Ou depuis votre IDE :
- **IntelliJ IDEA** : Clic droit sur build.gradle → "Reload Gradle Project"
- **Eclipse** : Clic droit sur le projet → Gradle → "Refresh Gradle Project"

### Étape 4 : Vérifier l'installation

Compilez le projet pour vérifier que tout fonctionne :

```bash
./gradlew build
```

## Exemples d'utilisation avancée

### Exemple 1 : Créer un Goal personnalisé

```java
public class MineBlockGoal extends AIGoal {
    private BlockPos targetBlock;

    public MineBlockGoal(Mob entity, int priority) {
        super(entity, priority);
    }

    @Override
    public boolean canUse() {
        // Chercher un bloc à miner
        return findNearbyBlock();
    }

    @Override
    public void start() {
        // Se déplacer vers le bloc
        entity.getNavigation().moveTo(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), 1.0);
    }

    @Override
    public void tick() {
        // Logique de minage
        if (entity.blockPosition().closerThan(targetBlock, 2.0)) {
            // Miner le bloc
            entity.level().destroyBlock(targetBlock, true);
        }
    }

    // ... autres méthodes ...
}
```

### Exemple 2 : Pathfinding asynchrone avec plusieurs destinations

```java
List<BlockPos> destinations = Arrays.asList(pos1, pos2, pos3);
CompletableFuture<?>[] futures = destinations.stream()
    .map(dest -> PathfindingManager.getInstance().findPathAsync(level, start, dest))
    .toArray(CompletableFuture[]::new);

CompletableFuture.allOf(futures).thenRun(() -> {
    // Tous les chemins sont calculés
    System.out.println("Tous les chemins sont prêts !");
});
```

## Dépannage

### Problème : Les packets ne sont pas reçus
**Solution** : Vérifiez que les packets sont bien enregistrés dans `NetworkHandler.register()`

### Problème : Les données ne sont pas sauvegardées
**Solution** : Vérifiez que la capability est bien attachée dans `AIDataCapabilityInit`

### Problème : Le pathfinding est trop lent
**Solution** : Utilisez le pathfinding asynchrone ou réduisez `maxSearchNodes`

## Références

- [Documentation Forge](https://docs.minecraftforge.net/)
- [Minecraft Modding Wiki](https://forge.gemwire.uk/wiki/Main_Page)
- [gdx-ai Documentation](https://github.com/libgdx/gdx-ai/wiki)
- [GeckoLib Documentation](https://docs.geckolib.com/)

## Auteurs

Développé dans le cadre du mod IAMOD pour Minecraft 1.21 avec Forge 51.0.33.

## Licence

Ce système d'IA fait partie du mod IAMOD et suit la même licence que le projet principal.

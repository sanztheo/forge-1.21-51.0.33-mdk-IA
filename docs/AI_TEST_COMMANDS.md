# Commandes de Test - Système d'IA (5.0-5.3)

Ce document liste toutes les commandes pour tester les fonctionnalités implémentées du système d'IA.

---

## 🔧 Commandes de Build et Setup

### 1. Vérifier la version Java (doit être Java 21 ARM64)
```bash
java -version
```
**Résultat attendu**: `OpenJDK Runtime Environment Temurin-21.0.9+10` avec architecture `aarch64`

### 2. Donner les permissions à gradlew
```bash
chmod +x gradlew
```

### 3. Build du projet
```bash
./gradlew build
```

### 4. Lancer le client de test
```bash
./gradlew runClient
```

### 5. Nettoyer et rebuild (si nécessaire)
```bash
./gradlew clean build
```

### 6. Vérifier la compilation sans build complet
```bash
npx tsc --noEmit
```
**Note**: Cette commande est pour les projets TypeScript, pas nécessaire pour Forge Java

---

## 🎮 Commandes In-Game (une fois dans Minecraft)

### Commandes AI Test (/aitest)

#### 1. Changer le mode IA
```
/aitest mode goals
```
Change le mode IA en mode "Goals" (système simple basé sur objectifs)

```
/aitest mode behaviortree
```
Change le mode IA en mode "Behavior Tree" (système avancé avec arbres de comportement)

**Résultat attendu**: Message de confirmation + tous les villageois à proximité passent au nouveau mode

---

#### 2. Obtenir les infos d'une entité IA
```
/aitest info
```
Affiche les informations du mob le plus proche:
- UUID de l'entité
- Mode IA actuel (GOALS ou BEHAVIOR_TREE)
- Objectif actuel (si en mode GOALS)
- Nombre d'objectifs actifs

**Résultat attendu**:
```
=== AI Info ===
Entity: Villager (UUID: ...)
Mode: BEHAVIOR_TREE
Current Goal: Sleep
Active Goals: 3
```

---

#### 3. Compter les entités gérées par l'IA
```
/aitest count
```
Affiche le nombre total d'entités actuellement gérées par le système IA

**Résultat attendu**: `Total AI-managed entities: 5`

---

## 🧪 Tests Fonctionnels

### Test 1: Architecture IA Modulable (5.0)

**Objectif**: Vérifier que le système Goals fonctionne

1. Spawn plusieurs villageois:
   ```
   /summon minecraft:villager ~ ~ ~
   ```

2. Passer en mode Goals:
   ```
   /aitest mode goals
   ```

3. Vérifier les infos:
   ```
   /aitest info
   ```

4. Observer les comportements:
   - **PatrolGoal**: Le villageois patrouille entre des points
   - **FollowPlayerGoal**: Le villageois suit le joueur le plus proche
   - **CollectResourcesGoal**: Le villageois ramasse les items au sol

**Résultat attendu**: Les villageois exécutent différents objectifs selon les priorités

---

### Test 2: Pathfinding Avancé (5.1)

**Objectif**: Vérifier que le pathfinding A* fonctionne avec cache et async

1. Spawn un villageois loin de vous
2. Passer en mode Goals (utilise le pathfinding avancé):
   ```
   /aitest mode goals
   ```

3. Observer le villageois:
   - Il doit calculer un chemin vers ses points de patrouille
   - Le pathfinding doit éviter les obstacles (lave, feu)
   - Le calcul doit être fait de manière asynchrone (pas de freeze)

4. Jeter des items au sol:
   ```
   /give @p minecraft:diamond 64
   ```
   Puis jeter les diamants au sol (touche Q)

5. Le villageois devrait calculer un chemin vers les items et les ramasser

**Résultat attendu**:
- Pas de lag pendant le calcul de chemin
- Le villageois évite les dangers (lave, feu)
- Les chemins sont mis en cache (observable via les logs si debug activé)

---

### Test 3: Persistance des Données (5.2)

**Objectif**: Vérifier que les données IA sont sauvegardées

1. Spawn un villageois:
   ```
   /summon minecraft:villager ~ ~ ~
   ```

2. Configurer son IA (les changements seront sauvegardés automatiquement):
   ```
   /aitest mode behaviortree
   ```

3. Vérifier les données:
   ```
   /aitest info
   ```

4. Quitter le monde et le recharger

5. Revérifier les données:
   ```
   /aitest info
   ```

**Résultat attendu**:
- Les données IA persistent après redémarrage
- Le mode IA est conservé
- Les objectifs et états sont restaurés

**Fichiers de sauvegarde**: Les données sont stockées via le système Capability de Forge dans les fichiers de monde

---

### Test 4: Interface GUI (5.3)

**Objectif**: Vérifier que la GUI de configuration fonctionne

**⚠️ Note**: La GUI nécessite une activation via un item ou une touche (à configurer)

1. Ouvrir la GUI de configuration IA (méthode à définir - par défaut via un item spécial ou keybind)

2. Dans la GUI, vous devriez voir:
   - Boutons pour activer/désactiver les objectifs:
     - ✅/❌ Patrol Goal
     - ✅/❌ Follow Player Goal
     - ✅/❌ Collect Resources Goal

3. Cliquer sur les boutons pour activer/désactiver les objectifs

4. Observer le changement de comportement en temps réel

**Résultat attendu**:
- Les boutons changent d'état (✅ ↔ ❌)
- Les objectifs sont activés/désactivés immédiatement
- Les changements sont synchronisés avec le serveur

---

## 🔬 Tests Avancés

### Test 5: Behavior Tree (gdx-ai)

**Objectif**: Vérifier le système avancé avec arbres de comportement

1. Passer en mode Behavior Tree:
   ```
   /aitest mode behaviortree
   ```

2. Observer le cycle quotidien d'un villageois:
   - **Matin (6h-12h)**: Cherche de la nourriture
   - **Midi (12h-18h)**: Travaille (craft, farm)
   - **Après-midi (18h-24h)**: Socialise avec d'autres villageois
   - **Nuit (0h-6h)**: Dort dans un lit

3. Changer l'heure pour tester:
   ```
   /time set day     # 6h du matin
   /time set noon    # Midi
   /time set night   # 18h (début de nuit)
   /time set midnight # Minuit
   ```

**Résultat attendu**: Le villageois change de comportement selon l'heure

---

### Test 6: Performance et Optimisation

**Objectif**: Vérifier que le système n'impacte pas les performances

1. Spawn 10 villageois:
   ```
   /execute as @p run summon minecraft:villager ~1 ~ ~
   /execute as @p run summon minecraft:villager ~2 ~ ~
   /execute as @p run summon minecraft:villager ~3 ~ ~
   # ... répéter 10 fois
   ```

2. Activer le mode Behavior Tree pour tous:
   ```
   /aitest mode behaviortree
   ```

3. Vérifier le compteur:
   ```
   /aitest count
   ```

4. Observer les FPS (F3 pour debug screen)

**Résultat attendu**:
- FPS stable (>30)
- Pas de lag visible
- Le pathfinding asynchrone empêche les freezes
- Collections fastutil optimisent la mémoire (15-20% plus rapide)

---

### Test 7: Pathfinding Cache

**Objectif**: Vérifier que le cache de pathfinding fonctionne

1. Activer les logs debug (dans le fichier de config ou via commande)

2. Faire patrouiller un villageois plusieurs fois sur le même trajet

3. Observer les logs console

**Résultat attendu**:
- Premier calcul: "Calculating path from X to Y"
- Calculs suivants: "Using cached path from X to Y"
- Cache limité à 100 chemins (LRU)

---

## 📊 Vérifications de Code

### Vérifier que les dépendances sont installées

```bash
./gradlew dependencies | grep -E "gdx-ai|fastutil"
```

**Résultat attendu**:
```
+--- com.badlogicgames.gdx:gdx-ai:1.8.2
+--- it.unimi.dsi:fastutil:8.5.12
```

---

### Vérifier la compilation

```bash
./gradlew compileJava
```

**Résultat attendu**: `BUILD SUCCESSFUL` sans erreurs

---

## 🐛 Débogage

### Logs importants à vérifier

Dans les logs Minecraft (`logs/latest.log`), rechercher:

```bash
grep -i "AI" logs/latest.log
grep -i "pathfind" logs/latest.log
grep -i "behavior" logs/latest.log
```

**Logs attendus**:
- `[AITickHandler] Registered AI for entity: Villager`
- `[PathfindingManager] Path calculated in Xms`
- `[BehaviorManager] Switching to mode: BEHAVIOR_TREE`

---

### En cas d'erreur

1. **Erreur de dépendances**:
   ```bash
   ./gradlew --refresh-dependencies build
   ```

2. **Erreur de cache**:
   ```bash
   ./gradlew clean
   rm -rf build/
   ./gradlew build
   ```

3. **Erreur de permissions (macOS)**:
   ```bash
   chmod +x gradlew
   ```

4. **Problème LWJGL natives (macOS M1/M2/M3)**:
   Vérifier que Java 21 ARM64 est utilisé:
   ```bash
   java -version | grep aarch64
   ```

---

## ✅ Checklist de Validation

### 5.0 - Architecture IA Modulable
- [ ] AIGoal.java existe et compile
- [ ] AIGoalManager gère plusieurs objectifs
- [ ] PatrolGoal fonctionne
- [ ] FollowPlayerGoal fonctionne
- [ ] CollectResourcesGoal fonctionne
- [ ] BehaviorTree via gdx-ai fonctionne
- [ ] BehaviorManager peut switcher entre modes

### 5.1 - Pathfinding Avancé
- [ ] AdvancedPathfinder utilise A*
- [ ] PathfindingManager gère async
- [ ] Cache de 100 chemins LRU
- [ ] Évite les dangers (lave, feu)
- [ ] Utilise fastutil pour optimisation
- [ ] Pas de freeze pendant calcul

### 5.2 - Persistance
- [ ] AIData avec tous les champs
- [ ] Sauvegarde NBT fonctionne
- [ ] Capability attachée aux entities
- [ ] Données restaurées après reload
- [ ] JSON config supporté

### 5.3 - GUI
- [ ] AIConfigScreen s'ouvre
- [ ] Boutons enable/disable objectifs
- [ ] Packets client-server fonctionnent
- [ ] Changements appliqués en temps réel
- [ ] GUI synchronisée avec état serveur

---

## 📝 Notes Additionnelles

### Dépendances installées
- ✅ **gdx-ai 1.8.2**: Behavior Trees, State Machines
- ✅ **fastutil 8.5.12**: Collections optimisées (15-20% plus rapide)
- ⏳ **GeckoLib 5.2.1**: Préparé pour 5.4 (animations 3D) - commenté pour l'instant

### Commandes futures (5.4+)
Ces commandes seront disponibles après implémentation des tâches futures:
- `/aianimation play <anim>` - Pour GeckoLib (5.4)
- `/aiinteract <action>` - Interactions environnement (5.5)
- `/aistats` - Statistiques performance (5.6)
- `/aisync` - Test synchronisation multiplayer (5.7)
- `/aichat <message>` - Communication avec IA (5.8)
- `/aieconomy` - Système économie/réputation (5.9)

---

**Document créé le**: 2025-01-05
**Système testé**: Tâches 5.0-5.3 du système d'IA modulable
**Version Minecraft**: 1.21
**Version Forge**: 51.0.33
**Java**: 21 (Temurin ARM64 pour macOS Apple Silicon)

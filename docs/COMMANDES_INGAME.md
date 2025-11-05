# 🎮 Commandes In-Game - Test du Système d'IA

Toutes les commandes à utiliser **dans Minecraft** pour tester le système d'IA (tâches 5.0-5.3).

---

## 📋 Commandes de Base `/aitest`

### 1. Changer le mode IA - Goals (Simple)
```
/aitest mode goals
```
**Résultat**: Tous les villageois proches passent en mode "Goals" (objectifs simples)

---

### 2. Changer le mode IA - Behavior Tree (Avancé)
```
/aitest mode behaviortree
```
**Résultat**: Tous les villageois proches passent en mode "Behavior Tree" (IA complexe avec cycle jour/nuit)

---

### 3. Obtenir les infos d'une entité IA
```
/aitest info
```
**Résultat**: Affiche les infos du mob le plus proche (type, UUID, mode IA, objectif actuel)

---

### 4. Compter les entités gérées
```
/aitest count
```
**Résultat**: Affiche le nombre total d'entités avec IA active

---

## 🧪 Tests Fonctionnels

### Test 1: Spawn des villageois
```
/summon minecraft:villager ~ ~ ~
```
Spawn un villageois à votre position

**Pour spawn plusieurs villageois**:
```
/summon minecraft:villager ~1 ~ ~
/summon minecraft:villager ~-1 ~ ~
/summon minecraft:villager ~ ~ ~1
/summon minecraft:villager ~ ~ ~-1
/summon minecraft:villager ~2 ~ ~
```

---

### Test 2: Mode Goals (Objectifs Simples)

1. Spawn des villageois:
```
/summon minecraft:villager ~ ~ ~
```

2. Activer le mode Goals:
```
/aitest mode goals
```

3. Observer les comportements:
   - **Patrouille**: Le villageois marche entre différents points
   - **Suivre joueur**: Le villageois suit le joueur le plus proche
   - **Ramasser items**: Le villageois ramasse les objets au sol

4. Tester le ramassage - donnez-vous des items:
```
/give @p minecraft:diamond 64
```
Puis jetez-les au sol (touche Q) et observez le villageois les ramasser

---

### Test 3: Mode Behavior Tree (IA Avancée)

1. Spawn un villageois:
```
/summon minecraft:villager ~ ~ ~
```

2. Activer le mode Behavior Tree:
```
/aitest mode behaviortree
```

3. Changer l'heure pour tester le cycle jour/nuit:

**Matin (cherche nourriture)**:
```
/time set 0
```

**Midi (travaille)**:
```
/time set 6000
```

**Après-midi (socialise)**:
```
/time set 12000
```

**Nuit (dort)**:
```
/time set 18000
```

4. Observer le changement de comportement selon l'heure

---

### Test 4: Pathfinding Avancé

1. Spawn un villageois loin de vous:
```
/summon minecraft:villager ~20 ~ ~20
```

2. Activer mode Goals:
```
/aitest mode goals
```

3. Observer:
   - Le villageois calcule un chemin intelligent
   - Évite les obstacles (lave, feu, eau profonde)
   - Pas de lag pendant le calcul (pathfinding asynchrone)

4. Tester avec obstacles - créer un mur de lave:
```
/fill ~5 ~ ~5 ~10 ~2 ~5 minecraft:lava
```

Le villageois doit contourner la lave automatiquement

---

### Test 5: Persistance des Données

1. Spawn un villageois:
```
/summon minecraft:villager ~ ~ ~
```

2. Configurer son IA:
```
/aitest mode behaviortree
```

3. Vérifier les infos:
```
/aitest info
```

4. **Sauvegarder et quitter le monde**

5. **Recharger le monde**

6. Revérifier les infos:
```
/aitest info
```

**Résultat attendu**: Le mode IA est conservé après rechargement

---

### Test 6: Performance avec Plusieurs Villageois

1. Spawn 10 villageois rapidement:
```
/summon minecraft:villager ~1 ~ ~
/summon minecraft:villager ~2 ~ ~
/summon minecraft:villager ~3 ~ ~
/summon minecraft:villager ~-1 ~ ~
/summon minecraft:villager ~-2 ~ ~
/summon minecraft:villager ~-3 ~ ~
/summon minecraft:villager ~ ~ ~1
/summon minecraft:villager ~ ~ ~2
/summon minecraft:villager ~ ~ ~3
/summon minecraft:villager ~ ~ ~-1
```

2. Activer Behavior Tree pour tous:
```
/aitest mode behaviortree
```

3. Vérifier le compteur:
```
/aitest count
```

4. Observer les FPS (appuyer sur F3)

**Résultat attendu**: FPS stable, pas de lag

---

## 🎯 Comportements Attendus par Mode

### Mode GOALS (Simple)

**PatrolGoal** (Priorité: 1):
- Le villageois marche entre 3 points de patrouille
- Change de point toutes les 100-200 ticks
- Utilise le pathfinding avancé A*

**FollowPlayerGoal** (Priorité: 2):
- Suit le joueur le plus proche (rayon: 16 blocs)
- S'arrête à 3 blocs du joueur
- Regarde le joueur pendant qu'il le suit

**CollectResourcesGoal** (Priorité: 3):
- Cherche les items au sol (rayon: 16 blocs)
- Se déplace vers l'item le plus proche
- Ramasse l'item en arrivant

---

### Mode BEHAVIOR_TREE (Avancé)

**Cycle quotidien automatique**:

| Heure (ticks) | Période | Comportement |
|---------------|---------|--------------|
| 0-6000 | Matin (6h-12h) | Cherche nourriture |
| 6000-12000 | Midi (12h-18h) | Travaille (craft/farm) |
| 12000-18000 | Après-midi (18h-0h) | Socialise avec autres villageois |
| 18000-24000 | Nuit (0h-6h) | Dort dans un lit |

**Comportements intelligents**:
- Cherche automatiquement un lit la nuit
- Évite les dangers (zombies, squelettes, lave)
- Interagit avec les workstations pendant la journée
- Socialise uniquement s'il y a d'autres villageois proches

---

## 🔍 Vérifications Visuelles

### Ce que vous devriez voir:

**Mode Goals**:
- ✅ Villageois marche de manière fluide vers ses objectifs
- ✅ Change d'objectif selon les priorités
- ✅ Suit le joueur s'il est proche
- ✅ Ramasse les items au sol automatiquement

**Mode Behavior Tree**:
- ✅ Comportement change selon l'heure du jour
- ✅ Cherche un lit quand la nuit tombe
- ✅ Reste près de son lit/workstation pendant la journée
- ✅ Regarde/interagit avec autres villageois l'après-midi

**Pathfinding**:
- ✅ Pas de freeze/lag pendant calcul de chemin
- ✅ Évite les obstacles intelligemment
- ✅ Contourne la lave, le feu, l'eau profonde
- ✅ Prend des chemins optimaux (A*)

---

## 🐛 En Cas de Problème

### Le villageois ne bouge pas
```
/aitest mode goals
```
Force le redémarrage de l'IA

---

### Vérifier si l'IA est active
```
/aitest info
```
Doit afficher les infos du villageois le plus proche

---

### Réinitialiser tous les villageois
```
/kill @e[type=minecraft:villager]
```
Puis spawn de nouveaux villageois:
```
/summon minecraft:villager ~ ~ ~
```

---

### L'heure ne change pas l'IA
Vérifier le mode:
```
/aitest info
```
Doit être en mode **BEHAVIOR_TREE** pour le cycle jour/nuit

Si en mode GOALS, passer en Behavior Tree:
```
/aitest mode behaviortree
```

---

## 📊 Résumé des Tests

### ✅ Checklist de Validation In-Game

**5.0 - Architecture IA**:
- [ ] `/aitest mode goals` fonctionne
- [ ] `/aitest mode behaviortree` fonctionne
- [ ] `/aitest info` affiche les données
- [ ] Les villageois ont des comportements visibles

**5.1 - Pathfinding**:
- [ ] Villageois trouve son chemin intelligemment
- [ ] Contourne les obstacles (lave, murs)
- [ ] Pas de lag pendant calcul de chemin
- [ ] Chemins optimisés (ligne droite quand possible)

**5.2 - Persistance**:
- [ ] Mode IA conservé après reload du monde
- [ ] `/aitest info` montre les mêmes données après reload
- [ ] Objectifs/états restaurés correctement

**5.3 - GUI** (à tester quand la GUI sera activée):
- [ ] GUI s'ouvre avec item/keybind
- [ ] Boutons enable/disable objectifs fonctionnent
- [ ] Changements visibles en temps réel

---

**📝 Note**: Ces commandes testent uniquement les fonctionnalités 5.0-5.3 implémentées.
Les fonctionnalités 5.4+ (animations 3D, économie, etc.) auront leurs propres commandes.

---

**Document créé le**: 2025-01-05
**Pour tester**: Système d'IA modulable (tâches 5.0-5.3)
**Version**: Minecraft 1.21 - Forge 51.0.33

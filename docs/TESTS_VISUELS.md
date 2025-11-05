# 🎯 Tests Visuels - Comment VOIR si l'IA Fonctionne

## 🤔 Le Problème
Vous tapez `/aitest info` et ça dit que l'IA est active, mais le villageois bouge normalement... Comment savoir si c'est VOTRE IA ou juste Minecraft ?

---

## ✅ Test #1 : Le Test du Diamant (TRÈS VISIBLE)

### Ce que vous faites:
```
1. Spawn un villageois loin de tout item
   /summon minecraft:villager ~ ~ ~

2. Activez l'IA Goals
   /aitest mode goals

3. Donnez-vous des diamants
   /give @p minecraft:diamond 64

4. Jetez TOUS les diamants loin du villageois (15 blocs)
   (Touche Q en regardant loin)
```

### 🎬 Ce que vous DEVEZ voir:

**❌ Villageois SANS votre IA (vanilla):**
- Il marche au hasard
- Il NE VA PAS vers les diamants
- Il peut passer à côté sans les ramasser

**✅ Villageois AVEC votre IA (Goals):**
- Il ARRÊTE ce qu'il fait
- Il SE DIRIGE EN LIGNE DROITE vers les diamants
- Il les RAMASSE dès qu'il arrive
- **CollectResourcesGoal** est actif → il cherche les items

### 📊 Vérification:
```
/aitest info
```
Vous devriez voir:
```
Current Goal: Collect Resources  ← IL CHERCHE LES ITEMS !
```

---

## ✅ Test #2 : Le Test du Suivi (TRÈS VISIBLE)

### Ce que vous faites:
```
1. Spawn un villageois
   /summon minecraft:villager ~ ~ ~

2. Activez l'IA Goals
   /aitest mode goals

3. ÉLOIGNEZ-VOUS de 10 blocs

4. ATTENDEZ 5 secondes

5. Le villageois doit venir vers vous
```

### 🎬 Ce que vous DEVEZ voir:

**❌ Villageois SANS votre IA:**
- Il vous regarde mais reste sur place
- Il marche au hasard, pas spécialement vers vous

**✅ Villageois AVEC votre IA:**
- Il COMMENCE à marcher vers vous
- Il S'ARRÊTE à 3 blocs de vous
- Il VOUS REGARDE constamment
- **FollowPlayerGoal** est actif → il vous suit

### 📊 Vérification:
```
/aitest info
```
Vous devriez voir:
```
Current Goal: Follow Player  ← IL VOUS SUIT !
```

---

## ✅ Test #3 : Le Test de la Patrouille (MOYEN VISIBLE)

### Ce que vous faites:
```
1. Spawn un villageois dans un espace VIDE (pas de village)
   /summon minecraft:villager ~ ~ ~

2. Activez l'IA Goals
   /aitest mode goals

3. RESTEZ LOIN (20 blocs)

4. Observez pendant 30 secondes
```

### 🎬 Ce que vous DEVEZ voir:

**❌ Villageois SANS votre IA:**
- Marche au hasard
- Pas de pattern particulier
- Change de direction aléatoirement

**✅ Villageois AVEC votre IA:**
- Marche vers un POINT PRÉCIS
- S'arrête quelques secondes
- Marche vers un AUTRE point précis
- **PatrolGoal** est actif → il patrouille entre 3 points

### 📊 Vérification:
```
/aitest info
```
Vous devriez voir:
```
Current Goal: Patrol  ← IL PATROUILLE !
Active Goals: 3       ← Il a 3 objectifs actifs
```

---

## ✅ Test #4 : Le Test Jour/Nuit (BEHAVIOR TREE)

### Ce que vous faites:
```
1. Spawn un villageois AVEC un lit à côté
   /summon minecraft:villager ~ ~ ~
   /setblock ~2 ~ ~ minecraft:red_bed

2. Activez l'IA Behavior Tree
   /aitest mode behaviortree

3. Passez à la NUIT
   /time set 18000

4. Observez
```

### 🎬 Ce que vous DEVEZ voir:

**❌ Villageois SANS votre IA:**
- Cherche un lit mais c'est le comportement vanilla
- Pas de différence notable

**✅ Villageois AVEC votre IA:**
- Va DIRECTEMENT au lit
- S'endort
- Tapez `/aitest info` :
```
Mode: BEHAVIOR_TREE
Current Goal: Sleep  ← IL DORT SELON VOTRE IA !
```

### Test du MATIN:
```
/time set 0
```
- Il se réveille
- Va chercher de la nourriture
```
Current Goal: Find Food  ← IL CHERCHE À MANGER !
```

---

## 🔥 Test ULTIME : Le Test de Comparaison

### Setup:
```
1. Spawn 2 villageois côte à côte
   /summon minecraft:villager ~ ~ ~
   /summon minecraft:villager ~2 ~ ~

2. Activez l'IA SEULEMENT pour les proches
   /aitest mode goals

3. Tuez le villageois #1 et respawn-en un nouveau
   /kill @e[type=minecraft:villager,limit=1,sort=nearest]
   /summon minecraft:villager ~ ~ ~
```

### Résultat:
- **Villageois #2** : A votre IA (Goals actif)
- **Villageois #1** : N'a PAS votre IA (nouveau spawn)

### Test:
```
Jetez des diamants entre les 2
```

**Vous DEVEZ voir:**
- Villageois #2 → Va chercher les diamants (IA active)
- Villageois #1 → Marche au hasard (IA inactive)

---

## 🎯 Checklist de Vérification Rapide

Après `/aitest mode goals`, testez dans l'ordre:

### ✅ Étape 1 : Vérifiez que l'IA est attachée
```
/aitest info
```
**Attendu**:
```
Mode: GOALS
Active Goals: 3
```

### ✅ Étape 2 : Test du ramassage (30 secondes)
```
/give @p minecraft:diamond 64
Jetez-les loin (touche Q)
```
**Attendu**: Le villageois marche vers les diamants

### ✅ Étape 3 : Test du suivi (30 secondes)
```
Éloignez-vous de 10 blocs
Attendez
```
**Attendu**: Le villageois vous suit

### ✅ Étape 4 : Vérifiez le changement d'objectif
```
/aitest info
(Tapez plusieurs fois pendant qu'il bouge)
```
**Attendu**: `Current Goal` change entre:
- `Patrol`
- `Follow Player`
- `Collect Resources`

---

## 🐛 Si Rien Ne Marche

### Le villageois bouge "normalement"
**Cause possible**: L'IA est attachée mais les Goals ne fonctionnent pas

**Solution**:
```bash
# Vérifiez les logs
grep -i "goal" logs/latest.log
grep -i "aigoal" logs/latest.log
```

**Vous devriez voir**:
```
[AIGoalManager] Registered PatrolGoal for Villager
[AIGoalManager] Registered FollowPlayerGoal for Villager
[AIGoalManager] Registered CollectResourcesGoal for Villager
```

### `/aitest info` ne montre rien
**Cause**: Le système Capability ne fonctionne pas

**Solution**: Vérifiez les logs au démarrage:
```bash
grep -i "capability" logs/latest.log
```

---

## 📊 Tableau Récapitulatif

| Test | Commande | Résultat Attendu | Temps |
|------|----------|------------------|-------|
| **Diamant** | Jetez diamants loin | Va les chercher | 30s |
| **Suivi** | Éloignez-vous | Vous suit | 30s |
| **Patrouille** | Restez loin | Marche entre points | 1min |
| **Info** | `/aitest info` | Mode + Goals affichés | Instant |
| **Jour/Nuit** | `/time set 18000` | Va dormir | 30s |

---

## 🎓 Ce Que Vous Devez Retenir

### Comportement Vanilla (sans votre IA):
- Marche au hasard
- Cherche un lit la nuit (comportement de base)
- Ignore les items au sol (sauf nourriture pour breeding)
- Ne suit PAS le joueur

### Comportement avec votre IA (Goals):
- Marche de manière DIRIGÉE (vers items, joueur, points de patrouille)
- **Ramasse TOUS les items** au sol (pas juste nourriture)
- **Suit le joueur** activement
- Change d'objectif selon les priorités

### Comportement avec votre IA (Behavior Tree):
- Cycle jour/nuit **contrôlé par votre code**
- Cherche nourriture le matin (votre AI)
- Travaille à midi (votre AI)
- Dort la nuit (votre AI)

---

**Si après ces tests vous ne voyez AUCUNE différence**, alors il y a un problème dans l'implémentation des Goals et on devra debugger le code ! 🔧

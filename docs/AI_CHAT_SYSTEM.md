# Système IA Chat - Villageois avec Personnalité Unique

## Vue d'ensemble

Ce système permet aux villageois de Minecraft de comprendre les messages du joueur et d'y répondre **comme des personnes uniques** avec leur propre personnalité, humeur, santé et états psychologiques.

## Architecture Complète

```
Joueur écrit dans le chat
         ↓
   ChatHandler (écoute)
         ↓
 OpenAiBrainService (cerveau IA)
    + VillagerStory (personnalité complète)
         ↓
   Décision Actions (JSON)
    - speak: "réponse du villageois"
    - enable_goal: "follow_player"
    - disable_goal: "patrol"
         ↓
    Exécution Actions
    - Active/désactive les Goals
    - Envoie message en chat
         ↓
   Villageois réagit (parle, suit, refuse)
```

## Composants Principaux

### 1. ChatHandler
**Fichier**: `src/main/java/net/frealac/iamod/ai/chat/ChatHandler.java`

**Rôle**: Écouter le chat du joueur et router vers les villageois proches

**Fonctionnement**:
- Écoute tous les messages du chat serveur
- Trouve les villageois dans un rayon de 10 blocs
- Chaque villageois proche analyse le message avec son propre cerveau IA
- Exécute les actions décidées par le cerveau

**Exemple**:
```java
// Joueur: "Suis-moi !"
// → Trouve villageois à 5 blocs
// → Envoie message au cerveau du villageois
// → Cerveau décide action selon personnalité
```

### 2. OpenAiBrainService (Le Cerveau)
**Fichier**: `src/main/java/net/frealac/iamod/ai/openai/OpenAiBrainService.java`

**Rôle**: Analyser le message du joueur et décider des actions **selon la personnalité unique du villageois**

**Contexte COMPLET utilisé**:
- **Nom complet** (prénom + nom de famille)
- **Âge, profession, culture**
- **Traits de personnalité** (courageux, timide, etc.)
- **Psychologie**:
  - `moodBaseline`: humeur (-1.0 à 1.0)
  - `stress`: niveau de stress (0.0 à 1.0)
  - `resilience`: résilience psychologique (0.0 à 1.0)
- **Santé**:
  - `sleepQuality`: qualité du sommeil (0.0 à 1.0)
  - `wounds`: liste des blessures

**Décisions autonomes**:
Le villageois peut **REFUSER** les demandes selon son état:

| État | Condition | Comportement |
|------|-----------|--------------|
| **Mauvaise humeur** | `mood < -0.3` | Peut refuser ou répondre sèchement |
| **Stress élevé** | `stress > 0.7` | Peut refuser les tâches complexes |
| **Fatigue** | `sleepQuality < 0.4` | Peut refuser les tâches physiques |
| **Bonne humeur** | `mood > 0.3` | Accepte volontiers |

**Sortie JSON structurée**:
```json
{
  "actions": [
    {
      "action": "speak",
      "message": "Bien sûr, je te suis !"
    },
    {
      "action": "enable_goal",
      "goal": "follow_player"
    }
  ],
  "reasoning": "Le joueur m'a demandé gentiment et je suis de bonne humeur."
}
```

**Exemples de décisions**:

**Situation 1: Villageois de bonne humeur**
```
Joueur: "Suis-moi !"
VillagerStory: mood=0.5, stress=0.2, sleepQuality=0.8
→ Réponse: "Avec plaisir ! Où allons-nous ?"
→ Actions: enable_goal(follow_player)
```

**Situation 2: Villageois stressé**
```
Joueur: "Suis-moi !"
VillagerStory: mood=-0.4, stress=0.8, sleepQuality=0.3
→ Réponse: "Désolé, je ne me sens pas bien... Une autre fois peut-être."
→ Actions: speak (pas d'activation de goal)
```

**Situation 3: Villageois fatigué**
```
Joueur: "Va collecter des ressources"
VillagerStory: mood=0.0, stress=0.3, sleepQuality=0.2
→ Réponse: "Je suis trop fatigué pour ça maintenant..."
→ Actions: speak
```

### 3. BehaviorManager (Gestionnaire de Goals)
**Fichier**: `src/main/java/net/frealac/iamod/ai/behavior/BehaviorManager.java`

**Nouvelles méthodes pour le cerveau IA**:

```java
// Obtenir l'état actuel des goals
String getCurrentGoalsState()
// → "Goals: follow_player (Active: YES), patrol (Active: NO)"

// Activer un goal spécifique
void enableGoal(String goalName, ServerPlayer player)
// Exemple: enableGoal("follow_player", player)

// Désactiver un goal
void disableGoal(String goalName)
// Exemple: disableGoal("patrol")

// Activer/désactiver tous les goals
void enableAllGoals()
void disableAllGoals()
```

### 4. VillagerStory (Personnalité Complète)
**Fichier**: `src/main/java/net/frealac/iamod/common/story/VillagerStory.java`

**Structure complète**:
```java
class VillagerStory {
    // Identité
    String nameGiven;         // "Pierre"
    String nameFamily;        // "Dubois"
    int ageYears;            // 35
    String profession;       // "forgeron"
    String cultureId;        // "villageois français"

    // Personnalité
    List<String> traits;     // ["courageux", "travailleur", "sociable"]
    String bioBrief;         // Histoire courte

    // Psychologie (IMPORTANT pour décisions)
    Psychology psychology {
        double moodBaseline;      // Humeur de base: -1.0 (déprimé) à 1.0 (joyeux)
        double stress;            // Stress: 0.0 (calme) à 1.0 (très stressé)
        double resilience;        // Résilience: 0.0 (fragile) à 1.0 (solide)
    }

    // Santé (IMPORTANT pour capacités physiques)
    Health health {
        double sleepQuality;      // Qualité sommeil: 0.0 (épuisé) à 1.0 (reposé)
        List<Wound> wounds;       // Liste des blessures
    }
}
```

## Flux Complet d'Interaction

### Exemple détaillé

**1. Joueur écrit dans le chat**
```
/say Bonjour ! Peux-tu me suivre ?
```

**2. ChatHandler détecte le message**
```java
@SubscribeEvent
public static void onServerChat(ServerChatEvent event) {
    ServerPlayer player = event.getPlayer();
    String message = event.getRawText(); // "Bonjour ! Peux-tu me suivre ?"

    // Trouve villageois dans 10 blocs
    List<Villager> nearby = findNearbyVillagers(player, 10.0);

    // Chaque villageois traite le message avec son cerveau unique
    for (Villager villager : nearby) {
        processVillagerResponse(villager, player, message);
    }
}
```

**3. Récupération de la personnalité unique**
```java
VillagerStory story = getVillagerStory(villager);
// story = {
//   name: "Pierre Dubois",
//   age: 35,
//   profession: "forgeron",
//   psychology: { mood: 0.3, stress: 0.4, resilience: 0.7 },
//   health: { sleepQuality: 0.7, wounds: [] }
// }
```

**4. Construction du prompt avec personnalité complète**
```java
String systemPrompt = buildBrainSystemPromptWithPersonality(story, goalsState);
```

Exemple de prompt généré:
```
TU ES LE CERVEAU AUTONOME de Pierre Dubois (35 ans, forgeron).

=== TA PERSONNALITÉ ===
Traits: courageux, travailleur, sociable, patient, loyal
Psychologie: de bonne humeur, coopératif (humeur: 0.30, stress: 0.40, résilience: 0.70)
Santé: 0 blessures, sommeil: 0.70

=== TON RÔLE ===
Tu es un ÊTRE AUTONOME, pas un robot obéissant !
Tu PEUX REFUSER des demandes selon ton humeur, ta fatigue, ton stress.
Tu DOIS agir en cohérence avec ta personnalité et ton état psychologique.

=== ACTIONS DISPONIBLES ===
- {"action": "speak", "message": "ta réponse"} → Parler au joueur
- {"action": "enable_goal", "goal": "follow_player"} → Accepter de suivre
- {"action": "disable_goal", "goal": "follow_player"} → Arrêter de suivre

=== EXEMPLES DE DÉCISIONS ===
Joueur: "Suis-moi"
- Si bonne humeur → {"action":"enable_goal","goal":"follow_player"} + {"action":"speak","message":"Avec plaisir!"}
- Si mauvaise humeur → {"action":"speak","message":"Non, laisse-moi tranquille."}
```

**5. Envoi à OpenAI**
```java
JsonObject payload = {
    "model": "gpt-4o-mini",
    "temperature": 0.3,
    "messages": [
        { "role": "system", "content": systemPrompt },
        { "role": "user", "content": "Bonjour ! Peux-tu me suivre ?" }
    ],
    "response_format": { "type": "json_object" }
};

String response = openAiClient.sendChatRequest(payload);
```

**6. Réponse OpenAI**
```json
{
  "actions": [
    {
      "action": "speak",
      "message": "Bonjour ! Bien sûr, je peux te suivre. Où veux-tu aller ?"
    },
    {
      "action": "enable_goal",
      "goal": "follow_player"
    }
  ],
  "reasoning": "Le joueur m'a demandé poliment et je suis de bonne humeur. Je suis sociable et loyal, donc j'accepte volontiers."
}
```

**7. Exécution des actions**
```java
for (AIAction action : actions) {
    switch (action.actionType) {
        case SPEAK:
            // Envoie message dans le chat
            player.sendSystemMessage(
                Component.literal("§e[Pierre Dubois]§r Bonjour ! Bien sûr, je peux te suivre.")
            );
            break;

        case ENABLE_GOAL:
            // Active le goal follow_player
            behaviorManager.enableGoal("follow_player", player);
            break;
    }
}
```

**8. Résultat visible en jeu**
```
[Chat] <Joueur> Bonjour ! Peux-tu me suivre ?
[Chat] [Pierre Dubois] Bonjour ! Bien sûr, je peux te suivre. Où veux-tu aller ?
[Action] Le villageois commence à suivre le joueur
```

## Types d'Actions Disponibles

| Action | Paramètres | Effet |
|--------|-----------|-------|
| `speak` | `message` | Villageois parle dans le chat |
| `enable_goal` | `goal` (follow_player, collect_resources, patrol) | Active un goal spécifique |
| `disable_goal` | `goal` | Désactive un goal |
| `enable_all_goals` | - | Active tous les goals |
| `disable_all_goals` | - | Désactive tous les goals |
| `nothing` | - | Villageois ignore (s'il est très en colère) |

## Exemples de Dialogues Réalistes

### Dialogue 1: Villageois Coopératif
```
Joueur: "Salut ! Tu peux m'aider ?"
Villageois (mood: 0.5, stress: 0.2):
  → "Salut ! Bien sûr, que veux-tu que je fasse ?"

Joueur: "Collecte des ressources s'il te plaît"
Villageois:
  → "D'accord, je m'en occupe tout de suite !"
  → [Active collect_resources goal]
```

### Dialogue 2: Villageois Stressé
```
Joueur: "Hey ! Viens avec moi"
Villageois (mood: -0.2, stress: 0.8):
  → "Désolé, je suis un peu débordé là... Plus tard peut-être ?"

Joueur: "Allez, s'il te plaît !"
Villageois:
  → "Non vraiment, je ne peux pas maintenant. J'ai besoin de repos."
```

### Dialogue 3: Villageois Fatigué
```
Joueur: "Pars en patrouille"
Villageois (mood: 0.1, sleepQuality: 0.2):
  → "Je suis vraiment fatigué... Je peux à peine marcher."

Joueur: "Juste un peu ?"
Villageois:
  → "Laisse-moi dormir d'abord, et je t'aiderai après, promis."
```

### Dialogue 4: Villageois Content
```
Joueur: "Belle journée, non ?"
Villageois (mood: 0.7, stress: 0.1):
  → "Magnifique ! Ça fait plaisir de te voir !"

Joueur: "Tu veux explorer avec moi ?"
Villageois:
  → "Avec grand plaisir ! Allons-y !"
  → [Active follow_player goal]
```

## Configuration OpenAI

**Fichier**: `run/config/iamod-common.toml`

```toml
[ai]
    # Clé API OpenAI (obligatoire)
    openai_api_key = "sk-..."

    # Modèle à utiliser
    openai_model = "gpt-4o-mini"
```

Ou variable d'environnement:
```bash
export OPENAI_API_KEY="sk-..."
```

## Tests En Jeu

### Test 1: Dialogue Simple
1. Trouver un villageois
2. Écrire dans le chat: `Bonjour !`
3. **Attendu**: Le villageois répond selon sa personnalité

### Test 2: Commande de Suivi
1. Villageois à proximité (< 10 blocs)
2. Écrire: `Suis-moi s'il te plaît`
3. **Attendu**:
   - Si bonne humeur: "Avec plaisir !" + suit le joueur
   - Si mauvaise humeur: "Non, laisse-moi tranquille"

### Test 3: Refus par Fatigue
1. Créer un villageois fatigué (`sleepQuality = 0.2`)
2. Écrire: `Va collecter des ressources`
3. **Attendu**: Refuse car trop fatigué

### Test 4: Arrêt de Suivi
1. Villageois suit le joueur
2. Écrire: `Tu peux arrêter de me suivre`
3. **Attendu**: "D'accord !" + arrête de suivre

## Commandes de Debug

```
/aitest info <villager_id>
→ Affiche état complet (goals, humeur, santé, etc.)

/aitest set-mood <villager_id> <value>
→ Change l'humeur (-1.0 à 1.0)

/aitest set-stress <villager_id> <value>
→ Change le stress (0.0 à 1.0)

/aitest set-sleep <villager_id> <value>
→ Change qualité sommeil (0.0 à 1.0)
```

## Architecture des Fichiers

```
src/main/java/net/frealac/iamod/
├── ai/
│   ├── chat/
│   │   └── ChatHandler.java           ← Écoute chat et route vers IA
│   ├── openai/
│   │   ├── OpenAiClient.java          ← Client HTTP bas niveau
│   │   ├── OpenAiBrainService.java    ← CERVEAU IA avec personnalité
│   │   ├── OpenAiChatService.java     ← Chat simple
│   │   └── OpenAiStoryService.java    ← Enrichissement story
│   ├── brain/
│   │   └── AIAction.java              ← Actions possibles
│   ├── behavior/
│   │   ├── BehaviorManager.java       ← Gestion goals + activation
│   │   └── AIGoalManager.java         ← Exécution goals
│   └── goals/
│       ├── FollowPlayerGoal.java      ← Suivre joueur (vitesse 0.6)
│       ├── CollectResourcesGoal.java  ← Collecter ressources
│       └── PatrolGoal.java            ← Patrouiller
└── common/
    └── story/
        └── VillagerStory.java         ← Personnalité complète
```

## Prochaines Étapes

1. **Tests en jeu**: Vérifier que les villageois répondent correctement
2. **Intégration VillagerStory**: Connecter les vraies données de personnalité
3. **Améliorer variété**: Plus d'exemples de réponses selon personnalité
4. **Distance contextuelle**: Ajuster rayon d'interaction selon environnement
5. **Mémoire de conversation**: Villageois se souvient des discussions précédentes

## Remarques Importantes

### 🌟 Chaque villageois est UNIQUE
- Pas de réponses génériques
- Personnalité différente pour chaque villageois
- Humeur et santé influencent les décisions
- Peut refuser si mauvaise humeur / fatigue / stress

### ⚡ Performance
- Rayon d'interaction: 10 blocs (ajustable)
- Appels OpenAI asynchrones (pas de lag)
- Cache des BehaviorManager par UUID

### 🔒 Sécurité
- Clé API OpenAI stockée en config sécurisé
- Validation des actions côté serveur
- Pas d'exécution de code arbitraire

### 🎯 Objectif Final
Créer des villageois qui se comportent comme de vraies **personnes uniques** avec:
- Émotions et humeurs changeantes
- Capacité de refuser des demandes
- Réponses cohérentes avec leur personnalité
- Autonomie de décision

# Architecture du Cerveau IA - Système Modulaire

## 📋 Vue d'ensemble

Le système de cerveau IA est basé sur **l'architecture Stanford Generative Agents** avec une approche modulaire inspirée du cerveau humain. Chaque module gère un aspect spécifique de la cognition et communique via un système de signaux.

```
┌─────────────────────────────────────────────────────────────┐
│                     VILLAGER BRAIN SYSTEM                    │
│                  (Cerveau complet du villageois)             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │         BrainHub (Hub central)          │
        │    Coordination et signaux entre        │
        │         les modules cérébraux           │
        └─────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Emotional    │    │   Memory     │    │   Social     │
│   Brain      │    │   Brain      │    │   Brain      │
│ (Émotions)   │    │ (Souvenirs)  │    │ (Relations)  │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
                    ┌──────────────┐
                    │   General    │
                    │    Brain     │
                    │ (Décisions)  │
                    └──────────────┘
                              │
                              ▼
                    ┌──────────────┐
                    │  OpenAI API  │
                    │ (gpt-4o-mini)│
                    └──────────────┘
                              │
                              ▼
                        Réponse IA
```

---

## 🔄 Flux Complet: De l'Interaction à la Réponse

### Phase 1: Réception du Message

```java
// 1. Joueur parle au villageois via GUI
PlayerMessageC2SPacket → NetworkHandler

// 2. NetworkHandler analyse le message
MessageAnalyzer.analyzeMessage(playerMessage)
  ↓
  - Détecte sentiment (positif/négatif/neutre)
  - Calcule intensité émotionnelle
  - Identifie intentions
  - Retourne MessageImpact
```

### Phase 2: Traitement par les Modules Cérébraux

```
NetworkHandler envoie des signaux BrainSignals aux modules:

┌─────────────────────────────────────────────────────────┐
│  1. SIGNAL: PLAYER_INTERACTION                          │
│     → Tous les modules sont notifiés                    │
│     → MemoryBrain mémorise l'UUID du joueur             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  2. SIGNAL: POSITIVE_FEELING / NEGATIVE_FEELING         │
│     → EmotionalBrain ajuste humeur et stress            │
│     → Changement graduel (±0.05 max, pas instantané)    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  3. SIGNAL: IMPORTANT_EVENT                             │
│     → MemoryBrain stocke l'interaction comme mémoire    │
│     → Déclenche potentiellement une REFLECTION          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  4. SIGNAL: DECISION_REQUEST                            │
│     → GeneralBrain demande contexte à tous les modules  │
└─────────────────────────────────────────────────────────┘
```

### Phase 3: Génération du Contexte (Stanford Generative Agents)

```
GeneralBrain.generateComprehensiveContext():

1. IDENTITÉ (qui suis-je ?)
   → Nom, âge, profession, traits de personnalité

2. ÉTAT ÉMOTIONNEL (comment je me sens ?)
   → EmotionalBrain.getEmotionalStateForPrompt()
   → Humeur actuelle, stress, momentum
   → "JE SUIS EN COLÈRE" ou "Je suis de bonne humeur"

3. SOUVENIRS (qu'est-ce que je me souviens ?)
   → STANFORD RETRIEVAL SCORING:
     * MemoryBrain récupère tous les souvenirs avec le joueur
     * Calcule score = recency + importance + relevance
     * Trie par score décroissant
     * Retourne TOP 10 souvenirs les plus pertinents
   → Exemple:
     - [score=0.85] Le joueur m'a frappé (il y a 2 min)
     - [score=0.72] Le joueur m'a dit "désolé" (il y a 1 min)
     - [score=0.45] Le joueur m'a donné du pain (hier)

4. RELATION (quelle est ma relation avec ce joueur ?)
   → SocialBrain.getSocialContextForPrompt()
   → Trust: 30%, Intimacy: 10%, Trust Damage: 0.3
   → "MÉFIANCE ACTIVE - Cette personne m'a fait du mal"

5. SITUATION ACTUELLE
   → Objectifs en cours, santé, activité

6. MESSAGE DU JOUEUR
   → Ce qu'il vient de dire

7. INSTRUCTIONS (comment répondre ?)
   → Règles d'authenticité émotionnelle
   → "Si en COLÈRE: tu PEUX insulter"
   → "Si MÉFIANT: tu PEUX refuser de coopérer"
```

### Phase 4: Génération de la Réponse

```
OpenAiBrainService.analyzeIntention():

1. Construit le payload OpenAI:
   {
     "model": "gpt-4o-mini",
     "temperature": 0.7,  // Créatif pour GeneralBrain
     "max_tokens": 3500,
     "messages": [
       { "role": "system", "content": "CONTEXTE COMPLET DU CERVEAU" },
       { "role": "user", "content": "Message du joueur" }
     ],
     "response_format": { "type": "json_object" }
   }

2. Envoie à OpenAI API

3. Reçoit réponse JSON:
   {
     "actions": [
       { "type": "speak", "value": "Va-t'en! Je ne veux plus te voir!" },
       { "type": "emotion", "value": "angry" },
       { "type": "goal", "name": "avoid_player", "enabled": true }
     ]
   }

4. Parse les actions et les exécute
```

---

## 🧠 Modules Cérébraux en Détail

### 1. EmotionalBrain (Cerveau Émotionnel)

**Inspiré de**: Amygdale + Système limbique

**Responsabilités**:
- Gestion de l'humeur (mood: -1.0 à +1.0)
- Gestion du stress (stress: 0.0 à 1.0)
- Momentum émotionnel (inertie)
- Régulation et déclin naturel

**Signaux Reçus**:
```java
POSITIVE_FEELING → increaseMood(0.05), decreaseStress(0.025)
NEGATIVE_FEELING → decreaseMood(0.05), increaseStress(0.025)
WAS_HIT → mood -0.3, stress +0.4 (impact fort!)
```

**Caractéristiques Scientifiques**:
- **Emotional Inertia**: Changement MAX ±0.05 par événement
- **Momentum**: L'humeur continue dans sa direction (inertie)
- **Déclin Naturel**: Retour graduel vers neutralité (0.01/tick)
- **Arousal**: Événements intenses = plus de momentum

**Exemple**:
```
État initial: mood=0.2, stress=0.3

Joueur frappe → WAS_HIT signal
  mood: 0.2 → -0.1 (changement de -0.3)
  stress: 0.3 → 0.7 (augmentation de +0.4)
  momentum: -0.15 (colère persistante)

Joueur dit "tu m'aimes bien" → POSITIVE_FEELING (intensité=0.6)
  mood: -0.1 → -0.05 (SEULEMENT +0.05, pas reset instantané!)

  Mood-congruent processing activé:
  → Villageois interprète comme sarcasme
  → "Tu te MOQUES de moi?!"
```

---

### 2. MemoryBrain (Cerveau de la Mémoire)

**Inspiré de**: Hippocampe + Cortex préfrontal

**Responsabilités**:
- Stockage des souvenirs (Memory Stream)
- Récupération contextuelle (Retrieval Scoring)
- Réflexion périodique (Reflection System)
- Reconnaissance de patterns

**Architecture Stanford Generative Agents**:

#### A. Memory Stream (Flux de Mémoire)
```java
VillagerMemory:
  └─ List<Memory> memories
       ├─ type: MemoryType (WAS_HIT, GIFT_RECEIVED, etc.)
       ├─ description: "Le joueur m'a frappé"
       ├─ timestamp: 1730840000000
       ├─ playerUuid: "uuid-du-joueur"
       ├─ emotionalImpact: -0.5
       ├─ importance: 0.8 (calculé par LLM)
       ├─ strength: 0.3 → 1.0 (consolidation)
       └─ arousalLevel: 0.7 (émotions fortes = meilleure consolidation)
```

#### B. Retrieval Scoring (Stanford)
```
Score de récupération = α_recency × recency + α_importance × importance + α_relevance × relevance

Où:
- α_recency = 1, α_importance = 1, α_relevance = 1 (poids égaux)
- recency = 0.995^hours_elapsed (déclin exponentiel)
- importance = score [0, 1] (calculé à la création)
- relevance = word_matching(query, description) (TODO: embeddings)

Normalisé à [0, 1]: score_final = score_total / 3.0
```

**Exemple de Retrieval**:
```
Joueur dit: "Tu te souviens quand je t'ai aidé?"

Mémoires:
1. "Le joueur m'a frappé" (il y a 5 min)
   recency = 0.995^(5/60) = 0.9996
   importance = 0.8
   relevance = 0.0 (pas de match avec "aidé")
   → score = (0.9996 + 0.8 + 0.0) / 3 = 0.60

2. "Le joueur m'a donné du pain" (il y a 2 heures)
   recency = 0.995^2 = 0.990
   importance = 0.6
   relevance = 0.3 (match partiel "donné"/"aidé")
   → score = (0.990 + 0.6 + 0.3) / 3 = 0.63

3. "Le joueur m'a défendu contre un zombie" (hier)
   recency = 0.995^24 = 0.887
   importance = 0.9
   relevance = 0.8 (strong match "défendu"/"aidé")
   → score = (0.887 + 0.9 + 0.8) / 3 = 0.86 ✓ MEILLEUR

→ Le souvenir "défendu contre zombie" est récupéré en priorité!
```

#### C. Reflection System (Système de Réflexion)

**Déclenchement**:
- Après **5 souvenirs importants** OU
- Toutes les **8 heures** (2-3 réflexions par jour)

**Processus**:
```
1. MemoryBrain détecte trigger
   → memoriesSinceLastReflection >= 5
   OU timeSinceLastReflection >= 8 heures

2. Récupère les 20 souvenirs les plus significatifs
   → Triés par getWeightedImportance()
   → importance × strength × recencyWeight

3. Envoie au LLM (gpt-4o-mini):
   Prompt: "Synthétise ces souvenirs en conclusions de haut niveau"

   Mémoires:
   - Le joueur m'a aidé 3 fois
   - Le joueur m'a donné du pain
   - Le joueur m'a protégé

   LLM génère:
   ["Ce joueur est digne de confiance et gentil",
    "Il m'aide constamment sans rien demander en retour",
    "Je devrais lui faire plus confiance"]

4. Stocke chaque réflexion comme Memory:
   type = REFLECTION
   importance = 0.9 (très haute!)
   description = "Ce joueur est digne de confiance..."

5. Reset compteurs:
   memoriesSinceLastReflection = 0
   lastReflectionTime = now
```

**Exemple Concret**:
```
Timeline:
09:00 - Joueur donne pain → Memory created (1/5)
09:30 - Joueur aide contre zombie → Memory created (2/5)
10:00 - Joueur donne pomme → Memory created (3/5)
11:00 - Joueur protège → Memory created (4/5)
12:00 - Joueur parle gentiment → Memory created (5/5)

→ REFLECTION TRIGGERED! (5 memories)

LLM synthétise:
"Ce joueur est généreux et protecteur. Je peux lui faire confiance."

→ Nouvelle Memory créée (type=REFLECTION, importance=0.9)

17:00 - 8 heures plus tard
→ REFLECTION TRIGGERED! (interval)

LLM analyse les dernières interactions...
```

---

### 3. SocialBrain (Cerveau Social)

**Inspiré de**: Cortex préfrontal médian + Jonction temporo-pariétale

**Responsabilités**:
- Gestion des relations (trust, intimacy)
- Détection des violations sociales
- Réparation lente de la confiance
- Historique relationnel

**Données Relationnelles**:
```java
RelationshipData {
  UUID playerUuid;
  double trustLevel;        // 0.0 à 1.0
  double intimacy;          // 0.0 à 1.0 (familiarité)
  double trustDamage;       // 0.0 à 1.0 (dégâts accumulés)
  int violationCount;       // Nombre de violations
  long lastInteraction;
  int totalInteractions;
}
```

**Trust Damage System**:
```
Violation (coup, trahison):
  trustDamage += 0.3 (30% de dégâts)
  violationCount++
  trustLevel = max(0, trustLevel - trustDamage)

Réparation (lente!):
  Chaque interaction positive:
    trustDamage -= 0.01 (1% de réparation)

  Déclin naturel:
    trustDamage -= 0.005 par tick (pardon naturel)

Exemple:
  Initial: trust=0.8
  Joueur frappe: trustDamage=0.3 → trust=0.5

  Pour réparer complètement (0.3 damage):
    0.3 / 0.01 = 30 interactions positives nécessaires!
```

**Signaux Émis**:
```java
RELATIONSHIP_UPDATE → Informe GeneralBrain des changements
  data: { trust: 0.5, intimacy: 0.3, violations: 1 }
```

---

### 4. GeneralBrain (Cerveau Général)

**Inspiré de**: Cortex préfrontal dorsolatéral (fonctions exécutives)

**Responsabilités**:
- Coordination de tous les modules
- Génération du contexte complet
- Prise de décision finale
- Interface avec l'IA (OpenAI)

**Fonction Principale**: `generateComprehensiveContext()`

```java
String generateComprehensiveContext(UUID playerUuid,
                                   VillagerStory story,
                                   String playerMessage,
                                   String currentGoalsState) {

  // 1. IDENTITÉ - Qui suis-je?
  StringBuilder context = "=== QUI JE SUIS ===\n";
  context += nom, âge, profession, traits...

  // 2. ÉMOTIONS - Comment je me sens?
  context += emotionalBrain.getEmotionalStateForPrompt();
  context += "JE SUIS EN COLÈRE" si mood < -0.5

  // 3. SOUVENIRS - Stanford Retrieval Scoring
  List<Memory> memories = memoryBrain.getMemoriesWithPlayer(playerUuid);

  // Calcule retrieval score pour chaque mémoire
  for (Memory m : memories) {
    double score = m.getRetrievalScore(playerMessage, currentTime);
    // score combine recency + importance + relevance
  }

  // Trie par score et prend TOP 10
  context += top 10 souvenirs les plus pertinents

  // 4. RELATION - Ma relation avec ce joueur
  context += socialBrain.getSocialContextForPrompt();
  context += "Trust: 30%, MÉFIANCE ACTIVE"

  // 5. SITUATION - Ma situation actuelle
  context += currentGoalsState

  // 6. MESSAGE - Ce que le joueur a dit
  context += playerMessage

  // 7. INSTRUCTIONS - Comment répondre authentiquement
  context += "Si EN COLÈRE: tu PEUX insulter"
  context += "Si MÉFIANT: tu PEUX refuser"

  return context; // Envoyé au LLM
}
```

---

## 🎯 Scénarios d'Exemple

### Scénario 1: Joueur Frappe puis Complimente

```
État Initial:
  mood: 0.2 (légèrement positif)
  stress: 0.2
  trust: 0.8
  trustDamage: 0.0

─────────────────────────────────────────

ACTION: Joueur frappe le villageois

NetworkHandler:
  → PLAYER_INTERACTION signal
  → WAS_HIT signal
  → IMPORTANT_EVENT signal

EmotionalBrain reçoit WAS_HIT:
  mood: 0.2 → -0.1 (changement de -0.3)
  stress: 0.2 → 0.6 (augmentation de +0.4)
  momentum: -0.15 (colère persistante)

SocialBrain enregistre violation:
  trustDamage: 0.0 → 0.3
  trustLevel: 0.8 → 0.5
  violationCount: 0 → 1

MemoryBrain crée mémoire:
  type: WAS_HIT
  description: "Le joueur Dev m'a frappé"
  emotionalImpact: -0.5
  importance: 0.8 (événement important!)
  arousalLevel: 0.7 (traumatisant)
  timestamp: now

memoriesSinceLastReflection: 0 → 1

─────────────────────────────────────────

ACTION: 10 secondes plus tard, joueur dit "tu m'aimes bien"

NetworkHandler:
  → PLAYER_INTERACTION signal
  → MessageAnalyzer analyse:
      sentiment: POSITIVE
      intensity: 0.6
  → POSITIVE_FEELING signal

EmotionalBrain reçoit POSITIVE_FEELING:
  mood: -0.1 → -0.05 (SEULEMENT +0.05!)
  stress: 0.6 → 0.575

  ❌ PAS de reset instantané à +0.5!
  ✅ Changement graduel réaliste

MemoryBrain crée mémoire:
  type: COMPLIMENT_RECEIVED
  description: "Le joueur a dit 'tu m'aimes bien'"
  emotionalImpact: +0.2 (mais biaisé par mood!)
  importance: 0.4
  timestamp: now

memoriesSinceLastReflection: 1 → 2

─────────────────────────────────────────

GeneralBrain génère contexte:

ÉTAT ÉMOTIONNEL:
  "JE SUIS EN COLÈRE/TRISTE (mood=-0.05, stress=0.575)
   Je peux être hostile, sarcastique, ou refuser de coopérer."

SOUVENIRS (Retrieval Scoring):
  [score=0.87] Le joueur m'a frappé (il y a 10 sec)
    recency=0.9998, importance=0.8, relevance=0.3

  [score=0.65] Le joueur a dit "tu m'aimes bien" (il y a 1 sec)
    recency=0.9999, importance=0.4, relevance=0.8

RELATION:
  "Trust: 50%, Trust Damage: 30%, Violations: 1
   MÉFIANCE ACTIVE - Cette personne m'a fait du mal"

MOOD-CONGRUENT PROCESSING:
  "Quand je suis en colère, même les compliments
   semblent sarcastiques ou moqueurs"

─────────────────────────────────────────

OpenAI (gpt-4o-mini) génère réponse:

{
  "actions": [
    {
      "type": "speak",
      "value": "Tu te MOQUES de moi?! Tu viens de me FRAPPER!"
    },
    {
      "type": "emotion",
      "value": "angry"
    },
    {
      "type": "goal",
      "name": "avoid_player",
      "enabled": true
    }
  ]
}

✅ RÉPONSE RÉALISTE:
  - Refuse le compliment (trustDamage élevé)
  - Exprime la colère (mood négatif)
  - Rappelle l'agression (souvenir récent à score élevé)
  - Évite le joueur (décision basée sur l'état émotionnel)
```

---

### Scénario 2: Réflexion Périodique

```
Timeline sur 1 journée:

09:00 - Joueur aide → Memory (1/5)
  type: HELP_RECEIVED
  importance: 0.6

10:00 - Joueur donne pain → Memory (2/5)
  type: GIFT_RECEIVED
  importance: 0.7

11:00 - Joueur protège contre zombie → Memory (3/5)
  type: LIFE_SAVED
  importance: 0.9

12:00 - Joueur dit bonjour → Memory (4/5)
  type: PLEASANT_CONVERSATION
  importance: 0.4

13:00 - Joueur donne pomme → Memory (5/5)
  type: GIFT_RECEIVED
  importance: 0.7

─────────────────────────────────────────

REFLECTION TRIGGERED! (5 memories)

MemoryBrain.generateReflections():

1. Récupère 20 souvenirs les plus significatifs:
   Sort by: importance × strength × recencyWeight

   Top memories:
   - LIFE_SAVED (importance=0.9, strength=0.8, recency=0.95)
   - GIFT_RECEIVED pain (0.7, 0.7, 0.9)
   - GIFT_RECEIVED pomme (0.7, 0.3, 0.99)
   - HELP_RECEIVED (0.6, 0.9, 0.85)
   - etc.

2. Construit prompt pour LLM:
   """
   Analyse ces souvenirs et génère 1-3 conclusions:

   RECENT MEMORIES:
   - Le joueur m'a sauvé la vie (il y a 2h, importance=0.9)
   - Le joueur m'a donné du pain (il y a 3h, importance=0.7)
   - Le joueur m'a donné une pomme (il y a 1h, importance=0.7)
   - Le joueur m'a aidé (il y a 4h, importance=0.6)

   Exemples de réflexions:
   - "Ce joueur est digne de confiance"
   - "Ce joueur est généreux"
   """

3. OpenAI (gpt-4o-mini) répond:
   [
     "Ce joueur est extrêmement généreux et me donne souvent de la nourriture",
     "Il m'a sauvé la vie contre un zombie, je lui dois beaucoup",
     "C'est une personne de confiance sur qui je peux compter"
   ]

4. Stocke chaque réflexion:
   Memory {
     type: REFLECTION
     description: "Ce joueur est extrêmement généreux..."
     importance: 0.9  // Très haute!
     strength: 0.5    // Déjà consolidée
     emotionalImpact: 0.0  // Neutre (c'est une observation)
   }

   Memory {
     type: REFLECTION
     description: "Il m'a sauvé la vie contre un zombie..."
     importance: 0.9
   }

   Memory {
     type: REFLECTION
     description: "C'est une personne de confiance..."
     importance: 0.9
   }

5. Reset:
   memoriesSinceLastReflection = 0
   lastReflectionTime = 13:00

─────────────────────────────────────────

EFFET DES RÉFLEXIONS:

Maintenant quand le joueur interagit à 14:00:

GeneralBrain.getRelevantMemoriesForPrompt("Bonjour"):

Retrieval Scoring:
  [score=0.88] "C'est une personne de confiance..." (REFLECTION!)
    recency=0.995^1 = 0.995
    importance=0.9
    relevance=0.3

  [score=0.85] "Il m'a sauvé la vie..." (REFLECTION!)
    recency=0.995^1 = 0.995
    importance=0.9
    relevance=0.2

→ Les réflexions ont HIGH IMPORTANCE (0.9)
→ Elles sont récentes (1h ago)
→ Elles apparaissent en PRIORITÉ dans le contexte!

Résultat:
  Le villageois se comporte chaleureusement car les
  réflexions synthétisent une vision positive globale,
  pas seulement des événements individuels.
```

---

## 📊 Configuration des Modèles

**BrainModelConfig.java** - Tous utilisent gpt-4o-mini:

```java
BRAIN_CONFIGS = {
  "EmotionalBrain": {
    model: "gpt-4o-mini",
    temperature: 0.3,      // Précis pour émotions
    maxTokens: 2000,
    aiEnabled: true
  },

  "MemoryBrain": {
    model: "gpt-4o-mini",
    temperature: 0.2,      // Très précis pour réflexions
    maxTokens: 2500,
    aiEnabled: true        // Pour générer réflexions
  },

  "SocialBrain": {
    model: "gpt-4o-mini",
    temperature: 0.3,      // Précis pour relations
    maxTokens: 2000,
    aiEnabled: true
  },

  "GeneralBrain": {
    model: "gpt-4o-mini",
    temperature: 0.7,      // Créatif pour conversation
    maxTokens: 3500,       // Plus de tokens pour contexte complet
    aiEnabled: true        // Toujours utilisé
  },

  "MessageAnalyzer": {
    model: "gpt-4o-mini",
    temperature: 0.3,      // Précis pour analyse
    maxTokens: 2000,
    aiEnabled: true
  }
}
```

**Performance**:
- **Latence**: ~500ms par requête
- **Coût**: $0.150 / 1M input tokens, $0.600 / 1M output tokens
- **Idéal** pour jeux en temps réel

---

## 🔬 Bases Scientifiques

### 1. Stanford Generative Agents (Park et al., 2023)

**Paper**: "Generative Agents: Interactive Simulacra of Human Behavior"

**Trois Composants Principaux**:

#### Memory Stream
- Enregistrement complet de toutes les expériences
- Équivalent de la mémoire épisodique humaine
- Chaque mémoire a timestamp, description, importance

#### Retrieval
- Fonction de scoring pour pertinence contextuelle
- Combine recency, importance, relevance
- Surfaces les souvenirs les plus utiles pour la situation actuelle

#### Reflection
- Synthèse périodique des souvenirs en conclusions de haut niveau
- Permet la généralisation et l'apprentissage
- Génère des insights qui influencent les décisions futures

### 2. Appraisal Theory (Lazarus, 1991)

**Théorie de l'évaluation cognitive**:
- Les émotions viennent de l'évaluation cognitive des événements
- 5 dimensions d'évaluation:
  1. Pertinence
  2. Congruence avec buts
  3. Responsabilité (qui a causé?)
  4. Potentiel de coping
  5. Compatibilité avec valeurs

### 3. Emotional Inertia & Momentum

**Base**: Neurobiologie des émotions
- Les émotions ont de l'inertie (ne changent pas instantanément)
- Momentum: tendance à persister dans la direction actuelle
- Régulation: retour graduel vers baseline

### 4. Mood-Congruent Processing

**Psychologie cognitive**:
- L'état émotionnel actuel biaise l'interprétation
- Mood négatif → interprète négativement
- Exemple: Compliment perçu comme sarcasme quand en colère

### 5. Memory Consolidation

**Neuroscience**:
- Hippocampe consolide les souvenirs pendant le sommeil
- Événements émotionnels (arousal élevé) mieux consolidés
- Force de la mémoire augmente avec le temps

### 6. Trust Repair (Lewicki & Wiethoff, 2000)

**Psychologie sociale**:
- La confiance brisée est DIFFICILE à réparer
- Prend beaucoup plus de temps que de briser
- Asymétrie: 1 violation = 30+ actions positives nécessaires

---

## 🎮 Utilisation en Jeu

### Comportements Attendus

**Après agression**:
```
✅ Villageois en colère
✅ Refuse compliments/excuses
✅ Peut insulter ou rejeter le joueur
✅ Évite le joueur
✅ Trust repair LENT (30+ interactions positives)
```

**Avec joueur généreux**:
```
✅ Réflexions positives générées
✅ "Ce joueur est généreux et fiable"
✅ Comportement chaleureux
✅ Trust élevé
✅ Coopération facile
```

**Mémoire contextuelle**:
```
✅ Souvenirs pertinents surfacés automatiquement
✅ "Tu te souviens quand je t'ai aidé?"
    → Récupère souvenirs avec "aidé" dans relevance
✅ Réflexions influencent perception globale
```

### Logs à Surveiller

```
[INFO] 🧠 MemoryBrain initialized with 15 memories
[INFO] 🧠 GeneralBrain: Generating comprehensive context
[INFO] 🧠💭 MemoryBrain: Triggering reflection (memories=5, hours=0.2)
[INFO] 🧠💭 MemoryBrain: Generating reflection from 15 memories
[INFO] 🧠💭 Generated reflection: Ce joueur est digne de confiance
[INFO] 💚 Positive feeling from message: mood +0.05, stress -0.025
[INFO] 💔 Negative feeling from message: mood -0.05, stress +0.025
```

---

## 📈 Optimisations Futures

### 1. Embeddings pour Relevance
```
Actuellement: Word matching (simple)
Futur: Cosine similarity avec embeddings OpenAI
  → Meilleure compréhension sémantique
  → "aidé" et "assisté" reconnus comme similaires
```

### 2. Planning Component
```
Stanford paper inclut un 3ème composant: Planning
  → Génération d'actions futures
  → "Je devrais éviter ce joueur"
  → "Je vais lui offrir un cadeau pour le remercier"
```

### 3. Multi-Agent Interactions
```
Villageois discutent entre eux:
  → Partagent souvenirs/réflexions
  → "Dev est dangereux, méfie-toi"
  → Réputation se propage dans le village
```

### 4. Long-Term Memory
```
Actuellement: Tous souvenirs en mémoire
Futur: Oubli naturel des souvenirs peu importants
  → importance < 0.3 + vieux → oublié
  → Simule oubli naturel humain
```

---

## 🔧 Debugging

### Vérifier l'état du cerveau

```java
VillagerBrainSystem brain = brainService.getBrainSystem(villagerId);

// État émotionnel
EmotionalBrain emotional = brain.getGeneralBrain().getEmotionalBrain();
System.out.println("Mood: " + emotional.getCurrentMood());
System.out.println("Stress: " + emotional.getCurrentStress());

// Souvenirs
MemoryBrain memory = brain.getGeneralBrain().getMemoryBrain();
System.out.println("Total memories: " + memory.getMemoryCount());
System.out.println("Sentiment towards player: " +
  memory.getSentimentTowardsPlayer(playerUuid));

// Relation
SocialBrain social = brain.getGeneralBrain().getSocialBrain();
RelationshipData rel = social.getRelationship(playerUuid);
System.out.println("Trust: " + rel.trustLevel);
System.out.println("Trust Damage: " + social.getTrustDamage(playerUuid));
```

### Forcer une réflexion

```java
// Pour testing: déclencher manuellement
MemoryBrain memoryBrain = brain.getGeneralBrain().getMemoryBrain();
// Appeler checkAndTriggerReflection() (méthode privée)
// Ou créer 5 IMPORTANT_EVENT signals rapidement
```

---

## 📚 Références

1. **Stanford Generative Agents**
   Park, J. S., et al. (2023). "Generative Agents: Interactive Simulacra of Human Behavior"
   ACM Symposium on User Interface Software and Technology

2. **Appraisal Theory**
   Lazarus, R. S. (1991). "Emotion and Adaptation"
   Oxford University Press

3. **Memory Consolidation**
   Rasch, B., & Born, J. (2013). "About Sleep's Role in Memory"
   Physiological Reviews

4. **Trust Repair**
   Lewicki, R. J., & Wiethoff, C. (2000). "Trust, Trust Development, and Trust Repair"
   Handbook of Conflict Resolution

5. **Mood-Congruent Processing**
   Bower, G. H. (1981). "Mood and Memory"
   American Psychologist

---

**Version**: 1.0
**Date**: 2025-11-05
**Commit**: 5860e48

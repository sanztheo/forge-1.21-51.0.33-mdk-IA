# Système de Mémoire Avancé - Villageois avec Souvenirs

## Vue d'ensemble

Chaque villageois possède maintenant une **mémoire complète** de toutes ses interactions. Il se souvient de :
- **Qui tu es** (ton nom)
- **Ce que tu lui as fait** (frappé, aidé, insulté, donné des cadeaux)
- **Ce qu'il a vu** (violence, événements traumatisants)
- **Ses conversations** avec toi

Cette mémoire **influence directement** ses décisions et comportements futurs.

## Fonctionnement Multijoueur

Le système est **100% compatible multijoueur** :

**Exemple concret** :
```
Villageois "Pierre"

Joueur 1 (Theo):
 - A frappé Pierre (il y a 2h)
 - Pierre sentiment: -0.6 (très négatif)
 - Réaction: "Toi ! Laisse-moi tranquille !"

Joueur 2 (Alex):
 - A donné un diamant à Pierre (il y a 1h)
 - Pierre sentiment: +0.7 (très positif)
 - Réaction: "Alex ! Content de te voir !"
```

**→ Le même villageois traite chaque joueur différemment selon sa mémoire**

## Architecture

### 1. Types de Souvenirs (MemoryType)

#### Souvenirs Positifs (+)
| Type | Impact | Description |
|------|--------|-------------|
| PLAYER_NAME_LEARNED | +0.1 | Apprend ton nom |
| HELP_RECEIVED | +0.3 | Tu l'as aidé |
| GIFT_RECEIVED | +0.4 | Tu lui as donné un cadeau |
| LIFE_SAVED | +0.8 | Tu lui as sauvé la vie |
| PLEASANT_CONVERSATION | +0.2 | Conversation agréable |
| COMPLIMENT_RECEIVED | +0.2 | Tu l'as complimenté |

#### Souvenirs Négatifs (-)
| Type | Impact | Description |
|------|--------|-------------|
| WAS_HIT | -0.5 | Tu l'as frappé |
| WAS_INSULTED | -0.3 | Tu l'as insulté |
| WAS_BETRAYED | -0.7 | Tu l'as trahi |
| WAS_THREATENED | -0.4 | Tu l'as menacé |
| WITNESSED_VIOLENCE | -0.2 | A vu de la violence |
| PROMISE_BROKEN | -0.3 | Tu n'as pas tenu ta promesse |

#### Souvenirs Neutres
| Type | Impact | Description |
|------|--------|-------------|
| GENERAL_INTERACTION | 0.0 | Interaction standard |
| INFORMATION_SHARED | 0.0 | Information partagée |
| REQUEST_MADE | 0.0 | Demande faite |

### 2. Structure d'un Souvenir (Memory)

```java
class Memory {
    MemoryType type;              // Type de souvenir
    String description;           // Description détaillée
    long timestamp;               // Quand ça s'est passé
    UUID playerUuid;              // UUID du joueur
    String playerName;            // Nom du joueur
    double emotionalImpact;       // Impact émotionnel (-1.0 à +1.0)
    double importance;            // Importance (0.0 à 1.0)
}
```

**Exemple**:
```json
{
  "type": "WAS_HIT",
  "description": "Theo Sanz m'a frappé (dégâts: 3.5) - Je m'en souviendrai !",
  "timestamp": 1699564800000,
  "playerUuid": "uuid-theo",
  "playerName": "Theo Sanz",
  "emotionalImpact": -0.5,
  "importance": 0.8
}
```

### 3. Gestionnaire de Mémoire (VillagerMemory)

Chaque villageois a son propre `VillagerMemory` qui :

#### Stocke jusqu'à 50 mémoires
- Garde les plus importantes
- Garde les plus récentes
- Élimine automatiquement les vieilles mémoires peu importantes

#### Calcule le sentiment envers chaque joueur
```java
double sentiment = getSentimentTowardsPlayer(playerUuid);
// -1.0 : haine totale
//  0.0 : neutre
// +1.0 : amour total
```

**Poids des mémoires** :
- **Récentes** (< 1h) : poids 1.0
- **Aujourd'hui** (< 24h) : poids 0.8
- **Cette semaine** (< 7j) : poids 0.5
- **Anciennes** (> 7j) : poids 0.2

#### Sélectionne les mémoires pour l'IA
Pour éviter de surcharger, seules les 10 mémoires les plus pertinentes sont envoyées à l'IA :
1. **Toutes** les mémoires du joueur actuel
2. Autres mémoires importantes ou récentes

## Captures Automatiques d'Événements

### 1. Coups Reçus (LivingHurtEvent)

**Quand** : Un joueur frappe un villageois

**Mémoire créée** :
```java
MemoryType: WAS_HIT
Description: "Theo m'a frappé (dégâts: 5.0) - Je m'en souviendrai !"
Impact: -0.5
```

**Effets psychologiques** :
```java
mood -= 0.1    // Humeur baisse
stress += 0.2  // Stress augmente
```

**Exemple en jeu** :
```
[Tu frappes le villageois]
[Log] Villager Pierre remembers being hit by Theo
[Log] Villager mood decreased to -0.3, stress increased to 0.7

[Plus tard, tu parles au villageois]
Toi: "Bonjour !"
Villageois: "Toi... reste loin de moi. Tu m'as frappé, je ne t'ai pas oublié."
```

### 2. Cadeaux Reçus (PlayerInteractEvent)

**Quand** : Un joueur donne un objet précieux (diamant, émeraude, or, nourriture)

**Mémoire créée** :
```java
MemoryType: GIFT_RECEIVED
Description: "Alex m'a donné Diamant - Quelle gentillesse !"
Impact: +0.4
```

**Effets psychologiques** :
```java
mood += 0.2    // Humeur monte
stress -= 0.1  // Stress diminue
```

**Exemple en jeu** :
```
[Tu donnes un diamant au villageois]
[Log] Villager Pierre received gift from Alex

[Plus tard]
Toi: "Tu as besoin d'aide ?"
Villageois: "Alex ! Bien sûr, tu m'as tellement aidé avec ce diamant. Que puis-je faire pour toi ?"
```

### 3. Témoin de Violence (LivingDeathEvent)

**Quand** : Un villageois voit un joueur tuer un autre villageois (rayon 16 blocs)

**Mémoire créée** :
```java
MemoryType: WITNESSED_VIOLENCE
Description: "J'ai vu Theo tuer un villageois... C'est horrible !"
Impact: -0.2
```

**Effets psychologiques** :
```java
stress += 0.5  // Stress augmente beaucoup
mood -= 0.3    // Humeur baisse
```

**Exemple en jeu** :
```
[Tu tues un villageois devant Pierre]
[Log] Villager Pierre witnessed killing by Theo

[Tous les villageois proches se souviennent]
[Plus tard, tu parles à Pierre]
Toi: "Salut !"
Villageois: "Tu... tu as tué mon ami... Monster ! Va-t'en !"
```

### 4. Apprentissage du Nom (Chat)

**Quand** : Tu dis "Je m'appelle X" dans le chat

**Mémoire créée** :
```java
MemoryType: PLAYER_NAME_LEARNED
Description: "A appris le nom: Theo Sanz"
Impact: +0.1
```

**Exemple en jeu** :
```
Toi: "Bonjour, je m'appelle Theo Sanz"
Villageois: "Enchanté Theo ! Je m'appelle Pierre."

[Prochaine interaction]
Toi: "Salut !"
Villageois: "Bonjour Theo ! Comment vas-tu ?"
```

## Intégration avec le Cerveau IA

### Prompt Envoyé à l'IA

Quand un joueur parle à un villageois, le cerveau IA reçoit :

```
TU ES Pierre Dubois, 35 ans, forgeron.

=== QUI TU ES ===
Traits de personnalité: courageux, travailleur, sociable
Psychologie: humeur=-0.3, stress=0.7, sommeil=0.4
Santé: 0 blessures, qualité sommeil=0.40

=== TES SOUVENIRS ===
Tu connais Theo Sanz.
Tu n'as pas confiance en cette personne.
Souvenirs récents:
- Theo Sanz m'a frappé (dégâts: 5.0) - Je m'en souviendrai ! (il y a 2 heures)
- Theo Sanz: A dit: 'Suis-moi' - Je l'ai ignoré (il y a 1 heure)
- A dit: 'Bonjour' (il y a quelques minutes)

=== TA SITUATION ACTUELLE ===
Goals: patrol (active), follow_player (inactive)

=== COMMENT TU FONCTIONNES ===
Tu es un ÊTRE VIVANT. Tu as des émotions, de la fatigue, du stress.
Tu réagis NATURELLEMENT selon ton état et ta personnalité.
Tes décisions viennent de QUI TU ES et de tes SOUVENIRS.

Joueur: "Tu peux me suivre ?"
```

**Réponse IA** :
```json
{
  "actions": [
    {"action": "speak", "message": "Te suivre ? Après que tu m'aies frappé il y a quelques heures ? NON. Tu m'as fait mal, je ne te fais plus confiance."}
  ],
  "reasoning": "Ce joueur m'a frappé récemment, j'ai encore mal et je suis stressé. Je refuse catégoriquement."
}
```

## Exemples de Scénarios

### Scénario 1 : Construction de Confiance

```
=== JOUR 1 ===
Theo: "Bonjour !"
Villageois: "Bonjour."
[Mémoire: GENERAL_INTERACTION, sentiment=0.0]

Theo: "Je m'appelle Theo"
Villageois: "Enchanté Theo, je suis Pierre."
[Mémoire: PLAYER_NAME_LEARNED, sentiment=+0.1]

=== JOUR 2 ===
Theo: [Donne un diamant]
Villageois: "Oh ! Merci beaucoup Theo !"
[Mémoire: GIFT_RECEIVED, sentiment=+0.5]

Theo: "Tu peux m'aider à construire ?"
Villageois: "Avec plaisir ! Tu as été si généreux."
[Active follow_player]

=== JOUR 3 ===
Theo: "Salut Pierre !"
Villageois: "Theo ! Mon ami ! Que puis-je faire pour toi aujourd'hui ?"
[Sentiment global: +0.6 - Ami proche]
```

### Scénario 2 : Perte de Confiance

```
=== DÉBUT ===
[Sentiment initial: +0.5 - Ami]

Theo: "Viens avec moi"
Villageois: "Bien sûr Theo !"

=== MILIEU ===
[Theo frappe le villageois par accident]
Villageois: "Aïe ! Pourquoi tu as fait ça ?!"
[Mémoire: WAS_HIT, sentiment: +0.5 → 0.0]

Theo: "Désolé, c'était un accident"
Villageois: "Hmm... d'accord, mais fais attention."
[Mémoire: modérée car expliquée]

=== FIN ===
[Theo refrappe le villageois]
Villageois: "ENCORE ?! C'en est trop ! Va-t'en !"
[Mémoire: WAS_HIT x2, sentiment: 0.0 → -0.5]
[Refuse toute interaction]
```

### Scénario 3 : Témoin et Conséquences

```
=== VILLAGE PAISIBLE ===
Pierre (ami de Theo, sentiment=+0.6)
Marie (ne connaît pas Theo, sentiment=0.0)
Jean (ne connaît pas Theo, sentiment=0.0)

=== THEO TUE MARIE DEVANT PIERRE ET JEAN ===
[Event: LivingDeathEvent]

Pierre: [Témoin]
  Mémoire: WITNESSED_VIOLENCE
  Sentiment: +0.6 → +0.1
  Psychologie: stress=0.8, mood=-0.4
  "Theo... comment as-tu pu ?! Je croyais te connaître..."

Jean: [Témoin]
  Mémoire: WITNESSED_VIOLENCE
  Sentiment: 0.0 → -0.3
  Psychologie: stress=0.9, mood=-0.5
  "Assassin ! Reste loin de moi !"

=== TOUT LE VILLAGE SE SOUVIENT ===
[Réputation: -0.4 auprès de tous les témoins]
[Difficulté: Reconstruire la confiance]
```

### Scénario 4 : Réconciliation

```
=== DÉPART (sentiment=-0.6) ===
Theo: "Salut"
Villageois: "Va-t'en."

=== TENTATIVE 1 ===
Theo: "Je suis désolé de t'avoir frappé"
Villageois: "Des excuses ne suffisent pas."
[Mémoire: REQUEST_MADE - "demande pardon"]

=== TENTATIVE 2 (lendemain) ===
Theo: [Donne émeraude]
Villageois: "... tu essaies de te faire pardonner ?"
[Mémoire: GIFT_RECEIVED, sentiment: -0.6 → -0.3]

=== TENTATIVE 3 (plusieurs jours après) ===
Theo: [Donne diamant]
Villageois: "D'accord... je vois que tu regrettes vraiment."
[Mémoire: GIFT_RECEIVED, sentiment: -0.3 → 0.0]

Theo: "On peut repartir à zéro ?"
Villageois: "Essayons. Mais ne recommence pas."
[Sentiment neutralisé]

=== RECONSTRUCTION ===
[Nouvelles interactions positives]
[Sentiment: 0.0 → +0.2 → +0.4 progressivement]
```

## Données Techniques

### Stockage

```java
// Dans VillagerStory
public transient VillagerMemory interactionMemory;

// Sérialisation NBT
tag.putString("interactionMemory", interactionMemory.toJson());

// Désérialisation NBT
interactionMemory = VillagerMemory.fromJson(tag.getString("interactionMemory"));
```

### Limites

| Paramètre | Valeur | Raison |
|-----------|--------|--------|
| Max mémoires | 50 | Performance + pertinence |
| Mémoires dans prompt IA | 10 | Limite tokens OpenAI |
| Temps oubli complet | Jamais | Réalisme (PTSD possible) |
| Poids mémoire ancienne | 0.2 | Importance réduite |

### Calcul du Sentiment

```
Pour chaque mémoire du joueur:
  impact = mémoire.emotionalImpact * mémoire.importance * recencyWeight

recencyWeight:
  < 1h   : 1.0
  < 24h  : 0.8
  < 7j   : 0.5
  > 7j   : 0.2

sentiment = moyenne pondérée de tous les impacts
```

**Exemple calcul** :
```
Mémoire 1: WAS_HIT, impact=-0.5, importance=0.8, age=2h
  → score = -0.5 * 0.8 * 0.8 = -0.32

Mémoire 2: GIFT_RECEIVED, impact=+0.4, importance=0.8, age=1j
  → score = +0.4 * 0.8 * 0.8 = +0.26

Mémoire 3: PLEASANT_CONVERSATION, impact=+0.2, importance=0.6, age=10min
  → score = +0.2 * 0.6 * 1.0 = +0.12

Sentiment global = (-0.32 + 0.26 + 0.12) / 3 = +0.02 (légèrement positif)
```

## Commandes de Debug

Pour tester le système :

```bash
# Afficher toutes les mémoires d'un villageois
/aitest memories <villager_id>

# Afficher sentiment envers un joueur
/aitest sentiment <villager_id> <player_name>

# Ajouter mémoire manuellement (test)
/aitest add-memory <villager_id> <player_name> <type> "<description>"

# Effacer toutes les mémoires (reset)
/aitest clear-memories <villager_id>

# Voir l'historique complet d'un joueur
/aitest player-history <villager_id> <player_name>
```

## Tests Recommandés

### Test 1 : Apprentissage du Nom
```
1. Parle à un villageois : "Bonjour"
2. Dis "Je m'appelle Theo"
3. Reparle au villageois
Attendu: Il t'appelle par ton nom
```

### Test 2 : Impact des Coups
```
1. Deviens ami avec un villageois (cadeaux)
2. Frappe-le une fois
3. Essaie de lui parler
Attendu: Il est méfiant/en colère
4. Frappe-le plusieurs fois
5. Essaie de lui parler
Attendu: Il refuse de parler
```

### Test 3 : Multijoueur
```
Joueur 1: Frappe le villageois
Joueur 2: Donne un cadeau au villageois

Joueur 1 parle au villageois
Attendu: Réaction négative

Joueur 2 parle au villageois
Attendu: Réaction positive

→ Mêmes villageois, réactions différentes
```

### Test 4 : Témoin
```
1. Deviens ami avec Pierre
2. Tue un autre villageois devant Pierre
3. Parle à Pierre
Attendu: Il a peur, ne te fait plus confiance
```

### Test 5 : Réconciliation
```
1. Frappe un villageois (sentiment=-0.5)
2. Attends quelques minutes
3. Donne des cadeaux (2-3 diamants)
4. Parle gentiment
Attendu: Réconciliation progressive
```

## Optimisations pour Local

Comme le mod est en local sans limites de coûts, j'ai optimisé pour :

1. **Mémoires détaillées** : Descriptions complètes (50-100 caractères)
2. **Prompt riche** : Inclut 10 mémoires au lieu de 5
3. **Psychologie dynamique** : Mood/stress changent en temps réel
4. **Stockage illimité** : 50 mémoires par villageois
5. **Calculs précis** : Sentiment pondéré avec plusieurs facteurs

## Architecture des Fichiers

```
src/main/java/net/frealac/iamod/ai/memory/
├── MemoryType.java                 ← Types de souvenirs
├── Memory.java                     ← Structure d'un souvenir
├── VillagerMemory.java             ← Gestionnaire de mémoire
└── MemoryEventListener.java       ← Capture automatique d'événements

src/main/java/net/frealac/iamod/common/story/
└── VillagerStory.java              ← Intégration mémoire

src/main/java/net/frealac/iamod/ai/openai/
└── OpenAiBrainService.java         ← Cerveau IA avec mémoire

src/main/java/net/frealac/iamod/ai/chat/
└── ChatHandler.java                ← Chat + ajout mémoires auto
```

## Conclusion

Le système de mémoire transforme les villageois en **personnages vivants** qui :
- **Se souviennent** de tout ce que tu leur fais
- **Réagissent différemment** selon leur historique avec toi
- **Ont des rancunes** ou de l'affection à long terme
- **Témoignent** de ce qu'ils voient
- **Pardonnent** (ou pas) selon leur personnalité

En **multijoueur**, chaque joueur construit sa propre relation unique avec chaque villageois !

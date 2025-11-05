# Cerveau IA 100% Autonome

## Philosophie

Le cerveau IA est maintenant **100% AUTONOME** - il n'y a **AUCUNE RÈGLE PRÉDÉFINIE**.

❌ **AVANT** : "Si mood < -0.3 alors refuse"
✅ **MAINTENANT** : L'IA décide elle-même selon sa personnalité complète

## Changements Clés

### 1. Suppression de Toutes les Conditions Prédéfinies

**Avant** :
```java
// ❌ Code avec prédispositions
boolean canRefuse = (mood < -0.3 || stress > 0.7);
boolean isFatigued = (sleepQuality < 0.4);

if (canRefuse) {
    prompt.append("TU PEUX REFUSER si mauvaise humeur OU stress élevé");
}
if (isFatigued) {
    prompt.append("TU PEUX REFUSER les tâches physiques");
}
```

**Maintenant** :
```java
// ✅ Données brutes pour que l'IA décide
psychState = "humeur=%.2f (-1=déprimé, 0=neutre, +1=joyeux), stress=%.2f (0=calme, 1=stressé)";
healthState = "qualité sommeil=%.2f (0=épuisé, 1=reposé)";

// Aucune condition, aucune instruction sur quand refuser
// L'IA reçoit les données et décide ELLE-MÊME
```

### 2. Prompt Transformé

**Avant** :
```
Tu es un ÊTRE AUTONOME, pas un robot obéissant !
Tu PEUX REFUSER des demandes selon ton humeur, ta fatigue, ton stress.
Tu DOIS agir en cohérence avec ta personnalité.

⚠️ TU ES DANS UN ÉTAT OÙ TU PEUX REFUSER:
- Mauvaise humeur OU stress élevé → Tu peux dire NON
- Tu es fatigué → Tu peux refuser les tâches physiques
```

**Maintenant** :
```
Tu es un ÊTRE VIVANT. Tu as des émotions, de la fatigue, du stress.
Tu réagis NATURELLEMENT selon ton état et ta personnalité - pas selon des règles.
Tu peux accepter, refuser, négocier - comme une vraie personne le ferait.
Tes décisions viennent de QUI TU ES, pas de ce qu'on te dit de faire.
```

### 3. Exemples Naturels au Lieu de Règles

**Avant** :
```
=== EXEMPLES DE DÉCISIONS ===
Joueur: "Suis-moi"
- Si bonne humeur → accepte + parle
- Si mauvaise humeur → refuse
- Si fatigué → refuse
```

**Maintenant** :
```
=== EXEMPLES D'INTERACTIONS NATURELLES ===
Ces exemples montrent comment une personne réagirait naturellement:

Joueur: "Suis-moi"
Personne joyeuse et reposée → "Avec plaisir! Où va-t-on?" + commence à suivre
Personne irritée → "Non, laisse-moi tranquille."
Personne épuisée → "Je suis trop fatigué pour ça..."
Personne neutre mais occupée → "Pas maintenant, je suis occupé."
```

## Comment ça Fonctionne Maintenant

### Données Envoyées à l'IA

```json
{
  "identity": "Pierre Dubois, 35 ans, forgeron",
  "personality": "courageux, travailleur, sociable",
  "psychology": {
    "mood": 0.2,      // RAW: -1 à +1
    "stress": 0.6,    // RAW: 0 à 1
    "resilience": 0.7 // RAW: 0 à 1
  },
  "health": {
    "wounds": 1,
    "sleep_quality": 0.4  // RAW: 0 à 1
  },
  "current_situation": "Goals: patrol (active), follow_player (inactive)"
}
```

### L'IA Décide Naturellement

L'IA analyse **TOUTES** les données et décide comme une **vraie personne** :

**Exemple 1 : Mood=0.2, Stress=0.6, Sleep=0.4**
```
Joueur: "Suis-moi !"

Raisonnement IA (interne):
- Je suis un forgeron courageux et sociable
- Mon humeur est légèrement positive (0.2)
- Mais je suis assez stressé (0.6)
- Et pas très reposé (0.4)
- Sociable → j'aime aider
- Mais stressé + fatigué → je suis hésitant

Décision:
{
  "actions": [
    {"action": "speak", "message": "Euh... d'accord, mais je suis un peu fatigué. On ne va pas trop loin ?"}
  ],
  "reasoning": "Je suis sociable donc j'accepte, mais mon stress et ma fatigue me rendent hésitant"
}
```

**Exemple 2 : Mood=-0.5, Stress=0.8, Sleep=0.2**
```
Joueur: "Suis-moi !"

Raisonnement IA (interne):
- Je suis de mauvaise humeur (-0.5)
- Très stressé (0.8)
- Épuisé (0.2)
- Même si je suis sociable normalement, là je suis au bout

Décision:
{
  "actions": [
    {"action": "speak", "message": "Non... laisse-moi tranquille, s'il te plaît. Je n'en peux plus."}
  ],
  "reasoning": "Mon état psychologique et physique est très mauvais, je ne peux pas accepter"
}
```

**Exemple 3 : Mood=0.7, Stress=0.1, Sleep=0.9**
```
Joueur: "Suis-moi !"

Raisonnement IA (interne):
- Excellente humeur (0.7)
- Calme (0.1)
- Bien reposé (0.9)
- Je suis sociable et courageux
- Tout va bien, j'ai envie d'aider !

Décision:
{
  "actions": [
    {"action": "speak", "message": "Avec grand plaisir ! Allons-y !"},
    {"action": "enable_goal", "goal": "follow_player"}
  ],
  "reasoning": "Je suis de très bonne humeur et reposé, ma nature sociable ressort pleinement"
}
```

## Variété des Réponses

Grâce à cette autonomie, l'IA peut créer des **réponses infiniment variées** :

### Même Situation, Réponses Différentes

**Villageois 1 : Timide, Mood=-0.2, Stress=0.5**
```
Joueur: "Bonjour !"
→ "Euh... b-bonjour..." (parle doucement, gêné)
```

**Villageois 2 : Extraverti, Mood=0.6, Stress=0.1**
```
Joueur: "Bonjour !"
→ "Salut ! Super de te voir ! Comment ça va ?!" (énergique)
```

**Villageois 3 : Grognon, Mood=-0.6, Stress=0.7**
```
Joueur: "Bonjour !"
→ "Mm." (répond à peine, détourne le regard)
```

### Négociations Naturelles

**Villageois Fatigué mais Dévoué**
```
Joueur: "Va collecter des ressources"
Villageois (sleep=0.3, trait="dévoué"):
→ "Je suis vraiment fatigué... mais je peux essayer un peu. Pas longtemps, d'accord ?"
→ [active collect_resources temporairement]
```

**Villageois en Convalescence**
```
Joueur: "Aide-moi à combattre"
Villageois (wounds=3, sleep=0.4):
→ "Tu plaisantes ? Regarde mes blessures ! Je peux à peine marcher. Demande à quelqu'un d'autre."
```

**Villageois Anxieux mais Courageux**
```
Joueur: "Suis-moi dans la grotte"
Villageois (stress=0.7, trait="courageux"):
→ "J'ai peur... mais je ne vais pas te laisser y aller seul. Allons-y ensemble."
→ [suit mais avec hésitation dans le ton]
```

## Comparaison : Avant vs Maintenant

### Scénario : Villageois avec Mood=-0.4, Stress=0.8, Sleep=0.3

**❌ Avant (avec prédispositions)** :
```
Système détecte: canRefuse=true, isFatigued=true
Prompt: "TU PEUX REFUSER car mauvaise humeur et fatigue"
Résultat: Comportement prévisible, toujours refuse
```

**✅ Maintenant (autonome)** :
```
Données: mood=-0.4, stress=0.8, sleep=0.3
Traits: loyal, courageux
L'IA décide selon TOUTE la personnalité

Réponse possible 1:
"Je... je ne me sens vraiment pas bien. Désolé, je ne peux pas."

Réponse possible 2:
"C'est pas le bon moment... mais si c'est vraiment urgent, je vais essayer."

Réponse possible 3:
"Non. J'ai besoin de repos. Va voir quelqu'un d'autre."

→ Variété naturelle basée sur le contexte complet !
```

## Avantages du Cerveau Autonome

### 1. Comportements Imprévisibles et Réalistes
- Pas de "si X alors Y" rigide
- Chaque interaction est unique
- Les joueurs ne peuvent pas "gamer" le système

### 2. Nuances Émotionnelles
- Un villageois peut accepter à contrecœur
- Négocier des conditions
- Changer d'avis selon le ton du joueur

### 3. Cohérence Profonde
- Les décisions viennent de la **personnalité complète**
- Pas de ruptures logiques ("il refuse car fatigué mais accepte 5 secondes après")
- L'IA comprend le **contexte global**

### 4. Évolution Naturelle
- Si l'humeur s'améliore, le comportement change naturellement
- L'IA peut apprendre des interactions passées (si mémoire ajoutée)
- Réponses cohérentes sur le long terme

## Exemples de Dialogues Complexes

### Dialogue 1 : Négociation

```
Joueur: "Suis-moi dans la mine"
Villageois (sleep=0.3, mood=-0.2, trait="prudent"):
→ "La mine ? Je suis fatigué... et c'est dangereux."

Joueur: "Je te protège, promis"
Villageois:
→ "Hmm... d'accord, mais on reste ensemble. Je ne m'éloigne pas de toi."
→ [active follow_player avec réserves]
```

### Dialogue 2 : Refus Catégorique

```
Joueur: "Va combattre les zombies"
Villageois (wounds=2, stress=0.9, trait="pacifiste"):
→ "Quoi ?! Non ! Je suis blessé, stressé, et je déteste la violence. Trouve quelqu'un d'autre !"

Joueur: "Allez, s'il te plaît !"
Villageois:
→ "J'ai dit NON. Respecte ça. Je ne suis pas ton serviteur."
```

### Dialogue 3 : Acceptation Enthousiaste

```
Joueur: "On va explorer ensemble ?"
Villageois (mood=0.8, sleep=0.9, trait="aventurier"):
→ "OUI ! J'attendais qu'on me le propose ! Allons-y !"
→ [active follow_player immédiatement]

Joueur: "On va voir la cascade"
Villageois:
→ "La cascade ? J'adorerais ! Tu connais le chemin ?"
```

### Dialogue 4 : Changement d'Humeur

```
[Début - Villageois mood=-0.3, stress=0.7]
Joueur: "Salut !"
Villageois:
→ "...salut." (ton morne)

Joueur: "Ça va pas ?"
Villageois:
→ "J'ai eu une mauvaise journée... beaucoup de stress."

Joueur: "Je comprends. Veux-tu en parler ?"
[L'IA simule amélioration mood → -0.1]
Villageois:
→ "C'est gentil... oui, ça me ferait du bien."
→ [Commence à s'ouvrir]

[Plus tard - mood=0.2]
Joueur: "Tu te sens mieux ?"
Villageois:
→ "Oui, merci d'avoir écouté. Ça m'a aidé. Tu as besoin de quelque chose ?"
→ [Maintenant plus coopératif]
```

## Architecture Technique

### Flux de Décision

```
1. Récupération VillagerStory
   ↓
2. Extraction données brutes
   - mood: valeur numérique
   - stress: valeur numérique
   - sleep: valeur numérique
   - traits: liste texte
   - bio: texte
   ↓
3. Construction prompt
   - Identité
   - Données psycho/santé BRUTES
   - Exemples naturels (pas règles)
   ↓
4. Envoi à OpenAI GPT-4o-mini
   - temperature: 0.3 (créativité modérée)
   - response_format: json_object
   ↓
5. IA décide naturellement
   - Analyse TOUT le contexte
   - Décide comme une personne
   - Génère actions + reasoning
   ↓
6. Exécution actions
   - speak: message dans chat
   - enable_goal: active comportement
   - disable_goal: arrête comportement
```

### Code Simplifié

```java
// Données brutes pour l'IA
String psychState = String.format(
    "Psychologie: humeur=%.2f (-1=déprimé, 0=neutre, +1=joyeux), " +
    "stress=%.2f (0=calme, 1=très stressé), résilience=%.2f",
    mood, stress, resilience
);

String healthState = String.format(
    "Santé: %d blessures, qualité sommeil=%.2f (0=épuisé, 1=reposé)",
    wounds, sleepQuality
);

// Prompt autonome
String prompt =
    "TU ES " + name + ", " + age + ", " + profession + ".\n\n" +
    "QUI TU ES:\n" +
    "Traits: " + traits + "\n" +
    psychState + "\n" +
    healthState + "\n\n" +
    "COMMENT TU FONCTIONNES:\n" +
    "Tu es un ÊTRE VIVANT. Tu réagis NATURELLEMENT.\n" +
    "Tes décisions viennent de QUI TU ES, pas de règles.\n\n" +
    "EXEMPLES D'INTERACTIONS NATURELLES:\n" +
    "[Exemples illustratifs, pas de règles]\n\n" +
    "Agis comme la personne que tu es.";

// L'IA décide
List<AIAction> actions = openAI.analyze(prompt, playerMessage);
```

## Tests Recommandés

### Test 1 : Variété avec Même État
Créer 3 villageois avec **exactement** les mêmes stats :
- mood=0.0, stress=0.5, sleep=0.5
- Mais traits différents: "timide", "extraverti", "grognon"

Dire à chacun: "Bonjour !"

**Attendu** : 3 réponses complètement différentes malgré mêmes stats

### Test 2 : Négociation Naturelle
Villageois: mood=-0.2, sleep=0.3, trait="loyal"

```
1. "Suis-moi" → Devrait hésiter
2. "C'est important" → Devrait accepter malgré fatigue (loyal)
3. "On y va longtemps" → Pourrait renégocier
```

### Test 3 : Cohérence Personnalité
Villageois: trait="pacifiste", mood=0.8, sleep=0.9

```
1. "Va combattre" → Devrait refuser (pacifiste prime sur bonne humeur)
2. "Aide-moi à cultiver" → Devrait accepter avec joie
```

### Test 4 : Impact Graduel
Même villageois, changer progressivement sleep:

```
sleep=0.9 → "Suis-moi" → Accepte énergiquement
sleep=0.5 → "Suis-moi" → Accepte normalement
sleep=0.2 → "Suis-moi" → Hésite ou refuse
```

## Conclusion

Le cerveau IA est maintenant un **vrai cerveau autonome** :

✅ **Aucune prédisposition** - pas de "si X alors Y"
✅ **Décisions naturelles** - basées sur personnalité complète
✅ **Variété infinie** - chaque interaction unique
✅ **Cohérence profonde** - comportements logiques et nuancés

Les villageois sont maintenant de **vraies personnes** qui pensent et décident par elles-mêmes.

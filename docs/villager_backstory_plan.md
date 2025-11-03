# Villager Backstory & AI Dialog Plan

Objectif: doter chaque villageois d’un background riche, crédible et cohérent (famille, traumatismes, personnalité, souvenirs, objectifs), alimentant des dialogues dynamiques, tout en minimisant les appels IA via génération déterministe, cache et batch.

Contenu de ce document:
- Modèle JSON ultra‑complet (données, types, taxonomies)
- Stratégie anti‑spam IA (cache, batch, fallback offline)
- Phases avec TODOs précis (boîte à outils Forge incluse)
- Acceptation et tests

---

## 1) Modèle JSON Complet (canon)

Versionnage commun à tous les objets:
- `schemaVersion: string` – version du schéma (ex. "1.0.0").
- `generatorVersion: string` – version du générateur.

Identité et rattachements:
- `uuid: string`
- `villageId: string`
- `worldSeed: string | number`
- `cultureId: string` – style noms/rites (ex. `plaine_nord`).
- `name: { given: string, family?: string, nickname?: string, honorifics?: string[] }`
- `sex: string` – ex. `male|female|other|unspecified` (extensible)
- `ageYears: int` – âge actuel
- `birth: { y: int, m: int, d: int, place: string }`
- `origin: string` – village/biome d’origine éventuel

Biométrie & santé:
- `biometrics: { heightCm?: int, weightKg?: int, handedness?: "left"|"right"|"ambidextrous", bloodType?: string }`
- `health: {
  disabilities: string[],
  chronicDiseases: string[],
  allergies: string[],
  addictions: { type: string, severity: 0..1 }[],
  phobias: { type: string, severity: 0..1 }[],
  wounds: { type: string, date: string, severity: 0..1, permanent: boolean }[],
  scars: string[],
  tattoos: string[],
  immunizations?: string[],
  stamina: 0..1,
  painTolerance: 0..1,
  sleepQuality: 0..1
}`

Traumatismes & mental:
- `psychology: {
  trauma: {
    events: {
      id: string,
      type: string,           // ex. perte_proche, famine, agression, catastrophe_naturelle
      ageAt: int,
      description: string,
      tags: string[],
      severity: 0..1,
      recurringSymptoms?: string[] // cauchemars, flashbacks, évitement, hypervigilance
    }[],
    cptsd?: boolean,
    coping: string[]          // stratégies d’adaptation: travail, isolement, humour, rituels
  },
  moodBaseline: -1..1,        // humeur moyenne
  stress: 0..1,
  resilience: 0..1,
  neurodivergences?: string[], // ex. dyslexie, TSA (si pertinent au lore)
  attachments?: { style: string, security: 0..1 },
  fears?: string[],
  hopes?: string[]
}`

Personnalité (Big Five + facettes + traits):
- `personality: {
  bigFive: { O: 0..1, C: 0..1, E: 0..1, A: 0..1, N: 0..1 },
  facets?: {
    openness: { intellect: 0..1, aesthetics: 0..1, noveltySeeking: 0..1 },
    conscientiousness: { orderliness: 0..1, industriousness: 0..1 },
    extraversion: { assertiveness: 0..1, enthusiasm: 0..1 },
    agreeableness: { compassion: 0..1, politeness: 0..1 },
    neuroticism: { volatility: 0..1, withdrawal: 0..1 }
  },
  temperament?: string, // flegmatique, colérique, sanguin, mélancolique
  traits: string[],     // généreux, prudent, rancunier, loyal, superstitieux…
  humorStyle?: string[] // absurde, ironie, pince-sans-rire
}`

Éthique, croyances & culture:
- `beliefs: {
  religion?: string,
  superstitions?: string[],
  taboos?: string[],
  values: { tradition: 0..1, honor: 0..1, community: 0..1, ambition: 0..1, mercy: 0..1 },
  politicalLeanings?: string,
  rituals?: string[]
}`
- `culture: {
  naming: { patterns?: string[], banned?: string[] },
  cuisine?: string[],
  festivals?: string[],
  dressCode?: string[],
  etiquette?: string[]
}`

Compétences, éducation & métiers:
- `education: {
  literacy: 0..1,
  numeracy: 0..1,
  guilds?: string[],
  apprenticeship?: { mentor?: string, startAge?: int, field?: string }
}`
- `skills: {
  gatherer?: 0..100,
  farmer?: 0..100,
  fisherman?: 0..100,
  miner?: 0..100,
  lumberjack?: 0..100,
  mason?: 0..100,
  blacksmith?: 0..100,
  fletcher?: 0..100,
  alchemist?: 0..100,
  healer?: 0..100,
  barter?: 0..100,
  storytelling?: 0..100,
  survival?: 0..100,
  combat?: { melee?: 0..100, ranged?: 0..100, defense?: 0..100 }
}`
- `profession: { current: string, level: int, seniorityYears?: int, aspirations?: string[] }`

Économie, statut & légal:
- `economy: { wealthTier: 0..5, savings: int, debts: { to: string, amount: int, reason?: string }[], possessions: string[] }`
- `legal: {
  record: { date: string, type: string, severity: 0..1, resolved: boolean, notes?: string }[],
  reputation: { village: 0..100, nearby?: { [villageId: string]: int } },
  trustworthiness: 0..1
}`

Maison, foyer & animaux:
- `household: {
  homeId: string,
  members: string[],
  property: { plots?: string[], livestock?: string[], vehicles?: string[] },
  petCompanions?: string[]
}`

Famille et relations:
- `family: { parents: string[], spouse?: string, exSpouses?: string[], children: string[], siblings: string[] }`
- `relations: {
  known: {
    id: string,
    relation?: string, // voisin, ami, rival, mentor…
    opinion: -100..100,
    lastInteraction?: string,
    gossipTags?: string[]
  }[]
}`

Souvenirs & journal:
- `memories: {
  entries: {
    id: string,
    date: string,
    topic: string,
    persons?: string[],
    place?: string,
    moodDelta?: -1..1,
    importance: 0..1,
    tags: string[]
  }[],
  forgettingRate: 0..1
}`

Chronologie de vie (événements):
- `life: {
  timeline: {
    age: int,
    type: string,    // naissance, apprentissage, union, accident, migration, deuil…
    place?: string,
    with?: string[],
    details?: string,
    impact?: { mood?: -1..1, skills?: { [k:string]: int }, reputation?: int }
  }[]
}`

Routines & préférences:
- `routines: { daily: string[], weekly?: string[], sleepSchedule?: { start: string, end: string } }`
- `preferences: {
  likes: string[],
  dislikes: string[],
  foods?: { favorite?: string[], disliked?: string[], allergies?: string[] },
  colors?: string[],
  music?: string[],
  hobbies?: string[]
}`

Objectifs & motivations:
- `goals: {
  shortTerm: string[],
  longTerm: string[],
  blockers?: string[]
}`

Langues & communication:
- `languages: { native: string, fluent: string[], basic?: string[] }`
- `dialogue: { hooks: string[], catchphrases?: string[], style?: string }`

Inventaire & apparence:
- `inventory: { items: { id: string, qty: int, durability?: int }[], favoriteItemId?: string }`
- `appearance: {
  hair?: string,
  eyes?: string,
  clothingStyle?: string[],
  cleanliness?: 0..1,
  distinctiveMarks?: string[]
}`

Texte généré:
- `text: { bioBrief: string, bioLong?: string, dialogueOpeners?: string[] }`

Cache & métadonnées IA:
- `ai: {
  cacheKeys?: { bioLong?: string, openers?: string },
  lastGeneration?: { bioLong?: string, openers?: string },
  promptTemplateVersion?: string
}`

Notes internes:
- `notes?: string[]`

---

## 2) Taxonomies et exemples

Traumatismes (type):
- `perte_proche`, `famine`, `agression`, `catastrophe_naturelle`, `guerre`, `exil`, `maladie_grave`, `accident_travail`, `trahison`, `ruine_financiere`, `incendie`, `disparition`, `attaque_monstre`.

Événements (timeline.type):
- `naissance`, `enfance_marquee`, `apprentissage`, `premiere_heure_gloire`, `premiere_chute`, `union`, `naissance_enfant`, `migration`, `accident`, `deuil`, `promotion`, `conversion`, `litige`, `reconciliation`, `decouverte`, `semi_legende_locale`, `incident_village`, `heroisme`, `blessure_grave`.

Relations (relation):
- `ami`, `rival`, `connaissance`, `voisin`, `mentor`, `protégé`, `concurrent`, `benefactor`, `creancier`, `ennemi`, `client`, `vendeur`.

Routines (daily):
- `prier`, `travailler_champs`, `forge`, `peche`, `chasse`, `garde`, `cuisine`, `couture`, `commerce`, `reparation`, `enseignement`, `jeu`, `repos`, `commérage`.

---

## 3) Anti‑Spam IA: cache, batch, fallback

- Clé de cache: `sha256(schemaVersion|promptTemplateVersion|worldSeed|villageId|cultureId|inputJson)`
- Stockage cache: `SavedData` par monde → `data/iamod/villages/<villageId>.json`
- Batch: regrouper `bioLong` et `dialogueOpeners` par village/culture à la première visite
- Rate limiting: quota global/session + min 24h IRL par village
- Fallback offline: gabarits grammaticaux (CFG/Tracery‑like) si réseau indisponible

---

## 4) Phases & TODOs

Phase 1 — Socle déterministe (offline)
- [x] Définir POJOs + sérialisation NBT/JSON
  - [x] `net.frealac.iamod.common.story.VillagerStory`
  - [ ] `net.frealac.iamod.common.story.Health`, `Psychology`, `Personality`, `Relations`, `LifeEvent`, etc. (Phase 2+)
- [x] Capability pour Villager
  - [x] `BackstoryCapability` + attach via `AttachCapabilitiesEvent<Entity>`
  - [x] Sync côté client (S2C) minimal (`bioBrief`, `family` inclus dans JSON)
- [x] Générateur déterministe seedé
  - [x] Noms/traits/métiers selon culture
  - [x] Famille simple (parents/enfants/fratrie)
  - [x] `bioBrief` + 1–3 `memories` (augmenter en Phase 2)
- [x] UI basique
  - [x] `VillagerDialogScreen`: affiche `bioBrief` injecté dans la conversation à l’ouverture (onglet “Résumé” repoussé en Phase 2)
- [x] SavedData village
  - [x] `VillageStoryData` pour stocker par UUID de PNJ (serveur)
- [x] Multijoueur (serveur autoritaire, même background pour tous les joueurs)
  - [x] Génération/stockage des stories uniquement côté serveur (SavedData)
  - [x] Interdire toute génération côté client (JSON reçu uniquement via S2C)
  - [x] Clé de seed stable: `seed = worldSeed ^ villagerUUID ^ bucket(chunk)`
  - [x] Synchronisation S2C: paquet initial complet (JSON)
  - [ ] Re‑sync à la (re)connexion et à l’entrée dans le suivi (à ajouter si nécessaire)
  - [ ] Validation: checksum identique sur 2 clients (tests)

Phase 2 — Chronologie & santé/trauma
- [x] Timeline de vie (événements clés) → `lifeTimeline`
- [x] Santé: blessures, allergies, phobies, stamina/sommeil
- [x] Psychologie: traumatismes (événements), coping, humeur/stress/résilience
- [x] Routines + préférences
- [x] `memoriesDetailed` (moodDelta, importance, tags)
- [x] UI: onglets “Histoire” + “Santé” et bouton “Debug” (overlay story complète)
  
Phase 3 — Graphe social & économie/légal
- [ ] Relations détaillées (opinions dynamiques)
- [ ] Économie: richesse, dettes, possessions
- [ ] Légal: casier, réputation multi‑villages
- [ ] Mécaniques de commérage + souvenirs dynamiques
- [ ] UI: onglet “Relations”

Phase 4 — LLM cosmétique + cache
- [ ] Batch `bioLong` et `dialogueOpeners` par village
- [ ] Cache adressable au contenu + rate limiting
- [ ] Fallback total offline (CFG) si réseau off
- [ ] Paramétrage prompts par culture/langue

Phase 5 — Dialogues IA & intentions
- [ ] Moteur d’intentions (saluer, informer, se plaindre, demander, raconter souvenir, commérer)
- [ ] Sélection de sujets (contexte/humeur/souvenirs/relation)
- [ ] Gabarits linguistiques multi‑langue
- [ ] Ajustements d’équilibrage, diversité et QA narrative

---

## 5) Détails d’implémentation (Forge)

- Capabilities (implémenté Phase 1)
  - Interface: `IVillagerStory`
  - Provider: `VillagerStoryProvider` (clé `iamod:story`)
  - Storage: sérialisation NBT (SavedData) + JSON pour réseau/debug

- SavedData par monde (implémenté Phase 1)
  - `VillageStoryData` (clé `iamod_villager_stories`)
  - Map: `UUID PNJ -> story NBT`

- Générateur déterministe (implémenté Phase 2)
  - `StoryGenerator.generate(ServerLevel, Villager)`
  - Seed: `worldSeed ^ villagerUUID ^ (chunkX<<32 ^ chunkZ)` (stable)
  - Noms: `NameMaker` (combinaison syllabique) → milliers de prénoms/noms plausibles
  - Métiers: racines+suffixes + métiers vanilla → centaines de combinaisons
  - Traits: pool élargi + intensifieurs (VarietyUtil)
  - Cultures: liste étendue (20+)
  - Génère: identité (culture/nom/sexe/âge), métier, 2–4 traits, famille, 1–3 souvenirs simples,
    santé, psychologie (traumas), `lifeTimeline`, `memoriesDetailed`, `routines`, `preferences`, `bioBrief`

- UI (Phase 2)
  - `VillagerDialogScreen`: onglets “Chat”, “Histoire”, “Santé”
  - Bouton “Debug”: overlay avec story complète (identité, famille, traits, santé, traumas, bioBrief)
  - `bioBrief` toujours affiché à l’ouverture du dialogue
  - Zone de chat scrollable à la molette (ancrée en bas par défaut)

- Variété procédurale (nouveau)
  - `NameMaker`: prénoms et noms via syllabes (2–3 + terminaisons)
  - Professions: `PROF_ROOTS` + `PROF_SUFFIX` + métiers vanilla
  - Traits: intensifieurs (`très`, `plutôt`, `vraiment`…) via `VarietyUtil`
  - Cultures: 20+ identifiants (plaine_nord, marais_ouest, archipel_brisé, …)

- Réseau (implémenté Phase 1)
  - `SyncVillagerStoryS2CPacket` (S2C): envoie JSON de `VillagerStory`
  - Réception côté client: mise en cache + append de `bioBrief` si l’écran est ouvert
  - Streaming IA (simulé): `AiReplyStreamChunkS2CPacket` envoie des morceaux d’output; le client affiche en direct (begin/append/done)

---

## 6) Acceptation & Tests

- Cohérence famille: pas de cycles, âges réalisables (parents plus âgés que enfants de >= 14 ans d’écart min configurable)
- Diversité: pas plus de X traits identiques par village (configurable)
- Perf: génération par village < 10 ms par PNJ en moyenne (sans LLM)
- Cache: taux de hit > 90% après première visite
- GameTests:
  - [ ] Validation familles/âges
  - [ ] Timeline triée, sans chevauchement invalide
  - [ ] Sérialisation NBT/JSON ronde‑trip
  - [ ] Multijoueur: même story pour un PNJ (même UUID) sur 2 clients (checksum égal), aucune divergence après reconnexion/chargement chunk
  - [ ] Diversité: sur 1000 PNJ avec seeds différents, taux de collisions (prénom+nom+métier+2 traits) < 5%; stabilité déterministe (mêmes seeds → mêmes sorties)

---

## 7) Exemples JSON (condensés)

```json
{
  "schemaVersion": "1.0.0",
  "generatorVersion": "1.0.0",
  "uuid": "v-3b9c2",
  "villageId": "meule_du_nord",
  "worldSeed": 123456789,
  "cultureId": "plaine_nord",
  "name": { "given": "Maelle", "family": "Dubreuil" },
  "sex": "female",
  "ageYears": 34,
  "birth": { "y": 198, "m": 3, "d": 14, "place": "Meule-du-Nord" },
  "biometrics": { "handedness": "right" },
  "health": {
    "disabilities": [],
    "chronicDiseases": ["asthme"],
    "allergies": ["pollen"],
    "addictions": [],
    "phobias": [{ "type": "orage", "severity": 0.6 }],
    "wounds": [{ "type": "brulure", "date": "228-07-12", "severity": 0.4, "permanent": true }],
    "scars": ["avant-bras droit"],
    "stamina": 0.7,
    "painTolerance": 0.6,
    "sleepQuality": 0.5
  },
  "psychology": {
    "trauma": {
      "events": [
        {
          "id": "t1",
          "type": "incendie",
          "ageAt": 20,
          "description": "Perte de l’atelier familial",
          "tags": ["perte_bien", "peur_feu"],
          "severity": 0.7,
          "recurringSymptoms": ["cauchemars"]
        }
      ],
      "cptsd": false,
      "coping": ["travail", "rituels"]
    },
    "moodBaseline": 0.1,
    "stress": 0.3,
    "resilience": 0.8,
    "fears": ["orage"],
    "hopes": ["rebâtir un grand atelier"]
  },
  "personality": {
    "bigFive": { "O": 0.6, "C": 0.8, "E": 0.5, "A": 0.7, "N": 0.3 },
    "traits": ["méticuleuse", "généreuse", "superstitieuse"]
  },
  "beliefs": {
    "superstitions": ["noeud rouge contre la malchance"],
    "values": { "tradition": 0.7, "honor": 0.6, "community": 0.8, "ambition": 0.6, "mercy": 0.6 }
  },
  "culture": { "cuisine": ["ragoûts", "pain"] },
  "education": { "literacy": 0.7, "numeracy": 0.6, "guilds": ["artisans"] },
  "skills": { "farmer": 20, "mason": 55, "blacksmith": 35, "barter": 40, "storytelling": 25 },
  "profession": { "current": "maçon", "level": 3, "seniorityYears": 12, "aspirations": ["maître-artisan"] },
  "economy": { "wealthTier": 2, "savings": 48, "debts": [], "possessions": ["chariot", "outils"] },
  "legal": { "record": [], "reputation": { "village": 74 }, "trustworthiness": 0.8 },
  "household": { "homeId": "H-12", "members": ["v-3b9c2", "v-9af22"], "property": { "livestock": ["poule", "chèvre"] } },
  "family": { "parents": ["v-0aa1", "v-0aa2"], "spouse": "v-9af22", "children": ["v-1c22f"], "siblings": [] },
  "relations": { "known": [{ "id": "v-77c1", "relation": "voisin", "opinion": 12, "gossipTags": ["commère"] }] },
  "memories": { "entries": [{ "id": "m1", "date": "229-02-01", "topic": "marché", "moodDelta": 0.2, "importance": 0.4, "tags": ["vente"] }], "forgettingRate": 0.2 },
  "life": { "timeline": [{ "age": 14, "type": "apprentissage", "place": "Meule-du-Nord", "details": "chez l’oncle" }] },
  "routines": { "daily": ["travail_chantier", "repos", "commérage"], "sleepSchedule": { "start": "21:30", "end": "06:00" } },
  "preferences": { "likes": ["pierre lisse"], "dislikes": ["boue"], "foods": { "favorite": ["ragoût"], "allergies": ["pollen"] } },
  "goals": { "shortTerm": ["réparer le puits"], "longTerm": ["école d’artisans"], "blockers": ["manque de pierre" ] },
  "languages": { "native": "fr", "fluent": ["fr"], "basic": [] },
  "dialogue": { "hooks": ["parler du chantier", "demander des matériaux"] },
  "inventory": { "items": [{ "id": "stone_brick", "qty": 32 }, { "id": "trowel", "qty": 1 }] },
  "appearance": { "clothingStyle": ["tablier"], "cleanliness": 0.6, "distinctiveMarks": ["cicatrice bras droit"] },
  "text": { "bioBrief": "Maçonne appliquée, rescapée d’un incendie, prête à rebâtir." },
  "ai": { "promptTemplateVersion": "p1" }
}
```

---

## 8) Prochaines étapes

- Confirmer le périmètre Phase 1 et les onglets UI prioritaires
- Démarrer le socle Java (POJOs + Capability + SavedData)
- Ajouter un premier générateur déterministe (noms/traits/famille/bioBrief)
- Relier `VillagerDialogScreen` à `bioBrief` et `family`

# 🧪 PLAN DE TEST - ChatHandler

## Problème
ChatHandler ne génère AUCUN log quand tu parles aux villageois.

## Ce qui fonctionne
✅ Mod IAMOD se charge
✅ MemoryEventListener fonctionne (logs de coups)
✅ ChatHandler compile et est dans le JAR
✅ Annotations correctes (@Mod.EventBusSubscriber + @SubscribeEvent)

## Test à faire

### Étape 1: Lancer avec surveillance
```bash
./run-game-and-watch.sh
```

### Étape 2: Dans ton IDE
Lance `runClient` (comme d'habitude)

### Étape 3: Dans le jeu
1. Trouve un villageois
2. **Écris N'IMPORTE QUOI dans le chat** (pas besoin d'être proche)
3. Regarde le terminal → tu DOIS voir:
   ```
   🎤 ChatHandler: Player Dev said: 'ton message'
   ```

## Résultats attendus

### Si TU VOIS les logs ChatHandler
✅ Super! Le système fonctionne, continue avec les villageois

### Si TU NE VOIS RIEN
❌ ServerChatEvent ne fire pas → problème Forge 1.21
→ Il faudra utiliser un autre event (ServerChatDecorator ou ChatMessageEvent)

## Prochaine étape
Dis-moi ce que tu vois après le test!

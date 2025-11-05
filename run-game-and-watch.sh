#!/bin/bash

echo "========================================="
echo "🎮 LANCEMENT DU JEU AVEC SURVEILLANCE"
echo "========================================="
echo ""

# Nettoyer les anciens logs
rm -f run/logs/latest.log ai-debug-*.txt

echo "📝 Les logs IA seront sauvés dans: ai-debug-$(date +%Y%m%d-%H%M%S).txt"
echo ""
echo "🚀 Lance le jeu maintenant avec runClient dans ton IDE"
echo "   (IntelliJ, Eclipse, VSCode...)"
echo ""
echo "⏳ Attente du fichier de log..."

# Attendre que le jeu démarre
while [ ! -f "run/logs/latest.log" ]; do
    sleep 1
done

echo ""
echo "✅ JEU DÉTECTÉ!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 SURVEILLANCE DES LOGS IA:"
echo ""

# Surveiller les logs
OUTPUT_FILE="ai-debug-$(date +%Y%m%d-%H%M%S).txt"
tail -f "run/logs/latest.log" | while read line; do
    if echo "$line" | grep -qE "🎤|💬|💚|💕|💔|💢|🧠|✓|ChatHandler|MessageAnalyzer|Brain|IAMOD"; then
        echo "$line" | tee -a "$OUTPUT_FILE"
    fi
done

#!/bin/bash

# Script de surveillance des logs IA
LOG_FILE="run/logs/latest.log"
OUTPUT_FILE="ai-debug-$(date +%Y%m%d-%H%M%S).txt"

echo "========================================"
echo "🔍 SURVEILLANCE LOGS IA"
echo "===============
========================="
echo "📁 Fichier: $LOG_FILE"
echo "💾 Sortie: $OUTPUT_FILE"
echo ""
echo "🎯 Attente du jeu..."
echo ""

# Attendre le fichier
while [ ! -f "$LOG_FILE" ]; do
    sleep 1
done

echo "✅ JEU DÉTECTÉ - Surveillance active"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Initialiser le fichier de sortie
echo "=== LOGS IA - $(date) ===" > "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Surveiller et afficher
tail -f "$LOG_FILE" | while read line; do
    if echo "$line" | grep -qE "🎤|💬|💚|💕|💔|💢|🧠|✓|ChatHandler|MessageAnalyzer|Brain"; then
        echo "$line" | tee -a "$OUTPUT_FILE"
    fi
done

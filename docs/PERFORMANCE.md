# Performance-Überarbeitung für Paper 26.2 und 26.3

Die Versionen 1.1.0 (26.2) und 2.1.0 (26.3) verwenden denselben optimierten
Quellcode, werden aber separat gegen die jeweilige Paper-Version gebaut.

## Änderungen

- Speicheranforderungen eines Ticks werden zu einem Snapshot zusammengefasst.
  Auf dem Hauptthread werden nur die Daten kopiert; JSON-Erzeugung und
  Dateizugriff erfolgen im Hintergrund. Ein einzelner Writer verarbeitet jeweils
  den neuesten wartenden Snapshot, sodass langsamer Datenträgerzugriff keine
  unbegrenzte Task-Warteschlange aufbaut. Beim Shutdown wird synchron gespeichert;
  ältere Hintergrundschreibvorgänge können diese Speicherung nicht überschreiben.
- Checkpoints lesen gespeicherte Sitzungen einmal und finden aktive Sitzungen
  über ihre ID. Der Aufwand ist linear statt quadratisch. Ohne aktive Dummys
  entfällt die regelmäßige Speicherung. Massenentfernung eines Besitzers und
  gleichzeitig ablaufende Sitzungen erzeugen jeweils nur einen Checkpoint.
- Spieler- und Entity-Erkennung verwenden direkte Suchindizes. Kolbenereignisse
  lesen jede Dummy-Position einmal pro Ereignis, statt pro bewegtem Block.
  Explosionsschutz prüft ausschließlich verwaltete Dummys und durchsucht keine
  umliegenden Mob- oder Item-Entities.
- Gleichzeitige Skin-Anfragen für denselben Schlüssel teilen sich eine Anfrage.
  Maximal acht HTTP-Abfragen können gleichzeitig ausstehen; beide Caches sind
  auf jeweils 256 Einträge begrenzt. Bei ausgelasteten Anfrageplätzen wird kein
  weiterer HTTP-Task gestartet; ein Skin kann dann auf den Besitzer-Skin bzw.
  den bisherigen/default Skin zurückfallen. Späteres erneutes Setzen ist möglich.
- Skin-Callbacks mit Plugin-Kontext laufen auf dem Hauptthread, einschließlich
  Cache-Treffern. Veraltete Antworten nach einer neueren Skin-Auswahl werden
  ignoriert. Entfernte Dummys werden nicht erneut aktualisiert.
- Beim verzögerten Wiederherstellen werden bereits aktive Sitzungen übersprungen.
  So werden Dummys, die unmittelbar nach dem Serverstart erstellt wurden, nicht
  erneut registriert.

Die Speicherung erfolgt im laufenden Betrieb frühestens im nächsten Tick.
Regelmäßige Restzeit-Checkpoints bleiben bei 30 Sekunden; bei einem harten
Prozessabbruch besteht weiterhin dieses Wiederherstellungsfenster.

## Builds

Java 25 verwenden. In PowerShell das Property-Argument vollständig zitieren:

```powershell
./gradlew.bat test shadowJar integrationJar '-PtargetPaper=26.2'
./gradlew.bat test shadowJar integrationJar '-PtargetPaper=26.3'
python tools/package_release.py --target-paper 26.2
python tools/package_release.py --target-paper 26.3
```

Die Ergebnisse liegen getrennt unter `build/26.2` bzw. `build/26.3`.
Auslieferbare Pakete liegen unter `dist/AFKDummyLimited-1.1.0` bzw.
`dist/AFKDummyLimited-2.1.0`. Die Probe-JAR ist ausschließlich für lokale Tests.

## Grenzen und Betrieb auf kleinen Servern

Ein lokaler Vorher/Nachher-Test für beide Paper-Versionen steht unter
[benchmarks/README.md](benchmarks/README.md). Er zeigt Einsparungen unter
synthetischer Spitzenlast, aber keine durchgehend belegte Beschleunigung im
normalen Mob-Betrieb; einige 26.2-Messstufen waren nach der Änderung langsamer.

Die Farmfunktion bleibt erhalten: Die Dummys aktivieren weiterhin Chunk-Simulation
und Mob-Spawning. Die dadurch laufenden Farmen, Redstone-Schaltungen und Mobs
verursachen zusätzliche Serverlast. Diese Last lässt sich nicht vollständig
entfernen, ohne das gewünschte Verhalten einzuschränken.

Die Änderungen reduzieren vermeidbare Plugin-Arbeit. Sie sind keine Garantie für
20 TPS bei beliebigen Farmen. Es wurde kein prozentualer Performancegewinn auf
dem Produktionsserver gemessen. Für diesen Server sollten MSPT/TPS bei identischer
Spielerzahl und denselben Farmen zuerst ohne Dummys, dann mit 1, 3 und 6 Dummys
verglichen werden. Das globale Limit lässt sich in `settings.max-server-wide-dummies`
reduzieren, falls die aktivierten Farmen den verfügbaren Tick-Spielraum aufbrauchen.

Die Lasttests wurden vor der Release-Nummerierung mit optimierten Vorab-JARs
unter den Nummern 1.0.0 und 2.0.0 durchgeführt. Die Rohdaten behalten deren
ursprüngliche SHA256-Werte. Veröffentlicht wird die Überarbeitung separat als
1.1.0 beziehungsweise 2.1.0; die bisherigen Releases bleiben erhalten.

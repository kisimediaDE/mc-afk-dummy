# Lokaler Lasttest: vor und nach der Performance-Überarbeitung

Messumgebung: lokaler Intel Core i7-14700K, Windows, Java 25, je ein Paper-Prozess mit 512 MiB Startheap und
1536 MiB maximalem Heap. Sicht- und Simulationsdistanz jeweils 3 Chunks.
Frische Flat-Welten mit Seed 20260925, Tageszeit fest, natürliches Mob-Spawning
ausgeschaltet. Keine echten Clients oder fremden Gameplay-Plugins.

Jede Stufe: 200 Ticks Aufwärmen, danach 400 gemessene Ticks (rund 20 Sekunden).
Je aktivem Dummy 32 KI-Husks in einer eingefassten Fläche; die Dummy-Standorte
liegen 160 Blöcke auseinander. Die Skin-Daten der Hilfsspieler sind vorbelegt.
Skin-Abfragen der verwalteten Dummys scheiterten an der lokalen Netzwerksperre
bereits während der Einrichtung vor den Messfenstern. Erfolgreiche HTTP-Abfragen
und Netzwerk-Langzeitverhalten sind damit ausdrücklich nicht getestet.

Die Stress-Stufe führt zusätzlich pro Tick 2000 direkte Kolben-Handler-Aufrufe
mit je 12 geschobenen Blöcken und zehn vollständige Checkpoint-Anforderungen aus.
Das sind synthetische Prüfungen, keine 2000 tatsächlich bewegten Kolben. Sie
isolieren die optimierten Codepfade und bilden keinen normalen Serverbetrieb ab.

Gemessen wird die Tickdauer über Papers ServerTickEndEvent, nicht die
50-ms-Wartezeit zwischen Ticks. CPU-Werte in den JSON-Dateien berücksichtigen
auch Hintergrundarbeit und sind als belegte CPU-Kerne angegeben.

Reihenfolge: 26.3 optimiert, 26.3 vorher, 26.2 vorher, 26.2 optimiert.
Die Server laufen nacheinander. Wegen deutlich abweichender Leerlaufzeiten
wurde das 26.2-Paar anschließend in der Reihenfolge vorher, optimiert wiederholt.
Pro Durchlauf und Variante wurde ein frischer Prozess gemessen;
die Stufen laufen stets in derselben Reihenfolge. JIT, GC, Mob-KI und andere
Aktivität des Desktop-Rechners verursachen Streuung. Keine statistisch
abgesicherte Aussage über kleine Unterschiede und kein Produktionsbenchmark.

## Paper 26.2

| Last | Vorher Ø / p95 MSPT | Optimiert Ø / p95 MSPT | Ticks >50 ms vorher / optimiert |
|---|---:|---:|---:|
| 0 Dummys, 0 KI-Mobs | 0.262 / 0.494 | 0.738 / 1.400 | 0 / 0 |
| 1 Dummys, 32 KI-Mobs | 0.993 / 2.057 | 2.565 / 5.156 | 0 / 0 |
| 3 Dummys, 96 KI-Mobs | 2.718 / 5.649 | 4.118 / 7.271 | 0 / 0 |
| 6 Dummys, 192 KI-Mobs | 4.866 / 8.552 | 5.808 / 9.098 | 0 / 0 |
| 6 Dummys + synthetischer Stress | 8.185 / 13.706 | 7.327 / 10.761 | 0 / 0 |

Die direkt gemessene Hauptthread-Zeit des synthetischen Zusatzworkloads betrug 6.065 ms/Tick vorher und 3.120 ms/Tick optimiert.

Rohdaten und JAR-Prüfsummen: [26.2-before.json](26.2-before.json), [26.2-after.json](26.2-after.json).

## Paper 26.2 – Wiederholung

| Last | Vorher Ø / p95 MSPT | Optimiert Ø / p95 MSPT | Ticks >50 ms vorher / optimiert |
|---|---:|---:|---:|
| 0 Dummys, 0 KI-Mobs | 0.526 / 1.100 | 0.836 / 1.503 | 0 / 0 |
| 1 Dummys, 32 KI-Mobs | 2.172 / 5.025 | 2.624 / 4.982 | 0 / 0 |
| 3 Dummys, 96 KI-Mobs | 2.357 / 5.151 | 4.256 / 7.296 | 0 / 0 |
| 6 Dummys, 192 KI-Mobs | 5.994 / 9.038 | 4.841 / 8.873 | 0 / 0 |
| 6 Dummys + synthetischer Stress | 8.686 / 13.287 | 7.330 / 11.821 | 0 / 0 |

Die direkt gemessene Hauptthread-Zeit des synthetischen Zusatzworkloads betrug 6.337 ms/Tick vorher und 3.194 ms/Tick optimiert.

Rohdaten und JAR-Prüfsummen: [26.2-before-repeat.json](26.2-before-repeat.json), [26.2-after-repeat.json](26.2-after-repeat.json).

## Paper 26.3

| Last | Vorher Ø / p95 MSPT | Optimiert Ø / p95 MSPT | Ticks >50 ms vorher / optimiert |
|---|---:|---:|---:|
| 0 Dummys, 0 KI-Mobs | 0.289 / 0.547 | 0.204 / 0.378 | 0 / 0 |
| 1 Dummys, 32 KI-Mobs | 1.241 / 2.212 | 0.949 / 1.593 | 0 / 0 |
| 3 Dummys, 96 KI-Mobs | 2.063 / 3.691 | 1.715 / 3.012 | 0 / 0 |
| 6 Dummys, 192 KI-Mobs | 3.447 / 5.543 | 3.211 / 5.506 | 0 / 0 |
| 6 Dummys + synthetischer Stress | 7.142 / 10.375 | 4.513 / 6.891 | 0 / 0 |

Die direkt gemessene Hauptthread-Zeit des synthetischen Zusatzworkloads betrug 5.390 ms/Tick vorher und 1.470 ms/Tick optimiert.

Rohdaten und JAR-Prüfsummen: [26.3-before.json](26.3-before.json), [26.3-after.json](26.3-after.json).

## Einordnung

- In allen sechs Läufen zusammen: 12000 gemessene Ticks, keine Überschreitung von 50 ms.
- Die Hauptthread-Zeit des synthetischen Zusatzworkloads sank bei 26.2 von
  6,06–6,34 auf 3,12–3,19 ms/Tick und bei 26.3 von 5,39 auf 1,47 ms/Tick.
  Das belegt eine Verbesserung in genau diesen übermäßig belasteten Codepfaden,
  keinen entsprechenden Prozentgewinn für den gesamten Server.
- Im normalen Mob-Szenario ist das Bild gemischt. Bei 26.2 waren insbesondere
  die Stufen mit einem und drei Dummys im optimierten Build in beiden Paaren langsamer.
  Zugleich schwanken bereits Leerlauf und unveränderte Vorher-Builds deutlich.
  Bei sechs Dummys wechselt die Richtung des Unterschieds zwischen den 26.2-Paaren.
  Eine generelle Verbesserung im normalen Betrieb ist daher nicht belegt.
  Eine Regression in einzelnen Situationen kann dieser Test nicht ausschließen;
  dafür wäre gezieltes Profiling unter besser kontrollierter Last nötig.

## Aussagegrenze

Dieses Ergebnis gilt für den beschriebenen lokalen Aufbau. Es ersetzt keine
Messung mit den Farmen, Sichtweiten, Plugins und der Hardware des Zielservers.
Insbesondere wurden keine echten Farm-Erträge, Client-Pakete, Redstone-Netze
oder HTTP-Störungen gemessen. Spawn- und Chunk-Generierungsspitzen liegen
vor den Messfenstern. Aus einem bestandenen Test folgt kein
absolutes Performance-Maximum und keine Garantie für 20 TPS auf anderer Hardware.

## Reproduzieren

Testcode: `src/integration/java/com/plugin/afkdummy/probe/LoadBenchmark.java`.
Der Runner `tools/run_load_benchmark.py` benötigt die vorbereiteten lokalen
Paper-Verzeichnisse `.integration/server` (26.2) und `.integration/server-26.3`
(26.3) samt akzeptierter EULA und lokal vorhandenen Libraries. Er erzeugt
für jeden Lauf ein neues Verzeichnis und bindet ausschließlich 127.0.0.1:25587.
Vorher-JARs stammen aus `build/libs`, optimierte JARs und Probe-JARs aus
`build/<Paper-Version>/libs`. Die konkreten SHA256-Werte stehen in den Rohdaten.

Beispiel: `python tools/run_load_benchmark.py --target 26.2 --variant before
--java <Pfad-zu-java.exe>` (als eine Zeile ausführen). Anschließend `--variant after`.
Für die Wiederholung `--run-id repeat` ergänzen. Die JVMs müssen nacheinander laufen.
Nach allen sechs Läufen erzeugt `python tools/summarize_load_benchmark.py` diesen Bericht.

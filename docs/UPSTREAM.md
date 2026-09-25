# Herkunft

- Projekt: https://github.com/subham1920/FakePlayerFarm (subham1920 / FakePlayerFarm).
- Ausgangscommit: `ca8bde931e7c71b7b32b6e58432d5ddcbb5a3e74`, 2026-08-20 04:36:35 UTC.
- Modrinth-Version: `OwTtKRIV`, AFKDummy 1.0.3, veröffentlicht 2026-08-20 04:42:58 UTC.
- Original-JAR: https://cdn.modrinth.com/data/PHiV6JLQ/versions/OwTtKRIV/AFKDummy-1.0.3.jar
- Original-SHA1: `60ceef15cfbc081d2c34dbb38940e2dfd81596c0` (Download geprüft).
- Der Ausgangscommit ist der letzte öffentliche Commit vor Veröffentlichung. Die vollständige `javap -c -p`-Ausgabe der kritischen Klasse `DummyPlayer` aus dem unverändert kompilierten Quellstand stimmt mit der veröffentlichten JAR überein. Das ist kein Nachweis eines byteidentischen Rebuilds der gesamten JAR.
- Die späteren Commits vom 21. August sind **nicht** die Grundlage. Insbesondere enthielt die veröffentlichte 1.0.3 noch keine nachträgliche Entfernung aus `PlayerList.players`. Die frühere Analyse des aktuellen `master` traf in diesem Punkt nicht auf die Release-JAR zu.
- Upstream README und Modrinth deklarieren MIT. Das heruntergeladene Quellarchiv enthält keine separate LICENSE-Datei. Urheberangaben bleiben in `plugin.yml` und dieser Datei erhalten; dies ist ein unabhängiger Community-Fork, kein offizielles Upstream-Release. Die hinzugefügte `LICENSE` enthält den MIT-Text auf Grundlage dieser Deklaration; `NOTICE` dokumentiert diese Herkunft, ohne eine originale Copyright-Zeile zu erfinden.

## Kritische Änderungen

- Skin-Eigenschaften werden kopiert. Der ursprüngliche `Unsafe`-Fallback konnte die geteilte `PropertyMap.EMPTY` verändern. Keine Reflexion zur Änderung von Profilnamen mehr.
- Eindeutige, sitzungsgebundene Profilnamen; Besitzer-/Anzeigenamen verändern nicht die Netzwerkidentität.
- Paper verwaltet Entity-Tracking und Player-Info; zusätzliche weltweite Spawn-/Teleport- und doppelte Scoreboard-Erzeugungspakete entfallen.
- Kein Listener auf dem veralteten `PlayerSpawnLocationEvent`: Paper warnt, dass dessen Registrierung die Erstellung echter Spieler vorzieht und undefiniertes Verhalten verursachen kann.
- Veränderbare Plugin-Channel-Menge für den Dummy; NPC-Metadaten vor dem Join; ausgehende Fake-Verbindung verwirft Pakete ohne Netzwerk-Queue.
- Die Spielerlisten bleiben konsistent. Dummies sind daher als Spieler in Serverlisten sichtbar und können von anderen Plugins mitgezählt werden.

Diese Befunde sind konkrete Reparaturen, aber **keine abgeschlossene Reproduktion** des gemeldeten Clientfehlers.

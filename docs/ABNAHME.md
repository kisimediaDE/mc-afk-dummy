# Abnahme und Testgrenzen

## AFKDummyLimited 2.0.0 — Paper 26.3 am 25.09.2026

- JDK 25: `test shadowJar integrationJar` erfolgreich; 1700 Tests bestanden,
  keine Fehler oder übersprungenen Tests.
- Release-JAR auf einem separaten lokalen Paper `26.3-41-a15fed9` (Alpha)
  unter `127.0.0.1:25586` gestartet. Paper-JAR-SHA256:
  `2b77166ee61886a9bc9ab33dc9e4847fa3538b36d9ba6e5f2fa7ed90973aa748`.
- Dummy am Zielort erzeugt, zweiter Spawn desselben Besitzers abgewiesen,
  nach simuliertem Besitzer-Logout weiterhin korrekt registriert und nach
  15 Sekunden automatisch entfernt.
- Sechs Dummies gleichzeitig erzeugt und nach simulierten Besitzer-Logouts
  mit Bukkit-Spielerliste, Welt-Spielerliste und gültigen Entities abgeglichen.
  Ein vorheriger Versuch startete vor Ablauf der ersten Sitzung und wurde
  erwartungsgemäß am Serverlimit abgewiesen; nach Bereinigung bestand der Sechser-Test.
- Beim Shutdown sechs Sitzungen mit rund 170 Sekunden Restzeit gespeichert.
  Rund eine Minute später alle sechs erfolgreich mit derselben Restzeit
  wiederhergestellt (`Restored: 6, Expired: 0, Failed: 0`). Registrierung
  erneut geprüft; nach Entfernen aller Dummies null Online-Spieler.
- Paketprüfung: Lizenzhinweise vorhanden, keine bStats- oder Probe-Klassen in
  der Release-JAR; Quellpaket enthält keine Serverdaten oder Build-Caches.
- Kein echter 26.3-Client-Join, kein Vier-Personen-Test und kein Test mit den
  unten genannten Fremd-Plugins durchgeführt. Der frühere Livebericht gilt
  ausschließlich für den damaligen 26.2-Testbuild.
- Paper 26.3 wird noch als Alpha angeboten. Version 1.0.0 bleibt unverändert
  für Paper 26.2 verfügbar; Version 2.0.0 unterstützt ausschließlich 26.3.

Die folgenden Abschnitte dokumentieren historische Prüfungen für Version 1.0.0
und deren Vorläufer auf Paper 26.2.

## AFKDummyLimited 1.0.0 — Releaseprüfung am 25.09.2026

- JDK 25: `test shadowJar integrationJar --offline --no-daemon` erfolgreich.
- 1700 Tests bestanden, keine Fehler oder übersprungenen Tests.
- Separate Release-JAR auf Paper `26.2-123-5001879` gestartet; bestehender
  Datenordner `plugins/AFKDummy` weiterverwendet.
- 15-Sekunden-Sitzung gestartet; zweiter Spawn desselben Besitzers abgewiesen.
  Nach simuliertem Besitzer-Logout blieb die Sitzung registriert und aktiv.
- Nach Ablauf: null Sitzungen, null Online-Spieler, gespeicherte Daten `[]`;
  sauberer Shutdown ohne zurückbleibenden Dummy. Log: `integration-limited-release.log`.
- Namensänderung aus Befehlen, Tab-Vervollständigung und Menü entfernt.
  Gespeicherte Namensfelder bleiben zum Lesen älterer Daten erhalten.
- Paketprüfung: Lizenz-/Herkunftshinweise enthalten; keine bStats- oder Probe-Klassen
  in der ausgelieferten Plugin-JAR. Quellpaket ohne Serverdaten und Build-Caches.
- Der Vier-Personen-Livebericht unten gilt für den vorherigen Testbuild, nicht
  als erneuter Clienttest dieser Release-JAR. Paper 26.3 bleibt ungeprüft.

## Historisch lokal geprüft — Paper 26.2

- JDK 25, Paper **26.2-123-5001879**, isolierter Server auf 127.0.0.1:25585.
- Paper-JAR-SHA256: `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`.
- Abschließender Build: **1700 Tests, 0 Fehler, 0 fehlgeschlagene Tests, 0 übersprungene Tests**. Enthalten sind Dauerparser, Konfiguration, Persistenz, Profilisolation, Backup und Reihenfolge asynchroner Speicherungen sowie die an das neue Verhalten angepasste Upstream-Suite.
- Tatsächliches Spawnen am Zielort, Ablehnung eines zweiten Dummys pro Besitzer, automatisches Entfernen nach 15 Sekunden und leere Speicherung danach.
- Speicherung beim Shutdown: 179971 ms Restzeit. Nach mehr als zwei Minuten Serverauszeit Wiederherstellung mit Anzeige 00:02:59.
- Sechs verwaltete Dummies an verschiedenen Orten, danach simulierte Besitzer entfernt. Prüfung von Bukkit-Spielerliste, Welt-Spielerliste und gültigen Entities erfolgreich.
- Alle sechs Sitzungen liefen anschließend ab; die Serverliste meldete danach null Spieler. Beim späteren Shutdown waren keine geladenen Overworld-Chunkholder mehr zu speichern.
- Natürliche Monster-Spawns nachts und Weizenwachstum ohne Besitzer beobachtet. Wachstumstest mit `random_tick_speed=300` auf einer isolierten flachen Testwelt; kein Nachweis identischer Farm-Ausbeute zu einem Vanilla-Spieler.
- Der separate Testtreiber liegt in `src/integration`. Seine JAR wird **nicht** mit ausgeliefert und gehört niemals auf den produktiven Server.

## Livebericht des Serverteams — gemeldet am 25.09.2026

Am Vortag wurde der bisherige Testbuild mit vier Personen auf dem Server getestet.
Laut Betreiber funktionierten wiederholte Beitritte, einzelne und mehrere Dummies,
kurze und lange Laufzeiten, Betrieb über Nacht und ein Server-Neustart problemlos.
Die einzige gemeldete Einschränkung war die Namensänderung. Diese Benutzerfunktion
wurde in der Community-Version aus Menü, Chat-Eingabe und Befehlen entfernt.

Dies ist ein vom Betreiber berichteter Live-Test, kein hier beobachteter Testlauf.
Exakte Client-/Plugin-Versionen, die Dimensionen und Messwerte wurden nicht
mitgeliefert. Die nachfolgenden Punkte bleiben deshalb eine weiterführende
Testmatrix und widersprechen dem erfolgreichen Livebericht nicht.

## Weiterführende Abnahme und offene Messungen

1. Auf einer Kopie eures Servers zunächst nur den Fork testen. Vanilla-26.2-Client: Besitzer und zweiter Spieler verbinden sich mehrfach bei aktivem Dummy; danach dasselbe mit eurem Client-Mod-Setup.
2. Dummies in gleicher/anderer Dimension sowie innerhalb/außerhalb der Sichtweite testen. Skinwechsel und Teleportieren prüfen. Im Client darf kein „Network Protocol Error“ auftreten. Die Umbenennung gehört nicht mehr zum Funktionsumfang.
3. Dasselbe mit euren **exakten Plugin-Versionen**: Calcmod, Deathchest, DecentHolograms, DiscordSRV, DropHead, FasterHappyGhats, LuckPerms, Simple-Frames, SimpleCrops, StatusPlugin, Timber, TradeCycle, Voicechat.
4. Farm-Erträge mit einem echten AFK-Spieler am gleichen Standort vergleichen: Pflanzen einschließlich SimpleCrops, Mobfarm, Hopper und Redstone. Spawnabstände und Mobcaps müssen gleich bleiben.
5. Bei gleicher Farm-/Spielerlast 0, 1, 2 und 6 Dummies jeweils mindestens fünf Minuten messen: Tickzeiten (Median/p95), TPS, Speicher nach GC, Entity-/Chunkzahlen und Join-Zeiten. Lokale TPS-Werte sind keine Kapazitätszusage für euren Server.
6. Mehrere Spawn-/Remove-Zyklen, Ablauf während geöffnetem Menü, Besitzer-Logout, Server-Neustart und fehlende Welt prüfen. Nach Entfernen aller Dummies dürfen keine durch sie gehaltenen Chunks oder verwaisten Fake-Spieler verbleiben.
7. Bei Disconnect den vollständigen **Client**-Log bzw. Disconnect-Bericht und den zeitgleichen Server-Log sichern. Die einzelne Serverzeile „Disconnected“ identifiziert kein defektes Paket.

Die lokale Bibliothek `minecraft-protocol` 1.68.0 bietet keine 26.2-Paketdefinitionen. Deshalb wurde kein erfolgreicher echter Client-Join behauptet. Die aufgelisteten Fremd-Plugins/Client-Mods sind hier nicht vorhanden. Paper 26.3 ist nicht freigegeben; der Fork deaktiviert sich dort.

## Reproduzierbarer lokaler Test

Mit JDK 25: `gradlew.bat test shadowJar integrationJar`. Auf einem separaten lokalen Paper-123-Server beide erzeugten JARs laden. Nur in dessen Konsole:

```text
afkprobe 1 15000
afkdummy list
afkprobe clear
afkprobe 6 180000
afkprobe logout
afkprobe inspect
afkprobe clear
stop
```

Zwischen Spawn und Ablaufprüfung mindestens 16 Sekunden warten. Für den Pausentest einen Dummy mit 180000 ms starten, `stop`, länger als eine Minute warten und erneut starten. `afkprobe inspect` prüft Registrierung und zeigt Restzeit/Monster-/Chunkzahlen. Der Probe verwendet virtuelle Spieler, keinen Vanilla-Client.

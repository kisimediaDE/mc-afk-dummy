<div align="center">

# AFKDummyLimited

### Du gehst offline. Deine Farm bleibt aktiv.

Kostenlose, zeitlich begrenzte AFK-Dummies für Paper.

![Paper 26.2](https://img.shields.io/badge/Paper-26.2-38bdf8?style=for-the-badge)
![Java 25](https://img.shields.io/badge/Java-25-f59e0b?style=for-the-badge)
![MIT](https://img.shields.io/badge/Lizenz-MIT-a78bfa?style=for-the-badge)
![Community](https://img.shields.io/badge/Edition-Community-34d399?style=for-the-badge)

[Schnellstart](#schnellstart) · [Befehle](#befehle) · [Konfiguration](#konfiguration) · [Tests](#tests--kompatibilität) · [Herkunft](#open-source--herkunft)

</div>

---

Ein Dummy übernimmt deinen Platz an der Farm und bleibt auch nach deinem Logout aktiv. Du bestimmst die Laufzeit im Menü oder per Befehl. Nach Ablauf wird er automatisch entfernt. Ohne Itemkosten, Economy-Plugin oder Client-Mod.

> **Unabhängiger Fork von AFKDummy 1.0.3.** Der Schwerpunkt liegt auf kostenlosen Sitzungen, zuverlässiger Restzeitspeicherung und Reparaturen für Paper 26.2. Der ursprüngliche Autor und das ursprüngliche Projekt bleiben ausdrücklich genannt.

## Was drin ist

| Für Spieler | Für Serverbetreiber |
| :--- | :--- |
| Menü mit 30 Minuten bis 8 Stunden als Vorauswahl | Einstellbare Dauer und Dummy-Limits |
| Freie Zeitangaben wie `30m`, `2h` und `1h30m` | Standard: 1 Dummy pro Spieler, 6 serverweit |
| Farmbetrieb auch nach dem Logout | Restzeit pausiert während der Serverauszeit |
| Restlaufzeit anzeigen und vorzeitig entfernen | Backup vor der ersten Datenmigration |
| Dummy versetzen und Skin ändern | Keine bStats-Telemetrie oder Upstream-Updateabfragen |

## Schnellstart

**Voraussetzungen:** Paper **26.2**, Java **25**. Referenzbuild: `26.2-123-5001879`.

1. Server vollständig stoppen.
2. `AFKDummyLimited-1.0.0.jar` in den Ordner `plugins` legen.
3. Eine vorhandene AFKDummy-JAR vorher herausnehmen: Es darf nur eine Variante installiert sein.
4. Server starten und `/afkdummy` öffnen.

```text
/afkdummy spawn 1h30m
/afkdummy status
/afkdummy remove
```

### Von AFKDummy oder unserem Testbuild wechseln

Den vorhandenen Ordner **`plugins/AFKDummy` behalten**. Technischer Pluginname, Befehle und Berechtigungen bleiben kompatibel. Beim ersten Start mit dem neuen Restzeitformat werden vorhandene Konfigurationen und Sitzungsdaten in `backup-before-free-timer-*` gesichert. Beim Wechsel vom bisherigen Testbuild wird die bereits erfolgte Migration nicht wiederholt.

**Eine vorhandene `config.json` hat Vorrang vor `config.yml`.** Alte Kostenfelder werden ignoriert; bestehende Limits werden übernommen. Für die Standardwerte unten eigene ältere Einstellungen gegebenenfalls anpassen.

Zum Zurückwechseln den Server stoppen und ursprüngliche JAR **sowie passende gesicherte Konfiguration und Sitzungsdaten** wiederherstellen. Das Original versteht die pausierte Restzeit nicht. Kein Server-`/reload` verwenden.

## Befehle

| Befehl | Funktion |
| :--- | :--- |
| `/afkdummy` | Verwaltungsmenü öffnen |
| `/afkdummy spawn <Dauer>` | Dummy am eigenen Standort starten |
| `/afkdummy status` | Restlaufzeit anzeigen |
| `/afkdummy remove` | Eigenen Dummy entfernen |
| `/afkdummy tp` | Eigenen Dummy zum aktuellen Standort versetzen |
| `/afkdummy skin <Spieler>` | Skin eines Minecraft-Spielers übernehmen |
| `/afkdummy list` | Aktive Sitzungen auflisten — Admin |
| `/afkdummy despawnall` | Alle Sitzungen beenden — Admin |
| `/afkdummy reload` | Plugin-Konfiguration neu laden — Admin |
| `/afkdummy debug` | Diagnose anzeigen — Admin |

`/dummy` ist ein Alias. `afkdummy.use` ist für Spieler standardmäßig erlaubt, `afkdummy.admin` für Operatoren. Bei administrativ erhöhtem Besitzerlimit beziehen sich Entfernen und Versetzen auf einen eigenen Dummy; das Menü bietet zusätzlich das Entfernen aller eigenen Dummies per Shift-Klick.

**Dauer:** `d`, `h`, `m`, `s`, in dieser Reihenfolge kombinierbar. Null, negative Werte und Angaben über dem konfigurierten Maximum werden abgewiesen. Ein erneuter Spawn bei ausgeschöpftem Limit verlängert die bestehende Sitzung nicht.

Die fehlerhafte **Umbenennungsfunktion wurde aus Menü und Befehlen entfernt**. Bereits gespeicherte Namensfelder bleiben für die Datenkompatibilität erhalten.

## Konfiguration

`plugins/AFKDummy/config.yml`:

```yaml
settings:
  max-dummies-per-player: 1
  max-server-wide-dummies: 6
  max-duration: '24h'
  duration-options: ['30m', '1h', '2h', '4h', '8h']
  respawn-delay-ticks: 40
  debug: false
```

Bei JSON stehen dieselben Schlüssel im Objekt `settings`. Das Menü unterstützt bis zu 18 Zeitoptionen. Ungültige Optionen werden mit einer Warnung übersprungen; eine ungültige maximale Laufzeit fällt auf 24 Stunden zurück. Jede neue Sitzung ist befristet und kostenlos.

<details>
<summary><strong>So funktionieren Restzeit und Speicherung</strong></summary>

- Während der Server läuft, wird die Laufzeit anhand verstrichener Zeit berechnet. Besitzer-Logout und niedrige TPS halten diese Uhr nicht an.
- Ablaufprüfung etwa einmal pro Sekunde; bei einem blockierten Serverthread entsprechend später.
- Speicherung bei Zustandsänderungen, sauberem Shutdown und etwa alle 30 Sekunden.
- Während der Server ausgeschaltet ist, bleibt die gespeicherte Restzeit erhalten.
- Bei einem Absturz kann der Fortschritt seit der letzten erfolgreichen Speicherung verloren gehen; bei normaler TPS ungefähr 30 Sekunden.
- Alte Sitzungen werden beim ersten Umstieg aus ihrem bisherigen Ablaufzeitpunkt umgerechnet. Bereits abgelaufene Sitzungen werden nicht wiederbelebt.
- Fehlende Welten oder ausgeschöpfte Limits lassen gespeicherte Sitzungen pausiert. Die Wiederherstellung wird beim nächsten Neustart erneut versucht.
- Beschädigte Sitzungsdaten brechen die Initialisierung ab, statt die Datei leer zu überschreiben.

</details>

## Tests & Kompatibilität

| Bereich | Stand |
| :--- | :--- |
| Paper `26.2-123-5001879` / Java 25 | Lokale Server- und Lifecycle-Tests erfolgreich |
| Livebetrieb mit vier Personen | Vom Serverteam erfolgreich gemeldet: wiederholte Joins, einzelne/mehrere Dummies, kurze/lange Laufzeiten, Nachtbetrieb und Neustart |
| Namensänderung | Im Live-Test fehlerhaft; Benutzerfunktion entfernt |
| Pflanzen und natürliche Mobspawns | Lokal beobachtet; kein Nachweis identischer Farm-Ausbeute |
| Paper 26.3 | **Nicht unterstützt**; eigene Prüfung erforderlich |
| Andere Paper-Builds, Forks und Client-/Plugin-Kombinationen | Keine pauschale Kompatibilitätszusage |

Der Livebericht bezieht sich auf den vorherigen Testbuild. Die Community-JAR wird separat gebaut und lokal geprüft. Details und verbleibende Tests stehen im [Testprotokoll](docs/ABNAHME.md).

### Betrieb auf kleinen Servern

Dummies aktivieren echte Farmarbeit: Chunks, Entities, Mobspawns und Redstone kosten Rechenzeit. **Sechs Dummies sind ein Funktionslimit, keine Leistungszusage** für zwei CPU-Kerne und 6 GB RAM. Simulation Distance und Farmbau bestimmen die Last.

Dummies bleiben in Papers Spielerregistrierung und können in Spielerzahlen oder anderen Plugins mitgezählt werden. Sie tragen `NPC`-Metadaten und werden beim Schlafen ignoriert. Farmen unterliegen weiterhin den normalen Welt-, Mobcap- und Abstandsregeln. Skin-Abfragen können Mojangs Dienste kontaktieren.

## Selbst bauen

JDK 25 installieren und `JAVA_HOME` darauf setzen:

```powershell
.\gradlew.bat test shadowJar
```

Unter Linux/macOS: `./gradlew test shadowJar`. Die fertige Plugin-JAR liegt in `build/libs`. Der Build nutzt fest `paperweight.paperDevBundle("26.2.build.123-stable")`.

`integrationJar` erstellt einen **separaten lokalen Testtreiber**. Er gehört nicht auf einen produktiven Server und ist nicht Bestandteil des Releasepakets. `test` allein startet keinen Minecraft-Server.

## Fehler melden

Für einen Bericht bitte Pluginversion, Paper-Build, Schritte zum Nachstellen und die zeitlich passenden Logs angeben. Bei einem Disconnect ist besonders der **Client-Log oder Disconnect-Bericht** hilfreich; `Disconnected` allein genügt nicht. Den Bericht beim Betreiber dieser Community-Version einreichen.

## Open Source & Herkunft

Dieser Fork basiert auf **[AFKDummy / FakePlayerFarm](https://github.com/subham1920/FakePlayerFarm)** von **subham1920 / FakePlayerFarm und Mitwirkenden**, Ausgangsstand AFKDummy 1.0.3. Es handelt sich um eine Weiterentwicklung mit umfangreich übernommenem Originalcode.

Die ursprüngliche README und [Modrinth-Seite](https://modrinth.com/plugin/afkdummy) deklarieren MIT. Da der verwendete Quellstand keine separate Lizenzdatei enthielt, dokumentiert [NOTICE](NOTICE) die Herkunft der hier beigefügten [MIT-Lizenz](LICENSE). Weitere Informationen: [Änderungen](CHANGELOG.md), [Quellstand](docs/UPSTREAM.md), [Drittanbieter-Lizenzen](THIRD_PARTY_NOTICES.md).

<div align="center">

<sub>Ein unabhängiges Community-Projekt. Kein offizielles Minecraft-Produkt und nicht mit Mojang oder Microsoft verbunden.</sub>

</div>

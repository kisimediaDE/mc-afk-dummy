<div align="center">

# AFKDummyLimited

### Feierabend für dich. Weiterbetrieb für deine Farm.

Setze einen Dummy, wähle die Laufzeit und geh offline.<br>
Dein Platz an der Farm bleibt besetzt — bis die Zeit abläuft.

![Paper 26.2](https://img.shields.io/badge/PAPER-26.2-38bdf8?style=for-the-badge&labelColor=0f172a)
![Java 25](https://img.shields.io/badge/JAVA-25-a78bfa?style=for-the-badge&labelColor=0f172a)
![Kostenlos](https://img.shields.io/badge/KOSTENLOS-OHNE_ITEMKOSTEN-34d399?style=for-the-badge&labelColor=0f172a)

<br>

[![Jetzt herunterladen](https://img.shields.io/badge/↓_JETZT_HERUNTERLADEN-34d399?style=for-the-badge&labelColor=0f172a)](https://github.com/kisimediaDE/mc-afk-dummy/releases/latest)

[Installation](#installation) &nbsp; · &nbsp; [Loslegen](#loslegen) &nbsp; · &nbsp; [Befehle](#befehle) &nbsp; · &nbsp; [Einstellungen](#einstellungen) &nbsp; · &nbsp; [FAQ](#faq)

</div>

---

## Deine Farm. Deine Zeit.

AFKDummyLimited bringt zeitlich begrenzte AFK-Dummies auf deinen Paper-Server. Spieler bedienen sie direkt im Spiel; als Serverbetreiber bestimmst du Laufzeiten und Limits. Kein Economy-Plugin, keine Itemkosten und keine zusätzliche Client-Mod nötig.

| 💤 Offline gehen | ⏱️ Laufzeit wählen | 🔄 Neustart? Kein Zeitverlust. |
| :--- | :--- | :--- |
| Dein Dummy bleibt nach dem Logout an der Farm. | Per Menü oder frei eingegeben, etwa `1h30m`. Nach Ablauf verschwindet er automatisch. | Bei sauberem Herunterfahren wird die Restzeit gespeichert. Während der Server aus ist, pausiert sie. |

## Installation

**Du brauchst Paper 26.2 und Java 25.** Paper 26.3 wird nicht unterstützt.

1. **Herunterladen:** Auf der [Downloadseite](https://github.com/kisimediaDE/mc-afk-dummy/releases/latest) unter **Assets** die Datei `AFKDummyLimited-…jar` auswählen.
2. **Server stoppen:** Den Server vollständig herunterfahren.
3. **Plugin ablegen:** Die heruntergeladene JAR in den Ordner `plugins` kopieren. Eine bereits vorhandene AFKDummy-JAR vorher entfernen.
4. **Starten:** Server hochfahren und im Spiel `/afkdummy` eingeben.

> **Schon AFKDummy installiert?** Behalte den Ordner `plugins/AFKDummy`, damit deine Einstellungen und gespeicherten Sitzungen erhalten bleiben. Tausche nur die JAR aus. Für Installation und Updates den Server immer vollständig neu starten.

## Loslegen

### Hinstellen. Zeit wählen. Ausloggen.

Stell dich an deine Farm und öffne mit **`/afkdummy`** das Menü. Wähle eine Laufzeit — standardmäßig von **30 Minuten bis 8 Stunden** — und starte deinen Dummy.

Lieber direkt per Befehl? So bleibt dein Dummy **eine Stunde und 30 Minuten**:

```text
/afkdummy spawn 1h30m
```

Mit `/afkdummy status` siehst du die Restzeit. Mit `/afkdummy remove` beendest du die Sitzung vorzeitig.

**Ab Werk:** ein Dummy pro Spieler, maximal sechs auf dem Server und bis zu 24 Stunden pro Sitzung.

## Befehle

`/dummy` funktioniert als Kurzform von `/afkdummy`.

| Befehl | Das passiert |
| :--- | :--- |
| `/afkdummy` | Menü öffnen |
| `/afkdummy spawn <Dauer>` | Dummy an deinem Standort starten |
| `/afkdummy status` | Verbleibende Laufzeit anzeigen |
| `/afkdummy remove` | Deinen Dummy entfernen |
| `/afkdummy tp` | Deinen Dummy zu dir versetzen |
| `/afkdummy skin <Spieler>` | Den Skin eines Minecraft-Spielers übernehmen |

**Zeitangaben:** `30m` für 30 Minuten, `2h` für zwei Stunden oder `1h30m` für beides. Unterstützt werden Tage (`d`), Stunden (`h`), Minuten (`m`) und Sekunden (`s`) in dieser Reihenfolge, bis zum eingestellten Maximum.

<details>
<summary><strong>🛠️ Verwaltung & Berechtigungen</strong></summary>

| Befehl | Funktion |
| :--- | :--- |
| `/afkdummy list` | Alle aktiven Sitzungen anzeigen |
| `/afkdummy despawnall` | Alle Dummies entfernen |
| `/afkdummy reload` | Plugin-Einstellungen neu laden |

Spieler dürfen das Plugin standardmäßig nutzen. Mit einem Berechtigungsplugin kannst du den Zugriff steuern:

| Berechtigung | Zugriff | Standard |
| :--- | :--- | :--- |
| `afkdummy.use` | Spielermenü und eigene Dummies | Alle Spieler |
| `afkdummy.admin` | Verwaltungsbefehle | Operatoren |

Wenn du mehrere Dummies pro Spieler erlaubst, beziehen sich Entfernen und Versetzen per Befehl auf einen eigenen Dummy. Im Menü lassen sich per Shift-Klick alle eigenen Dummies entfernen.

</details>

## Einstellungen

Nach dem ersten Start findest du die Einstellungen unter **`plugins/AFKDummy/config.yml`**. Damit legst du fest, wie viele Dummies erlaubt sind und welche Laufzeiten im Menü erscheinen. Die wichtigsten Werte im Überblick:

```yaml
settings:
  max-dummies-per-player: 1
  max-server-wide-dummies: 6
  max-duration: '24h'
  duration-options: ['30m', '1h', '2h', '4h', '8h']
```

| Einstellung | Bedeutung |
| :--- | :--- |
| `max-dummies-per-player` | Maximale Anzahl pro Spieler |
| `max-server-wide-dummies` | Maximale Anzahl auf dem gesamten Server |
| `max-duration` | Längste erlaubte Sitzung |
| `duration-options` | Laufzeiten im Menü; bis zu 18 Auswahlmöglichkeiten |

Die übrigen Werte kannst du auf der Voreinstellung lassen. Speichere deine Änderungen und lade sie mit `/afkdummy reload` neu.

> **Bei einer bestehenden Installation:** Falls `plugins/AFKDummy/config.json` vorhanden ist, wird diese anstelle der `config.yml` verwendet. Ändere dann die entsprechenden Werte im Abschnitt `settings` der JSON-Datei.

## FAQ

<details>
<summary><strong>Läuft meine Farm genauso wie mit einem echten Spieler?</strong></summary>

Der Dummy hält deinen Platz an der Farm besetzt. Wie viel die Farm produziert, hängt weiterhin von Farmbau, Simulation Distance, Moblimits und den Regeln deiner Welt ab. Eine identische Ausbeute ist nicht garantiert.

</details>

<details>
<summary><strong>Was passiert beim Logout oder Serverneustart?</strong></summary>

Beim Logout bleibt dein Dummy aktiv und die Zeit läuft weiter. Beim sauberen Herunterfahren wird die Restzeit gespeichert; während der Server aus ist, vergeht keine Sitzungszeit. Nach dem Start werden gespeicherte Dummies wiederhergestellt, sofern ihre Welt verfügbar ist und die Limits es zulassen.

Bei einem Serverabsturz kann der seit der letzten Speicherung vergangene Zeitraum verloren gehen — normalerweise bis zu etwa 30 Sekunden.

</details>

<details>
<summary><strong>Wie viele Dummies verträgt mein Server?</strong></summary>

Das hängt von deinen Farmen und der Serverleistung ab. Aktive Farmen verursachen auch mit Dummies Rechenlast. Starte mit wenigen Dummies und erhöhe das Limit nur, wenn dein Server flüssig läuft. Die Voreinstellung von sechs ist ein Mengenlimit, keine Leistungszusage.

</details>

<details>
<summary><strong>Beeinflussen Dummies Schlafen oder Spielerzahlen?</strong></summary>

Beim Schlafen werden Dummies ignoriert. In der Spielerzahl und bei anderen Plugins können sie jedoch als Spieler mitgezählt werden.

</details>

<details>
<summary><strong>Kann ich eine laufende Sitzung verlängern?</strong></summary>

Ein erneuter Spawn verlängert eine bestehende Sitzung nicht. Entferne deinen Dummy und starte eine neue Sitzung mit der gewünschten Dauer.

</details>

---

<div align="center">

**Bereit für deine nächste AFK-Pause?**

[Plugin herunterladen](https://github.com/kisimediaDE/mc-afk-dummy/releases/latest) &nbsp; · &nbsp; [Zur Installation](#installation)

<br>

<sub>Basierend auf AFKDummy / FakePlayerFarm von subham1920 und Mitwirkenden.<br>
<a href="NOTICE">Herkunft</a> · <a href="LICENSE">MIT-Lizenz</a> · <a href="THIRD_PARTY_NOTICES.md">Drittanbieter-Lizenzen</a><br>
Kein offizielles Minecraft-Produkt. Nicht mit Mojang oder Microsoft verbunden.</sub>

</div>

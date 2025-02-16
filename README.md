# Internet Radio

Mobilní aplikace pro Android, která umožňuje poslech internetových rádií. Aplikace je napsaná v Kotlinu s využitím moderních technologií a postupů.

## Funkce

- 📻 Přehrávání internetových rádií
- 🗂️ Kategorizace stanic (Pop, Rock, Jazz, Dance, atd.)
- ⭐ Správa oblíbených stanic
- 🎵 Ukládání oblíbených skladeb
- 📍 Automatické vyhledávání místních stanic
- 🎚️ Ekvalizér s předvolbami
- ⏰ Časovač vypnutí s postupným snižováním hlasitosti
- 🎯 Widget pro rychlé ovládání
- ⌚ Podpora pro Wear OS
- 🔄 Export/Import nastavení a oblíbených stanic

### Hlavní výhody

- 🎨 Moderní Material Design
- 🔍 Vyhledávání stanic z Radio Browser API
- 📱 Responzivní UI pro různé velikosti obrazovek
- 🌙 Tmavý režim
- 🔊 Vysoká kvalita zvuku
- 🔋 Nízká spotřeba baterie

## Instalace

1. Stáhněte si nejnovější verzi APK z [releases](https://github.com/Morganczech/internetradio/releases)
2. Povolte instalaci z neznámých zdrojů v nastavení Android:
   - Otevřete Nastavení > Zabezpečení
   - Povolte "Instalace z neznámých zdrojů" nebo "Instalovat neznámé aplikace"
3. Otevřete stažený APK soubor a nainstalujte aplikaci
4. Po instalaci můžete zakázat "Instalace z neznámých zdrojů"

## Použití

### První spuštění
- Při prvním spuštění se automaticky načtou místní stanice podle vaší lokace
- Můžete procházet kategorie nebo vyhledávat nové stanice
- Přidejte si oblíbené stanice pro rychlý přístup

### Hlavní funkce
- **Přehrávání**: Klikněte na stanici pro spuštění přehrávání
- **Oblíbené**: Přidejte stanici do oblíbených pomocí hvězdičky
- **Vyhledávání**: Použijte vyhledávací pole pro nalezení nových stanic
- **Časovač**: Nastavte časovač vypnutí v menu nastavení
- **Ekvalizér**: Upravte zvuk pomocí ekvalizéru
- **Export/Import**: Zálohujte své nastavení a oblíbené stanice

## Technologie

- 🎯 Kotlin
- 🎨 Jetpack Compose
- 🎵 Media3 ExoPlayer
- 💉 Hilt (Dependency Injection)
- ⚡ Kotlin Coroutines & Flow
- 💾 Room Database
- 🏗️ MVVM architektura

## Požadavky

- Android 8.0 (API level 26) nebo vyšší
- Připojení k internetu
- Minimálně 50 MB volného místa

## Vývoj

Pro vývoj budete potřebovat:
- Android Studio Hedgehog nebo novější
- JDK 17
- Android SDK

Klonování repozitáře:
```bash
git clone https://github.com/Morganczech/internetradio.git
```

### Sestavení projektu
1. Otevřete projekt v Android Studiu
2. Synchronizujte Gradle
3. Spusťte build
4. Pro vytvoření APK použijte "Build > Build Bundle(s) / APK(s) > Build APK(s)"

## Licence

Tento projekt je licencován pod [MIT licencí](LICENSE).

## Poděkování

- [Radio Browser API](https://api.radio-browser.info/) za poskytnutí databáze rádií
- Všem přispěvatelům a testerům 
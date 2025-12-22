# Touch Radio 📻

Moderní mobilní aplikace pro Android, která umožňuje poslech tisíců internetových rádií z celého světa. Aplikace je postavená na nejnovějších technologiích (Jetpack Compose, Media3) s důrazem na stabilitu, nízkou spotřebu a skvělý uživatelský zážitek.

## Funkce

- 🔊 **Přehrávání na pozadí**: Stabilní streamování pomocí Media3 ExoPlayeru s plnou integrací do systémového ovládání a zamykací obrazovky.
- 🗂️ **Kategorizace**: Stanice rozdělené podle žánrů (Pop, Rock, Jazz, Dance, atd.).
- ❤️ **Oblíbené**: Snadná správa oblíbených stanic pomocí ikony srdíčka.
- 📑 **Vlastní řazení**: Dedikovaný režim pro uspořádání stanic pomocí madel (Drag & Drop).
- 🎵 **Skladby**: Možnost ukládání informací o právě hrajících skladbách do seznamu oblíbených.
- 📍 **Lokální obsah**: Automatické vyhledávání stanic podle vaší aktuální polohy.
- 🔍 **Inteligentní hledání**: Vyhledávání stanic v databázi Radio Browser bez nutnosti psát diakritiku (např. "cesky" najde "Český").
- 🎚️ **Audio vylepšení**: Pětipásmový ekvalizér s předvolbami.
- ⏰ **Sleep Timer**: Časovač vypnutí s plynulým zeslabováním zvuku (Fade-out).
- 🎯 **Widget**: Ovládání přehrávače přímo z plochy telefonu.
- ⌚ **Wear OS**: Doprovodná aplikace pro vaše chytré hodinky.
- 🔄 **Záloha**: Export a import kompletního nastavení a seznamů do JSON souboru.

### Hlavní výhody

- 🎨 **Moderní UI**: Čistý design s podporou světlého i tmavého režimu.
- 🔋 **Úspora dat a baterie**: Efektivní správa síťových prostředků a procesoru.
- 📱 **Plná podpora Android 13+**: Korektní správa oprávnění pro notifikace a polohu.

## Instalace

1. Stáhněte si nejnovější verzi APK z [releases](https://github.com/Morganczech/internetradio/releases).
2. Povolte instalaci z neznámých zdrojů v nastavení Androidu.
3. Otevřete APK a nainstalujte aplikaci.

## Použití

### První spuštění
- Aplikace vás přivítá krátkým dialogem s vysvětlením potřebných oprávnění.
- Automaticky se načtou místní stanice podle vaší lokace (pokud je povolena).

### Tipy pro ovládání
- **Hledání**: Kliknutím na lupu v horní liště aktivujete filtr uložených stanic. Ikona "+" slouží k hledání nových stanic na internetu.
- **Řazení**: Klikněte na ikonu seznamu v horní liště pro aktivaci režimu přesouvání karet.
- **Přehrávač**: Kliknutím na kartu hrající stanice rozbalíte pokročilé ovládání (hlasitost, časovač, bitrate).

## Technologie

- **Kotlin**: 100% čistý kód.
- **Jetpack Compose**: Moderní deklarativní UI.
- **Media3 ExoPlayer**: Špičkový engine pro zpracování audia.
- **Hilt**: Dependency Injection pro čistou architekturu.
- **Room**: Lokální SQLite databáze.
- **MVVM**: Ověřený architektonický vzor.

## Požadavky

- Android 8.0 (API 26) nebo novější.
- Připojení k internetu (Wi-Fi nebo mobilní data).
- Cílové SDK: 34 (Android 14).

## Licence

Tento projekt je licencován pod [MIT licencí](LICENSE).

## Poděkování

- [Radio Browser API](https://api.radio-browser.info/) za poskytnutí globální databáze rádií.
- Všem přispěvatelům a testerům za pomoc s laděním aplikace.

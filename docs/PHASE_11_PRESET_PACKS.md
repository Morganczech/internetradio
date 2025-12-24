# Phase 11 – Předpřipravené balíčky stanic (Preset Packs)

## Cíl
Umožnit uživatelům rychle začít pomocí předpřipravených seznamů stanic (balíčků), které lze jedním klikem importovat do aplikace.
Bez účtů, bez backendu, bez složité logiky.

## Základní princip
- Balíčky jsou statické JSON soubory
- Hostované externě (např. GitHub / statický hosting)
- Aplikace je stáhne a naimportuje podobně jako Import nastavení
- Není potřeba aktualizace aplikace při přidání nového balíčku

## UI návrh (high-level)
Nová obrazovka v Nastavení (nebo samostatná položka):
**„Předpřipravené balíčky stanic“**

Každý balíček:
- Název
- Krátký popis
- Počet stanic / kategorií
- Tlačítko **Importovat**

## Chování při importu
1. Aplikace stáhne JSON balíčku
2. Validuje strukturu
3. Zobrazí potvrzení: *Importovat X stanic do Y kategorií?*
4. Importuje stanice do lokální DB

## Režim importu (zatím jednoduchý)
- **Výchozí:** Přidat k existujícím stanicím
- *(Rozšíření do budoucna: Nahradit / Sloučit – mimo scope této fáze)*

## Offline chování
- Pokud není internet:
  - Zobrazí se hláška „Vyžaduje připojení k internetu“
  - Žádný pokus o import
  - Žádný fallback, žádné náhodné balíčky

## Technické poznámky
- Balíčky mají vlastní JSON strukturu (oddělenou od exportu nastavení)
- Import využívá existující DB / Repository logiku
- Bez uživatelských účtů
- Bez hodnocení, sdílení, marketplace

## Přínos
- Rychlý start pro nové uživatele
- Žádná složitost navíc pro pokročilé
- Možnost rozšiřování obsahu bez release aplikace
- Přirozené rozšíření existující funkce Export / Import

## Stav
📌 Plánováno do budoucna
📌 Neimplementovat nyní
📌 Slouží jako koncept a směr pro další verze

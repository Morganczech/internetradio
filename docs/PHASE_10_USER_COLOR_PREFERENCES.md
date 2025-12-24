# Phase 10 – User Color Preferences (Planned)

Cílem této fáze je umožnit uživateli upravit barevný akcent aplikace, aniž by došlo k narušení čitelnosti, kontrastu nebo konzistence UI.

**Status:** Plánováno (Future Release)

## Motivace
- Ne všichni uživatelé preferují výrazné barvy.
- Možnost personalizace zvyšuje komfort a pravděpodobnost dlouhodobého používání aplikace.
- Zachování jednotného chování přehrávače a UI je prioritou.

## Navrhovaný přístup (řízená personalizace)

❌ **Nebude použit** plnohodnotný color picker (RGB/HSL bez omezení), aby se předešlo vizuálnímu chaosu a problémům s kontrastem.
✅ **Použije se** jeden z bezpečných modelů:

### Varianta A – Předdefinované palety
- **Barevný** (dle kategorií – výchozí stav)
- **Klidný šedý** (již částečně řešeno ve Fázi 9)
- **Studený** (modro-šedý, teal)
- **Teplý tmavý** (amber, hnědá)
- **Vysoký kontrast** (černo-bílá/žlutá)

### Varianta B – Hue Slider (preferovaná)
- Uživatel mění pouze **odstín (Hue)**.
- **Saturace a světlost** jsou pevně dané (uzamčené), aby byl zaručen kontrast vůči textu a pozadí.
- Gradienty i přehrávač se dynamicky přepočítají podle zvoleného Hue, ale zachovají vizuální styl aplikace.

## Umístění v UI
- **Nastavení aplikace → Vzhled**
- Přepínač/Slider je perzistentní (uložený v AppSettings / DataStore).

## Poznámky k implementaci
Tato fáze navazuje na Fázi 9 (Unified Accent Color). Zatímco Fáze 9 přepíná na jednu konkrétní neutrální barvu, Fáze 10 by tento koncept rozšířila o možnost volby této "jednotné" barvy.

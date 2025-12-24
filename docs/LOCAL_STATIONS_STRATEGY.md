# Strategie Místních Stanic a Offline Handling

Tento dokument popisuje logiku určení regionu pro načítání stanic a chování aplikace v režimu bez internetového připojení. Tato strategie byla implementována ve Fázi 8.

## 1. Určení Regionu (Země)

Aplikace striktně rolišuje mezi **jazykem UI** a **regionem obsahu**.

*   **Jazyk UI (UI Language):** Používá se výhradně pro překlady textů v aplikaci (strings.xml).
*   **Region Obsahu (Content Region):** Určuje, které stanice se zobrazí v kategorii "Místní".
    *   **Zdroj:** `Locale.getDefault().country` (např. "CZ", "SK", "DE", "US").
    *   **Fallback:** Pokud je locale prázdné nebo neplatné, použije se výchozí "CZ".
    *   **Zákaz:** Nepoužívá se jazyk (`Locale.language`) ani IP adresa pro určení obsahu během inicializace databáze.

## 2. Inicializace Databáze ("Místní stanice")

Při prvním spuštění aplikace (nebo po vymazání dat) se seznam místních stanic naplní podle následující priority:

1.  **Seed JSON (Offline Ready)**
    *   Aplikace zkontroluje existenci souboru `stations_{region}.json` v assets (pro region získaný v bodě 1).
    *   Pokud soubor existuje (např. `stations_cz.json`, `stations_sk.json`), obsah se načte do databáze.
    *   Toto funguje i **bez internetu**.

2.  **RadioBrowser API (Online Fallback)**
    *   Pokud seed JSON pro daný region neexistuje (např. uživatel má region "DE"), aplikace ověří dostupnost internetu.
    *   **Pokud je online:** Stáhne top 10 stanic pro danou zemi (řazeno dle hlasů) z API a uloží je do databáze.
    *   **Pokud je offline:** Místní stanice zůstanou **prázdné**. Aplikace nezobrazuje žádné náhodné ani globální stanice. Uživatel uvidí prázdný seznam, což je korektní a očekávané chování.

**Důležité:** Flag `favorites_initialized` se nastaví na `true` pouze po **úspěšném** načtení (ze seedu nebo API). Pokud se inicializace nezdaří (např. offline bez seedu), pokus se bude opakovat při dalším startu aplikace.

## 3. Offline Handling (Řízení přehrávání)

Aby aplikace působila robustně, je vynucena striktní kontrola konektivity.

### Start Přehrávání
1.  Uživatel klikne na **Play** (nebo vybere stanici).
2.  Aplikace okamžitě zkontroluje `isNetworkAvailable()`.
3.  **Pokud je offline:**
    *   Přehrávání se **vůbec nespustí** (nevolá se `RadioService`).
    *   Uživateli se zobrazí hláška (Snackbar): *"Nejste připojeni k internetu"*.
    *   UI zůstane ve stavu "Zastaveno".

### Během Přehrávání
1.  Pokud dojde k chybě přehrávání (loss of connection, stream error), `ExoPlayer` vyvolá chybu.
2.  Služba zachytí chybu a pošle broadcast s `EXTRA_ERROR`.
3.  UI zobrazí chybovou hlášku a přepne stav na "Zastaveno".

## 4. Uživatelské Změny

Pokud uživatel ručně přidá, odebere nebo změní pořadí stanic v kategorii "Místní", aplikace tyto změny respektuje a při dalším startu se již nepokouší o re-inicializaci (díky flagu `favorites_initialized` a kontrole obsazenosti DB).

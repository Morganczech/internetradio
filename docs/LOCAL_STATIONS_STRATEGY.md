# Strategie Místních Stanic a Offline Handling (Aktualizováno)

Cíl: Upravit strategii inicializace stanic a offline chování aplikace podle nových UX pravidel. Zjednodušit logiku, odstranit offline fallbacky a zajistit jasnou zpětnou vazbu uživateli.

## 1. Zrušení Seed JSON souborů (Důležité)

*   **Odstranění:** Seed JSON soubory (`stations_cz.json`, `stations_sk.json` atd.) se kompletně přestanou používat a budou odstraněny z projektu.
*   **Žádná lokální data:** Aplikace již nebude mít žádnou offline databázi předpřipravených stanic. Inicializace probíhá výhradně online.
*   **Důvod:** Zjednodušení údržby a zajištění aktuálnosti stanic.

## 2. Inicializace Databáze ("Místní stanice")

Proces načítání prvních stanic při startu (tzv. onboarding):

1.  **Určení Regionu:**
    *   Výhradně podle `Locale.getDefault().country`.
    *   Žádné odhadování podle IP nebo jazyka.

2.  **Pouze Online (RadioBrowser API):**
    *   Pokus o načtení seznamu stanic proběhne **POUZE pokud je dostupný internet**.
    *   Volá se RadioBrowser API pro daný kód země.

3.  **Offline Stav při inicializaci:**
    *   Pokud **není internet** při prvním spuštění, databáze stanic zůstává **PRÁZDNÁ**.
    *   Nenačtou se žádné náhodné ani globální fallback stanice.
    *   Flag `favorites_initialized` zůstává `false` -> pokus o inicializaci se zopakuje při příštím startu aplikace.

## 3. Offline UX – Empty State (Trvalá hláška)

Zobrazování trvalé hlášky (např. uprostřed obrazovky místo seznamu):

*   **Podmínka zobrazení:**
    1.  Detekován stav **Offline** (žádný internet).
    2.  **A ZÁROVEŇ** uživatel nemá v databázi uloženou **ANI JEDNU stanici** (DB je prázdná).

*   **Obsah hlášky:**
    *   Text: *„Nejste připojeni k internetu. Bez připojení nelze načíst ani vyhledávat stanice.“*
    *   Vizuál: Ikona offline + Text.

*   **Pokud má uživatel stanice:**
    *   Pokud uživatel má alespoň jednu stanici (načtenou z minula), tato trvalá hláška se **NEZOBRAZÍ**. Seznam stanic je viditelný.

## 4. Offline Handling – Playback

Chování při kliknutí na stanici v seznamu:

1.  **Kliknutí (Play):**
2.  **Kontrola:** Okamžitá kontrola konektivity (`isNetworkAvailable`).
3.  **Pokud je OFFLINE:**
    *   **STOP:** Přehrávání se **NESPUSTÍ**.
    *   **Zákaz volání:** `RadioService` se vůbec nevolá.
    *   **Stav UI:** UI zůstává ve stavu "Zastaveno" (žádný loading spinner).
    *   **Feedback:** Zobrazí se **okamžitá hláška** (Snackbar/Toast): *„Nejste připojeni k internetu“*.

## 5. Offline Handling – Search (Vyhledávání)

Chování na obrazovce vyhledávání (`BrowseStationsScreen`):

*   **Offline:** API se nesmí volat.
*   **Chování:** Okamžitě zobrazit textovou informaci: *„Bez připojení k internetu nelze vyhledávat stanice“*.
*   **Stav:** Žádný loading indikátor (spinner), který by běžel do timeoutu.

## 6. Flag `favorites_initialized`

*   **TRUE:** Nastaví se až ve chvíli, kdy se **úspěšně** stáhnou a uloží stanice z API.
*   **FALSE:** Zůstává po celou dobu, dokud se inicializace nepovede (např. trvalý offline). To zajistí, že aplikace se bude pokoušet získat základní sadu stanic, dokud se to nepovede.

---
**Shrnutí pro vývoj:**
Tato strategie eliminuje "stavy neurčitosti". Uživatel buď vidí data z internetu, data z cache (svoje uložené), nebo jasnou informaci, že bez internetu to nejde.

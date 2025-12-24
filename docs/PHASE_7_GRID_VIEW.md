# FÁZE 7: Grid View (Mřížkové zobrazení stanic) – Analýza a Návrh

Tento dokument slouží jako technická specifikace a roadmapa pro budoucí implementaci mřížkového zobrazení stanic v aplikaci Touch Radio.

---

## 1. Cíl a Motivace
Uživatelé chtějí mít možnost volby mezi hustotou informací (List) a vizuální orientací (Grid).
- **List View (Seznam)**: Efektivní pro čtení názvů a metadat.
- **Grid View (Mřížka)**: Vizuálně atraktivnější, klade důraz na loga stanic, zobrazí více položek na jedné obrazovce (2 sloupce).

Cílem je implementovat přepínání těchto dvou režimů v reálném čase se zapamatováním volby.

## 2. Rozsah (Scope)

### Dotčené obrazovky
Grid zobrazení by mělo být dostupné na obrazovkách, které zobrazují seznamy stanic:
1.  **AllStationsScreen** (Hlavní přehled dle kategorií)
2.  **FavoritesScreen** (Oblíbené)
3.  **PopularStationsScreen** (Místní / Populární)
4.  *(Volitelně)* BrowseStationsScreen (Vyhledávání) - *zvážit, zda je u vyhledávání grid vhodný.*

### UI/UX Komponenty
1.  **Přepínač (Toggle)**
    - Umístění: **Horní nástrojová lišta (TopAppBar)**.
    - Ikona: Mění se podle aktuálního/cílového stavu.
        - Zobrazuje se `Icons.Default.GridView`, pokud je aktivní List (kliknutím přepne na Grid).
        - Zobrazuje se `Icons.Default.ViewList`, pokud je aktivní Grid (kliknutím přepne na List).
    - Pozice: Vedle ikony lupy/hledání.

2.  **Karta Stanice (Grid Item)**
    - **Tvar**: Čtverec nebo poměr stran 1:1 / 4:3.
    - **Obsah**:
        - Velké logo stanice (centirované).
        - Název stanice (překryvný text dole nebo pod logem).
        - Indikátor "Oblíbené" (srdíčko v rohu).
        - Kontextové menu (tři tečky v rohu).
    - **Interakce**: Stejné jako v listu (klik = play, long-press/menu = akce).

## 3. Technický Návrh

### 3.1 Správa Stavu (Persistence)
Nastavení zobrazení musí být globální a perzistentní.
- **Data Store / SharedPreferences**: Přidat klíč `view_mode` (enum: `LIST`, `GRID`).
- **AppSettings**: Rozšířit datový model o tuto preferenci.
- **ViewModel**: `RadioViewModel` bude vystavovat `StateFlow<ViewMode> viewMode`.

```kotlin
enum class ViewMode {
    LIST, GRID
}
```

### 3.2 Změny v UI Hierarchii

**Současný stav:**
```kotlin
LazyColumn {
    items(radios) { radio ->
        RadioItem(radio)
    }
}
```

**Nový stav:**
Bude nutné dynamicky volit mezi `LazyColumn` a `LazyVerticalGrid` (nebo použít jen Grid s 1 sloupcem pro List, ale LazyColumn je performantnější pro složité itemy).

Nejčistší řešení v Compose:
```kotlin
Crossfade(targetState = viewMode) { mode ->
    when (mode) {
        ViewMode.LIST -> {
            LazyColumn(...) { items(radios) { RadioItem(...) } }
        }
        ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(radios) { GridRadioItem(...) }
            }
        }
    }
}
```

### 3.3 Komponenta `GridRadioItem`
Nutno vytvořit novou Composable funkci optimalizovanou pro čtverec.
- **Layout**: `Card` -> `Box` (pro pozadí/gradient) -> `Column` (Logo + Text).
- **Gradient**: Musí respektovat barvu stanice, stejně jako v Listu.

## 4. Otevřené Otázky a Rizika

1.  **Drag & Drop v Gridu**:
    - Aktuální implementace řazení ("Reorder Mode") spoléhá na `LazyColumn`.
    - *Řešení*: Řazení (Drag & Drop) povolit **POUZE** v režimu `LIST`. Pokud uživatel zapne režim úprav, aplikace se automaticky přepne do List zobrazení (nebo Grid reorder bude vyžadovat komplexnější knihovnu).

2.  **Konzistence obrázků**:
    - Některá loga jsou širokoúhlá a ve čtverci mohou vypadat malá.
    - *Řešení*: Použít `ContentScale.Fit` a dostatečný padding, nebo `ContentScale.Crop` s blur pozadím (vizuálně náročnější).

3.  **Dlouhé názvy**:
    - Ve čtvercové kartě je méně místa na text.
    - *Řešení*: Ořezávání textu (`maxLines = 2`, `Ellipsis`) je kritické.

## 5. Plán Implementace (Roadmap)

1.  **Backend & State**:
    - Přidat `ViewMode` do `AppSettings` a `RadioViewModel`.
    - Implementovat ukládání/načítání preference.

2.  **UI Components**:
    - Vytvořit `GridRadioItem` (návrh layoutu).
    - Přidat ikonu přepínače do `TopAppBar`.

3.  **Integration**:
    - Refaktorovat `AllStationsScreen` pro podporu podmíněného renderování (List/Grid).
    - Aplikovat na `FavoritesScreen`.

4.  **Testing**:
    - Ověřit perzistenci (pamatuje si to po restartu?).
    - Ověřit chování na různých velikostech displeje.

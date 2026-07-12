# HealthTracker

HealthTracker to otwarta (Open Source) aplikacja na system Android, stworzona z myślą o pełnej prywatności i niezależności użytkownika. Aplikacja działa w 100% offline – wszystkie dane, takie jak pomiary ciała, dzienniki aktywności, trasy GPS czy plany żywieniowe, są zapisywane wyłącznie na Twoim urządzeniu w lokalnej bazie danych SQLite. Aplikacja nie korzysta z rozwiązań chmurowych, nie wysyła danych na zewnętrzne serwery ani nie śledzi aktywności użytkownika.

Aplikacja jest dostępna w języku polskim.

---

## Kluczowe funkcje

*   **Pomiary ciała**: Śledzenie wagi, poziomu tkanki tłuszczowej, mięśniowej oraz innych parametrów fizycznych w czasie.
*   **Treningi i aktywność**: Rejestrowanie ćwiczeń siłowych i sprawnościowych z automatycznym wyliczaniem spalonych kalorii.
*   **Spacery i GPS**: Moduł spacerów rejestrujący czas, kroki oraz rysujący pokonaną trasę na mapie za pomocą lokalnego odbiornika GPS w telefonie.
*   **Jadłospis tygodniowy (Meal Planner)**:
    *   Generowanie zbalansowanych planów posiłków na cały tydzień (opcje dla 3, 4 lub 5 posiłków dziennie) dopasowanych do wyznaczonego celu kalorycznego.
    *   Inteligentny dobór potraw (podział na śniadania, drugie śniadania, obiady i kolacje) z dbałością o urozmaicenie i unikanie powtórzeń w kolejnych dniach.
    *   Możliwość wylosowania alternatywnego posiłku o podobnej kaloryczności.
    *   Szczegółowy podgląd gramatury składników bezpośrednio po rozwinięciu karty danej potrawy.
*   **Lista zakupów**:
    *   Automatyczne zestawienie składników niezbędnych do przygotowania posiłków z wybranego tygodnia.
    *   Sumowanie wag tych samych produktów ze wszystkich przepisów w jednym miejscu.
    *   Przejrzyste formatowanie jednostek (zamiana gramów na kilogramy przy większych ilościach).
    *   Interaktywne odznaczanie kupionych produktów z możliwością skopiowania całej listy do schowka telefonu jako czysty tekst.

---

## Prywatność i bezpieczeństwo

*   **Działanie offline**: Aplikacja nie wymaga połączenia z internetem i nie wysyła żadnych danych użytkownika na zewnętrzne serwery.
*   **Lokalna baza danych**: Wszystkie dane są przechowywane w bezpiecznej piaskownicy systemu Android i nie są udostępniane innym aplikacjom.
*   **Czysty kod**: Projekt nie zawiera reklam, trackerów ani zewnętrznych bibliotek telemetrycznych (np. Firebase Analytics, Facebook SDK).

---

## Źródło danych

Zbiory danych dotyczące wartości odżywczych potraw oraz ich składników pochodzą z rządowego portalu otwartych danych:
[Dane.gov.pl - Skład i wartość odżywcza żywności](https://dane.gov.pl/pl/dataset/2961)

Baza zawiera ponad 6000 szczegółowych rekordów potraw wraz z ich pełnym profilem odżywczym i składem surowcowym.

---

## Wymagania i kompilacja

*   **Wymagany system**: Android 8.0 (API 26) lub nowszy.
*   **Technologie**: Kotlin, Jetpack Compose, SQLite, Material Design 3.
*   **Budowanie projektu**:
    Projekt można skompilować przy użyciu Gradle w środowisku Android Studio:
    ```bash
    ./gradlew assembleDebug
    ```

---

## Licencja

Projekt jest dostępny na otwartej licencji MIT. Możesz go dowolnie modyfikować i rozwijać według własnych potrzeb.

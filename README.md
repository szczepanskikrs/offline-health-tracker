# HealthTracker 🍏💪

HealthTracker to nowoczesna, otwarta (Open Source) aplikacja na system Android, stworzona z myślą o pełnej prywatności i niezależności użytkownika. Działa w **100% offline** — wszystkie Twoje dane (pomiary ciała, dzienniki treningowe, spacery, trasy GPS oraz jadłospisy) są przechowywane wyłącznie na Twoim urządzeniu w lokalnej bazie danych SQLite. Brak chmury, brak serwerów zewnętrznych, brak śledzenia.

Aplikacja jest dostępna **wyłącznie w języku polskim**.

---

## 🌟 Kluczowe Funkcje

*   📈 **Pomiary Ciała**: Śledzenie wagi, poziomu tkanki tłuszczowej, mięśniowej i innych parametrów w czasie.
*   🏋️ **Treningi i Aktywność**: Rejestrowanie ćwiczeń (pompki, przysiady, mostki itp.) z automatycznym przeliczaniem spalonych kalorii.
*   🚶 **Spacery i GPS**: Moduł spacerów rejestrujący czas, kroki oraz rysujący trasę na mapie za pomocą lokalnego odbiornika GPS.
*   📅 **Jadłospis tygodniowy (Meal Planner)**:
    *   Generowanie zrównoważonego jadłospisu na wybrany tydzień (3, 4 lub 5 posiłków dziennie) dostosowanego do Twojego celu kalorycznego.
    *   Wyszukiwanie i klasyfikowanie posiłków (Śniadanie, Drugie śniadanie, Obiad, Kolacja) z dbałością o brak powtórzeń w ciągu 2 dni.
    *   Losowanie alternatywnych posiłków z tej samej kategorii w zbliżonym przedziale kalorycznym ($\pm 150$ kcal).
    *   Podgląd szczegółowych składników potraw (w gramach) bezpośrednio na rozwijanych kartach dań.
*   🛒 **Lista Zakupów**:
    *   Generowana automatycznie na wybrany tydzień.
    *   Agreguje i sumuje wagi takich samych składników ze wszystkich zaplanowanych posiłków.
    *   Inteligentnie przelicza jednostki (np. `1.25 kg` zamiast `1250 g`).
    *   Interaktywne pole wyboru (Checkbox) umożliwiające wygodne odznaczanie produktów podczas zakupów.
    *   Przycisk **"Kopiuj listę"** kopiujący sformatowaną listę zakupów do schowka systemu jako czytelny tekst.

---

## 🔒 Prywatność i Bezpieczeństwo

*   **100% Offline**: Aplikacja nie wymaga i nie nawiązuje połączeń internetowych w celu przesyłania danych użytkownika.
*   **100% Prywatności**: Baza danych SQLite znajduje się w bezpiecznym katalogu piaskownicy systemu Android. Twoje dane należą wyłącznie do Ciebie.
*   **Brak reklam i trackerów**: Czysty kod, brak zewnętrznych pakietów telemetrycznych (np. Firebase Analytics, Facebook SDK).

---

## 📊 Źródło Danych

Zestawy danych o wartościach odżywczych posiłków oraz ich składnikach zostały pobrane z oficjalnego rządowego portalu otwartych danych:
🔗 [Dane.gov.pl - Skład i wartość odżywcza żywności](https://dane.gov.pl/pl/dataset/2961)

Dane te obejmują ponad 6000 szczegółowych rekordów potraw wraz z pełnym profilem makro/mikroelementów oraz powiązanymi wagami składników.

---

## ⚙️ Wymagania i Kompilacja

*   **System operacyjny**: Android 8.0 (API 26) lub nowszy.
*   **Technologia**: Kotlin, Jetpack Compose (UI), SQLite (Baza danych), Material Design 3.
*   **Budowanie projektu**:
    Do skompilowania projektu zaleca się użycie systemu Gradle w Android Studio.
    ```bash
    ./gradlew assembleDebug
    ```

---

## 📝 Licencja

Projekt jest udostępniany jako oprogramowanie **Open Source** na licencji MIT. Możesz go dowolnie modyfikować i dostosowywać do własnych potrzeb.

# Contributing to Leelo

Thanks for your interest in Leelo. This document explains how to set up the
project, run it, run the tests and find your way around the code.

## Requirements

- **Java 25** or higher
- **Apache Maven 3.6+**

## Installation and usage

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd leelo
   ```

2. **Compile the project**: this creates the runnable jars in `target/`

   ```bash
   mvn clean package
   java -jar target/leelo-0.1.2-shaded.jar
   ```

   To build the Windows installer (needs the
   [WiX Toolset](https://github.com/wixtoolset/wix3/releases) installed):

   ```bash
   mvn clean package -Pinstaller
   ```

3. **Run in development mode**

   ```bash
   mvn javafx:run
   ```

4. **Run the tests**

   ```bash
   mvn test
   ```

## How the code is organised

```
com.leelo
├── model      The domain: languages, books, pages, terms, tags, the palette.
├── parse      The analysers that turn a text into tokens.
├── render     The engine that decides what is displayed on a page.
├── dao        The persistence, on top of SQLite.
├── service    The use cases: reading, vocabulary, statistics, settings.
├── controller The screens.
├── view       The components built by hand.
├── viewmodel  The state of the screens.
└── util       The helpers: characters, tokens, timestamps.
```

The heart of the application is the `render` package. A page is parsed into
tokens, the tokens are matched against the terms of the language, and the
longest matching term wins. `TextRenderer` implements that rule and
`MultiwordTermIndexer` makes the lookup of the multi-word terms a single pass.

Terms are stored as their tokens joined by a zero-width space. This keeps the
word boundaries of the original text and lets a multi-word term be matched
against a text token by token.

## Before opening a pull request

- Run `mvn test` and make sure the whole suite passes.
- Keep the behaviour that Leelo shares with Lute compatible: page splitting,
  term statuses and colours, multi-word term matching and the CSV term format.
- Add a test for every new behaviour or bug fix.
- Keep the changes focused; unrelated refactors make the review harder.

## For end users

If you only want to use Leelo, you do not need Maven:

- **Download the installer** `Leelo-0.1.2.exe` for Windows.
- The database is created in `%LOCALAPPDATA%\Leelo\leelo.db`. The texts and the
  words of the earlier versions of Leelo are copied into it the first time the
  new version starts.

## License

Leelo is distributed under the [MIT license](https://opensource.org/licenses/MIT).
See the [README](README.md) for the origin of the project and the
acknowledgements to Lute.

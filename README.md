# Adventure Book App

A choose-your-own-adventure game: a Spring Boot (WebFlux) backend with an Angular frontend.

## Just run it

The jar on the [Releases page](https://github.com/carrilhas/AdventureBookApp/releases) contains the whole app, frontend included, so only **Java 21** or newer is needed.

1. Download the `.jar` from the latest release.
2. Put it in a folder next to a `books/` folder (copy the one from this repository).
3. From that folder, run:

   ```sh
   java -jar Adventure_Book_App-0.0.1-SNAPSHOT.jar
   ```

4. Open http://localhost:8080.

Player progress is saved in a `players/` folder, created next to the jar on first use.

The sections below are for building and developing the app from source.
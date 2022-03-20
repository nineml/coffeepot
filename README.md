# CoffeePot, an Invisible XML processor

CoffeePot is a command-line [Invisible XML](https://invisiblexml.org/) processor
built on top of the [CoffeeGrinder](https://github.com/nineml/coffeegrinder) and
[CoffeeFilter](https://github.com/nineml/coffeefilter) projects.

For more detailed documentation, see
[https://coffeepot.nineml.org/](https://coffeepot.nineml.org/).

## Installation

The easiest way to try it out is by downloading a release.

1. Download the [latest release](https://github.com/nineml/coffeepot/releases/latest).
2. Unzip it somewhere on your system
3. Run the jar directly with `java -jar coffeepot-x.y.z.jar [options]`

Running it with the option `--help` (or no options at all) will print a summary of the command line options.

## Building it yourself

If you want to build it yourself, you’ll need [Gradle](https://gradle.org).

1. Optionally, fork the repository.
2. Check out the repository.
3. Run `./gradlew jar`

That will produce the jar file in `build/libs`.

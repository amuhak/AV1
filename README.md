# How to run

## Install dependencies

You will need to have installed:

- Java 22 (Preferably GraalVM)
- Maven 3.8.7 or higher
- FFmpeg 7.0 or higher (Full version)

## Run the project

The following commands should run on both Windows and Linux systems.

```shell
git clone https://github.com/amuhak/AV1.git
cd AV1
mvn clean install
mvn spring-boot:run
```

You can replace `mvn` with `mvnw` if you don't have Maven installed on your system.

You can also compile the project to a native image using GraalVM:

```shell
mvn native:compile -Pnative
./target/AV1
```

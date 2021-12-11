# Simple network storage based [Netty](https://netty.io/) - "Netty-network-storage"

### Hello :)  
This is my study pet-project of network file storage. 
Course [gb.ru](https://gb.ru/) "Developing Network Storage in Java"  
Teacher: [Lezenford](https://github.com/Lezenford)

### A little about the stack of technologies used:
* Client-Server communication - [Netty.io](https://netty.io/)
* UI - [JavaFX](https://openjfx.io/)
* Easy dialogs - [FXAlert](https://github.com/dustinkredmond/FXAlert)
* DB - [Postgresql](https://www.postgresql.org/)
* JDBC connection pool - [HikariCP](https://github.com/brettwooldridge/HikariCP)
* Version control DB - [Flyway](https://flywaydb.org/)
* [Docker](https://www.docker.com/)
* Containerization tools - [Jib](https://github.com/GoogleContainerTools/jib)
* Building - [spring-boot-maven-plugin](https://docs.spring.io/spring-boot/docs/2.5.6/maven-plugin/reference/htmlsingle/)
* [Lombok](https://projectlombok.org/)

### How run this project?

######1. First way - Docker (backend), IDEA (frontend):
* be sure you have docker installed.
* grab `docker-compose.yml` from root directory.
* volumes: 
  * in `server` part you can mapping your path instead of `./storage`
`volumes: - ./storage:/storage/root`  
* you can also change port forwarding in `server` - `8080` by default.
* run `docker compose up -d`
* that's all, you have started db and server containers.
* check them status through `docker stats`  
  IntelliJ IDEA (frontend):
* be sure u have JDK11 and maven 3.8.3
* clone project from github
* run terminal in root project directory
* `mvn clean install`
* `cd client`
* `mvn javafx:run`

######2. Second way - IntelliJ IDEA:
* required versions:
  * JDK11
  * maven 3.8.3
  * postgres 14.1
* clone project from github


* create db named "network_storage"
* change host and port in:
  * `./server/src/main/resources/hikari.properties`
  * `./server/pom.xml`
    * see `<artifactId>flyway-maven-plugin</artifactId>`
* run terminal in root project directory
* `cd server`
* `mvn flyway:migrate`
* `cd ..`
* `mvn clean install`

* Run server:
  * `./server/src/main/java/ru/gb/storage/App.java`
* Run UI desktop client:
  * `cd client`
  * `mvn javafx:run`


### Screenshots:
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
<img width="798" alt="Screenshot 2021-12-12 at 01 49 02" src="https://user-images.githubusercontent.com/10568936/145694136-a69c4710-78fb-4f22-a381-f69bbe91fbdf.png">
<img width="794" alt="Screenshot 2021-12-12 at 01 50 41" src="https://user-images.githubusercontent.com/10568936/145694145-45db921f-cfef-4ae1-97b0-fdc5d3a151b1.png">
<img width="797" alt="Screenshot 2021-12-12 at 01 52 15" src="https://user-images.githubusercontent.com/10568936/145694150-f48469be-39f0-43bb-bf09-9323b6a02de6.png">
<img width="795" alt="Screenshot 2021-12-12 at 01 51 40" src="https://user-images.githubusercontent.com/10568936/145694153-6b952d89-7ef7-4f6d-b375-78bcfbdb4d08.png">
<img width="797" alt="Screenshot 2021-12-12 at 01 52 51" src="https://user-images.githubusercontent.com/10568936/145694158-6b8a782b-cc50-496e-9afa-e4ce6cf2dd46.png">
<img width="798" alt="Screenshot 2021-12-12 at 01 53 08" src="https://user-images.githubusercontent.com/10568936/145694161-add3e1cd-cc83-4582-be8f-29a888bf18cd.png">


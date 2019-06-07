# reactor-binlog-stream-to-materialized-view

## 애플리케이션 소개

1. MySQL에 쌓이는 계좌 입출금 이력 데이터를,
2. MySQL Binary Log connector를 통해 스트림으로 읽어 들이고,
3. 읽어 들인 데이터를 계산하여 계좌별 잔고 데이터(materialized view)로 만든 뒤,
4. 로컬 캐시(GuavaCache)에 저장한다.
5. 일련의 데이터의 흐름은 반응형으로 처리하며,
6. 계좌별 잔고 데이터는 HTTP API를 통해 사용자에게 제공할 수 있어야 한다.

## 사용 기술

- [Kotlin](https://kotlinlang.org)
- [MySQL](https://www.mysql.com)
- [MySQL Bianary Log connector](https://github.com/shyiko/mysql-binlog-connector-java)
- [Reactor 3](https://projectreactor.io/docs/core/release/reference/)
- [Guava Cache](https://github.com/google/guava/wiki/CachesExplained)
- [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
- [JPA](https://spring.io/projects/spring-data-jpa)

## MySQL 준비

- MySQL은 Docker를 이용함.
- 그 중에서도 [Docker Official Images](https://hub.docker.com/_/mysql)를 사용.
- 컨테이너 실행은 `docker run --name mysql -e MYSQL_ROOT_PASSWORD=test -p 3306:3306 -d mysql:latest`
- 그리고 mysql_native_password 설정. `alter user 'root'@'localhost' identified with mysql_native_password by 'test'`
- 그 외 권한 등의 준비 작업은 [mysql-binlog-connector-java README](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/README.md) 참고.

## 기술적으로 확인해 보고 싶은 것

- [x] binary log client의 이벤트를 flux로 수신
- [x] MySQL write row log를 적절한 Java 객체(WriteRowEvent)로 읽어들이기
- [x] flux 파이프라인을 통해 WriteRowEvent를 전달하기
- [x] flux 파이프라인을 통해 수신한 WriteRowEvent를 기반으로 계좌의 잔고 계산하기
- [x] 생략하고 넘어갈 수 없는 에러가 발생한 경우에는 애플리케이션을 중단하기
- ~~[ ] 생략할 수 있는 에러가 발생한 경우에는 폴백 로직을 태우고 다음 처리를 계속하기~~
- [x] 파이프라인의 중간에 무거운 연산을 넣고 이를 비동기로 처리하되 이벤트의 순서는 보장하기
- [x] 하나의 스트림을 여러 스트림으로 분할하여 병렬로 처리하기 by ([TopicProcessor](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/TopicProcessor.html))
- [x] flux의 가용한 스레드 갯수를 제어하여 애플리케이션 전체 장애가 나지 않게 하기
- [x] flux overflow 에러가 나지 않도록 처리량을 적절히 제어하기

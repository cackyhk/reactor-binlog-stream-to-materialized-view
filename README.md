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


# MSA 설계 보고서

## 분리 도메인 단위
- Member (회원) - 가입 및 정보 조회
- Concert (콘서트) - 콘서트 CRUD, 좌석 정보, 매진 랭킹 (Redis)
- Reserve (예약) - 좌석 CRUD, 임시 예약 (Redis),
- Queue (대기열) - Redis만 사용
- Payment (결제) - 포인트 충전, 결제
- API gateway (인증/인가, 라우팅, 대기열 검증 등) - 스프링 시큐리티

## 비동기 통신 (kafka)를 사용
- 이벤트 발행 부분을 kafka 사용으로 변경, 동기적으로 조회되는 외부 서비스는 REST로
- `DomainEventPublisher` 포트의 구현체를 kafka를 사용하는 구현체로 변경 가능

## 트랜잭션 처리의 한계
- 각 모듈마다 각자의 DB만 의존한다고 가정
- 여러 서비스에서 분산 트랜잭션 발생 가능 
### 예약 생성 시
```kotlin
ReservationService.make() {
    concert = concertRepository.findById(concertId)    // Concert DB
    member = memberRepository.findById(memberId)       // Member DB
    reservation = reservationRepository.save(...)      // Reservation DB
    eventPublisher.publish(ReservationCreatedEvent)     // Outbox DB
}
```
- 문제 \
현재 4개 테이블 접근이 하나의 트랜잭션에서 원자적으로 실행중, 이것을 MSA로 분리하면 REST 통신이 필요
```kotlin
ReservationService.make() {
    concert = concertServiceClient.findById(concertId)  // REST 호출 → 네트워크 장애 가능
    member = memberServiceClient.findById(memberId)     // REST 호출 → 네트워크 장애 가능
    reservation = reservationRepository.save(...)       // 로컬 DB
    eventPublisher.publish(ReservationCreatedEvent)      // Kafka
}
```
concert, member는 외부의 서비스이므로 REST 통신이 발생하는데, 이 부분에서 네트워크 장애가 발생하면 예약 자체가 불가함
- 해결방안 \
CQRS를 도입하여 read 전용 로컬 테이블을 두어 네트워크 통신 자체를 막는다 -> 실시간성이 필요하다면 REST가 나을것 같기도?? + 메모리 용량이 부족해질수도??

### 결제 시
```kotlin
ReservationService.payReservation() {
    member.usePoint(price)                              // Member 포인트 차감
    memberRepository.saveAndFlush(member)               // Optimistic Lock 체크
    reservation.status = RESERVE                        // Reservation 상태 변경
    reservationRepository.save(reservation)             // 저장
}
```
- 문제 \
하나의 트랜잭션에서 결제 시 회원의 포인트를 차감하고, DB에서 중복이 발생하지 않았는지 체크한 다음 예약을 확정시켜 상태를 변경시킴 \
따라서 하나라도 실패하면 모두 롤백됨\
이를 MSA로 도입해서 분리하면 다음과 같다
```kotlin
PaymentService.pay() {
    paymentService.deductPoint(memberId, price)         // Payment DB (로컬)
    reservationClient.confirmReservation(reservationId) // REST 호출 → 장애 가능!
}
```
`confirmReservation`에서 REST 호출이 발생하는데, 이를 하나의 트랜잭션으로 묶을수가 없음\
따라서 포인트는 차감됐지만 예약 확정이 안되는 상황 발생 가능\
- 해결방안
REST 호출 대신 비동기 event로 `reservationClient`에 이벤트를 보내고 실패한다면 보상 트랜잭션을 발동시켜 실패 이벤트를 `paymentService`에서 처리하도록 설정

### 좌석 조회
```kotlin
SeatFinderImpl.getAvailableSeats() {
    totalSeats = concertRepository.findById(id).totalSeats  // Concert DB
    reserved = reservationRepository.getReservedSeatNumbers() // Reservation DB
    tempReserved = tempReservationPort.getTempReservation()    // Redis
    return (1..totalSeats) - reserved - tempReserved
}
```
- 문제 \
현재 하나의 트랜잭션에서 콘서트, 예약, redis를 모두 조회하고 있음. 이를 MSA로 설계하면 REST 통신이 발생하고, 네트워크 장애가 생기면 실패 가능성\
- 해결방안
예약 가능 좌석을 조회하는 책임은 `ReservationService`에 둔다고 가정, `ConcertService`를 호출하지 않도록 수정한다.\
`Concert`도메인의 `totalSeats` 에서 업데이트가 발생했을 때 Concert -> Reservation 으로 이벤트 발행.\
Reservation 도메인에서는 로컬 캐시에 특정 콘서트에 대입되는 totalSeats 테이블을 가지고 있다가 이벤트가 감지되면 테이블을 변경.\
- 남은 문제
하지만 트래픽이 갑자기 엄청나게 몰릴 때는 어떻게 할 것인가? 이벤트가 발행되고 소비되는 그 수ms의 오버헤드 순간에 트래픽이 몰려 조회가 잘못된다면?\
이미 분산 락을 사용중이라 같은 좌석에 대한 예약은 중복되지 않음 !!

# Event Loop Blocking example

## 목적
- Event Loop Thread Blocking 발생 시, 해당 Thread만의 지연이 아닌 전체 서비스의 성능 저하를 발생시킴
- Demo를 통해 1개의 Worker를 강제로 Block 시킨 후, 전체 성능이 어떻게 저하되는지 확인
- Blockhound를 통한 Blocking Call을 탐지
- Block 된 Thread를 별도의 Scheduler에 위임하여, 전체 성능이 향상되는지 확인

### 환경
```
reactor.netty.ioWorkerCount=4
```

### 서비스 구성
> /sleep<br>
> - 10초의 시간동안 Thead Sleep (Blocking Call)

> /ok
> - 즉시 200 OK를 응답으로 내림 (지연 X)

### 테스트 구성
1. ```/sleep``` 을 호출하여 10초간 blocking되는 요청 발생
2. ```/ok```를 10번 호출하여, blocking 되지 않는 쓰레드에서 정상 시간 내 응답이 오는지 확인

<hr>

### 테스트 결과

```sh
Tests                 Duration
--------------------------------
EventLoopDemoTest     44.506s
testOk()[1]           0.573s
testOk()[2]           0.573s
testOk()[3]           0.574s
testOk()[4]           10.550s
testOk()[5]           0.574s
testOk()[6]           10.549s
testOk()[7]           10.549s
testOk()[8]           0.005s
testOk()[9]           0.005s
testOk()[10]          0.005s
testSleep()           10.549s

```

## Blockhound
- [Blockhound](https://github.com/reactor/BlockHound) 를 설치하여, Blocking Call을 찾을 수 있다.
- Blockhound는 Java Agent로, 미리 정의된 blocking 메소드들이 호출될 때 오류를 발생시킨다.
- 해당 메소드들의 byte code를 바꿔치는 식으로 사용된다.
- 아래의 설정으로 의존성을 추가한다.<br>

### Gradle
~~~
dependencies {
    testImplementation 'io.projectreactor.tools:blockhound:1.0.6.RELEASE'
}
~~~

- 테스트 코드에 아래의 코드블록을 삽입 후 실행한다.
~~~JAVA
static {
    Blockhound.install();
}
~~~

### 결과
```
reactor.blockhound.BlockingOperationError: Blocking call! java.lang.Thread.sleep
at java.base/java.lang.Thread.sleep(Thread.java) ~[na:na]
Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
``` 
- 위의 오류가 발생하며, Blocking Call을 찾을 수 있다.

## 해결
```JAVA
return Mono.fromSupplier(() -> blockingFunction(sleepMs));
```
- 이 메소드는 Blocking Call이지만, 별도의 Scheduler를 지정해주지 않아 Event Loop 내에서 호출된다.
- 아래와 같이 별도의 데몬 스케쥴러를 지정하여, Event Loop 밖에서 처리해줄 수 있도록 변경한다.
```JAVA
return Mono.fromSupplier(() -> blockingFunction(sleepMs))
        .subscribeOn(Schedulers.boundedElastic());
```
### 결과
```sh
Tests                 Duration
--------------------------------
EventLoopDemoTest     14.583s
testOk()[1]           0.569s
testOk()[2]           0.569s
testOk()[4]           0.569s
testOk()[3]           0.569s
testOk()[5]           0.569s
testOk()[6]           0.569s
testOk()[7]           0.569s
testOk()[8]           0.006s
testOk()[9]           0.006s
testOk()[10]          0.006s
testSleep()           10.582s
```
- 위와 같이, 모든 요청이 Blocking 되지 않고 정상적인 시간 내에 도달함을 알 수 있다.

## Reference
- [JDrive Blog](https://blog.jdriven.com/2020/10/spring-webflux-reactor-meltdown-slow-responses/)

- [BlockHound](https://github.com/reactor/BlockHound)
# Spring Cloud Config  
Spring Cloud Config는 MSA 시스템의 환경 설정들을 중앙화해서 한 곳에서 관리를 할 수 있게 해주고 설정파일의 변경되어도 어플리케이션의 재배포 없이 적용이 가능하다.  
진행할 프로젝트 구성은 아래와 같이 진행할 예정이다.  
1. Spring Cloud Config 서버  
2. Spring Cloud Zuul(API Gateway) 서버  
3. Spring Cloud Eureka 서버  

1~3번까지 완료 후 시간이 된다면 인증서버까지 적용해 볼 예정이다. 오늘은 MSA 프로젝트의 첫번째 Spring Cloud Config 서버를 구성해 보려고 한다. 그리고 Config 서버에서 설정한 프로퍼티들을 Resource 서버에서 확인을 해 볼 예정이다.

## PreSetting  
프로젝트는 Intellij의 이전에 정리한 [노트](https://sisipapa.github.io/blog/2021/08/16/Intellij-Springboot-Multiple-Module(ver.-Gradle)/)를 참고해서 Multi Module 프로젝트를 구성했다.  
<img src="https://sisipapa.github.io/assets/images/posts/msa-config0.PNG" >  
- Config : Spring Cloud Config 서버  
- config-repo : Spring Cloud Config 서버가 바라보는 설정파일  
- Eureka : Spring Cloud Eureka 서버  
- Gateway : Spring Cloud Zuul(API Gateway) 서버  
- Resource : Application 서버
- Resource2 : Application 서버  

## Config 서버  
### build.gradle
```properties
# 공통 Dependency
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}

# Config Dependency
project(':Config') {
    dependencies {
        implementation 'org.springframework.cloud:spring-cloud-config-server'
    }
    
    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```  

### Application.java  
EnableConfigServer 어노테이션 추가
```java
@EnableConfigServer
@SpringBootApplication
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }

}
```  

### application-{env}.yml  
#### local
```yaml
server:
  port: 9000
spring:
  application:
    name: configServer
  cloud:
    config:
      server:
        git:
          uri: https://github.com/sisipapa/Springboot-MSA
          search-paths: config-repo/local
```
#### prod  
```yaml
server:
  port: 9000
spring:
  application:
    name: configServer
  cloud:
    config:
      server:
        git:
          uri: https://github.com/sisipapa/Springboot-MSA
          search-paths: config-repo/prod
```  

### config-repo 
config-repo 모듈에 application 별 active.profiles 별로 설정파일을 만든다. 여기서는 message의 내용만 조금 다르게 설정했다.
#### Resource/local 
```yaml
spring:
  profiles: local
  message: Resource Service Local Server!!!!!
```  

#### Resource2/local  
```yaml
spring:
  profiles: local
  message: Resource2 Service Local Server!!!!!
```

여기까지만 하면 Spring Cloud Config 서버의 설정은 끝이다. 로컬서버 구동시 -Dspring.profiles.active=local로 설정 후 서버를 기동한다.  

### Config 서버 테스트
```json
GET http://localhost:9000/resource/local

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 24 Aug 2021 09:31:28 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
  "name": "resource",
  "profiles": [
    "local"
  ],
  "label": null,
  "version": "5eb3f2ee05568a9878370171ccfc4a88af79a71c",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/sisipapa/Springboot-MSA/file:C:\\Users\\user\\AppData\\Local\\Temp\\config-repo-17387822938375408351\\config-repo\\local\\resource-local.yml",
      "source": {
        "spring.profiles": "local",
        "spring.message": "Resource Service Local Server!!!!!"
      }
    }
  ]
}

Response code: 200; Time: 3933ms; Content length: 401 bytes  


GET http://localhost:9000/resource2/local

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 24 Aug 2021 09:32:15 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
  "name": "resource2",
  "profiles": [
    "local"
  ],
  "label": null,
  "version": "5eb3f2ee05568a9878370171ccfc4a88af79a71c",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/sisipapa/Springboot-MSA/file:C:\\Users\\user\\AppData\\Local\\Temp\\config-repo-17387822938375408351\\config-repo\\local\\resource2-local.yml",
      "source": {
        "spring.profiles": "local",
        "spring.message": "Resource Service Local Server!!!!!"
      }
    }
  ]
}

Response code: 200; Time: 822ms; Content length: 403 bytes
```  

## Resource 서버  
### application-{env}.yml
```yaml
server:
  port: 8080
spring:
  application:
    name: resource
  config:
    import: "optional:configserver:http://localhost:9000"
management:
  endpoints:
    web:
      exposure:
        include: info, refresh
```  

### Controller  
GIT 에 저장된 property 값이 변경 된 경우, client에서 최신 파일을 다시 받아 값을 refresh 하기 위해서 RefreshScope 어노테이션을 추가해준다.  
```java
@RestController
@RefreshScope
public class ResourceController {

    @Value("${spring.message}")
    private String message;

    @GetMapping("/message")
    public String message() {
        return "ResourceController message : " + message;
    }
}
```  

### Resource 서버에서 Config 조회  
```json
GET http://localhost:8080/message

HTTP/1.1 200 
Content-Type: application/json
Content-Length: 63
Date: Tue, 24 Aug 2021 09:42:19 GMT
Keep-Alive: timeout=60
Connection: keep-alive

ResourceController message : Resource Service Local Server!!!!!

Response code: 200; Time: 327ms; Content length: 63 bytes
```  

Resource 서버의 [GET]/message API 호출 결과로 Config property를 정상적으로 읽어 온 것을 확인했다. 이제 config-repo의 설정파일의 내용을 수정하고 동일한 API를 호출하면 변경된 결과가 나오지 않는다. 변경된 설정을 Resource 서버에서도 적용을 하기 위해서는 [POST]/actuator/refresh API를 호출해야 변경된 설정이 적용된다. API 결과로 변경된 property 목록이 리턴된다.     
```json
POST http://localhost:8080/actuator/refresh

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Tue, 24 Aug 2021 09:47:06 GMT
Keep-Alive: timeout=60
Connection: keep-alive

[
  "config.client.version",
  "spring.message"
]

Response code: 200; Time: 1540ms; Content length: 42 bytes
```   

## Property value 암호화/복호화  
Config 서버에서 Resource 서버에 제공하는 설정 정보 중에는 외부에 노출되어서는 안되는 정보들이 있을 수 있다. 그래서 value값을 저장 시 암호화된 값을 저장할 수 있도록 지원한다.  

### keypair 생성
keytool -genkeypair 옵션 설명   

```shell
$ keytool -genkeypair -help
keytool -genkeypair [OPTION]...

Generates a key pair

Options:

 -alias <alias>          alias name of the entry to process
 -keyalg <alg>           key algorithm name
 -keysize <size>         key bit size
 -groupname <name>       Group name. For example, an Elliptic Curve name.
 -sigalg <alg>           signature algorithm name
 -destalias <alias>      destination alias
 -dname <name>           distinguished name
 -startdate <date>       certificate validity start date/time
 -ext <value>            X.509 extension
 -validity <days>        validity number of days
 -keypass <arg>          key password
 -keystore <keystore>    keystore name
 -storepass <arg>        keystore password
 -storetype <type>       keystore type
 -providername <name>    provider name
 -addprovider <name>     add security provider by name (e.g. SunPKCS11)
   [-providerarg <arg>]    configure argument for -addprovider
 -providerclass <class>  add security provider by fully-qualified class name
   [-providerarg <arg>]    configure argument for -providerclass
 -providerpath <list>    provider classpath
 -v                      verbose output
 -protected              password through protected mechanism

Use "keytool -?, -h, or --help" for this help message
```  

keypair 생성  
Window PC의 경우 JDK를 환경변수 Path등록을 하지 않았다면 JDK가 설치된 경로로 이동해서 아래 명령어를 실행한다.
```shell
$ keytool -genkeypair -alias config-server-key -keyalg RSA \
  -dname "CN=Config Server,OU=Spring Cloud,O=sisipapa" \
  -keypass keypass123 -keystore config-server.jks -storepass storepass123
```  
위의 명령을 실행하면 config-server.jks 파일이 생성된다. jks 파일을 Config 모듈의 resources 디렉토리에 복사한다.  

### 암호화 설정추가  
application-{env}.yml 파일에 암호화 관련 설정을 추가한다.  
```yaml
encrypt:
  key-store:
    location: classpath:/config-server.jks
    password: storepass123
    alias: config-server-key
    secret: keypass123
```  

### 데이터 암호화  
암호화 설정을 추가하고 Config 모듈을 재기동 후 Config 서버의 /encrypt API를 통해 데이터를 암호화한다. 아래는 config-repo의 resource-local.yml db.ip, db.port
```json
POST http://localhost:9000/encrypt

HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 388
Date: Tue, 24 Aug 2021 15:06:10 GMT
Keep-Alive: timeout=60
Connection: keep-alive
        
AQBXjueGBbxNGPkv7DNYGs/zbQzJKC7rov1IgLG+Qm4l+OQ+ItN108j8Kw+bCcE57O2nD5zaH/pu8796iqxDRpZfdHJD+TS5Ej0zggdfvdaT+8nPOa3gwy9b5WfBi9PtZXgjeimqVwo9xJ4h4YE7I69dP09w4Y0FYJuciE6t+D5oCSbur1joySz8ausDFvw72YUtmpuEw1jLTwka+T3zQfgMBQsnAmV0QH+p/xnCJJ6UHHS0fu6bu+rqyViHfFbB1NRBzUn/b9sVgx9xbMWN80AeBFZebsspHBA76B+RG7N2Kksk5WtmoebsjDPVvlESCKNY0LnXtrOT2s8+7BqO6nYKF8+s/S8x350UrmC2cWSQ5fRDuGTZ05vNpBHFaGl3MP4=
AQAWG7JW48DuhyEqaFRsja62We9Fw94lGqCJBDRv1nXvolWV17nMarXqVqHQSgdD86l8WasXh6k9SlIH660fJclruOgfM0EBwJK+bXmSrGsfzqQs7UmWnJGAraH2Xe6vUL2dj9uq/nXih8glq3UPjpzoK5c4iYEr3G7xdesQhzupG9Yqu7Dqv53HzRVxQnooj2WiSRD2tiqYw1JCGR79Q4RtNs7t0d3ObpcaVOSGQLsl/8L5Rw9GFZfZbfiuYwYQfZ+ooHxJf3XjiO2qCiKuk8e64hy+PDk0WhF9zlZK0svj1HxBdDRNrYjWZI7TfIXinOmaVu5wVDkaNg14oS7y6Toa9vcTCfi1HK24r94GD0naOhDHCWmL5Caek/0JQk7ihjI=
AQBdqizzBGEwvI9n09gVVbPM9yapxvluq1nOVWSHYlfRrQXHf2ONsYsUqYtIP0TE/ijG8O6raD8L02PflI1NdUe/TCwo59VJX2wdvf041HuNVnJN0poYuwdWSg0BWZ7MiDDmID0m202u249exj91nnOjQ+fzbRp80y39QMtaZ90X74e0rAWxU+8kqePjk7z+Y4Yx+iqRTY88Cxulk05ijnyR4oG/vv6+Q4DQH/h9U5Rdcjh0Ndttc3XJGnDa7DmILGWD1gZ3+dbpXU2GisaZgEk2MRc21J20Ac0NFZWJvmSMwm9hLpaCGB4CceghDYZ/yAZJvUMgA3aZtFWd2LJ0qKekL5SSVA/4oGLi8pBVvaCSmKgqvzHS94opZz+srLHwljw=
AQCGy8uXlX8tFPHJTCEhMVYRiJFkaN3yvucHeXrsNsoxFO3l5ds3oHnnwZlbNv23HLbLCt7CmFsw1V7i8b3yqgtDSfgL8nMgmZUJ0rDRAToUEqIkcohkKS8tHreYbjFTurz632S+m4AouFsq6LaGIZU6ftctwTH6bb8v2SccCejcIuvTWoKOr8QKzJhszZH17pJLG4NJDcKqktv0+ZwA2tdr5nb+IE4Eg8RMO6sPnZUHKibqiwmle5C0hDMSBE0KlL4UvNAQg1yyJfLdk/Lm/P1tNyii6WWgG79IWwitbYDtBfSj4nm3mQyMq6zdcS1wYyCb+LvmMejp0r1LwXwYbYFqpwhFUtAkTKrvAhVnYpbQbtfJvdpyu1ZWq3WDKNseE3Y=
        
Response code: 200; Time: 316ms; Content length: 388 bytes
```  

resource-local.yml 설정파일에 암호화된 db.ip, db.port, db.id, db.password 설정추가  
```yaml
spring:
  profiles: local
  message: Resource Service Local Server!!!!!=>수정1
db:
  ip: '{cipher}AQBXjueGBbxNGPkv7DNYGs/zbQzJKC7rov1IgLG+Qm4l+OQ+ItN108j8Kw+bCcE57O2nD5zaH/pu8796iqxDRpZfdHJD+TS5Ej0zggdfvdaT+8nPOa3gwy9b5WfBi9PtZXgjeimqVwo9xJ4h4YE7I69dP09w4Y0FYJuciE6t+D5oCSbur1joySz8ausDFvw72YUtmpuEw1jLTwka+T3zQfgMBQsnAmV0QH+p/xnCJJ6UHHS0fu6bu+rqyViHfFbB1NRBzUn/b9sVgx9xbMWN80AeBFZebsspHBA76B+RG7N2Kksk5WtmoebsjDPVvlESCKNY0LnXtrOT2s8+7BqO6nYKF8+s/S8x350UrmC2cWSQ5fRDuGTZ05vNpBHFaGl3MP4='
  port: '{cipher}AQAWG7JW48DuhyEqaFRsja62We9Fw94lGqCJBDRv1nXvolWV17nMarXqVqHQSgdD86l8WasXh6k9SlIH660fJclruOgfM0EBwJK+bXmSrGsfzqQs7UmWnJGAraH2Xe6vUL2dj9uq/nXih8glq3UPjpzoK5c4iYEr3G7xdesQhzupG9Yqu7Dqv53HzRVxQnooj2WiSRD2tiqYw1JCGR79Q4RtNs7t0d3ObpcaVOSGQLsl/8L5Rw9GFZfZbfiuYwYQfZ+ooHxJf3XjiO2qCiKuk8e64hy+PDk0WhF9zlZK0svj1HxBdDRNrYjWZI7TfIXinOmaVu5wVDkaNg14oS7y6Toa9vcTCfi1HK24r94GD0naOhDHCWmL5Caek/0JQk7ihjI='
  id: '{cipher}AQBdqizzBGEwvI9n09gVVbPM9yapxvluq1nOVWSHYlfRrQXHf2ONsYsUqYtIP0TE/ijG8O6raD8L02PflI1NdUe/TCwo59VJX2wdvf041HuNVnJN0poYuwdWSg0BWZ7MiDDmID0m202u249exj91nnOjQ+fzbRp80y39QMtaZ90X74e0rAWxU+8kqePjk7z+Y4Yx+iqRTY88Cxulk05ijnyR4oG/vv6+Q4DQH/h9U5Rdcjh0Ndttc3XJGnDa7DmILGWD1gZ3+dbpXU2GisaZgEk2MRc21J20Ac0NFZWJvmSMwm9hLpaCGB4CceghDYZ/yAZJvUMgA3aZtFWd2LJ0qKekL5SSVA/4oGLi8pBVvaCSmKgqvzHS94opZz+srLHwljw='
  password: '{cipher}AQCGy8uXlX8tFPHJTCEhMVYRiJFkaN3yvucHeXrsNsoxFO3l5ds3oHnnwZlbNv23HLbLCt7CmFsw1V7i8b3yqgtDSfgL8nMgmZUJ0rDRAToUEqIkcohkKS8tHreYbjFTurz632S+m4AouFsq6LaGIZU6ftctwTH6bb8v2SccCejcIuvTWoKOr8QKzJhszZH17pJLG4NJDcKqktv0+ZwA2tdr5nb+IE4Eg8RMO6sPnZUHKibqiwmle5C0hDMSBE0KlL4UvNAQg1yyJfLdk/Lm/P1tNyii6WWgG79IWwitbYDtBfSj4nm3mQyMq6zdcS1wYyCb+LvmMejp0r1LwXwYbYFqpwhFUtAkTKrvAhVnYpbQbtfJvdpyu1ZWq3WDKNseE3Y='
```  

### 복호화 테스트
config-repo 모듈에서 resource 서버의 local 서버가 바라보는 설정파일에 db접속정보를 조회하는 API 호출테스트
```java
@RestController
@RefreshScope
public class ResourceController {

    @Value("${spring.message}")
    private String message;

    @Value("${db.ip}")
    private String ip;
    
    @Value("${db.port}")
    private String port;
    
    @Value("${db.id}")
    private String id;
    
    @Value("${db.password}")
    private String password;

    @GetMapping("/message")
    public String message() {
        return "ResourceController message : " + message;
    }

    @GetMapping("/db")
    public String db() {return "ResourceController db : " + ip + ":" + port + " [" + id +" : " + password + "]";}
}
```  

```json
GET http://localhost:8080/db

HTTP/1.1 200 
Content-Type: application/json
Content-Length: 56
Date: Tue, 24 Aug 2021 15:28:00 GMT
Keep-Alive: timeout=60
Connection: keep-alive

ResourceController db : 10.10.10.10: 2222 [dbid: dbpass]

Response code: 200; Time: 235ms; Content length: 56 bytes
```  

Config 서버에서 암호화 되있던 property db.ip,db.port,db.id,db.password 정보가 resource 서버에서 복호화 된 데이터로 출력되는 것을 확인할 수 있다.  

## 참고
[DaddyProgrammer Spring CLoud MSA](https://daddyprogrammer.org/post/4347/spring-cloud-msa-configuration-server/)  
[Jorten [Spring] Cloud Config 구축하기](https://goateedev.tistory.com/167)  
[Spring Cloud Config 에서 변경된 정보를 마이크로서비스 인스턴스에서 Spring Boot Actuator 를 이용하여 반영하기](https://wonit.tistory.com/505)  
  

# Spring Cloud Gateway  
Spring Cloud Gateway는 API로 라우팅할 수 있는 간단하면서도 효과적인 방법을 제공하고 보안, 모니터링/메트릭스, 복원력 등과 같은 다양한 우려 사항을 제공하는 것을 목표로 한다. 서비스 엔드포인트를 하나로 통일해서 요청의 특성별로 알맞는 서비스로 라우팅 할 수 있는 기능을 제공한다. 라우팅 설정은 무중단 적용이 가능하다. 클라이언트는 엔드포인트가 한군데로 통일되어 관리포인트가 줄어드는 장점이 있다.  

## API Gateway의 역할
프록시의 역할과 로드밸런싱 - URI에 따라 서비스 엔드포인트를 다르게 가져가는 동적 라우팅이 가능하다.  
인증 서버로서의 기능 - 모든 요청/응답을 관리할 수가 있어 앞단에 인증 및 보안을 적용하기가 용이하다.  
로깅 서버로서의 기능 - 모든 요청/응답을 관제할 수 있는 모니터링 시스템 구성이 단순해진다.  

## Config서버에서 관리하는 config-repo 모듈에 Gateway Route 설정 추가  

### Gateway Route 설정추가  
```yaml
# gateway-local.yml
zuul:
  routes:
    member:
      stripPrefix: false
      path: /v1/member/**
      url: http://localhost:8080
      serviceId: resource
    pay:
      stripPrefix: false
      path: /v1/pay/**
      url: http://localhost:8081
      serviceId: resource2
    else:
      stripPrefix: false
      path: /v1/**
      url: http://localhost:8081
      serviceId: resource2
      
--- 

# gateway-prod.yml
zuul:
  routes:
    member:
      stripPrefix: false
      path: /v1/member/**
      url: http://localhost:8080
      serviceId: resource
    pay:
      stripPrefix: false
      path: /v1/pay/**
      url: http://localhost:8081
      serviceId: resource2
    else:
      stripPrefix: false
      path: /v1/**
      url: http://localhost:8081
      serviceId: resource2
```   

### Config 서버확인  
```json
GET http://localhost:9000/gateway/local

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 25 Aug 2021 03:39:41 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
  "name": "gateway",
  "profiles": [
    "local"
  ],
  "label": null,
  "version": "46e8eb3c8d007a6aef573aa6c815950d0a56fd3b",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/sisipapa/Springboot-MSA/file:C:\\Users\\user\\AppData\\Local\\Temp\\config-repo-10700435317011410158\\config-repo\\local\\gateway-local.yml",
      "source": {
        "spring.profiles": "local",
        "zuul.routes.member.stripPrefix": false,
        "zuul.routes.member.path": "/v1/member/**",
        "zuul.routes.member.url": "http://localhost:8080",
        "zuul.routes.member.serviceId": "resource",
        "zuul.routes.pay.stripPrefix": false,
        "zuul.routes.pay.path": "/v1/pay/**",
        "zuul.routes.pay.url": "http://localhost:8081",
        "zuul.routes.pay.serviceId": "resource2",
        "zuul.routes.else.stripPrefix": false,
        "zuul.routes.else.path": "/v1/**",
        "zuul.routes.else.url": "http://localhost:8081",
        "zuul.routes.else.serviceId": "resource2"
      }
    }
  ]
}

Response code: 200; Time: 1120ms; Content length: 843 bytes
```  
## Gateway 모듈 구성  
Gateway 서버의 경우 클라이언트들의 엔드포인트로 트래픽이 높기 때문에 단일 서비스로 운영하기 보다는 여러대로 구성하고 LoadBalancer로 묶어 HA를 확보하는 것이 좋다.  
### build.gradle  
Multi Module 프로젝트 구성의 build.gradle의 Gateway 모듈에 해당하는 부분만 정리. spring-cloud-starter-netflix-zuul dependency 추가한다. gradle에서 zuul 라이브러리를 내려받지 못해 2.2.9.RELEASE 버전을 입력해 주었다.   
```properties
project(':Gateway') {
    dependencies {
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-zuul:2.2.9.RELEASE'
        implementation 'org.springframework.cloud:spring-cloud-starter-config'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```  

### Gateway 모듈 application-{env}.yml
local, prod 내용은 동일하다. 
```yaml
# application-local.yml
server:
  port: 9100
spring:
  application:
    name: gateway
  config:
    import: "optional:configserver:http://localhost:9000"
management:
  endpoints:
    web:
      exposure:
        include: refresh

---

# application-local.yml
server:
  port: 9100
spring:
  application:
    name: gateway
  config:
    import: "optional:configserver:http://localhost:9000"
management:
  endpoints:
    web:
      exposure:
        include: refresh
```  

### Application.java  
EnableZuulProxy 어노테이션을 추가한다.  
```java
@EnableZuulProxy
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
```  

### Resource 서버에서 확인을 위한 Gateway Routing 확인을 위한 Controller 추가
#### Resource 서버(8080) 추가된 Controller  
```java
@RequestMapping("/v1")
@RestController
@RefreshScope
public class MemberController {

    @GetMapping("/member/health")
    public String memberHealth() {
        return "MemberController running";
    }
    
    @GetMapping("/member2/health")
    public String member2Health() {
        return "MemberController running";
    }

}
```  
#### Resource2 서버(8081) 추가된 Controller  
PayController
```java
@RequestMapping("/v1/pay")
@RestController
@RefreshScope
public class PayController {
    @GetMapping("/health")
    public String health() {
        return "PayController running";
    }
}
```  
ProductController
```java
@RequestMapping("/v1")
@RestController
@RefreshScope
public class ProductController {
    @GetMapping("/product/health")
    public String productHealth() {
        return "PayController running";
    }
    @GetMapping("/product2/health")
    public String product2Health() {
        return "PayController running";
    }
}
```  

## Gateway Route Test 테스트  
### 테스트 중 Exception 발생
```shell
java.lang.NoSuchMethodError: org.springframework.boot.web.servlet.error.ErrorController.getErrorPath()Ljava/lang/String;
	at org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping.lookupHandler(ZuulHandlerMapping.java:87) ~[spring-cloud-netflix-zuul-2.2.9.RELEASE.jar:2.2.9.RELEASE]
	at org.springframework.web.servlet.handler.AbstractUrlHandlerMapping.getHandlerInternal(AbstractUrlHandlerMapping.java:152) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.handler.AbstractHandlerMapping.getHandler(AbstractHandlerMapping.java:498) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.DispatcherServlet.getHandler(DispatcherServlet.java:1258) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1040) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.52.jar:4.0.FR]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.9.jar:5.3.9]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.52.jar:4.0.FR]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.9.jar:5.3.9]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.9.jar:5.3.9]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.9.jar:5.3.9]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.9.jar:5.3.9]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:96) ~[spring-boot-actuator-2.5.4.jar:2.5.4]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.9.jar:5.3.9]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.9.jar:5.3.9]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.9.jar:5.3.9]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:382) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:893) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1726) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.52.jar:9.0.52]
	at java.base/java.lang.Thread.run(Thread.java:834) ~[na:na]
```  
spring-boot-starter 버전이 업그레이드 되면서 spring-cloud-starter-netflix-zuul의 ZuulHandlerMapping 클래스에서 ErrorController.getErrorPath() NoSuchMethodError 발생했고 오류 두시간 이상 오류 해결이 되지 않아 springboot, spring cloud 라이브러리 버전을 변경하게 되었다.  
변경전
Springboot - 2.5.4  
Spring Cloud - 2020.0.3

변경후
Springboot - 2.3.12.RELEASE  
Spring Cloud - Hoxton.SR5  

라이브러리 변경 후 Resource,Resource2 서버에서 config 서버에 접속하기 위한 설정변경  
```yaml
#AS-IS(Spring Cloud-2020.0.3)
spring:
  config:
    import: "optional:configserver:http://localhost:9000"  
    
---

#TO-BE(Spring Cloud-Hoxton.SR5)
spring:
  cloud:
    config:
      uri: http://localhost:9000
```


라이브러리 버전 변경후 Config 서버 구동시 오류  
Springboot 2.5.3 버전에서는 applicaition.yml 파일내에서 encrypt설정을 정상적으로 로드가 되지만 Springboot 버전을 2.3.12.RELEASE로 변경 후 아래 오류가 발생해서 Config 서버의 application-{env}.yml 파일의 이름을 bootstrap-{env}.yml로 변경
```shell
***************************l
APPLICATION FAILED TO START
***************************

Description:

Field rsaProperties in org.springframework.cloud.config.server.config.EncryptionAutoConfiguration$KeyStoreConfiguration required a bean of type 'org.springframework.cloud.bootstrap.encrypt.RsaProperties' that could not be found.

The injection point has the following annotations:
	- @org.springframework.beans.factory.annotation.Autowired(required=false)


Action:

Consider defining a bean of type 'org.springframework.cloud.bootstrap.encrypt.RsaProperties' in your configuration.
```

[장애와 관련된 링크](https://github.com/spring-cloud/spring-cloud-netflix/issues/4008) 이다.  
관련해서 해결책을 못찾아서 현재는 라이브러리 버전 다운그레이드....    

### 장애해결 후 재테스트!!!
Gateway 서버로 요청을 보내면 Resource, Resource2 서버로 Routing 되는 것을 확인할 수 있다.  

#### Gateway(9000) 서버로 요청 > Resource 서버로 Routing
```json
GET http://localhost:9100/v1/member/health

HTTP/1.1 200 
Date: Wed, 25 Aug 2021 09:03:23 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

MemberController running

Response code: 200; Time: 195ms; Content length: 24 bytes
```  

#### Gateway(9000) 서버로 요청 > Resource2 서버로 Routing  
```json
GET http://localhost:9100/v1/product/health

HTTP/1.1 200 
Date: Wed, 25 Aug 2021 08:56:42 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

PayController running

Response code: 200; Time: 56ms; Content length: 21 bytes
```  
여기까지 Spring Cloud Gateway 설정을 통한 Routing 기능을 확인해 보았다.  

## Spring Cloud Gateway 필터 적용  
- Pre Filter - 라우팅 전 실행되고 logging 및 인증 등 처리  
- Routing Filter - 요청에 대한 라우팅 처리  
- Post Filter - 라우팅 후 실행되고 사용자 정의 헤더 추가/제거 또는 통계 및 matrix 수집   
- Error Filter - 에러 발생시 핸들링   

### Filter class 생성
```java
@Slf4j
public class GatewayPreFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info("Using Pre Filter : "+request.getMethod() + " request to " + request.getRequestURL().toString());
        return null;
    }
}
```  

```java
@Slf4j
public class GatewayRouteFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        log.info("GatewayRouteFilter");
        return null;
    }
}
```  

```java
@Slf4j
public class GatewayPostFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        log.info("GatewayPostFilter");
        return null;
    }
}
```  

```java
@Slf4j
public class GatewayErrorFilter extends ZuulFilter {
    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        log.info("GatewayErrorFilter");
        return null;
    }
}
```  

### Filter Bean 등록
```java
@Configuration
public class ZuulFilterConfig {
    @Bean
    public GatewayPreFilter preFilter() {
        return new GatewayPreFilter();
    }

    @Bean
    public GatewayPostFilter postFilter() {
        return new GatewayPostFilter();
    }

    @Bean
    public GatewayRouteFilter routeFilter() {
        return new GatewayRouteFilter();
    }

    @Bean
    public GatewayErrorFilter errorFilter() {
        return new GatewayErrorFilter();
    }
}
```  

### Filter 테스트
Filter는 확인을 위한 로그만 출력한다.  
```json
GET http://localhost:9100/v1/product/health

HTTP/1.1 200 
Date: Wed, 25 Aug 2021 14:29:17 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

PayController running

Response code: 200; Time: 181ms; Content length: 21 bytes
```   
Filter 적용 후 로그  
```shell
2021-08-25 23:37:17.758  INFO 13004 --- [nio-9100-exec-7] c.s.s.m.gateway.filter.GatewayPreFilter  : Using Pre Filter : GET request to http://localhost:9100/v1/product/health
2021-08-25 23:37:17.758  INFO 13004 --- [nio-9100-exec-7] c.s.s.m.g.filter.GatewayRouteFilter      : GatewayRouteFilter
2021-08-25 23:37:17.775  INFO 13004 --- [nio-9100-exec-7] c.s.s.m.g.filter.GatewayPostFilter       : GatewayPostFilter
```  

## 참고
[DaddyProgrammer Spring CLoud MSA(2)](https://daddyprogrammer.org/post/4401/spring-cloud-msa-gateway-routing-by-netflix-zuul/)  


# Spring CLoud Eureka  

Spring Cloud Eureka는 MSA 시스템에서 서비스의 로드밸런싱과 실패처리 등을 유연하게 하기 위해 각 서비스들의 IP,PORT,InstanceId를 가지고 있는 REST 기반의 미들웨어 서버이다. Eureka는 마이크로 서비스 기반의 아키텍처의 핵심 원칙 중 하나인 Service Discovery의 역할을 수행한다. MSA에서는 Service의 IP와 Port가 일정하지 않고 지속적을 변화한다. Eureka는 Client-Server의 방식으로 Eureka Server는 모든 Client 서버들이 본인의 IP와 Port, InstanceId를 Eureka-Server로 전달한다. 그리고 Eureka에 있는 정보를 Fetch하여 Eureka-Client간 통신에 사용한다.    

## Eureka 서버

### build.gradle
공통으로 사용하는 Dependency는 전체소스를 내려받아서 확인하면 되고 여기서는 Eureka 모듈에서 사용하는 Dependency만을 정리한다.  
```properties
project(':Eureka') {
    dependencies {
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```  

### Application.java  
EnableEurekaServer 어노테이션을 설정한다.  
```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }

}
```  

### bootstrap.yml  
```yaml
server:
  port: 8761
spring:
  application:
    name: eureka
eureka:
  client:
    registerWithEureka: true
    fetchRegistry: false
```  

Eureka의 설정정보를 Config 서버에서 가져오도록 Config서버에 EurekaClient 설정을 하게 되면 Config서버와 Eureka 서버간의 Dependency가 생기게 되기 떄문에 설정을 하지는 않는다.

### Eureka 대시보드 확인   
Eureka 서버의 기본설정은 여기까지이고 -Dspring.profiles.active=local 파라미터를 주고 서버를 구동하고 대시보드에 접속한다.     
Eureka 대시보드 접속 - <http://localhost:8761>   
<img src="https://sisipapa.github.io/assets/images/posts/eureka-board.PNG" >  

## Eureka Discovery에 클라이언트 서비스 등록  
gateway, resource, resource2 모듈에 eureka client 설정을 추가한다.  

### build.gradle  
gateway, resource, reource2 모듈에 spring-cloud-starter-netflix-eureka-client dependency를 추가한다.
```properties
project(':Gateway') {
    dependencies {
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-zuul'
        implementation 'org.springframework.cloud:spring-cloud-starter-config'
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}

project(':Resource') {
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.cloud:spring-cloud-starter-config'
        implementation 'org.springframework.boot:spring-boot-starter-actuator'
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    }
    
    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}

project(':Resource2') {
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.cloud:spring-cloud-starter-config'
        implementation 'org.springframework.boot:spring-boot-starter-actuator'
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```  

### Application.jva 설정
#### resource    
```java
@EnableDiscoveryClient
@SpringBootApplication
public class ResourceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceApplication.class, args);
    }
}
```  

#### resource2  
```java
@EnableDiscoveryClient
@SpringBootApplication
public class Resource2Application {
    public static void main(String[] args) {
        SpringApplication.run(Resource2Application.class, args);
    }
}
```  

#### gateway  
```java
@EnableDiscoveryClient
@EnableZuulProxy
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```  

### Eureka 대시보드 확인  
gateway, resource, resource2 모듈을 재기동하고 대시보드를 확인한다.  
Eureka 대시보드 접속 - <http://localhost:8761>  
<img src="https://sisipapa.github.io/assets/images/posts/eureka-board2.PNG" >  

## Eureka Discovery 활용1  
config-repo 모듈의 gateway 관련 설정 중 zuul관련설정을 변경한다.  
```yaml
# 변경전
zuul:
  routes:
    member:
      stripPrefix: false
      path: /v1/member/**
      url: http://localhost:8080
      serviceId: resource
    pay:
      stripPrefix: false
      path: /v1/pay/**
      url: http://localhost:8081
      serviceId: resource2
    else:
      stripPrefix: false
      path: /v1/**
      url: http://localhost:8081
      serviceId: resource2  

---

#변경후  
zuul:
  routes:
    member:
      stripPrefix: false
      path: /v1/member/**
      serviceId: resource
    pay:
      stripPrefix: false
      path: /v1/pay/**
      serviceId: resource2
    else:
      stripPrefix: false
      path: /v1/**
      serviceId: resource2
```  
재배포 없이 실시간 반영을 위한 gateway 서버의 /actuator/refresh API를 호출한다.  
```json
POST http://localhost:9100/actuator/refresh

HTTP/1.1 200 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Thu, 26 Aug 2021 06:07:53 GMT
Keep-Alive: timeout=60
Connection: keep-alive

[
  "config.client.version",
  "zuul.routes.member.url",
  "zuul.routes.else.url",
  "zuul.routes.pay.url"
]

Response code: 200; Time: 7755ms; Content length: 95 bytes
```  
IP,PORT 접속정보 제거후 serviceId 접속테스트  
```json
GET http://localhost:9100/v1/member/health

HTTP/1.1 200 
Date: Thu, 26 Aug 2021 06:08:43 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

MemberController running

Response code: 200; Time: 1087ms; Content length: 24 bytes
```  

## Eureka Discovery 활용2
Config 서버의 정보도 Eureka에 등록하고 활용하는 테스트를 진행해 보겠다.  
### build.gradle   
Config 모듈에도 spring-cloud-starter-netflix-eureka-client Dependency를 추가한다.  
```properties
project(':Config') {
    dependencies {
        implementation 'org.springframework.cloud:spring-cloud-config-server'
        implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```  

### Applicaiton.java 설정  
```java
@EnableDiscoveryClient
@EnableConfigServer
@SpringBootApplication
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }

}
```  

### 모듈의 config 서버정보 config.uri > config.discovery.service.id로 변경
```yaml
#변경전 
spring:
  application:
    name: resource
  cloud:
    config:
      uri: http://localhost:9000

---

#변경후
spring:
  application:
    name: resource
  cloud:
    config:
      fail-fast: true
      discovery:
        service-id: config
        enabled: true
    # uri: http://localhost:9000
```  

### Eureka 대시보드 확인  
Eureka 대시보드에서 Config 서버 등록 확인.  
<img src="https://sisipapa.github.io/assets/images/posts/eureka-board3.PNG" >  

### config 서버 service명으로 접속 확인
resource, reource2 서버의 config서버의 설정 파일에 설정 값을 읽어오는 API 호출 시 정상 응답 확인.
```json
GET http://localhost:9100/v1/resource2/message

HTTP/1.1 200 
Date: Thu, 26 Aug 2021 07:30:23 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

Resource2Controller message : Resource2 Service Local Server!!!!!

Response code: 200; Time: 470ms; Content length: 65 bytes
```    

## Eureka Discovery 활용3  
Resource2 서버의 요청이 많아서 서버의 증설이 필요한 상황을 가정해서 Service 모듈을 추가하는 과정을 정리해 보려고 한다.

### 실행 가능한 jar파일 생성  
IntelliJ의 Gradle bootJar를 실행해서 실행가능한 jar파일을 생성한다.  
<img src="https://sisipapa.github.io/assets/images/posts/eureka-bootjar.PNG" >  

### jar 파일 실행
```shell
$ java -jar -Dserver.port=8082 -Dspring.profiles.active=local C:\projects\Springboot-MSA\Resource2\build\libs\Resource2-0.01-SNAPSHOT.jar
```  

### Eureka 대시보드 확인  
8082 포트로 실행한 Resource2 서비스 추가확인
<img src="https://sisipapa.github.io/assets/images/posts/eureka-board4.PNG" >  

### 테스트  
2번의 요청을 날렸는데 8081, 8082로 한번씩 로드밸런싱 되어 요청을 한다.  
```json
GET http://localhost:9100/v1/resource2/message

HTTP/1.1 200 
Date: Thu, 26 Aug 2021 09:18:16 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

Resource2Controller message : Resource2 Service Local Server!!!!! | port: 8082

Response code: 200; Time: 317ms; Content length: 79 bytes
        
        
GET http://localhost:9100/v1/resource2/message

HTTP/1.1 200
Date: Thu, 26 Aug 2021 09:19:27 GMT
Keep-Alive: timeout=60
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

Resource2Controller message : Resource2 Service Local Server!!!!! | port: 8081

Response code: 200; Time: 411ms; Content length: 79 bytes
```  

## 참고
[DaddyProgrammer Spring CLoud MSA(3) - Service Discovery by Eureka](https://daddyprogrammer.org/post/4446/spring-cloud-msa-service-discovery-by-eureka/)   

# Springboot MSA - Authorization  

오늘은 특정 API에 대한 접근 권한을 적용해서 인가/인증된 사용자만이 API 사용이 가능하도록 구현해 볼 예정이다. Auth 서버 구성은 얼마전에 정리했던 소스를 그대로 가지고 와서 사용할 예정이다.   
아래는 Auth 서버 구성 시 참고  
Springboot Oauth2 - AuthorizationServer - <https://sisipapa.github.io/blog/2021/07/12/Springboot-Oauth2-AuthorizationServer/>  
Springboot Oauth2 - AuthorizationServer2(JWT, DB인증) - <https://sisipapa.github.io/blog/2021/07/13/Springboot-Oauth2-AuthorizationServer2/>  
Auth 서버 구성 Git - <https://github.com/sisipapa/oauth2-AuthorizationServer.git>  
  
## Auth 서버 구성
이전 정리했던 노트내용 중에 Oauth2 Authorization 서버 작성했던 소스를 그대로 가져와서 Auth 서버에 적용을 했다. 소스를 그대로 복사해서 붙여넣기를 하고 변경된 package 구조만 변경했다. 기존에는 Authorization 서버를 하나의 어플리케이션으로 구성을 했었는데 지금은 MSA 구조의 Multi Module로 변경되면서 buiuld.gradle의 내용만 조금 변경되었다.   
  
### build.gradle  
공통부분을 제외하고 Authorization 모듈에서 필요한 모듈만 정리. 나머지 소스는 [Auth 서버 구성 전체소스](https://github.com/sisipapa/oauth2-AuthorizationServer.git)를 참고하면 된다.  
```properties
project(':Auth') {
    dependencies {
        implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
        implementation 'org.springframework.boot:spring-boot-starter-web'
        runtimeOnly 'com.h2database:h2'

        implementation 'com.google.code.gson:gson'
        implementation 'org.springframework.cloud:spring-cloud-starter-security'
        implementation 'org.springframework.cloud:spring-cloud-starter-oauth2'
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```

## Gateway 모듈
### bootstrap.yml 
zuul.sensitiveHeaders 속성을 추가한다. sensitiveHeaders를 설정하여 내부에서 사용되는 헤더값이 노출되는 것을 막을 수 있다. Cookie, Set-Cookie, Authorization은 zuul의 기본값이며, sensitiveHeaders를 사용하지 않으려면 명시적으로 값을 비워둬야한다.  
```yaml
server:
  port: 9100
spring:
  application:
    name: gateway
  cloud:
    config:
      fail-fast: true
      discovery:
        service-id: config
        enabled: true
    # uri: http://localhost:9000
management:
  endpoints:
    web:
      exposure:
        include: refresh

zuul:
  sensitiveHeaders: Cookie,Set-Cookie
```  

### Config 추가
EnableResourceServer 어노테이션은 API 서버를 설정하기 위한 부분이다. MSA구조에서는 개별 API 서버마다 EnableResourceServer 어노테이션을 설정하는 것이 아니라 gateway 서버에 설정한다.   
Resource 서버의 /v1/member/* URI 패턴은 인증된 사용자만 호출이 가능하도록 설정한다.   
```java
@Configuration
@EnableResourceServer
public class Oauth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.headers().frameOptions().disable();
        http.authorizeRequests()
                .antMatchers("/v1/member/*").authenticated();
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        Resource resource = new ClassPathResource("publicKey.txt");
        String publicKey = null;
        try {
            publicKey = IOUtils.toString(resource.getInputStream());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        converter.setVerifierKey(publicKey);
        return converter;
    }
}
```  

### 공개키 추가  
인증서버는 리소스 서버에게 공개키를 공유하고 함호화해서 데이터를 전달한다. 암호화된 데이터와 공개키가 노출 되더라도 비밀키가 없이 데이터를 복호화 할 수 없기 때문에 보안이 높다.  
```text
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAplzVr93U21Dp9vZuzBJt
cGSdl4Cavf2Em1ISjYxEPvfYihhf70zYjYkq1XJQzWNOFxOWq9i/vric+0f7H8ET
eDJ/k4BOtyYhuWGGpr8N944xK09zMkOsDQUoJTlgj1K4HCS+9mYvC7gheG95JCWG
+pDvr4KmR23Wfe5ieopWs3UnyFU2hKqEhi7Cog/QTVPFVGUgxr8SbCW+L4T13uBD
kRAbkLWpB9/Q7fBm75QLza9zU1/B5N3c5lwOhs4z4hDF9bJ5XreaBUiP9boI9kFL
T0TAwmHMtTAPXunHqbPkdPQ97pN2cqkR1bJhlEVIZK2V0RECQb8ELBpp3bzo3obR
zwIDAQAB
-----END PUBLIC KEY-----
```  

## 테스트  
### AccessToken 생성
인증서버의 /oauth/token API를 통해 access_token을 얻는다.  
```java
@SpringBootTest
class Oauth2ControllerTest {

    Logger log = (Logger) LoggerFactory.getLogger(Oauth2ControllerTest.class);

    @Autowired
    private Gson gson;
    @Autowired
    private RestTemplate restTemplate;

    @Test
    void callbackSocial(){
        String credentials = "testClientId:testSecret";
        String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJzaXNpcGFwYTIzOUBnbWFpbC5jb20iLCJzY29wZSI6WyJyZWFkIl0sImF0aSI6ImU0ODM4MmEzLTk2OWMtNGFlYy05MThkLWQyNzc4ZTEzNzg0NiIsImV4cCI6MTYzMDE4NDkzMSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sImp0aSI6ImIyMjg4ZTg0LTEwNTctNDI5Yy1hNWY3LWQwMGQ4ZDU0ZjhkOSIsImNsaWVudF9pZCI6InRlc3RDbGllbnRJZCJ9.VQAyQ-y-IV6s-a-vHZ_h-wRbU_R03mwkb_XLKaPtQCll4KTOOuuLyHYvsiDcR7RblITbBBj4gnRzUdSNG0M87dAP3NYAarLq_DF-MHVrxalj7o_PbIoXmgrI7geO-51bvoj0bApd9Rgo9dF-yIKTUp7BkCJeJaIpzKg_TXO0KmaVCpltwenMnDUsbbcME9dLoMOquQ28w_Rcm46mdceT-vfq4gpFmy_fBhDVPc4702YyuKbfb0BxGDCZ_SXP4G7ykWqt7IuMdjuBrtPBqKZCrolQiVI3Dcsk3n9UADx6wfwwx-UBRtNtVSKpJMl3LbWAjxSnFqjVZskdFgCrvK68BA");
        params.add("grant_type", "refresh_token");
//        params.add("code", code);
//        params.add("grant_type", "authorization_code");
//        params.add("redirect_uri", "http://localhost:8081/oauth2/callback");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8081/oauth/token", request, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            OAuthToken token = gson.fromJson(response.getBody(), OAuthToken.class);
            System.out.println("###############################################################################################################################");
            log.info("### access_token : " + token.getAccess_token());
            log.info("### refresh_token : " + token.getRefresh_token());
            log.info("### token_type : " + token.getToken_type());
            log.info("### scope : " + token.getScope());
            log.info("### expires_in : " + token.getExpires_in());
            System.out.println("###############################################################################################################################");
        }
    }
}
```   
단위테스트 결과 로그확인(access_token을 복사)    
```text
###############################################################################################################################
2021-08-28 21:07:24.398  INFO 15892 --- [    Test worker] c.s.s.m.a.c.Oauth2ControllerTest         : ### access_token : eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MzAxODg0NDQsInVzZXJfbmFtZSI6InNpc2lwYXBhMjM5QGdtYWlsLmNvbSIsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiJdLCJqdGkiOiJiOWMyNTE3Ni03MzczLTRjN2YtYjQ4NC0zNjM5NjRhNmFhZDQiLCJjbGllbnRfaWQiOiJ0ZXN0Q2xpZW50SWQiLCJzY29wZSI6WyJyZWFkIl19.hCWbWLJGlM4SFzy3AchClbDND3Tb5OT0l5kefMPw8fkXkIQB0Q6H6ZzCdE6_iuGWLGWoUYdm7chC6cwiflgUZ0T7zkNJEsOf4nY6lLh7oXwXBQ4aamUQlwLoUj0sFOriLOA7DLGywRoTglhXaZ02ROiphHDBq4jmzktz-v0zdRPr82ibIWiiR65yTYcLl2KpI8NNyI8HcrD3BhOJ1QqTD3PyqfvjIa1rSLYfQHqiDqbnu8ZvCNam6wXcc1EBvm69RUcEP97NjVjxKNUZzGhE0ulWHjxR_9c_zbJpTHY_tJ-iVd0pCQ6JK9uGK7TZHlMwQz7yZ6oJBkStoZehRMpMQg
2021-08-28 21:07:24.398  INFO 15892 --- [    Test worker] c.s.s.m.a.c.Oauth2ControllerTest         : ### refresh_token : eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJzaXNpcGFwYTIzOUBnbWFpbC5jb20iLCJzY29wZSI6WyJyZWFkIl0sImF0aSI6ImI5YzI1MTc2LTczNzMtNGM3Zi1iNDg0LTM2Mzk2NGE2YWFkNCIsImV4cCI6MTYzMDE4NDkzMSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sImp0aSI6ImIyMjg4ZTg0LTEwNTctNDI5Yy1hNWY3LWQwMGQ4ZDU0ZjhkOSIsImNsaWVudF9pZCI6InRlc3RDbGllbnRJZCJ9.WSTm2-sm6ZZN7AlKXcazL_jFiOLzBCbs8DTTQFEtoW4X5naIgy8pReQ_uYCj5LJdFRgJFPMthOd4q4f3WeOE_2SyWuSF38kutG_gxE95MScfdVHPeuh70pem13p2Ms9AeFxD5GN27qazCSXQXEl-8IZWPq05l_q8TzxTE2pbvW1vjz3Jpmv6K1pfaQHxqjEbxcQ82S5m1-WuximQW52EJhQNEmC7_lDLSh5aJmO51JkSNrkgvE0koEhsyGFm4C5uZYR3-rGsVimYAE6ILzD_MWK37jhtXGnTWf5zLnYygyBvurLn-ANKS--rniEjZMNLg4Vv8ip97kWR3oOuef-0Eg
2021-08-28 21:07:24.398  INFO 15892 --- [    Test worker] c.s.s.m.a.c.Oauth2ControllerTest         : ### token_type : bearer
2021-08-28 21:07:24.399  INFO 15892 --- [    Test worker] c.s.s.m.a.c.Oauth2ControllerTest         : ### scope : read
2021-08-28 21:07:24.402  INFO 15892 --- [    Test worker] c.s.s.m.a.c.Oauth2ControllerTest         : ### expires_in : 35999
###############################################################################################################################
```

### /v1/member/health API 호출
#### Authorization header 설정 안한 경우  
```json
GET http://localhost:9100/v1/member/health

HTTP/1.1 401 
Cache-Control: no-store
Pragma: no-cache
WWW-Authenticate: Bearer realm="oauth2-resource", error="unauthorized", error_description="Full authentication is required to access this resource"
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Content-Type: application/json
Transfer-Encoding: chunked
Date: Sat, 28 Aug 2021 12:10:13 GMT
Keep-Alive: timeout=60
Connection: keep-alive

{
  "error": "unauthorized",
  "error_description": "Full authentication is required to access this resource"
}

Response code: 401; Time: 264ms; Content length: 102 bytes
```    

#### Authorization header 설정 했을 경우  
```json
GET http://localhost:9100/v1/member/health

HTTP/1.1 200 
Date: Sat, 28 Aug 2021 12:09:05 GMT
Keep-Alive: timeout=60
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
Content-Type: application/json
Transfer-Encoding: chunked
Connection: keep-alive

MemberController running

Response code: 200; Time: 209ms; Content length: 24 bytes
```  

테스트를 통해 /v1/member/* URI 패턴은 Authorization Bearer 타입의 access_token이 없을 경우 401 HTTP 응답코드의 에러메세지를 응답한다.  

## 참고  
[Spring MSA (4) - 인증서비스](https://taes-k.github.io/2019/06/20/spring-msa-4/)  


[SISIPAPA 노트연결 - Spring Cloud Config](https://sisipapa.github.io/blog/2021/08/20/Springboot-MSA-%EA%B5%AC%EC%84%B11-Spring-Cloud-Config/)  
[SISIPAPA 노트연결 - Spring Cloud Gateway](https://sisipapa.github.io/blog/2021/08/24/Springboot-MSA-%EA%B5%AC%EC%84%B12-Spring-Cloud-Gateway/)  
[SISIPAPA 노트연결 - Spring Cloud Eureka](https://sisipapa.github.io/blog/2021/08/25/Springboot-MSA-%EA%B5%AC%EC%84%B13-Spring-Cloud-Eureka/)  
[SISIPAPA 노트연결 - Authorization](https://sisipapa.github.io/blog/2021/08/26/Springboot-MSA-%EA%B5%AC%EC%84%B14-Authorization/)  


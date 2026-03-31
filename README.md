# Auto Comment 🤖
GitHub PR 이벤트 기반 GPT 자동 코드 리뷰 & 로그 시스템

---

## 🔥 프로젝트 소개
GitHub Pull Request 이벤트를 감지하여 변경된 diff를 분석하고,
GPT 기반 코드 리뷰를 자동 생성해 PR 댓글과 리뷰 로그로 저장하는 **백엔드 자동화 시스템**입니다.

- 코드 변경 분석
- AI 리뷰 생성
- GitHub 기록 저장

🎥 시연 영상 (Click!)<br><br>
<a href="https://www.youtube.com/watch?v=386D_PckPxE" target="_blank">
<img src="http://img.youtube.com/vi/386D_PckPxE/0.jpg" alt="시연 영상">
</a>

---

## 🚀 주요 기능
- 🔔 GitHub Webhook 기반 PR 이벤트 수신
- 📌 PR 생성 / 수정 / 재오픈 이벤트 감지
- 🔍 변경된 코드(diff) 분석
- 🤖 GPT 기반 코드 리뷰 자동 생성
- 💬 PR 댓글 자동 등록
- 📝 리뷰 로그 파일 자동 저장

---

## 🛠 기술 스택

### **Backend**
  ![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
  ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
### **API**
  ![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=for-the-badge&logo=openai&logoColor=white)
  ![GitHub API](https://img.shields.io/badge/GitHub_REST_API-181717?style=for-the-badge&logo=github&logoColor=white)
### **Integration**
  ![Webhook](https://img.shields.io/badge/GitHub_Webhook-F05032?style=for-the-badge&logo=github&logoColor=white)
### **Networking**
  ![ngrok](https://img.shields.io/badge/ngrok-1F1E37?style=for-the-badge&logo=ngrok&logoColor=white)

---

## 🧩 시스템 구조

![img.png](src/main/resources/static/images/img_4.png)

1) diff 분석<br>![img_1.png](src/main/resources/static/images/img_1.png)<br><br>
2) GPT 요청
3) 리뷰 생성
4) GitHub 댓글 등록<br>![img.png](src/main/resources/static/images/img.png)<br><br>
5) 로그 파일 저장<br>
![img_2.png](src/main/resources/static/images/img_2.png)<br><br>
![img_3.png](src/main/resources/static/images/img_3.png)<br><br>
- 이벤트 타입별 분기 처리 구조
- PR 단위 상태 관리

---

## 👨‍💻 리뷰 저장 구조
- 브랜치: `auto-comment-logs`
- 경로: `reviews/`<br>
  &emsp;&emsp;&emsp;&emsp;&emsp;└── `pr-{번호}/`<br>
  &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;├── `latest.md`<br>
  &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;└── `{날짜}/{시간}.md`<br>


- 최신 리뷰 + 히스토리 동시 관리

---

## ⚡ 트러블슈팅

### 🔍 문제 상황

GitHub Webhook을 통해 Pull Request 이벤트를 수신하고,
해당 PR에 대해 자동으로 리뷰를 생성하는 기능을 구현했다.

초기 구조에서는 webhook 요청을 처리하는 과정에서 다음 작업을 **동기적으로 순차 실행**했다.

* GitHub API를 통한 PR diff 조회
* OpenAI API를 통한 코드 리뷰 생성
* GitHub PR 댓글 등록
* 리뷰 결과 파일 저장 (GitHub Repository)

즉, 하나의 요청 흐름에서 모든 외부 API 호출이 수행되는 구조이다.

---

### ⚠️ 무슨 문제가 생길 수 있을지 고민

* OpenAI API 호출의 응답 시간이 길어질 수도 있어서 전체 응답시간이 길어질 수 있음
* GitHub API가 여러 번 호출되며 네트워크 지연이 발생할 수 있음

GitHub Docs에 따르면 GitHub Webhook은 **10초 이내에 2XX 응답을 반환해야 안정적으로 처리**된다고 한다.
(https://docs.github.com/en/webhooks/using-webhooks/best-practices-for-using-webhooks#respond-within-10-seconds)

따라서, 현재 구조는 webhook delivery 실패 가능성이 있다.

---

### 🧠 원인 분석

예상되는 병목의 원인을 찾아보았다.

현재 `review()` 메서드가 실행되면, 하나의 요청 안에서 여러 외부 API 호출이 순차적으로 수행된다.

- `githubDiffService.getPullRequestDiff(...)`
  - GitHub REST API를 호출하여 PR diff 조회
- `gptReviewService.generateReview(diff)`
  - OpenAI API를 호출하여 리뷰 생성
- `githubCommentService.createComment(...)`
  - GitHub PR 댓글 등록 (POST)
- `githubFileService.saveReviewFile(...)`
  - GitHub Repository에 리뷰 파일 저장 (POST)

`review()` 내부에서

    **diff 조회 → OpenAI 호출 → 댓글 등록 → 파일 저장**

과정이 하나의 요청 스레드에서 순차적으로 실행되고 있다.

이로 인해:
- 외부 API 응답 속도에 따라 전체 처리 시간이 쉽게 늘어날 수 있다는 문제
- 현재 구조상 하나의 API라도 지연되면 다음 작업들은 모두 대기
- webhook 응답 지연 가능성 발생

따라서, **외부 API 성능**에 영향을 받음.

---

### 🛠 해결 방법

문제를 해결하기 위해 `비동기 처리`와`병렬 처리`를 도입하였다.

- 비동기: webhook 요청에 대해 10초 안에 2xx으로 응답하기 위해 사용
- 병렬처리: PR 리뷰 등록 및 저장을 독립적으로 실행하기 위해 사용 (작업 처리 시간 단축)

---

### 🔁 개선 구조

#### 기존 구조 (동기)
```
Webhook 요청 → diff 조회 → OpenAI 호출 → 댓글 등록 → 리뷰 저장 → 응답
```
---
#### 개선 구조 (비동기 + 병렬 처리)
```
Webhook 요청 → 이벤트 검증 → 202 Accepted 응답
                                ↓
                           reviewAsync()
                                ↓
                    diff 조회 → OpenAI 호출
                                ↓
               ┌──────── 댓글 등록
               └──────── 리뷰 파일 저장
```


## ⚙️구현 방식:

1. 비동기 처리
   - WebhookController는 이벤트 검증 후 AsyncReviewService.reviewAsync()를 호출하고 즉시 응답 반환
   - Spring의 @Async를 사용하여 리뷰 파이프라인을 별도 스레드에서 실행
   - 전용 ThreadPoolTaskExecutor를 구성하여 비동기 작업 관리

    → webhook 응답 경로에서 외부 API 호출을 제거

---

2. 병렬 처리

- GitHub 댓글 등록
- 리뷰 파일 저장

동일한 리뷰 데이터를 기반이지만, 서로 의존하지 않기 때문에 `CompletableFuture`를 활용하여 병렬 실행하도록 구현하였다.

```
// 두 작업을 동시에 실행하고
CompletableFuture<Void> commentFuture = commentAsync(...);
CompletableFuture<Void> saveFuture = saveAsync(...);

// 모두 완료될 때까지 대기
CompletableFuture.allOf(commentFuture, saveFuture).join();
```

---

### ✅ 개선 결과

* webhook 요청 처리 시 즉시 응답이 가능하도록 구조 개선 
* 외부 API 지연이 webhook 처리 성공 여부에 영향을 주지 않도록 분리
* 댓글 등록 및 파일 저장 작업을 병렬화하여 **전체 처리 시간 단축**

---

## ⚠️ 한계 및 추가 개선 사항

- 비동기 작업 실패 시 재시도 로직 미구현
  - 작업 상태 추적 (성공 / 실패 / 진행 중) 구조 필요
- 중복 webhook 처리 방지 미구현

→ 이를 보완하기 위해 향후 Redis와 DB를 함께 활용한 구조로 확장할 수 있을까?

- Redis: 
  - webhook 중복 요청 방지
  - 작업 큐 관리
  - 실패 작업 재시도 처리
  - 짧은 수명의 상태값 저장

  
- DB:
  - 작업 이력 저장
  - 성공 / 실패 상태 관리
  - 리뷰 생성 결과 및 로그 조회

---

### 💡 배운 점
- webhook과 같은 외부 시스템은 응답 시간 제약을 고려한 설계가 필수적이라는 점을 이해했다.
- 병목이 외부 I/O에서 발생하는 경우가 많다는 것을 경험했다.
- 비동기와 병렬 처리는 서로 다른 개념이며, 적절히 조합할 수 있다는 것을 알게되었다.

---
## ▶ 실행 방법

### 1) 환경 변수 설정
- `OPENAI_API_KEY`
- `GITHUB_TOKEN`
  - 권한
    - Pull Requests: Read and Write
    - Contents: Read and Write

### 2) ngrok 연결
```
ngrok http 8080
```

### 3) 애플리케이션 실행
```bash
./gradlew bootRun
```

---
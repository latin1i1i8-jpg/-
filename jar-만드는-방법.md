# jar 만드는 방법 (3가지 중 하나만 하면 됩니다)

플러그인 jar 는 **소스를 컴파일해서** 만들어집니다. 컴파일할 때 마인크래프트 서버 API(`paper-api`)가
필요한데, 제가 작업하는 환경은 그 파일을 내려받는 서버(repo.papermc.io, Maven Central 등)가
**전부 차단**되어 있어서 제가 직접 jar 를 뽑아드릴 수 없었습니다. 아래 방법 중 하나로 받으세요.

---

## ⭐ 방법 1. GitHub 에 올리면 자동으로 만들어줌 (설치할 것 없음, 권장)

컴퓨터에 아무것도 설치하지 않아도 됩니다. 3분이면 끝납니다.

1. [github.com](https://github.com) 로그인 → 오른쪽 위 **+** → **New repository** → 이름 아무거나 → **Create**
2. 새 저장소 화면에서 **uploading an existing file** 클릭
3. 이 압축을 풀어서 나온 **`rpg-core-plugin` 폴더 안의 내용 전체**를 드래그해서 업로드 → **Commit changes**
   - ⚠️ `.github` 폴더도 같이 올라가야 합니다. 안 보이면 숨김 파일 표시를 켜세요.
4. 저장소 상단 **Actions** 탭 클릭 → `Build plugin jar` 작업이 자동 실행됩니다 (2~3분)
5. 작업이 초록색 ✔ 이 되면 클릭 → 아래 **Artifacts** 의 **`RpgCore-plugin-jar`** 다운로드
6. 압축을 풀면 **`rpg-core-plugin.jar`** → 서버 `plugins/` 폴더에 넣고 재시작

빨간 ✖ 로 실패하면, 그 로그를 열어서 에러 메시지를 저에게 그대로 붙여넣어 주세요. 바로 고쳐드립니다.

---

## 방법 2. 내 컴퓨터에서 빌드

**필요한 것**: JDK 17 이상, Maven

- JDK: [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17) 설치
- Maven: [maven.apache.org/download](https://maven.apache.org/download.cgi) → 압축 풀고 `bin` 폴더를 PATH 에 추가

압축을 푼 `rpg-core-plugin` 폴더에서 터미널(윈도우는 CMD/PowerShell)을 열고:

```bash
mvn package
```

성공하면 `target/rpg-core-plugin.jar` 가 생깁니다. 그 파일을 서버 `plugins/` 에 넣고 재시작하세요.

처음 실행하면 라이브러리를 내려받아서 1~2분 걸리고, 그다음부터는 몇 초면 끝납니다.

---

## 방법 3. 저에게 다시 시키기 (네트워크 허용)

이 대화 환경의 **네트워크 설정에서 아래 도메인을 허용 목록에 추가**해주시면,
제가 여기서 바로 컴파일해서 **완성된 jar 파일을 첨부**해드릴 수 있습니다.

```
repo.papermc.io
repo1.maven.org
```

추가하신 뒤 "이제 빌드해줘" 라고 말씀만 주세요. 컴파일 에러가 있으면 제가 고쳐서
최종 jar 까지 확인해서 드립니다. (지금은 구문 검사까지만 확인된 상태입니다)

---

## 설치 후 확인

서버 재시작 후 콘솔에 이렇게 뜨면 정상입니다:

```
[RpgCore] 몬스터 스탯 26종 로드 완료 (체력/공격력 커스텀)
행성 6개 로드 완료: 테라, 이그니스, 글라키에스, 볼투스, 움브라, 종언
[RpgCore] 시련의 탑 준비 완료 — 50층, 월드: trial_tower
RpgCore 활성화 완료!
```

그 다음 게임에서:

| 명령어 | 확인할 것 |
|---|---|
| `/직업` 또는 `/job` | 직업 4개가 번호와 함께 나오는지 |
| `/행성이동 테라` 또는 `/warp 1` | 행성 월드로 이동되는지 |
| `/지도` 또는 `/map` | 서식 몬스터·난이도 링이 나오는지 |
| `/시련의탑` 또는 `/tower` | 돌방 1층에 몬스터 **1마리**가 나오는지, 잡으면 천장이 열리는지 |

베드락으로도 같이 하려면 `plugins/` 에 **Geyser-Spigot.jar + floodgate-spigot.jar** 를 추가하고
UDP 19132 포트를 열어주세요 (README 참고).

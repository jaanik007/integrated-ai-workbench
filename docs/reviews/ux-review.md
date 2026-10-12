# Integrated AI Workbench — hetkeseisu ja UX audit

## A. Ülevaatuse kontekst

- **Projekt:** Integrated AI Workbench, local-first IntelliJ Platform Base Plugin
- **Kuupäev:** 2026-10-11
- **Aktiivne verstapost:** **Milestone B — Base Plugin MVP**. Järeldus põhineb juur-README, arhitektuuri- ja
  arendusdokumentidel ning TASK-001–TASK-011 backlogil.
- **Vaadatud:** README/AGENTS juhiste hierarhia, Gradle ja IntelliJ konfiguratsioon, `base/frontend`, `base/shared`,
  `base/backend`, `.ai`, testid, taski-raportid ja projektisisene IDEA konfiguratsioon.
- **Käesolevas auditis valideeritud:**
  `GRADLE_USER_HOME=.gradle-user-home .\\gradlew.bat check verifyPluginStructure --rerun-tasks --no-daemon` lõpetas
  edukalt (6 min 29 s); 51 XML-raporteeritud testi, 0 failures/errors/skipped.
- **Piirangud:** opt-in reaalse inference'i, reaalse MCP ja reaalse judge'i testid ei saanud praeguses jooksus
  käivituda, sest nende süsteemiseadeid ei olnud antud. Need testid lõpetavad seadeta kohe edukalt. Global IntelliJ
  MCP/teema seaded ja kasutajaliidese käsitsi kasutus polnud siin sessioonis ligipääsetavad. Varasemate taski-raportite
  reaalset tõendit käsitletakse ajaloolise, mitte selle auditi uue tõendina.

## B. Kokkuvõte

| Status  | Count |
|---------|------:|
| PASS    |    42 |
| FAIL    |     6 |
| UNKNOWN |     4 |
| N/A     |     1 |

Milestone B **ei ole valmis**. Aluskiht on koherentne ja praegune kompileerimine, testid ning plugin-deskriptor läbivad
kontrolli. MVP-d blokeerivad eelkõige TASK-009 kasutajaliidese lõpetamata nõuded, TASK-011 integreeritud tegelik tõend
ning dokumentide vastuoluline staatusteave.

## C. Olulised leiud

1. **P1 — dokumentatsioon ei kirjelda üht hetkeseisu.** `docs/development/tasks/README.md` nimetab TASK-009 `BLOCKED` ja
   TASK-010 `VERIFIED`, kuid sama faili päis ütleb veel „TASK-001 ready“. `docs/development/README.md` ütleb TASK-009
   `READY` ja TASK-010 `IN PROGRESS`; `docs/security/README.md` nimetab Milestone B-d endiselt `PLANNED`. See muudab
   jätkuplaneerimise ebausaldusväärseks.
2. **P1 — Tool Window ei täida veel kogu TASK-009 kasutusvoogu.** `AiWorkbenchPanel` käivitab olemasoleva koordinaatori
   taustal ning kuvab tulemuse, kuid ei kuva avastatud tööriistade/skeemide ja runtime'i tegelikku olekut ega paku
   tagajärjega MCP-kõne operatsioonispetsiifilist approve/reject voogu. Backend oskab sellise kõne tagasi lükata olekuga
   `TOOL_APPROVAL_REQUIRED`, kuid UI ei saa seda kinnituseks muuta. Lisaks puudub selles auditis kasutajale nähtav
   Development Instance'i läbikäik registreeritud mudeliga.
3. **P1 — MVP otsast-lõpuni tõend puudub.** TASK-009 raportis on ajalooline reaalne model → MCP → continuation
   testitõend, kuid tegelik Tool Window läbikäik jääb tõendamata. TASK-010 raport ütleb otse, et reaalset kohaliku
   judge'i inference'it ei käivitatud. TASK-011 on õigesti `BLOCKED`; seda ei tohi lugeda lahendatuks pelgalt 51
   deterministliku testi järgi.
4. **P2 — few-shot on ainult API-võimekus, mitte runtime-SKILL funktsioon.** `PromptPreparationService` võtab vastu
   `examples`, kuid runtime Markdown-loader/`SKILL.md` ei anna sellele praeguses teostuses kontrollitavat näidete
   semantikat ning Tool Window ei võimalda neid valida. Zero-shot ja programmiliselt edastatud näited on eri tõendus.

## D. Kontrollnimekiri

| ID                                                        | Status  | Evidence                                                                                                                                                                                        | Required action                                                                                                                             | Priority |
|-----------------------------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|----------|
| PRJ-01–06, PRJ-08                                         | PASS    | Juur-README, `build.gradle.kts`, loogilised `base/frontend`, `base/shared`, `base/backend` source rootid; plugin ei nõua cloud providerit ega teist AI-pluginat.                                | —                                                                                                                                           | -        |
| PRJ-07                                                    | FAIL    | Arendus-, turbe- ja taski-backlogis on omavahel vastuolus Milestone B/TASK-009/TASK-010 seisud.                                                                                                 | Sünkroniseeri staatustabelid ja „Current next step” ainult kontrollitud tõendiga.                                                           | P1       |
| ENV-01–05, ENV-07–08                                      | PASS    | JDK 21 toolchain, Kotlin 2.3.20, IntelliJ Platform 2026.1.3, Gradle Wrapper 9.1.0; käesoleva auditi `check` ja `verifyPluginStructure` PASS.                                                    | —                                                                                                                                           | -        |
| ENV-06                                                    | UNKNOWN | `.idea/misc.xml` kasutab `ms-21` ja bytecode target on 21; global IntelliJ Gradle/JDK/MCP seaded polnud kontrollitavad.                                                                         | Kontrolli Development Instance'i ja põhi-IDE projekti SDK/Gradle JVM ning MCP Serveri tegelikud seaded TASK-009 käigus.                     | P2       |
| STR-01–10                                                 | PASS    | Kõik Gradle source rootid, paketid, `META-INF/plugin.xml`, Tool Window factory, `.ai` struktuur ja ignoreerimisreeglid on olemas; `verifyPluginStructure` PASS.                                 | —                                                                                                                                           | -        |
| AI-01 Grounding                                           | PASS    | `FileSystemContextService` canonical path'i, lubatud rootide, failitüübi, mahu ja secret filteringuga; testid läbivad.                                                                          | —                                                                                                                                           | -        |
| AI-02 System/user prompts                                 | PASS    | `PromptPreparationService` ja `PromptComposer` hoiavad system, task, user, grounding ja examples eraldi.                                                                                        | —                                                                                                                                           | -        |
| AI-03 Reasoning ja AI-08 thinking level                   | UNKNOWN | `ModelProfile` modelleerib `ThinkingLevel`, kuid `LlamaServerAdapter.capabilities()` ütleb `supportsThinkingLevel=false`; selles auditis ei olnud reaalse toetatud runtime'i tõendit.           | Näita UI-s efektiivset thinking-võimekust ning lisa/tee capability-põhine kontroll, kui toetav runtime lisandub.                            | P2       |
| AI-04 Basic security                                      | PASS    | Programmilised Input/Output guards, ContextService, schema validation ja fail-closed ToolPermissionGuard; lokaatne runtime on seotud `127.0.0.1`-ga.                                            | —                                                                                                                                           | -        |
| AI-05 One-shot/few-shot                                   | FAIL    | `examples` argument ja piirangud eksisteerivad, kuid runtime `SKILL.md` näiteid ei parsita/laeta ega ole UI-st konfigureeritavad.                                                               | Defineeri Markdown näidete formaat, lae see RuntimeMarkdownLoaderiga ning testi, et näide jõuab koostatud prompti.                          | P2       |
| AI-06 Structured output, AI-07 parameters, AI-09 sampling | PASS    | `StructuredResponseValidator`, `ModelRequest` ning `LlamaServerAdapter.requestBody` valideerivad/edastavad schema, temperature, top-p ja max tokens.                                            | —                                                                                                                                           | -        |
| AI-10 LLM-as-a-Judge                                      | FAIL    | `EvaluationCoordinator` ja deterministlikud testid olemas; `EvaluationRealJudgeIntegrationTest` on opt-in ja TASK-010 raport ütleb „real local judge NOT RUN”.                                  | Käivita kontrollitud kohaliku judge'i päris inference ning talleta anonüümne tulemus TASK-011 tõendisse.                                    | P1       |
| AI-11 Markdown runtime loading                            | PASS    | `RuntimeMarkdownLoader` ja TASK-006/008 testid; `.ai/agents` ning `.ai/skills` on eraldi `.codex/skills`-ist.                                                                                   | —                                                                                                                                           | -        |
| AI-12 End-to-end workflow                                 | FAIL    | Ajalooline TASK-009 reaalne MCP-integratsioonitest on olemas, kuid kasutajale nähtav Tool Window rada ja päris judge puuduvad; TASK-011 on `BLOCKED`.                                           | Tee üks dokumenteeritud päris läbikäik: UI → agent/skill → grounding/security → local model → MCP → continuation → validation → evaluation. | P1       |
| UX-01–03, UX-05–06, UX-08                                 | PASS    | Natiivne Tool Window/Swing, UI töö jookseb pooled threadil ja tulemus naaseb `invokeLater` kaudu; loogilised frontend/backend/shared piirid säilivad.                                           | —                                                                                                                                           | -        |
| UX-04                                                     | UNKNOWN | Kasutatakse JB komponente ja `JBUI`, kuid light/dark käsitsi kontroll puudub.                                                                                                                   | Kontrolli Development Instance'is mõlemas teemas TASK-009 kasutusvoo käigus.                                                                | P3       |
| UX-07, UX-09                                              | FAIL    | Cancel on olemas, kuid consequential-call approve/reject, avastatud tööriistade skeemide, efektiivse runtime-võimekuse ning selge runtime läbipaistvuse UI puudub.                              | Lõpeta TASK-009 minimaalne oleku-, tööriista- ja approval-vaade; ära dubleeri backend poliitikat UI-s.                                      | P1       |
| SEC-01, SEC-03–06                                         | PASS    | Hetkeseisu lähtefailides ei leitud hard-code'itud credentials'e; `.gitignore` katab `.aim`, `_local`, mudelid ja `.env`; backend enforce'ib tool allowlisti, path/size/piire ning provenance'i. | —                                                                                                                                           | -        |
| SEC-02                                                    | N/A     | Püsivaid tundlikke kasutajatunnuseid ei ole praeguses Base Pluginis implementeeritud.                                                                                                           | Kasuta PasswordSafe'i ainult siis, kui lisatakse püsiv credential.                                                                          | -        |

## E. Core AI concepts

| Concept                       | Current implementation | Evidence                                                  | Gap / next action                                       |
|-------------------------------|------------------------|-----------------------------------------------------------|---------------------------------------------------------|
| AI-01 Grounding               | IMPLEMENTED            | `FileSystemContextService`, grounding testid              | Näita UI-s limiite/omissions.                           |
| AI-02 System and user prompts | IMPLEMENTED            | `PromptPreparationService`, `PromptComposer`              | —                                                       |
| AI-03 Reasoning               | PARTIAL                | `ThinkingLevel` contract; llama.cpp adapter ei toeta seda | Capability-põhine UI ja pärisruntime'i tõend.           |
| AI-04 Basic security          | IMPLEMENTED            | Guards, sanitizer, validator, permission guard            | Hoia enforcement backendis.                             |
| AI-05 One/few-shot            | PARTIAL                | API `examples` olemas                                     | Seo SKILL näited runtime loaderi ja UI-ga.              |
| AI-06 Structured output       | IMPLEMENTED            | Schema request + validator + testid                       | —                                                       |
| AI-07 Generation parameters   | IMPLEMENTED            | Profile/request ja HTTP request body                      | —                                                       |
| AI-08 Thinking level          | PARTIAL                | Typed profile setting                                     | Toetatud runtime'i korral kontrolli tegelikku edastust. |
| AI-09 Sampling configuration  | IMPLEMENTED            | temperature/top-p/max tokens jõuavad request body-sse     | —                                                       |
| AI-10 LLM-as-a-Judge          | PARTIAL                | `EvaluationCoordinator`, deterministlikud testid          | Käivita päris local judge.                              |
| AI-11 Markdown prompt files   | IMPLEMENTED            | Runtime loader, `.ai` agent/skill eristus                 | —                                                       |
| AI-12 End-to-end workflow     | PARTIAL                | Reaalne MCP test on taski-raportis                        | UI-põhine pärisvoog + judge + TASK-011 tõend.           |

## F. Remediation input

| Priority | File / component                                                                                                           | Required change                                                                                                                   | Verification target                                                                                                                 |
|----------|----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| P1       | `docs/development/tasks/README.md`, `docs/development/README.md`, `docs/architecture/README.md`, `docs/security/README.md` | Sünkroniseeri Milestone B ja TASK-009/010/011 staatuste tekstid; säilita `BLOCKED` kuni puuduva tõendini.                         | Dokumentides on üks tõeallikas ja kõik staatustabelid kattuvad.                                                                     |
| P1       | `base/frontend/.../AiWorkbenchPanel.kt`, TASK-009                                                                          | Lisa minimaalne tööriistade availability/provenance ja consequential-call approval/reject UX; kuva efektiivne runtime/capability. | Development Instance'is: registreeritud mudel, agent, read-only MCP tööriist, continuation, cancellation ning approval/reject voog. |
| P1       | TASK-011, reaalne lokaalne runtime                                                                                         | Käivita dokumenteeritud integreeritud töövoog ja reaalne judge; ära käsitle opt-in testide seadeta läbimist runtime-tõendina.     | TASK-011 kõik kohustuslikud kriteeriumid ning eraldi UI ja judge tõendid.                                                           |
| P2       | `RuntimeMarkdownLoader`, `.ai/skills/*`, prompt/UI valik                                                                   | Rakenda piiratud few-shot näidete formaat ja edastus; säilita näidete sanitize/mahupiirid.                                        | Test tõestab, et lubatud SKILL näide jõuab valmis prompti ja keelatud/ülemäärane näide lükatakse tagasi.                            |
| P2       | Runtime capability presentation                                                                                            | Hoia `ThinkingLevel` ainult toetatud runtime'i efektiivse seadistusena, mitte dekoratiivse valikuna.                              | Toetatud/mitte-toetatud capability test ja selge UI tagasiside.                                                                     |

## G. Lõpphinnang

Arhitektuur on sihipärane: üks Gradle moodul, selged vastutusalad, kohaliku inference'i abstraktsioon ning
programmilised turvapiirid on kooskõlas kohaliku-first eesmärgiga. Praegune lähtekood on build-ready ja plugin
descriptor on korrektne.

Järgmine rakendustöö peab olema **TASK-009 lõpetamine**: keskendu Tool Window'i nähtavale runtime/tool/approval voole ja
selle päris kasutustõendile. Kohe pärast seda tee **TASK-011** ainult päris kohalikku mudelit, MCP-d ja judge'i kasutava
integreeritud kontrollina. Dokumentatsiooni staatuste sünkroniseerimine tuleb teha samas tööjärjekorras enne, kui
backlogi põhjal uusi ülesandeid valitakse.

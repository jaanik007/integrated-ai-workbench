# Integrated AI Workbench — näidispistikprogrammide arhitektuuriaudit

## 1. Auditi päis

- **Kuupäev:** 2026-10-14
- **Repositooriumi juur:** `C:\Users\Jaanik Virve\plugins\integrated-ai-workbench`
- **Git-kontekst:** `master`, `601dbab`; enne auditit oli kasutaja tööpuus muudetud `.codex/skills/skill-iwb-plugin-pattern-audit/SKILL.md`. Seda faili ega ühtki rakenduse, Gradle'i või Git-faili ei muudetud.
- **Sihtplatvorm:** IntelliJ IDEA `2026.1.3`, IntelliJ Platform Gradle Plugin `2.19.0`, Kotlin `2.3.20`, JDK 21 (`build.gradle.kts:1-39`).
- **Ulatus:** ALL; kogu `.sampleplugins/` juurkaustade inventuur, UI/seadete lünkade audit esmajärjekorras ning valikuline süvaanalüüs Gauge, Qodana ja OpenRewrite lähtekoodis.
- **Välistused:** näidiste `build/`, `out/`, `target/`, `node_modules/`, `.git`, genereeritud ja testandmete kood; näidiste käivitamine või installimine; võrgu-, IDE- ja Qodana skannid.
- **Tõendusmeetod:** staatiline lähtekoodi, `plugin.xml`, Gradle'i, runtime Markdowni ja testide lugemine. Ükski Gradle'i test, `runIde`, päris MCP ühendus ega käsitsi UI-test ei käivitatud selle auditi ajal.
- **Oluline piirang:** lähtekoodis olemas olev klass või eelmine raport ei tõesta praeguses checkout'is käivituvat funktsiooni. Eelkõige ei ole siin auditil kontrollitud GUI-d ega päris inferentsi.

## 2. Juhtleiud

1. **P0 — praegune frontend ei ole allikapuus terviklik.** `plugin.xml` registreerib `AiWorkbenchToolWindowFactory`, `WorkbenchSettingsConfigurable` ja `OpenWorkbenchAction` (`base/frontend/src/main/resources/META-INF/plugin.xml:37-57`), kuid nende faile ei ole `base/frontend/src/main` all. Ka `AiWorkbenchPanel` viitab olematutele `WorkbenchExecutionPanel`, `ModelCatalogPanel` ja `InferenceProviderPanel` klassidele (`AiWorkbenchDashboard.kt:15-28`). See blokeerib puhta kompileerimise ja kõik UI-väited enne funktsionaalseid täiustusi.
2. **P1 — sätestuskontrakt on tugevam kui UI.** Rakendustaseme `WorkbenchSettingsService` teeb atomaarse valideeritud snapshot'i (`shared/settings/WorkbenchSettingsService.kt:21-60`), kuid nähtav `WorkbenchFeaturePanel` salvestab kohe oma nupu kaudu ega paku IntelliJ Settingsi `isModified` / Apply / Reset / Cancel elutsüklit (`WorkbenchFeaturePanel.kt:14-65`). Gauge annab väikese, sobiva mustri.
3. **P1 — mudeli-, profiili- ja runtime-backend on olemas, kuid vastavad ekraanid puuduvad.** `PersistentModelRegistry` registreerib kasutaja GGUF-faili viitena ja ei salvesta binaari (`ModelRegistry.kt:198-266`); `ModelPresetService` valideerib muudetavaid parameetreid (`ModelPresetService.kt:11-40`). Neid kasutav `ModelCatalogPanel` puudub. Ärge looge uut registrit ega kopeerige mudeleid.
4. **P1 — TASK-009 UI-osa on praegu backend-only.** `AiWorkbenchService.execute` on üks olemasolev koordinaatori sissepääs (`AiWorkbenchService.kt:62-87`), `AgentExecutionService` juba eristab tühistust, poliitika-, skeemi- ja MCP-tõrkeid (`AgentExecutionService.kt:51-173`). Puudub kasutajaliides, mis valiks profiili/agendi/konteksti, käivitaks töö, kuvaks olekut või küsiks ühe operatsiooni kinnitust.
5. **P1 — MCP-l on turvaline backend, kuid puudub läbipaistev UI.** `McpToolExecutionService` avastab ainult agendile lubatud read-only tööriistu, kontrollib sihtprojekti ning valideerib skeemi (`McpToolExecution.kt:257-339`); `ToolPermissionGuard` keelab tundmatud ja nõuab consequential käsule võtmega kinnitust (`ToolPermissionGuard.kt:6-36`). UI ei näita tegelikku MCP sihtmärki, avastatud skeeme, turvaklassi ega kinnitustaotlust.
6. **P2 — seade peab olema õige skoopiga.** Qodana eristab masinataseme `@Service(APP)` ning projektitaseme `@Service(PROJECT)` oleku (`.sampleplugins/qodana/core/src/org/jetbrains/qodana/settings/QodanaSettings.kt:26-70`). IWB-s on praegu ainult rakendustaseme `WorkbenchSettingsService`; projektipõhisteks kandidaatideks sobivad valitud agent/profiil, konteksti valik ja mittesalajased Workspace'i eelistused, mitte kasutaja runtime'i binaaritee.
7. **P2 — täpne tööolek peab jõudma UI-sse.** Qodana hoiab `StateFlow<QodanaRunState>` ja liigutab oleku käivitamisest lõpetamise või tühistamiseni (`QodanaRunInIdeServiceImpl.kt:57-160`, `QodanaRunState.kt:14-23`). IWB-l on rikkalik lõppolekute enum (`shared/run/AgentExecution.kt:5-21`), kuid puudub projektiteenus või UI-state, mis väldiks dubleeritud käivitust ja aegunud callback'i.
8. **P2 — evalveerimise tulemus on backendis struktureeritud, kuid nähtamatu.** `EvaluationCoordinator` säilitab deterministliku tõrke ülimuslikkuse ning tagastab eraldi `UNKNOWN` judge'i korral (`EvaluationCoordinator.kt:30-46`); `EvaluationResult` sisaldab olekut, fingerprint'i ja provenance'it (`shared/evaluation/EvaluationResult.kt:3-15`). See tuleb lisada alles pärast käivitatava Tool Windowi taastamist ja TASK-009 MVP voogu.

Juba olemas: local-first `llama.cpp` adapter koos localhosti, hash'i, valmisoleku ning kontrollitud seiskamisega (`LlamaServerAdapter.kt:65-164`); piiratud grounding (`ContextService.kt:12-47`); Markdownist runtime agentide/skillide turvaline laadimine (`RuntimeMarkdownLoader.kt:18-103`); tool-policy ning struktureeritud tulemuse lepingud. Puudu või UI-sse ühendamata on nähtav kontroll- ja täitmisrada.

## 3. Täielik näidiskogude inventuur

Allpoolne `sources` arv jätab välja build-väljundid, cache'id ja testandmed. Litsentsi staatus tähendab auditi käigus nähtud tõendit, mitte juriidilist arvamust kogu puu kohta. `plugin.xml` puudumine ei tähenda binaarse näidise puudumist, vaid seda, et vastavat metadata-faili juurkausta alt ei leitud.

| Kaust | Nähtav nimetus / plugin ID | Keel ja sources | Allikas / litsents | Sobivuse hüpotees | Süvaanalüüs |
|---|---|---:|---|---|---|
| `contrib-configuration` | puudub `plugin.xml` | 0 | Gradle'i konfiguratsioon, päritolu ja litsents **UNAVAILABLE** | ainult build-konventsioon; IWB UI jaoks tõend puudub | ei |
| `gauge` | Gauge / `com.thoughtworks.gauge` | Java + genereeritud Kotlin, 215 | lähtekoodi päises Apache-2.0; pluginis vendor JetBrains (`resources/META-INF/plugin.xml:1-5`) | Settings, process lifecycle, background progress | jah |
| `ide-features-trainer` | juurplugin metadata puudub | Kotlin, 49 | alamprojektides `LICENSE.txt`; juurpäritolu/litsents **UNVERIFIED** | õppetundide sisu, mitte Workbenchi runtime/UI | inventuur ainult |
| `javascript` | Bower / `com.jetbrains.plugins.bower`; ESLint / `intellij.javascript.eslint`; JSHint / `jshint` | Java + Kotlin, 177 | iga alamnäidise `LICENSE.txt` on olemas; ulatus **UNVERIFIED** | external tool seadistus ja process handler, API vanuse risk | inventuur ainult |
| `jhipster` | JHipster JDL / `org.strangeway.jdl` | Java + Kotlin, 219 | plugin metadata loeb vendor JetBrains; eraldi litsentsitõend **UNAVAILABLE** | DSL/PSI, IWB-ga nõrk funktsionaalne kattuvus | inventuur ainult |
| `open-rewrite` | OpenRewrite / `com.intellij.openRewrite` | Kotlin, 79 | README ja JetBrains metadata; kopeerimislitsentsi kindlustamiseks vaja eraldi kinnitust | Kotlin UI DSL, settings-fragmendid, async runner | jah |
| `prettierJS` | Prettier / `intellij.prettierJS` | Java + Kotlin, 76 | `readme.md`; litsentsitõend juures **UNAVAILABLE** | external tool konfiguratsioon; ei sobi mudeli/runtime'i dubleerimiseks | inventuur ainult |
| `qodana` | Qodana / `org.intellij.qodana` | Kotlin, 1357 | osa lähtekoodi päiseid Apache-2.0; profile-resurssidel LICENSE; täielik levitamisluba **UNVERIFIED** | StateFlow, cancellation, APP/PROJECT state, tulemuste kuva | jah |
| `terraform` | Terraform and HCL / `org.intellij.plugins.hcl` | Java + Kotlin, 632 | `ReadMe.md`; metadata-crawler alal LICENSE, kogu plugini litsents **UNVERIFIED** | run configuration, failitöötlus; IWB otsene kasu väike | inventuur ainult |
| `vuejs` | Vue.js / `org.jetbrains.plugins.vue` | Java + Kotlin, 649 | README; kogu puu litsents **UNVERIFIED** | keeleintegratsioon, mitte kohalik AI runtime/UI | inventuur ainult |

Inventuuri järeldus: kõik kümme kausta olid loetavad. Kaustades `contrib-configuration` ja `ide-features-trainer` puudus juurkausta `plugin.xml`; neid ei käsitletud kompileeritud pluginitena. Binaarset või litsentsilt ebaselget koodi ei soovitata kopeerida. Gauge'i, Qodana ja OpenRewrite'i mustrid on üksnes arhitektuursed võrdluspunktid.

## 4. IWB teostuskaart

| Ala | Kontrollitud teostus ja piir | Staatus |
|---|---|---|
| Sissepääsud | `plugin.xml:37-57` deklareerib Tool Windowi, Settingsi ja Tools actioni, kuid kõik kolm viidatud frontend-klassi puuduvad; `AiWorkbenchPanel` ise viitab veel kolmele puuduvale paneelile | **MISSING** |
| Seaded | `WorkbenchSettingsService` on app-level `PersistentStateComponent`, versiooniga snapshot ja valideerimisega (`shared/settings/WorkbenchSettingsService.kt:16-65`) | **IMPLEMENTED** backendis, UI **PARTIAL** |
| Mudelid/profiilid | `PersistentModelRegistry` säilitab kanonilise tee ja profiili, välistab duplikaadi; `ModelPresetService.save` valideerib displayName/temperature/topP/tokenid | **IMPLEMENTED** backendis, UI **MISSING** |
| Runtime | `AiWorkbenchService` loob ühe Workbench-managed lifecycle'i; `LlamaServerAdapter` kontrollib ressurssi/hashi ja käivitab localhosti protsessi | **IMPLEMENTED** backendis; käitus selles auditis **UNVERIFIED** |
| Agendid ja skillid | `RuntimeMarkdownLoader.reload` laeb ainult `.ai`-sse jäävad Markdown-failid, seob agenti profiili ja lubatud skill/tool-id-dega | **IMPLEMENTED** backendis, UI **MISSING** |
| Grounding | `FileSystemContextService` lubab ainult approved root'i regulaarsed toetatud failid, piirab mahu ja sanitiseerib | **IMPLEMENTED** backendis, UI **MISSING** |
| MCP/poliitika | `McpToolExecutionService` avastab/skeemib/sihtmärgib ja `ToolPermissionGuard` klassifitseerib ning autoriseerib | **IMPLEMENTED** backendis, UI **MISSING** |
| Täitmine | `AgentExecutionService.execute` orkestreerib prompti, inference'i, tool-continue'i, guardid ning typed outcomes | **IMPLEMENTED** backendis; päris UI rada **UNVERIFIED** |
| Evalveerimine | `EvaluationCoordinator` ei lase judge'il deterministlikku viga üle kirjutada ja märgib ebakindla judge'i `UNKNOWN` | **IMPLEMENTED** backendis, UI **MISSING** |
| Testid | Kontrollitud lähtekoodis on mudeliregistri, Markdown-loaderi, MCP fake-kliendi ning settingsi unit-testid, nt `PersistentModelRegistryTest.kt:10-21`, `RuntimeMarkdownLoaderTest.kt:10-37`, `McpToolExecutionServiceTest.kt:9-54` | olemas, selle auditi käigus **NOT RUN** |

## 5. UI võimekuse maatriks

| Funktsionaalne lõik | Praegune API / persistentne olek | UI / kasutaja tegevus | Hinnang | Puuduv ühendus ja minimaalne laiendus | Näidise tõend |
|---|---|---|---|---|---|
| Entry points | `plugin.xml:37-57` | registreeringud viitavad puuduvatele klassidele | **MISSING** | loo/taasta ainult registreeritud `AiWorkbenchToolWindowFactory`, `WorkbenchSettingsConfigurable`, `OpenWorkbenchAction`; aktiveeri sama Tool Window | Gauge registreerib tõendatult olemasoleva configurable'i ja service'i (`gauge/.../plugin.xml:152-174`) |
| Model registry | `PersistentModelRegistry.registerLocalModel`, mudelite persistimine | `ModelCatalogPanel` puudub | **BACKEND-ONLY** | kasutaja valib ühe GGUF-i või directory; kuva readonly path/size/status, ära kopeeri faili | OpenRewrite kasutab UI-s `TextFieldWithBrowseButton` seotust (`open-rewrite/.../OpenRewriteFragmentSettingsEditor.kt:164-188`) |
| Model profile | `ModelPresetService.save`; `ModelProfile` valideerib parameetrid | profiiliredaktor puudub | **BACKEND-ONLY** | ühenda vaid olemasolevad `displayName`, temperature, topP, maxOutputTokens, thinkingLevel; unsupported välju ei kuva | Gauge'i model-to-editor Apply/Reset (`GaugeSettings.java:44-65`) |
| Runtime | `AiWorkbenchService.unavailableReason/execute`, `LlamaServerAdapter` | runtime staatuse paneel puudub | **BACKEND-ONLY** | kuva effective config, ready/error, start/stop kui backend-leping seda toetab; ära loo teist runtime'i | Qodana käigu state ja taustaprogress (`QodanaRunInIdeServiceImpl.kt:57-160`) |
| Roles/agents | `AiWorkbenchService.agents`, `RuntimeMarkdownLoader` | agentide valik puudub | **BACKEND-ONLY** | profiili järel loe agentide list; kuva model binding ja allowed skills/tools | Qodana APP vs PROJECT scope muster (`QodanaSettings.kt:26-70`) |
| Skills | `RuntimeMarkdownLoader.configuration().skills` | skillide loend/enable/assign puudub | **BACKEND-ONLY** | näita loaderi kinnitatud skillid ja agendi tegelik seos; ära käivita Markdowni | puudub otsene ülekantav sample-muster |
| Permissions | `ToolPermissionGuard.authorize` | approval UI puudub | **BACKEND-ONLY** | UI koostab ainult konkreetse `toolId|arguments` kinnituse ja saadab selle koordinaatorile; guard jääb autoriteediks | Qodana tühistav state-masin, mitte poliitika (`QodanaRunState.kt:14-23`) |
| MCP | `McpToolExecutionService.discover/execute` | connection, tools ja schemas ei ole nähtavad | **BACKEND-ONLY** | kuva avastatud allowed read-only tööriistad, target-check ja input schema; ära esitle tundmatut käivitatavana | Qodana eraldab service'i ning view-modeli (`QodanaRunInIdeServiceImpl.kt:57-94`) |
| Context | `FileSystemContextService.collect`, `GroundingRequest` | valik puudub | **BACKEND-ONLY** | explicit file chooser/list koos max piiride ja omission reason'iga; backend kinnitab tee | OpenRewrite path-fieldi selge bindimine (`OpenRewriteFragmentSettingsEditor.kt:164-188`) |
| Inference | `AiWorkbenchService.execute`, `AgentExecutionOutcome`, `InferenceCancellation` | `WorkbenchExecutionPanel` puudub | **BACKEND-ONLY** | üks projektiteenus/UI-state käivituseks, progressiks, cancellationiks ja typed resultiks; ei tohi teha frontendist uut inference rada | Qodana `StateFlow` + cancel (`QodanaRunInIdeServiceImpl.kt:57-138`) |
| Evaluation | `EvaluationCoordinator`, `EvaluationResult` | rubric/judge/result UI puudub | **BACKEND-ONLY** | lisa hiljem read-only tulemusvaade; deterministic failure on alati esmane | Qodana tulemuste eraldi flow (`QodanaRunInIdeServiceImpl.kt:81-94`) |
| Persistence | `WorkbenchSettingsService` ja `PersistentModelRegistry` | `WorkbenchFeaturePanel` pakub otse Save'i, kuid Settings Configurable puudub | **BACKEND-ONLY** | Configurable kasutab draft-state'i, `isModified`, apply/reset; projektispetsiifiline state eraldi teenuses | Gauge `SearchableConfigurable` elutsükkel (`GaugeSettings.java:29-75`) |
| Observability | `RunMetadata`, tool provenance, validation error on tulemuses | ühtki UI-d ei ole | **MISSING** | kuva safe model/agent/skill/tool/context provenance, kestus ja reason; ära kuva secrets/hidden reasoning | Qodana UI hoiab state/resulti eraldi (`QodanaRunInIdeServiceImpl.kt:81-94`) |

Maatriksi tähendus: `BACKEND-ONLY` ei võrdu kasutajale kättesaadav funktsioon. See tähendab, et lähtekoodis on piisav või osaline leping, millele UI tohib toetuda. Kõik UI-rakenduse väited jäävad kuni puhta buildi ja Development Instance'i kontrollini **UNVERIFIED**.

## 6. Reastatud võrdlusleiud

### PAT-001 — registreeringud peavad viitama olemasolevale, kompileeritavale UI-le

- **Subsystem / raskus / usaldus:** frontend registration; **P0**; **HIGH**.
- **Näidis:** Gauge'i `resources/META-INF/plugin.xml:152-174` seob `applicationConfigurable` klassiga `GaugeSettings` ja registreerib `GaugeSettingsService`; mõlemad lähteklassid on olemas. Juhttee on Settings → `GaugeSettings.createComponent` → `GaugeConfig` → `GaugeSettingsService.loadState` (`GaugeSettings.java:44-65`).
- **IWB vaste:** `base/frontend/src/main/resources/META-INF/plugin.xml:37-57` registreerib kolm puuduvat klassi. `AiWorkbenchDashboard.kt:15-28` nõuab veel kolme puuduvat paneeli. Staatus: **MISSING**.
- **Erinevus:** Gauge'i descriptor ja classpath on kooskõlas; IWB descriptor/kompileeritav allikapuu ei ole.
- **Minimaalne kohandus:** enne feature-tööd taasta või loo ainult kuus viidatud frontend-klassi, hoides tegevuslogika `AiWorkbenchService`is ja persistence'i olemasolevates teenustes. Kontroll: `compileKotlin`, `verifyPluginStructure`, Development Instance'i kolm entry pointi.
- **Sobivus/risk/maht:** IntelliJ 2026.1.3 API-ga sobiv põhitee; ühtki Gauge'i koodi ei kopeerita. **M**. Risk on stale build-output'i pidamine lähteallikaks.
- **Nõude kaart:** `skill-create-ui-frontend` Phase 1; TASK-009 Tool Window; TASK-011 eeltingimus.
- **Otsus:** **ADOPT PATTERN** — descriptor ja tegelik klass peab olema ühe terviku osa.

### PAT-002 — Settingsi vorm peab kasutama draft'i ning Apply/Reset/Cancel semantikat

- **Subsystem / raskus / usaldus:** application settings; **P1**; **HIGH**.
- **Näidis:** Gauge `GaugeSettings` rakendab `SearchableConfigurable`: `createComponent` loeb mudeli, `isModified` võrdleb draft'i, `apply` salvestab, `reset` taastab ja `disposeUIResources` vabastab UI (`.sampleplugins/gauge/src/com/thoughtworks/gauge/settings/GaugeSettings.java:29-75`).
- **IWB vaste:** `WorkbenchSettingsService.update` valideerib ja asendab snapshot'i atomaarseks (`shared/settings/WorkbenchSettingsService.kt:24-60`). `WorkbenchFeaturePanel.save` kirjutab otse sellele teenusele (`WorkbenchFeaturePanel.kt:54-65`); Settings Configurable'i allikas puudub. Staatus: **PARTIAL**.
- **Erinevus:** IWB backend suudab hoida viimast kehtivat olekut, kuid UI ei saa eristada pending/kehtivat seisundit ega kasutada tavapärast Cancel/Reset toimingut.
- **Minimaalne kohandus:** `WorkbenchSettingsConfigurable` hoiab immutable draft'i, kasutab Kotlin UI DSL-i, kutsub `update` ainult Apply'l ning kuvab teenusest tulevat valideerimisviga. Tool Windowi capability-paneel peab kas kasutama sama editorkomponenti või suunama Settingsisse.
- **Sobivus/risk/maht:** IntelliJ standard-API; Gauge'i vana Java/Swing vormi ei kanta üle. **S**. Ärge salvestage secreti ega modeli binaari XML-i.
- **Nõude kaart:** `skill-create-ui-frontend` §§3,7,10; Milestone C konfiguratsiooni järkjärguline töö, mitte TASK-009 tool-executioni asendus.
- **Otsus:** **IMPROVE EXISTING** — säilita IWB valideeriv service, paranda vaid presentatsioonielutsükkel.

### PAT-003 — projekti konfiguratsioon ja masinakonfiguratsioon tuleb eristada

- **Subsystem / raskus / usaldus:** persistence/settings scope; **P2**; **HIGH**.
- **Näidis:** Qodana kasutab app scope'i `QodanaSettings` ning project scope'i `QodanaPerProjectSettings`, sh `StateFlow`-põhist olekut (`.sampleplugins/qodana/core/src/org/jetbrains/qodana/settings/QodanaSettings.kt:26-70`).
- **IWB vaste:** `WorkbenchSettingsService` on ainult `@Service(Service.Level.APP)` (`shared/settings/WorkbenchSettingsService.kt:16-21`); `AiWorkbenchService` on projektiteenus (`backend/service/AiWorkbenchService.kt:32-38`). Staatus: **PARTIAL**.
- **Erinevus:** ühe app XML-i alla ei mahu turvaliselt eri projektide default-agent, valitud profile, explicit context ja UI workflow, samas kasutaja runtime-binaari tee ei tohi projekti seadeks muutuda.
- **Minimaalne kohandus:** lisa üks `@Service(PROJECT)` ainult siis, kui UI hakkab püsivalt hoidma projektivalikuid; hoia default model directory, lokaalne endpoint ja registri tee app scope'is. Ära lisa teist mudeliregistrit.
- **Sobivus/risk/maht:** target API-ga kooskõlas. **S**. Riski maandab selge migreerimine ja üks autoriteet iga skoobi kohta.
- **Nõude kaart:** `skill-create-ui-frontend` §7; Milestone C või TASK-009 minimaalne projektivalik, kui see on vastuvõtukriteeriumi jaoks vältimatu.
- **Otsus:** **ADOPT PATTERN** — ainult skoobi eristus, mitte Qodana cloud-funktsioonid.

### PAT-004 — käivitus, tühistus ja UI peavad jagama üht typed state-machine'i

- **Subsystem / raskus / usaldus:** execution UX; **P1**; **HIGH**.
- **Näidis:** Qodana eksponeerib `StateFlow<QodanaRunState>`, alustab coroutine'is, kasutab taustaprogressi, eristab `CANCELLED`/`FAILED`, puhastab `finally` plokis ning `cancel()` vahetab state'i (`QodanaRunInIdeServiceImpl.kt:57-160`; `QodanaRunState.kt:14-23`).
- **IWB vaste:** `AgentExecutionStatus` loetleb vajalikud lõppseisundid (`shared/run/AgentExecution.kt:5-21`) ja `AgentExecutionService.execute` kontrollib `InferenceCancellation`it ning tagastab typed outcomes (`AgentExecutionService.kt:97-140`). `WorkbenchExecutionPanel` puudub. Staatus: **BACKEND-ONLY**.
- **Erinevus:** IWB-l on tulemuse taxonomy, kuid pole üht projektiteenust, mis hoiaks aktiivset tööd, kaitseks topelt-submit'i eest ja juhiks EDT-välise täitmise UI-sse.
- **Minimaalne kohandus:** lisa TASK-009 scope'i projektipõhine presentation controller/state, mis omab üht `InferenceCancellation`it töö kohta, serialiseerib käivituse/tühistuse, kogub `AgentExecutionOutcome`i ning postitab ainult turvalist render-state'i EDT-le. Ta ei ehita runtime'i ega MCP-klienti.
- **Sobivus/risk/maht:** Qodana konkreetne coroutine/Cloud-kood ei ole ülekantav; state-transitioni muster on. **M**. Kontroll: duplicate submit, cancel, timeout, disposal, stale completion UI-testid.
- **Nõude kaart:** TASK-009 §Execution state ja §Threading; TASK-011 ainult pärast live UI tõendit.
- **Otsus:** **ADOPT PATTERN**.

### PAT-005 — mudelikataloog peab siduma olemasoleva registri ja preset-service'i

- **Subsystem / raskus / usaldus:** model configuration UI; **P1**; **HIGH**.
- **Näidis:** OpenRewrite loob settings-fragmendi `TextFieldWithBrowseButton`iga ning seob read/apply reset otse ühe settings-objektiga (`.sampleplugins/open-rewrite/src/com/intellij/openRewrite/run/editor/OpenRewriteFragmentSettingsEditor.kt:164-188`); see on kitsas vormi-andmesidumise, mitte runtime-duplikatsiooni muster.
- **IWB vaste:** `PersistentModelRegistry.registerLocalModel` kanoniseerib ja kontrollib ühe kasutajavalitud faili ning loob local profiili (`ModelRegistry.kt:211-232`). `ModelPresetService.save` valideerib lubatud parameetrid (`ModelPresetService.kt:14-40`). `AiWorkbenchPanel` kutsub olematut `ModelCatalogPanel`it (`AiWorkbenchDashboard.kt:23-25`). Staatus: **BACKEND-ONLY**.
- **Erinevus:** backend juba välistab kopeerimise, duplikaadi ning modelReference'i vaikse muutuse; UI puudumise tõttu pole seda võimalik kasutada.
- **Minimaalne kohandus:** `ModelCatalogPanel` kutsub ainult `AiWorkbenchService.profiles/registerLocalModel` ja `ModelPresetService.save`; default directory jääb `WorkbenchSettingsService`i. Näita reaalselt registry-st tulevat canonical path'i, format'i ja size'i; unsupported metainfo märgi `Unknown`.
- **Sobivus/risk/maht:** `TextFieldWithBrowseButton` on IntelliJ-native, kuid vorm tuleb teha IWB Kotlin UI DSL/Swing stiilis. **M**. Keelatud on mudeli failisisu muutmine, automaatne disk-scan või seadete kuvamine kui runtime-applied fakt.
- **Nõude kaart:** `skill-create-ui-frontend` Phase 2; TASK-009 profile selection.
- **Otsus:** **IMPROVE EXISTING**.

### PAT-006 — MCP nähtavus ja approval tuleb lisada olemasoleva guard'i peale, mitte selle kõrvale

- **Subsystem / raskus / usaldus:** MCP security/presentation; **P1**; **HIGH**.
- **Näidis:** Qodana hoiab worker'i ja tulemuste view-state'i eraldi (`QodanaRunInIdeServiceImpl.kt:81-94`), mistõttu UI ei tee analüüsi ise. Muster sobib IWB esitusse; Qodana cloud- ja publish-osa ei sobi local-first piiriga.
- **IWB vaste:** `McpToolExecutionService.discover` filtreerib agenti lubatud ja read-only tööriistad (`McpToolExecution.kt:257-259`); `execute` kontrollib projektisihtmärki, approval key'd, skeemi ja piiranguid (`McpToolExecution.kt:261-339`). `ToolPermissionGuard` keelab unknown tooli ning nõuab consequential operationile konkreetset approval key'd (`ToolPermissionGuard.kt:10-35`). UI puudub. Staatus: **BACKEND-ONLY**.
- **Erinevus:** turvakontroll on server-side autoriteetne, kuid kasutaja ei näe milline IDE, project, tool/schema ja argumendikokkuvõte päriselt otsustati; seetõttu ei saa ta teadlikku ühekordset otsust teha.
- **Minimaalne kohandus:** expose'i kitsas read-only view-model API: connection/target status, discovered allowed tools, schema description, proposed tool ja safe arguments summary. Approval-event kannab operation key'd ning UI tagastab ainult explicit approve/reject; `ToolPermissionGuard` otsustab lõplikult.
- **Sobivus/risk/maht:** **M**. Ärge tehke uusi IDE tööriistu, ärge tõstke `UNKNOWN` read-onlyks ega hoidke approval'it globaalsete seadetena.
- **Nõude kaart:** TASK-009 acceptance 16–27; TASK-011 UI-eeltingimus.
- **Otsus:** **ADOPT PATTERN**.

### PAT-007 — diff/apply mustrit ei tohi veel võtta kasutusele

- **Subsystem / raskus / usaldus:** proposed edits; **P3**; **MEDIUM**.
- **Näidis:** OpenRewrite registreerib eraldiseisvad Run Configurationi, `AsyncProgramRunner`i ja editor-fragmendid (`.sampleplugins/open-rewrite/resources/META-INF/plugin.xml:80-96`; `OpenRewriteProgramRunner.kt:39-79`).
- **IWB vaste:** praegune MCP guard klassifitseerib `create_`, `write_`, `edit_`, `delete_`, `execute_`, `run_`, `terminal_` ja `git_` consequential operations'iks (`ToolPermissionGuard.kt:16-34`). Staatus: **IMPLEMENTED** turvapiirina; diff/apply UX **MISSING**.
- **Erinevus:** OpenRewrite'il on eesmärgipärane migration domain ja IntelliJ execution integratsioon; IWB-l ei ole veel kinnitatud nõuet mudeli kirjutusettepanekute preview/apply jaoks.
- **Minimaalne kohandus:** ära kanna seda mustrit TASK-009-sse. Kui tulevikus tekib eraldi requirement, kasuta IntelliJ native diff/undo ning operation-specific approval'i, kuid ära kirjuta faili otse LLM-vastusest.
- **Sobivus/risk/maht:** potentsiaalselt **L**, kõrge turva- ja UX-risk. Litsentsi/API-veresioon vajab enne kopeerimist kontrolli.
- **Nõude kaart:** uus nõue, mitte TASK-009 ega olemasolev UI skill.
- **Otsus:** **DEFER**.

## 7. Qodana / inspections

- **Staatus:** **NOT ACCESSIBLE**.
- **Mehhanism:** `Get-Command qodana` ei leidnud CLI-d; repositooriumi juurest ei leitud `qodana.yaml`, SARIF-i ega muud kohalikku Qodana raportit. Agendile ei ole pakutud IDE inspection-result API-t.
- **Tulemus:** skanni ei käivitatud. Ei laaditud alla linterit/imagenit, ei käivitatud containerit, ei muudatud IDE seadeid ega saadetud lähtekoodi võrku.
- **Seos leidudega:** Qodana näidis on ainult source-pattern'i tõend; see ei ole IWB staatilise analüüsi tulemus ega tõesta ühtki IWB runtime-viga.

## 8. Teostuse üleandmine

Järjekord säilitab ühe Gradle mooduli ning `frontend/shared/backend` piirid.

1. **P0 — tee frontend uuesti kompileeritavaks.** Omanik: `frontend`. Kontrolli/taasta `AiWorkbenchToolWindowFactory`, `WorkbenchSettingsConfigurable`, `OpenWorkbenchAction`, `WorkbenchExecutionPanel`, `ModelCatalogPanel`, `InferenceProviderPanel`; seejärel käivita puhas `compileKotlin` ja `verifyPluginStructure`. Viited: `plugin.xml:37-57`, `AiWorkbenchDashboard.kt:15-31`, Gauge `plugin.xml:152-174`. Kaardistus: `skill-create-ui-frontend` Phase 1, TASK-009 precondition.
2. **P1 — Settingsi draft-semantika.** Omanik: `frontend` + olemasolev `shared/settings/WorkbenchSettingsService.kt`. Ära muuda persistentsi autoriteeti; lisa Configurable, `isModified`, Apply/Reset/Cancel ja valideerimisvea kuva. Viited: `WorkbenchSettingsService.kt:21-60`, `WorkbenchFeaturePanel.kt:14-65`, Gauge `GaugeSettings.java:44-75`. Kaardistus: UI skill §§3/7.
3. **P1 — model catalog/profiiliredaktor.** Omanik: `frontend`, kasutades `backend/model`. File chooser registreerib ainult kasutaja valitud GGUF-i; profiilivorm kasutab ainult `ModelPresetService`. Viited: `ModelRegistry.kt:198-266`, `ModelPresetService.kt:14-40`, OpenRewrite `OpenRewriteFragmentSettingsEditor.kt:164-188`. Kaardistus: UI skill Phase 2 ja TASK-009 selection.
4. **P1 — TASK-009 execution presentation controller.** Omanik: `frontend` projektiteenus või selgelt dispose'itav controller. See kutsub ainult `AiWorkbenchService.execute`, hoiab cancellationi ja renderdab `AgentExecutionOutcome`i. Viited: `AiWorkbenchService.kt:62-87`, `AgentExecutionService.kt:51-173`, `AgentExecution.kt:5-45`. Kaardistus: TASK-009 §§Execution state/Threading; see on TASK-011 live-kontrolli eeldus.
5. **P1 — MCP discovery/provenance/approval UI.** Omanik: `frontend` read-only view model + `backend/mcp` minimaalne ekspordileping, kui olemasolev API ei anna turvalist summary't. Viited: `McpToolExecution.kt:257-367`, `ToolPermissionGuard.kt:6-36`, Qodana `QodanaRunInIdeServiceImpl.kt:81-94`. Kaardistus: TASK-009 16–27.
6. **P2 — projektiskoop ainult vajalikele valikutele.** Omanik: `shared/settings` + `frontend`. Lisa üks PROJECT service vaid selgete project-defaultide jaoks; säilita masinapõhised runtime path'id APP scope'is. Viited: `WorkbenchSettingsService.kt:16-21`, Qodana `QodanaSettings.kt:26-70`. Kaardistus: UI skill §7; eraldi väikese scope'iga töö.
7. **P2 — tulemuse ja evaluatsiooni vaade.** Omanik: `frontend`. Lisa pärast punktide 1–5 täitmist read-only provenance/timing/validation/evaluation paneel. Viited: `AgentExecution.kt:23-45`, `EvaluationCoordinator.kt:30-46`, `EvaluationResult.kt:3-15`. Kaardistus: TASK-011 järel või eraldi töö.

## 9. Riskid ja litsentsid

- Näidised pärinevad eri IntelliJ Platformi ajastutest ning kasutavad eri internal/public API-sid. Võtta üle ainult põhimõte ja kontrollida valitud API kompileerumist target `2026.1.3` vastu.
- Gauge'i `SearchableConfigurable` flow on sobiv, aga tema Java/Swing ning `invokeAndWait` processi-detailid ei ole IWB inference'i jaoks eeskujuks. IWB pika töö peab jääma EDT-st välja.
- Qodana sisaldab cloud, tokeni ja publish vooge, mida local-first IWB ei tohi kaasa võtta. Sobivad vaid StateFlow/state-scope/cancellation kontseptsioonid.
- OpenRewrite'i run configuration ja kirjutavad migratsioonid ei anna õigust model-generated muudatusteks. Consequential MCP tegevused jäävad `ToolPermissionGuard`i ning operation-specific approval'i taha.
- Mudeli failitee, runtime executable'i provenance ja võimalikud future tokenid on masinaseaded; neid ei tohi commit'ida ega panna project UI-state'i. `WorkbenchSettingsService` juba ütleb õigesti, et model binaarid ja secrets jäävad settings XML-ist välja (`WorkbenchSettingsService.kt:11-14`).
- Audit ei kinnita sample-source'i levitusõigust peale nähtud päiste. Koodi kopeerimine nõuab enne eraldi litsentsi- ja attribution-kontrolli.

## 10. Tundmatud ja kontrollitaotlused

1. Kas kuus puuduvat frontend-klassi on kogemata checkout'ist puudu või on need mõnes välises/generated source root'is? Praegune `sourceSets` hõlmab ainult `base/frontend`, `base/shared` ja `base/backend` (`build.gradle.kts:25-38`), seega vajab see enne rakendamist kinnitust lähtepõhiselt, mitte vana ZIP-i järgi.
2. Pärast puhta buildi taastamist on vaja Development Instance'is käsitsi kontrollida: Tool Windowi avamine, Settings Apply/Cancel/Reset, Tools action, GGUF registreerimine, profiili/agendi valik, päris request/result, cancellation ning ühe read-only MCP tööriista avastamine/käivitamine. Ekraanipilt üksi ei kinnita seda voogu.
3. Consequential MCP approval UI jaoks on vaja otsustada, milline backend-pausi/continuation leping eksponeerida; praegune `AgentExecutionService` tagastab `TOOL_APPROVAL_REQUIRED`i, kuid selles audititavas lähtekoodis ei ole veel nähtavat pending-call resume API-t. Seda tuleb modelleerida enne UI nupu lisamist.
4. Kui soovitakse Qodana tulemust, kasutaja peab andma olemasoleva kohaliku raporti või eraldi loa kontrollitud lokaalseks scan'iks; agent ei saa olemasolevate vahenditega Qodana CLI-d käivitada.

---

Analyzed plugin folders: 3 / discovered: 10

Source-backed findings: 7

UI controls: 0 wired / 10 backend-only / 3 missing / 0 unverified

Qodana: NOT ACCESSIBLE — CLI, config ja olemasolev raport puuduvad; skanni ei käivitatud.

Source modified: NO

Recommended next existing workflow: `skill-create-ui-frontend` — alusta ainult P0 frontend-klassi/descriptor'i kooskõla taastamisest; seejärel TASK-009 minimaalne execution UI.

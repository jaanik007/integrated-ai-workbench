# Frontend ja pärisinferentsi tööseisu kontrollpunkt

Kuupäev: 2026-10-14  
Allikad: tegelik lähtekood, Git working tree ja selle sessiooni käivitatud käsud. See ei ole üldine arhitektuuriaudit.

## 1. Git status

- Aktiivne haru: `master`.
- Sessiooni alguses olid Git working tree's juba järgmised jälgitavad muudatused: neli `.codex/skills/*` faili ja `.gemini/settings.json` (`AM`), `ModelRegistry.kt`, `AiWorkbenchService.kt`, `PersistentModelRegistryTest.kt`, `WorkbenchSettingsServiceTest.kt` ja `WorkbenchSettingsService.kt` (`M`); kustutatud `AiWorkbenchDashboard.kt`, `WorkbenchFeaturePanel.kt` ning varasemad `docs/reports/*` ja `docs/reviews/ux-review.md`; lisatud frontend-klassi failid ja `tabs/` kataloog, `.ai/model/` ning `docs/ACP-ADAPTER.md`.
- See sessioon ei muutnud ühtegi rakenduskoodi faili. Sessioonis kirjutati ainult Git-ignore'itud `.aim/runtime/llama-server.properties`, kus vana puuduva `llama-server.exe` tee asendati ehitatud CPU binaariga, ning loodi `.local/` alla CPU/CUDA build-artifaktid ja kaks CUDA abiskripti. Need on Git-is praegu untracked ja neid ei tohi automaatselt commit'ida.
- Käesolev fail asub ignore'itud `_local/` all. Varasemaid muudatusi ei saa autorluse järgi kindlalt tuvastada; eristus põhineb sessiooni alguses salvestatud `git status --short` tulemil.

## 2. Frontend architecture

- Keskne paneel on `base/frontend/src/main/kotlin/com/jaanik/integratedaiworkbench/frontend/AiWorkbenchSettingsPanel.kt`. See loob `JBTabbedPane`-i ja ühise `WorkbenchFrontendContext`i.
- Viis uut tab-klassi on `SettingsPanelOverviewTab`, `SettingsPanelModelsTab`, `SettingsPanelProvidersTab`, `SettingsPanelCapabilitiesTab` ja `SettingsPanelExecuteTab` kataloogis `base/frontend/src/main/kotlin/com/jaanik/integratedaiworkbench/frontend/tabs/`.
- `plugin.xml` seob sama paneeli Tool Window factory (`AiWorkbenchToolWindowFactory`), Settings configurable'i (`WorkbenchSettingsConfigurable`) ja Tools-menu action'i (`OpenWorkbenchAction`) kaudu.
- Vanad `AiWorkbenchDashboard.kt` ja `WorkbenchFeaturePanel.kt` on working tree's kustutatud; uus paneel on nende asendusena lisatud, kuid füüsiline IDE kasutus pole kinnitatud.
- Tegelikud teenuseseosed: context kasutab `WorkbenchSettingsService`i ja `PersistentModelRegistry`t application service'itena ning `AiWorkbenchService`it project service'ina. Execute kasutab `AiWorkbenchService.executeLocal/execute`; Models kasutab `PersistentModelRegistry.registerLocalModel/remove`; Providers kasutab `AiWorkbenchService.runtimeAvailability`; Capabilities kirjutab settings snapshot'i.

## 3. Funktsionaalne seis tabide kaupa

| Tab | Seis | Tõend ja piirang |
| --- | --- | --- |
| Overview | PARTIAL | Kuvab settings snapshot'i, registreeritud mudelite arvu ja runtime availability't. Availability kontrollib faili/config'i, mitte töötavat serverit; UI-d ei kasutatud füüsiliselt. |
| Models | PARTIAL | Valib GGUF-i, peab pending add/remove loendit ning kutsub registrit Save ajal. Tegelikku IntelliJ-klikki, persistentset registreeringut ega UI kaudu valitud testmudelit ei kinnitatud. |
| Providers | PARTIAL | CPU/GPU valik salvestub; runtime availability loeb managed config'i. Provider discovery't ega GPU tervisekontrolli ei ole. |
| Capabilities | PARTIAL | Neli boolean-togglit salvestuvad `WorkbenchSettingsService`i. Rollide haldust, võimekusdokumentide haldust ega tööriistade määramist ei ole. |
| Execute | PARTIAL | Kood koostab mudeli/rolli valiku, saadab päringu ja kuvab vastuse/cancel-toimingu. Reaalne vastus saavutati testis backend-tee kaudu, mitte Execute UI nähtaval väljal. |

Ükski tab ei ole märgitud DONE ainult kompileerimise ega `runIde` käivitamise põhjal.

## 4. Settings ja püsivus

- `WorkbenchSettingsService` on persistent application service nimega `IntegratedAiWorkbenchSettings`; IntelliJ salvestab selle `integrated-ai-workbench.xml`-i. See sisaldab muu hulgas `defaultModelProfileId`, `selectedProviderId` (vaikimisi `llama-cpp-cpu`) ja capability toggle'e. Legacy `defaultModelDirectory` ning `lmStudioEndpoint` on migratsiooniks alles.
- `PersistentModelRegistry` on persistent application service nimega `IntegratedAiWorkbenchModelRegistry`; IntelliJ salvestab selle `integrated-ai-workbench-models.xml`-i. Registry hoiab ID-d, display name'i, kanonilist teed ja profile-assotsiatsioone, mitte GGUF baitandmeid.
- `.aim/runtime/llama-server.properties` on projektis masin-lokaalne ja Git-ignore'itud runtime-konfiguratsioon. See sisaldab käivitusfaili teed, provenance/hash'i, timeout'i ja `cudaEnabled` väärtust; see ei ole IntelliJ persistent settings.
- Kontrollitud ühendused lähtekoodis: Models → registry, Providers/Capabilities → settings ja Execute → `AiWorkbenchService`. Nende ühenduste kasutamist IntelliJ UI kaudu ei kontrollitud.

## 5. Inference pipeline

`PersistentModelRegistry.registerLocalModel` valideerib `.gguf` laiendi ja kanonilise tee ning loob `ModelProfile`i provideriga `llama-cpp-server`. `AiWorkbenchService.executeLocal` valib profiili, ehitab `ModelRequest`i ja loob `LocalInferenceService`i. See kasutab `ModelPathValidator`it, `RuntimeCompatibilityMatcher`it, `JvmHardwareDetector`it ja `LifecycleRuntimeManager(LlamaServerAdapter(...))`it. Adapter käivitab `.aim/runtime/llama-server.properties` järgi `llama-server.exe`i localhostis, teeb päringu ja peatab hallatud protsessi.

Tegelik testmudel kontrolliti: `.ai/model/Llama-3.2-1B-Instruct-UD-Q8_K_XL.gguf` on loetav, 1,843,568,672 baiti, algab `GGUF`-iga, versioon on 3, tensorite arv 147 ja metadata key/value arv 36. SHA-256 on `5C890975F8A8A88277E74A875623CB2C410B020B546C4DF1AD984732787A1129`.

CPU tee on käivitatud päriselt automatiseeritud backend-testidena: `LlamaServerRealInferenceTest` sai serverilt mitte-tühja vastuse ja `ModelResponseStatus.SUCCESS`. Test ei logi vastuse teksti, seega selle sessiooni tõend ei kuva mudeli sõnastatud vastust Execute-vaates. Persistent registry ega UI pole selles katses tõendatud.

## 6. Testid ja kontroll

Käivitatud käsud/tulemused:

- `cmake -S .llamacpp -B .local\\llamacpp-cpu -G 'Visual Studio 17 2022' -A x64 -DLLAMA_CURL=OFF -DGGML_CUDA=OFF` — õnnestus.
- `cmake --build .local\\llamacpp-cpu --config Release --target llama-server --parallel 4` — õnnestus; olemas on `.local/llamacpp-cpu/bin/Release/llama-server.exe`, versioon `0.6.0-dev (build 4, commit 601dbab)`, SHA-256 `43284170E33C4211728F5AE8F01E945AE4C458CB2ACFCEA12A47221ED6258F3C`.
- Keskkonnamuutujatega `IWB_REAL_INFERENCE=true`, CPU executable'i ja GGUF absoluutsete teedega: `.\\gradlew.bat test --tests com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerRealInferenceTest --rerun-tasks --no-daemon` — `BUILD SUCCESSFUL in 58s`. JUnit XML: `build/test-results/test/TEST-com.jaanik.integratedaiworkbench.backend.runtime.LlamaServerRealInferenceTest.xml`; 1 test, 0 failures/errors/skips, aeg 5.679 s.
- `.\\gradlew.bat runIde --no-daemon` — Development Instance käivitati; `.intellijPlatform/sandbox/integrated-ai-workbench/IU-2026.1.3/log_runIde/idea.log` sisaldab `Loaded custom plugins: Integrated AI Workbench (0.1.0-SNAPSHOT)`. Tool Windowi tegelikku kasutust ei saanud CUA abil teha: surface inventory oli `apps: []`, `browsers: []`.
- Käsitsi ajutise CPU serveri health-capture ei jõudnud valmis: katse lõppes veaga `llama-server did not become healthy within 180 seconds.` See toimus paralleelselt CUDA build'iga; testiprotsess peatati finally-plokis. See ei asenda ega tühista õnnestunud hallatud JUnit inferentsi.

Käivitamata või kontrollimata: `check`, `verifyPluginStructure`, füüsiline UI registreerimine/valik/päring/vastus/cancel, UI-s nähtav mudelivastus, persistent registry tegelik fail, provider discovery ja GPU inferents.

## 7. Tõendatud takistused

- Algses masin-lokaalses runtime-konfiguratsioonis viidati puuduvale `C:/Users/Jaanik Virve/plugins/llama-b11429-bin-win-cpu-x64/llama-server.exe`-ile. See sessioon muutis ignore'itud konfiguratsiooni uue CPU binaari absoluutsele teele.
- `.llamacpp/` sisaldab lähtekoodi, mitte valmis runtime'i; CPU binary tuli eraldi ehitada `.local/llamacpp-cpu` alla.
- UI automatiseerimise surface puudus, mistõttu Models → Providers → Execute füüsilist töövoogu ei saanud kontrollida.
- Esimene CUDA CMake katse Visual Studio generatoriga ebaõnnestus: `No CUDA toolset found.`, sest `nvcc` ei leidnud `cl.exe`-d tavalisest shellist. `vcvars64.bat` + NMake konfiguratsioon õnnestus (CUDA 12.8.61, RTX 5090, 32607 MiB; arhitektuur 120a), kuid build peatati kasutaja checkpointi soovil umbes 25% juures. GPU `llama-server.exe`i ega inference't ei ole.
- CPU CMake build tõmbas CMake konfiguratsiooni käigus llama.cpp UI assetid upstreamist; edasises checkpointi töös uusi sõltuvusi ei laaditud.

## 8. Allesolev juurutus

- Rollide loomine, muutmine, profiilile seostamine ja nende tegelik UI haldus.
- Capability dokumentide, skillide ning tool-permission'ite haldus ja nähtavus, mitte ainult neli toggle'it.
- Provider discovery, tervisekontroll ning GPU tegelik riistvara/VRAM/runtime-validatsioon.
- Models → Providers → Execute täielik füüsiline UI töövoog: mudeli registreerimine, valik, päring, nähtav vastus ja katkestamine.
- Töövoogude/agentide orkestreerimise kasutajaliides ning UI kaudu MCP/tool execution. Praegune Execute saadab `toolExecutionEnabled=false`.

## 9. Järgmised tegevused

1. Ava esmalt see checkpoint ja kontrolli `git status --short`; hoia olemasolevad kasutaja muudatused ning `.local/` build-artifaktid eraldi.
2. Taasta või ühenda ligipääs Development Instance'i UI-le, seejärel registreeri test-GGUF ainult olemasoleva Models-tab'i kaudu ja kinnita IntelliJ persistent registry tegelik salvestus.
3. Vali Providers-tab'is CPU, käivita Execute-tab'is lühike päring ning salvesta nähtav vastus ja cancel-katse; vajadusel paranda ainult esimene tegelik tõrge.
4. Kui CPU UI tee töötab, lõpeta eraldi `.local/llamacpp-cuda-nmake` GPU build, fikseeri binaari provenance/hash ja käivita GPU inferents explicit `cudaEnabled=true` konfiguratsiooniga.
5. Pärast funktsionaalse voolu kinnitamist käivita sihitud plugin/runtime validatsioonid ning vasta alles siis lõpetamata rollide, capability dokumentide, discovery ja töövoogude nõuetele.

## 10. Jätkamise juhis

Ava järjekorras:

1. `_local/reports/frontend-progress-checkpoint.md` ja `git status --short`;
2. `.aim/runtime/llama-server.properties`, `base/backend/src/main/kotlin/com/jaanik/integratedaiworkbench/backend/service/AiWorkbenchService.kt` ja `base/backend/src/main/kotlin/com/jaanik/integratedaiworkbench/backend/model/ModelRegistry.kt`;
3. `base/shared/src/main/kotlin/com/jaanik/integratedaiworkbench/shared/settings/WorkbenchSettingsService.kt` ning `base/frontend/src/main/kotlin/com/jaanik/integratedaiworkbench/frontend/AiWorkbenchSettingsPanel.kt`;
4. viis `base/frontend/src/main/kotlin/com/jaanik/integratedaiworkbench/frontend/tabs/SettingsPanel*Tab.kt` faili;
5. `base/backend/src/test/kotlin/com/jaanik/integratedaiworkbench/backend/runtime/LlamaServerRealInferenceTest.kt` ja vajadusel `.local/build-llamacpp-cuda.cmd`.

Jätka pooleliolevast UI-tõendamisest, mitte uue inference engine'i või registry loomisega. CPU backend-inferents on tõendatud; UI kaudu nähtav vastus ja GPU tee ei ole.

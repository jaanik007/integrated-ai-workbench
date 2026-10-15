# Integrated AI Workbench — Qodana readiness audit

## 1. Analüüsi metaandmed ja keskkond

- **Kuupäev:** 2026-10-14
- **Režiim:** `$skill-iwb-qodana-audit CHECK`
- **Repositoorium:** `C:\Users\Jaanik Virve\plugins\integrated-ai-workbench`
- **Git-kontekst:** `master`, `601dbab`. Tööpuus olid enne auditit muudetud kaks skill-faili:
  `.codex/skills/skill-iwb-plugin-pattern-audit/SKILL.md` ja `.codex/skills/skill-iwb-qodana-audit/SKILL.md`; neid ei
  muudetud.
- **Kontrollitud kontekst:** `AGENTS.md`, root `README.md`, `docs/architecture/README.md`, `docs/security/README.md`,
  `docs/development/ROADMAP.md`, `docs/development/tasks/README.md`, `docs/reviews/ux-review.md`, Gradle'i
  konfiguratsioon, `plugin.xml`, production/test source roots ja [plugin-pattern-audit.md](plugin-pattern-audit.md).
- **Projekti tööriistaahel:** `build.gradle.kts:4-39` deklareerib Kotlin `2.3.20`, IntelliJ Platform Gradle Plugin
  `2.19.0`, IntelliJ IDEA `2026.1.3` ja JDK 21. Tegelik `java -version` tagastas Temurin `21.0.12.1`.
- **Eesmärk:** teha kindlaks, kas agent saab teha tasuta kohaliku Qodana JVM analüüsi ilma installi, allalaadimise,
  Dockeri, cloudi või IDE seadete muutmiseta.

## 2. Qodana konfiguratsioon ja tegelik täitmisrežiim

- `Get-Command qodana` leidis: **NOT FOUND**.
- Repositooriumi juure `qodana.yaml`: **NOT FOUND**.
- Juba olemasolev IWB SARIF / `qodana-report.json`, välja arvatud näidised, build-output ja `_local/reports`: **NOT
  FOUND**.
- IntelliJ kasutajapluginite loetavad kaustad `AppData\Roaming\JetBrains\IntelliJIdea2026.2\plugins` ja
  `AppData\Local\JetBrains\IntelliJIdea2026.2\plugins`: Qodana-nimelist pluginat ei leitud.
- CUA rakenduste inventuur: `apps: []`, `browsers: []`; agendil ei ole nähtavat IntelliJ akent ega IDE inspection-result
  API-t.
- **Tegelik täitmisrežiim:** capability/readiness check. Qodana skanni ei käivitatud.

## 3. Skanni lõpetatuse staatus ja katvus

**Scan status: NOT RUN — no accessible Qodana engine or report.**

Kavandatud, kuid käivitamata scope oleks olnud `base/frontend/src/main/kotlin`, `base/shared/src/main/kotlin`,
`base/backend/src/main/kotlin` ja asjakohased testid. `build.gradle.kts:25-38` ühendab need ühe Gradle mooduli source
set'idesse. Näidiskogud, generated output, cache'id, `_local`, mudelid ja runtime-binaarid jäid kontrollist välja.

Qodana CLI puudumise tõttu ei kontrollitud `qodana-jvm-community` versiooni, CLI valikuid, profile'i, linteri lokaalset
olemasolu, täpset failikatvust ega exit code'i. Ei tehtud ühtki võrgu- või Docker-operatsiooni.

## 4. Leiud Qodana natiivse raskusastme järgi

**Puuduvad.** Qodana ei tootnud SARIF-it ega muid diagnostikaid; natiivseid severity-count'e ei ole lubatud välja
mõelda.

## 5. Kinnitatud leiud P0/P1/P2/P3 prioriteedi järgi

**Puuduvad Qodana leiud.** Järgmised on ainult auditivõimekuse takistused, mitte lähtekoodi vead.

| ID     | Prioriteet | Staatus   | Takistus                                                                                                               | Tõend                                                |
|--------|------------|-----------|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| QD-E01 | P1         | CONFIRMED | Qodana CLI puudub PATH-ist, seega kohalikku `qodana-jvm-community` skanni ei saa käivitada.                            | `Get-Command qodana` → `NOT FOUND`                   |
| QD-E02 | P2         | CONFIRMED | Repos puudub `qodana.yaml`; see ei ole rakendusdefekt, kuid puudub korratav analüüsiprofiil.                           | juurtaseme `qodana.yaml` puudub                      |
| QD-E03 | P2         | CONFIRMED | Agent ei leia olemasolevat IWB SARIF-raportit, mida REPORT-režiimis analüüsida.                                        | SARIF-i/reporti otsing ei andnud tulemust            |
| QD-E04 | P2         | CONFIRMED | IDE Qodana kontrolli ei saa agendi poolt teha: kasutajapluginite kaustades puudub vaste ja CUA-l puudub IntelliJ aken. | pluginkaustade read-only inventuur; `cua.getState()` |
| QD-E05 | P3         | CONFIRMED | JDK ja Gradle'i target on Qodana JVM Community eeltingimuseks sobivad, kuid see ei asenda engine'i olemasolu.          | `java -version`; `build.gradle.kts:4-39`             |

## 6. Üksikasjalikud lähtekoodipõhised leiud

**Puuduvad.** Ilma Qodana rule ID, severity, sõnumi ja asukohata ei klassifitseerita ühtki IWB lähtekoodi mustrit Qodana
leiuks.

Seotud [plugin-pattern-audit.md](plugin-pattern-audit.md) tuvastas eraldi lähtekoodiauditis frontendi registration'i ja
UI puudujäägid. Need jäävad architecture/UI auditile, mitte käesoleva Qodana CHECK-tulemuseks. Qodana puudumine ei
kinnita ega lükka neid väiteid ümber.

## 7. Valepositiivid ja konfiguratsiooniprobleemid

- Valepositiive ei ole, sest ühtki diagnostikat ei saadud.
- `qodana.yaml` puudumine on **CONFIGURATION_ISSUE** ainult korduva Qodana töövoo seisukohalt. See ei tõesta Kotlin- ega
  IntelliJ Platformi lähtekoodi defekti.
- Vana build-artifact või varasem raport ei oleks praeguse checkout'i scan evidence; ühtegi sellist raportit ei leitud.

## 8. IntelliJ Platformi ühilduvuse tähelepanekud

- Qodana jaoks oluline project baseline on selgelt määratud: JDK 21 ning IDEA `2026.1.3` (`build.gradle.kts:16-39`). See
  erineb võimalikust kasutaja IDEA `2026.2` paigaldusest; neid ei tohi segi ajada.
- `base/frontend/src/main/resources/META-INF/plugin.xml:35-57` deklareerib Platform sõltuvuse, Tool Windowi, Settings
  Configurable'i ja Tools actioni. Qodana scan puudub, seega descriptor'i kehtivust ei hinnatud Qodana poolt.
- Käsitsi IDE tee, kui kasutaja käivitab Qodana: `Tools → Qodana → Try Code Analysis with Qodana` või
  `Problems → Qodana → Try locally`. Tulemus tuleb hoida lokaalsena ning eksportida SARIF; cloud publish/upload on
  väljaspool auditi scope'i.

## 9. Kotlin / backend / frontend / shared kvaliteedihinnang

- **Kotlin:** Qodana poolt **UNVERIFIED**. Projektis on JDK 21 ja Kotlin `2.3.20`, kuid IDE/CLI inspectionit ei olnud.
- **Backend:** Qodana poolt **UNVERIFIED**. Staatiline lähtekoodi ülevaade kinnitas, et `LlamaServerAdapter` omab
  protsessi ja sulgeb selle `shutdown`is
  (`base/backend/src/main/kotlin/com/jaanik/integratedaiworkbench/backend/runtime/LlamaServerAdapter.kt:82-108`), kuid
  see ei ole Qodana diagnoos ega runtime-test.
- **Frontend:** Qodana poolt **UNVERIFIED**. `AiWorkbenchPanel` ja `WorkbenchFeaturePanel` on olemas
  (`base/frontend/src/main/kotlin/com/jaanik/integratedaiworkbench/frontend/AiWorkbenchDashboard.kt:15-59`,
  `WorkbenchFeaturePanel.kt:14-65`), kuid UI lifecycle'i ei inspekteeritud.
- **Shared:** Qodana poolt **UNVERIFIED**. `WorkbenchSettingsService` kasutab `PersistentStateComponent`i ning
  valideerib state'i
  (`base/shared/src/main/kotlin/com/jaanik/integratedaiworkbench/shared/settings/WorkbenchSettingsService.kt:16-60`),
  kuid Qodana ei kontrollinud seda.

## 10. Turbe-, concurrency- ja lifecycle-riskid

Qodana diagnostikaid ei ole. Allolev on vaid kontrollinimekiri järgmise päris skanni prioriteediks:

1. `LlamaServerAdapter`i blocking health-loop (`LlamaServerAdapter.kt:88-96`) ja tema kutsesaitide EDT-kontekst.
2. `IntelliJMcpStdioClient`i executor, timeout ja `close` cleanup
   (`base/backend/src/main/kotlin/com/jaanik/integratedaiworkbench/backend/mcp/McpToolExecution.kt:76-234`).
3. `ToolPermissionGuard.authorize` ning MCP dispatchi authorization path (`ToolPermissionGuard.kt:6-36`,
   `McpToolExecution.kt:261-339`).
4. `WorkbenchFeaturePanel`i persistence ja tulevase Settings Configurable'i Apply/Reset/Cancel elutsükkel
   (`WorkbenchFeaturePanel.kt:14-65`).

Need ei ole leidude loetelu ega tõend turbeveast. Need on IWB arhitektuuri põhjal valitud inspectioni fookusalad.

## 11. Soovitatud minimaalsed parandused

Käesolev CHECK ei soovita rakenduse lähtekoodi parandust. Korratava lokaalse Qodana analüüsi minimaalne eeldus on üks
järgmistest:

1. teha juba heakskiidetud, lokaalselt kättesaadav Qodana CLI/Community linter agentile nähtavaks ilma automaatset
   installi või allalaadimist tegemata; või
2. käivitada Qodana kasutaja IDE-s lokaalselt ja anda loodud SARIF agentile REPORT-režiimi jaoks.

Kui tulevikus CLI on juba olemas, kontrollida esmalt selle versioon ja `scan --help`, siis kasutada ainult sobivat
kohalikku käsku, näiteks pärast optionite kinnitamist:

```powershell
qodana scan --linter qodana-jvm-community --within-docker false --only-directory base --results-dir _local/qodana/results
```

See käsk on siin dokumenteeritud, **seda ei käivitatud**.

## 12. Asjakohased regressioonitestid

Qodana tõttu teste ei käivitatud. Järgmise scan'i järel tuleb iga tegelik diagnostika siduda olemasoleva väikseima
testiga, näiteks:

- `base/backend/src/test/kotlin/com/jaanik/integratedaiworkbench/backend/runtime/LifecycleRuntimeManagerTest.kt`
- `base/backend/src/test/kotlin/com/jaanik/integratedaiworkbench/backend/mcp/McpToolExecutionServiceTest.kt`
- `base/backend/src/test/kotlin/com/jaanik/integratedaiworkbench/backend/model/PersistentModelRegistryTest.kt`
- `base/frontend/src/test/kotlin/com/jaanik/integratedaiworkbench/frontend/WorkbenchSettingsServiceTest.kt`

Nende testide olemasolu ei ole värske testitõend.

## 13. Käsitsi kontrolli vajavad leiud

- Kas Qodana on kasutaja IDEA-s bundled või seatud muusse install-location'i; kasutajapluginite kaustades ei olnud
  vastet.
- Kas `Tools → Qodana` või `Problems → Qodana` on aktiivses IDEA-s saadaval. Agent ei näe praegu ühtki native app'i.
- Kui lokaalne scan toimub, tuleb kontrollida, et linter oleks `qodana-jvm-community`, scope oleks ainult `base`, cloud
  upload oleks väljas ning SARIF oleks loetav.

## 14. Lahendamata takistused

| Takistus                                    | Mõju                                      | Ohutu järgmine samm                                                  |
|---------------------------------------------|-------------------------------------------|----------------------------------------------------------------------|
| Qodana CLI/engine pole agendile kättesaadav | SCAN-režiimi ei saa alustada              | tee juba paigaldatud CLI PATH-is nähtavaks või anna olemasolev SARIF |
| Puudub `qodana.yaml`                        | scan-profiil ei ole repos dokumenteeritud | loo see ainult eraldi, selgelt lubatud konfiguratsioonitöö raames    |
| IDE UI pole CUA kaudu nähtav                | IDE-põhist skanni ei saa agent juhtida    | kasuta käsitsi lokaalset UI-teed ja jaga SARIF-i                     |
| Puuduvad Qodana tulemused                   | puuduvad rule/severity/line-põhised leiud | käivita tulevikus kontrollitud lokaalne scan                         |

## 15. Lõpphinnang

Qodana kvaliteediauditit ei olnud võimalik teha, sest ei CLI-d, olemasolevat SARIF-it ega agendile nähtavat IDE Qodana
pinda. See on kontrollitav tööriistapiirang, mitte järeldus IWB kvaliteedist. Projekti JDK 21 ja IntelliJ Platform
`2026.1.3` konfiguratsioon sobivad tulevaseks JVM Community auditiks, kuid enne tuleb saada kättesaadavaks kohalik
engine või kasutaja loodud SARIF.

| Priority | File               | Problem                       | Required correction                                             | Test                                     |
|----------|--------------------|-------------------------------|-----------------------------------------------------------------|------------------------------------------|
| P1       | Qodana environment | CLI/engine unavailable        | expose existing local CLI or provide SARIF; do not auto-install | `qodana --version`, then controlled scan |
| P2       | `qodana.yaml`      | reproducible profile missing  | define only in an explicitly authorized configuration task      | YAML validation and local scan           |
| P2       | IDE integration    | agent cannot access Qodana UI | manual local Qodana run and SARIF export                        | SARIF import in REPORT mode              |

---

Scan status: NOT RUN

Qodana engine/linter: NOT AVAILABLE TO AGENT

Inspected source scope: configured `base/frontend`, `base/shared`, `base/backend`; no Qodana scan coverage established

Total findings: N/A — no usable Qodana report

Confirmed actionable findings: 0

Unverified findings: 0

Critical blockers: Qodana CLI/engine, SARIF and agent-visible IDE Qodana surface unavailable

Report location: `_local/reports/qodana-audit.md`

Source modified: NO

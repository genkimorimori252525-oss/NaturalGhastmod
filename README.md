# NaturalGhastmod — 総統ガスト v0.02

Minecraft **1.20.1 / Forge 47.4.10 / Java 17** 向けの未完成MODです。
ユーザー提供の最新ソースを保存した開発snapshotであり、既知の不具合を修正した動作保証版ではありません。

**現在のソースにはコンパイルを妨げる問題があります。実機受入・マルチプレイ検証は未実施です。**

## 再設計・Codex実装の入口

現在の**目標仕様はv0.02の既存実装ではなく、全面再設計側**です。

- [Natural Ghast redesign — current design](docs/design/NATURAL-GHAST-REDESIGN-v0.1.md)
- [Codex implementation handoff — 2026-10-07](docs/CODEX-HANDOFF-2026-10-07.md)

特に、**Grand Danmakuの最終弾幕パターンはまだ未制作**です。Draft PR #1やTECH-HUB上のScoreはruntime / authoring経路を検証するためのテスト資産であり、完成した弾幕デザインではありません。

## 現在の実装

- `SCOUTING / CHASING / BOMBARDMENT / ENRAGED / RETREATING` の5段階AI。
- 性格4種、距離・高度の調整、最後の目撃位置、予測照準、移動先のfeint。
- `FAST_SMALL / HEAVY_SLOW / GROUND_FIRE / SPLITTER / WALL_BURST` の5種類の火球。
- 総統ガストの装甲描画、独自音声、spawn egg。

登録IDは引き続き `soutou_ghast:soutou_ghast` です。v0.02への移入でゲーム挙動は変更していません。

## 既知の不具合と未確定仕様

| 項目 | 現状 |
|---|---|
| コンパイル | 火球の`BaseFireBlock` importが誤っており、`LargeFireball.explosionPower`のprivateフィールドを直接参照しています。診断compileで2種類の原因による4 errorsを確認 |
| ソースと旧JAR | 元アーカイブのJARは旧世代で、新AI/variantを含みません。混同を避けるため旧JARは本repositoryに含めていません |
| 火球の型・描画・同期 | 独自火球をVanilla EntityTypeで生成。独自renderer未登録、variant未同期、client/serverで速度処理が不一致。保存後の再ロードで独自動作を失う構造 |
| 射撃間隔 | 視線遮蔽・射程外などで負のcooldownが0へ戻り、次の射撃が早まります |
| 周回 | 通常周回の式でdirectionの符号が相殺され、符号反転だけでは周回が逆転しません |
| 弾速・照準 | Vanillaの加速と独自profileが重なり、設定弾速・照準用speedHint・計算上の速度が一致しません |
| 地面着火 | 追加着火が`mobGriefing`/Forge eventを確認せず、通常爆発の制御を迂回します |
| 状態・追跡・移動 | 溶岩ENRAGEDが毎tick抽選、最後の目撃情報とlive情報が混在、rayと本体AABBの到達判定が不整合 |
| 分裂・壁放出 | 保存された古い目標への分裂、反射との不整合、元弾爆発による子弾への干渉経路があります。具体的な症状は実機未確認 |
| 耐久・装甲・反射 | Vanilla GhastのHP10・反射火球1000 damageを継承。装甲は描画のみ。ボス仕様として決定が必要 |

詳細な根拠行、14項目の指摘、設計不足、次担当AIへの作業順は
[現状調査・引き継ぎ](docs/SOUTOU-GHAST-AUDIT-2026-10-06.md)を参照してください。

## 検証状況

- 原本73ファイルをSHA-256で固定し、ゲーム実装と資産を照合して移入しました。
- 火球単体の診断compileは**4 errors**。完全なGradle buildの代わりではありません。
- 隔離copyの正式wrapperによる`compileJava --offline`は、foojay-resolver未キャッシュでJava compile前に停止しました。
- 静的計算・音声参照・snapshot照合のprobeを同梱しています。Minecraftの実行試験ではありません。
- **Forge実機・専用server・マルチプレイ・性能・ボスbalanceは未検証です。**

静的probeはrepository rootで実行できます（Python標準libraryのみ）。

```powershell
python docs/audit/static_probes.py
```

ビルドはJDK17を使用し、依存を取得できる環境で実行してください。
現状は上記のソースエラーが残るため、成功を期待しないでください。

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
.\gradlew.bat compileJava build --no-daemon --console=plain
```

## バージョンと資料

- 公開snapshot: **0.02**。
- 元ソースの内部versionは1.0.0でした。今回の移入でversion/display名のみ0.02へ変更しています。
- [変更履歴](CHANGELOG.md)
- [ソース・旧JARの由来](docs/audit/provenance.json)
- [火球診断compileの記録](docs/audit/javac-fireball.log)
- `README.txt`等は元アーカイブのForge MDK資料です。現在の状態は本READMEと調査報告を参照してください。

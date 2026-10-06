# 総統ガスト：現状調査・担当AI向け引き継ぎ

調査日: 2026-10-06。公開snapshotはv0.02。ゲーム挙動の修正は含まず、移入時にgradle.propertiesのversion/display名と.gitignoreのみ更新した。監査対象原本の内部versionは1.0.0。初回監査の依頼範囲は調査のみで、監査時にゲーム実装の修正、Minecraftの起動、ワールドへの投入は行っていない。本v0.02のGitHub反映は、その後のユーザー依頼による現状snapshotの公開である。

## 結論

現在のソースは「Vanilla Ghastを基礎に、性格・距離制御・戦闘段階・5種の火球を追加した試作」である。機能の入口はあるが、現在のソースにはコンパイルを妨げる問題があり、同梱JARもこのソースの実装世代ではない。先にビルドと火球の識別・描画・同期を成立させ、その後に移動・射撃・技の設計を調整する必要がある。

特に、火球のEntityType、射撃cooldown、周回方向、加速込みの弾速、地面着火のmobGriefing扱いは、技の数値調整より先に扱うべきである。本体のHP・装甲・反射耐性は不具合と断定せず、総統ガストの仕様として決める必要がある。

## 調査対象と証拠の固定

- 実際のGradle project: 本repositoryのroot（元アーカイブではSoutouGhastディレクトリ）
- Minecraft `1.20.1` / Forge `47.4.10` / official mappings `1.20.1` / Java `17` / Gradle wrapper `8.8`。
- `mod_id=soutou_ghast`、`mod_version=1.0.0`。Javaはmain 16ファイル、`Models`の非ビルド対象1ファイル。調査対象のソース・資産・ビルド定義等73ファイルをSHA-256で固定。
- 原本の上記73ファイルは調査前後で一致し、対象範囲への追加ファイルも0件。キャッシュ・IDE・CodeGraph index・既存buildはこの73ファイルに含めていない。
- 同梱JAR: `build/libs/Soutou_Ghastmod-1.0.0.jar`、SHA-256 `248da7ee01775ebe667f10e4f7fd05ce32fc5393c4013be305d0fc3b816c6240`。manifestの実装日時は `2026-03-16T02:30:27+0900`。
- 同梱JARは15クラスで、`CombatPhase` / `Temperament` / `Variant` / `SoutouGhastCombatMoveGoal` / `SoutouGhastMoveControl`を含まない。bytecodeでも本体はVanilla属性を返し、目標プレイヤーの高度差条件は4 blocks。現ソースの条件は24 blocks。
- `AI_IMPLEMENTATION_NOTES.txt`にも「更新したsource archiveだがJARを再ビルドできなかった」旨の記載がある。JARとソースの不一致は隠された問題ではなく、引き継ぎ時に必ず区別すべき既知の世代差。
- 継承元はキャッシュの `forge-1.20.1-47.4.10_mapped_official_1.20.1-sources.jar` と、その`recomp.jar`を照合した。

証拠ファイル: [provenance.json](audit/provenance.json)、[原本ハッシュ](audit/source-manifest-before.json)、[原本ハッシュ再確認](audit/source-manifest-after.json)、[既存JARの本体・火球bytecode](audit/existing-jar-core.javap.txt)。

以下のソース参照はこのproject rootを起点とする。Java package rootは `src/main/java/com/genki/soutoughast/`。実機の症状・頻度は未確認である。

## 現在のAI

### 本体と目標

`entity/SoutouGhast.java:28,74,79`でGhastを継承し、Vanilla Goalを登録する代わりに独自Goalを登録する。

| 優先度 | Goal | 役割 |
|---|---|---|
| MOVE 4 | `SoutouGhastCombatMoveGoal` | 目標がいる場合の移動目的地を生成 |
| MOVE 5 | `SoutouGhastRandomFloatGoal` | 目標がいない場合のランダム浮遊・最後の目撃位置付近への移動 |
| LOOK 7 | `SoutouGhastLookGoal` | 目標の6 ticks先の位置にyawを即座に合わせる。pitchは変更しない |
| 7 | `SoutouGhastShootGoal` | 射撃charge・variant選択・予測照準 |
| TARGET 1 | `NearestAttackableTargetGoal<Player>` | 見えるプレイヤー、高度差24 blocks以下を取得 |

独自Brain、Goalごとの行動計画、経路探索アルゴリズム、被攻撃者への報復Goalは追加されていない。目標はプレイヤーのみ。本体はVanilla属性のMAX_HEALTH 10、FOLLOW_RANGE 100、XP reward 5を継承し、Peacefulではdespawnする。

### 戦闘段階

`entity/SoutouGhast.java:99,144`。20 TPSを仮定した秒換算で、実時間保証ではない。

| 状態 | 選択条件・優先順位 | 移動速度係数 | charge解放 | 通常の射撃後待機 | 次弾までの基準間隔 |
|---|---|---:|---:|---:|---:|
| SCOUTING | 目標なし | 0.85 | 24 ticksだが射撃しない | -40 | なし |
| RETREATING | 目標との距離が退避距離未満。ENRAGEDより優先 | 1.28 | 16 | -18 | 34 ticks / 1.70秒 |
| ENRAGED | 退避条件を満たさず、低HPまたは溶岩条件 | 1.35 | 14 | 通常-22、burst開始-10、続き-8 | 通常36 ticks / 1.80秒。burstは24/22 ticks |
| BOMBARDMENT | 非ENRAGEDでLoSあり、距離≦砲撃距離+8 | 1.05 | 18 | -26 | 44 ticks / 2.20秒 |
| CHASING | その他。射撃には距離≦砲撃距離+4が必要 | 1.15 | 20 | -30 | 50 ticks / 2.50秒 |

基準間隔は状態・目標・LoSが変化しない場合。変化でchargeとcooldownが影響を受けるため、必ずこの間隔になるわけではない。

性格はspawn時にランダムに選ばれる。NBTに保存される。COWARDLY / TENACIOUS / IRASCIBLE / BALANCEDで、希望距離、退避距離、記憶時間、低HP閾値等が変わる。IRASCIBLEのENRAGED判定は実際にはHP 72%以下でも成立し、`getEnrageThreshold()`の55%だけを読んでは把握できない。

希望距離は周囲の空間の広さから22〜32を基礎に性格・溶岩補正を加えて14〜34にclamp。高度差は個体ごとの5〜9を基礎に補正して2.5〜12。記憶はCOWARDLY 60、TENACIOUS 140、その他80 ticks。地形cacheは26個の空気サンプルと最大343個の溶岩サンプルを、実装上21 ticks間隔で更新する。

CHASINGではプレイヤーの向きの後方・側方へ移動し、通常は周回を試み、近距離では退避、時々横・上方向のfeintを加える。これは目的地の再選択であり、攻撃の偽予兆や敵弾に反応した回避ではない。

## 現在の技・火球

`entity/ai/SoutouGhastShootGoal.java:131,165,196,210`、`entity/projectile/SoutouGhastFireball.java:50,56,70,104,136`。

| Variant | 選ばれる主な条件 | 実際に書かれている処理 | 不足・注意 |
|---|---|---|---|
| FAST_SMALL | RETREATINGはこれのみ。CHASING 50%、ENRAGED 35%、他状態のfallback | 通常火球に弾速profileを加える | SmallFireballではない。見た目・当たり判定・直撃damageは他variantと共通 |
| HEAVY_SLOW | 通常状態の残った選択分で25% | 高めの爆発powerと遅めのprofile | 重量・サイズ・直撃damage増加は未実装。基礎power1なら爆発power2 |
| GROUND_FIRE | 通常状態で地上目標かつ距離>16、最初の抽選25% | 目標の足元付近を狙い、着弾位置Yの3×3領域に条件付きで火を設置、その後通常爆発 | 地表を追う弾ではない。Yを地形へ投影しない。着火はmobGriefing判定を通らない |
| SPLITTER | 通常状態で距離>24かつground抽選を通過後1/3。ENRAGED 25% | 18 ticks到達、または発射時の目標地点から6 blocks以内で、FAST_SMALL 3弾に分裂し元弾を破棄 | 追尾弾ではない。分裂先は保存された発射時の目標。反射時も保存目標が残る |
| WALL_BURST | CHASING 50%、ENRAGED 40% | ブロック着弾面の外向き法線と接線を基に4弾を放出、その後元弾が爆発 | 敵への直接命中では4弾は出ない。壁を狙う専用照準もない。元弾の爆発と子弾の干渉に注意 |

全variantはLargeFireballの直撃6 damage、通常火球の反射、爆発を継承する。爆発powerはHPではなく現在のENRAGED状態で+1し、HEAVYにはさらに+1する。通常spawnではbase1。爆発damageは距離・遮蔽等で変わり、固定damageと混同しない。

予測照準は目標の現在のdeltaMovementに見積もり飛行時間を掛ける一次予測。飛行時間は4〜28 ticksにclampし、ENRAGEDは×1.1、TENACIOUSは×1.08するので最終上限は28を超える。実際の火球の加速・水中減速・反射・地形・目標の方向転換は解かない。

ENRAGEDのburstも一度に円形弾幕を発射する技ではなく、単発射撃間隔を一時的に短縮する処理。扇・円・螺旋・波・安全帯・一定の技順序等の弾幕設計は現在のコードにはない。

## 先に対処すべき問題

優先度は次担当の作業順であり、すべてを同じ重大度の不具合として扱わない。「静的確定」はコードの分岐・計算・継承を確認したことを意味し、実機で症状を観測したことは意味しない。

### F01 / 最優先 / コンパイル阻害：火球クラスに2種類のソース誤り

`entity/projectile/SoutouGhastFireball.java:12,125,172,186`。

- `BaseFireBlock`のimportが`net.minecraft.world.level.BaseFireBlock`だが、1.20.1の実クラスは`net.minecraft.world.level.block.BaseFireBlock`。
- `this.explosionPower`は継承元LargeFireballのprivateフィールドであり、子クラスから直接読めない。分裂・壁放出の2箇所で参照。

JDK17と実際のForge 47.4.10 mapped jarを使った火球単体の診断コンパイルで4件のエラーを確認した。この4件は2種類の原因による。修正案はここでは適用していない。powerの保持・保存・ロード・子弾への伝達を一貫させる必要があり、単にpublic化すればよいという結論にはしない。

### F02 / 最優先 / 成果物同一性：現ソースと同梱JARは別世代

同梱JARを起動しても、この資料の5段階AI・5variantは検証できない。現ソースをビルドしてから、そのJARのSHA、実際に読み込んだMOD一覧とloaderを証拠に残す必要がある。両方が`1.0.0`であるため、version文字列だけでは見分けられない。

### F03 / 高 / 静的確定：独自火球がVanillaのEntityTypeになる

`entity/projectile/SoutouGhastFireball.java:46`は`super(level, owner, ..., explosionPower)`を呼ぶ。継承元`LargeFireball.java:21`のそのconstructorは`EntityType.FIREBALL`を固定で使用する。独自の`ModEntities.SOUTOU_GHAST_FIREBALL`は射撃・分裂・壁放出の生成に使われていない。

サーバー上のJavaオブジェクトは独自クラスでも、`getType()`はVanilla fireball。spawn packetがVanilla型を伝え、クライアントはLargeFireballを生成する。保存IDも`Entity.getEncodeId()`が型から決めるため`minecraft:fireball`になり、再ロードで独自variant・分裂・壁放出などの挙動を失う。TECH水槽の観測・識別でも独自型として分類できない。

### F04 / 高 / 修正連動事項：独自火球rendererが未登録

`client/ClientModEvents.java:22`で登録されているのは本体rendererのみ。独自火球のrendererはない。現状はF03でVanilla rendererを使っているため、この不足が見えにくい。F03だけを直すと独自型の描画が成立しない可能性がある。独自型の直接summon・クライアント生成も確認対象。未登録の具体的なエラー症状は実機未確認。

### F05 / 高 / 静的確定・症状未測定：variantと弾速がクライアントへ一致しない

`SoutouGhastFireball.java:37,58,64,70`。variantは通常フィールドで、SynchedEntityDataやspawn追加情報を定義していない。speed profileもサーバーのみ。F03により現在のクライアントはVanilla火球なので、その加速で移動し、サーバーのvariant別速度と一致しない。updateInterval10の位置・速度補正は存在するが、毎tickの物理モデルの一致を保証しない。

F03を直しても、クライアントのvariant初期値FAST_SMALLとサーバーのみのprofileという問題は残る。型・renderer・variant情報・移動同期を一つの検証範囲として扱い、映像上の命中とサーバー上の命中の差を計測する必要がある。

### F06 / 高 / 静的確定：遮蔽などで射撃cooldownを飛ばす

`entity/ai/SoutouGhastShootGoal.java:58,64,70,87,111`。射撃後の待機を負のchargeTimeで表現する一方、射程外・LoSなし・射撃条件不成立の分岐で`Math.max(0, chargeTime - n)`にする。

BOMBARDMENTで射撃直後の-26が、遮蔽1 tickで0になる。状態が維持され視線が戻れば18 ticksで射撃し、遮蔽tickを含め19 ticksで再射撃し得る。通常の44 ticksより短い。待機とchargeの状態が一つの変数で混ざっていることが原因。目標を失ってGoalが再startした際もchargeとburstが0へリセットされる。

### F07 / 高 / 数式で確認：通常周回の回転方向が相殺される

`entity/ai/SoutouGhastCombatMoveGoal.java:71–81`。angleにdirectionを掛け、tangentialにも同じdirectionを掛ける。directionが±1の場合、`cos(d*t)=cos(t)`かつ`d*sin(d*t)=sin(t)`なので、通常のorbitOffsetは両方向で完全に同じになる。5つの時刻で座標差0を確認。

`flipOrbitDirection()`はangleOffsetも変えるため目的地の変化自体は起きる。しかし「符号反転で周回が逆転する」とはならない。CHASING/RETREATING/ENRAGED追加offsetには方向依存の別項があるため、すべての移動が同一という主張ではない。

### F08 / 高 / 数式で確認：設定弾速と実速度・予測照準が一致しない

`SoutouGhastFireball.java:58,70–94`は先に`super.tick()`を呼ぶ。Minecraftは`(v+0.1)*0.95`の加速を行い、その後にvariant別lerpを加える。空中・進行方向一定・衝突なしの場合、定常速度は次の式となる。

`v* = [0.095*(1-a) + a*targetSpeed] / [1 - 0.95*(1-a)]`

| Variant | profileのtargetSpeed | 計算上の定常速度 blocks/tick | 照準に使うspeedHint |
|---|---:|---:|---:|
| FAST_SMALL | 1.45 | 1.5335 | 1.35 |
| HEAVY_SLOW | 0.55 | 0.9690 | 0.65 |
| GROUND_FIRE | 0.85 | 1.1317 | 0.80 |
| SPLITTER | 0.92 | 1.2241 | 0.95 |
| WALL_BURST | 0.95 | 1.2448 | 0.90 |

これを実機の平均速度とは呼ばない。発射直後は加速過程であり、水中・衝突・ネットワーク補正は含まない。ただし、HEAVYが0.55へ落ち着く前提と、固定speedHintでの照準は整合しない。弾速契約を決めてから軌跡と予測を合わせるべきである。

### F09 / 高 / 静的確定：GROUND_FIREがmobGriefingを迂回する

`SoutouGhastFireball.java:142,180–186`で`setBlockAndUpdate`を直接呼び、GameRulesもForgeのMobGriefing eventも確認しない。継承元LargeFireballの通常爆発にはForgeの判定があるが、この追加着火はその前に発生する。床上のair等の条件を満たす場所では、`mobGriefing=false`でも追加着火し得る。ワールド破壊可否の設定契約を決め、追加技でも尊重する必要がある。

### F10 / 中 / 静的確定：溶岩ENRAGEDが毎tick抽選で切り替わる

`entity/SoutouGhast.java:156–171`。低HP条件を満たさず、溶岩付近・LoSあり・射程条件成立・退避距離外の場合、ENRAGEDは毎tick確率1/8で選ばれ、それ以外のtickはBOMBARDMENT等へ戻る。最小継続時間・hysteresis・状態移行イベントはない。

条件を固定した20 TPSの独立抽選では、期待値で約4.375回/秒の状態切替となる。実測値ではない。移動係数、charge解放閾値、variant抽選条件が変わり、技の予兆を一定に設計しにくい。IRASCIBLEの72%判定と55%設定の二重定義も整理対象。

### F11 / 中 / 知覚契約の不足：最後の目撃情報だけで追っているわけではない

`CombatMoveGoal.java:66–94,138`では隠れた目標の現在位置・距離・向きからoffsetを作り、代替候補は現在位置を中心にする。`LookGoal.java:43`もLoSを問わず現在位置・速度を読む。メモリは座標と残りticksだけで、目標UUIDを保持しない。

独自の60/80/140 ticksの記憶とは別に、TargetGoalは独自設定を受けていないVanillaのunseenMemoryTicks60を使用する。内部で30回の確認に短縮され、通常Mobの2 ticksごとのselector cleanupで概ね60 ticks後に目標を外す。TENACIOUSの140 ticks保持がそのまま140 ticksの戦闘追跡を意味するわけではなく、残りは主にidleのanchorとして使用する。

「壁越しにも現在位置を知るAI」を意図するなら仕様化すべきであり、そうでなければ観測済み情報とliveな情報を分ける必要がある。目標変更・再ロード時のメモリ所属も検証項目。

### F12 / 中 / 移動成立の不足：rayが通ることと4×4の体が通ることが別判定

`CombatMoveGoal.java:128–158`は1本のrayで候補を採用し、着地点2 blocks手前の衝突も許容する。`MoveControl.java:37–58`は全区間の本体AABBで判定し、通れなければWAITにする。候補選択側はこの結果を受け取らず、同じような候補を繰り返し選び得る。

現時点では「障害物を迂回する3D pathfinding」ではなく、直進候補の選び直し。狭い通路・低い天井・凸角で停止し続ける可能性がある。体積を満たす候補・到達進捗・stuck復帰の設計がない。実際に停止する地形と頻度は実機で測る必要がある。

### F13 / 中 / 静的確定：分裂目標は予測照準・反射と連動しない

`ShootGoal.java:141,160`は予測したaimPointへ元弾を飛ばすが、configureに渡すのは`target.position()`。`SoutouGhastFireball.java:97,110`の分裂判定・子弾方向は保存された旧目標地点である。プレイヤーが移動しても更新されない。

さらにVanillaの反射はowner・velocity・power方向を変えるだけで、variantや保存目標を消さない。SPLITTERを反射しても、分裂時に旧目標へ向く子弾を生成するため、反射方向を維持するとは限らない。これは仕様決定と実機確認が必要。保存地点がワールド原点Vec3.ZEROだと「目標なし」相当の分岐にもなる。

### F14 / 中 / 継承処理からのリスク：壁放出の子弾が元弾の爆発に巻き込まれる

`SoutouGhastFireball.java:145–149,161,170–176`。壁から0.35 blocks外に子弾4体を追加した後、`super.onHit`で元弾を爆発させる。Vanilla Explosionは範囲内の子弾にもhurtとknockbackを与え得る。火球のhurtはlivingなdamage ownerの向きへvelocityと加速方向を変更するため、設計した4方向が爆発で上書きされる経路がある。

根拠: 抽出した`vanilla/.../Explosion.java:180–212,319`、`AbstractHurtingProjectile.java`のhurt、`Entity.java:2825`のignoreExplosion=false。fireImmuneはこの爆発干渉を解決しない。元弾の現在位置とhit位置、遮蔽、Forge eventに依存するので常に発生するとは断定しない。平面壁・床・天井で、爆発直前と直後の子弾velocityを観測すべきである。

## 設計として不足している項目

これらは依頼に存在しない要求を「未実装バグ」と扱うものではない。次担当とボスの仕様を決めるための論点である。

1. **本体の役割・耐久**: HP10、armor/toughnessの追加なし、装甲modelは描画のみ。反射LargeFireballはGhast.hurtを継承して1000 damage。ボスならHPを増やすだけでは反射の即死を解決しない。boss bar、部位破壊、無敵/弱点、攻撃後の隙、段階進行はない。
2. **技ごとの読みやすさ**: charge中のGhastの表情と共通音はあるが、variant別の色・サイズ・音・予兆・攻撃範囲表示はない。攻撃音はcharge10、発射音は解放時で、ENRAGEDの予告から射撃までは基準4 ticks。全弾が同じ1×1当たり判定、同じfire-charge描画、直撃6 damage。FAST/HEAVY等の名前だけでは避け方が伝わらない。
3. **技選択と攻撃構成**: ランダム抽選と単発射撃が中心。前技の履歴、同技連続の制限、技ごとのcooldown、コンボ、wave単位の開始・終了・cancel・回復時間はない。大量の弾を出す前に、安全帯・避けられる速度・攻撃の隙を決める必要がある。
4. **GROUND/WALLの狙い方**: 壁を狙う専用aimや地表追従がないため、技名と実際の発生条件に差がある。地面着火は着弾Yのみで探索し、持続領域・duration・所有者・消火/片付けを管理しない。
5. **寿命・密度制御**: lifeTicksは分裂判定に使うだけで、独自の最大寿命・最大飛距離・子弾数上限・戦闘終了時のcleanupはない。Vanilla側にはowner除去・unloaded chunk等によるdiscardがあり、「永久に残る」とは断定しない。命中しない開放空間で弾数を計測する必要がある。
6. **調整と再現**: ConfigはMDK sampleのlogDirtBlock/magicNumber/itemsのみ。AI/HP/弾速/射撃間隔/技確率/破壊可否の実用設定はない。性格・feint・技・溶岩状態等はrandom依存で、固定seed/replayの専用入口、選択理由・charge・variant・parent/child IDを出す観測機能もない。TECH水槽から位置を観測できても、選択理由は自動的には得られない。
7. **資産と運用**: 必要な音声参照は解決し、37 oggファイルが存在。ambient14/hurt19/death1/attack15/shoot15/impact2で、attackとshootは同じ音声pool。日本語lang、専用loot table、自然spawn/biome追加は見当たらず、手動spawn egg/summon中心。専用lootがないこととVanilla lootを継承することは別で、報酬の実際は未検証。example block/item/tabやmod作者・説明のplaceholderが残る。`Models/soutou_ghast.java`は現在のmainビルドに含まれず、rendererは`SoutouGhastArmorModel`を使用する。

性能については、必要な外部MOD依存はForgeだけで、重いアニメーションframework等は追加されていない。26+最大343の地形queryや候補ray/AABB検査の負荷はあるが、今回の調査ではTPS、起動時間、メモリを測っていない。性能超過とは判定しない。

## 検証結果と限界

| 検証 | 結果 | 言える範囲 |
|---|---|---|
| 現ソース・既存JAR・実Forge継承元の照合 | 実施 | 上記の構造・分岐・世代差 |
| 原本73ファイルの前後SHA確認 | 一致、追加0 | 対象原本は未変更 |
| `compile-copy`でwrapper8.8 / JDK17の`compileJava --offline` | 失敗 | foojay-resolver0.7.0の未キャッシュで、Java compile前に停止。wrapper本体は取得できた |
| 既存Gradle8.14.3で補助確認 | 失敗 | offline plugin解決で停止。正式wrapperの成功には数えない |
| 火球1クラスのJDK17診断compile | 4 errors | exact Forge47.4.10のMC classと、既存JARのModSounds/bootstrap等の型を使用。誤importとprivate参照を再現。完全なGradle/reobf/package検証ではない |
| 静的計算probe | exit0 | ±周回の座標差0、cooldown例、加速を含む速度の漸化式、音声参照、原本hash |
| Minecraft実機・専用server・マルチプレイ | 未実施 | 体感、描画の症状、命中、boss balance、TPSは未確認 |

全16 Javaをcached jarsで診断compileする補助試行では、4つの本物の火球エラーに加え、FML classpath不足/混在によるエラーも出た。`context.registerConfig`の補助エラーは古いNeoForge側のFMLクラスを拾ったものと判明したため、このMODの不具合として列挙していない。official MavenからのFML47.4.10補完はHTTP403だった。診断結果と完全build成功を区別する。

有効な証拠: [火球compile log](audit/javac-fireball.log)、[正式wrapper log](audit/compile.log)、[静的probe結果](audit/static-probe-results.json)、[再実行用probe](audit/static_probes.py)。公開probeはrepositoryのv0.02 snapshotを改行正規化して照合する。原本73ファイルの前後一致はhistorical evidenceとして別に保持する。probeはMinecraftの実行・回帰testの代わりではない。

次担当が実行する正式buildは、JDK17と必要なGradle/Forge依存を使用可能にした隔離copy内で、`./gradlew.bat compileJava build --no-daemon --console=plain`。原本への直接buildや既存JARの上書きは今回行っていない。

## 次担当AIへ渡す推奨順序

1. **ビルドと同一性**: F01修正後に正式build。ソースmanifest/JAR SHA/読み込みMODを固定し、旧JAR混在を除く。完成版versionの決定は別作業。
2. **火球の成立**: F03/F04/F05をまとめて検証。5variantのspawn、型ID、描画、保存→再ロード、client/serverの軌跡・命中位置を確認。地面破壊設定F09もこの段階。
3. **決定と移動**: cooldown、周回方向、弾速、状態継続、最後の目撃情報の仕様を確定し、F06/F07/F08/F10〜F12を小さな差分で扱う。
4. **派生技**: 3弾分裂・4弾壁放出を、移動目標・反射・元弾爆発・床/天井/壁で検証。原点、死亡/目標変更、save/loadも含める。
5. **ボス設計**: HP/反射/装甲、技の予兆・安全帯・隙、技順序と段階進行を仕様化。その後に弾幕draftツールでパターンを詰め、Forge実機で調整する。今のランダム単発AIを、完成したボスAIとして扱わない。

実機の最小シナリオは、開放水槽、4×4未満の通路、低天井、角、地面/壁/天井着弾、直線移動と方向転換をする目標、視線遮蔽1 tick、溶岩あり/なし、性格4種、低HP/近距離、反射、2プレイヤー、save/load、mobGriefing=false、長時間miss射撃。client/server tick・entity UUID/type・variant・phase・charge/cooldown・位置/速度・parent/child関係を記録し、改善前後を同じ条件で比較する。

この資料は調査の引き継ぎであり、上記修正・仕様追加を実装した記録ではない。

import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {pathToFileURL, fileURLToPath} from 'node:url';

// Explicit opt-in integration with an existing registered TANK_CORE host.
const option = name => { const i = process.argv.indexOf('--' + name); if (i < 0 || !process.argv[i + 1]) throw Error('Missing --' + name); return path.resolve(process.argv[i + 1]); };
const lab = option('lab'), templateFile = option('template'), original = option('original'), classpathFile = option('classpath-file');
const javaHome = option('java-home');
const repository = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const template = JSON.parse(await fs.readFile(templateFile));
assert.equal(template.launch.env.KNEEKURA_DEBUG_MOD_PROFILE, 'TANK_CORE');
const privateParent = path.join(repository, 'build/tank');
await fs.mkdir(privateParent, {recursive: true});
const trial = await fs.mkdtemp(path.join(privateParent, 'foundation-'));
const json = async file => JSON.parse(await fs.readFile(file, 'utf8'));
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const write = (file, value) => fs.writeFile(file, JSON.stringify(value) + '\n', {flag: 'wx'});
const load = name => import(pathToFileURL(path.join(lab, 'debug-workspace', name)));
const {stableJson} = await load('bridge/json.mjs');
const {registerBridgeRequest} = await load('bridge/registration.mjs');
const {launchDebugRun, readCurrent, stopCurrent} = await load('core.mjs');
const {setTargetControl} = await load('evidence/target-control.mjs');
const {evidenceRuntimeFromCurrent} = await load('evidence/runtime.mjs');
const {readTankContext} = await load('tank-cli.mjs');
const hashJson = value => sha(Buffer.from(stableJson(value)));
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const report = {schema: 'naturalghast.foundation-tank/v1', trial, profile: 'TANK_CORE', failures: [],
    limitations: ['Native player-facing and flight maneuvers NOT_RUN: original Tank too small and owner API excludes Player subjects.',
        'Canonical LAB evidence and supplementary idle/frame observations have different producers.',
        'Class-resource/container linkage is not resident transformed-definition attestation.']};
console.log('TRIAL ' + trial);
let originalRows = [], current, runtime;
async function inventory(root, relative = '') {
    const rows = [];
    for (const entry of await fs.readdir(path.join(root, relative), {withFileTypes: true})) {
        const file = path.join(relative, entry.name);
        assert(!entry.isSymbolicLink(), 'Symlink not permitted in world');
        if (entry.isDirectory()) rows.push(...await inventory(root, file));
        else if (entry.isFile()) rows.push({file, sha256: sha(await fs.readFile(path.join(root, file)))});
    }
    return rows.sort((a, b) => a.file.localeCompare(b.file));
}
async function verifyOriginal() { assert.deepEqual(await inventory(original), originalRows, 'Original world changed'); }
try {
    // Build the exact clean target source once for this coherent native verification unit.
    const targetRevision = execFileSync('git', ['rev-parse', 'HEAD'], {cwd: repository, encoding: 'utf8', windowsHide: true}).trim();
    assert.equal(execFileSync('git', ['status', '--porcelain'], {cwd: repository, encoding: 'utf8', windowsHide: true}).trim(), '', 'Commit source before verification');
    let targetBuild;
    try {
        targetBuild = execFileSync('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command',
            '& $env:NATURALGHAST_VERIFY_WRAPPER compileJava build --no-daemon --console=plain; exit $LASTEXITCODE'],
            {cwd: repository, windowsHide: true, maxBuffer: 16 * 1024 * 1024,
                env: {...process.env, JAVA_HOME: javaHome, NATURALGHAST_VERIFY_WRAPPER: path.join(repository, 'gradlew.bat')}});
    } catch (error) {
        await fs.writeFile(path.join(trial, 'target-build.log'), error.stdout ?? error.message, {flag: 'wx'});
        throw error;
    }
    await fs.writeFile(path.join(trial, 'target-build.log'), targetBuild, {flag: 'wx'});
    assert.equal(execFileSync('git', ['rev-parse', 'HEAD'], {cwd: repository, encoding: 'utf8', windowsHide: true}).trim(), targetRevision, 'Target revision changed during build');
    report.targetBuild = {status: 'PASS', sourceRevision: targetRevision};
    originalRows = await inventory(original); await write(path.join(trial, 'original-hashes.json'), originalRows);
    const world = path.join(trial, 'game/saves/KNEEKURA_DEBUG_WORLD');
    await fs.mkdir(path.dirname(world), {recursive: true}); await fs.cp(original, world, {recursive: true, errorOnExist: true, force: false});
    // Copy presentation settings only, never accounts, credentials or an existing live save.
    for (const name of ['options.txt', 'resourcepacks']) {
        const source = path.join(template.gameDir, name);
        if (await fs.stat(source).catch(() => null)) await fs.cp(source, path.join(trial, 'game', name), {recursive: true, force: false, errorOnExist: true});
    }
    const savedArgs = await fs.readFile(classpathFile, 'utf8');
    const classpath = savedArgs.split(/\r?\n/)[1].replace(/^"|"$/g, '');
    const classes = path.join(trial, 'classes'); await fs.mkdir(classes);
    const exec = (tool, args) => execFileSync(path.join(javaHome, 'bin', tool + '.exe'), args, {windowsHide: true, stdio: ['ignore', 'pipe', 'pipe']});
    const sources = ['PrepareFoundationTank.java', 'ObserveFoundationTank.java'].map(name => path.join(repository, 'tools/tank', name));
    const compileCp = classpath + ';' + path.join(repository, 'build/classes/java/main');
    const compileArgs = path.join(trial, 'javac.args');
    await fs.writeFile(compileArgs, ['--release', '17', '-encoding', 'UTF-8', '-cp', compileCp, '-d', classes, ...sources].map(x => '"' + x.replaceAll('\\', '/') + '"').join('\n'), {flag: 'wx'});
    exec('javac', ['@' + compileArgs]);
    const prepareArgs = path.join(trial, 'java.args');
    await fs.writeFile(prepareArgs, ['-cp', classes + ';' + compileCp, 'com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFoundationTank', trial, privateParent].map(x => '"' + x.replaceAll('\\', '/') + '"').join('\n'), {flag: 'wx'});
    exec('java', ['@' + prepareArgs]);
    const inputs = path.join(trial, 'inputs'), privateDir = path.join(trial, 'private');
    await fs.mkdir(inputs); await fs.mkdir(privateDir);
    const artifact = path.join(inputs, 'naturalghast-dev.jar');
    exec('jar', ['--create', '--file', artifact, '-C', path.join(repository, 'build/classes/java/main'), '.', '-C', path.join(repository, 'build/resources/main'), '.']);
    const observer = path.join(trial, 'observer.jar');
    const observerMeta = path.join(trial, 'observer-resource/META-INF'); await fs.mkdir(observerMeta, {recursive: true});
    await fs.writeFile(path.join(observerMeta, 'mods.toml'), 'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="Private verification"\n[[mods]]\nmodId="naturalghast_tank_observer"\nversion="1"\ndisplayName="Finite NaturalGhast Tank observer"\n', {flag: 'wx'});
    await write(path.join(trial, 'observer-resource/pack.mcmeta'), {pack: {pack_format: 15, description: 'Finite read-only NaturalGhast Tank observations'}});
    exec('jar', ['--create', '--file', observer, '-C', classes, 'com/genki/soutoughast/tank', '-C', path.join(trial, 'observer-resource'), '.']);
    assert(exec('jar', ['--list', '--file', observer]).toString().split(/\r?\n/).includes('pack.mcmeta'), 'Observer resource metadata missing');
    await fs.copyFile(path.join(path.dirname(templateFile), 'inputs/resources.zip'), path.join(inputs, 'resources.zip'));
    const configMaterial = Buffer.from(stableJson({profile: 'TANK_CORE', nativeScope: 'ACTIVE_IDLE_4X4_RENDER', product: 'soutou_ghast', sourceBuilt: true}));
    await fs.writeFile(path.join(inputs, 'config.bin'), configMaterial, {flag: 'wx'});
    const sourceRevision = execFileSync('git', ['rev-parse', 'HEAD'], {cwd: repository, encoding: 'utf8', windowsHide: true}).trim();
    const dirty = execFileSync('git', ['diff', '--binary', 'HEAD'], {cwd: repository, windowsHide: true});
    assert.equal(dirty.length, 0, 'Commit the source before binding runtime identity');
    assert.equal(execFileSync('git', ['status', '--porcelain'], {cwd: repository, encoding: 'utf8', windowsHide: true}).trim(), '', 'Runtime requires a clean source checkout');
    const fixture = await json(path.join(trial, 'fixture.json'));
    assert.match(fixture.baselineHash, /^[a-f0-9]{64}$/);
    const buildHash = sha(await fs.readFile(artifact)), resourceHash = sha(await fs.readFile(path.join(inputs, 'resources.zip')));
    const target = {profile_id: hashJson({profile: 'TANK_CORE', observer: 'OBSERVE_GRID'}), index_snapshot_id: hashJson({sourceRevision}),
        build_artifact_hash: buildHash, source_revision: sourceRevision, dirty_hash: sha(dirty), config_hash: sha(configMaterial), resource_hash: resourceHash};
    const experiment = 'naturalghast-' + path.basename(trial);
    const request = {schema_version: 1, experiment_id: experiment, generation: 1, target,
        arena: {arena_id: experiment, baseline_hash: fixture.baselineHash, bounds: {min: [7,224,7], max: [13,229,13]}, preset: 'original-tank-active-idle'},
        subjects: [{subject_id: 'ghast', entity_type: 'soutou_ghast:soutou_ghast', uuid: fixture.subjectUuid}], initial_state: [], actions: [],
        observation_scopes: [{kind: 'ENTITY_UUID', lanes: ['SERVER_ENTITY_STATE'], level: 'L1', subject_id: 'ghast'}],
        visual_rig: {mode: 'none'}, assertions: [{assertion_id: 'idle-health', expected: 10, field: 'health', kind: 'structured', operator: 'equals', subject_id: 'ghast'}],
        budgets: {time_budget_ms: 120000, max_actions: 0, max_captures: 0}};
    await write(path.join(inputs, 'request.json'), request); await write(path.join(inputs, 'assertions.json'), request.assertions);
    const requestHash = sha(await fs.readFile(path.join(inputs, 'request.json')));
    const binding = {schema_version: 1, experiment_id: experiment, generation: 1, request_hash: requestHash, target,
        arena_id: experiment, arena_baseline_hash: fixture.baselineHash, assertions_hash: sha(await fs.readFile(path.join(inputs, 'assertions.json')))};
    await write(path.join(inputs, 'binding.json'), binding);
    const operator = {schemaVersion: 1, requestHash,
        materialDescriptor: {schemaVersion: 1, targetModId: 'soutou_ghast', linkageMode: 'OBSERVED_CLASS_RESOURCE_AND_CONTAINER_LINKAGE',
            buildArtifactHash: buildHash, configArtifactHash: target.config_hash, resourceArtifactHash: resourceHash,
            classResources: [{className: 'com.genki.soutoughast.SoutouGhastMod', sha256: sha(await fs.readFile(path.join(repository, 'build/classes/java/main/com/genki/soutoughast/SoutouGhastMod.class')))}]},
        selection: {grantId: experiment, leaseId: experiment + '-120s', arenaEpoch: 8, expectedArenaRevision: 0, allowedActions: []},
        worldRegistration: {schemaVersion: 1, registrationId: experiment, canonicalWorldRoot: world, worldName: 'KNEEKURA_DEBUG_WORLD',
            dimensionId: 'minecraft:overworld', permissions: ['BOUNDED_DIAGNOSTIC_CONTROL']}};
    const operatorFile = path.join(privateDir, 'operator.json'); await write(operatorFile, operator);
    const initScript = path.join(trial, 'native.init.gradle');
    await fs.writeFile(initScript, `gradle.beforeProject { p ->\n p.plugins.withId('net.minecraftforge.gradle') {\n  p.dependencies.add('runtimeOnly', p.files('${artifact.replaceAll('\\','/')}','${observer.replaceAll('\\','/')}'))\n  p.afterEvaluate { p.minecraft.runs.client.workingDirectory p.file('${path.join(trial, 'game').replaceAll('\\','/')}') }\n }\n}\n`, {flag: 'wx'});
    const config = structuredClone(template);
    // The registered target source is NaturalGhast. The separate TANK_CORE host is
    // selected explicitly by Gradle project-dir and recorded as a second identity.
    config.workspaceId = experiment; config.workspaceDir = repository;
    config.runtimeRoot = path.join(trial, 'runtime'); config.gameDir = path.join(trial, 'game');
    config.readyTimeoutMs = 240000;
    config.launch.command = path.join(template.workspaceDir, template.launch.command);
    config.launch.args = ['--project-dir', template.workspaceDir, '-Pforge_version=1.20.1-47.4.10',
        ...template.launch.args.slice(0, -2).filter(arg => arg !== '--offline'), '--init-script', initScript];
    config.launch.env = {JAVA_HOME: javaHome, KNEEKURA_DEBUG_MOD_PROFILE: 'TANK_CORE', NATURALGHAST_PRIVATE_GAME_DIR: path.join(trial, 'game')};
    config.ownerControl = {requestHash, operatorRegistration: {trustedRoot: privateDir, relativePath: 'operator.json', sha256: sha(await fs.readFile(operatorFile))}};
    await write(path.join(trial, 'config.json'), config);
    await registerBridgeRequest({runtimeRoot: config.runtimeRoot, registration: {schemaVersion: 1, trustedRoot: inputs,
        requestFile: 'request.json', bindingFile: 'binding.json', assertionsFile: 'assertions.json',
        materials: {buildArtifact: {relativePath: 'naturalghast-dev.jar'}, configArtifact: {relativePath: 'config.bin'}, resourceArtifact: {relativePath: 'resources.zip'}}}});
    report.sourceRevision = sourceRevision; report.buildHash = buildHash; report.hostRevision = execFileSync('git', ['rev-parse','HEAD'], {cwd: template.workspaceDir, encoding: 'utf8', windowsHide: true}).trim();
    await verifyOriginal(); console.log('REGISTERED ' + experiment);
    try {
        await launchDebugRun(config, lab); current = await readCurrent(config, lab); assert(current.live && current.runtimeOwnership?.owned);
        report.runDir = current.runDir;
        const targetControl = await setTargetControl(current, fixture.subjectUuid, {decisionSnapshot: false}); report.targetRevision = targetControl.revision;
        runtime = evidenceRuntimeFromCurrent(current); await runtime.init();
        const deadline = Date.now() + 45000;
        let retainedObservations = [];
        while (Date.now() < deadline) {
            await runtime.ingestAvailable();
            // Use the preceding immutable ingestion snapshot. The owner clock file
            // can lag the newest client-render sample; never rewrite either clock.
            const observations = retainedObservations;
            retainedObservations = await runtime.store.readObservations();
            const context = await readTankContext({current, observations, arenaEpoch: 8,
                worldBinding: {authorityHash: hashJson(originalRows), copyBaselineHash: hashJson(originalRows), fixtureHash: hashJson(fixture), fixtureChanges: [fixture.changes]},
                profile: {kind: 'OBSERVE_GRID', grid: true, brightness: true, motion: false, decisionChannels: ['SERVER_ENTITY_STATE']},
                timeBudget: {experimentMs: 10000, finalizationMs: 5000, cleanupMs: 5000, marginMs: 5000}});
            report.preflight = context.preflight;
            const frame = await json(path.join(current.runDir, 'evidence/derived/naturalghast-foundation/frame.json')).catch(() => null);
            if (context.preflight.status === 'READY' && frame) { report.frame = frame; break; }
            const owner = await json(path.join(current.runDir, 'control/owner-status.json')).catch(() => null);
            if (['BLOCKED','OWNER_CLOSED','OUTCOME_UNKNOWN'].includes(owner?.status)) throw Error('OWNER_' + owner.status + ':' + owner.error);
            await sleep(500);
        }
        assert.equal(report.preflight?.status, 'READY'); assert(report.frame, 'Native frame missing');
        const idle = (await fs.readFile(path.join(current.runDir, 'evidence/derived/naturalghast-foundation/idle.jsonl'), 'utf8')).trim().split('\n').map(JSON.parse);
        assert.equal(idle.length, 40); assert(idle.every(row => !row.noAI && !row.targetPresent && row.intent === 'HOLD' && row.speed < 1e-6 && row.collisionFree && row.width === 4 && row.height === 4 && row.health === 10));
        assert(report.frame.loadedModIds.includes('soutou_ghast')); assert(report.frame.loadedModIds.includes('embeddium'));
        report.idleSamples = idle.length; report.nativeScope = 'PASS_ACTIVE_IDLE_4X4_CLEARANCE_RENDER';
        console.log(report.nativeScope);
    } finally {
        current ??= await readCurrent(config, lab).catch(() => null);
        if (current?.live) { const stopped = await stopCurrent(config, lab); report.cleanup = {live: stopped.live, clean: stopped.evidenceShutdown?.clean}; assert.equal(stopped.live, false); assert.equal(stopped.evidenceShutdown?.clean, true); }
        if (runtime) { await runtime.ingestAvailable(); report.canonicalObservations = (await runtime.store.readObservations()).length; }
    }
} catch (error) {
    report.failures.push(error.message);
    if (error.stderr) await fs.writeFile(path.join(trial, 'tool-error.log'), error.stderr, {flag: 'wx'});
    console.log('FAIL ' + error.message.split('\n')[0]);
} finally {
    if (originalRows.length) { try { await verifyOriginal(); report.originalFilesVerified = originalRows.length; } catch (error) { report.failures.push(error.message); } }
    await write(path.join(trial, 'report.json'), report);
}
process.exitCode = report.failures.length ? 1 : 0;

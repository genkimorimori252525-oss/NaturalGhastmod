"""Read-only audit calculations; not Minecraft integration/game acceptance tests."""
import hashlib
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).parent


def orbit(direction, time, radius=24.0):
    return (math.cos(time * direction) * radius * 0.55,
            direction * math.sin(time * direction) * radius)


def motion_profile(target, adjustment, ticks=200):
    speed = distance = 0.0
    at18 = None
    for tick in range(1, ticks + 1):
        distance += speed
        speed = (1.0 - adjustment) * 0.95 * (speed + 0.1) + adjustment * target
        if tick == 18:
            at18 = distance
    return {'configured': target, 'steady_speed': speed,
            'formula_steady_speed': (0.095 * (1-adjustment) + adjustment*target) / (1-0.95*(1-adjustment)),
            'distance_at_tick_18': at18}


def snapshot_hash(path):
    data = path.read_bytes()
    if path.suffix.lower() not in {'.jar', '.png', '.ogg'}:
        data = data.replace(b'\r\n', b'\n')
    return hashlib.sha256(data).hexdigest()


def main():
    comparisons = [math.dist(orbit(1, t), orbit(-1, t)) for t in (0.0, .2, 1.1, 3.7, 8.0)]
    assert max(comparisons) < 1e-10
    cooldown = {'post_fire': -26, 'after_one_occluded_tick': max(0, -26-1),
                'normal_next_release_ticks': 26+18, 'after_occlusion_release_ticks': 1+18}
    assert cooldown['after_one_occluded_tick'] == 0
    profiles = {name: motion_profile(target, a) for name, target, a in (
        ('FAST_SMALL', 1.45, .18), ('HEAVY_SLOW', .55, .10),
        ('GROUND_FIRE', .85, .12), ('SPLITTER', .92, .10), ('WALL_BURST', .95, .10))}
    assert all(math.isclose(p['steady_speed'], p['formula_steady_speed'], abs_tol=1e-9) for p in profiles.values())
    assert profiles['HEAVY_SLOW']['steady_speed'] > .96
    sound_data = json.loads((ROOT/'src/main/resources/assets/soutou_ghast/sounds.json').read_text(encoding='utf8'))
    missing = []
    for definition in sound_data.values():
        for item in definition['sounds']:
            name = item if isinstance(item, str) else item['name']
            ns, resource = name.split(':', 1)
            if not (ROOT/'src/main/resources/assets'/ns/'sounds'/(resource+'.ogg')).is_file():
                missing.append(name)
    assert not missing
    before = json.loads((OUT/'source-manifest-v0.02.json').read_text(encoding='utf8'))
    changed = [r['path'] for r in before if snapshot_hash(ROOT/r['path']) != r['sha256']]
    assert not changed
    result = {'scope': 'Static algebra/scalar motion recurrence/resource resolution/hash checks; no Minecraft execution.',
              'orbit_plus_minus_max_difference': max(comparisons), 'cooldown': cooldown,
              'motion_profiles': profiles, 'missing_sound_files': missing,
              'sound_event_counts': {k: len(v['sounds']) for k, v in sound_data.items()},
              'lava_only_enrage_probability_per_tick': 1/8,
              'expected_lava_only_phase_switches_per_second_at_20tps': 20*2*(1/8)*(7/8),
              'snapshot_files_checked': len(before), 'snapshot_changed_files': changed}
    (OUT/'static-probe-results.json').write_text(json.dumps(result, indent=2), encoding='utf8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()

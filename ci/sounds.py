"""Physically-inspired marble-on-padded-wood sounds for BrainVita.

Each hit = short impact excitation -> (glass marble modes, very fast decay) + (wood plate modes, damped by
felt) + (low thud). A gentle low-pass stands in for the padding. Rendered at 44.1 kHz, 16-bit mono.
"""
import numpy as np, wave, sys, os
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(11)

def t(dur): return np.arange(int(SR * dur)) / SR

def mode(freq, decay_ms, amp, dur, drift=0.0):
    """One damped sinusoid; drift < 0 lets the pitch sag slightly as the energy dies (wood thud)."""
    x = t(dur)
    f = freq * (1 + drift * (1 - np.exp(-x * 1000 / decay_ms)))
    return amp * np.exp(-x * 1000 / decay_ms) * np.sin(2 * np.pi * np.cumsum(f) / SR)

def noise_burst(dur, decay_ms, amp, lo=None, hi=None):
    x = t(dur); n = rng.standard_normal(len(x)) * np.exp(-x * 1000 / decay_ms) * amp
    if lo or hi:
        b, a = butter(2, [lo or 20, hi or 20000], btype='band', fs=SR); n = lfilter(b, a, n)
    return n

def lowpass(x, cutoff, order=2):
    b, a = butter(order, cutoff, fs=SR); return lfilter(b, a, x)

def mix(*parts):
    n = max(len(p) for p in parts); out = np.zeros(n)
    for p in parts: out[:len(p)] += p
    return out

def delay(x, ms): return np.concatenate([np.zeros(int(SR * ms / 1000)), x])

def attack(x, ms=0.8):
    n = int(SR * ms / 1000); env = np.ones(len(x)); env[:n] = np.linspace(0, 1, n); return x * env

def marble_hit(weight=1.0, glass=1.0, dur=0.45, seed=0):
    """A heavy glass marble landing on felt-padded wood."""
    r = np.random.default_rng(seed)
    j = lambda: 1 + 0.03 * r.standard_normal()
    glass_modes = mix(*[mode(f * j(), d, a * glass, 0.06) for f, d, a in
                        [(2850, 4, 0.35), (4120, 3.5, 0.28), (5590, 3, 0.18), (7300, 2.5, 0.10)]])
    wood_modes = mix(*[mode(f * j(), d, a, dur) for f, d, a in
                       [(178, 70, 0.55), (236, 60, 0.45), (312, 48, 0.35), (431, 40, 0.25), (598, 30, 0.15)]])
    thud = mode(96 * j(), 85 * weight, 0.9 * weight, dur, drift=-0.12) + mode(64 * j(), 110 * weight, 0.5 * weight, dur, drift=-0.08)
    contact = noise_burst(0.02, 2.5, 0.5, 1500, 9000)
    hit = mix(glass_modes, wood_modes, thud, contact)
    hit = lowpass(hit, 5200)          # felt soaks up the highs
    return attack(hit, 0.6)

def lift(dur=0.09, amp=0.25, seed=1):
    """A marble lifted off felt: a soft brushed swish, no tap."""
    r = np.random.default_rng(seed)
    x = t(dur); env = np.sin(np.pi * x / dur) ** 1.5
    n = r.standard_normal(len(x)) * env * amp
    b, a = butter(2, [900, 4200], btype='band', fs=SR)
    return lfilter(b, a, n)

def roll(dur=0.12, amp=0.12, seed=2):
    """A short roll into a hole: granular rumble."""
    r = np.random.default_rng(seed)
    x = t(dur); env = np.sin(np.pi * x / dur)
    n = r.standard_normal(len(x)) * env * amp
    b, a = butter(2, [120, 900], btype='band', fs=SR)
    return lfilter(b, a, n)

def write(name, x, peak=0.85):
    x = x / max(1e-9, np.max(np.abs(x))) * peak
    # a gentle fade-out so nothing clicks at the end
    n = min(len(x), int(SR * 0.02)); x[-n:] *= np.linspace(1, 0, n)
    with wave.open(name, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype('<i2').tobytes())

out = sys.argv[1] if len(sys.argv) > 1 else '.'
os.makedirs(out, exist_ok=True)
P = lambda n: os.path.join(out, n)

# Landing after a jump: the main ASMR hit, with a tiny settle bounce.
write(P('jump.wav'), mix(marble_hit(1.0, 1.0, seed=3), delay(marble_hit(0.5, 0.6, dur=0.25, seed=4) * 0.22, 38), delay(roll(0.08, 0.035, 5), 50)), 0.9)
# The jumped marble lifted off the board, a beat after the landing.
write(P('capture.wav'), mix(lift(0.12, 0.3, 6), delay(mode(150, 60, 0.25, 0.3), 20)), 0.5)
# Picking a marble up.
write(P('pickup.wav'), lift(0.08, 0.3, 7), 0.45)
# Putting it back down without moving: a light hit.
write(P('drop.wav'), marble_hit(0.55, 0.7, dur=0.3, seed=8), 0.6)
# Undo: the marble slides back and settles, softer.
write(P('undo.wav'), mix(roll(0.14, 0.2, 9), delay(marble_hit(0.6, 0.5, dur=0.3, seed=10), 90)), 0.6)
# Illegal tap: a dull felt tap, no glass at all.
write(P('reject.wav'), mix(marble_hit(0.7, 0.0, dur=0.25, seed=11), delay(marble_hit(0.5, 0.0, dur=0.2, seed=12) * 0.7, 110)), 0.5)
# Win: a slow cascade of soft landings resolving into a warm wooden hum.
cascade = mix(*[delay(marble_hit(0.6 + 0.1 * i, 0.8, dur=0.35, seed=20 + i) * (0.5 + 0.1 * i), 170 * i) for i in range(4)])
hum = delay(mix(mode(178, 900, 0.5, 2.2), mode(267, 800, 0.3, 2.2), mode(356, 700, 0.2, 2.2)), 600)
write(P('win.wav'), mix(cascade, lowpass(hum, 1200)), 0.8)
# Stuck: one slow, heavy settle.
write(P('stuck.wav'), mix(marble_hit(1.3, 0.3, dur=0.6, seed=30), delay(mode(120, 500, 0.3, 1.2, drift=-0.15), 80)), 0.7)
print("wrote", sorted(os.listdir(out)))

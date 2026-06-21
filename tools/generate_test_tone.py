#!/usr/bin/env python3
"""Generate a 10-second 440 Hz sine wave stereo WAV file."""
import wave
import struct
import math

sample_rate = 48000
duration = 10.0
frequency = 440.0
amplitude = 0.5

num_samples = int(sample_rate * duration)

with wave.open("app/src/main/assets/test_tone.wav", "w") as f:
    f.setnchannels(2)
    f.setsampwidth(2)  # 16-bit
    f.setframerate(sample_rate)
    for i in range(num_samples):
        t = i / sample_rate
        value = int(amplitude * 32767.0 * math.sin(2.0 * math.pi * frequency * t))
        # Pack as stereo interleaved little-endian
        f.writeframes(struct.pack('<hh', value, value))

print("test_tone.wav generated.")
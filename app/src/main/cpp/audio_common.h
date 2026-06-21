#ifndef AUDIO_COMMON_H
#define AUDIO_COMMON_H

#include <cstdint>
#include <cmath>
#include <algorithm>

constexpr int kSampleRate = 48000;
constexpr int kChannels = 2;
constexpr float kPi = 3.14159265358979323846f;

inline float clamp(float v, float lo, float hi) {
    return std::max(lo, std::min(hi, v));
}

#endif
#include "orbit_panner.h"
#include "audio_common.h"

void OrbitPanner::setSpeed(float hz) {
    speed_ = clamp(hz, 0.05f, 0.8f);
}

void OrbitPanner::setDepth(float d) {
    depth_ = clamp(d, 0.0f, 1.0f);
}

void OrbitPanner::reset() {
    phase_ = 0.0f;
}

void OrbitPanner::process(float& left, float& right) {
    // Phase accumulator (avoid wrapping at very high samples)
    const float phaseIncrement = 2.0f * kPi * speed_ / kSampleRate;
    phase_ += phaseIncrement;
    if (phase_ > 2.0f * kPi) phase_ -= 2.0f * kPi;

    // Angle driven by orbit (circle)
    float angle = phase_;
    // If depth is zero, no panning -> both channels equal
    float panL = std::cos(angle);
    float panR = std::sin(angle);

    // Depth blend: output = original * (1 - depth) + panned * depth
    float origLeft = left;
    float origRight = right;

    float pannedLeft = origLeft * panL;
    float pannedRight = origRight * panR;

    left = origLeft * (1.0f - depth_) + pannedLeft * depth_;
    right = origRight * (1.0f - depth_) + pannedRight * depth_;
}
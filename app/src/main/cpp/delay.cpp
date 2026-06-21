#include "delay.h"
#include "audio_common.h"

void Delay::setMix(float m) {
    mix_ = clamp(m, 0.0f, 1.0f);
}

void Delay::init() {
    std::fill_n(buffer_, kMaxDelay, 0.0f);
    writeIdx_ = 0;
}

void Delay::process(float& left, float& right) {
    float input = (left + right) * 0.5f;
    int delaySamples = static_cast<int>(0.06f * kSampleRate); // 60ms
    delaySamples = std::min(delaySamples, kMaxDelay - 1);
    int readIdx = writeIdx_ - delaySamples;
    if (readIdx < 0) readIdx += kMaxDelay;
    float delayed = buffer_[readIdx];
    buffer_[writeIdx_] = input + delayed * 0.6f;
    writeIdx_ = (writeIdx_ + 1) % kMaxDelay;

    left = left * (1.0f - mix_) + delayed * mix_;
    right = right * (1.0f - mix_) + delayed * mix_;
}
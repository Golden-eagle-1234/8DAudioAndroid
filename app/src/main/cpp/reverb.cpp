#include "reverb.h"
#include "audio_common.h"

void Reverb::setMix(float m) {
    mix_ = clamp(m, 0.0f, 1.0f);
}

void Reverb::init() {
    std::fill_n(comb1_, kCombSize1, 0.0f);
    std::fill_n(comb2_, kCombSize2, 0.0f);
    std::fill_n(allpass_, kAllPassSize, 0.0f);
}

void Reverb::process(float& left, float& right) {
    float input = (left + right) * 0.5f;

    // Comb filters
    float combOut = 0.0f;
    comb1_[idx1_] = input + 0.7f * comb1_[idx1_];
    combOut += comb1_[idx1_];
    idx1_ = (idx1_ + 1) % kCombSize1;

    comb2_[idx2_] = input + 0.7f * comb2_[idx2_];
    combOut += comb2_[idx2_];
    idx2_ = (idx2_ + 1) % kCombSize2;

    combOut *= 0.5f;

    // All-pass
    float apIn = combOut;
    float apOut = -0.7f * apIn + allpass_[idxap_];
    allpass_[idxap_] = apIn + 0.7f * allpass_[idxap_];
    idxap_ = (idxap_ + 1) % kAllPassSize;

    float wet = apOut;
    left = left * (1.0f - mix_) + wet * mix_;
    right = right * (1.0f - mix_) + wet * mix_;
}
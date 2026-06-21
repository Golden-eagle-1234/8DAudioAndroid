#include "biquad.h"
#include <cmath>

void Biquad::setParams(float fs, float f0, float Q, float dBgain) {
    float A = std::pow(10.0f, dBgain / 40.0f);
    float w0 = 2.0f * M_PI * f0 / fs;
    float cosw = std::cos(w0);
    float sinw = std::sin(w0);
    float alpha = sinw / (2.0f * Q);

    float b0 = A * ((A + 1.0f) - (A - 1.0f) * cosw + 2.0f * std::sqrt(A) * alpha);
    float b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosw);
    float b2 = A * ((A + 1.0f) - (A - 1.0f) * cosw - 2.0f * std::sqrt(A) * alpha);
    float a0 = (A + 1.0f) + (A - 1.0f) * cosw + 2.0f * std::sqrt(A) * alpha;
    float a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosw);
    float a2 = (A + 1.0f) + (A - 1.0f) * cosw - 2.0f * std::sqrt(A) * alpha;

    // Normalize
    a0_ = b0 / a0;
    a1_ = b1 / a0;
    a2_ = b2 / a0;
    b1_ = a1 / a0;
    b2_ = a2 / a0;
}

void Biquad::reset() {
    x1_ = x2_ = y1_ = y2_ = 0.0f;
}

float Biquad::process(float sample) {
    float out = a0_ * sample + a1_ * x1_ + a2_ * x2_ - b1_ * y1_ - b2_ * y2_;
    x2_ = x1_;
    x1_ = sample;
    y2_ = y1_;
    y1_ = out;
    return out;
}
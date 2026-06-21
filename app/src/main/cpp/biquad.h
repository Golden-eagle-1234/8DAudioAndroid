#ifndef BIQUAD_H
#define BIQUAD_H

class Biquad {
public:
    // Low-shelf with given gain in dB, freq, Q
    void setParams(float sampleRate, float freq, float q, float gainDB);
    void reset();
    float process(float sample);

private:
    float a0_ = 1.0f, a1_ = 0.0f, a2_ = 0.0f;
    float b1_ = 0.0f, b2_ = 0.0f;
    float x1_ = 0.0f, x2_ = 0.0f;
    float y1_ = 0.0f, y2_ = 0.0f;
};

#endif
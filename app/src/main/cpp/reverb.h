#ifndef REVERB_H
#define REVERB_H

class Reverb {
public:
    void setMix(float mix);
    void init();
    void process(float& left, float& right);

private:
    float mix_ = 0.4f;
    // Simple comb/all-pass structures
    static constexpr int kCombSize1 = 1557;
    static constexpr int kCombSize2 = 1617;
    static constexpr int kAllPassSize = 359;
    float comb1_[kCombSize1] = {0};
    float comb2_[kCombSize2] = {0};
    float allpass_[kAllPassSize] = {0};
    int idx1_ = 0, idx2_ = 0, idxap_ = 0;
};

#endif
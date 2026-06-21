#ifndef DELAY_H
#define DELAY_H

class Delay {
public:
    void setMix(float mix);
    void init();
    void process(float& left, float& right);

private:
    float mix_ = 0.2f;
    static constexpr int kMaxDelay = 4800; // 100 ms @ 48k
    float buffer_[kMaxDelay] = {0};
    int writeIdx_ = 0;
};

#endif
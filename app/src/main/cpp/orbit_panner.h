#ifndef ORBIT_PANNER_H
#define ORBIT_PANNER_H

class OrbitPanner {
public:
    void setSpeed(float hz);
    void setDepth(float d);        // 0..1
    void reset();

    // Processes one stereo frame (left/right samples)
    void process(float& left, float& right);

private:
    float phase_ = 0.0f;
    float speed_ = 0.15f;
    float depth_ = 0.8f;
};

#endif
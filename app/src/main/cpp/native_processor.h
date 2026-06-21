#ifndef NATIVE_PROCESSOR_H
#define NATIVE_PROCESSOR_H

#include "orbit_panner.h"
#include "reverb.h"
#include "delay.h"
#include "biquad.h"
#include "audio_common.h"
#include <oboe/Oboe.h>

class NativeProcessor {
public:
    bool init(int sampleRate, int framesPerBurst);
    void destroy();

    void setOrbitSpeed(float hz);
    void setDepth(float depth);
    void setReverbMix(float mix);
    void setDelayMix(float mix);
    void setBassBoost(float db);

    // Called from Java when capture data arrives (interleaved short)
    void processCapture(const int16_t* input, int numFrames);

    // Called from Java for player mode: fill output ring with test tone
    void generateTestTone(int numFrames);

    // Start/stop Oboe stream
    bool startPlayback();
    void stopPlayback();

private:
    class AudioCallback : public oboe::AudioStreamCallback {
    public:
        explicit AudioCallback(NativeProcessor* parent) : parent_(parent) {}
        oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream,
                                              void* audioData,
                                              int32_t numFrames) override;
    private:
        NativeProcessor* parent_;
    };

    static constexpr int kMaxBufferFrames = 4096;
    float outputRing_[kMaxBufferFrames * kChannels];
    int writePos_ = 0;
    int readPos_ = 0;
    int availableFrames_ = 0;

    OrbitPanner panner_;
    Reverb reverb_;
    Delay delay_;
    Biquad bassBoostL_, bassBoostR_;

    int sampleRate_ = 48000;
    int framesPerBurst_ = 1024;

    oboe::ManagedStream stream_;
    std::unique_ptr<AudioCallback> callback_;
    std::mutex mutex_;

    void processFrame(float& left, float& right);
    void writeToRing(const float* stereoFrames, int numFrames);
    int readFromRing(float* stereoFrames, int numFrames);
};

#endif
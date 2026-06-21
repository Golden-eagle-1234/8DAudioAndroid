#include "native_processor.h"
#include <android/log.h>
#include <cstring>
#include <algorithm>

#define TAG "NativeProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

NativeProcessor* g_processor = nullptr;

// ----------- Oboe Callback -----------
oboe::DataCallbackResult NativeProcessor::AudioCallback::onAudioReady(oboe::AudioStream*, void* audioData, int32_t numFrames) {
    if (!parent_) return oboe::DataCallbackResult::Stop;
    float* out = static_cast<float*>(audioData);
    int framesRead = parent_->readFromRing(out, numFrames);
    if (framesRead < numFrames) {
        // underrun: zero out remaining
        std::fill(out + framesRead * kChannels, out + numFrames * kChannels, 0.0f);
    }
    return oboe::DataCallbackResult::Continue;
}

// ----------- Init / Destroy -----------
bool NativeProcessor::init(int sampleRate, int framesPerBurst) {
    sampleRate_ = sampleRate;
    framesPerBurst_ = framesPerBurst;

    // Reset DSP
    panner_.reset();
    reverb_.init();
    delay_.init();
    bassBoostL_.setParams(sampleRate_, 200.0f, 0.7f, 4.0f); // default 4 dB
    bassBoostR_.setParams(sampleRate_, 200.0f, 0.7f, 4.0f);
    bassBoostL_.reset();
    bassBoostR_.reset();

    // Clear ring buffer
    std::fill(std::begin(outputRing_), std::end(outputRing_), 0.0f);
    writePos_ = readPos_ = availableFrames_ = 0;

    // Build Oboe stream
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(kChannels)
           ->setSampleRate(sampleRate_)
           ->setFramesPerCallback(framesPerBurst_)
           ->setCallback(callback_.get());

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
        return false;
    }
    LOGD("Oboe stream opened successfully.");
    return true;
}

void NativeProcessor::destroy() {
    stopPlayback();
    stream_.close();
    g_processor = nullptr;
}

void NativeProcessor::setOrbitSpeed(float hz) { panner_.setSpeed(hz); }
void NativeProcessor::setDepth(float d) { panner_.setDepth(d); }
void NativeProcessor::setReverbMix(float m) { reverb_.setMix(m); }
void NativeProcessor::setDelayMix(float m) { delay_.setMix(m); }
void NativeProcessor::setBassBoost(float db) {
    bassBoostL_.setParams(sampleRate_, 200.0f, 0.7f, db);
    bassBoostR_.setParams(sampleRate_, 200.0f, 0.7f, db);
}

// ----------- Audio Processing -----------
void NativeProcessor::processFrame(float& left, float& right) {
    // Bass boost (mono-mids but per channel)
    left = bassBoostL_.process(left);
    right = bassBoostR_.process(right);

    // Orbit panner (creates stereo movement)
    panner_.process(left, right);

    // Reverb
    reverb_.process(left, right);

    // Delay
    delay_.process(left, right);

    // Soft clip
    left = clamp(left, -1.0f, 1.0f);
    right = clamp(right, -1.0f, 1.0f);
}

// Called from JNI with short buffer (interleaved)
void NativeProcessor::processCapture(const int16_t* input, int numFrames) {
    if (!stream_ || stream_->getState() != oboe::StreamState::Started) return;

    // Convert short -> float and process
    float stereoOut[kMaxBufferFrames * kChannels];
    int outFrames = std::min(numFrames, kMaxBufferFrames);
    for (int i = 0; i < outFrames; ++i) {
        float left = input[i * kChannels] / 32768.0f;
        float right = input[i * kChannels + 1] / 32768.0f;
        processFrame(left, right);
        stereoOut[i * kChannels] = left;
        stereoOut[i * kChannels + 1] = right;
    }
    writeToRing(stereoOut, outFrames);
}

// Player mode: generate sine wave at 440Hz
void NativeProcessor::generateTestTone(int numFrames) {
    if (!stream_ || stream_->getState() != oboe::StreamState::Started) return;
    float stereoOut[kMaxBufferFrames * kChannels];
    static float tonePhase = 0.0f;
    float freq = 440.0f;
    float increment = 2.0f * kPi * freq / sampleRate_;
    int genFrames = std::min(numFrames, kMaxBufferFrames);
    for (int i = 0; i < genFrames; ++i) {
        float sample = std::sin(tonePhase) * 0.5f;
        tonePhase += increment;
        if (tonePhase > 2.0f * kPi) tonePhase -= 2.0f * kPi;
        float left = sample, right = sample;
        processFrame(left, right);
        stereoOut[i * kChannels] = left;
        stereoOut[i * kChannels + 1] = right;
    }
    writeToRing(stereoOut, genFrames);
}

bool NativeProcessor::startPlayback() {
    if (!stream_) return false;
    oboe::Result r = stream_->requestStart();
    return r == oboe::Result::OK;
}

void NativeProcessor::stopPlayback() {
    if (stream_) stream_->requestStop();
}

// ----------- Ring Buffer (lock‑free? simple mutex) -----------
void NativeProcessor::writeToRing(const float* stereoFrames, int numFrames) {
    std::lock_guard<std::mutex> lock(mutex_);
    int capacity = kMaxBufferFrames;
    for (int i = 0; i < numFrames; ++i) {
        if (availableFrames_ >= capacity) return; // drop
        outputRing_[writePos_ * kChannels] = stereoFrames[i * kChannels];
        outputRing_[writePos_ * kChannels + 1] = stereoFrames[i * kChannels + 1];
        writePos_ = (writePos_ + 1) % capacity;
        ++availableFrames_;
    }
}

int NativeProcessor::readFromRing(float* stereoFrames, int numFrames) {
    std::lock_guard<std::mutex> lock(mutex_);
    int read = 0;
    int capacity = kMaxBufferFrames;
    while (read < numFrames && availableFrames_ > 0) {
        stereoFrames[read * kChannels] = outputRing_[readPos_ * kChannels];
        stereoFrames[read * kChannels + 1] = outputRing_[readPos_ * kChannels + 1];
        readPos_ = (readPos_ + 1) % capacity;
        --availableFrames_;
        ++read;
    }
    return read;
}
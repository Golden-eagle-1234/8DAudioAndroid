#include <jni.h>
#include <sys/system_properties.h>
#include <zygisk.hpp>

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

class PirateModule : public zygisk::ModuleBase {
public:
    void onLoad(Api* api, JNIEnv* env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(AppSpecializeArgs* args) override {
        // Use env to call original methods or prepare hooks.
        // If the process is audioflinger or mediaserver, inject.
        const char* process = env->GetStringUTFChars(args->nice_name, nullptr);
        if (process && (strstr(process, "audio") || strstr(process, "media"))) {
            // Request to inject.
            api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
        }
        env->ReleaseStringUTFChars(args->nice_name, process);
    }

    void postAppSpecialize(const AppSpecializeArgs*) override {
        // Here we can hook audio-related functions using Dobby or xDL.
        // Placeholder for future expansion.
    }

private:
    Api* api = nullptr;
    JNIEnv* env = nullptr;
};

REGISTER_ZYGISK_MODULE(PirateModule)
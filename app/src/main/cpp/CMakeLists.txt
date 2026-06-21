cmake_minimum_required(VERSION 3.22.1)
project("audio8d")

add_library(audio8d SHARED
    jni_bridge.cpp
    native_processor.cpp
    orbit_panner.cpp
    reverb.cpp
    delay.cpp
    biquad.cpp
)

find_library(log-lib log)
find_library(oboe oboe)

target_include_directories(audio8d PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(audio8d
    ${log-lib}
    ${oboe}
    android
)
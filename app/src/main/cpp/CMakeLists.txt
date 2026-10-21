cmake_minimum_required(VERSION 3.22.1)
project(klang_portal_native LANGUAGES C CXX)

option(KLANG_ENABLE_PROJECTM "Enable a separately supplied projectM source tree" OFF)
option(KLANG_ENABLE_SPATIAL_DSP "Enable reviewed spatial DSP integration" OFF)
add_library(klang_portal_native SHARED portal_native.cpp)

if(KLANG_ENABLE_PROJECTM)
    message(FATAL_ERROR "Complete and pin the version-specific projectM bridge before enabling.")
endif()
if(KLANG_ENABLE_SPATIAL_DSP)
    message(FATAL_ERROR "Pin a license-reviewed spatial DSP implementation before enabling.")
endif()

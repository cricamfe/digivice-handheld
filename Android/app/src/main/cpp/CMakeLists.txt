cmake_minimum_required(VERSION 3.22)
project(digivice_chd LANGUAGES C CXX)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CHDR_WANT_TESTS OFF CACHE BOOL "Build libchdr's upstream tests")
add_subdirectory(libchdr EXCLUDE_FROM_ALL)

add_library(digivice_chd SHARED chd_jni.cpp)
target_compile_features(digivice_chd PRIVATE cxx_std_17)
target_compile_options(digivice_chd PRIVATE -Wall -Wextra)
target_link_libraries(digivice_chd PRIVATE chdr-static)

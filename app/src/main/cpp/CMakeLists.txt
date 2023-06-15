# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)
#添加内联支持
enable_language(C ASM)
# Declares and names the project.
#开始优化
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O")
# inline
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility-inlines-hidden")
project("testipc1")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
# 添加原始的添加库代码块
add_library(native-lib SHARED native-lib.cpp syscall64.s)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
# 添加main.c和syscall64.s文件
add_executable( native-lib.cpp syscall64.s)
include_directories(
        ./
)
if (${CMAKE_ANDROID_ARCH_ABI} STREQUAL "arm64-v8a")
    set(syscall /syscall64.s)
elseif (${CMAKE_ANDROID_ARCH_ABI} STREQUAL "armeabi-v7a")
    set(syscall /syscall32.s)
endif ()
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
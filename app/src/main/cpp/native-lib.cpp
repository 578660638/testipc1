#include <jni.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdlib.h>
#include <syscall.h>
#include "syscall.h"
#include <asm/fcntl.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <android/log.h>
#include <android/log.h>
#define __NR_open 5

extern "C" long raw_syscall(long sysno, long arg0, long arg1, long arg2, long arg3, long arg4);
ssize_t read_write_file(const char *src_path, const char *dst_path) {


    int src_fd = raw_syscall(__NR_open, (long) src_path, O_RDONLY, 0, 0, 0);
    __android_log_print(4, "Tag", "开始%d",src_fd);
    if (src_fd < 0) {
        return src_fd;
    }

    int dst_fd = raw_syscall(__NR_open, (long) dst_path, O_WRONLY | O_CREAT | O_TRUNC,
                             S_IRUSR | S_IWUSR, 0, 0);
    if (dst_fd < 0) {
        raw_syscall(__NR_close, src_fd, 0, 0, 0, 0);
        return dst_fd;
    }

    char buffer[4096];
    ssize_t total_bytes = 0;

    while (1) {
        ssize_t bytes_read = raw_syscall(__NR_read, src_fd, (long) buffer, sizeof(buffer), 0, 0);
        if (bytes_read <= 0) {
            break;
        }

        ssize_t bytes_written = raw_syscall(__NR_write, dst_fd, (long) buffer, bytes_read, 0, 0);
        if (bytes_written < 0) {
            total_bytes = bytes_written;
            break;
        }

        total_bytes += bytes_written;
    }

    raw_syscall(__NR_close, src_fd, 0, 0, 0, 0);
    raw_syscall(__NR_close, dst_fd, 0, 0, 0, 0);

    return total_bytes;
}


void print_file_content(const char *file_path) {
    int fd = raw_syscall(__NR_openat, AT_FDCWD, (long)file_path, O_RDONLY, 0, 0);

    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "Failed to open file: %s", file_path);
        return;
    }

    char buffer[1024];
    ssize_t bytes_read;

    while ((bytes_read = raw_syscall(__NR_read, fd, (long)buffer, sizeof(buffer), 0, 0)) > 0) {
        __android_log_print(ANDROID_LOG_INFO, "MyTag", "%.*s", (int)bytes_read, buffer);
    }

    if (bytes_read < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "Error reading file: %s", file_path);
    }

    raw_syscall(__NR_close, fd, 0, 0, 0, 0);
}
void write_hello_world(const char *file_path) {
    int fd = raw_syscall(__NR_openat, AT_FDCWD, (long)file_path, O_WRONLY | O_CREAT | O_TRUNC, 0644,  0);

    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "Failed to open file for writing: %s", file_path);
        return;
    }

    const char *message = "Hello, World!";
    size_t message_len = strlen(message);

    ssize_t bytes_written = raw_syscall(__NR_write, fd, (long)message, message_len, 0, 0);

    if (bytes_written < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "Error writing to file: %s", file_path);
    } else {
        __android_log_print(ANDROID_LOG_INFO, "MyTag", "Successfully written to file: %s", file_path);
    }

    raw_syscall(__NR_close, fd, 0, 0, 0, 0);
}
int main() {
    const char *src_path = "/sdcard/test.txt";
    const char *dst_path = "/sdcard/test1.txt";

   print_file_content(src_path);
    write_hello_world(dst_path);

    return 0;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_yyf_testipc1_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    int a1 = main();

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
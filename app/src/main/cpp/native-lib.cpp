#include <jni.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
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
#include <sys/prctl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/ptrace.h>
#include <android/log.h>
#include <sys/wait.h>
#include <sys/user.h>
#include <sys/uio.h> // 包含这个头文件以使用struct iovec
#include <stdio.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <android/log.h>
#include <elf.h>
#include <link.h>
#define APPNAME "AntiDebugExample"
auto mainProcessPid = getpid();
//附件自己的进程
int fibonacci(int n) {
    if (n <= 1) {
        return n;
    }

    return fibonacci(n - 1) + fibonacci(n - 2);
}

int antidebug() {
    pid_t pid = fork();
    if (pid == 0) {
        // 子进程
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "子进程创建成功, PID: %d", getpid());
        if (ptrace(PTRACE_ATTACH, getppid(), NULL, NULL) == -1) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "ptrace error: %s", strerror(errno));
            return 1;
        }
        // 等待父进程停止
        waitpid(getppid(), NULL, 0);
        // 子进程循环 ptrace 父进程
        while (1) {
            if (ptrace(PTRACE_CONT, getppid(), NULL, NULL) == -1) {
                __android_log_print(ANDROID_LOG_ERROR, APPNAME, "ptrace CONT error: %s", strerror(errno));
                break;
            }
            waitpid(getppid(), NULL, 0);
        }
        // 子进程结束，退出
        _exit(0);

    } else if (pid > 0) {
        // 父进程

        int result = fibonacci(10);
        __android_log_print(ANDROID_LOG_INFO, APPNAME, "父进程计算结果: %d", result);
        int status;
        // 等待子进程状态变化
        waitpid(pid, &status, WNOHANG);
        if (WIFEXITED(status)) {
            __android_log_print(ANDROID_LOG_INFO, APPNAME, "子进程已正常结束, 退出状态: %d", WEXITSTATUS(status));
        } else if (WIFSIGNALED(status)) {
            __android_log_print(ANDROID_LOG_INFO, APPNAME, "子进程被信号终止, 信号: %d", WTERMSIG(status));
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "fork error: %s", strerror(errno));
        return 1;
    }

    return 0;
}
struct Tracer {
    pid_t pid;
    bool wait_sigcont;
};

Tracer *get_tracer(Tracer *tracer, pid_t pid, bool wait_sigcont) {
    if (tracer == nullptr) {
        tracer = new Tracer();
    }
    tracer->pid = pid;
    tracer->wait_sigcont = wait_sigcont;
    return tracer;
}

void handle_svc_instruction(Tracer *tracer) {
    // 你的调试线程逻辑
    // 在这个函数中，你可以根据需要实现对被调试线程的操作
    // 例如，检查和修改内存、寄存器值，设置断点等

    int svc_number = 0;
#ifdef __arm__
    // ARM架构
    struct user_regs_struct regs;
    ptrace(PTRACE_GETREGS, tracer->pid, NULL, &regs);
    svc_number = regs.ARM_ORIG_r0;
#elif defined(__aarch64__)
    // ARM64架构 (AArch64)
    struct user_pt_regs regs;
    struct iovec iov;
    iov.iov_base = &regs;
    iov.iov_len = sizeof(regs);
    ptrace(PTRACE_GETREGSET, tracer->pid, NT_PRSTATUS, &iov);
    __android_log_print(ANDROID_LOG_DEBUG, "MyTag", "Detected svc instruction with number1: %d", svc_number);
    svc_number = regs.regs[8];
#elif defined(__i386__)
    // x86架构
    struct user_regs_struct regs;
    ptrace(PTRACE_GETREGS, tracer->pid, NULL, &regs);
    svc_number = regs.orig_eax;
#endif

    __android_log_print(ANDROID_LOG_DEBUG, "MyTag", "Detected svc instruction with number: %d", svc_number);
}

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

int event_loop() {
    Tracer *tracer = get_tracer(nullptr, mainProcessPid, true);
    while (true) {
        int status;
        pid_t pid = waitpid(tracer->pid, &status, __WALL);
        if (pid == tracer->pid && WIFSTOPPED(status)) {
            if ((status >> 8) == (SIGTRAP | 0x80)) {
                handle_svc_instruction(tracer);
            }
            ptrace(PTRACE_SYSCALL, tracer->pid, NULL, NULL);
        }
    }
    return 0;
}
void enable_syscall_filtering(Tracer *tracer) {
    // 这里仅作为示例，你需要根据实际需求实现系统调用过滤
    if (tracer != nullptr) {
        // 根据tracer的状态启用或配置系统调用过滤
    }
}

int trace_current_process(int sdkVersion) {
    __android_log_print(ANDROID_LOG_ERROR, "MyTag", "start trace_current_process ");
    prctl(PR_SET_DUMPABLE, 1, 0, 0, 0);

    pid_t child = fork();
    if (child < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "ptrace svc fork() error ");
        return -errno;
    }
    //init first tracer
    Tracer *first = get_tracer(nullptr, mainProcessPid, true);

    if (child == 0) {


        int status = ptrace(PTRACE_ATTACH, mainProcessPid, NULL, NULL);
        if (status != 0) {
            //attch失败
            __android_log_print(ANDROID_LOG_ERROR, "MyTag", ">>>>>>>>> error: attach target process %d ", status);
            return -errno;
        }
        first->wait_sigcont = true;
        //开始执行死循环代码,因为处于一直监听状态,理论上该进程不会退出
        exit(event_loop());
    } else {
        //init seccomp by main process
        //the seccomp filtering rule is intended only for the current process
        __android_log_print(ANDROID_LOG_ERROR, "MyTag", "else >=0 ");
        enable_syscall_filtering(first);
    }
    return 0;
}




extern "C" JNIEXPORT jstring JNICALL
Java_com_yyf_testipc1_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
   // int a3 =  antidebug();
    int a1 = main();
    int a2  =trace_current_process(2);
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
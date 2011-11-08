#include "com_couchbase_android_ErlangThread.h"

#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>

#include "android/log.h"

#include "erl_printf.h"

#define LOG_TAG "ErlangThread"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define ALLOC(X) malloc(X)
#define REALLOC(X,Y) realloc(X,Y)
#define FREE(X) free(X)

#define ANDROID_VPRINTF_BUF_INC_SIZE 1024

int (*ref_erts_vdsprintf)(erts_dsprintf_buf_t *dsbufp, const char *format, va_list arglist) = NULL;

static erts_dsprintf_buf_t * grow_android_vprintf_buf(erts_dsprintf_buf_t *dsbufp, size_t need)
{
    char *buf;
    size_t size;

    if (!dsbufp->str) {
        size = (((need + ANDROID_VPRINTF_BUF_INC_SIZE - 1)
                 / ANDROID_VPRINTF_BUF_INC_SIZE)
                * ANDROID_VPRINTF_BUF_INC_SIZE);
        buf = (char *) ALLOC(size * sizeof(char));
    }
    else {
        size_t free_size = dsbufp->size - dsbufp->str_len;

        if (need <= free_size)
            return dsbufp;

        size = need - free_size + ANDROID_VPRINTF_BUF_INC_SIZE;
        size = (((size + ANDROID_VPRINTF_BUF_INC_SIZE - 1)
                 / ANDROID_VPRINTF_BUF_INC_SIZE)
                * ANDROID_VPRINTF_BUF_INC_SIZE);
        size += dsbufp->size;
        buf = (char *) REALLOC((void *) dsbufp->str,
                               size * sizeof(char));
    }
    if (!buf)
        return NULL;
    if (buf != dsbufp->str)
        dsbufp->str = buf;
    dsbufp->size = size;
    return dsbufp;
}

static int android_stdout_vprintf(char *format, va_list arg_list) {
    int res,i;
    erts_dsprintf_buf_t dsbuf = ERTS_DSPRINTF_BUF_INITER(grow_android_vprintf_buf);
    res = ref_erts_vdsprintf(&dsbuf, format, arg_list);
    if (res >= 0) {
	char *tmp = (char*)ALLOC((dsbuf.str_len + 1)*sizeof(char));
	for (i=0;i<dsbuf.str_len;++i) {
	    tmp[i] = dsbuf.str[i];
	}
        tmp[dsbuf.str_len] = NULL;
        LOGV(tmp);
	FREE(tmp);
    }
    if (dsbuf.str)
      FREE((void *) dsbuf.str);
    return res;
}

static int android_stderr_vprintf(char *format, va_list arg_list) {
    int res,i;
    erts_dsprintf_buf_t dsbuf = ERTS_DSPRINTF_BUF_INITER(grow_android_vprintf_buf);
    res = ref_erts_vdsprintf(&dsbuf, format, arg_list);
    if (res >= 0) {
        char *tmp = (char*)ALLOC((dsbuf.str_len + 1)*sizeof(char));
        for (i=0;i<dsbuf.str_len;++i) {
            tmp[i] = dsbuf.str[i];
        }
        tmp[dsbuf.str_len] = NULL;
        LOGE(tmp);
        FREE(tmp);
    }
    if (dsbuf.str)
      FREE((void *) dsbuf.str);
    return res;
}

JavaVM *jvm;
jclass ErlangThread;
jmethodID ErlMessageMethod;

JNIEXPORT void JNICALL Java_com_couchbase_android_ErlangThread_start_1erlang
            (JNIEnv *env, jclass cls, jstring j_bindir, jstring j_sopath, jobjectArray j_args) {
    jboolean iscopy;
    int i, argc;
    char ** argv;
    void (*erl_start)(int, char**) = NULL;
    int (**erts_printf_stdout_func)(char *, va_list) = NULL;
    int (**erts_printf_stderr_func)(char *, va_list) = NULL;

    const char *sopath = env->GetStringUTFChars(j_sopath, &iscopy);
    const char *bindir = env->GetStringUTFChars(j_bindir, &iscopy);

    void* handle = dlopen(sopath, RTLD_LAZY);

    if(!handle) {
        LOGE("Failed to open beam .so: %s", dlerror());
        goto cleanup;
    }
    *(void **)(&erl_start) = dlsym(handle, "erl_start");
    if(!erl_start) {
        LOGE("Failed to find erl_start: %s", dlerror());
        goto cleanup;
    }

    *(void **)(&erts_printf_stdout_func) = dlsym(handle, "erts_printf_stdout_func");
    if(!erts_printf_stdout_func) {
        LOGE("Failed to find erts_printf_stdout_func: %s", dlerror());
        goto cleanup;
    }
    else {
        *erts_printf_stdout_func = android_stdout_vprintf;
    }

    *(void **)(&erts_printf_stderr_func) = dlsym(handle, "erts_printf_stderr_func");
    if(!erts_printf_stderr_func) {
        LOGE("Failed to find erts_printf_stderr_func: %s", dlerror());
        goto cleanup;
    }
    else {
        *erts_printf_stderr_func = android_stderr_vprintf;
    }

    *(void **)(&ref_erts_vdsprintf) = dlsym(handle, "erts_vdsprintf");
    if(!erts_vdsprintf) {
        LOGE("Failed to find erts_vdsprintf: %s", dlerror());
        goto cleanup;
    }

    argc = env->GetArrayLength(j_args);
    argv = (char**) malloc(sizeof(char*) * argc);

    for(i = 0; i < argc; i++)
    {
        argv[i] = (char*) env->GetStringUTFChars((jstring) env->GetObjectArrayElement(j_args, i), &iscopy);
    }
    setenv("BINDIR", bindir, 1);
    env->GetJavaVM(&jvm);
    ErlMessageMethod = env->GetStaticMethodID(cls, "erl_message", "(Ljava/lang/String;[BJ)V");
    ErlangThread = (jclass) env->NewGlobalRef(cls);
    erl_start(argc, argv);

    cleanup:
    env->DeleteGlobalRef(ErlangThread);
    env->ReleaseStringUTFChars(j_sopath, sopath);
    env->ReleaseStringUTFChars(j_bindir, bindir);
    if(handle) dlclose(handle);
    if(argv)
    {
        for(i = 0; i < argc; i++)
        {
            env->ReleaseStringUTFChars((jstring) env->GetObjectArrayElement(j_args, i), argv[i]);
        }
        free(argv);
    }
    return;
}

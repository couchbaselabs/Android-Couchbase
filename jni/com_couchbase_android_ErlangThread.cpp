#include "com_couchbase_android_ErlangThread.h"

#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>

#include "android/log.h"

#define LOG_TAG "ErlangThread"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JavaVM *jvm;
jclass ErlangThread;
jmethodID ErlMessageMethod;

JNIEXPORT void JNICALL Java_com_couchbase_android_ErlangThread_start_1erlang
            (JNIEnv *env, jclass cls, jstring j_bindir, jstring j_sopath, jobjectArray j_args) {
    jboolean iscopy;
    int i, argc;
    char ** argv;
    void (*erl_start)(int, char**) = NULL;

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

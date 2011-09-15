#include "android/log.h"
#include "erl_nif.h"
#include <string.h>
#include <jni.h>

#include "com_couchbase_android_ErlangThread.h"

#define LOG_TAG "JNINIF"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern JavaVM *jvm;
extern jclass ErlangThread;
extern jmethodID ErlMessageMethod;

struct caller_info {
    ErlNifPid pid;
    ErlNifEnv *env;
};

JNIEXPORT void JNICALL Java_com_couchbase_android_ErlangThread_send_1bin
  (JNIEnv *env, jclass cls, jbyteArray jbin, jlong cinfo)  {
    struct caller_info* inf = (struct caller_info*) cinfo;
    ErlNifEnv* msg = enif_alloc_env();
    ERL_NIF_TERM bin_term;
    jsize length = env->GetArrayLength(jbin);
    jbyte* bindata = (jbyte*) enif_make_new_binary(msg, length, &bin_term);
    env->GetByteArrayRegion(jbin, 0, length, bindata);
    enif_send(inf->env, &(inf->pid), msg, bin_term);
    enif_free_env(msg);
}

static ERL_NIF_TERM mkatom(ErlNifEnv* env, const char* name) {
    ERL_NIF_TERM atom;
    if(enif_make_existing_atom(env, name, &atom, ERL_NIF_LATIN1)) {
        return atom;
    } else {
        return enif_make_atom(env, name);
    }
}

static ERL_NIF_TERM jninif_send(ErlNifEnv* nif_env, int argc, const ERL_NIF_TERM argv[])
{
    struct caller_info cinf;
    JNIEnv *env = NULL;
    char namebuf[512];

    cinf.env = nif_env;
    enif_self(nif_env, &(cinf.pid));

    if(!enif_get_atom(nif_env, argv[0], namebuf, 512, ERL_NIF_LATIN1)) {
        return mkatom(nif_env, "name_too_long");
    }

    jvm->AttachCurrentThread(&env, NULL);
    if(env == NULL) {
        return mkatom(nif_env, "jni_error");
    }

    env->PushLocalFrame(2); //We use 2 local JNI refs per invoc.
    ErlNifBinary ebin;
    jbyteArray jbin;
    jstring name = env->NewStringUTF(namebuf);
    enif_inspect_binary(nif_env, argv[1], &ebin);
    jbin = env->NewByteArray(ebin.size);
    env->SetByteArrayRegion(jbin, 0, ebin.size, (jbyte*) ebin.data);
    env->CallStaticVoidMethod(ErlangThread, ErlMessageMethod, name, jbin, (jlong) &cinf);
    env->PopLocalFrame(NULL);
    return mkatom(nif_env, "ok");
}

static ErlNifFunc jninif_funcs[] =
{
    {"send", 2, jninif_send}
};

ERL_NIF_INIT(jninif, jninif_funcs, NULL, NULL, NULL, NULL);

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := com_couchbase_android_ErlangThread
LOCAL_SRC_FILES := com_couchbase_android_ErlangThread.cpp android_jni_nif.cpp
LOCAL_LDLIBS    := -llog -ldl -L$(LOCAL_PATH) -L$(ERL_HOME)/bin/arm-unknown-eabi/ -lbeam

include $(BUILD_SHARED_LIBRARY)

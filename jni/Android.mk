LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)

########################################################
## native
########################################################

include $(CLEAR_VARS)
LOCAL_MODULE    := uv-prebuilt
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libuv.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := sodium-prebuilt
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libsodium.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := xTun-prebuilt
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libxTun.a
include $(PREBUILT_STATIC_LIBRARY)

########################################################
## xTun jni
########################################################

include $(CLEAR_VARS)
LOCAL_MODULE:= xTun
LOCAL_SRC_FILES:= xTun.c local_ns_parser.c dns.c
LOCAL_CFLAGS := -IxTun/src
LOCAL_LDFLAGS += -fPIC
LOCAL_LDLIBS += -llog
LOCAL_STATIC_LIBRARIES := xTun-prebuilt uv-prebuilt sodium-prebuilt
include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := terminal
LOCAL_SRC_FILES := terminal.c

include $(BUILD_SHARED_LIBRARY)
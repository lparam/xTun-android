SHELL := /bin/bash

LIB_SYSTEM=app/src/main/jniLibs/armeabi/libxTun.so

all: $(LIB_SYSTEM)

.PHONY: clean

clean:
	rm -rf libs
	rm -rf app/src/main/jniLibs
	$(ANDROID_NDK_HOME)/ndk-build clean

distclean: clean
	rm -rf jni/xTun/xTun-android-i686
	rm -rf jni/xTun/xTun-android-armv7-a

$(LIB_SYSTEM): jni/xTun.c jni/Android.mk
	if [ a == a$(ANDROID_NDK_HOME) ]; then \
		echo ANDROID_NDK_HOME is not set ;\
		exit 1 ;\
	fi ;\
	pushd jni/xTun || exit 1 ;\
	dist-build/android-x86.sh || exit 1 ;\
	dist-build/android-armv7-a.sh || exit 1 ;\
	popd ;\
	pushd jni ;\
	install -d x86 ;\
	install -d armeabi-v7a ;\
	install xTun/xTun-android-i686/3rd/libsodium/src/libsodium/.libs/libsodium.a x86 ;\
	install xTun/xTun-android-i686/3rd/libuv/.libs/libuv.a x86 ;\
	install xTun/xTun-android-i686/libxTun.a x86 ;\
	install xTun/xTun-android-armv7-a/3rd/libsodium/src/libsodium/.libs/libsodium.a armeabi-v7a ;\
	install xTun/xTun-android-armv7-a/3rd/libuv/.libs/libuv.a armeabi-v7a ;\
	install xTun/xTun-android-armv7-a/libxTun.a armeabi-v7a ;\
	$(ANDROID_NDK_HOME)/ndk-build NDK_LOG=1 V=1 || exit 1 ;\
	popd ;\
	rm -rf app/src/main/assets/x86 ;\
	rm -rf app/src/main/assets/armeabi-v7a ;\
	mkdir -p app/src/main/assets/x86 ;\
	mkdir -p app/src/main/assets/armeabi-v7a ;\
	install -d app/src/main/jniLibs/x86 ;\
	install -d app/src/main/jniLibs/armeabi-v7a ;\
	install libs/x86/libxTun.so app/src/main/jniLibs/x86 ;\
	install libs/armeabi-v7a/libxTun.so app/src/main/jniLibs/armeabi-v7a ;

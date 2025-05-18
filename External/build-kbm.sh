REPO="https://mirrors.saswata.cc/termux/termux-main/pool/main"
ARCHS=("aarch64" "arm" "i686" "x86_64")
PROOT="proot_5.1.107-65_"
LIBTALLOC="libtalloc_2.4.2_"
TMP="tmp"
OUTPUT="output"
PREFIX="data/data/com.termux/files/usr"


pkg i wget patchelf -y
rm -rf $OUTPUT
mkdir -p $TMP $OUTPUT
for arch in "${ARCHS[@]}"; do
 mkdir "${OUTPUT}/${arch}"
 wget "${REPO}/p/proot/${PROOT}${arch}.deb" -P $TMP
 dpkg-deb -x "${TMP}/${PROOT}${arch}.deb" $TMP
 mv "${TMP}/${PREFIX}/bin/proot" "${OUTPUT}/${arch}/libkaboot.so"
 patchelf --replace-needed "libtalloc.so.2" "libkabootoc.so" "${OUTPUT}/${arch}/libkaboot.so"
 mv "${TMP}/${PREFIX}/libexec/proot/loader" "${OUTPUT}/${arch}/libkabooter.so"
 mv "${TMP}/${PREFIX}/libexec/proot/loader32" "${OUTPUT}/${arch}/libkabooter32.so"

 wget "${REPO}/libt/libtalloc/${LIBTALLOC}${arch}.deb" -P $TMP
 dpkg-deb -x "${TMP}/${LIBTALLOC}${arch}.deb" $TMP 
 mv "${TMP}/${PREFIX}/lib/libtalloc.so.2.4.2" "${OUTPUT}/${arch}/libkbm.so"
done
rm -rf $TMP
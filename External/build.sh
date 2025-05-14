#!/bin/bash

repo="https://mirrors.saswata.cc/termux/termux-main/pool/main/p/proot"
repot="https://mirrors.saswata.cc/termux/termux-main/pool/main/libt/libtalloc"

file="proot_5.1.107-65_"
filet="libtalloc_2.4.2_"

aarch=("aarch64" "arm" "i686" "x86_64")
prefix="data/data/com.termux/files/usr"

tmpdir="tmp"
outputdir="output"

pkg i wget patchelf -y

rm -rf "$tmpdir" "$outputdir"
mkdir -p "$outputdir" "$tmpdir"

for arc in "${aarch[@]}"; do
  mkdir -p "${outputdir}/${arc}"

  # Download and extract proot
  wget "${repo}/${file}${arc}.deb" -P "${tmpdir}/${arc}"
  dpkg-deb -x "${tmpdir}/${arc}/${file}${arc}.deb" "${tmpdir}/${arc}"
  patchelf --replace-needed "libtalloc.so.2" "libtalloc.so" "${tmpdir}/${arc}/${prefix}/bin/proot"
  mv "${tmpdir}/${arc}/${prefix}/bin/proot" "${outputdir}/${arc}/libkaboot.so"
  mv "${tmpdir}/${arc}/${prefix}/libexec/proot/loader" "${outputdir}/${arc}/libkabooter.so"
  mv "${tmpdir}/${arc}/${prefix}/libexec/proot/loader32" "${outputdir}/${arc}/libkabooter32.so"

  # Download and extract libtalloc
  wget "${repot}/${filet}${arc}.deb" -P "${tmpdir}/${arc}"
  dpkg-deb -x "${tmpdir}/${arc}/${filet}${arc}.deb" "${tmpdir}/${arc}"
  mv "${tmpdir}/${arc}/${prefix}/lib/libtalloc.so.2.4.2" "${outputdir}/${arc}/libtalloc.so"
done

echo -e "\nDone!"

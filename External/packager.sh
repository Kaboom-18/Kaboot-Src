#!/bin/bash

BLUE='\033[1;34m'
GREEN='\033[1;32m'
CYAN='\033[1;36m'
RED='\033[1;31m'
RST='\033[0m'

INSTALLED_ROOTFS_DIR=$HOME/packages
OUTPUT_DIR=$HOME/output

msg() {
    echo -e "$@"
}

distro_name="$1"
config="$2"

if [ -z "$distro_name" ] || [ -z "$config" ]; then
    msg "${RED}[!] Missing arguments.${RST}"
    msg "${CYAN}Usage: <distro_name> <config.json>${RST}"
    exit 1
fi

if [ ! -d "${INSTALLED_ROOTFS_DIR}/${distro_name}" ]; then
    msg "${RED}[!] Distro '${distro_name}' is not installed.${RST}"
    exit 1
fi

if [ ! -f "$config" ]; then
    msg "${RED}[!] Config file '${config}' not found.${RST}"
    exit 1
fi

msg "${BLUE}[${GREEN}*${BLUE}] ${CYAN}Fixing file permissions in rootfs...${RST}"
find "${INSTALLED_ROOTFS_DIR}/${distro_name}" -type d -print0 | xargs -0 -r chmod u+rx
find "${INSTALLED_ROOTFS_DIR}/${distro_name}" -type f -executable -print0 | xargs -0 -r chmod u+rx
find "${INSTALLED_ROOTFS_DIR}/${distro_name}" -type f ! -executable -print0 | xargs -0 -r chmod u+r
msg "${BLUE}[${GREEN}*${BLUE}] ${CYAN}File permissions fixed.${RST}"

mkdir -p "${OUTPUT_DIR}"
temp_dir=$(mktemp -d)

# Copy rootfs and config
cp -r "${INSTALLED_ROOTFS_DIR}/${distro_name}" "${temp_dir}/rootfs"
cp "$config" "${temp_dir}/config.json"

backup_file="${OUTPUT_DIR}/${distro_name}.tar.gz"
msg "${BLUE}[${GREEN}*${BLUE}] ${CYAN}Backing up to '${backup_file}'...${RST}"

proot -l tar -czf "${backup_file}" -C "${temp_dir}" rootfs config.json

if [ $? -eq 0 ]; then
    msg "${BLUE}[${GREEN}✓${BLUE}] ${GREEN}Backup completed successfully.${RST}"
else
    msg "${RED}[!] Backup failed.${RST}"
    exit 1
fi

# Cleanup temp
rm -rf "${temp_dir}"

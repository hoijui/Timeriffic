#!/bin/bash

ADB=~/sdk/platform-tools/adb
AAPT=~/sdk/platform-tools/aapt
ANDROID=~/sdk/tools/android.bat
EMULATOR=~/sdk/tools/emulator

APK="$1"
PKG=""

function warn() {
    echo "$*" > /dev/stderr
}

function die() {
    echo "$*" > /dev/stderr
    exit 1
}

function check_tool() {
    if [[ ! -x $1 && ! -x $(which $1 2> /dev/null) ]] ; then
        echo "Missing $1. Please install first."
        exit 2
    fi
}

function check_tools() {
    check_tool $ADB
    check_tool $AAPT
    check_tool $ANDROID
    check_tool $EMULATOR
    check_tool nc
}

#-----

function check_apk() {
    [[ ! -f "$APK" ]] && die "## APK to test not found: $APK"

    PKG=$($AAPT dump badging "$APK" | sed -n "/package/s/.*name='\([^']\+\).*/\1/p")
    [[ ! $PKG ]] && die "## Can't find package name for $APK"
    warn "## Package name: $PKG"
}

#-----

function send_emu_cmd() {
    # $1 = emulator-5554 or just 5554
    # $* = command to send
    local PORT="${1/emulator-/}"
    shift
    ( nc -w 2 localhost $PORT <<EOE
$*
quit
EOE
    ) | grep -E -v "OK|KO|Android Console" 
}

function close_all_emus() {
    local EMU
    
    # find any emulator (offline or online) and kill them
    EMU=$(adb devices | grep emulator | cut -f 1)
    while [[ $EMU ]]; do
        warn "## Closing $EMU"
        send_emu_cmd $EMU kill
        EMU=$(adb devices | grep emulator | cut -f 1)
    done
}

function list_avds() {
    # find all avds named avd_<number>
    $ANDROID list avds | grep Name: | grep avd_[1-9] | cut -d : -f 2    
}

function start_avd() {
    local AVD="$1"
    close_all_emus

    warn "## Starting emulator @$AVD"
    ( $EMULATOR -no-snapshot-save -no-audio @$AVD ) &
    
    # wait for an emulator to come online
    local EMU
    local N=0
    while [[ ! $EMU ]]; do
        EMU=$(adb devices | grep device | grep emulator | cut -f 1)
        if [[ ! $EMU ]]; then
            warn "## [$N] Waiting for emulator @$AVD..."
            [[ $N -gt 5 ]] && $ADB devices > /dev/stderr
            sleep 1
            N=$((N+1))
            if [[ $N -gt 60 ]]; then
                warn "## Giving up starting emulator @$AVD"
                exit 1
            fi
        fi
    done
    warn "## Emulator found: $EMU"
    
    # Remove any previous package, install new one, run monkey
    $ADB uninstall $PKG
    $ADB install $APK
    $ADB shell monkey -p $PKG -v 1000 -c Intent.CATEGORY_LAUNCHER

    $ADB uninstall $PKG
    $ADB install $APK
    $ADB shell monkey -p $PKG -v 1000

    close_all_emus
}

check_tools
check_apk
for AVD in $(list_avds); do
    start_avd $AVD
done


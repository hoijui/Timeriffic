#!/bin/bash

set -e # fail early

shopt -s extglob  # extended glob pattern

function die() {
  echo "Error: " $*
  echo
  echo "USage: $0 <version>"
  echo "Automatically sign [A-Z].apk"
  exit 1
}

function process() {
	SRC="$1"

	BASE="${SRC/.apk/}"
	BASE="${BASE/Pub/}"
	
	DATE=`date +%Y%m%d`
	N=1
	while /bin/true; do
		EXT=`python -c "print chr(96+$N)"`
		DEST="${BASE}_${VERS}${DATE}${EXT}.apk"
		[ ! -e "$DEST" ] && break
		N=$((N+1))
		[ "$N" == "27" ] && die "$DEST exists, can't generate higher letter."
	done
	
	ALIAS="${USER/p*/lf}2"
	
	echo "Signing $SRC => $DEST with alias $ALIAS"
	
	jarsigner -verbose -keystore `cygpath -w ~/*-release-*.keystore` "$SRC" $ALIAS
	
	SIZE1=`stat -c "%s" "$SRC"`
	
	for z in ~/{usdk,sdk}/tools/zipalign.exe; do
		if [[ -x "$z" && -e "$SRC" ]]; then
			echo "Using $z"
			"$z" -f -v 4 "$SRC" "$DEST" && rm -v "$SRC"
		fi
	done
	
	[[ -e "$SRC" ]] && mv -v "$SRC" "$DEST"

	SIZE2=`stat -c "%s" "$DEST"`
	
	echo "$DEST has been signed and zipaligned (added $((SIZE2-SIZE1)) bytes)" 

    hg add "$DEST"
    hg commit -m "Binary for version ${VERS/_/}" "$DEST"
}

function update() {
	F="$1"
	K="Key"
	I="i"
	U="u"
	if ! unzip -l $F | grep -qs $K$I ; then
		echo "Missing file $K$I in $F"
		exit 2
	else
		echo "Verified $K$I"
	fi
	if unzip -l $F | grep -qs $K$U ; then
		N=`unzip -l $F | grep $K$U | awk '{ print $4 }'`
		echo "Extra file $N found in $F"
		zip -d $F $N
	else
		echo "Verified !$K$U"
	fi
}

APK=( [tTfFB]+([^_]).apk )
APK="${APK}"
if [ ! -f "$APK" ]; then
    die "Failed to find an APK to sign"
fi

VERS="$1"
if [ -z "$VERS" ]; then
    # Try to use AAPT on first APK to guess the version number
    AAPT=( ~/sdk/platforms/*/tools/aapt.exe )
    AAPT="${AAPT}"  # convert first's array value into its own value
    if [ ! -x "$AAPT" ]; then
        die "Failed to find aapt.exe"
    fi
    
    VERS=`"$AAPT" dump badging "$APK" | grep versionName | sed "s/.*versionName='\(.*\)'/\1/g"`
    [ -n "$VERS" ] && VERS="v${VERS}"
    echo "Found version $VERS"
fi

[ -n "$VERS" ] && VERS="${VERS}_"

[ -z "$VERS" ] && die "Missing version number"

for i in "$APK" ; do
	if [ -f "$i" ]; then
        ##--chmod a+r "$i"

	# We're currently not using the Key wrapper
	# [[ "${i:0:1}" == "T" ]] && update "$i"

	process "$i"
	fi
done

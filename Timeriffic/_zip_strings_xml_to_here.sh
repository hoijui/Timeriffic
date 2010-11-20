#!/bin/bash
set -e
DRY="echo"
[[ "$1" == "-f" ]] && DRY=""
for i in res/values-*/strings.xml ; do
  L=${i#*values-}
  L=${L%%/*}

  A=`basename ${i}`
  A=${A%.xml}_$L.xml

  $DRY cp -v $i $A
  $DRY unix2dos $A
  $DRY zip -9 ${A%.xml}.zip $A
  $DRY rm -f $A
done
if [[ $DRY ]] ; then echo ; echo "DRY RUN ONLY. Use -f to force." ; fi

#!/bin/sh

if ! java -version >& /dev/null
then
echo "Could not find Java."
echo "You must have Java installed and in your path."
exit
fi

relativePathToGame=`dirname $0`
cd $relativePathToGame

java -cp lib/patch.jar:classes:lib/plastic-1.2.0.jar:lib/backport-util-concurrent.jar games.strategy.engine.framework.GameRunner

#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Determine the Java command to use to start the JVM.
if [ -n "" ]; then
    JAVACMD=""
fi

if [ -z "$JAVACMD" ]; then
    if [ -n "$JAVA_HOME" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="java"
    fi
fi

# Determine the script path and name, which is useful for finding the wrapper JAR.
APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`

# Resolve directory of the current script, and set the directory as the current working directory.
# This is required for the application to find its resources.
# It also avoids issues with Windows drives that are not mapped in the WSL environment.
# For example, when invoking the script from a drive that is different than the one it is on.
# See https://github.com/microsoft/WSL/issues/4256
case "$(uname)" in
    *CYGWIN*|*MINGW*|*MSYS*) APP_HOME=`cygpath -u "$APP_HOME"` ;;
esac
cd "$APP_HOME"

# Add default JVM options for the Gradle daemon.
# You can change these options by setting the JAVA_OPTS environment variable.
DEFAULT_JVM_OPTS='"-Xmx1024m" "-Dfile.encoding=UTF-8"'

# Make the wrapper JAR and other necessary tools available.
if [ -e "gradle/wrapper/gradle-wrapper.jar" ]; then
    GRADLE_JAR="gradle/wrapper/gradle-wrapper.jar"
else
    echo "Cannot find gradle/wrapper/gradle-wrapper.jar"
    exit 1
fi

# Execute Gradle, with the wrapper JAR."$JAVACMD" $JAVA_OPTS $DEFAULT_JVM_OPTS -cp "$APP_HOME/$GRADLE_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
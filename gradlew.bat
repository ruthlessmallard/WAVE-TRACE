@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@rem %~dp0 is the directory where this script is located.
@rem Change the current directory to the directory where this script is located.
@pushd %~dp0

@rem Set %JAVA_HOME% to point to the Java installation directory, for example, C:\Program Files\Java\jdk-17
@set JAVA_HOME=

@rem Find the Java executable.
@set JAVACMD="%JAVA_HOME%\bin\java.exe"
@if not exist %JAVACMD% set JAVACMD=java.exe

@rem Set default JVM options for the Gradle daemon.
@rem You can change these options by setting the JAVA_OPTS environment variable.
@set DEFAULT_JVM_OPTS="-Xmx1024m -Dfile.encoding=UTF-8"

@rem Make the wrapper JAR and other necessary tools available.
@if exist "gradle\wrapper\gradle-wrapper.jar" (
    @set GRADLE_JAR="gradle\wrapper\gradle-wrapper.jar"
) else (
    @echo "Cannot find gradle\wrapper\gradle-wrapper.jar"
    @exit /b 1
)

@rem Execute Gradle, with the wrapper JAR.
@%JAVACMD% %JAVA_OPTS% %DEFAULT_JVM_OPTS% -cp %GRADLE_JAR% org.gradle.wrapper.GradleWrapperMain %*
@echo off

if "%DSPACE_HOME%" == "" (
	echo Environment variable DSPACE_HOME not set.
	goto end
)

set CLASSPATH=%~sdp0*

echo Additional CLASSPATH: %CLASSPATH%
call "%DSPACE_HOME%\bin\dspace.bat" dsrun %*

set CLASSPATH=

:end

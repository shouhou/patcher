@echo off
echo Start the jar installation package...
call mvn install:install-file -Dfile=expect4j-1.0.jar -DgroupId=com.github.cverges.expect4j -DartifactId=expect4j -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
call mvn install:install-file -Dfile=jakarta-oro-2.0.7.jar -DgroupId=ant -DartifactId=ant-jakarta-oro -Dversion=2.0.7 -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true

echo The jar package is installed.
pause
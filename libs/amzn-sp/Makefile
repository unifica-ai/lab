all: install

AA_DIR ?= ${HOME}/src/github.com/unifica-ai/selling-partner-api-models

install:
	mvn package -f ${AA_DIR}/clients/sellingpartner-api-aa-java/pom.xml
	mvn install:install-file \
	-Dfile=${AA_DIR}/clients/sellingpartner-api-aa-java/target/sellingpartnerapi-aa-java-2.0-jar-with-dependencies.jar \
	-DgroupId=com.amazon.sellingpartnerapi -DartifactId=sellingpartnerapi-aa-java -Dversion=2.0 -Dpackaging=jar

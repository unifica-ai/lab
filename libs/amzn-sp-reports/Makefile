AA_DIR ?= ${HOME}/src/github.com/unifica-ai/selling-partner-api-models

SWAGGER_CODEGEN ?= swagger-codegen

client:
	${SWAGGER_CODEGEN} generate -i ${AA_DIR}/models/reports-api-model/reports_2021-06-30.json \
	-l java -t ${AA_DIR}/clients/sellingpartner-api-aa-java/resources/swagger-codegen/templates \
	-o java -c ${AA_DIR}/clients/sellingpartner-api-aa-java/resources/java/config.json

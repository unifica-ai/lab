# Amazon Selling Partner Reports API

See the `amazon-sp` lib

## Generating client libraries

When Amazon releases a new version of their [API][apidocs], you need to generate
a new version of the Java client using the Swagger Code Generator.

See [Using SDKs][sdks] for more.

[apidocs]: https://developer-docs.amazon.com/sp-api/docs/reports-api-v2021-06-30-reference
[sdks]: https://developer-docs.amazon.com/sp-api/docs/generating-a-java-sdk-with-lwa-token-exchange

Use `make client` to create a client library from the model in the `models` directory and put it in `java`:

``` 4d
pushd libs/amzn-sp-reports
SWAGGER_CODEGEN="java -DapiTests=false -jar /Users/kyle/dist/swagger-codegen-cli.jar" make client
popd
```

This will create a file in the `models` directory.

TODO do this with tools.deps instead of Maven

## Clean

```
clj -T:build clean
```


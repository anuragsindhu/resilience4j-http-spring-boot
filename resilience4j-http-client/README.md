# Testing

## Self-signed Java Keystore and Truststore for testing

The following keystore, trustore pair is generated in a way so that it can also be used to simulate a secured HTTPs
backend.

- **Generate a self-signed certificate and private key**

    ```shell
    keytool -genkeypair \
      -alias wiremock \
      -keyalg RSA \
      -keysize 2048 \
      -validity 3650 \
      -keystore keystore.p12 \
      -storetype PKCS12 \
      -storepass changeit \
      -keypass changeit \
      -dname "CN=localhost" \
      -ext "SAN=dns:localhost"
     ```
- **Export the certificate**
    ```shell
    keytool -exportcert \
      -alias wiremock \
      -storetype PKCS12 \
      -keystore keystore.p12 \
      -storepass changeit \
      -rfc \
      -file cert.pem
    ```
- **Import the certificate into a truststore**
    ```shell
    keytool -importcert \
      -alias testcert \
      -file cert.pem \
      -storetype PKCS12 \
      -keystore truststore.p12 \
      -storepass changeit \
      -noprompt
    ```
---

```shell
keytool -genkeypair \
  -alias wiremock \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore wiremock-keystore.p12 \
  -storepass changeit \
  -dname "CN=test.example.com"
```

```shell
keytool -exportcert \
  -alias wiremock \
  -keystore wiremock-keystore.p12 \
  -storepass changeit \
  -file wiremock-cert.cer \
  -rfc
```

```shell
keytool -importcert \
  -alias wiremock \
  -file wiremock-cert.cer \
  -keystore wiremock-truststore.jks \
  -storepass changeit \
  -noprompt
```
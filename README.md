# <img height="25" src="./images/AILLogoSmall.png" width="40"/> Config-service

<a href="https://www.legosoft.com.mx"><img height="160px" src="./images/ConfigRepoLogo.png" alt="AI Legorreta" align="left"/></a>
Microservice for query the configuration files (*.yml files) for all micro.services. Platforms:
* Local no docker: `localNoDocker` platform tu run the microservice inside the IDE.
* Local docker: `local` on-premise (docker desktop).

Other platforms does not use Spring Config Server but Kubernetes ConfigMap & Secret.

## Introduction

Spring Cloud Config server that is deployed as Docker container and can manage a services' configuration information using a file system/ classpath or GitHub-based repository.

This project is for storage properties files for all projects.
Every project has a folder. The name of the folder must be the name of the project.

note: Fow Kubernetes platform instead of using Spring Config Server we use a better approach that is to use `ConfigMaps`.

## First steps

Once you add this dependency add the **@EnableConfigServer** on your main spring boot application class, this annotation
enable that your application will be expose as a Config Server.

```java
//Add this annotation to expose this application as Config Server
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

}
``` 

## Configuration to encrypt and decrypt passwords

To use encrypt and decrypt is necessary install **Oracle JCE**, to install it read this article [Installation guide](https://dzone.com/articles/install-java-cryptography-extension-jce-unlimited).

Now we have installed **Oracle JCE** we need to generate our keys to encrypt and decrypt to do that follow this instructions:

Open a terminal and find your project, go to _src/resources_, once in resources:

```
keytool -genkeypair -alias config-server-template -keyalg RSA -sigalg SHA512withRSA \ 
-dname 'CN=LegoSoft Templates,OU=LegoSoft Development,O=LegoSoft' \
-keypass L3g050ft -keystore config-server-template.jks -storepass L0m4s11000
```

If you want to know more about [Keytool](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html)

Once we run this instruction a file ***.jks** will appear in our resources directory.

The actual version we created a password for the Neo4j database for the iam-server-repo micro services. The file is called iam-server-repo-keystore.jks

This will help us to encrypt and decrypt using the keys that we create.

* location: Where is our ***.jks** file
* password: Password of our key store
* alias: The name of the key pair that we will use
* secret: Password of our key pair


##### Encrypt

To encrypt a password, start your config server project, in postman execute:

**http://host:port/encrypt** This is a **POST**, send as text body the text that you want to encrypt and add the response to your YML file for each micro service.

For example: you can check the iamserverepo.yml and see the username and pasword for the Noeo4j database.

Since the Config server can have different environments:
- Default: e.g., iamserverrepo.yml
- Development; eg. iamserverrepo-dev.yml
- Testing; e.g. iamserverrepo-qa.yml
- Pre production; e.g., iamserverrepo-pre-prod.yml
- Production; e.g., iamserverepo-prod.yml

We can define different usernames and passwords for each data base in different environments.

## Running on Docker Desktop

### Create the image manually

```
./gradlew bootBuildImage
```

### Publish the image to GitHub manually

```
./gradlew bootBuildImage \
   --imageName ghcr.io/rlegorreta/config-service \
   --publishImage \
   -PregistryUrl=ghcr.io \
   -PregistryUsername=rlegorreta \
   -PregistryToken=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
```

### Publish the image to GitHub from the IntelliJ

To publish the image to GitHub from the IDE IntelliJ a file inside the directory `.github/workflows/commit-stage.yml`
was created.

To validate the manifest file for kubernetes run the following command:

```
kubeval --strict -d k8s
```

This file compiles de project, test it (for this project is disabled for some bug), test vulnerabilities running
skype, commits the code, sends a report of vulnerabilities, creates the image and lastly push the container image.

<img height="340" src="./images/commit-stage.png" width="550"/>

For detail information see `.github/workflows/commit-stage.yml` file.


### Run the image inside the Docker desktop

```
docker run \
    --net ailegorretaNet \
    -p 8071:8071 \
    -e SPRING_PROFILES_ACTIVE=local \
    config-service
```

Or a better method use the `docker-compose` tool. Go to the directory `ailegorreta-deployment/docker-platform` and run the command:

```
docker-compose up
```

## Run inside Kubernetes

### Manually

If we do not use the `Tilt`tool nd want to do it manually, first we need to create the image:

Fist step:

```
./gradlew bootBuildImage
```

Second step:

Then we have to load the image inside the minikube executing the command:

```
image load ailegorreta/config-service --profile ailegorreta 
```

To verify that the image has been loaded we can execute the command that lists all minikube images:

```
kubectl get pods --all-namespaces -o jsonpath="{..image}" | tr -s '[[:space:]]' '\n' | sort | uniq -c\n
```

Third step:

Then execute the deployment defined in the file `k8s/deployment.yml` with the command:

```
kubectl apply -f k8s/deplyment.yml
```

And after the deployment can be deleted executin:

```
kubectl apply -f k8s/deplymengt.yml
```

Fourth step:

For service discovery we need to create a service applying with the file: `k8s/service.yml` executing the command:

```
kubectl apply -f k8s/service.yml
```

And after the process we can delete the service executing:

```
kubectl deltete -f k8s/service.yml
```

Fifth step:

If we want to use the project outside kubernetes we have to forward the port as follows:

```
kubectl port-forward service/config-service 8071:80
```

Appendix:

If we want to see the logs for this `pod` we can execute the following command:

```
kubectl logs deployment/config-service
```

### Using Tilt tool

To avoid all these boilerplate steps is much better and faster to use the `Tilt` tool as follows: first create see the 
file located in the root directory of the proyect called `TiltFile`. This file has the content:

```
# Tilt file for config-service
# Build
custom_build(
    # Name of the container image
    ref = 'config-service',
    # Command to build the container image
    command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
    # Files to watch that trigger a new build
    deps = ['build.gradle', 'src']
)

# Deploy
k8s_yaml(['k8s/deployment.yml', 'k8s/service.yml'])

# Manage
k8s_resource('config-service', port_forwards=['8071'])
```

To execute all five steps manually we just need to execute the command:

```
tilt up
```

In order to see the log of the deployment process please visit the following URL:

```
http://localhost:10350
```

Or execute outsitde Tilt the commend: 

```
kubectl logs deployment/config-service
```

In order to undeploy everything just execute the command:

```
tilt down
```

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.6/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.0.6/gradle-plugin/reference/html/#build-image)
* [Config Server](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_spring_cloud_config_server)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.0.1/reference/htmlsingle/#production-ready)

#### Guides
The following guides illustrate how to use some features concretely:

* [Centralized Configuration](https://spring.io/guides/gs/centralized-configuration/).
* Chapter 4 of 'Cloud Native Spring in Action'.
* Chapter 6 of 'Cloud Native Spring in Action'.
* Chapter 7 of 'Cloud Native Spring in Action'.
* Book 'Spring Microservices in Action'.

#### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

#### Contact AI Legorreta

Feel free to reach out to AI Legorreta on [web page](https://legosoft.com.mx).

Version: 2.0
©LegoSoft Soluciones, S.C., 2023
# bnuuy-war
WAR Common LIB in Java with Spring 

# Dev

Tech stack:
- Java
- Spring
- etc

## Gradle

Este é um projeto construído com Gradle (similar ao maven)
Dentro do gradle está sendo usado um estilo multi-módulo com cada gradle module com sua responsabilidade.

O Arquivo `settings.gradle` tem algumas definições gerais e a lista de modulos do projeto.
O arquivo `gradle.properties` possui diversas propriedades como versao do java, versões das dependencias, encoding e outras configs.

Existe um `gradle.build` geral com detalhes de dependencias e instruções de build gerais.

Cada modulo tem seu `build.gradle` com dependencias que cabem a esse modulo, inclusive se usam outros modulos internos.

### CMDS

Clean 

    ./gradlew clean

Build with tests

    ./gradlew build

Running Spring Boot from jar with Gradle

    ./gradlew bootRun -Dspring.profiles.active=dev

Build Docker image

    ./gradlew clean bootBuildImage

## Github

Atualmente tudo está em GitHub Actions

Principalmente existe o pipe de _**dev**_, onde qualquer coisa que cair na branch _**develop**_ vai trigar esse pipe.
Ele vai buildar a aplicação, gerar a o jar

Existe um pipeline manual para subir

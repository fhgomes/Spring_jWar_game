# bnuuy-war
WAR Server in Java Spring boot 

# Dev

Tech stack:
- Java
- Spring
- SpringBoot
- SpringCloud
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

## Docker & Compose 

This project contains a Docker Compose file named `others/docker/compose.yaml`.
Nesse compose tem as dependencias de infra para rodar o projeto e realizar as rotas necessárias.

### Configuração do docker local

Para rodar o docker local, é necessário executar o seguinte comando:

```bash 
docker-compose -f compose_dev_local.yaml up -d
```
Está ação irá atualizar ou criar a imagem local e executa-lá a partir do arquivo **compose_dev_local.yaml** que está na pasta raiz.

O arquivo de produção usando as imagens do app e com parametrizações é o compose.yaml

### Tips Rodar Local

Os scripts .sh do BD precisam estar com line ending = LF

## Banco de dados

É o PostgreSQL rodando em container docker com docker-compose.
Existem dois databases, um para dev e um para prod.
As evoluções, alterações no BD são feitas através de scripts gerenciados com a ferramenta `Flyway`
Os scripts se encontram em `./jwar-server/jwar-sboot/src/resources/db.migration`

# Infra & Devops

## Server AWS EC2

    REGIÃO              ->  
    EC2 elastic IP: 
    ip interno: 
    ssh user: ec2-user
    key: essa key-pair é gerada pela conta root administradora e/ou com permissoes na aws.
        caso não for root, ela deve ter permissões de IAM para acessar a instancia
    
    Caso precisar add outro usuário/key: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/replacing-key-pair.html
    Acesso via putty    

## DNS

Utilizado a cloudflare como reverse proxy e CDN + WAF
A cloudflare recebe as requisições do dominio.
Cada subdominio com prefixos é redirecionado para algum lugar mapeado.
Existem alguns DNS como _api.bnuuywar.com_ que direcionam para o ELASTIC IP 
que está alocado para a maquina EC2 com NGINX

### SSL

A cloudflare cuida do SSL
Através do dns proxy + SSL Strict ela redireciona tudo via https para o servidor de destino.
O Servidor de destino (nginx) está configurado com um "certificado de origem" de propria cloudflare.
Com isso a requisição de ponta a ponta fica segura e confiável.

## Como roda a aplicação e ambientes na mesma instancia ec2

    Tudo roda com Docker e Docker compose.
    Um Nginx faz o roteamento (proxy reverso, como se fosse um load balancer) para os envs
    Existem dois containers com a aplicação rodando com profiles DEV e PROD.
    Existe uma instalação PostgreSQL com dois Databases, um para cada profile

#### PROD
    REGIÃO              ->  
    API                 ->  api.*: 
    BD RDS              ->	
    SECRETS/PARAMETROS  ->  

####  QA
    REGIÃO              ->  norte virginia
    API                 ->  qa-api.*: 
    BD RDS              ->	
    SECRETS/PARAMETROS  ->  

# Processo de deploy da aplicacão - CI-CD

Hoje as versões de `DEV` e `PRD` estão sendo executadas na mesma instancia EC2 da AWS

Todo o fluxo de deploy está integrado a pipeline do projeto, ou seja, ao ser mergeado uma nova versão na branch de `develop` será gerado uma nova imagem da versão de `DEV` e realizado o deploy na instância.
E quando for realizado um merge na branch `main` será gerada uma nova imagem da versão de `PRD` e realizado o deploy na instância.

Existe um runner no GitHub instalado na instância, onde é possível via pipeline executar comandos diretamente na instância.

Para mais informacões, segue a documentacão: https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners/about-self-hosted-runners

## Github

Atualmente tudo está em GitHub Actions

Principalmente existe o pipe de _**dev**_, onde qualquer coisa que cair na branch _**develop**_ vai trigar esse pipe.
Ele vai buildar a aplicação, gerar a imagem spring boot do container, subir para o ECR, baixar na máquina EC2, 
atualizar o docker compose.

A diferença do pipeline de _**prod**_ é que ele está semi-automatizado. 
Qualquer coisa que cair na branch _**main**_ , vai trigar o pipe de **_build_release_**, 
Ele vai buildar a aplicação, gerar a imagem spring boot do container, subir para o ECR, mas não vai atualizar a aplicação.
Existe um pipeline manual para subir

Existem outros auxiliares para atualizar os demais componentes da infra

## Secrets 

Existem alguns valores secretos configurados no secrets que são injetados no System Env (ec2) (pelo githubactions), para depois serem lidos
pelo docker-compose e serem injetados de forma segura e parametrizada pelo spring boot profiles.

## Self Hosted Runner

Foi optado por rodar uma parte do pipeline via _**Self Hosted Runner**_ que nada mais é que o proprio EC2 escutando e rodando
pipelines do repositorio.
Ele foi baixado na pasta `/home/ec2-user/runner` e configurado para conectar com o repositório.
Também foi connfigurado para rodar automaticamente quando o sistema iniciar, usando `SYSTEMD`

https://github.com/fhgomes/Spring_jWar_game/settings/actions/runners/
https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners/about-self-hosted-runners


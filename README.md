# LinkTrack - URL Shortener con Analytics

Acortador de URLs con analiticas en tiempo real, construido con Spring Boot y desplegado en AWS usando Terraform e infraestructura como codigo.

## Arquitectura

```
Cliente -> ALB -> ECS Fargate (Spring Boot)
                      |-- RDS PostgreSQL (datos)
                      |-- ElastiCache Redis (cache redirects)
                      |-- SQS (eventos de clicks async)
                               |-- Consumer -> RDS
```

Stack tecnologico:
- **Backend:** Java 21, Spring Boot 3.4, Spring Security JWT, Spring Data JPA, Flyway
- **Mensajeria:** AWS SQS publisher/consumer asincrono
- **Cache:** Redis - ElastiCache en prod, Docker en local
- **Base de datos:** PostgreSQL 16 - RDS en prod, Docker en local
- **Infraestructura:** Terraform modular - VPC, ECS Fargate, RDS, ElastiCache, SQS, ALB, Secrets Manager
- **CI/CD:** GitHub Actions - test, build Docker, push ECR, terraform apply, deploy ECS
- **Observabilidad:** CloudWatch Logs y health checks

## Estructura del proyecto

```
linktrack/
|-- backend/
|   |-- src/main/java/com/linktrack/
|   |   |-- config/         # AWS, Redis, Security
|   |   |-- controller/     # Auth, URLs, Redirect
|   |   |-- dto/            # Request/Response records
|   |   |-- exception/      # Global exception handler
|   |   |-- model/          # Entidades JPA
|   |   |-- repository/     # Spring Data repositories
|   |   |-- security/       # JWT filter y service
|   |   |-- service/        # Logica de negocio + SQS
|   |-- src/main/resources/db/migration/   # Flyway SQL
|   |-- Dockerfile          # Multi-stage build
|-- infrastructure/terraform/
|   |-- modules/
|   |   |-- vpc/            # VPC, subredes, NAT Gateway
|   |   |-- ecs/            # ECR, ECS Fargate, ALB, IAM
|   |   |-- rds/            # PostgreSQL en subred privada
|   |   |-- elasticache/    # Redis cluster
|   |   |-- sqs/            # Cola principal + Dead Letter Queue
|   |-- environments/dev/   # Variables y backend S3
|-- .github/workflows/deploy.yml   # CI/CD pipeline
|-- docker-compose.yml      # Local: Postgres + Redis + LocalStack
```

## Ejecutar en local

Requisitos: Java 21, Docker, AWS CLI

```bash
# 1. Clonar el repo
git clone https://github.com/rafamorenoo/linktrack
cd linktrack

# 2. Levantar infraestructura local
docker compose up -d

# 3. Crear cola SQS en LocalStack
aws configure set aws_access_key_id test
aws configure set aws_secret_access_key test
aws --endpoint-url=http://localhost:4566 --region eu-west-1 sqs create-queue --queue-name click-events

# 4. Compilar
cd backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw package -DskipTests

# 5. Arrancar la app
DB_HOST=localhost DB_PORT=5432 DB_NAME=linktrack DB_USER=linktrack DB_PASSWORD=linktrack \
REDIS_HOST=localhost REDIS_PORT=6379 \
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_REGION=eu-west-1 \
java -jar target/linktrack-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Auth

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/login` | Login, devuelve JWT |
| POST | `/api/auth/refresh?token=` | Refresh access token |

### URLs

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/urls` | Crear URL corta |
| GET | `/api/urls` | Listar URLs del usuario paginado |
| DELETE | `/api/urls/{id}` | Desactivar URL |
| GET | `/api/urls/{id}/analytics` | Clicks por dia, pais y dispositivo |

### Redirect

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/s/{code}` | Redirect 302 y registra click via SQS |

### Ejemplo completo

```bash
# Registro
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123","name":"User"}'

# Login y guardar token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Crear URL corta con codigo personalizado
curl -X POST http://localhost:8080/api/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://github.com/rafamorenoo/linktrack","title":"Mi repo","customCode":"demo"}'

# Redirect - registra click async via SQS
curl -v http://localhost:8080/api/s/demo

# Ver analytics (esperar ~5s a que el consumer procese el mensaje)
curl http://localhost:8080/api/urls/{id}/analytics \
  -H "Authorization: Bearer $TOKEN"
```

## Despliegue en AWS

```bash
# 1. Crear bucket S3 para Terraform state
aws s3 mb s3://linktrack-terraform-state --region eu-west-1

# 2. Inicializar Terraform
cd infrastructure/terraform/environments/dev
terraform init

# 3. Aplicar infraestructura
export TF_VAR_db_password="tu-password-segura"
export TF_VAR_jwt_secret="tu-jwt-secret-minimo-256-bits"
terraform apply

# 4. Build y push de la imagen a ECR
ECR_URL=$(terraform output -raw ecr_repository_url)
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin $ECR_URL
docker build -t $ECR_URL:latest backend/
docker push $ECR_URL:latest

# 5. URL de la app
terraform output alb_url

# 6. Destruir todo cuando no lo necesites para evitar costes
terraform destroy
```

## CI/CD con GitHub Actions

El pipeline se activa en cada push a `main` y ejecuta estos pasos en orden:

1. Tests con Maven
2. Build de imagen Docker multicapa
3. Push a Amazon ECR
4. Terraform apply para actualizar infraestructura
5. Force redeploy del servicio ECS

Secrets necesarios en el repositorio de GitHub:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DB_PASSWORD`
- `JWT_SECRET`

## Decisiones de diseno

**SQS para clicks:** el redirect es el endpoint mas critico en latencia. Publicar el evento de click en SQS de forma asincrona evita que la escritura en BD afecte el tiempo de respuesta del usuario.

**Redis para redirects:** el endpoint `GET /s/{code}` cachea solo el string de la URL original, no la entidad JPA completa. Esto evita problemas de serializacion de relaciones lazy y reduce drasticamente la carga en RDS para URLs populares.

**Subredes privadas:** RDS y ElastiCache viven en subredes privadas sin acceso desde internet. Solo las tasks de ECS pueden conectarse a ellas mediante security groups especificos.

**Secrets Manager:** las credenciales de BD, Redis y JWT se inyectan en el contenedor desde Secrets Manager en tiempo de arranque. Ninguna credencial viaja en texto plano en el task definition ni en el repositorio.

**Flyway:** las migraciones de BD estan versionadas en el repositorio y se aplican automaticamente al arrancar la app, garantizando consistencia entre entornos.

**Multi-stage Dockerfile:** la imagen final usa JRE en vez de JDK y aprovecha el sistema de layers de Spring Boot para que los rebuilds tras cambios de codigo sean instantaneos.

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
  backend "s3" {
    bucket = "linktrack-terraform-state"
    key    = "dev/terraform.tfstate"
    region = "eu-west-1"
  }
}

provider "aws" {
  region = var.region
}

module "vpc" {
  source          = "../../modules/vpc"
  project         = var.project
  env             = var.env
  cidr            = "10.0.0.0/16"
  azs             = ["eu-west-1a", "eu-west-1b"]
  public_subnets  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnets = ["10.0.11.0/24", "10.0.12.0/24"]
}

module "sqs" {
  source  = "../../modules/sqs"
  project = var.project
  env     = var.env
}

module "ecs" {
  source              = "../../modules/ecs"
  project             = var.project
  env                 = var.env
  region              = var.region
  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  private_subnet_ids  = module.vpc.private_subnet_ids
  sqs_queue_arn       = module.sqs.queue_arn
  secrets_arn         = aws_secretsmanager_secret.app.arn
  task_cpu            = 256
  task_memory         = 512
  desired_count       = 1
}

module "rds" {
  source         = "../../modules/rds"
  project        = var.project
  env            = var.env
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  app_sg_id      = module.ecs.app_sg_id
  instance_class = "db.t3.micro"
  db_name        = "linktrack"
  db_user        = var.db_user
  db_password    = var.db_password
}

module "elasticache" {
  source     = "../../modules/elasticache"
  project    = var.project
  env        = var.env
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  app_sg_id  = module.ecs.app_sg_id
  node_type  = "cache.t3.micro"
}

# Secrets Manager
resource "aws_secretsmanager_secret" "app" {
  name = "${var.project}/${var.env}/app"
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    DB_HOST              = split(":", module.rds.endpoint)[0]
    DB_NAME              = "linktrack"
    DB_USER              = var.db_user
    DB_PASSWORD          = var.db_password
    REDIS_HOST           = module.elasticache.endpoint
    JWT_SECRET           = var.jwt_secret
    SQS_CLICK_EVENTS_URL = module.sqs.queue_url
  })
}

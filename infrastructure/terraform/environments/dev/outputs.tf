output "alb_url"            { value = "http://${module.ecs.alb_dns_name}" }
output "ecr_repository_url" { value = module.ecs.ecr_repository_url }
output "rds_endpoint"       { value = module.rds.endpoint }
output "redis_endpoint"     { value = module.elasticache.endpoint }

variable "project"            { type = string }
variable "env"                { type = string }
variable "region"             { type = string }
variable "vpc_id"             { type = string }
variable "public_subnet_ids"  { type = list(string) }
variable "private_subnet_ids" { type = list(string) }
variable "sqs_queue_arn"      { type = string }
variable "secrets_arn"        { type = string }
variable "task_cpu"           { type = number; default = 512 }
variable "task_memory"        { type = number; default = 1024 }
variable "desired_count"      { type = number; default = 1 }

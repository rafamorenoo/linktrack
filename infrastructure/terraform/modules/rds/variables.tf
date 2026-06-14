variable "project"        { type = string }
variable "env"            { type = string }
variable "vpc_id"         { type = string }
variable "subnet_ids"     { type = list(string) }
variable "app_sg_id"      { type = string }
variable "instance_class" { type = string; default = "db.t3.micro" }
variable "db_name"        { type = string }
variable "db_user"        { type = string }
variable "db_password"    { type = string; sensitive = true }

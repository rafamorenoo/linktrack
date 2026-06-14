variable "project"     { type = string; default = "linktrack" }
variable "env"         { type = string; default = "dev" }
variable "region"      { type = string; default = "eu-west-1" }
variable "db_user"     { type = string; default = "linktrack" }
variable "db_password" { type = string; sensitive = true }
variable "jwt_secret"  { type = string; sensitive = true }

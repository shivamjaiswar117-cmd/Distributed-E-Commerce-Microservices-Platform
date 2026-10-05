terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-south-1"
}

resource "aws_db_instance" "ecommerce_db" {
  identifier = "ecommerce-db"

  engine         = "mysql"
  engine_version = "8.0.46"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp2"

  username = "admin"

  publicly_accessible = true
  multi_az            = false

  backup_retention_period = 1
  deletion_protection     = false

  skip_final_snapshot = true
}

resource "aws_default_vpc" "main" {}

resource "aws_security_group" "rds" {
  name        = "default"
  description = "default VPC security group"
  vpc_id      = "vpc-094b10c626b234075"

  ingress {
    from_port = 3306
    to_port   = 3306
    protocol  = "tcp"

    cidr_blocks = [
      "152.59.110.186/32",
      "152.59.107.124/32",
      "152.59.107.229/32",
      "152.58.1.241/32"
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
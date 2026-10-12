resource "aws_iam_role" "eks_cluster" {
  name = "ecommerce-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  role       = aws_iam_role.eks_cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "eks_nodes" {
  name = "ecommerce-eks-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_worker_node" {
  role       = aws_iam_role.eks_nodes.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "eks_cni" {
  role       = aws_iam_role.eks_nodes.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "eks_ecr_read_only" {
  role       = aws_iam_role.eks_nodes.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_eks_cluster" "ecommerce" {
  name     = "distributed-ecommerce-cluster"
  role_arn = aws_iam_role.eks_cluster.arn

  vpc_config {
    subnet_ids = [
      "subnet-07a650396cf35db99",
      "subnet-0639f2d1045a0fbe8",
      "subnet-0c37dabda53611241"
    ]

    endpoint_public_access  = true
    endpoint_private_access = true

    public_access_cidrs = [
      "152.58.2.137/32"
    ]
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_policy
  ]
}

resource "aws_eks_node_group" "ecommerce" {
  cluster_name    = aws_eks_cluster.ecommerce.name
  node_group_name = "ecommerce-workers"
  node_role_arn   = aws_iam_role.eks_nodes.arn

  subnet_ids = [
    "subnet-07a650396cf35db99",
    "subnet-0639f2d1045a0fbe8",
    "subnet-0c37dabda53611241"
  ]

  instance_types = ["t3.small"]

  scaling_config {
    desired_size = 3
    max_size     = 3
    min_size     = 2
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_worker_node,
    aws_iam_role_policy_attachment.eks_cni,
    aws_iam_role_policy_attachment.eks_ecr_read_only
  ]
}
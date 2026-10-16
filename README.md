@'

\# Distributed E-Commerce Microservices Platform



A cloud-native e-commerce backend built with Java, Spring Boot, Docker, Apache Kafka, Redis, MySQL, Kubernetes, Terraform, and AWS.



\## Architecture



```text

Client

&#x20; |

&#x20; v

API Gateway (9090)

&#x20; |-- User Service (8081) ------ MySQL / RDS

&#x20; |-- Product Service (8082) --- MySQL / RDS + Redis Cache

&#x20; |-- Order Service (8083) ----- MySQL / RDS

&#x20;               |

&#x20;               v

&#x20;             Kafka

&#x20;               |

&#x20;      +--------+---------+

&#x20;      |        |         |

&#x20;      v        v         v

&#x20;   Payment  Inventory  Notification

&#x20;   (8084)    (8085)      (8086)

&#x20;      |

&#x20;      v

&#x20;  Payment Result

&#x20;      |

&#x20;      v

&#x20;Order Service updates order status


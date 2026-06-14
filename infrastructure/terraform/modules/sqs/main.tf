resource "aws_sqs_queue" "dlq" {
  name                      = "${var.project}-${var.env}-click-events-dlq"
  message_retention_seconds = 1209600
  tags                      = { Name = "${var.project}-${var.env}-dlq" }
}

resource "aws_sqs_queue" "main" {
  name                       = "${var.project}-${var.env}-click-events"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 86400
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 3
  })
  tags = { Name = "${var.project}-${var.env}-click-events" }
}
